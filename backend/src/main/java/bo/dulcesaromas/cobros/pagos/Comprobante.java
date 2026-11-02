package bo.dulcesaromas.cobros.pagos;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Comprobante que adjunta el cliente al registrar un pago: JPG, PNG o PDF de hasta 5 MB (RNF09).
 */
@Entity
@Table(name = "comprobante")
class Comprobante {

	@Id
	private Long pagoId;

	@Embedded
	private Archivo archivo;

	protected Comprobante() {
	}

	Comprobante(Long pagoId, Archivo archivo) {
		this.pagoId = pagoId;
		this.archivo = archivo;
	}

	Archivo getArchivo() {
		return archivo;
	}

}
