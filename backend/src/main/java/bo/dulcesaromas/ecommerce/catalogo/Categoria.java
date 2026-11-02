package bo.dulcesaromas.ecommerce.catalogo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Agrupa productos para el filtro del catálogo. Se crean desde el Panel, sin cambiar código (RNF18).
 */
@Entity
@Table(name = "categoria")
public class Categoria {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;

	private String nombreClave;

	private boolean activa;

	protected Categoria() {
	}

	Categoria(String nombre) {
		renombrar(nombre);
		this.activa = true;
	}

	void renombrar(String nombre) {
		this.nombre = Textos.limpio(nombre);
		this.nombreClave = Textos.clave(nombre);
	}

	void activar() {
		activa = true;
	}

	void desactivar() {
		activa = false;
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public boolean isActiva() {
		return activa;
	}

}
