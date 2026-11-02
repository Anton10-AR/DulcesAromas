package bo.dulcesaromas.ecommerce.catalogo;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Categorías en el Panel (HU18).
 */
@RestController
@RequestMapping("/api/categorias")
class CategoriasControlador {

	private final CategoriaServicio servicio;

	CategoriasControlador(CategoriaServicio servicio) {
		this.servicio = servicio;
	}

	/**
	 * Todas las categorías, con cuántos productos activos tiene cada una.
	 */
	@GetMapping
	@PreAuthorize("hasAnyAuthority('CATALOGO_GESTIONAR', 'CATALOGO_DISPONIBILIDAD')")
	List<CategoriaVista> listar() {
		Map<Long, Long> conteo = servicio.productosActivosPorCategoria();
		return servicio.listar().stream().map(c -> CategoriaVista.de(c, conteo.getOrDefault(c.getId(), 0L))).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('CATALOGO_GESTIONAR')")
	CategoriaVista crear(@Valid @RequestBody DatosCategoria datos) {
		return CategoriaVista.de(servicio.crear(datos.nombre()), 0);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('CATALOGO_GESTIONAR')")
	CategoriaVista renombrar(@PathVariable Long id, @Valid @RequestBody DatosCategoria datos) {
		return vista(servicio.renombrar(id, datos.nombre()));
	}

	@PostMapping("/{id}/activar")
	@PreAuthorize("hasAuthority('CATALOGO_GESTIONAR')")
	CategoriaVista activar(@PathVariable Long id) {
		return vista(servicio.activar(id));
	}

	/**
	 * Si la categoría tiene productos activos, responde 409 con cuántos afecta; el Panel pide confirmación y
	 * repite la solicitud con {@code confirmar=true}.
	 */
	@PostMapping("/{id}/desactivar")
	@PreAuthorize("hasAuthority('CATALOGO_GESTIONAR')")
	CategoriaVista desactivar(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean confirmar) {
		return vista(servicio.desactivar(id, confirmar));
	}

	private CategoriaVista vista(Categoria categoria) {
		return CategoriaVista.de(categoria, servicio.productosActivosPorCategoria().getOrDefault(categoria.getId(), 0L));
	}

	record DatosCategoria(@NotBlank(message = "Escribe el nombre.") @Size(max = 60, message = "Usa como máximo 60 caracteres.") String nombre) {
	}

	record CategoriaVista(Long id, String nombre, boolean activa, long productosActivos) {

		static CategoriaVista de(Categoria c, long productosActivos) {
			return new CategoriaVista(c.getId(), c.getNombre(), c.isActiva(), productosActivos);
		}

	}

}
