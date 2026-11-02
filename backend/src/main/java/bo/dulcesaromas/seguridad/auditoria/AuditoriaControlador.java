package bo.dulcesaromas.seguridad.auditoria;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasAuthority('SEGURIDAD')")
class AuditoriaControlador {

	private static final int TAMANO_PAGINA = 50;

	private final AuditoriaRepositorio auditorias;

	AuditoriaControlador(AuditoriaRepositorio auditorias) {
		this.auditorias = auditorias;
	}

	@GetMapping
	@Transactional(readOnly = true)
	Pagina listar(@RequestParam(defaultValue = "0") int pagina) {
		var resultado = auditorias.findAllByOrderByFechaDescIdDesc(PageRequest.of(Math.max(pagina, 0), TAMANO_PAGINA));
		var registros = resultado.map(a -> new Registro(a.getFecha(),
				a.getUsuario() == null ? "Sistema" : a.getUsuario().getCorreo(), a.getAccion(), a.getDetalle()));
		return new Pagina(registros.getContent(), resultado.getNumber(), resultado.getTotalPages());
	}

	record Registro(Instant fecha, String usuario, AccionAuditada accion, String detalle) {
	}

	record Pagina(List<Registro> registros, int pagina, int totalPaginas) {
	}

}
