package bo.dulcesaromas.ecommerce.pedidos;

import bo.dulcesaromas.seguridad.IAutenticacion;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Carrito y pedido de la Tienda (CU05, HU10).
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

	@GetMapping("/{id}")
	PedidoVista obtener(@PathVariable Long id) {
		return servicio.obtener(id, autenticacion.usuarioActual());
	}

}
