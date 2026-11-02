package bo.dulcesaromas.ecommerce.catalogo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.compartido.Textos;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Categorías del catálogo (CU14, HU18).
 */
@Service
class CategoriaServicio {

	private final CategoriaRepositorio categorias;

	private final ProductoRepositorio productos;

	CategoriaServicio(CategoriaRepositorio categorias, ProductoRepositorio productos) {
		this.categorias = categorias;
		this.productos = productos;
	}

	@Transactional(readOnly = true)
	List<Categoria> listar() {
		return categorias.findAllByOrderByNombreAsc();
	}

	@Transactional(readOnly = true)
	List<Categoria> listarActivas() {
		return categorias.findByActivaTrueOrderByNombreAsc();
	}

	/**
	 * @return cuántos productos activos tiene cada categoría, por id
	 */
	@Transactional(readOnly = true)
	Map<Long, Long> productosActivosPorCategoria() {
		Map<Long, Long> conteo = new HashMap<>();
		productos.contarActivosPorCategoria().forEach(fila -> conteo.put((Long) fila[0], (Long) fila[1]));
		return conteo;
	}

	@Transactional(readOnly = true)
	Categoria obtener(Long id) {
		return categorias.findById(id)
			.orElseThrow(() -> new ReglaNegocioException(HttpStatus.NOT_FOUND, "CATEGORIA_NO_ENCONTRADA",
					"No existe esa categoría."));
	}

	@Transactional
	Categoria crear(String nombre) {
		exigirNombreLibre(nombre, null);
		return categorias.save(new Categoria(nombre));
	}

	@Transactional
	Categoria renombrar(Long id, String nombre) {
		Categoria categoria = obtener(id);
		exigirNombreLibre(nombre, id);
		categoria.renombrar(nombre);
		return categoria;
	}

	@Transactional
	Categoria activar(Long id) {
		Categoria categoria = obtener(id);
		categoria.activar();
		return categoria;
	}

	/**
	 * Sus productos dejan de verse en la Tienda. Si tiene productos activos, el administrador debe confirmar
	 * después de saber cuántos afecta.
	 */
	@Transactional
	Categoria desactivar(Long id, boolean confirmado) {
		Categoria categoria = obtener(id);
		long afectados = productos.countByCategoriaIdAndActivoTrue(id);
		if (afectados > 0 && !confirmado) {
			String productosAfectados = afectados == 1 ? "1 producto activo" : afectados + " productos activos";
			throw new ReglaNegocioException(HttpStatus.CONFLICT, "CONFIRMACION_REQUERIDA", "La categoría tiene "
					+ productosAfectados + " que dejarán de verse en la Tienda. Confirma para desactivarla.");
		}
		categoria.desactivar();
		return categoria;
	}

	private void exigirNombreLibre(String nombre, Long idPropio) {
		String clave = Textos.clave(nombre);
		boolean ocupado = idPropio == null ? categorias.existsByNombreClave(clave)
				: categorias.existsByNombreClaveAndIdNot(clave, idPropio);
		if (ocupado) {
			throw new ReglaNegocioException(HttpStatus.CONFLICT, "CATEGORIA_REPETIDA",
					"Ya existe una categoría con ese nombre.");
		}
	}

}
