package bo.dulcesaromas.ecommerce.inventario;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.compartido.Textos;
import bo.dulcesaromas.seguridad.IAutenticacion;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inventario de insumos (CU03, HU09).
 */
@Service
class InventarioServicio {

	private final ItemInventarioRepositorio items;

	private final MovimientoInventarioRepositorio movimientos;

	private final IAutenticacion autenticacion;

	private final Clock reloj;

	InventarioServicio(ItemInventarioRepositorio items, MovimientoInventarioRepositorio movimientos,
			IAutenticacion autenticacion, Clock reloj) {
		this.items = items;
		this.movimientos = movimientos;
		this.autenticacion = autenticacion;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	List<ItemInventario> listar() {
		return items.findAllByOrderByNombreAsc();
	}

	@Transactional(readOnly = true)
	List<ItemInventario> alertas() {
		return items.findAllByOrderByNombreAsc().stream().filter(ItemInventario::enMinimo).toList();
	}

	/**
	 * Nace con stock 0; las existencias se cargan con una entrada.
	 */
	@Transactional
	ItemInventario crear(String nombre, String unidad, BigDecimal minimo) {
		exigirNombreLibre(nombre, null);
		return items.save(new ItemInventario(nombre, unidad, minimo));
	}

	@Transactional
	ItemInventario editar(Long id, String nombre, String unidad, BigDecimal minimo) {
		ItemInventario item = items.findById(id).orElseThrow(InventarioServicio::noEncontrado);
		exigirNombreLibre(nombre, id);
		item.editar(nombre, unidad, minimo);
		return item;
	}

	@Transactional
	ItemInventario registrarMovimiento(Long id, TipoMovimiento tipo, BigDecimal cantidad, String motivo,
			Long usuarioId) {
		if (tipo != TipoMovimiento.AJUSTE && cantidad.signum() == 0) {
			throw new ReglaNegocioException(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", "Revisa los campos marcados.",
					Map.of("cantidad", "La cantidad debe ser mayor que cero."));
		}
		ItemInventario item = items.bloquear(id).orElseThrow(InventarioServicio::noEncontrado);
		movimientos.save(item.registrar(tipo, cantidad, motivo, usuarioId, reloj.instant()));
		return item;
	}

	/**
	 * Historial de movimientos del insumo, del más reciente al más antiguo, con quién registró cada uno (RF31).
	 */
	@Transactional(readOnly = true)
	List<MovimientoVista> historial(Long id) {
		if (!items.existsById(id)) {
			throw noEncontrado();
		}
		List<MovimientoInventario> lista = movimientos.findByItemIdOrderByFechaDescIdDesc(id);
		Map<Long, String> nombres = autenticacion
			.nombres(lista.stream().map(MovimientoInventario::getUsuarioId).collect(Collectors.toSet()));
		return lista.stream()
			.map(m -> new MovimientoVista(m.getTipo(), m.getCantidad(), m.getStockResultante(), m.getMotivo(),
					nombres.get(m.getUsuarioId()), m.getFecha()))
			.toList();
	}

	record MovimientoVista(TipoMovimiento tipo, BigDecimal cantidad, BigDecimal stockResultante, String motivo,
			String usuario, Instant fecha) {
	}

	private void exigirNombreLibre(String nombre, Long idPropio) {
		String clave = Textos.clave(nombre);
		boolean ocupado = idPropio == null ? items.existsByNombreClave(clave)
				: items.existsByNombreClaveAndIdNot(clave, idPropio);
		if (ocupado) {
			throw new ReglaNegocioException(HttpStatus.CONFLICT, "INSUMO_REPETIDO", "Ya existe un insumo con ese nombre.");
		}
	}

	private static ReglaNegocioException noEncontrado() {
		return new ReglaNegocioException(HttpStatus.NOT_FOUND, "INSUMO_NO_ENCONTRADO", "No existe ese insumo.");
	}

}
