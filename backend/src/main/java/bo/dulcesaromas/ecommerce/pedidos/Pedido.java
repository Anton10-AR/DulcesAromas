package bo.dulcesaromas.ecommerce.pedidos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.ecommerce.EstadoPedido;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import static bo.dulcesaromas.ecommerce.EstadoPedido.CANCELADO;
import static bo.dulcesaromas.ecommerce.EstadoPedido.CONFIRMADO;
import static bo.dulcesaromas.ecommerce.EstadoPedido.EN_CAMINO;
import static bo.dulcesaromas.ecommerce.EstadoPedido.EN_PREPARACION;
import static bo.dulcesaromas.ecommerce.EstadoPedido.ENTREGADO;
import static bo.dulcesaromas.ecommerce.EstadoPedido.LISTO;
import static bo.dulcesaromas.ecommerce.EstadoPedido.PENDIENTE;

/**
 * Compra de un cliente. Nace Pendiente y solo cambia de estado por las transiciones de la tabla 2.2 de los
 * casos de uso; cualquier otra se rechaza.
 */
@Entity
@Table(name = "pedido")
class Pedido {

	private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = new EnumMap<>(Map.of(
			PENDIENTE, EnumSet.of(CONFIRMADO, CANCELADO),
			CONFIRMADO, EnumSet.of(EN_PREPARACION, CANCELADO),
			EN_PREPARACION, EnumSet.of(LISTO),
			LISTO, EnumSet.of(EN_CAMINO, ENTREGADO),
			EN_CAMINO, EnumSet.of(ENTREGADO, LISTO)));

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long clienteId;

	private Long registradoPor;

	@Enumerated(EnumType.STRING)
	private EstadoPedido estado;

	@Enumerated(EnumType.STRING)
	private TipoEntrega tipoEntrega;

	private String direccion;

	private String telefono;

	private Instant horarioEntrega;

	private BigDecimal costoEnvio;

	private BigDecimal total;

	private Instant fechaCreacion;

	private String motivoCancelacion;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "pedido_id", nullable = false)
	@OrderColumn(name = "orden")
	private List<LineaPedido> lineas = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "pedido_id", nullable = false)
	@OrderBy("id")
	private List<HistorialEstado> historial = new ArrayList<>();

	protected Pedido() {
	}

	/**
	 * Total = suma de subtotales + tarifa de envío si es delivery (RN11).
	 * @param usuarioId quien lo registra: el cliente, o el vendedor en un pedido asistido
	 */
	Pedido(Long clienteId, Long registradoPor, TipoEntrega tipoEntrega, String direccion, String telefono,
			Instant horarioEntrega, BigDecimal costoEnvio, List<LineaPedido> lineas, Long usuarioId, Instant ahora) {
		this.clienteId = clienteId;
		this.registradoPor = registradoPor;
		this.tipoEntrega = tipoEntrega;
		this.direccion = tipoEntrega == TipoEntrega.DELIVERY ? direccion : null;
		this.telefono = telefono;
		this.horarioEntrega = horarioEntrega;
		this.costoEnvio = costoEnvio;
		this.lineas.addAll(lineas);
		this.total = lineas.stream().map(LineaPedido::getSubtotal).reduce(costoEnvio, BigDecimal::add);
		this.fechaCreacion = ahora;
		this.estado = PENDIENTE;
		this.historial.add(new HistorialEstado(null, PENDIENTE, ahora, usuarioId, null));
	}

	/**
	 * El único camino a Confirmado: su pago fue aprobado (D1, RN05).
	 */
	void confirmarPorPago(Long verificadorId, Instant ahora) {
		pasarA(CONFIRMADO, verificadorId, null, ahora);
	}

	/**
	 * Cualquier otra transición de la tabla 2.2. Quién puede hacerla lo decide quien llama.
	 */
	void cambiarEstado(EstadoPedido nuevo, Long usuarioId, String observacion, Instant ahora) {
		if (nuevo == CONFIRMADO) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "CONFIRMACION_SOLO_POR_PAGO",
					"Un pedido pasa a Confirmado solo cuando se aprueba su pago.");
		}
		pasarA(nuevo, usuarioId, observacion, ahora);
	}

	boolean puedePasarA(EstadoPedido nuevo) {
		if (!TRANSICIONES.getOrDefault(estado, Set.of()).contains(nuevo)) {
			return false;
		}
		// Desde Listo, el delivery sale En camino y el recojo pasa directo a Entregado.
		if (estado == LISTO) {
			return nuevo == (tipoEntrega == TipoEntrega.DELIVERY ? EN_CAMINO : ENTREGADO);
		}
		return true;
	}

	private void pasarA(EstadoPedido nuevo, Long usuarioId, String observacion, Instant ahora) {
		if (!puedePasarA(nuevo)) {
			throw new ReglaNegocioException(HttpStatus.CONFLICT, "TRANSICION_NO_PERMITIDA",
					"Un pedido " + estado.getNombre() + " no puede pasar a " + nuevo.getNombre() + ".");
		}
		// Cancelar (HU20) y una entrega fallida (RN13) exigen explicar por qué.
		boolean exigeObservacion = nuevo == CANCELADO || (estado == EN_CAMINO && nuevo == LISTO);
		if (exigeObservacion && !StringUtils.hasText(observacion)) {
			throw new ReglaNegocioException(HttpStatus.BAD_REQUEST, "OBSERVACION_REQUERIDA", "Indica el motivo.",
					Map.of("observacion", "Indica el motivo."));
		}
		if (nuevo == CANCELADO) {
			motivoCancelacion = observacion;
		}
		historial.add(new HistorialEstado(estado, nuevo, ahora, usuarioId, observacion));
		estado = nuevo;
	}

	Long getId() {
		return id;
	}

	Long getClienteId() {
		return clienteId;
	}

	Long getRegistradoPor() {
		return registradoPor;
	}

	EstadoPedido getEstado() {
		return estado;
	}

	TipoEntrega getTipoEntrega() {
		return tipoEntrega;
	}

	String getDireccion() {
		return direccion;
	}

	String getTelefono() {
		return telefono;
	}

	Instant getHorarioEntrega() {
		return horarioEntrega;
	}

	BigDecimal getCostoEnvio() {
		return costoEnvio;
	}

	BigDecimal getTotal() {
		return total;
	}

	Instant getFechaCreacion() {
		return fechaCreacion;
	}

	String getMotivoCancelacion() {
		return motivoCancelacion;
	}

	List<LineaPedido> getLineas() {
		return lineas;
	}

	List<HistorialEstado> getHistorial() {
		return historial;
	}

}
