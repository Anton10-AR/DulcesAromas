package bo.dulcesaromas.ecommerce.pedidos;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Copia de la opción de personalización y su recargo tal como estaban al hacer el pedido.
 */
@Entity
@Table(name = "opcion_elegida")
class OpcionElegida {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String tipo;

	private String nombre;

	private BigDecimal recargo;

	protected OpcionElegida() {
	}

	OpcionElegida(String tipo, String nombre, BigDecimal recargo) {
		this.tipo = tipo;
		this.nombre = nombre;
		this.recargo = recargo;
	}

	String getTipo() {
		return tipo;
	}

	String getNombre() {
		return nombre;
	}

	BigDecimal getRecargo() {
		return recargo;
	}

}
