package bo.dulcesaromas.ecommerce;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Lo que E-commerce ofrece sobre los pedidos a Cobros y Distribución. Los métodos que cambian el estado se unen a
 * la transacción de quien llama, así que si su operación falla, el pedido no cambia.
 */
public interface IPedidos {

	/**
	 * Falla con 404 si el pedido no existe.
	 */
	PedidoResumen resumen(Long pedidoId);

	/**
	 * Los ids inexistentes no aparecen.
	 */
	List<PedidoResumen> resumenes(Collection<Long> pedidoIds);

	/**
	 * Pedidos creados en [desde, hasta), del más reciente al más antiguo.
	 * @param estado nulo para todos los estados
	 */
	List<PedidoResumen> creadosEntre(Instant desde, Instant hasta, EstadoPedido estado);

	/**
	 * Si el producto aparece en algún pedido, en cualquier estado. El catálogo lo usa para no borrar un producto con
	 * pedidos (RN10).
	 */
	boolean tienePedidosConProducto(Long productoId);

	/**
	 * Ids de los pedidos de un cliente, del más reciente al más antiguo.
	 */
	List<Long> pedidosDeCliente(Long clienteId);

	/**
	 * Lo que necesita quien entrega: dirección, teléfono, horario y productos. Los ids inexistentes no aparecen.
	 */
	List<DatosEntrega> datosEntrega(Collection<Long> pedidoIds);

	/**
	 * Pasa el pedido de Pendiente a Confirmado porque su pago fue aprobado (D1, RN05). Es el único camino a
	 * Confirmado.
	 */
	void confirmarPorPagoAprobado(Long pedidoId, Long verificadorId);

	/**
	 * Listo → En camino. Solo para delivery.
	 */
	void marcarEnCamino(Long pedidoId, Long usuarioId);

	/**
	 * En camino → Entregado.
	 */
	void marcarEntregado(Long pedidoId, Long usuarioId);

	/**
	 * En camino → Listo, con observación obligatoria (RN13).
	 */
	void registrarEntregaFallida(Long pedidoId, Long usuarioId, String observacion);

	/**
	 * @param registradoPor quien registró un pedido asistido, o nulo si lo hizo el cliente
	 */
	record PedidoResumen(Long id, Long clienteId, Long registradoPor, EstadoPedido estado, BigDecimal total,
			Instant fechaCreacion) {
	}

	/**
	 * @param productos cada línea como «2 × Torta de chocolate (Grande, Fresa)»
	 */
	record DatosEntrega(Long pedidoId, Long clienteId, EstadoPedido estado, TipoEntrega tipoEntrega, String direccion,
			String telefono, Instant horarioEntrega, BigDecimal total, List<String> productos) {
	}

}
