package bo.dulcesaromas.seguridad.acceso;

import java.time.Instant;

import bo.dulcesaromas.seguridad.cuentas.Usuario;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Sesión abierta. Solo se guarda el hash del token, así que una copia de la base no permite suplantar a nadie.
 */
@Entity
@Table(name = "sesion")
class Sesion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String tokenHash;

	@ManyToOne
	@JoinColumn(name = "usuario_id")
	private Usuario usuario;

	private Instant creada;

	private Instant ultimoUso;

	protected Sesion() {
	}

	Sesion(String tokenHash, Usuario usuario, Instant ahora) {
		this.tokenHash = tokenHash;
		this.usuario = usuario;
		this.creada = ahora;
		this.ultimoUso = ahora;
	}

	void registrarUso(Instant ahora) {
		ultimoUso = ahora;
	}

	Usuario getUsuario() {
		return usuario;
	}

	Instant getUltimoUso() {
		return ultimoUso;
	}

}
