package bo.dulcesaromas.ecommerce.pedidos;

import java.util.List;

import bo.dulcesaromas.ecommerce.EstadoPedido;
import bo.dulcesaromas.seguridad.IAutenticacion;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cocina: los pedidos por preparar y los pasos de la preparación (CU08, HU13). Es lo que usa el Repostero, y
 * cualquiera con el permiso de preparar; muestra solo lo necesario para preparar (RNF16).
 */
@RestController
@RequestMapping("/api/cocina")
@PreAuthorize("hasAuthority('PEDIDOS_PREPARAR')")
class CocinaControlador {

	private final PedidoServicio servicio;

	private final IAutenticacion autenticacion;

	CocinaControlador(PedidoServicio servicio, IAutenticacion autenticacion) {
		this.servicio = servicio;
		this.autenticacion = autenticacion;
	}

	/**
	 * Los pedidos Confirmados y En preparación, del horario de entrega más cercano al más lejano.
	 */
	@GetMapping
	List<PedidoCocina> pendientes() {
		return servicio.paraCocina();
	}

	@PostMapping("/{id}/en-preparacion")
	PedidoCocina enPreparacion(@PathVariable Long id) {
		return servicio.avanzarEnCocina(id, EstadoPedido.EN_PREPARACION, autenticacion.usuarioActual());
	}

	@PostMapping("/{id}/listo")
	PedidoCocina listo(@PathVariable Long id) {
		return servicio.avanzarEnCocina(id, EstadoPedido.LISTO, autenticacion.usuarioActual());
	}

}
