package bo.dulcesaromas.cobros.pagos;

import java.math.BigDecimal;

/**
 * Quien cobra con tarjeta y responde si el cobro se aprobó. El sistema solo recibe la respuesta: nunca ve ni guarda
 * los datos de la tarjeta, que quedan en el terminal del procesador. Hoy hay una sola implementación, la simulada
 * para la feria; un procesador real sería otra implementación de esta interfaz.
 */
interface ProcesadorTarjetas {

	/**
	 * @param referencia lo que identifica el cobro ante el procesador, por ejemplo «Pedido #25»
	 */
	Respuesta cobrar(BigDecimal monto, String referencia);

	/**
	 * @param autorizacion el código del procesador, que queda como referencia del pago
	 * @param motivo por qué se rechazó, o nulo si se aprobó
	 */
	record Respuesta(boolean aprobado, String autorizacion, String motivo) {
	}

}
