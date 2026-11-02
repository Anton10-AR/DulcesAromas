package bo.dulcesaromas.ecommerce.catalogo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Imagen de un producto. Va en la base y no en disco para que el sistema se despliegue en la nube sin
 * almacenamiento propio (RNF21).
 */
@Entity
@Table(name = "imagen_producto")
class ImagenProducto {

	@Id
	private Long productoId;

	private String tipoContenido;

	private byte[] datos;

	protected ImagenProducto() {
	}

	ImagenProducto(Long productoId, String tipoContenido, byte[] datos) {
		this.productoId = productoId;
		reemplazar(tipoContenido, datos);
	}

	void reemplazar(String tipoContenido, byte[] datos) {
		this.tipoContenido = tipoContenido;
		this.datos = datos;
	}

	String getTipoContenido() {
		return tipoContenido;
	}

	byte[] getDatos() {
		return datos;
	}

}
