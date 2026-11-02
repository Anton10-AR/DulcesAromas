package bo.dulcesaromas.ecommerce.pedidos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import bo.dulcesaromas.ecommerce.EstadoPedido;

record PedidoVista(Long id, EstadoPedido estado, TipoEntrega tipoEntrega, String direccion, String telefono,
		Instant horarioEntrega, List<LineaVista> lineas, BigDecimal costoEnvio, BigDecimal total,
		Instant fechaCreacion, boolean asistido, String motivoCancelacion, List<CambioVista> historial) {

	static PedidoVista de(Pedido p) {
		return new PedidoVista(p.getId(), p.getEstado(), p.getTipoEntrega(), p.getDireccion(), p.getTelefono(),
				p.getHorarioEntrega(), p.getLineas().stream().map(l -> LineaVista.de(l, true)).toList(),
				p.getCostoEnvio(), p.getTotal(), p.getFechaCreacion(), p.getRegistradoPor() != null,
				p.getMotivoCancelacion(),
				p.getHistorial().stream()
					.map(h -> new CambioVista(h.getEstadoAnterior(), h.getEstadoNuevo(), h.getFecha(), h.getObservacion()))
					.toList());
	}

	record LineaVista(Long productoId, String nombre, int cantidad, BigDecimal precioUnitario,
			List<OpcionVista> opciones, String mensaje, BigDecimal subtotal, boolean disponible) {

		static LineaVista de(LineaPedido l, boolean disponible) {
			return new LineaVista(l.getProductoId(), l.getNombreProducto(), l.getCantidad(), l.getPrecioUnitario(),
					l.getOpciones().stream().map(o -> new OpcionVista(o.getTipo(), o.getNombre(), o.getRecargo())).toList(),
					l.getMensaje(), l.getSubtotal(), disponible);
		}

	}

	record OpcionVista(String tipo, String nombre, BigDecimal recargo) {
	}

	record CambioVista(EstadoPedido anterior, EstadoPedido nuevo, Instant fecha, String observacion) {
	}

	/**
	 * El carrito cotizado. {@code confirmable} es falso si algún producto no está disponible (RN08).
	 */
	record CotizacionVista(List<LineaVista> lineas, BigDecimal costoEnvio, BigDecimal total, boolean confirmable,
			List<String> noDisponibles) {

		static CotizacionVista de(Cotizador.Cotizacion c) {
			return new CotizacionVista(
					c.lineas().stream().map(l -> LineaVista.de(l.linea(), l.disponible())).toList(),
					c.costoEnvio(), c.total(), c.noDisponibles().isEmpty(), c.noDisponibles());
		}

	}

}
