package bo.dulcesaromas.ecommerce.catalogo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import bo.dulcesaromas.ecommerce.IStock;

/**
 * El máximo por pedido y la receta de un producto, como los edita el Panel. Cada ingrediente trae el nombre y la
 * unidad del insumo, para mostrarlo sin otra consulta.
 */
record ProduccionVista(Integer maximoPorPedido, Integer minutosPorUnidad, List<IngredienteVista> receta) {

	static ProduccionVista de(Producto producto, Map<Long, IStock.Insumo> insumos) {
		return new ProduccionVista(producto.getMaximoPorPedido(), producto.getMinutosPorUnidad(),
				producto.getReceta().stream().map(i -> {
					IStock.Insumo insumo = insumos.get(i.getInsumoId());
					return new IngredienteVista(i.getInsumoId(), insumo == null ? "(insumo borrado)" : insumo.nombre(),
							insumo == null ? "" : insumo.unidad(), i.getCantidad());
				}).toList());
	}

	record IngredienteVista(Long insumoId, String nombre, String unidad, BigDecimal cantidad) {
	}

}
