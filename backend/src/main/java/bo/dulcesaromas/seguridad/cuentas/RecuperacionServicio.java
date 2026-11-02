package bo.dulcesaromas.seguridad.cuentas;

import java.time.Clock;
import java.time.Instant;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.seguridad.TipoUsuario;
import bo.dulcesaromas.seguridad.acceso.PropiedadesSeguridad;
import bo.dulcesaromas.seguridad.acceso.ServicioSesiones;
import bo.dulcesaromas.seguridad.acceso.Tokens;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El cliente recupera su contraseña con un enlace por correo de un solo uso (CU12, D5). Al personal se la
 * restablece el administrador.
 */
@Service
class RecuperacionServicio {

	private final UsuarioRepositorio usuarios;

	private final RecuperacionContrasenaRepositorio recuperaciones;

	private final EnviadorCorreo correo;

	private final PasswordEncoder codificador;

	private final ServicioSesiones sesiones;

	private final PropiedadesSeguridad propiedades;

	private final Clock reloj;

	RecuperacionServicio(UsuarioRepositorio usuarios, RecuperacionContrasenaRepositorio recuperaciones,
			EnviadorCorreo correo, PasswordEncoder codificador, ServicioSesiones sesiones,
			PropiedadesSeguridad propiedades, Clock reloj) {
		this.usuarios = usuarios;
		this.recuperaciones = recuperaciones;
		this.correo = correo;
		this.codificador = codificador;
		this.sesiones = sesiones;
		this.propiedades = propiedades;
		this.reloj = reloj;
	}

	/**
	 * Solo un cliente activo recibe el enlace. Quien llama responde siempre lo mismo, exista o no el correo.
	 */
	@Transactional
	void solicitar(String correoIngresado) {
		usuarios.findByCorreo(Usuario.normalizarCorreo(correoIngresado))
			.filter(u -> u.getTipo() == TipoUsuario.CLIENTE && u.isActivo())
			.ifPresent(this::enviarEnlace);
	}

	/**
	 * Guarda la contraseña nueva, anula el enlace y cierra las sesiones anteriores.
	 */
	@Transactional
	void confirmar(String token, String contrasenaNueva) {
		Instant ahora = reloj.instant();
		RecuperacionContrasena recuperacion = recuperaciones.bloquear(Tokens.hash(token))
			.filter(r -> r.vigente(ahora))
			.orElseThrow(() -> new ReglaNegocioException(HttpStatus.GONE, "ENLACE_VENCIDO",
					"El enlace venció o ya se usó. Solicita otro."));
		Usuario usuario = usuarios.findById(recuperacion.getUsuarioId()).orElseThrow();
		usuario.cambiarContrasena(codificador.encode(contrasenaNueva));
		recuperacion.usar();
		recuperaciones.anularDe(usuario.getId());
		sesiones.cerrarSesionesDe(usuario.getId());
	}

	private void enviarEnlace(Usuario usuario) {
		String token = Tokens.nuevo();
		var recuperacion = propiedades.recuperacion();
		recuperaciones.save(new RecuperacionContrasena(usuario.getId(), Tokens.hash(token),
				reloj.instant().plus(recuperacion.vigencia())));
		long minutos = recuperacion.vigencia().toMinutes();
		correo.enviar(usuario.getCorreo(), "Recupera tu contraseña de Dulces Aromas", """
				Hola, %s:

				Para definir una contraseña nueva, abre este enlace:
				%s?token=%s

				El enlace sirve una sola vez y vence en %d minutos. Si no pediste cambiar tu contraseña, ignora este correo.
				""".formatted(usuario.getNombre(), recuperacion.enlace(), token, minutos));
	}

}
