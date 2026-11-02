package bo.dulcesaromas.cobros.pagos;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Un pago con su última decisión, si la tiene. {@code comprobante} es la ruta del archivo, o nulo si aún no se
 * registró.
 */
record PagoVista(Long id, Long pedidoId, BigDecimal monto, EstadoPago estado, String metodo, String referencia,
		Instant registrado, String comprobante, VerificacionVista verificacion) {

	static PagoVista de(Pago p, VerificacionVista verificacion) {
		String comprobante = p.getRegistrado() == null ? null : "/api/pagos/" + p.getId() + "/comprobante";
		return new PagoVista(p.getId(), p.getPedidoId(), p.getMonto(), p.getEstado(), p.getMetodoNombre(),
				p.getReferencia(), p.getRegistrado(), comprobante, verificacion);
	}

	record VerificacionVista(Verificacion.Decision decision, String observacion, String verificador, Instant fecha) {
	}

}
