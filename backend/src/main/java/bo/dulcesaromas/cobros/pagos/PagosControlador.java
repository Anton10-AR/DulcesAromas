package bo.dulcesaromas.cobros.pagos;

import java.io.IOException;
import java.util.List;

import bo.dulcesaromas.cobros.IPagos;
import bo.dulcesaromas.cobros.PagoVista;
import bo.dulcesaromas.seguridad.IAutenticacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Pagos de los pedidos (HU11, HU12).
 */
@RestController
@RequestMapping("/api/pagos")
class PagosControlador {

	private final IPagos servicio;

	private final IAutenticacion autenticacion;

	PagosControlador(IPagos servicio, IAutenticacion autenticacion) {
		this.servicio = servicio;
		this.autenticacion = autenticacion;
	}

	/**
	 * Todos los pagos de un pedido, del primero al último. El último es el vigente.
	 */
	@GetMapping
	List<PagoVista> dePedido(@RequestParam Long pedido) {
		return servicio.dePedido(pedido, autenticacion.usuarioActual());
	}

	/**
	 * El estado del cobro de cada pedido del cliente, para combinar con Mis pedidos (HU19).
	 */
	@GetMapping("/mios")
	@PreAuthorize("hasAuthority('CLIENTE')")
	List<IPagos.Cobro> mios() {
		return servicio.deCliente(autenticacion.usuarioActual().id());
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAnyAuthority('CLIENTE', 'COBRANZA_REGISTRAR')")
	PagoVista registrar(@Valid @ModelAttribute RegistroPago datos, @RequestPart MultipartFile comprobante)
			throws IOException {
		return servicio.registrar(datos.pedidoId(), datos.metodoPagoId(), datos.referencia(), comprobante.getBytes(),
				autenticacion.usuarioActual());
	}

	@GetMapping("/en-verificacion")
	@PreAuthorize("hasAuthority('COBRANZA_VERIFICAR')")
	List<PagoVista> enVerificacion() {
		return servicio.enVerificacion();
	}

	@GetMapping("/{id}/comprobante")
	ResponseEntity<byte[]> comprobante(@PathVariable Long id) {
		IPagos.ArchivoComprobante archivo = servicio.comprobante(id, autenticacion.usuarioActual());
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(archivo.tipoContenido())).body(archivo.datos());
	}

	@PostMapping("/{id}/aprobar")
	@PreAuthorize("hasAuthority('COBRANZA_VERIFICAR')")
	PagoVista aprobar(@PathVariable Long id) {
		return servicio.aprobar(id, autenticacion.usuarioActual());
	}

	@PostMapping("/{id}/rechazar")
	@PreAuthorize("hasAuthority('COBRANZA_VERIFICAR')")
	PagoVista rechazar(@PathVariable Long id, @Valid @RequestBody Rechazo rechazo) {
		return servicio.rechazar(id, rechazo.observacion(), autenticacion.usuarioActual());
	}

	record RegistroPago(@NotNull(message = "Indica el pedido.") Long pedidoId,
			@NotNull(message = "Elige el método de pago.") Long metodoPagoId,
			@NotBlank(message = "Escribe la referencia o el número de operación.") @Size(max = 60, message = "Usa como máximo 60 caracteres.") String referencia) {
	}

	/**
	 * RN07: rechazar exige una observación.
	 */
	record Rechazo(@NotBlank(message = "Explica por qué rechazas el pago.") @Size(max = 500, message = "Usa como máximo 500 caracteres.") String observacion) {
	}

}
