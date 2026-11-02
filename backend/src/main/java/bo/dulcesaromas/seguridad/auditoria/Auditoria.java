package bo.dulcesaromas.seguridad.auditoria;

import java.time.Instant;

import bo.dulcesaromas.seguridad.cuentas.Usuario;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Quién hizo qué y cuándo. {@code usuario} es quien actuó; es nulo si lo hizo el propio sistema.
 */
@Entity
@Table(name = "auditoria")
public class Auditoria {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id")
	private Usuario usuario;

	@Enumerated(EnumType.STRING)
	private AccionAuditada accion;

	private String detalle;

	private Instant fecha;

	protected Auditoria() {
	}

	Auditoria(Usuario usuario, AccionAuditada accion, String detalle, Instant fecha) {
		this.usuario = usuario;
		this.accion = accion;
		this.detalle = detalle;
		this.fecha = fecha;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public AccionAuditada getAccion() {
		return accion;
	}

	public String getDetalle() {
		return detalle;
	}

	public Instant getFecha() {
		return fecha;
	}

}
