package bo.dulcesaromas.seguridad;

import java.util.Set;

/**
 * El usuario de la sesión en curso. {@code rol} es nulo para un cliente y {@code permisos} está vacío.
 */
public record UsuarioActual(Long id, String nombre, String correo, TipoUsuario tipo, RolBase rol,
		Set<Permiso> permisos) {

	public boolean tienePermiso(Permiso permiso) {
		return permisos.contains(permiso);
	}

}
