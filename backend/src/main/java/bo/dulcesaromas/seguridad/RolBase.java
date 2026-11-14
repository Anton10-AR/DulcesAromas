package bo.dulcesaromas.seguridad;

/**
 * Los cuatro roles fijos del personal. El administrador no crea roles nuevos (D6); solo edita sus permisos.
 */
public enum RolBase {

	ADMINISTRADOR("Administrador", "Acceso total. Gestiona usuarios, permisos, catálogo, inventario, pagos y reportes."),
	VENDEDOR("Vendedor", "Atiende pedidos y pagos, registra pedidos asistidos y asigna repartidores. "
			+ "Marca la preparación y verifica pagos solo si tiene el permiso."),
	REPARTIDOR("Repartidor", "Ve y actualiza solo sus entregas."),
	REPOSTERO("Repostero", "Prepara los pedidos: ve en Cocina qué hay que hacer y para cuándo, y marca En preparación "
			+ "y Listo. No ve datos personales ni montos del cliente.");

	private final String nombre;

	private final String descripcion;

	RolBase(String nombre, String descripcion) {
		this.nombre = nombre;
		this.descripcion = descripcion;
	}

	public String getNombre() {
		return nombre;
	}

	public String getDescripcion() {
		return descripcion;
	}

}
