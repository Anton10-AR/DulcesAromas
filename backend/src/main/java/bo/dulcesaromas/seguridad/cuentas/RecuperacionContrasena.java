package bo.dulcesaromas.seguridad.cuentas;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Enlace de un solo uso para definir una contraseña nueva (CU12, D5).
 */
@Entity
@Table(name = "recuperacion_contrasena")
class RecuperacionContrasena {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long usuarioId;

	private String tokenHash;

	private Instant expira;

	private boolean usada;

	protected RecuperacionContrasena() {
	}

	RecuperacionContrasena(Long usuarioId, String tokenHash, Instant expira) {
		this.usuarioId = usuarioId;
		this.tokenHash = tokenHash;
		this.expira = expira;
	}

	boolean vigente(Instant ahora) {
		return !usada && ahora.isBefore(expira);
	}

	void usar() {
		usada = true;
	}

	Long getUsuarioId() {
		return usuarioId;
	}

}
