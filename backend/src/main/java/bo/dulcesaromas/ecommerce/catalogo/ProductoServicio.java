package bo.dulcesaromas.ecommerce.catalogo;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.compartido.Textos;
import bo.dulcesaromas.ecommerce.IPedidos;
import bo.dulcesaromas.ecommerce.IStock;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Productos del catálogo (CU02, CU04, HU07, HU08).
 */
@Service
class ProductoServicio {

	private final ProductoRepositorio productos;

	private final CategoriaRepositorio categorias;

	private final ImagenProductoRepositorio imagenes;

	private final IPedidos pedidos;

	private final IStock stock;

	private final Clock reloj;

	ProductoServicio(ProductoRepositorio productos, CategoriaRepositorio categorias, ImagenProductoRepositorio imagenes,
			IPedidos pedidos, IStock stock, Clock reloj) {
		this.productos = productos;
		this.categorias = categorias;
		this.imagenes = imagenes;
		this.pedidos = pedidos;
		this.stock = stock;
		this.reloj = reloj;
	}

	/**
	 * Busca por nombre sin distinguir mayúsculas ni tildes y filtra por categoría.
	 * @param soloVisibles solo los que se ven en la Tienda: activos y de una categoría activa
	 */
	@Transactional(readOnly = true)
	List<Producto> buscar(String texto, Long categoriaId, boolean soloVisibles) {
		Specification<Producto> filtro = (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			if (soloVisibles) {
				condiciones.add(cb.isTrue(raiz.get("activo")));
				condiciones.add(cb.isTrue(raiz.get("categoria").get("activa")));
			}
			if (categoriaId != null) {
				condiciones.add(cb.equal(raiz.get("categoria").get("id"), categoriaId));
			}
			if (StringUtils.hasText(texto)) {
				String patron = "%" + escaparComodines(Textos.clave(texto)) + "%";
				condiciones.add(cb.like(raiz.get("nombreBusqueda"), patron, '\\'));
			}
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
		return productos.findAll(filtro, Sort.by("nombre", "id"));
	}

	@Transactional(readOnly = true)
	Producto obtener(Long id) {
		return productos.findById(id).orElseThrow(ProductoServicio::noEncontrado);
	}

	/**
	 * Un producto desactivado, o de una categoría desactivada, no existe para la Tienda.
	 */
	@Transactional(readOnly = true)
	Producto obtenerVisible(Long id) {
		return productos.findById(id).filter(Producto::esVisible).orElseThrow(ProductoServicio::noEncontrado);
	}

	@Transactional
	Producto crear(DatosProducto datos) {
		Categoria categoria = categoriaPara(datos, null);
		return productos.save(new Producto(categoria, datos.nombre(), datos.descripcion(), datos.precio(),
				datos.disponible(), datos.personalizable(), opciones(datos)));
	}

	@Transactional
	Producto editar(Long id, DatosProducto datos) {
		Producto producto = obtener(id);
		producto.editar(categoriaPara(datos, producto), datos.nombre(), datos.descripcion(), datos.precio(),
				datos.disponible(), datos.personalizable(), opciones(datos));
		return producto;
	}

	@Transactional(readOnly = true)
	ProduccionVista produccion(Long id) {
		return vistaProduccion(obtener(id));
	}

	/**
	 * El máximo por pedido (RN18) y la receta aproximada con su tiempo (RF45, D12). Cada insumo debe existir en el
	 * inventario y aparecer una sola vez.
	 */
	@Transactional
	ProduccionVista definirProduccion(Long id, DatosProduccion datos) {
		Producto producto = obtener(id);
		Set<Long> existentes = stock.insumos().stream().map(IStock.Insumo::id).collect(Collectors.toSet());
		Set<Long> vistos = new HashSet<>();
		List<IngredienteReceta> receta = new ArrayList<>();
		for (var ingrediente : datos.recetaOVacia()) {
			if (!existentes.contains(ingrediente.insumoId())) {
				throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "INSUMO_NO_ENCONTRADO",
						"Uno de los insumos de la receta ya no existe en el inventario.", Map.of("receta", "Revisa los insumos."));
			}
			if (!vistos.add(ingrediente.insumoId())) {
				throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "INSUMO_REPETIDO",
						"Cada insumo va una sola vez en la receta.", Map.of("receta", "Quita el insumo repetido."));
			}
			receta.add(new IngredienteReceta(ingrediente.insumoId(), ingrediente.cantidad()));
		}
		// Hibernate inserta las filas nuevas antes de borrar las quitadas: se borra la receta anterior primero, para no
		// chocar con la regla de un insumo por producto.
		producto.definirProduccion(datos.maximoPorPedido(), datos.minutosPorUnidad(), List.of());
		productos.flush();
		producto.definirProduccion(datos.maximoPorPedido(), datos.minutosPorUnidad(), receta);
		return vistaProduccion(producto);
	}

	private ProduccionVista vistaProduccion(Producto producto) {
		Map<Long, IStock.Insumo> insumos = stock.insumos().stream()
			.collect(Collectors.toMap(IStock.Insumo::id, Function.identity()));
		return ProduccionVista.de(producto, insumos);
	}

	@Transactional
	Producto cambiarDisponibilidad(Long id, boolean disponible) {
		Producto producto = obtener(id);
		producto.cambiarDisponibilidad(disponible);
		return producto;
	}

	@Transactional
	Producto activar(Long id) {
		Producto producto = obtener(id);
		producto.activar();
		return producto;
	}

	/**
	 * Deja de verse en la Tienda y conserva sus pedidos (RN10).
	 */
	@Transactional
	Producto desactivar(Long id) {
		Producto producto = obtener(id);
		producto.desactivar();
		return producto;
	}

	/**
	 * RN10: un producto que nunca se pidió se puede borrar, por ejemplo si se creó por error. Uno con pedidos solo
	 * se desactiva, para conservar el historial.
	 */
	@Transactional
	void borrar(Long id) {
		Producto producto = obtener(id);
		// Las líneas de pedido son de Pedidos: el catálogo pregunta por IPedidos en vez de leer su tabla.
		if (pedidos.tienePedidosConProducto(id)) {
			throw new ReglaNegocioException(HttpStatus.CONFLICT, "PRODUCTO_CON_PEDIDOS",
					"Este producto ya tiene pedidos, así que no se borra: desactívalo para que deje de verse en la Tienda.");
		}
		imagenes.deleteById(id);
		productos.delete(producto);
	}

	@Transactional
	Producto guardarImagen(Long id, byte[] datos) {
		Producto producto = obtener(id);
		ValidadorImagen.Imagen lista = ValidadorImagen.preparar(datos);
		imagenes.findById(id).ifPresentOrElse(imagen -> imagen.reemplazar(lista.tipo(), lista.datos()),
				() -> imagenes.save(new ImagenProducto(id, lista.tipo(), lista.datos())));
		producto.imagenCambiada(reloj.instant());
		return producto;
	}

	@Transactional(readOnly = true)
	Optional<ImagenProducto> imagen(Long id) {
		return imagenes.findById(id);
	}

	/**
	 * La categoría debe existir. Debe estar activa, salvo que el producto ya estuviera en ella.
	 */
	private Categoria categoriaPara(DatosProducto datos, Producto actual) {
		Categoria categoria = categorias.findById(datos.categoriaId())
			.orElseThrow(() -> new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "CATEGORIA_NO_ENCONTRADA",
					"La categoría elegida no existe."));
		boolean sigueEnLaMisma = actual != null && actual.getCategoria().getId().equals(categoria.getId());
		if (!categoria.isActiva() && !sigueEnLaMisma) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "CATEGORIA_INACTIVA",
					"La categoría elegida está desactivada. Actívala o elige otra.");
		}
		return categoria;
	}

	private static List<OpcionPersonalizacion> opciones(DatosProducto datos) {
		List<DatosProducto.DatosOpcion> opciones = datos.opcionesOVacio();
		if (datos.personalizable() && opciones.isEmpty()) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "OPCIONES_REQUERIDAS",
					"Un producto personalizable necesita al menos una opción.");
		}
		if (!datos.personalizable() && !opciones.isEmpty()) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "PRODUCTO_NO_PERSONALIZABLE",
					"Marca el producto como personalizable para agregarle opciones.");
		}
		return opciones.stream().map(o -> new OpcionPersonalizacion(o.tipo(), o.nombre(), o.recargo(), o.factorOUno())).toList();
	}

	private static String escaparComodines(String texto) {
		return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static ReglaNegocioException noEncontrado() {
		return new ReglaNegocioException(HttpStatus.NOT_FOUND, "PRODUCTO_NO_ENCONTRADO", "No existe ese producto.");
	}

}
