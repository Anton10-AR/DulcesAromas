package bo.dulcesaromas.cobros.pagos;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.ecommerce.EstadoPedido;
import bo.dulcesaromas.ecommerce.IPedidos;
import bo.dulcesaromas.seguridad.IAutenticacion;
import bo.dulcesaromas.seguridad.Permiso;
import bo.dulcesaromas.seguridad.TipoUsuario;
import bo.dulcesaromas.seguridad.UsuarioActual;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registrar un pago (CU06, HU11) y verificarlo (CU07, HU12).
 */
@Service
class PagoServicio {

	private final PagoRepositorio pagos;

	private final VerificacionRepositorio verificaciones;

	private final ComprobanteRepositorio comprobantes;

	private final MetodoPagoRepositorio metodos;

	private final IPedidos pedidos;

	private final IAutenticacion autenticacion;

	private final Clock reloj;

	PagoServicio(PagoRepositorio pagos, VerificacionRepositorio verificaciones, ComprobanteRepositorio comprobantes,
			MetodoPagoRepositorio metodos, IPedidos pedidos, IAutenticacion autenticacion, Clock reloj) {
		this.pagos = pagos;
		this.verificaciones = verificaciones;
		this.comprobantes = comprobantes;
		this.metodos = metodos;
		this.pedidos = pedidos;
		this.autenticacion = autenticacion;
		this.reloj = reloj;
	}

	/**
	 * Abre el cobro de un pedido recién creado: un pago Pendiente de pago por su total.
	 */
	@Transactional
	void abrirCobro(Long pedidoId, BigDecimal monto) {
		pagos.save(new Pago(pedidoId, monto, reloj.instant()));
	}

	@Transactional(readOnly = true)
	List<PagoVista> dePedido(Long pedidoId, UsuarioActual usuario) {
		exigirAcceso(pedidos.resumen(pedidoId), usuario);
		return vistas(pagos.findByPedidoIdOrderByIdAsc(pedidoId));
	}

	/**
	 * El cliente informa el método, la referencia y el comprobante. El pago pasa a En verificación.
	 */
	@Transactional
	PagoVista registrar(Long pedidoId, Long metodoPagoId, String referencia, byte[] comprobante, UsuarioActual cliente) {
		var pedido = pedidos.resumen(pedidoId);
		if (!pedido.clienteId().equals(cliente.id())) {
			throw pedidoNoEncontrado();
		}
		if (pedido.estado() != EstadoPedido.PENDIENTE) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "PEDIDO_NO_PENDIENTE",
					"Este pedido ya no espera un pago.");
		}
		MetodoPago metodo = metodos.findById(metodoPagoId).filter(MetodoPago::isActivo)
			.orElseThrow(() -> new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "METODO_NO_DISPONIBLE",
					"Ese método de pago no está disponible.", Map.of("metodoPagoId", "Elige otro método de pago.")));
		Archivo archivo = ValidadorArchivos.comprobante(comprobante);
		Pago pago = pagos.bloquearDePedidoEn(pedidoId, EstadoPago.PENDIENTE_DE_PAGO)
			.orElseThrow(() -> new ReglaNegocioException(HttpStatus.CONFLICT, "PAGO_NO_PENDIENTE",
					"Este pedido ya tiene un pago en verificación."));
		pago.registrar(metodo, referencia, reloj.instant());
		comprobantes.save(new Comprobante(pago.getId(), archivo));
		return PagoVista.de(pago, null);
	}

	@Transactional(readOnly = true)
	List<PagoVista> enVerificacion() {
		return vistas(pagos.findByEstadoOrderByRegistradoAsc(EstadoPago.EN_VERIFICACION));
	}

	/**
	 * El pago queda Pagado y el pedido Confirmado, en la misma transacción (RN05).
	 */
	@Transactional
	PagoVista aprobar(Long pagoId, UsuarioActual verificador) {
		Pago pago = bloquearParaDecidir(pagoId, verificador);
		pago.aprobar();
		verificaciones.save(new Verificacion(pagoId, Verificacion.Decision.APROBADO, null, verificador.id(), reloj.instant()));
		pedidos.confirmarPorPagoAprobado(pago.getPedidoId(), verificador.id());
		return vistas(List.of(pago)).getFirst();
	}

	/**
	 * RN07: el rechazo lleva observación y el sistema reabre el cobro con un pago nuevo Pendiente de pago.
	 */
	@Transactional
	PagoVista rechazar(Long pagoId, String observacion, UsuarioActual verificador) {
		Pago pago = bloquearParaDecidir(pagoId, verificador);
		pago.rechazar();
		verificaciones.save(new Verificacion(pagoId, Verificacion.Decision.RECHAZADO, observacion.strip(),
				verificador.id(), reloj.instant()));
		pagos.save(new Pago(pago.getPedidoId(), pago.getMonto(), reloj.instant()));
		return vistas(List.of(pago)).getFirst();
	}

	@Transactional(readOnly = true)
	Archivo comprobante(Long pagoId, UsuarioActual usuario) {
		Pago pago = pagos.findById(pagoId).orElseThrow(PagoServicio::pagoNoEncontrado);
		exigirAcceso(pedidos.resumen(pago.getPedidoId()), usuario);
		return comprobantes.findById(pagoId).orElseThrow(PagoServicio::pagoNoEncontrado).getArchivo();
	}

	/**
	 * RN06: quien registró un pedido asistido no verifica su pago.
	 */
	private Pago bloquearParaDecidir(Long pagoId, UsuarioActual verificador) {
		Pago pago = pagos.bloquear(pagoId).orElseThrow(PagoServicio::pagoNoEncontrado);
		if (verificador.id().equals(pedidos.resumen(pago.getPedidoId()).registradoPor())) {
			throw new ReglaNegocioException(HttpStatus.FORBIDDEN, "VERIFICACION_PROPIA",
					"No puedes verificar el pago de un pedido que tú registraste.");
		}
		return pago;
	}

	/**
	 * Un cliente solo ve los pagos de sus pedidos; el personal necesita un permiso de pedidos o de cobranza.
	 */
	private static void exigirAcceso(IPedidos.PedidoResumen pedido, UsuarioActual usuario) {
		if (usuario.tipo() == TipoUsuario.CLIENTE) {
			if (!pedido.clienteId().equals(usuario.id())) {
				throw pedidoNoEncontrado();
			}
			return;
		}
		if (!usuario.tienePermiso(Permiso.COBRANZA_VERIFICAR) && !usuario.tienePermiso(Permiso.COBRANZA_REGISTRAR)
				&& !usuario.tienePermiso(Permiso.PEDIDOS_CONSULTAR)) {
			throw new AccessDeniedException("Sin permiso para ver pagos");
		}
	}

	private List<PagoVista> vistas(List<Pago> lista) {
		Map<Long, Verificacion> porPago = verificaciones.findByPagoIdIn(lista.stream().map(Pago::getId).toList()).stream()
			.collect(Collectors.toMap(Verificacion::getPagoId, Function.identity()));
		Map<Long, String> nombres = autenticacion
			.nombres(porPago.values().stream().map(Verificacion::getVerificadorId).collect(Collectors.toSet()));
		return lista.stream().map(p -> {
			Verificacion v = porPago.get(p.getId());
			var verificacion = v == null ? null : new PagoVista.VerificacionVista(v.getDecision(), v.getObservacion(),
					nombres.get(v.getVerificadorId()), v.getFecha());
			return PagoVista.de(p, verificacion);
		}).toList();
	}

	private static ReglaNegocioException pedidoNoEncontrado() {
		return new ReglaNegocioException(HttpStatus.NOT_FOUND, "PEDIDO_NO_ENCONTRADO", "No existe ese pedido.");
	}

	private static ReglaNegocioException pagoNoEncontrado() {
		return new ReglaNegocioException(HttpStatus.NOT_FOUND, "PAGO_NO_ENCONTRADO", "No existe ese pago.");
	}

}
