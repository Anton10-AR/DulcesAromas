package bo.dulcesaromas.ecommerce.catalogo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Un producto como lo ven la Tienda y el Panel. {@code imagen} es la ruta de la imagen, o nulo si no tiene.
 * La ruta cambia con cada imagen nueva, así que la app puede guardarla en caché. {@code maximoPorPedido} es nulo si el
 * producto no tiene máximo propio (RN18). La receta no sale aquí: es del Panel ({@link ProduccionVista}).
 */
record ProductoVista(Long id, String nombre, String descripcion, BigDecimal precio, CategoriaResumen categoria,
		boolean activo, boolean disponible, boolean personalizable, String imagen, Integer maximoPorPedido,
		List<OpcionVista> opciones) {

	static ProductoVista de(Producto p) {
		String imagen = p.getImagenActualizada() == null ? null
				: "/api/catalogo/productos/" + p.getId() + "/imagen?v=" + p.getImagenActualizada().toEpochMilli();
		return new ProductoVista(p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
				new CategoriaResumen(p.getCategoria().getId(), p.getCategoria().getNombre()), p.isActivo(),
				p.isDisponible(), p.isPersonalizable(), imagen, p.getMaximoPorPedido(),
				p.getOpciones().stream().map(o -> new OpcionVista(o.getId(), o.getTipo(), o.getNombre(), o.getRecargo(), o.getFactor())).toList());
	}

	record CategoriaResumen(Long id, String nombre) {
	}

	record OpcionVista(Long id, String tipo, String nombre, BigDecimal recargo, BigDecimal factor) {
	}

}
