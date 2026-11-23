package bo.dulcesaromas.ecommerce.pedidos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.ecommerce.EstadoPedido;
import bo.dulcesaromas.ecommerce.IProductos;
import bo.dulcesaromas.ecommerce.IProductos.Receta;
import bo.dulcesaromas.ecommerce.IStock;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estima los insumos y el tiempo que requiere un pedido, sumados a los que ya están en la cola de la cocina
 * (Confirmados y En preparación), para que quien verifica el pago sepa si se puede cumplir (RF46, CU07). Usa las
 * recetas aproximadas del catálogo y no descuenta stock ni impide aprobar (D12).
 */
@Service
class Estimador {

	/** Lo que la cocina ya tiene comprometido. */
	private static final Set<EstadoPedido> EN_COLA = Set.of(EstadoPedido.CONFIRMADO, EstadoPedido.EN_PREPARACION);

	private final PedidoRepositorio pedidos;

	private final IProductos productos;

	private final IStock stock;

	private final Clock reloj;

	Estimador(PedidoRepositorio pedidos, IProductos productos, IStock stock, Clock reloj) {
		this.pedidos = pedidos;
		this.productos = productos;
		this.stock = stock;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	Estimacion estimar(Long pedidoId) {
		Pedido pedido = pedidos.findById(pedidoId)
			.orElseThrow(() -> new ReglaNegocioException(HttpStatus.NOT_FOUND, "PEDIDO_NO_ENCONTRADO", "No existe ese pedido."));
		List<LineaPedido> cola = pedidos.findByEstadoIn(EN_COLA).stream()
			.filter(p -> !p.getId().equals(pedidoId))
			.flatMap(p -> p.getLineas().stream())
			.toList();

		Set<Long> ids = new HashSet<>();
		pedido.getLineas().forEach(l -> ids.add(l.getProductoId()));
		cola.forEach(l -> ids.add(l.getProductoId()));
		Map<Long, Receta> recetas = productos.recetas(ids).stream()
			.collect(Collectors.toMap(Receta::productoId, Function.identity()));

		Suma delPedido = sumar(pedido.getLineas(), recetas);
		Suma deLaCola = sumar(cola, recetas);

		List<InsumoEstimado> insumos = new ArrayList<>();
		for (IStock.Insumo insumo : stock.insumos()) {
			BigDecimal necesita = delPedido.insumos().get(insumo.id());
			if (necesita == null) {
				continue;
			}
			BigDecimal comprometido = deLaCola.insumos().getOrDefault(insumo.id(), BigDecimal.ZERO);
			BigDecimal falta = necesita.add(comprometido).subtract(insumo.stock()).max(BigDecimal.ZERO);
			insumos.add(new InsumoEstimado(insumo.id(), insumo.nombre(), insumo.unidad(), redondear(necesita),
					redondear(comprometido), insumo.stock(), redondear(falta)));
		}

		int minutosPedido = minutos(delPedido.minutos());
		int minutosCola = minutos(deLaCola.minutos());
		Instant listo = reloj.instant().plus(Duration.ofMinutes(minutosPedido + minutosCola));
		return new Estimacion(pedidoId, insumos, List.copyOf(delPedido.sinReceta()), minutosPedido, minutosCola,
				pedido.getHorarioEntrega(), listo, !listo.isAfter(pedido.getHorarioEntrega()),
				insumos.stream().allMatch(i -> i.falta().signum() == 0));
	}

	/**
	 * La receta de cada línea del pedido, con los insumos y los minutos ya multiplicados por sus unidades de receta
	 * (la cantidad por el factor del tamaño), para prepararlo en Cocina.
	 */
	@Transactional(readOnly = true)
	RecetaPedido recetaDe(Long pedidoId) {
		Pedido pedido = pedidos.findById(pedidoId)
			.orElseThrow(() -> new ReglaNegocioException(HttpStatus.NOT_FOUND, "PEDIDO_NO_ENCONTRADO", "No existe ese pedido."));
		Map<Long, Receta> recetas = productos.recetas(pedido.getLineas().stream().map(LineaPedido::getProductoId).toList())
			.stream()
			.collect(Collectors.toMap(Receta::productoId, Function.identity()));
		Map<Long, IStock.Insumo> insumos = stock.insumos().stream()
			.collect(Collectors.toMap(IStock.Insumo::id, Function.identity()));
		List<LineaReceta> lineas = new ArrayList<>();
		int minutosTotal = 0;
		for (LineaPedido linea : pedido.getLineas()) {
			Receta receta = recetas.get(linea.getProductoId());
			BigDecimal unidades = linea.unidadesDeReceta();
			List<String> opciones = linea.getOpciones().stream().map(OpcionElegida::getNombre).toList();
			if (receta == null || !receta.definida()) {
				lineas.add(new LineaReceta(linea.getNombreProducto(), linea.getCantidad(), opciones, redondear(unidades),
						null, List.of()));
				continue;
			}
			Integer minutos = receta.minutosPorUnidad() == null ? null
					: minutos(unidades.multiply(BigDecimal.valueOf(receta.minutosPorUnidad())));
			minutosTotal += minutos == null ? 0 : minutos;
			List<IngredienteTotal> ingredientes = receta.ingredientes().stream().map(i -> {
				IStock.Insumo insumo = insumos.get(i.insumoId());
				return new IngredienteTotal(insumo == null ? "(insumo borrado)" : insumo.nombre(),
						insumo == null ? "" : insumo.unidad(), redondear(i.cantidad().multiply(unidades)));
			}).toList();
			lineas.add(new LineaReceta(linea.getNombreProducto(), linea.getCantidad(), opciones, redondear(unidades),
					minutos, ingredientes));
		}
		return new RecetaPedido(pedidoId, lineas, minutosTotal);
	}

	/**
	 * Cada línea cuenta sus unidades de receta (la cantidad por el factor de sus opciones) por los insumos y los
	 * minutos de una unidad.
	 */
	private static Suma sumar(Collection<LineaPedido> lineas, Map<Long, Receta> recetas) {
		Map<Long, BigDecimal> insumos = new HashMap<>();
		BigDecimal minutos = BigDecimal.ZERO;
		Set<String> sinReceta = new LinkedHashSet<>();
		for (LineaPedido linea : lineas) {
			Receta receta = recetas.get(linea.getProductoId());
			if (receta == null || !receta.definida()) {
				sinReceta.add(linea.getNombreProducto());
				continue;
			}
			BigDecimal unidades = linea.unidadesDeReceta();
			for (var ingrediente : receta.ingredientes()) {
				insumos.merge(ingrediente.insumoId(), ingrediente.cantidad().multiply(unidades), BigDecimal::add);
			}
			if (receta.minutosPorUnidad() != null) {
				minutos = minutos.add(unidades.multiply(BigDecimal.valueOf(receta.minutosPorUnidad())));
			}
		}
		return new Suma(insumos, minutos, sinReceta);
	}

	private static int minutos(BigDecimal minutos) {
		return minutos.setScale(0, RoundingMode.CEILING).intValueExact();
	}

	private static BigDecimal redondear(BigDecimal cantidad) {
		return cantidad.setScale(3, RoundingMode.HALF_UP);
	}

	private record Suma(Map<Long, BigDecimal> insumos, BigDecimal minutos, Set<String> sinReceta) {
	}

	/**
	 * @param insumos los insumos que usa este pedido, con lo comprometido en la cola, el stock y lo que faltaría
	 * @param sinReceta productos del pedido sin receta ni tiempo: no entran en la estimación
	 * @param listoEstimado si la cocina trabajara sin parar desde ahora, primero la cola y después este pedido
	 */
	record Estimacion(Long pedidoId, List<InsumoEstimado> insumos, List<String> sinReceta, int minutosPedido,
			int minutosCola, Instant horarioEntrega, Instant listoEstimado, boolean alcanzaElTiempo,
			boolean alcanzanLosInsumos) {
	}

	/**
	 * @param minutosTotal la suma de las líneas con tiempo definido
	 */
	record RecetaPedido(Long pedidoId, List<LineaReceta> lineas, int minutosTotal) {
	}

	/**
	 * @param unidades cantidad por el factor de sus opciones (2 tortas Grandes de factor 1.5 son 3)
	 * @param minutos nulo si el producto no tiene tiempo definido
	 * @param ingredientes vacío si el producto no tiene receta
	 */
	record LineaReceta(String producto, int cantidad, List<String> opciones, BigDecimal unidades, Integer minutos,
			List<IngredienteTotal> ingredientes) {
	}

	record IngredienteTotal(String nombre, String unidad, BigDecimal cantidad) {
	}

	record InsumoEstimado(Long id, String nombre, String unidad, BigDecimal necesita, BigDecimal comprometido,
			BigDecimal stock, BigDecimal falta) {
	}

}
