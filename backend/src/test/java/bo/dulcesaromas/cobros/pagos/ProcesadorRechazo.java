package bo.dulcesaromas.cobros.pagos;

import java.math.BigDecimal;

/**
 * Un procesador de prueba que rechaza todo cobro, para comprobar que una tarjeta rechazada no confirma el pedido.
 */
public class ProcesadorRechazo implements ProcesadorTarjetas {

	@Override
	public Respuesta cobrar(BigDecimal monto, String referencia) {
		return new Respuesta(false, null, "Fondos insuficientes");
	}

}
