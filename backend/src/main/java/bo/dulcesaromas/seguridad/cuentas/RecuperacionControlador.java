package bo.dulcesaromas.seguridad.cuentas;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recuperar contraseña (CU12, HU16). No exige sesión.
 */
@RestController
@RequestMapping("/api/auth/recuperacion")
class RecuperacionControlador {

	private final RecuperacionServicio servicio;

	RecuperacionControlador(RecuperacionServicio servicio) {
		this.servicio = servicio;
	}

	/**
	 * El mensaje es igual exista o no el correo, para no revelar qué correos están registrados.
	 */
	@PostMapping
	ResponseEntity<Mensaje> solicitar(@Valid @RequestBody Solicitud solicitud) {
		servicio.solicitar(solicitud.correo());
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(new Mensaje(
				"Si el correo está registrado, te enviamos un enlace para definir una contraseña nueva. Revisa tu bandeja."));
	}

	@PostMapping("/confirmar")
	ResponseEntity<Void> confirmar(@Valid @RequestBody Confirmacion confirmacion) {
		servicio.confirmar(confirmacion.token(), confirmacion.contrasenaNueva());
		return ResponseEntity.noContent().build();
	}

	record Solicitud(@NotBlank(message = "Escribe tu correo.") String correo) {
	}

	record Confirmacion(@NotBlank(message = "Falta el código del enlace.") String token,
			@ContrasenaValida String contrasenaNueva) {
	}

	record Mensaje(String mensaje) {
	}

}
