package bo.dulcesaromas.ecommerce.inventario;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entrada, salida o ajuste de un insumo, con quién lo registró y el stock que dejó (RF31).
 */
@Entity
@Table(name = "movimiento_inventario")
class MovimientoInventario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long itemId;

	@Enumerated(EnumType.STRING)
	private TipoMovimiento tipo;

	private BigDecimal cantidad;

	private BigDecimal stockResultante;

	private String motivo;

	private Long usuarioId;

	private Instant fecha;

	protected MovimientoInventario() {
	}

	MovimientoInventario(Long itemId, TipoMovimiento tipo, BigDecimal cantidad, BigDecimal stockResultante,
			String motivo, Long usuarioId, Instant fecha) {
		this.itemId = itemId;
		this.tipo = tipo;
		this.cantidad = cantidad;
		this.stockResultante = stockResultante;
		this.motivo = motivo;
		this.usuarioId = usuarioId;
		this.fecha = fecha;
	}

	TipoMovimiento getTipo() {
		return tipo;
	}

	BigDecimal getCantidad() {
		return cantidad;
	}

	BigDecimal getStockResultante() {
		return stockResultante;
	}

	String getMotivo() {
		return motivo;
	}

	Long getUsuarioId() {
		return usuarioId;
	}

	Instant getFecha() {
		return fecha;
	}

}
