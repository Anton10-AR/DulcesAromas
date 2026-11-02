package bo.dulcesaromas.ecommerce;

/**
 * Estados del pedido (CU, tabla 2.1). Las transiciones permitidas están en {@code Pedido}.
 */
public enum EstadoPedido {

	PENDIENTE("Pendiente"),
	CONFIRMADO("Confirmado"),
	EN_PREPARACION("En preparación"),
	LISTO("Listo"),
	EN_CAMINO("En camino"),
	ENTREGADO("Entregado"),
	CANCELADO("Cancelado");

	private final String nombre;

	EstadoPedido(String nombre) {
		this.nombre = nombre;
	}

	public String getNombre() {
		return nombre;
	}

}
