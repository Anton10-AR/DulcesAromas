package bo.dulcesaromas.cobros.pagos;

import bo.dulcesaromas.cobros.EstadoPago;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import bo.dulcesaromas.compartido.Fechas;
import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.ecommerce.EstadoPedido;
import bo.dulcesaromas.ecommerce.IPedidos;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reporte de pedidos y ventas del administrador, por fecha, estado y método de pago (CU10, HU14, RF23). Está en
 * Cobros porque el método de pago es de Cobros; los pedidos los lee por IPedidos.
 */
@Service
class ReporteVentas {

	/** Doce meses de pedidos responden en 5 s o menos (RNF02); no se piden rangos mayores. */
	private static final int DIAS_MAXIMOS = 366;

	private final IPedidos pedidos;

	private final PagoRepositorio pagos;

	ReporteVentas(IPedidos pedidos, PagoRepositorio pagos) {
		this.pedidos = pedidos;
		this.pagos = pagos;
	}

	/**
	 * @param estado nulo para todos
	 * @param metodoPagoId nulo para todos; si se indica, solo los pedidos pagados con ese método
	 */
	@Transactional(readOnly = true)
	Reporte generar(LocalDate desde, LocalDate hasta, EstadoPedido estado, Long metodoPagoId) {
		if (hasta.isBefore(desde) || desde.plusDays(DIAS_MAXIMOS).isBefore(hasta)) {
			throw new ReglaNegocioException(HttpStatus.BAD_REQUEST, "RANGO_INVALIDO",
					"Elige un rango de hasta 12 meses, con la fecha inicial antes de la final.",
					Map.of("hasta", "Elige una fecha posterior a la inicial y a no más de 12 meses."));
		}
		List<IPedidos.PedidoResumen> lista = pedidos.creadosEntre(Fechas.inicioDe(desde), Fechas.finDe(hasta), estado);
		Map<Long, Pago> pagados = pagos
			.findByPedidoIdInAndEstado(lista.stream().map(IPedidos.PedidoResumen::id).toList(), EstadoPago.PAGADO)
			.stream().collect(Collectors.toMap(Pago::getPedidoId, Function.identity()));
		List<Fila> filas = lista.stream()
			.filter(p -> metodoPagoId == null
					|| (pagados.containsKey(p.id()) && metodoPagoId.equals(pagados.get(p.id()).getMetodoPagoId())))
			.map(p -> {
				Pago pago = pagados.get(p.id());
				return new Fila(p.id(), p.fechaCreacion(), p.estado(), p.total(), pago == null ? null : pago.getMetodoNombre(),
						pago != null);
			})
			.toList();
		BigDecimal montoPedidos = filas.stream().map(Fila::total).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal montoVendido = filas.stream().filter(Fila::esVenta).map(Fila::total)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new Reporte(desde, hasta, filas.size(), montoPedidos, montoVendido, filas);
	}

	/**
	 * @param montoVendido suma de los pedidos pagados que no se cancelaron
	 */
	record Reporte(LocalDate desde, LocalDate hasta, int cantidadPedidos, BigDecimal montoPedidos,
			BigDecimal montoVendido, List<Fila> pedidos) {
	}

	/**
	 * @param metodo método del pago aprobado, o nulo si el pedido no está pagado
	 */
	record Fila(Long pedidoId, Instant fecha, EstadoPedido estado, BigDecimal total, String metodo, boolean pagado) {

		boolean esVenta() {
			return pagado && estado != EstadoPedido.CANCELADO;
		}

	}

}
