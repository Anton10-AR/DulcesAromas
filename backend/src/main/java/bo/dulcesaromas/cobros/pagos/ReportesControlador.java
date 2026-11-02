package bo.dulcesaromas.cobros.pagos;

import java.time.LocalDate;

import bo.dulcesaromas.ecommerce.EstadoPedido;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reportes de pedidos y ventas. Solo con el permiso de reportes: el Vendedor no ve montos de ventas (HU14).
 */
@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasAuthority('REPORTES')")
class ReportesControlador {

	private final ReporteVentas reporte;

	ReportesControlador(ReporteVentas reporte) {
		this.reporte = reporte;
	}

	/**
	 * @param desde y {@code hasta}: días en La Paz, en formato aaaa-mm-dd, incluidos
	 */
	@GetMapping("/ventas")
	ReporteVentas.Reporte ventas(@RequestParam LocalDate desde, @RequestParam LocalDate hasta,
			@RequestParam(required = false) EstadoPedido estado, @RequestParam(required = false) Long metodoPago) {
		return reporte.generar(desde, hasta, estado, metodoPago);
	}

}
