package bo.dulcesaromas.ecommerce.pedidos;

import java.time.Clock;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.ecommerce.IPedidos;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class AdaptadorPedidos implements IPedidos {

	private final PedidoRepositorio pedidos;

	private final Clock reloj;

	AdaptadorPedidos(PedidoRepositorio pedidos, Clock reloj) {
		this.pedidos = pedidos;
		this.reloj = reloj;
	}

	@Override
	@Transactional(readOnly = true)
	public PedidoResumen resumen(Long pedidoId) {
		Pedido p = pedidos.findById(pedidoId).orElseThrow(AdaptadorPedidos::noEncontrado);
		return new PedidoResumen(p.getId(), p.getClienteId(), p.getRegistradoPor(), p.getEstado(), p.getTotal());
	}

	@Override
	@Transactional
	public void confirmarPorPagoAprobado(Long pedidoId, Long verificadorId) {
		pedidos.bloquear(pedidoId).orElseThrow(AdaptadorPedidos::noEncontrado)
			.confirmarPorPago(verificadorId, reloj.instant());
	}

	private static ReglaNegocioException noEncontrado() {
		return new ReglaNegocioException(HttpStatus.NOT_FOUND, "PEDIDO_NO_ENCONTRADO", "No existe ese pedido.");
	}

}
