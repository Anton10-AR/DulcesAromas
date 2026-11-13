package bo.dulcesaromas.cobros.pagos;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import bo.dulcesaromas.cobros.EstadoPago;
import bo.dulcesaromas.cobros.IPagos;
import bo.dulcesaromas.cobros.PagoVista;
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
 * Registrar un pago (CU06, HU11) y verificarlo (CU07, HU12). Es la implementación de IPagos.
 */
@Service
class PagoServicio implements IPagos {

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

	@Override
	@Transactional(readOnly = true)
	public List<PagoVista> dePedido(Long pedidoId, UsuarioActual usuario) {
		exigirAcceso(pedidos.resumen(pedidoId), usuario);
		return vistas(pagos.findByPedidoIdOrderByIdAsc(pedidoId));
	}

	/**
	 * El cliente informa el método, la referencia y el comprobante, y el pago pasa a En verificación. En un pedido
	 * asistido lo registra el personal con permiso (CU17, alt. 2); otra persona lo verifica (RN06).
	 */
	@Override
	@Transactional
	public PagoVista registrar(Long pedidoId, Long metodoPagoId, String referencia, byte[] comprobante, UsuarioActual usuario) {
		var pedido = pedidos.resumen(pedidoId);
		if (usuario.tipo() == TipoUsuario.CLIENTE && !pedido.clienteId().equals(usuario.id())) {
			throw pedidoNoEncontrado();
		}
		if (usuario.tipo() == TipoUsuario.PERSONAL && pedido.registradoPor() == null) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "NO_ES_ASISTIDO",
					"El personal solo registra el pago de un pedido asistido; este lo paga el cliente desde la Tienda.");
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
		return vistas(List.of(pago)).getFirst();
	}

	/**
	 * El estado del cobro de cada pedido del cliente, para Mis pedidos (HU19). Si el último pago está Pendiente de
	 * pago porque rechazaron el anterior, trae la observación del rechazo para que el cliente sepa qué corregir.
	 */
	@Override
	@Transactional(readOnly = true)
	public List<Cobro> deCliente(Long clienteId) {
		List<Long> pedidoIds = pedidos.pedidosDeCliente(clienteId);
		Map<Long, List<Pago>> porPedido = pagos.findByPedidoIdInOrderByIdAsc(pedidoIds).stream()
			.collect(Collectors.groupingBy(Pago::getPedidoId));
		List<Pago> rechazados = porPedido.values().stream().flatMap(List::stream)
			.filter(p -> p.getEstado() == EstadoPago.RECHAZADO).toList();
		Map<Long, String> observaciones = verificaciones.findByPagoIdIn(rechazados.stream().map(Pago::getId).toList())
			.stream().collect(Collectors.toMap(Verificacion::getPagoId, Verificacion::getObservacion));
		return pedidoIds.stream().filter(porPedido::containsKey).map(id -> {
			List<Pago> historial = porPedido.get(id);
			Pago vigente = historial.getLast();
			String rechazo = null;
			if (vigente.getEstado() == EstadoPago.PENDIENTE_DE_PAGO && historial.size() > 1) {
				rechazo = observaciones.get(historial.get(historial.size() - 2).getId());
			}
			return new Cobro(id, vigente.getId(), vigente.getEstado(), vigente.getMonto(), rechazo);
		}).toList();
	}

	/**
	 * El pago de un pedido cancelado no se vuelve a verificar (CU16), así que no aparece aquí.
	 */
	@Override
	@Transactional(readOnly = true)
	public List<PagoVista> enVerificacion() {
		List<Pago> lista = pagos.findByEstadoOrderByRegistradoAsc(EstadoPago.EN_VERIFICACION);
		Set<Long> cancelados = pedidos.resumenes(lista.stream().map(Pago::getPedidoId).toList()).stream()
			.filter(p -> p.estado() == EstadoPedido.CANCELADO).map(IPedidos.PedidoResumen::id)
			.collect(Collectors.toSet());
		return vistas(lista.stream().filter(p -> !cancelados.contains(p.getPedidoId())).toList());
	}

	/**
	 * El pago queda Pagado y el pedido Confirmado, en la misma transacción (RN05).
	 */
	@Override
	@Transactional
	public PagoVista aprobar(Long pagoId, UsuarioActual verificador) {
		Pago pago = bloquearParaDecidir(pagoId, verificador);
		pago.aprobar();
		verificaciones.save(new Verificacion(pagoId, Verificacion.Decision.APROBADO, null, verificador.id(), reloj.instant()));
		pedidos.confirmarPorPagoAprobado(pago.getPedidoId(), verificador.id());
		return vistas(List.of(pago)).getFirst();
	}

	/**
	 * RN07: el rechazo lleva observación y el sistema reabre el cobro con un pago nuevo Pendiente de pago.
	 */
	@Override
	@Transactional
	public PagoVista rechazar(Long pagoId, String observacion, UsuarioActual verificador) {
		Pago pago = bloquearParaDecidir(pagoId, verificador);
		pago.rechazar();
		verificaciones.save(new Verificacion(pagoId, Verificacion.Decision.RECHAZADO, observacion.strip(),
				verificador.id(), reloj.instant()));
		pagos.save(new Pago(pago.getPedidoId(), pago.getMonto(), reloj.instant()));
		return vistas(List.of(pago)).getFirst();
	}

	@Override
	@Transactional(readOnly = true)
	public ArchivoComprobante comprobante(Long pagoId, UsuarioActual usuario) {
		Pago pago = pagos.findById(pagoId).orElseThrow(PagoServicio::pagoNoEncontrado);
		exigirAcceso(pedidos.resumen(pago.getPedidoId()), usuario);
		Archivo archivo = comprobantes.findById(pagoId).orElseThrow(PagoServicio::pagoNoEncontrado).getArchivo();
		return new ArchivoComprobante(archivo.tipoContenido(), archivo.datos());
	}

	/**
	 * Solo se decide el pago de un pedido que no fue cancelado, y quien registró un pedido asistido no verifica
	 * su pago (RN06).
	 */
	private Pago bloquearParaDecidir(Long pagoId, UsuarioActual verificador) {
		Pago pago = pagos.bloquear(pagoId).orElseThrow(PagoServicio::pagoNoEncontrado);
		var pedido = pedidos.resumen(pago.getPedidoId());
		if (pedido.estado() == EstadoPedido.CANCELADO) {
			throw new ReglaNegocioException(HttpStatus.CONFLICT, "PEDIDO_CANCELADO",
					"El pedido fue cancelado; su pago ya no se verifica.");
		}
		if (verificador.id().equals(pedido.registradoPor())) {
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
		// El titular de cada pedido lo da E-commerce, y los nombres, Seguridad: Cobros no lee sus tablas.
		Map<Long, Long> clientePorPedido = pedidos.resumenes(lista.stream().map(Pago::getPedidoId).collect(Collectors.toSet()))
			.stream()
			.collect(Collectors.toMap(IPedidos.PedidoResumen::id, IPedidos.PedidoResumen::clienteId));
		Set<Long> personas = new HashSet<>(clientePorPedido.values());
		porPago.values().forEach(v -> personas.add(v.getVerificadorId()));
		Map<Long, String> nombres = autenticacion.nombres(personas);
		return lista.stream().map(p -> {
			Verificacion v = porPago.get(p.getId());
			var verificacion = v == null ? null : new PagoVista.VerificacionVista(v.getDecision().name(),
					v.getObservacion(), nombres.get(v.getVerificadorId()), v.getFecha());
			String comprobante = p.getRegistrado() == null ? null : "/api/pagos/" + p.getId() + "/comprobante";
			return new PagoVista(p.getId(), p.getPedidoId(), nombres.get(clientePorPedido.get(p.getPedidoId())),
					p.getMonto(), p.getEstado(), p.getMetodoNombre(), p.getReferencia(), p.getRegistrado(), comprobante,
					verificacion);
		}).toList();
	}

	private static ReglaNegocioException pedidoNoEncontrado() {
		return new ReglaNegocioException(HttpStatus.NOT_FOUND, "PEDIDO_NO_ENCONTRADO", "No existe ese pedido.");
	}

	private static ReglaNegocioException pagoNoEncontrado() {
		return new ReglaNegocioException(HttpStatus.NOT_FOUND, "PAGO_NO_ENCONTRADO", "No existe ese pago.");
	}

}
