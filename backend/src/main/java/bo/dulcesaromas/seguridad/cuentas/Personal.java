package bo.dulcesaromas.seguridad.cuentas;

import java.time.Instant;
import java.util.Set;

import bo.dulcesaromas.seguridad.Permiso;
import bo.dulcesaromas.seguridad.RolBase;
import bo.dulcesaromas.seguridad.TipoUsuario;
import bo.dulcesaromas.seguridad.UsuarioActual;
import bo.dulcesaromas.seguridad.permisos.Rol;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Cuenta del Panel con un solo rol fijo. La crea un administrador (RN16).
 */
@Entity
@DiscriminatorValue("PERSONAL")
public class Personal extends Usuario {

	@ManyToOne
	@JoinColumn(name = "rol")
	private Rol rol;

	protected Personal() {
	}

	public Personal(String nombre, String correo, String telefono, String contrasenaHash, Rol rol, Instant creado) {
		super(nombre, correo, telefono, contrasenaHash, creado);
		this.rol = rol;
	}

	@Override
	public TipoUsuario getTipo() {
		return TipoUsuario.PERSONAL;
	}

	@Override
	public Set<Permiso> permisos() {
		return Set.copyOf(rol.getPermisos());
	}

	@Override
	public UsuarioActual comoUsuarioActual() {
		return new UsuarioActual(getId(), getNombre(), getCorreo(), getTipo(), rol.getCodigo(), permisos());
	}

	public boolean esAdministrador() {
		return rol.getCodigo() == RolBase.ADMINISTRADOR;
	}

	void editar(String nombre, String correo, String telefono, Rol rol) {
		cambiarDatos(nombre, correo, telefono);
		this.rol = rol;
	}

	public Rol getRol() {
		return rol;
	}

}
