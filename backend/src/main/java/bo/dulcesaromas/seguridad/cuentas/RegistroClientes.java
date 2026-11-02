package bo.dulcesaromas.seguridad.cuentas;

import java.time.Clock;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.seguridad.acceso.ServicioSesiones;
import bo.dulcesaromas.seguridad.acceso.SesionIniciada;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro de clientes desde la Tienda (CU11, HU15). La cuenta nace con el tipo Cliente, sin rol (RN16),
 * y queda con la sesión iniciada.
 */
@RestController
@RequestMapping("/api/clientes")
class RegistroClientes {

	private final UsuarioRepositorio usuarios;

	private final PasswordEncoder codificador;

	private final ServicioSesiones sesiones;

	private final Clock reloj;

	RegistroClientes(UsuarioRepositorio usuarios, PasswordEncoder codificador, ServicioSesiones sesiones,
			Clock reloj) {
		this.usuarios = usuarios;
		this.codificador = codificador;
		this.sesiones = sesiones;
		this.reloj = reloj;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	SesionIniciada registrar(@Valid @RequestBody DatosRegistro datos) {
		if (usuarios.existsByCorreo(Usuario.normalizarCorreo(datos.correo()))) {
			// RN01. La Tienda ofrece iniciar sesión o recuperar la contraseña al recibir este código.
			throw new ReglaNegocioException(HttpStatus.CONFLICT, "CORREO_REGISTRADO",
					"Ya existe una cuenta con este correo. Puedes iniciar sesión o recuperar tu contraseña.");
		}
		Cliente cliente = usuarios
			.save(new Cliente(datos.nombre(), datos.correo(), codificador.encode(datos.contrasena()), reloj.instant()));
		return sesiones.abrirSesion(cliente);
	}

	record DatosRegistro(@NotBlank(message = "Escribe tu nombre.") @Size(max = 100, message = "Usa como máximo 100 caracteres.") String nombre,
			@NotBlank(message = "Escribe tu correo.") @Email(message = "Escribe un correo válido, como nombre@ejemplo.com.") @Size(max = 254) String correo,
			@ContrasenaValida String contrasena) {
	}

}
