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
	 * Las recetas aproximadas de esos productos, para estimar los insumos y el tiempo de un pedido (RF46, D12). Un
	 * producto sin receta ni tiempo aparece con la lista vacía y los minutos nulos.
	 */
	List<Receta> recetas(Collection<Long> ids);

	/**
	 * @param disponible se ve en la Tienda (activo, en una categoría activa) y está marcado como disponible: se puede
	 * pedir hoy (D2, RN08)
	 * @param personalizable admite un mensaje escrito sobre la torta (RN17)
	 * @param maximoPorPedido cuántas unidades se aceptan en un pedido, sumando sus líneas; nulo si no tiene (RN18)
	 */
	record ProductoParaPedido(Long id, String nombre, BigDecimal precio, boolean personalizable, boolean disponible,
			Integer maximoPorPedido, List<Opcion> opciones) {
	}

	/**
	 * Una opción de personalización con su recargo fijo por unidad (D9) y el factor que aplica a la receta (D12).
	 */
	record Opcion(Long id, String tipo, String nombre, BigDecimal recargo, BigDecimal factor) {
	}

	/**
	 * @param minutosPorUnidad tiempo de preparación de una unidad; nulo si no se definió
	 * @param ingredientes cantidad de cada insumo por unidad, en la unidad del insumo
	 */
	record Receta(Long productoId, String nombre, Integer minutosPorUnidad, List<Ingrediente> ingredientes) {

		public boolean definida() {
			return minutosPorUnidad != null || !ingredientes.isEmpty();
		}

	}

	record Ingrediente(Long insumoId, BigDecimal cantidad) {
	}

}
