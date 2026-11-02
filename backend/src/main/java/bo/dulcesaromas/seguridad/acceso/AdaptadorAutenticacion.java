package bo.dulcesaromas.seguridad.acceso;

import bo.dulcesaromas.seguridad.IAutenticacion;
import bo.dulcesaromas.seguridad.UsuarioActual;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
class AdaptadorAutenticacion implements IAutenticacion {

	@Override
	public UsuarioActual usuarioActual() {
		var autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioActual usuario) {
			return usuario;
		}
		throw new IllegalStateException("La solicitud no tiene sesión");
	}

}
