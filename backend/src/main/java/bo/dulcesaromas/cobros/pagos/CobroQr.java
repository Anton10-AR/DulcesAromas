package bo.dulcesaromas.cobros.pagos;

import java.time.Duration;
import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un cobro por QR: el código secreto que va en el QR, de un solo uso y con vencimiento. Quien lo tiene puede ver el
 * monto y confirmar el pago en la pasarela; por eso no lleva datos del cliente.
 */
@Entity
@Table(name = "cobro_qr")
class CobroQr {

	/** Cuánto vale un QR: después hay que generar otro. */
	static final Duration VIGENCIA = Duration.ofMinutes(15);

	@Id
	private String codigo;

	private Long pagoId;

	private Instant creado;

	private Instant vence;

	private Instant usado;

	protected CobroQr() {
	}

	CobroQr(String codigo, Long pagoId, Instant ahora) {
		this.codigo = codigo;
		this.pagoId = pagoId;
		this.creado = ahora;
		this.vence = ahora.plus(VIGENCIA);
	}

	boolean vencido(Instant ahora) {
		return !ahora.isBefore(vence);
	}

	boolean usado() {
		return usado != null;
	}

	void usar(Instant ahora) {
		usado = ahora;
	}

	String getCodigo() {
		return codigo;
	}

	Long getPagoId() {
		return pagoId;
	}

	Instant getVence() {
		return vence;
	}

}
