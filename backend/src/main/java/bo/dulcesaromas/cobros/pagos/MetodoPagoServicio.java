package bo.dulcesaromas.cobros.pagos;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.compartido.Textos;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Métodos de pago editables desde el Panel (RNF18).
 */
@Service
class MetodoPagoServicio {

	private final MetodoPagoRepositorio metodos;

	private final QrMetodoPagoRepositorio qrs;

	private final Clock reloj;

	MetodoPagoServicio(MetodoPagoRepositorio metodos, QrMetodoPagoRepositorio qrs, Clock reloj) {
		this.metodos = metodos;
		this.qrs = qrs;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	List<MetodoPago> activos() {
		return metodos.findByActivoTrueOrderByIdAsc();
	}

	@Transactional(readOnly = true)
	List<MetodoPago> todos() {
		return metodos.findAllByOrderByIdAsc();
	}

	/**
	 * Nace desactivado: el administrador lo activa cuando los datos para pagar estén completos.
	 */
	@Transactional
	MetodoPago crear(String nombre, String instrucciones) {
		exigirNombreLibre(nombre, null);
		return metodos.save(new MetodoPago(nombre, instrucciones));
	}

	@Transactional
	MetodoPago editar(Long id, String nombre, String instrucciones) {
		MetodoPago metodo = obtener(id);
		exigirNombreLibre(nombre, id);
		metodo.editar(nombre, instrucciones);
		if (metodo.getInstrucciones().isEmpty()) {
			metodo.desactivar();
		}
		return metodo;
	}

	@Transactional
	MetodoPago activar(Long id) {
		MetodoPago metodo = obtener(id);
		if (metodo.getInstrucciones().isEmpty()) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "INSTRUCCIONES_REQUERIDAS",
					"Escribe los datos para pagar antes de activar el método.");
		}
		metodo.activar();
		return metodo;
	}

	@Transactional
	MetodoPago desactivar(Long id) {
		MetodoPago metodo = obtener(id);
		metodo.desactivar();
		return metodo;
	}

	@Transactional
	MetodoPago guardarQr(Long id, byte[] datos) {
		MetodoPago metodo = obtener(id);
		Archivo archivo = ValidadorArchivos.qr(datos);
		qrs.findById(id).ifPresentOrElse(qr -> qr.reemplazar(archivo), () -> qrs.save(new QrMetodoPago(id, archivo)));
		metodo.qrCambiado(reloj.instant());
		return metodo;
	}

	@Transactional(readOnly = true)
	Optional<Archivo> qr(Long id) {
		return qrs.findById(id).map(QrMetodoPago::getArchivo);
	}

	private MetodoPago obtener(Long id) {
		return metodos.findById(id).orElseThrow(() -> new ReglaNegocioException(HttpStatus.NOT_FOUND,
				"METODO_NO_ENCONTRADO", "No existe ese método de pago."));
	}

	private void exigirNombreLibre(String nombre, Long idPropio) {
		String clave = Textos.clave(nombre);
		boolean ocupado = idPropio == null ? metodos.existsByNombreClave(clave)
				: metodos.existsByNombreClaveAndIdNot(clave, idPropio);
		if (ocupado) {
			throw new ReglaNegocioException(HttpStatus.CONFLICT, "METODO_REPETIDO",
					"Ya existe un método de pago con ese nombre.");
		}
	}

}
