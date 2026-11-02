package bo.dulcesaromas.seguridad.cuentas;

import java.time.Clock;
import java.util.List;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.seguridad.RolBase;
import bo.dulcesaromas.seguridad.acceso.ServicioSesiones;
import bo.dulcesaromas.seguridad.auditoria.AccionAuditada;
import bo.dulcesaromas.seguridad.auditoria.Auditor;
import bo.dulcesaromas.seguridad.permisos.Rol;
import bo.dulcesaromas.seguridad.permisos.RolRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuentas del personal (CU09, HU03, HU04). Solo opera sobre cuentas de personal: un cliente no aparece
 * aquí y por eso nunca recibe un rol (RN16).
 */
@Service
class PersonalServicio {

	private final PersonalRepositorio personal;

	private final UsuarioRepositorio usuarios;

	private final RolRepositorio roles;

	private final PasswordEncoder codificador;

	private final ServicioSesiones sesiones;

	private final Auditor auditor;

	private final Clock reloj;

	PersonalServicio(PersonalRepositorio personal, UsuarioRepositorio usuarios, RolRepositorio roles,
			PasswordEncoder codificador, ServicioSesiones sesiones, Auditor auditor, Clock reloj) {
		this.personal = personal;
		this.usuarios = usuarios;
		this.roles = roles;
		this.codificador = codificador;
		this.sesiones = sesiones;
		this.auditor = auditor;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	List<Personal> listar() {
		return personal.findAllByOrderByNombreAsc();
	}

	@Transactional(readOnly = true)
	Personal obtener(Long id) {
		return personal.findById(id)
			.orElseThrow(() -> new ReglaNegocioException(HttpStatus.NOT_FOUND, "CUENTA_NO_ENCONTRADA",
					"No existe una cuenta de personal con ese identificador."));
	}

	@Transactional
	Personal crear(String nombre, String correo, String telefono, RolBase rol, String contrasenaInicial,
			Long actorId) {
		exigirCorreoLibre(correo, null);
		Personal cuenta = personal.save(new Personal(nombre, correo, telefono, codificador.encode(contrasenaInicial),
				rol(rol), reloj.instant()));
		auditor.registrar(actorId, AccionAuditada.CUENTA_CREADA,
				"Cuenta " + cuenta.getCorreo() + " creada con el rol " + rol.getNombre() + ".");
		return cuenta;
	}

	@Transactional
	Personal editar(Long id, String nombre, String correo, String telefono, RolBase rol, Long actorId) {
		Personal cuenta = obtener(id);
		exigirCorreoLibre(correo, id);
		RolBase rolAnterior = cuenta.getRol().getCodigo();
		if (rol != rolAnterior) {
			if (id.equals(actorId)) {
				throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "PROPIO_ROL",
						"No puedes cambiar tu propio rol.");
			}
			exigirOtroAdministradorActivo(cuenta);
		}
		cuenta.editar(nombre, correo, telefono, rol(rol));
		auditor.registrar(actorId, AccionAuditada.CUENTA_EDITADA, "Datos de la cuenta " + cuenta.getCorreo() + " editados.");
		if (rol != rolAnterior) {
			auditor.registrar(actorId, AccionAuditada.ROL_CAMBIADO, "Cuenta " + cuenta.getCorreo() + ": rol cambiado de "
					+ rolAnterior.getNombre() + " a " + rol.getNombre() + ".");
		}
		return cuenta;
	}

	@Transactional
	Personal activar(Long id, Long actorId) {
		Personal cuenta = obtener(id);
		if (!cuenta.isActivo()) {
			cuenta.activar();
			auditor.registrar(actorId, AccionAuditada.CUENTA_ACTIVADA, "Cuenta " + cuenta.getCorreo() + " activada.");
		}
		return cuenta;
	}

	/**
	 * La cuenta pierde el acceso enseguida, pero no se borra: conserva su historial (RN02).
	 */
	@Transactional
	Personal desactivar(Long id, Long actorId) {
		if (id.equals(actorId)) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "PROPIA_CUENTA",
					"No puedes desactivar tu propia cuenta.");
		}
		Personal cuenta = obtener(id);
		if (cuenta.isActivo()) {
			exigirOtroAdministradorActivo(cuenta);
			cuenta.desactivar();
			sesiones.cerrarSesionesDe(id);
			auditor.registrar(actorId, AccionAuditada.CUENTA_DESACTIVADA, "Cuenta " + cuenta.getCorreo() + " desactivada.");
		}
		return cuenta;
	}

	/**
	 * RN03: si la cuenta es un administrador activo que va a dejar de serlo, debe quedar otro.
	 */
	private void exigirOtroAdministradorActivo(Personal cuenta) {
		if (!cuenta.esAdministrador() || !cuenta.isActivo()) {
			return;
		}
		boolean hayOtro = personal.bloquearAdministradoresActivos().stream()
			.anyMatch(admin -> !admin.getId().equals(cuenta.getId()));
		if (!hayOtro) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "ULTIMO_ADMINISTRADOR",
					"Debe quedar al menos un administrador activo.");
		}
	}

	/**
	 * RN01: un correo identifica una sola cuenta, sea de cliente o de personal.
	 */
	private void exigirCorreoLibre(String correo, Long idPropio) {
		String normalizado = Usuario.normalizarCorreo(correo);
		boolean ocupado = idPropio == null ? usuarios.existsByCorreo(normalizado)
				: usuarios.existsByCorreoAndIdNot(normalizado, idPropio);
		if (ocupado) {
			throw new ReglaNegocioException(HttpStatus.CONFLICT, "CORREO_REGISTRADO",
					"Ya existe una cuenta con este correo.");
		}
	}

	private Rol rol(RolBase codigo) {
		return roles.findById(codigo).orElseThrow();
	}

}
