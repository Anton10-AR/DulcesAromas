package bo.dulcesaromas.seguridad.cuentas;

import java.time.Instant;
import java.util.Set;

import bo.dulcesaromas.seguridad.Permiso;
import bo.dulcesaromas.seguridad.TipoUsuario;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Persona que compra desde la Tienda. No tiene rol ni permisos del Panel (RN16).
 */
@Entity
@DiscriminatorValue("CLIENTE")
public class Cliente extends Usuario {

	private String direccionHabitual;

	protected Cliente() {
	}

	public Cliente(String nombre, String correo, String contrasenaHash, Instant creado) {
		this(nombre, correo, null, contrasenaHash, creado);
	}

	public Cliente(String nombre, String correo, String telefono, String contrasenaHash, Instant creado) {
		super(nombre, correo, telefono, contrasenaHash, creado);
	}

	public void cambiarDireccionHabitual(String direccion) {
		this.direccionHabitual = direccion;
	}

	@Override
	public TipoUsuario getTipo() {
		return TipoUsuario.CLIENTE;
	}

	@Override
	public Set<Permiso> permisos() {
		return Set.of();
	}

	public String getDireccionHabitual() {
		return direccionHabitual;
	}

}
