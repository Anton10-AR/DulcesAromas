package bo.dulcesaromas.ecommerce.pedidos;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.ecommerce.EstadoPedido;
import bo.dulcesaromas.ecommerce.IPedidos;
import org.springframework.data.domain.Sort;
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
		return resumenDe(pedidos.findById(pedidoId).orElseThrow(AdaptadorPedidos::noEncontrado));
	}

	@Override
	@Transactional(readOnly = true)
	public List<PedidoResumen> resumenes(Collection<Long> pedidoIds) {
		return pedidos.findAllById(pedidoIds).stream().map(AdaptadorPedidos::resumenDe).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<PedidoResumen> creadosEntre(Instant desde, Instant hasta, EstadoPedido estado) {
		return pedidos.findAll(FiltrosPedido.de(estado == null ? null : Set.of(estado), desde, hasta),
				Sort.by(Sort.Direction.DESC, "fechaCreacion", "id")).stream().map(AdaptadorPedidos::resumenDe).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean tienePedidosConProducto(Long productoId) {
		return pedidos.existeLineaConProducto(productoId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Long> pedidosDeCliente(Long clienteId) {
		return pedidos.findByClienteIdOrderByFechaCreacionDescIdDesc(clienteId).stream().map(Pedido::getId).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<DatosEntrega> datosEntrega(Collection<Long> pedidoIds) {
		return pedidos.findAllById(pedidoIds).stream()
			.map(p -> new DatosEntrega(p.getId(), p.getClienteId(), p.getEstado(), p.getTipoEntrega(), p.getDireccion(),
					p.getTelefono(), p.getHorarioEntrega(), p.getTotal(),
					p.getLineas().stream().map(AdaptadorPedidos::describir).toList()))
			.toList();
	}

	@Override
	@Transactional
	public void confirmarPorPagoAprobado(Long pedidoId, Long verificadorId) {
		bloquear(pedidoId).confirmarPorPago(verificadorId, reloj.instant());
	}

	@Override
	@Transactional
	public void marcarEnCamino(Long pedidoId, Long usuarioId) {
		bloquear(pedidoId).cambiarEstado(EstadoPedido.EN_CAMINO, usuarioId, null, reloj.instant());
	}

	@Override
	@Transactional
	public void marcarEntregado(Long pedidoId, Long usuarioId) {
		Pedido pedido = bloquear(pedidoId);
		// Desde Listo, Entregado es el cierre del recojo; ese lo hace el Vendedor desde E-commerce, no quien reparte.
		if (pedido.getEstado() != EstadoPedido.EN_CAMINO) {
			throw transicionNoPermitida(pedido, EstadoPedido.ENTREGADO);
		}
		pedido.cambiarEstado(EstadoPedido.ENTREGADO, usuarioId, null, reloj.instant());
	}

	@Override
	@Transactional
	public void registrarEntregaFallida(Long pedidoId, Long usuarioId, String observacion) {
		Pedido pedido = bloquear(pedidoId);
		if (pedido.getEstado() != EstadoPedido.EN_CAMINO) {
			throw transicionNoPermitida(pedido, EstadoPedido.LISTO);
		}
		pedido.cambiarEstado(EstadoPedido.LISTO, usuarioId, observacion, reloj.instant());
	}

	private Pedido bloquear(Long pedidoId) {
		return pedidos.bloquear(pedidoId).orElseThrow(AdaptadorPedidos::noEncontrado);
	}

	private static PedidoResumen resumenDe(Pedido p) {
		return new PedidoResumen(p.getId(), p.getClienteId(), p.getRegistradoPor(), p.getEstado(), p.getTotal(),
				p.getFechaCreacion());
	}

	private static String describir(LineaPedido linea) {
		String opciones = linea.getOpciones().stream().map(OpcionElegida::getNombre).collect(Collectors.joining(", "));
		return linea.getCantidad() + " × " + linea.getNombreProducto() + (opciones.isEmpty() ? "" : " (" + opciones + ")");
	}

	private static ReglaNegocioException transicionNoPermitida(Pedido pedido, EstadoPedido nuevo) {
		return new ReglaNegocioException(HttpStatus.CONFLICT, "TRANSICION_NO_PERMITIDA",
				"Un pedido " + pedido.getEstado().getNombre() + " no puede pasar a " + nuevo.getNombre() + ".");
	}

	private static ReglaNegocioException noEncontrado() {
		return new ReglaNegocioException(HttpStatus.NOT_FOUND, "PEDIDO_NO_ENCONTRADO", "No existe ese pedido.");
	}

}
