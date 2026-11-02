package bo.dulcesaromas.ecommerce.catalogo;

import java.io.IOException;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Productos en el Panel (HU08). El administrador los gestiona; el Vendedor solo cambia la disponibilidad.
 */
@RestController
@RequestMapping("/api/productos")
class ProductosControlador {

	private final ProductoServicio servicio;

	ProductosControlador(ProductoServicio servicio) {
		this.servicio = servicio;
	}

	/**
	 * Todos los productos, también los desactivados.
	 */
	@GetMapping
	@PreAuthorize("hasAnyAuthority('CATALOGO_GESTIONAR', 'CATALOGO_DISPONIBILIDAD')")
	List<ProductoVista> listar(@RequestParam(required = false) String buscar,
			@RequestParam(required = false) Long categoria) {
		return servicio.buscar(buscar, categoria, false).stream().map(ProductoVista::de).toList();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyAuthority('CATALOGO_GESTIONAR', 'CATALOGO_DISPONIBILIDAD')")
	ProductoVista obtener(@PathVariable Long id) {
		return ProductoVista.de(servicio.obtener(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('CATALOGO_GESTIONAR')")
	ProductoVista crear(@Valid @RequestBody DatosProducto datos) {
		return ProductoVista.de(servicio.crear(datos));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('CATALOGO_GESTIONAR')")
	ProductoVista editar(@PathVariable Long id, @Valid @RequestBody DatosProducto datos) {
		return ProductoVista.de(servicio.editar(id, datos));
	}

	@PutMapping("/{id}/disponibilidad")
	@PreAuthorize("hasAuthority('CATALOGO_DISPONIBILIDAD')")
	ProductoVista cambiarDisponibilidad(@PathVariable Long id, @Valid @RequestBody Disponibilidad cambio) {
		return ProductoVista.de(servicio.cambiarDisponibilidad(id, cambio.disponible()));
	}

	@PostMapping("/{id}/activar")
	@PreAuthorize("hasAuthority('CATALOGO_GESTIONAR')")
	ProductoVista activar(@PathVariable Long id) {
		return ProductoVista.de(servicio.activar(id));
	}

	@PostMapping("/{id}/desactivar")
	@PreAuthorize("hasAuthority('CATALOGO_GESTIONAR')")
	ProductoVista desactivar(@PathVariable Long id) {
		return ProductoVista.de(servicio.desactivar(id));
	}

	/**
	 * Solo si el producto nunca se pidió; si tiene pedidos, se desactiva (RN10).
	 */
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('CATALOGO_GESTIONAR')")
	ResponseEntity<Void> borrar(@PathVariable Long id) {
		servicio.borrar(id);
		return ResponseEntity.noContent().build();
	}

	@PutMapping(path = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('CATALOGO_GESTIONAR')")
	ProductoVista cambiarImagen(@PathVariable Long id, @RequestParam MultipartFile archivo) throws IOException {
		return ProductoVista.de(servicio.guardarImagen(id, archivo.getBytes()));
	}

	record Disponibilidad(@NotNull(message = "Indica si está disponible.") Boolean disponible) {
	}

}
