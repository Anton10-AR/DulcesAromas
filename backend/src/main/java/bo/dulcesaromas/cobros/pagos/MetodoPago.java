package bo.dulcesaromas.cobros.pagos;

import java.time.Instant;

import bo.dulcesaromas.compartido.Textos;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * QR, transferencia, depósito u otro que agregue el administrador sin cambiar código (RNF18). Las instrucciones
 * son los datos para pagar que ve el cliente (CU06).
 */
@Entity
@Table(name = "metodo_pago")
class MetodoPago {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;

	private String nombreClave;

	private String instrucciones;

	private boolean activo;

	private Instant qrActualizado;

	protected MetodoPago() {
	}

	MetodoPago(String nombre, String instrucciones) {
		editar(nombre, instrucciones);
	}

	void editar(String nombre, String instrucciones) {
		this.nombre = Textos.limpio(nombre);
		this.nombreClave = Textos.clave(nombre);
		this.instrucciones = instrucciones.strip();
	}

	void activar() {
		activo = true;
	}

	void desactivar() {
		activo = false;
	}

	void qrCambiado(Instant cuando) {
		qrActualizado = cuando;
	}

	Long getId() {
		return id;
	}

	String getNombre() {
		return nombre;
	}

	String getInstrucciones() {
		return instrucciones;
	}

	boolean isActivo() {
		return activo;
	}

	Instant getQrActualizado() {
		return qrActualizado;
	}

}
