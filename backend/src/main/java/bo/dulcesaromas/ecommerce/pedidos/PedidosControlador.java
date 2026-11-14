package bo.dulcesaromas.ecommerce.pedidos;

import java.time.LocalDate;
import java.util.List;

import bo.dulcesaromas.ecommerce.EstadoPedido;
import bo.dulcesaromas.seguridad.IAutenticacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Carrito y pedido de la Tienda (CU05, HU10), Mis pedidos (CU15, HU19) y preparación en el Panel (CU08, HU13).
 */
@RestController
@RequestMapping("/api/pedidos")
class PedidosControlador {

	private final PedidoServicio servicio;

	private final IAutenticacion autenticacion;

	PedidosControlador(PedidoServicio servicio, IAutenticacion autenticacion) {
		this.servicio = servicio;
		this.autenticacion = autenticacion;
	}

	/**
	 * Total y disponibilidad del carrito. No exige sesión, para mostrar el total antes de iniciarla.
	 */
	@PostMapping("/cotizacion")
	PedidoVista.CotizacionVista cotizar(@Valid @RequestBody SolicitudPedido solicitud) {
		return servicio.cotizar(solicitud);
	}

	/**
	 * Hacer un pedido exige cuenta de cliente (RN15).
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('CLIENTE')")
	PedidoVista crear(@Valid @RequestBody SolicitudPedido solicitud) {
		return servicio.crear(autenticacion.usuarioActual(), solicitud);
	}

	@GetMapping("/mios")
	@PreAuthorize("hasAuthority('CLIENTE')")
	List<PedidoVista.Resumen> mios() {
		return servicio.deCliente(autenticacion.usuarioActual().id());
	}

	/**
	 * @param desde y {@code hasta}: días de creación en formato aaaa-mm-dd, incluidos
	 */
	@GetMapping
	// Con solo el permiso de preparar (el Repostero) no se ve esta lista, que trae nombres y montos: se usa Cocina.
	@PreAuthorize("hasAnyAuthority('PEDIDOS_CONSULTAR', 'DISTRIBUCION_ASIGNAR')")
	List<PedidoVista.Resumen> delPanel(@RequestParam(required = false) EstadoPedido estado,
			@RequestParam(required = false) LocalDate desde, @RequestParam(required = false) LocalDate hasta) {
		return servicio.delPanel(estado, desde, hasta);
	}

	/**
	 * Pedido asistido, a nombre de un cliente (CU17, HU21).
	 */
	@PostMapping("/asistidos")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('PEDIDOS_CONSULTAR')")
	PedidoVista crearAsistido(@Valid @RequestBody PedidoAsistido datos) {
		return servicio.crearAsistido(autenticacion.usuarioActual(), datos.clienteId(), datos.pedido());
	}

	/**
	 * El cliente cancela los suyos; el personal necesita el permiso de cancelar (CU16, HU20).
	 */
	@PostMapping("/{id}/cancelar")
	PedidoServicio.Cancelacion cancelar(@PathVariable Long id, @Valid @RequestBody Motivo motivo) {
		return servicio.cancelar(id, motivo.motivo(), autenticacion.usuarioActual());
	}

	@GetMapping("/{id}")
	PedidoVista obtener(@PathVariable Long id) {
		return servicio.obtener(id, autenticacion.usuarioActual());
	}

	/**
	 * Quién prepara depende del permiso del rol (D3).
	 */
	// Devuelve el pedido completo, así que además del permiso de preparar pide el de consultar pedidos. El Repostero
	// marca la preparación desde Cocina, que muestra solo lo necesario (RNF16).
	@PostMapping("/{id}/en-preparacion")
	@PreAuthorize("hasAuthority('PEDIDOS_PREPARAR') and hasAuthority('PEDIDOS_CONSULTAR')")
	PedidoVista enPreparacion(@PathVariable Long id) {
		return servicio.avanzarPreparacion(id, EstadoPedido.EN_PREPARACION, autenticacion.usuarioActual());
	}

	@PostMapping("/{id}/listo")
	@PreAuthorize("hasAuthority('PEDIDOS_PREPARAR') and hasAuthority('PEDIDOS_CONSULTAR')")
	PedidoVista listo(@PathVariable Long id) {
		return servicio.avanzarPreparacion(id, EstadoPedido.LISTO, autenticacion.usuarioActual());
	}

	/**
	 * El cliente retiró su pedido en tienda. Usa el permiso de asignar repartidor: entregar en mostrador es la
	 * alternativa a mandarlo con un repartidor.
	 */
	@PostMapping("/{id}/retirado")
	@PreAuthorize("hasAuthority('DISTRIBUCION_ASIGNAR')")
	PedidoVista retirado(@PathVariable Long id) {
		return servicio.entregarRecojo(id, autenticacion.usuarioActual());
	}

	record PedidoAsistido(@NotNull(message = "Elige al cliente.") Long clienteId,
			@NotNull(message = "Arma el pedido.") @Valid SolicitudPedido pedido) {
	}

	record Motivo(@NotBlank(message = "Indica el motivo de la cancelación.") @Size(max = 255, message = "Usa como máximo 255 caracteres.") String motivo) {
	}

}
