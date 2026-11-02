package bo.dulcesaromas.cobros.pagos;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * Métodos de pago: el cliente ve los activos con sus datos para pagar (CU06); el administrador los gestiona.
 */
@RestController
@RequestMapping("/api/metodos-pago")
class MetodosPagoControlador {

	private final MetodoPagoServicio servicio;

	MetodosPagoControlador(MetodoPagoServicio servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	List<MetodoVista> activos() {
		return servicio.activos().stream().map(MetodoVista::de).toList();
	}

	@GetMapping("/gestion")
	@PreAuthorize("hasAuthority('CONFIGURACION')")
	List<MetodoVista> todos() {
		return servicio.todos().stream().map(MetodoVista::de).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('CONFIGURACION')")
	MetodoVista crear(@Valid @RequestBody DatosMetodo datos) {
		return MetodoVista.de(servicio.crear(datos.nombre(), datos.instrucciones()));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('CONFIGURACION')")
	MetodoVista editar(@PathVariable Long id, @Valid @RequestBody DatosMetodo datos) {
		return MetodoVista.de(servicio.editar(id, datos.nombre(), datos.instrucciones()));
	}

	@PostMapping("/{id}/activar")
	@PreAuthorize("hasAuthority('CONFIGURACION')")
	MetodoVista activar(@PathVariable Long id) {
		return MetodoVista.de(servicio.activar(id));
	}

	@PostMapping("/{id}/desactivar")
	@PreAuthorize("hasAuthority('CONFIGURACION')")
	MetodoVista desactivar(@PathVariable Long id) {
		return MetodoVista.de(servicio.desactivar(id));
	}

	@PutMapping(path = "/{id}/qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('CONFIGURACION')")
	MetodoVista cambiarQr(@PathVariable Long id, @RequestParam MultipartFile archivo) throws IOException {
		return MetodoVista.de(servicio.guardarQr(id, archivo.getBytes()));
	}

	/**
	 * Público como las imágenes del catálogo. La ruta lleva la versión del QR, así que se guarda en caché.
	 */
	@GetMapping("/{id}/qr")
	ResponseEntity<byte[]> qr(@PathVariable Long id) {
		return servicio.qr(id)
			.map(qr -> ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(qr.tipoContenido()))
				.cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
				.body(qr.datos()))
			.orElse(ResponseEntity.notFound().build());
	}

	record DatosMetodo(@NotBlank(message = "Escribe el nombre.") @Size(max = 40, message = "Usa como máximo 40 caracteres.") String nombre,
			@NotNull(message = "Escribe los datos para pagar, aunque sea vacío.") @Size(max = 1000, message = "Usa como máximo 1000 caracteres.") String instrucciones) {
	}

	record MetodoVista(Long id, String nombre, String instrucciones, boolean activo, String qr) {

		static MetodoVista de(MetodoPago m) {
			String qr = m.getQrActualizado() == null ? null
					: "/api/metodos-pago/" + m.getId() + "/qr?v=" + m.getQrActualizado().toEpochMilli();
			return new MetodoVista(m.getId(), m.getNombre(), m.getInstrucciones(), m.isActivo(), qr);
		}

	}

}
