package bo.dulcesaromas.cobros.pagos;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Imagen del QR estático de un método de pago. Va aparte para no cargarla al listar los métodos.
 */
@Entity
@Table(name = "qr_metodo_pago")
class QrMetodoPago {

	@Id
	private Long metodoPagoId;

	@Embedded
	private Archivo archivo;

	protected QrMetodoPago() {
	}

	QrMetodoPago(Long metodoPagoId, Archivo archivo) {
		this.metodoPagoId = metodoPagoId;
		this.archivo = archivo;
	}

	void reemplazar(Archivo archivo) {
		this.archivo = archivo;
	}

	Archivo getArchivo() {
		return archivo;
	}

}
