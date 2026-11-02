package bo.dulcesaromas.cobros.pagos;

import bo.dulcesaromas.ecommerce.PedidoRegistrado;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Escucha en la misma transacción que crea el pedido, así que el pedido y su cobro se guardan juntos o no se
 * guarda ninguno (RNF14).
 */
@Component
class OyentePedidos {

	private final PagoServicio pagos;

	OyentePedidos(PagoServicio pagos) {
		this.pagos = pagos;
	}

	@EventListener
	void alRegistrarPedido(PedidoRegistrado evento) {
		pagos.abrirCobro(evento.pedidoId(), evento.total());
	}

}
