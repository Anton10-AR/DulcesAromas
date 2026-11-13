package bo.dulcesaromas.ecommerce;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Lo que el inventario de insumos ofrece (CU03, HU09). Es independiente del catálogo: la disponibilidad de un
 * producto no sale de aquí (D2).
 */
public interface IStock {

	/**
	 * Todos los insumos, por nombre.
	 */
	List<Insumo> insumos();

	/**
	 * Los insumos en su mínimo o por debajo (RN09).
	 */
	List<Insumo> alertas();

	/**
	 * Un insumo nuevo nace con stock 0; las existencias se cargan con una entrada.
	 */
	Insumo crear(String nombre, String unidad, BigDecimal minimo);

	Insumo editar(Long id, String nombre, String unidad, BigDecimal minimo);

	/**
	 * Una salida no supera el stock (RN09).
	 * @param cantidad para un ajuste, el stock contado
	 */
	Insumo registrarMovimiento(Long id, TipoMovimiento tipo, BigDecimal cantidad, String motivo, Long usuarioId);

	/**
	 * Movimientos del insumo, del más reciente al más antiguo, con quién registró cada uno (RF31).
	 */
	List<Movimiento> historial(Long id);

	/**
	 * @param alerta el stock llegó al mínimo
	 */
	record Insumo(Long id, String nombre, String unidad, BigDecimal stock, BigDecimal minimo, boolean alerta) {
	}

	record Movimiento(TipoMovimiento tipo, BigDecimal cantidad, BigDecimal stockResultante, String motivo, String usuario,
			Instant fecha) {
	}

}
