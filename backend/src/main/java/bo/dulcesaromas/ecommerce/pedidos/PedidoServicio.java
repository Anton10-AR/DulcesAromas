package bo.dulcesaromas.ecommerce.pedidos;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.ecommerce.EstadoPedido;
import bo.dulcesaromas.ecommerce.PedidoRegistrado;
import bo.dulcesaromas.ecommerce.TipoEntrega;
import bo.dulcesaromas.seguridad.IAutenticacion;
import bo.dulcesaromas.seguridad.Permiso;
import bo.dulcesaromas.seguridad.TipoUsuario;
import bo.dulcesaromas.seguridad.UsuarioActual;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Hacer un pedido (CU05, HU10), consultarlo (CU15, HU19) y prepararlo (CU08, HU13).
 */
@Service
class PedidoServicio {

	/** Lo que el Panel muestra si no se filtra por estado: los pedidos que todavía hay que atender. */
	private static final Set<EstadoPedido> EN_CURSO = Set.of(EstadoPedido.PENDIENTE, EstadoPedido.CONFIRMADO,
			EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO, EstadoPedido.EN_CAMINO);

	private final Cotizador cotizador;

	private final PedidoRepositorio pedidos;

	private final ConfiguracionRepositorio configuracion;

	private final IAutenticacion autenticacion;

	private final ApplicationEventPublisher eventos;

	private final Clock reloj;

	PedidoServicio(Cotizador cotizador, PedidoRepositorio pedidos, ConfiguracionRepositorio configuracion,
			IAutenticacion autenticacion, ApplicationEventPublisher eventos, Clock reloj) {
		this.cotizador = cotizador;
		this.pedidos = pedidos;
		this.configuracion = configuracion;
		this.autenticacion = autenticacion;
		this.eventos = eventos;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	PedidoVista.CotizacionVista cotizar(SolicitudPedido solicitud) {
		return PedidoVista.CotizacionVista.de(cotizador.cotizar(solicitud, false));
	}

	/**
	 * Valida la entrega y la disponibilidad, crea el pedido Pendiente y abre su cobro, todo en una transacción
	 * (RNF14). Los productos quedan bloqueados mientras tanto, así que nadie cambia su disponibilidad a medias.
	 */
	@Transactional
	PedidoVista crear(UsuarioActual cliente, SolicitudPedido solicitud) {
		validarEntrega(solicitud);
		Cotizador.Cotizacion cotizacion = cotizador.cotizar(solicitud, true);
		if (!cotizacion.noDisponibles().isEmpty()) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "PRODUCTOS_NO_DISPONIBLES",
					"Ya no están disponibles: " + String.join(", ", cotizacion.noDisponibles())
							+ ". Quítalos del carrito para confirmar.");
		}
		Pedido pedido = pedidos.save(new Pedido(cliente.id(), null, solicitud.tipoEntrega(), limpio(solicitud.direccion()),
				solicitud.telefono().strip(), solicitud.horarioEntrega().toInstant(), cotizacion.costoEnvio(),
				cotizacion.lineas().stream().map(Cotizador.LineaCotizada::linea).toList(), cliente.id(), reloj.instant()));
		eventos.publishEvent(new PedidoRegistrado(pedido.getId(), pedido.getTotal()));
		return vista(pedido);
	}

	/**
	 * Un cliente solo ve sus pedidos; el personal necesita el permiso de consultar pedidos.
	 */
	@Transactional(readOnly = true)
	PedidoVista obtener(Long id, UsuarioActual usuario) {
		Pedido pedido = pedidos.findById(id)
			.filter(p -> usuario.tipo() == TipoUsuario.PERSONAL || p.getClienteId().equals(usuario.id()))
			.orElseThrow(PedidoServicio::noEncontrado);
		if (usuario.tipo() == TipoUsuario.PERSONAL && !usuario.tienePermiso(Permiso.PEDIDOS_CONSULTAR)) {
			throw new AccessDeniedException("Sin permiso para consultar pedidos");
		}
		return vista(pedido);
	}

	/**
	 * Mis pedidos, del más reciente al más antiguo (HU19). El estado del pago lo da Cobros.
	 */
	@Transactional(readOnly = true)
	List<PedidoVista.Resumen> deCliente(Long clienteId) {
		return pedidos.findByClienteIdOrderByFechaCreacionDescIdDesc(clienteId).stream()
			.map(PedidoVista.Resumen::de)
			.toList();
	}

	/**
	 * Pedidos del Panel, del horario de entrega más próximo al más lejano.
	 * @param estado vacío para ver todos los que siguen en curso
	 */
	@Transactional(readOnly = true)
	List<PedidoVista.Resumen> delPanel(EstadoPedido estado) {
		Set<EstadoPedido> estados = estado == null ? EN_CURSO : Set.of(estado);
		return pedidos.findByEstadoInOrderByHorarioEntregaAscIdAsc(estados).stream()
			.map(PedidoVista.Resumen::de)
			.toList();
	}

	/**
	 * Confirmado → En preparación, o En preparación → Listo. Solo se prepara un pedido con el pago aprobado:
	 * únicamente así llega a Confirmado (RN05).
	 */
	@Transactional
	PedidoVista avanzarPreparacion(Long id, EstadoPedido nuevo, UsuarioActual usuario) {
		if (nuevo != EstadoPedido.EN_PREPARACION && nuevo != EstadoPedido.LISTO) {
			throw new IllegalArgumentException("No es un paso de la preparación: " + nuevo);
		}
		Pedido pedido = bloquear(id);
		pedido.cambiarEstado(nuevo, usuario.id(), null, reloj.instant());
		return vista(pedido);
	}

	/**
	 * Listo → Entregado cuando el cliente retira en tienda (CU08).
	 */
	@Transactional
	PedidoVista entregarRecojo(Long id, UsuarioActual usuario) {
		Pedido pedido = bloquear(id);
		if (pedido.getTipoEntrega() != TipoEntrega.RECOJO) {
			throw new ReglaNegocioException(HttpStatus.CONFLICT, "NO_ES_RECOJO",
					"Este pedido es con delivery; lo cierra el repartidor.");
		}
		pedido.cambiarEstado(EstadoPedido.ENTREGADO, usuario.id(), null, reloj.instant());
		return vista(pedido);
	}

	@Transactional(readOnly = true)
	BigDecimal tarifaEnvio() {
		return configuracion.actual().getTarifaEnvio();
	}

	@Transactional
	BigDecimal cambiarTarifaEnvio(BigDecimal tarifa) {
		configuracion.actual().cambiarTarifaEnvio(tarifa);
		return tarifa;
	}

	private Pedido bloquear(Long id) {
		return pedidos.bloquear(id).orElseThrow(PedidoServicio::noEncontrado);
	}

	/**
	 * El historial lleva el nombre de quien hizo cada cambio (HU13).
	 */
	private PedidoVista vista(Pedido pedido) {
		Set<Long> usuarios = pedido.getHistorial().stream().map(HistorialEstado::getUsuarioId).filter(Objects::nonNull)
			.collect(Collectors.toSet());
		return PedidoVista.de(pedido, autenticacion.nombres(usuarios));
	}

	/**
	 * RN12: el delivery exige dirección, teléfono y horario; el recojo, teléfono y horario.
	 */
	private void validarEntrega(SolicitudPedido s) {
		Map<String, String> campos = new LinkedHashMap<>();
		if (s.tipoEntrega() == null) {
			campos.put("tipoEntrega", "Elige recojo en tienda o delivery.");
		}
		if (s.tipoEntrega() == TipoEntrega.DELIVERY && !StringUtils.hasText(s.direccion())) {
			campos.put("direccion", "Escribe la dirección de entrega.");
		}
		if (!StringUtils.hasText(s.telefono())) {
			campos.put("telefono", "Escribe un teléfono de contacto.");
		}
		Instant ahora = reloj.instant();
		if (s.horarioEntrega() == null) {
			campos.put("horarioEntrega", "Elige el horario de entrega.");
		}
		else if (!s.horarioEntrega().toInstant().isAfter(ahora)) {
			campos.put("horarioEntrega", "Elige un horario que todavía no haya pasado.");
		}
		if (!campos.isEmpty()) {
			throw new ReglaNegocioException(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", "Revisa los datos de entrega.",
					campos);
		}
	}

	private static String limpio(String texto) {
		return StringUtils.hasText(texto) ? texto.strip() : null;
	}

	private static ReglaNegocioException noEncontrado() {
		return new ReglaNegocioException(HttpStatus.NOT_FOUND, "PEDIDO_NO_ENCONTRADO", "No existe ese pedido.");
	}

}
