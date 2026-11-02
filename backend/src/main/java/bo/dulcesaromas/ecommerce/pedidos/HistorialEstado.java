package bo.dulcesaromas.ecommerce.pedidos;

import java.time.Instant;

import bo.dulcesaromas.ecommerce.EstadoPedido;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un cambio de estado del pedido, con quién lo hizo y cuándo (RF43).
 */
@Entity
@Table(name = "historial_estado")
class HistorialEstado {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	private EstadoPedido estadoAnterior;

	@Enumerated(EnumType.STRING)
	private EstadoPedido estadoNuevo;

	private Instant fecha;

	private Long usuarioId;

	private String observacion;

	protected HistorialEstado() {
	}

	HistorialEstado(EstadoPedido estadoAnterior, EstadoPedido estadoNuevo, Instant fecha, Long usuarioId,
			String observacion) {
		this.estadoAnterior = estadoAnterior;
		this.estadoNuevo = estadoNuevo;
		this.fecha = fecha;
		this.usuarioId = usuarioId;
		this.observacion = observacion;
	}

	EstadoPedido getEstadoAnterior() {
		return estadoAnterior;
	}

	EstadoPedido getEstadoNuevo() {
		return estadoNuevo;
	}

	Instant getFecha() {
		return fecha;
	}

	Long getUsuarioId() {
		return usuarioId;
	}

	String getObservacion() {
		return observacion;
	}

}
