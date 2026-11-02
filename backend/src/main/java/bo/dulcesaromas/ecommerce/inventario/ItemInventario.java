package bo.dulcesaromas.ecommerce.inventario;

import java.math.BigDecimal;
import java.time.Instant;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.compartido.Textos;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.http.HttpStatus;

/**
 * Insumo con su stock y su mínimo obligatorio (CU03).
 */
@Entity
@Table(name = "item_inventario")
class ItemInventario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;

	private String nombreClave;

	private String unidad;

	private BigDecimal stock;

	private BigDecimal minimo;

	protected ItemInventario() {
	}

	ItemInventario(String nombre, String unidad, BigDecimal minimo) {
		editar(nombre, unidad, minimo);
		this.stock = BigDecimal.ZERO;
	}

	void editar(String nombre, String unidad, BigDecimal minimo) {
		this.nombre = Textos.limpio(nombre);
		this.nombreClave = Textos.clave(nombre);
		this.unidad = Textos.limpio(unidad);
		this.minimo = minimo;
	}

	/**
	 * RN09: una salida no puede superar el stock, así que el stock nunca queda negativo.
	 */
	MovimientoInventario registrar(TipoMovimiento tipo, BigDecimal cantidad, String motivo, Long usuarioId,
			Instant ahora) {
		BigDecimal nuevo = switch (tipo) {
			case ENTRADA -> stock.add(cantidad);
			case SALIDA -> stock.subtract(cantidad);
			case AJUSTE -> cantidad;
		};
		if (nuevo.signum() < 0) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "STOCK_INSUFICIENTE",
					"La salida supera el stock: hay " + stock.stripTrailingZeros().toPlainString() + " " + unidad + ".");
		}
		stock = nuevo;
		return new MovimientoInventario(id, tipo, cantidad, nuevo, motivo.strip(), usuarioId, ahora);
	}

	/**
	 * RN09: al llegar al mínimo o bajar de él, el insumo aparece en las alertas.
	 */
	boolean enMinimo() {
		return stock.compareTo(minimo) <= 0;
	}

	Long getId() {
		return id;
	}

	String getNombre() {
		return nombre;
	}

	String getUnidad() {
		return unidad;
	}

	BigDecimal getStock() {
		return stock;
	}

	BigDecimal getMinimo() {
		return minimo;
	}

}
