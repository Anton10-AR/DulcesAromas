package bo.dulcesaromas.cobros.pagos;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

/**
 * Procesador de demostración para el tótem de la feria: aprueba cada cobro y devuelve un código de autorización
 * «SIM-…». No se conecta a ningún banco, no cobra dinero y no recibe datos de tarjetas.
 */
@Component
class ProcesadorTarjetasSimulado implements ProcesadorTarjetas {

	private final SecureRandom azar = new SecureRandom();

	@Override
	public Respuesta cobrar(BigDecimal monto, String referencia) {
		byte[] codigo = new byte[3];
		azar.nextBytes(codigo);
		return new Respuesta(true, "SIM-" + HexFormat.of().withUpperCase().formatHex(codigo), null);
	}

}
