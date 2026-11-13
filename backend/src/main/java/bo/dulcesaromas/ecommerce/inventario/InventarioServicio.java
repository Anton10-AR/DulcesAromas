package bo.dulcesaromas.ecommerce.inventario;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.compartido.Textos;
import bo.dulcesaromas.ecommerce.IStock;
import bo.dulcesaromas.ecommerce.TipoMovimiento;
import bo.dulcesaromas.seguridad.IAutenticacion;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inventario de insumos (CU03, HU09). Es la implementación de IStock.
 */
@Service
class InventarioServicio implements IStock {

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

	@Override
	@Transactional(readOnly = true)
	public List<Insumo> insumos() {
		return items.findAllByOrderByNombreAsc().stream().map(InventarioServicio::insumo).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Insumo> alertas() {
		return items.findAllByOrderByNombreAsc().stream().filter(ItemInventario::enMinimo).map(InventarioServicio::insumo)
			.toList();
	}

	/**
	 * Nace con stock 0; las existencias se cargan con una entrada.
	 */
	@Override
	@Transactional
	public Insumo crear(String nombre, String unidad, BigDecimal minimo) {
		exigirNombreLibre(nombre, null);
		return insumo(items.save(new ItemInventario(nombre, unidad, minimo)));
	}

	@Override
	@Transactional
	public Insumo editar(Long id, String nombre, String unidad, BigDecimal minimo) {
		ItemInventario item = items.findById(id).orElseThrow(InventarioServicio::noEncontrado);
		exigirNombreLibre(nombre, id);
		item.editar(nombre, unidad, minimo);
		return insumo(item);
	}

	@Override
	@Transactional
	public Insumo registrarMovimiento(Long id, TipoMovimiento tipo, BigDecimal cantidad, String motivo, Long usuarioId) {
		if (tipo != TipoMovimiento.AJUSTE && cantidad.signum() == 0) {
			throw new ReglaNegocioException(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", "Revisa los campos marcados.",
					Map.of("cantidad", "La cantidad debe ser mayor que cero."));
		}
		ItemInventario item = items.bloquear(id).orElseThrow(InventarioServicio::noEncontrado);
		movimientos.save(item.registrar(tipo, cantidad, motivo, usuarioId, reloj.instant()));
		return insumo(item);
	}

	/**
	 * Historial de movimientos del insumo, del más reciente al más antiguo, con quién registró cada uno (RF31).
	 */
	@Override
	@Transactional(readOnly = true)
	public List<Movimiento> historial(Long id) {
		if (!items.existsById(id)) {
			throw noEncontrado();
		}
		List<MovimientoInventario> lista = movimientos.findByItemIdOrderByFechaDescIdDesc(id);
		Map<Long, String> nombres = autenticacion
			.nombres(lista.stream().map(MovimientoInventario::getUsuarioId).collect(Collectors.toSet()));
		return lista.stream()
			.map(m -> new Movimiento(m.getTipo(), m.getCantidad(), m.getStockResultante(), m.getMotivo(),
					nombres.get(m.getUsuarioId()), m.getFecha()))
			.toList();
	}

	private static Insumo insumo(ItemInventario i) {
		return new Insumo(i.getId(), i.getNombre(), i.getUnidad(), i.getStock(), i.getMinimo(), i.enMinimo());
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
