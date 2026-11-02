package bo.dulcesaromas.distribucion.entregas;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una asignación o cambio de repartidor (CU08, alt. 4).
 */
@Entity
@Table(name = "asignacion_entrega")
class AsignacionEntrega {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long repartidorId;

	private Long asignadoPor;

	private Instant fecha;

	protected AsignacionEntrega() {
	}

	AsignacionEntrega(Long repartidorId, Long asignadoPor, Instant fecha) {
		this.repartidorId = repartidorId;
		this.asignadoPor = asignadoPor;
		this.fecha = fecha;
	}

	Long getRepartidorId() {
		return repartidorId;
	}

	Long getAsignadoPor() {
		return asignadoPor;
	}

	Instant getFecha() {
		return fecha;
	}

}
