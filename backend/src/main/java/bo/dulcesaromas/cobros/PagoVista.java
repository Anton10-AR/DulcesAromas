package bo.dulcesaromas.cobros;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Un pago con su última decisión, si la tiene. {@code comprobante} es la ruta del archivo, o nulo si aún no se
 * registró.
 * @param cliente nombre del titular del pedido, para quien verifica el pago
 */
public record PagoVista(Long id, Long pedidoId, String cliente, BigDecimal monto, EstadoPago estado, String metodo, String referencia,
		Instant registrado, String comprobante, VerificacionVista verificacion) {


	/**
	 * @param decision APROBADO o RECHAZADO
	 */
	public record VerificacionVista(String decision, String observacion, String verificador, Instant fecha) {
	}

}
