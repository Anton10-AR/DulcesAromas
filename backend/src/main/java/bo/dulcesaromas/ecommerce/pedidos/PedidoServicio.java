package bo.dulcesaromas.ecommerce.pedidos;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.ecommerce.PedidoRegistrado;
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
 * Hacer un pedido (CU05, HU10).
 */
@Service
class PedidoServicio {

	private final Cotizador cotizador;

	private final PedidoRepositorio pedidos;

	private final ConfiguracionRepositorio configuracion;

	private final ApplicationEventPublisher eventos;

	private final Clock reloj;

	PedidoServicio(Cotizador cotizador, PedidoRepositorio pedidos, ConfiguracionRepositorio configuracion,
			ApplicationEventPublisher eventos, Clock reloj) {
		this.cotizador = cotizador;
		this.pedidos = pedidos;
		this.configuracion = configuracion;
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
		return PedidoVista.de(pedido);
	}

	/**
	 * Un cliente solo ve sus pedidos; el personal necesita el permiso de consultar pedidos.
	 */
	@Transactional(readOnly = true)
	PedidoVista obtener(Long id, UsuarioActual usuario) {
		Pedido pedido = pedidos.findById(id)
			.filter(p -> usuario.tipo() == TipoUsuario.PERSONAL || p.getClienteId().equals(usuario.id()))
			.orElseThrow(() -> new ReglaNegocioException(HttpStatus.NOT_FOUND, "PEDIDO_NO_ENCONTRADO",
					"No existe ese pedido."));
		if (usuario.tipo() == TipoUsuario.PERSONAL && !usuario.tienePermiso(Permiso.PEDIDOS_CONSULTAR)) {
			throw new AccessDeniedException("Sin permiso para consultar pedidos");
		}
		return PedidoVista.de(pedido);
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

}
