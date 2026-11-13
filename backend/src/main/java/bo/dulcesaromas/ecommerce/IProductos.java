package bo.dulcesaromas.ecommerce;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * Lo que el catálogo ofrece a quien arma un pedido: el precio, la disponibilidad y las opciones de hoy de cada
 * producto (CU05, RN08, RN11). Pedidos no lee las tablas del catálogo; usa esta interfaz.
 */
public interface IProductos {

	/**
	 * Los productos pedidos, con su precio, disponibilidad y opciones actuales. Los ids inexistentes no aparecen.
	 * @param bloquear bloquea los productos hasta el fin de la transacción de quien llama, para confirmar un pedido
	 * sin que su precio o disponibilidad cambien a la vez (RNF14)
	 */
	List<ProductoParaPedido> paraPedido(Collection<Long> ids, boolean bloquear);

	/**
	 * @param disponible se ve en la Tienda (activo, en una categoría activa) y está marcado como disponible: se puede
	 * pedir hoy (D2, RN08)
	 * @param personalizable admite un mensaje escrito sobre la torta (RN17)
	 */
	record ProductoParaPedido(Long id, String nombre, BigDecimal precio, boolean personalizable, boolean disponible,
			List<Opcion> opciones) {
	}

	/**
	 * Una opción de personalización con su recargo fijo por unidad (D9).
	 */
	record Opcion(Long id, String tipo, String nombre, BigDecimal recargo) {
	}

}
