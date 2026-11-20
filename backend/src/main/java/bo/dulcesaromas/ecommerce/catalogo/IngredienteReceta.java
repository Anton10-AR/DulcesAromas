package bo.dulcesaromas.ecommerce.catalogo;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Cuánto de un insumo lleva una unidad del producto, en la unidad del insumo (RF45). La receta es aproximada y solo
 * estima lo que necesita un pedido: no descuenta stock (D12). El insumo se guarda por su id, porque es del inventario.
 */
@Entity
@Table(name = "ingrediente_receta")
class IngredienteReceta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long insumoId;

	private BigDecimal cantidad;

	protected IngredienteReceta() {
	}

	IngredienteReceta(Long insumoId, BigDecimal cantidad) {
		this.insumoId = insumoId;
		this.cantidad = cantidad;
	}

	public Long getInsumoId() {
		return insumoId;
	}

	public BigDecimal getCantidad() {
		return cantidad;
	}

}
