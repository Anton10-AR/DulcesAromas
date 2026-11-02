package bo.dulcesaromas.ecommerce;

import java.math.BigDecimal;

/**
 * Lo que E-commerce ofrece sobre los pedidos a Cobros y Distribución.
 */
public interface IPedidos {

	/**
	 * Falla con 404 si el pedido no existe.
	 */
	PedidoResumen resumen(Long pedidoId);

	/**
	 * Pasa el pedido de Pendiente a Confirmado porque su pago fue aprobado (D1, RN05). Es el único camino a
	 * Confirmado. Se une a la transacción de quien llama, así que si la aprobación falla, el pedido no cambia.
	 */
	void confirmarPorPagoAprobado(Long pedidoId, Long verificadorId);

	/**
	 * @param registradoPor quien registró un pedido asistido, o nulo si lo hizo el cliente
	 */
	record PedidoResumen(Long id, Long clienteId, Long registradoPor, EstadoPedido estado, BigDecimal total) {
	}

}
