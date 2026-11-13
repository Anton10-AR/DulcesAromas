package bo.dulcesaromas.ecommerce.catalogo;

import java.util.Collection;
import java.util.List;

import bo.dulcesaromas.ecommerce.IProductos;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * El catálogo visto desde Pedidos: copia lo necesario de cada producto, así Pedidos no depende de las entidades del
 * catálogo.
 */
@Component
class AdaptadorProductos implements IProductos {

	private final ProductoRepositorio productos;

	AdaptadorProductos(ProductoRepositorio productos) {
		this.productos = productos;
	}

	/**
	 * Se une a la transacción de quien llama: el bloqueo dura hasta que se confirma el pedido.
	 */
	@Override
	@Transactional
	public List<ProductoParaPedido> paraPedido(Collection<Long> ids, boolean bloquear) {
		List<Producto> lista = bloquear ? productos.bloquearParaPedido(ids) : productos.findAllById(ids);
		return lista.stream().map(AdaptadorProductos::paraPedido).toList();
	}

	private static ProductoParaPedido paraPedido(Producto p) {
		List<Opcion> opciones = p.getOpciones().stream()
			.map(o -> new Opcion(o.getId(), o.getTipo(), o.getNombre(), o.getRecargo()))
			.toList();
		return new ProductoParaPedido(p.getId(), p.getNombre(), p.getPrecio(), p.isPersonalizable(),
				p.esVisible() && p.isDisponible(), opciones);
	}

}
