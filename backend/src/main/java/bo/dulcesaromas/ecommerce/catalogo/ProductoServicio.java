package bo.dulcesaromas.ecommerce.catalogo;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.compartido.Textos;
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

	private final Clock reloj;

	ProductoServicio(ProductoRepositorio productos, CategoriaRepositorio categorias, ImagenProductoRepositorio imagenes,
			Clock reloj) {
		this.productos = productos;
		this.categorias = categorias;
		this.imagenes = imagenes;
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
	 * Deja de verse en la Tienda y conserva sus pedidos. No hay forma de borrar un producto (RN10).
	 */
	@Transactional
	Producto desactivar(Long id) {
		Producto producto = obtener(id);
		producto.desactivar();
		return producto;
	}

	@Transactional
	Producto guardarImagen(Long id, byte[] datos) {
		Producto producto = obtener(id);
		String tipo = ValidadorImagen.validar(datos);
		imagenes.findById(id).ifPresentOrElse(imagen -> imagen.reemplazar(tipo, datos),
				() -> imagenes.save(new ImagenProducto(id, tipo, datos)));
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
		return opciones.stream().map(o -> new OpcionPersonalizacion(o.tipo(), o.nombre(), o.recargo())).toList();
	}

	private static String escaparComodines(String texto) {
		return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static ReglaNegocioException noEncontrado() {
		return new ReglaNegocioException(HttpStatus.NOT_FOUND, "PRODUCTO_NO_ENCONTRADO", "No existe ese producto.");
	}

}
