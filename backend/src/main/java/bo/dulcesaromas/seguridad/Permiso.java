package bo.dulcesaromas.seguridad;

/**
 * Catálogo de permisos del Panel, según la matriz de permisos (CU, sección 8). Cada permiso es una
 * autoridad de Spring Security con su mismo nombre, para usarlo en {@code @PreAuthorize("hasAuthority('...')")}.
 */
public enum Permiso {

	INICIO_VENTAS("Inicio", "Ver montos de ventas"),
	INICIO_PENDIENTES("Inicio", "Ver pedidos y pagos pendientes"),
	CATALOGO_GESTIONAR("Catálogo", "Crear, editar y desactivar productos y categorías"),
	CATALOGO_DISPONIBILIDAD("Catálogo", "Consultar y cambiar disponibilidad"),
	INVENTARIO_CONSULTAR("Inventario", "Consultar y ver alertas"),
	INVENTARIO_MOVIMIENTOS("Inventario", "Registrar movimientos"),
	PEDIDOS_CONSULTAR("Pedidos", "Consultar y registrar pedido asistido"),
	PEDIDOS_CANCELAR("Pedidos", "Cancelar"),
	PEDIDOS_PREPARAR("Pedidos", "Marcar preparación y Listo"),
	COBRANZA_REGISTRAR("Cobranza", "Registrar pago de un pedido asistido"),
	COBRANZA_VERIFICAR("Cobranza", "Verificar pagos (aprobar o rechazar)"),
	DISTRIBUCION_ASIGNAR("Distribución", "Asignar repartidor"),
	DISTRIBUCION_ENTREGAS("Distribución", "Actualizar entregas"),
	REPORTES("Reportes", "Reportes de pedidos y ventas"),
	CONFIGURACION("Configuración", "Tarifa de envío y métodos de pago"),
	SEGURIDAD("Seguridad", "Usuarios y permisos");

	private final String modulo;

	private final String accion;

	Permiso(String modulo, String accion) {
		this.modulo = modulo;
		this.accion = accion;
	}

	public String getModulo() {
		return modulo;
	}

	public String getAccion() {
		return accion;
	}

}
