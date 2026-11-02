package bo.dulcesaromas.seguridad.acceso;

import bo.dulcesaromas.seguridad.IAutenticacion;
import bo.dulcesaromas.seguridad.UsuarioActual;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Iniciar y cerrar sesión (CU01, HU01, HU02).
 */
@RestController
@RequestMapping("/api/auth")
class AutenticacionControlador {

	private final ServicioSesiones sesiones;

	private final IAutenticacion autenticacion;

	AutenticacionControlador(ServicioSesiones sesiones, IAutenticacion autenticacion) {
		this.sesiones = sesiones;
		this.autenticacion = autenticacion;
	}

	@PostMapping("/login")
	SesionIniciada iniciarSesion(@Valid @RequestBody Credenciales credenciales) {
		return sesiones.iniciarSesion(credenciales.correo(), credenciales.contrasena());
	}

	@PostMapping("/logout")
	ResponseEntity<Void> cerrarSesion(HttpServletRequest solicitud) {
		FiltroToken.token(solicitud).ifPresent(sesiones::cerrarSesion);
		return ResponseEntity.noContent().build();
	}

	/**
	 * El usuario de la sesión con sus permisos actuales, para que la interfaz muestre solo lo permitido (HU06).
	 */
	@GetMapping("/yo")
	UsuarioActual yo() {
		return autenticacion.usuarioActual();
	}

	record Credenciales(@NotBlank(message = "Escribe tu correo.") String correo,
			@NotBlank(message = "Escribe tu contraseña.") String contrasena) {
	}

}
