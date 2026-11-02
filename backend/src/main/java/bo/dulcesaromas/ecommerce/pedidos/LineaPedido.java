package bo.dulcesaromas.ecommerce.pedidos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * Un producto dentro de un pedido, con copia de su nombre y precio del momento.
 */
@Entity
@Table(name = "linea_pedido")
class LineaPedido {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long productoId;

	private String nombreProducto;

	private BigDecimal precioUnitario;

	private int cantidad;

	/** Mensaje escrito sobre la torta: hasta 24 caracteres y sin recargo (RN17). */
	private String mensaje;

	private BigDecimal subtotal;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "linea_id", nullable = false)
	@OrderColumn(name = "orden")
	private List<OpcionElegida> opciones = new ArrayList<>();

	protected LineaPedido() {
	}

	/**
	 * El recargo de cada opción se cobra por unidad: subtotal = (precio + recargos) × cantidad (RN11).
	 */
	LineaPedido(Long productoId, String nombreProducto, BigDecimal precioUnitario, int cantidad, String mensaje,
			List<OpcionElegida> opciones) {
		this.productoId = productoId;
		this.nombreProducto = nombreProducto;
		this.precioUnitario = precioUnitario;
		this.cantidad = cantidad;
		this.mensaje = mensaje;
		this.opciones.addAll(opciones);
		BigDecimal recargos = opciones.stream().map(OpcionElegida::getRecargo).reduce(BigDecimal.ZERO, BigDecimal::add);
		this.subtotal = precioUnitario.add(recargos).multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.UNNECESSARY);
	}

	Long getProductoId() {
		return productoId;
	}

	String getNombreProducto() {
		return nombreProducto;
	}

	BigDecimal getPrecioUnitario() {
		return precioUnitario;
	}

	int getCantidad() {
		return cantidad;
	}

	String getMensaje() {
		return mensaje;
	}

	BigDecimal getSubtotal() {
		return subtotal;
	}

	List<OpcionElegida> getOpciones() {
		return opciones;
	}

}
