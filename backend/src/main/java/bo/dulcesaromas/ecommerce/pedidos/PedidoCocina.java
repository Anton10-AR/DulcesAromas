package bo.dulcesaromas.ecommerce.pedidos;

import java.time.Instant;
import java.util.List;

import bo.dulcesaromas.ecommerce.EstadoPedido;
import bo.dulcesaromas.ecommerce.TipoEntrega;

/**
 * Un pedido como lo ve quien lo prepara: qué hacer y para cuándo. No lleva nombre, teléfono, dirección ni montos del
 * cliente (RNF16).
 */
record PedidoCocina(Long id, EstadoPedido estado, TipoEntrega tipoEntrega, Instant horarioEntrega,
		List<LineaCocina> lineas) {

	/**
	 * @param opciones lo elegido, por ejemplo «Grande» o «Frutilla»
	 * @param mensaje lo que va escrito sobre la torta, o nulo
	 */
	record LineaCocina(String producto, int cantidad, List<String> opciones, String mensaje) {
	}

	static PedidoCocina de(Pedido p) {
		List<LineaCocina> lineas = p.getLineas().stream()
			.map(l -> new LineaCocina(l.getNombreProducto(), l.getCantidad(),
					l.getOpciones().stream().map(OpcionElegida::getNombre).toList(), l.getMensaje()))
			.toList();
		return new PedidoCocina(p.getId(), p.getEstado(), p.getTipoEntrega(), p.getHorarioEntrega(), lineas);
	}

}
