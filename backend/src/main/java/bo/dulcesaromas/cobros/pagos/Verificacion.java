package bo.dulcesaromas.cobros.pagos;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Aprobación o rechazo de un pago, con quién lo decidió y cuándo (RF39). Es también el registro de auditoría
 * de las verificaciones (RNF08).
 */
@Entity
@Table(name = "verificacion")
class Verificacion {

	enum Decision {
		APROBADO, RECHAZADO
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long pagoId;

	@Enumerated(EnumType.STRING)
	private Decision decision;

	private String observacion;

	private Long verificadorId;

	private Instant fecha;

	protected Verificacion() {
	}

	Verificacion(Long pagoId, Decision decision, String observacion, Long verificadorId, Instant fecha) {
		this.pagoId = pagoId;
		this.decision = decision;
		this.observacion = observacion;
		this.verificadorId = verificadorId;
		this.fecha = fecha;
	}

	Long getPagoId() {
		return pagoId;
	}

	Decision getDecision() {
		return decision;
	}

	String getObservacion() {
		return observacion;
	}

	Long getVerificadorId() {
		return verificadorId;
	}

	Instant getFecha() {
		return fecha;
	}

}
