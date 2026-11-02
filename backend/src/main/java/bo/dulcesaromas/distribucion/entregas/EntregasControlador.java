package bo.dulcesaromas.distribucion.entregas;

import java.util.List;

import bo.dulcesaromas.seguridad.IAutenticacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entregas de los pedidos con delivery (CU08, HU13).
 */
@RestController
@RequestMapping("/api/entregas")
class EntregasControlador {

	private final EntregaServicio servicio;

	private final IAutenticacion autenticacion;

	EntregasControlador(EntregaServicio servicio, IAutenticacion autenticacion) {
		this.servicio = servicio;
		this.autenticacion = autenticacion;
	}

	@GetMapping("/repartidores")
	@PreAuthorize("hasAuthority('DISTRIBUCION_ASIGNAR')")
	List<IAutenticacion.UsuarioResumen> repartidores() {
		return servicio.repartidores();
	}

	@GetMapping
	@PreAuthorize("hasAuthority('DISTRIBUCION_ASIGNAR')")
	List<EntregaVista> abiertas() {
		return servicio.abiertas();
	}

	@GetMapping("/mias")
	@PreAuthorize("hasAuthority('DISTRIBUCION_ENTREGAS')")
	List<EntregaVista> mias() {
		return servicio.de(autenticacion.usuarioActual());
	}

	@GetMapping("/{pedidoId}")
	@PreAuthorize("hasAnyAuthority('DISTRIBUCION_ASIGNAR', 'DISTRIBUCION_ENTREGAS')")
	EntregaVista obtener(@PathVariable Long pedidoId) {
		return servicio.obtener(pedidoId, autenticacion.usuarioActual());
	}

	/**
	 * Asigna el repartidor o lo cambia, hasta que el pedido sale.
	 */
	@PutMapping("/{pedidoId}/repartidor")
	@PreAuthorize("hasAuthority('DISTRIBUCION_ASIGNAR')")
	EntregaVista asignar(@PathVariable Long pedidoId, @Valid @RequestBody Asignacion asignacion) {
		return servicio.asignar(pedidoId, asignacion.repartidorId(), autenticacion.usuarioActual());
	}

	/**
	 * Según la tabla 2.2, el pedido sale En camino de manos del Vendedor o del Repartidor.
	 */
	@PostMapping("/{pedidoId}/en-camino")
	@PreAuthorize("hasAnyAuthority('DISTRIBUCION_ENTREGAS', 'DISTRIBUCION_ASIGNAR')")
	EntregaVista enCamino(@PathVariable Long pedidoId) {
		return servicio.marcarEnCamino(pedidoId, autenticacion.usuarioActual());
	}

	@PostMapping("/{pedidoId}/entregado")
	@PreAuthorize("hasAuthority('DISTRIBUCION_ENTREGAS')")
	EntregaVista entregado(@PathVariable Long pedidoId) {
		return servicio.marcarEntregada(pedidoId, autenticacion.usuarioActual());
	}

	@PostMapping("/{pedidoId}/fallida")
	@PreAuthorize("hasAuthority('DISTRIBUCION_ENTREGAS')")
	EntregaVista fallida(@PathVariable Long pedidoId, @Valid @RequestBody Fallo fallo) {
		return servicio.registrarFallo(pedidoId, fallo.observacion(), autenticacion.usuarioActual());
	}

	record Asignacion(@NotNull(message = "Elige un repartidor.") Long repartidorId) {
	}

	/**
	 * RN13: una entrega fallida exige observación.
	 */
	record Fallo(@NotBlank(message = "Explica por qué no se pudo entregar.") @Size(max = 500, message = "Usa como máximo 500 caracteres.") String observacion) {
	}

}
