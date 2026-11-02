package bo.dulcesaromas.seguridad.acceso;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.seguridad.TipoUsuario;
import bo.dulcesaromas.seguridad.UsuarioActual;
import bo.dulcesaromas.seguridad.auditoria.AccionAuditada;
import bo.dulcesaromas.seguridad.auditoria.Auditor;
import bo.dulcesaromas.seguridad.cuentas.Usuario;
import bo.dulcesaromas.seguridad.cuentas.UsuarioRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicio y cierre de sesión (CU01). La sesión es un token opaco guardado en la base, para poder cerrarla
 * por inactividad, al cerrar sesión o al desactivar la cuenta, y para que un cambio de permisos rija enseguida.
 */
@Service
public class ServicioSesiones {

	/** Una sesión se marca como usada como mucho una vez por minuto, para no escribir en cada solicitud. */
	private static final Duration PRECISION_USO = Duration.ofMinutes(1);

	private final SesionRepositorio sesiones;

	private final UsuarioRepositorio usuarios;

	private final PasswordEncoder codificador;

	private final Auditor auditor;

	private final PropiedadesSeguridad propiedades;

	private final Clock reloj;

	/** Se compara contra este hash cuando el correo no existe, para que la respuesta tarde lo mismo. */
	private final String hashFicticio;

	ServicioSesiones(SesionRepositorio sesiones, UsuarioRepositorio usuarios, PasswordEncoder codificador,
			Auditor auditor, PropiedadesSeguridad propiedades, Clock reloj) {
		this.sesiones = sesiones;
		this.usuarios = usuarios;
		this.codificador = codificador;
		this.auditor = auditor;
		this.propiedades = propiedades;
		this.reloj = reloj;
		this.hashFicticio = codificador.encode(UUID.randomUUID().toString());
	}

	/**
	 * Los fallos no deshacen la transacción: el conteo de intentos y el bloqueo deben quedar guardados.
	 */
	@Transactional(noRollbackFor = ReglaNegocioException.class)
	public SesionIniciada iniciarSesion(String correo, String contrasena) {
		Instant ahora = reloj.instant();
		Optional<Usuario> encontrado = usuarios.findByCorreo(Usuario.normalizarCorreo(correo));
		if (encontrado.isEmpty()) {
			codificador.matches(contrasena, hashFicticio);
			throw credencialesIncorrectas();
		}
		Usuario usuario = encontrado.get();
		if (usuario.estaBloqueado(ahora)) {
			throw cuentaBloqueada(usuario, ahora);
		}
		if (!codificador.matches(contrasena, usuario.getContrasenaHash())) {
			var bloqueo = propiedades.bloqueo();
			if (usuario.registrarIntentoFallido(ahora, bloqueo.intentos(), bloqueo.duracion())) {
				auditor.registrar(usuario.getId(), AccionAuditada.CUENTA_BLOQUEADA,
						"Cuenta " + usuario.getCorreo() + " bloqueada tras " + bloqueo.intentos() + " intentos fallidos.");
				throw cuentaBloqueada(usuario, ahora);
			}
			throw credencialesIncorrectas();
		}
		// RN02. Se revisa después de la contraseña para no revelar a un extraño que la cuenta existe y está desactivada.
		if (!usuario.isActivo()) {
			throw credencialesIncorrectas();
		}
		usuario.registrarIngreso();
		return abrirSesion(usuario);
	}

	@Transactional
	public SesionIniciada abrirSesion(Usuario usuario) {
		String token = Tokens.nuevo();
		sesiones.save(new Sesion(Tokens.hash(token), usuario, reloj.instant()));
		return new SesionIniciada(token, usuario.comoUsuarioActual(), usuario.isDebeCambiarContrasena());
	}

	/**
	 * @return el usuario del token, o vacío si el token no existe, venció por inactividad o la cuenta está desactivada
	 */
	@Transactional
	public Optional<UsuarioActual> autenticar(String token) {
		Optional<Sesion> encontrada = sesiones.findByTokenHash(Tokens.hash(token));
		if (encontrada.isEmpty()) {
			return Optional.empty();
		}
		Sesion sesion = encontrada.get();
		Usuario usuario = sesion.getUsuario();
		Instant ahora = reloj.instant();
		if (!usuario.isActivo() || !ahora.isBefore(sesion.getUltimoUso().plus(inactividadPermitida(usuario)))) {
			sesiones.delete(sesion);
			return Optional.empty();
		}
		if (!ahora.isBefore(sesion.getUltimoUso().plus(PRECISION_USO))) {
			sesion.registrarUso(ahora);
		}
		return Optional.of(usuario.comoUsuarioActual());
	}

	@Transactional
	public void cerrarSesion(String token) {
		sesiones.borrarPorToken(Tokens.hash(token));
	}

	/**
	 * Cierra todas las sesiones del usuario, por ejemplo al desactivar su cuenta.
	 */
	@Transactional
	public void cerrarSesionesDe(Long usuarioId) {
		sesiones.borrarDeUsuario(usuarioId);
	}

	/**
	 * Cierra las demás sesiones del usuario y deja abierta la de este token, por ejemplo al cambiar la contraseña.
	 */
	@Transactional
	public void cerrarOtrasSesiones(Long usuarioId, String tokenActual) {
		sesiones.borrarDeUsuarioSalvo(usuarioId, Tokens.hash(tokenActual));
	}

	@Scheduled(cron = "0 0 * * * *")
	@Transactional
	public void borrarSesionesVencidas() {
		var sesion = propiedades.sesion();
		Duration mayor = sesion.inactividadCliente().compareTo(sesion.inactividadPersonal()) > 0
				? sesion.inactividadCliente() : sesion.inactividadPersonal();
		sesiones.borrarSinUsoDesde(reloj.instant().minus(mayor));
	}

	private Duration inactividadPermitida(Usuario usuario) {
		return usuario.getTipo() == TipoUsuario.PERSONAL ? propiedades.sesion().inactividadPersonal()
				: propiedades.sesion().inactividadCliente();
	}

	private static ReglaNegocioException credencialesIncorrectas() {
		return new ReglaNegocioException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INCORRECTAS",
				"El correo o la contraseña no son correctos.");
	}

	private static ReglaNegocioException cuentaBloqueada(Usuario usuario, Instant ahora) {
		long minutos = Math.max(1, (Duration.between(ahora, usuario.getBloqueadoHasta()).toSeconds() + 59) / 60);
		return new ReglaNegocioException(HttpStatus.LOCKED, "CUENTA_BLOQUEADA",
				"La cuenta está bloqueada por varios intentos fallidos. Intenta de nuevo en " + minutos + " minutos.");
	}

}
