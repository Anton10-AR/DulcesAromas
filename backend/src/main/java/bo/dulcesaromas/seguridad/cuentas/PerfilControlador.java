package bo.dulcesaromas.seguridad.cuentas;

import java.util.Map;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.seguridad.IAutenticacion;
import bo.dulcesaromas.seguridad.acceso.ServicioSesiones;
import bo.dulcesaromas.seguridad.acceso.Tokens;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mi perfil (CU13, HU17): datos propios y cambio de contraseña. El correo no se cambia desde aquí.
 */
@RestController
@RequestMapping("/api/perfil")
class PerfilControlador {

	private final UsuarioRepositorio usuarios;

	private final IAutenticacion autenticacion;

	private final PasswordEncoder codificador;

	private final ServicioSesiones sesiones;

	PerfilControlador(UsuarioRepositorio usuarios, IAutenticacion autenticacion, PasswordEncoder codificador,
			ServicioSesiones sesiones) {
		this.usuarios = usuarios;
		this.autenticacion = autenticacion;
		this.codificador = codificador;
		this.sesiones = sesiones;
	}

	@GetMapping
	@Transactional(readOnly = true)
	Perfil obtener() {
		return Perfil.de(propio());
	}

	/**
	 * La dirección habitual solo la tiene un cliente; para el personal se ignora.
	 */
	@PutMapping
	@Transactional
	Perfil editar(@Valid @RequestBody DatosPerfil datos) {
		Usuario usuario = propio();
		usuario.editarPerfil(datos.nombre(), vacioANulo(datos.telefono()));
		if (usuario instanceof Cliente cliente) {
			cliente.cambiarDireccionHabitual(vacioANulo(datos.direccionHabitual()));
		}
		return Perfil.de(usuario);
	}

	/**
	 * Exige la contraseña actual. Las demás sesiones del usuario se cierran; esta sigue abierta.
	 */
	@PutMapping("/contrasena")
	@Transactional
	ResponseEntity<Void> cambiarContrasena(@Valid @RequestBody CambioContrasena cambio, HttpServletRequest solicitud) {
		Usuario usuario = propio();
		if (!codificador.matches(cambio.actual(), usuario.getContrasenaHash())) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "CONTRASENA_ACTUAL_INCORRECTA",
					"La contraseña actual no es correcta.", Map.of("actual", "La contraseña actual no es correcta."));
		}
		usuario.cambiarContrasena(codificador.encode(cambio.nueva()));
		Tokens.deSolicitud(solicitud).ifPresent(token -> sesiones.cerrarOtrasSesiones(usuario.getId(), token));
		return ResponseEntity.noContent().build();
	}

	private Usuario propio() {
		return usuarios.findById(autenticacion.usuarioActual().id()).orElseThrow();
	}

	private static String vacioANulo(String texto) {
		return texto == null || texto.isBlank() ? null : texto.strip();
	}

	record DatosPerfil(@NotBlank(message = "Escribe tu nombre.") @Size(max = 100, message = "Usa como máximo 100 caracteres.") String nombre,
			@Pattern(regexp = "|[+0-9 ()-]{7,20}", message = "Escribe un teléfono válido, por ejemplo 70012345.") String telefono,
			@Size(max = 255, message = "Usa como máximo 255 caracteres.") String direccionHabitual) {
	}

	record CambioContrasena(@NotBlank(message = "Escribe tu contraseña actual.") String actual,
			@ContrasenaValida String nueva) {
	}

	record Perfil(String nombre, String correo, String telefono, String direccionHabitual, boolean debeCambiarContrasena) {

		static Perfil de(Usuario u) {
			String direccion = u instanceof Cliente c ? c.getDireccionHabitual() : null;
			return new Perfil(u.getNombre(), u.getCorreo(), u.getTelefono(), direccion, u.isDebeCambiarContrasena());
		}

	}

}
