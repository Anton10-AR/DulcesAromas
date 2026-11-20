package bo.dulcesaromas.ecommerce.catalogo;

import java.math.BigDecimal;

import bo.dulcesaromas.compartido.Textos;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tamaño, sabor, relleno o decoración de un producto, con su recargo fijo (D9). Un pedido guarda una copia
 * del nombre y del recargo, así que cambiar o quitar una opción no altera pedidos anteriores. El factor multiplica la
 * receta y el tiempo del producto: una torta Grande lleva más que una Mediana (D12).
 */
@Entity
@Table(name = "opcion_personalizacion")
class OpcionPersonalizacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String tipo;

	private String nombre;

	private BigDecimal recargo;

	private BigDecimal factor;

	protected OpcionPersonalizacion() {
	}

	OpcionPersonalizacion(String tipo, String nombre, BigDecimal recargo, BigDecimal factor) {
		this.tipo = Textos.limpio(tipo);
		this.nombre = Textos.limpio(nombre);
		this.recargo = recargo;
		this.factor = factor;
	}

	public Long getId() {
		return id;
	}

	public String getTipo() {
		return tipo;
	}

	public String getNombre() {
		return nombre;
	}

	public BigDecimal getRecargo() {
		return recargo;
	}

	public BigDecimal getFactor() {
		return factor;
	}

}
