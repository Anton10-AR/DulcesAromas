package bo.dulcesaromas.ecommerce.pedidos;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Valores del negocio que el administrador ajusta sin cambiar código. Hay una sola fila.
 */
@Entity
@Table(name = "configuracion")
class Configuracion {

	static final short ID = 1;

	@Id
	private Short id;

	/** Tarifa fija de envío para el delivery (D4). */
	private BigDecimal tarifaEnvio;

	protected Configuracion() {
	}

	void cambiarTarifaEnvio(BigDecimal tarifa) {
		this.tarifaEnvio = tarifa;
	}

	BigDecimal getTarifaEnvio() {
		return tarifaEnvio;
	}

}
