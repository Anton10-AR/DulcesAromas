package bo.dulcesaromas.seguridad.acceso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import bo.dulcesaromas.seguridad.UsuarioActual;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lee el token de {@code Authorization: Bearer} y deja al usuario en el contexto de seguridad. Sus autoridades
 * son los permisos de su rol, leídos en cada solicitud, más su tipo ({@code CLIENTE} o {@code PERSONAL}).
 */
class FiltroToken extends OncePerRequestFilter {

	private final ServicioSesiones sesiones;

	FiltroToken(ServicioSesiones sesiones) {
		this.sesiones = sesiones;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest solicitud, HttpServletResponse respuesta, FilterChain cadena)
			throws ServletException, IOException {
		Tokens.deSolicitud(solicitud).flatMap(sesiones::autenticar).ifPresent(usuario -> {
			var autenticacion = new UsernamePasswordAuthenticationToken(usuario, null, autoridades(usuario));
			SecurityContextHolder.getContext().setAuthentication(autenticacion);
		});
		cadena.doFilter(solicitud, respuesta);
	}

	private static List<GrantedAuthority> autoridades(UsuarioActual usuario) {
		List<GrantedAuthority> autoridades = new ArrayList<>();
		autoridades.add(new SimpleGrantedAuthority(usuario.tipo().name()));
		usuario.permisos().forEach(p -> autoridades.add(new SimpleGrantedAuthority(p.name())));
		return autoridades;
	}

}
