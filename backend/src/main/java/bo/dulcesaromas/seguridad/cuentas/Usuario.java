package bo.dulcesaromas.seguridad.cuentas;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

import bo.dulcesaromas.seguridad.Permiso;
import bo.dulcesaromas.seguridad.TipoUsuario;
import bo.dulcesaromas.seguridad.UsuarioActual;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * Credenciales, estado de la cuenta y bloqueo por intentos fallidos. Una cuenta nunca se borra:
 * se desactiva y conserva su historial (RN02).
 */
@Entity
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo")
public abstract class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;

	private String correo;

	private String telefono;

	private String contrasenaHash;

	private boolean activo;

	private int intentosFallidos;

	private Instant bloqueadoHasta;

	/** El administrador restableció la contraseña; el usuario debe cambiarla al ingresar (CU12). */
	private boolean debeCambiarContrasena;

	private Instant creado;

	protected Usuario() {
	}

	protected Usuario(String nombre, String correo, String telefono, String contrasenaHash, Instant creado) {
		this.nombre = nombre.strip();
		this.correo = normalizarCorreo(correo);
		this.telefono = telefono;
		this.contrasenaHash = contrasenaHash;
		this.activo = true;
		this.creado = creado;
	}

	/**
	 * El correo se compara sin mayúsculas ni espacios, para que RN01 no se evada escribiéndolo distinto.
	 */
	public static String normalizarCorreo(String correo) {
		return correo.strip().toLowerCase(Locale.ROOT);
	}

	public abstract TipoUsuario getTipo();

	public abstract Set<Permiso> permisos();

	public UsuarioActual comoUsuarioActual() {
		return new UsuarioActual(id, nombre, correo, getTipo(), null, permisos());
	}

	public boolean estaBloqueado(Instant ahora) {
		return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
	}

	/**
	 * Cuenta un intento fallido. Al llegar a {@code maximo} intentos seguidos bloquea la cuenta.
	 * @return si la cuenta quedó bloqueada con este intento
	 */
	public boolean registrarIntentoFallido(Instant ahora, int maximo, Duration duracionBloqueo) {
		intentosFallidos++;
		if (intentosFallidos < maximo) {
			return false;
		}
		intentosFallidos = 0;
		bloqueadoHasta = ahora.plus(duracionBloqueo);
		return true;
	}

	public void registrarIngreso() {
		intentosFallidos = 0;
		bloqueadoHasta = null;
	}

	/**
	 * La contraseña nueva la eligió el propio usuario: deja de exigirse el cambio y se levanta el bloqueo.
	 */
	public void cambiarContrasena(String contrasenaHash) {
		this.contrasenaHash = contrasenaHash;
		this.debeCambiarContrasena = false;
		registrarIngreso();
	}

	/**
	 * La contraseña la puso el administrador: el usuario debe cambiarla al ingresar (CU12).
	 */
	public void restablecerContrasena(String contrasenaHash) {
		cambiarContrasena(contrasenaHash);
		this.debeCambiarContrasena = true;
	}

	/**
	 * Datos que el usuario edita en Mi perfil. El correo no se cambia desde ahí (CU13).
	 */
	public void editarPerfil(String nombre, String telefono) {
		this.nombre = nombre.strip();
		this.telefono = telefono;
	}

	protected void cambiarDatos(String nombre, String correo, String telefono) {
		this.nombre = nombre.strip();
		this.correo = normalizarCorreo(correo);
		this.telefono = telefono;
	}

	public void activar() {
		activo = true;
	}

	public void desactivar() {
		activo = false;
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public String getCorreo() {
		return correo;
	}

	public String getTelefono() {
		return telefono;
	}

	public String getContrasenaHash() {
		return contrasenaHash;
	}

	public boolean isActivo() {
		return activo;
	}

	public Instant getBloqueadoHasta() {
		return bloqueadoHasta;
	}

	public boolean isDebeCambiarContrasena() {
		return debeCambiarContrasena;
	}

	public Instant getCreado() {
		return creado;
	}

}
