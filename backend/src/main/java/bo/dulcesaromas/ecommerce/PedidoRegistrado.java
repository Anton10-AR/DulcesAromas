package bo.dulcesaromas.ecommerce;

import java.math.BigDecimal;

/**
 * Se publica dentro de la transacción que crea un pedido. Cobros lo escucha y abre el cobro Pendiente de pago,
 * sin que E-commerce dependa de Cobros.
 */
public record PedidoRegistrado(Long pedidoId, BigDecimal total) {
}
