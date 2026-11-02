package bo.dulcesaromas.distribucion.entregas;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * La entrega de un pedido con delivery y su repartidor (RF40). El estado lo lleva el pedido; aquí quedan el
 * repartidor, las fechas y la observación de una entrega fallida.
 */
@Entity
@Table(name = "entrega")
class Entrega {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long pedidoId;

	private Long repartidorId;

	private Instant fechaAsignacion;

	private Instant fechaEntrega;

	private String observacionFallo;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "entrega_id", nullable = false)
	@OrderBy("id")
	private List<AsignacionEntrega> asignaciones = new ArrayList<>();

	protected Entrega() {
	}

	Entrega(Long pedidoId, Long repartidorId, Long asignadoPor, Instant ahora) {
		this.pedidoId = pedidoId;
		asignar(repartidorId, asignadoPor, ahora);
	}

	void asignar(Long repartidorId, Long asignadoPor, Instant ahora) {
		this.repartidorId = repartidorId;
		this.fechaAsignacion = ahora;
		asignaciones.add(new AsignacionEntrega(repartidorId, asignadoPor, ahora));
	}

	void marcarEntregada(Instant ahora) {
		fechaEntrega = ahora;
	}

	void registrarFallo(String observacion) {
		observacionFallo = observacion.strip();
	}

	boolean esDe(Long usuarioId) {
		return repartidorId.equals(usuarioId);
	}

	Long getPedidoId() {
		return pedidoId;
	}

	Long getRepartidorId() {
		return repartidorId;
	}

	Instant getFechaAsignacion() {
		return fechaAsignacion;
	}

	Instant getFechaEntrega() {
		return fechaEntrega;
	}

	String getObservacionFallo() {
		return observacionFallo;
	}

	List<AsignacionEntrega> getAsignaciones() {
		return asignaciones;
	}

}
