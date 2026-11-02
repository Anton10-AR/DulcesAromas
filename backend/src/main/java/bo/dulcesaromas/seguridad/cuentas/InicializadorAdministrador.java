package bo.dulcesaromas.seguridad.cuentas;

import java.time.Clock;

import bo.dulcesaromas.seguridad.RolBase;
import bo.dulcesaromas.seguridad.acceso.PropiedadesSeguridad;
import bo.dulcesaromas.seguridad.auditoria.AccionAuditada;
import bo.dulcesaromas.seguridad.auditoria.Auditor;
import bo.dulcesaromas.seguridad.permisos.RolRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Crea el primer administrador desde la configuración (variables ADMIN_CORREO y ADMIN_CONTRASENA), solo si
 * no hay ningún administrador activo. Así las credenciales nunca van en el código.
 */
@Component
class InicializadorAdministrador implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(InicializadorAdministrador.class);

	private final PersonalRepositorio personal;

	private final UsuarioRepositorio usuarios;

	private final RolRepositorio roles;

	private final PasswordEncoder codificador;

	private final Auditor auditor;

	private final PropiedadesSeguridad propiedades;

	private final Clock reloj;

	InicializadorAdministrador(PersonalRepositorio personal, UsuarioRepositorio usuarios, RolRepositorio roles,
			PasswordEncoder codificador, Auditor auditor, PropiedadesSeguridad propiedades, Clock reloj) {
		this.personal = personal;
		this.usuarios = usuarios;
		this.roles = roles;
		this.codificador = codificador;
		this.auditor = auditor;
		this.propiedades = propiedades;
		this.reloj = reloj;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments argumentos) {
		if (personal.existsByActivoTrueAndRolCodigo(RolBase.ADMINISTRADOR)) {
			return;
		}
		var inicial = propiedades.administradorInicial();
		if (!StringUtils.hasText(inicial.correo())) {
			log.warn("No hay ningún administrador activo. Define ADMIN_CORREO y ADMIN_CONTRASENA para crear uno.");
			return;
		}
		ValidadorContrasena.problema(inicial.contrasena()).ifPresent(problema -> {
			throw new IllegalStateException("ADMIN_CONTRASENA no es válida: " + problema);
		});
		if (usuarios.existsByCorreo(Usuario.normalizarCorreo(inicial.correo()))) {
			throw new IllegalStateException("ADMIN_CORREO ya pertenece a otra cuenta: " + inicial.correo());
		}
		Personal admin = personal.save(new Personal(inicial.nombre(), inicial.correo(), null,
				codificador.encode(inicial.contrasena()), roles.findById(RolBase.ADMINISTRADOR).orElseThrow(),
				reloj.instant()));
		auditor.registrar(null, AccionAuditada.CUENTA_CREADA,
				"Cuenta " + admin.getCorreo() + " creada como administrador inicial desde la configuración.");
		log.info("Administrador inicial creado: {}", admin.getCorreo());
	}

}
