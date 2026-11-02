package bo.dulcesaromas.seguridad.acceso;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Propiedades {@code seguridad.*} de application.properties.
 */
@ConfigurationProperties("seguridad")
public record PropiedadesSeguridad(@DefaultValue Sesion sesion, @DefaultValue Bloqueo bloqueo,
		@DefaultValue AdministradorInicial administradorInicial) {

	/**
	 * Tiempo sin uso tras el cual se cierra la sesión: 30 minutos en el Panel (RNF06).
	 */
	public record Sesion(@DefaultValue("30m") Duration inactividadPersonal,
			@DefaultValue("30d") Duration inactividadCliente) {
	}

	/**
	 * Intentos fallidos seguidos que bloquean la cuenta, y por cuánto tiempo (RNF05).
	 */
	public record Bloqueo(@DefaultValue("5") int intentos, @DefaultValue("15m") Duration duracion) {
	}

	/**
	 * Cuenta que se crea al arrancar si no hay ningún administrador activo. Vacía, no se crea nada.
	 */
	public record AdministradorInicial(@DefaultValue("Administrador") String nombre, String correo,
			String contrasena) {
	}

}
