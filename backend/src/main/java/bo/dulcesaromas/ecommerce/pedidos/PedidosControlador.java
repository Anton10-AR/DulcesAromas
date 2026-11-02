package bo.dulcesaromas.ecommerce.pedidos;

import java.util.List;

import bo.dulcesaromas.ecommerce.EstadoPedido;
import bo.dulcesaromas.seguridad.IAutenticacion;
import jakarta.validation.Valid;
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

	@GetMapping
	@PreAuthorize("hasAnyAuthority('PEDIDOS_CONSULTAR', 'PEDIDOS_PREPARAR', 'DISTRIBUCION_ASIGNAR')")
	List<PedidoVista.Resumen> delPanel(@RequestParam(required = false) EstadoPedido estado) {
		return servicio.delPanel(estado);
	}

	@GetMapping("/{id}")
	PedidoVista obtener(@PathVariable Long id) {
		return servicio.obtener(id, autenticacion.usuarioActual());
	}

	/**
	 * Quién prepara depende del permiso del rol (D3).
	 */
	@PostMapping("/{id}/en-preparacion")
	@PreAuthorize("hasAuthority('PEDIDOS_PREPARAR')")
	PedidoVista enPreparacion(@PathVariable Long id) {
		return servicio.avanzarPreparacion(id, EstadoPedido.EN_PREPARACION, autenticacion.usuarioActual());
	}

	@PostMapping("/{id}/listo")
	@PreAuthorize("hasAuthority('PEDIDOS_PREPARAR')")
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

}
