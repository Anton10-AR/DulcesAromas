package bo.dulcesaromas.cobros;

/**
 * Estados del pago (CU, tabla 2.1). Un pago Rechazado se queda así y el sistema abre otro Pendiente de pago.
 */
public enum EstadoPago {

	PENDIENTE_DE_PAGO("Pendiente de pago"),
	EN_VERIFICACION("En verificación"),
	PAGADO("Pagado"),
	RECHAZADO("Rechazado");

	private final String nombre;

	EstadoPago(String nombre) {
		this.nombre = nombre;
	}

	public String getNombre() {
		return nombre;
	}

}
