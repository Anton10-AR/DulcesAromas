package bo.dulcesaromas.ecommerce.pedidos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.ecommerce.IProductos;
import bo.dulcesaromas.ecommerce.IProductos.Receta;
import bo.dulcesaromas.ecommerce.IStock;
import bo.dulcesaromas.ecommerce.TipoMovimiento;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * RN19: al pasar un pedido a En preparación, descuenta del inventario los insumos de su receta, multiplicados por la
 * cantidad y el factor del tamaño de cada línea (D12). Se une a la transacción que cambia el estado: si un insumo no
 * alcanza, no se descuenta nada y el pedido no avanza (RN09). Los productos sin receta no descuentan.
 */
@Component
class DescuentoInsumos {

	private final IProductos productos;

	private final IStock stock;

	DescuentoInsumos(IProductos productos, IStock stock) {
		this.productos = productos;
		this.stock = stock;
	}

	void descontar(Pedido pedido, Long usuarioId) {
		Map<Long, BigDecimal> necesita = necesidades(pedido);
		if (necesita.isEmpty()) {
			return;
		}
		Map<Long, IStock.Insumo> insumos = stock.insumos().stream()
			.collect(Collectors.toMap(IStock.Insumo::id, Function.identity()));
		List<String> faltan = new ArrayList<>();
		necesita.forEach((id, cantidad) -> {
			IStock.Insumo insumo = insumos.get(id);
			if (insumo != null && cantidad.compareTo(insumo.stock()) > 0) {
				faltan.add(cantidad.subtract(insumo.stock()).stripTrailingZeros().toPlainString() + " " + insumo.unidad()
						+ " de " + insumo.nombre());
			}
		});
		if (!faltan.isEmpty()) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "STOCK_INSUFICIENTE",
					"No alcanza el stock para preparar el pedido #" + pedido.getId() + ": faltan " + String.join(", ", faltan)
							+ ". Registra la compra o ajusta el inventario.");
		}
		String motivo = "Pedido #" + pedido.getId() + " (descuento automático por receta)";
		necesita.forEach((id, cantidad) -> {
			if (insumos.containsKey(id)) {
				stock.registrarMovimiento(id, TipoMovimiento.SALIDA, cantidad, motivo, usuarioId);
			}
		});
	}

	/**
	 * Cuánto de cada insumo lleva el pedido, redondeado a la precisión del inventario (3 decimales).
	 */
	private Map<Long, BigDecimal> necesidades(Pedido pedido) {
		Map<Long, Receta> recetas = productos.recetas(pedido.getLineas().stream().map(LineaPedido::getProductoId).toList())
			.stream()
			.collect(Collectors.toMap(Receta::productoId, Function.identity()));
		Map<Long, BigDecimal> total = new LinkedHashMap<>();
		for (LineaPedido linea : pedido.getLineas()) {
			Receta receta = recetas.get(linea.getProductoId());
			if (receta == null) {
				continue;
			}
			BigDecimal unidades = linea.unidadesDeReceta();
			receta.ingredientes()
				.forEach(i -> total.merge(i.insumoId(), i.cantidad().multiply(unidades), BigDecimal::add));
		}
		Map<Long, BigDecimal> redondeado = new LinkedHashMap<>();
		total.forEach((id, cantidad) -> {
			BigDecimal valor = cantidad.setScale(3, RoundingMode.HALF_UP);
			if (valor.signum() > 0) {
				redondeado.put(id, valor);
			}
		});
		return redondeado;
	}

}
