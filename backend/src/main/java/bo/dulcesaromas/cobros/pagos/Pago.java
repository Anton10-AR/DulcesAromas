package bo.dulcesaromas.cobros.pagos;

import java.math.BigDecimal;
import java.time.Instant;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.http.HttpStatus;

/**
 * Un intento de pago de un pedido. Sigue el diagrama de estados del pago: Pendiente de pago → En verificación
 * → Pagado o Rechazado.
 */
@Entity
@Table(name = "pago")
class Pago {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long pedidoId;

	private BigDecimal monto;

	@Enumerated(EnumType.STRING)
	private EstadoPago estado;

	private Long metodoPagoId;

	private String metodoNombre;

	private String referencia;

	private Instant creado;

	private Instant registrado;

	protected Pago() {
	}

	Pago(Long pedidoId, BigDecimal monto, Instant ahora) {
		this.pedidoId = pedidoId;
		this.monto = monto;
		this.estado = EstadoPago.PENDIENTE_DE_PAGO;
		this.creado = ahora;
	}

	void registrar(MetodoPago metodo, String referencia, Instant ahora) {
		exigir(EstadoPago.PENDIENTE_DE_PAGO);
		this.metodoPagoId = metodo.getId();
		this.metodoNombre = metodo.getNombre();
		this.referencia = referencia.strip();
		this.registrado = ahora;
		this.estado = EstadoPago.EN_VERIFICACION;
	}

	void aprobar() {
		exigir(EstadoPago.EN_VERIFICACION);
		estado = EstadoPago.PAGADO;
	}

	void rechazar() {
		exigir(EstadoPago.EN_VERIFICACION);
		estado = EstadoPago.RECHAZADO;
	}

	/**
	 * Si otra persona ya decidió este pago, no se aplica una segunda decisión (RNF15).
	 */
	private void exigir(EstadoPago esperado) {
		if (estado != esperado) {
			throw new ReglaNegocioException(HttpStatus.CONFLICT, "PAGO_YA_DECIDIDO",
					"Este pago ya no está " + esperado.getNombre() + "; ahora está " + estado.getNombre() + ".");
		}
	}

	Long getId() {
		return id;
	}

	Long getPedidoId() {
		return pedidoId;
	}

	BigDecimal getMonto() {
		return monto;
	}

	EstadoPago getEstado() {
		return estado;
	}

	String getMetodoNombre() {
		return metodoNombre;
	}

	String getReferencia() {
		return referencia;
	}

	Instant getCreado() {
		return creado;
	}

	Instant getRegistrado() {
		return registrado;
	}

}
