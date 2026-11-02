package bo.dulcesaromas.seguridad;

/**
 * Los tres roles fijos del personal. No se crean roles nuevos (D6); el administrador solo edita sus permisos.
 */
public enum RolBase {

	ADMINISTRADOR("Administrador", "Acceso total. Gestiona usuarios, permisos, catálogo, inventario, pagos y reportes."),
	VENDEDOR("Vendedor", "Atiende pedidos y pagos, registra pedidos asistidos y asigna repartidores. "
			+ "Marca la preparación y verifica pagos solo si tiene el permiso."),
	REPARTIDOR("Repartidor", "Ve y actualiza solo sus entregas.");

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
