package bo.dulcesaromas.seguridad.acceso;

import java.io.IOException;

import bo.dulcesaromas.compartido.ErrorRespuesta;
import bo.dulcesaromas.seguridad.UsuarioActual;
import bo.dulcesaromas.seguridad.auditoria.AccionAuditada;
import bo.dulcesaromas.seguridad.auditoria.Auditor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Respuestas a una solicitud sin sesión (401) o sin permiso (403). El acceso denegado queda en la
 * auditoría (RNF07).
 */
@Component
class ManejadorAccesos implements AuthenticationEntryPoint, AccessDeniedHandler {

	private static final Logger log = LoggerFactory.getLogger(ManejadorAccesos.class);

	private final Auditor auditor;

	private final JsonMapper json;

	ManejadorAccesos(Auditor auditor, JsonMapper json) {
		this.auditor = auditor;
		this.json = json;
	}

	@Override
	public void commence(HttpServletRequest solicitud, HttpServletResponse respuesta, AuthenticationException ex)
			throws IOException {
		responder(respuesta, HttpStatus.UNAUTHORIZED, new ErrorRespuesta("SIN_SESION", "Inicia sesión para continuar."));
	}

	@Override
	public void handle(HttpServletRequest solicitud, HttpServletResponse respuesta, AccessDeniedException ex)
			throws IOException {
		String recurso = solicitud.getMethod() + " " + solicitud.getRequestURI();
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Long usuarioId = principal instanceof UsuarioActual usuario ? usuario.id() : null;
		log.warn("Acceso denegado a {} para el usuario {}", recurso, usuarioId);
		auditor.registrar(usuarioId, AccionAuditada.ACCESO_DENEGADO, recurso);
		responder(respuesta, HttpStatus.FORBIDDEN, new ErrorRespuesta("ACCESO_DENEGADO", "Acceso denegado."));
	}

	private void responder(HttpServletResponse respuesta, HttpStatus estado, ErrorRespuesta error) throws IOException {
		respuesta.setStatus(estado.value());
		respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
		respuesta.setCharacterEncoding("UTF-8");
		json.writeValue(respuesta.getOutputStream(), error);
	}

}
