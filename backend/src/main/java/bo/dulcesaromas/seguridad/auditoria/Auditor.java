package bo.dulcesaromas.seguridad.auditoria;

import java.time.Clock;

import bo.dulcesaromas.seguridad.cuentas.UsuarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Auditor {

	private final AuditoriaRepositorio auditorias;

	private final UsuarioRepositorio usuarios;

	private final Clock reloj;

	Auditor(AuditoriaRepositorio auditorias, UsuarioRepositorio usuarios, Clock reloj) {
		this.auditorias = auditorias;
		this.usuarios = usuarios;
		this.reloj = reloj;
	}

	/**
	 * Se une a la transacción en curso, para que el registro y el cambio se guarden o se descarten juntos.
	 * @param usuarioId quien actuó, o nulo si fue el sistema
	 */
	@Transactional
	public void registrar(Long usuarioId, AccionAuditada accion, String detalle) {
		var usuario = usuarioId == null ? null : usuarios.getReferenceById(usuarioId);
		auditorias.save(new Auditoria(usuario, accion, detalle, reloj.instant()));
	}

}
