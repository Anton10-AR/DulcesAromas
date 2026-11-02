package bo.dulcesaromas.seguridad.acceso;

import bo.dulcesaromas.seguridad.UsuarioActual;

/**
 * Respuesta al iniciar sesión o registrarse. La interfaz manda el token en {@code Authorization: Bearer}
 * y usa {@code usuario.tipo} para abrir la Tienda o el Panel, y {@code usuario.permisos} para armar el menú.
 */
public record SesionIniciada(String token, UsuarioActual usuario) {
}
