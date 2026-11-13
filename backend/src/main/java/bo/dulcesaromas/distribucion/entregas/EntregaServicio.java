package bo.dulcesaromas.distribucion.entregas;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.distribucion.EntregaVista;
import bo.dulcesaromas.distribucion.IEntregas;
import bo.dulcesaromas.ecommerce.EstadoPedido;
import bo.dulcesaromas.ecommerce.IPedidos;
import bo.dulcesaromas.ecommerce.TipoEntrega;
import bo.dulcesaromas.seguridad.IAutenticacion;
import bo.dulcesaromas.seguridad.RolBase;
import bo.dulcesaromas.seguridad.UsuarioActual;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asignar repartidor y entregar (CU08, HU13). El pedido cambia de estado a través de IPedidos, en la misma
 * transacción que la entrega.
 */
@Service
class EntregaServicio implements IEntregas {

	/** Se puede asignar o cambiar el repartidor hasta que el pedido sale (CU08, alt. 4). */
	private static final Set<EstadoPedido> ASIGNABLE = Set.of(EstadoPedido.CONFIRMADO, EstadoPedido.EN_PREPARACION,
			EstadoPedido.LISTO);

	private final EntregaRepositorio entregas;

	private final IPedidos pedidos;

	private final IAutenticacion autenticacion;

	private final Clock reloj;

	EntregaServicio(EntregaRepositorio entregas, IPedidos pedidos, IAutenticacion autenticacion, Clock reloj) {
		this.entregas = entregas;
		this.pedidos = pedidos;
		this.autenticacion = autenticacion;
		this.reloj = reloj;
	}

	@Override
	public List<IAutenticacion.UsuarioResumen> repartidores() {
		return autenticacion.personalActivo(RolBase.REPARTIDOR);
	}

	@Override
	@Transactional
	public EntregaVista asignar(Long pedidoId, Long repartidorId, UsuarioActual actor) {
		IPedidos.DatosEntrega pedido = datos(pedidoId);
		if (pedido.tipoEntrega() != TipoEntrega.DELIVERY) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "SOLO_DELIVERY",
					"Un pedido con recojo en tienda no lleva repartidor.");
		}
		if (!ASIGNABLE.contains(pedido.estado())) {
			throw new ReglaNegocioException(HttpStatus.CONFLICT, "ASIGNACION_NO_PERMITIDA",
					"No se asigna repartidor a un pedido " + pedido.estado().getNombre() + ".");
		}
		if (repartidores().stream().noneMatch(r -> r.id().equals(repartidorId))) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "REPARTIDOR_INVALIDO",
					"Elige un repartidor activo.", Map.of("repartidorId", "Elige un repartidor activo."));
		}
		Entrega entrega = entregas.bloquearDePedido(pedidoId)
			.map(e -> {
				e.asignar(repartidorId, actor.id(), reloj.instant());
				return e;
			})
			.orElseGet(() -> entregas.save(new Entrega(pedidoId, repartidorId, actor.id(), reloj.instant())));
		return vistas(List.of(entrega)).getFirst();
	}

	/**
	 * Entregas que siguen abiertas, para el personal que asigna.
	 */
	@Override
	@Transactional(readOnly = true)
	public List<EntregaVista> abiertas() {
		return vistas(entregas.findByFechaEntregaIsNullOrderByIdAsc());
	}

	/**
	 * Solo las entregas del repartidor (RF41, RNF16).
	 */
	@Override
	@Transactional(readOnly = true)
	public List<EntregaVista> de(UsuarioActual repartidor) {
		return vistas(entregas.findByRepartidorIdAndFechaEntregaIsNullOrderByIdAsc(repartidor.id()));
	}

	@Override
	@Transactional(readOnly = true)
	public EntregaVista obtener(Long pedidoId, UsuarioActual usuario) {
		Entrega entrega = entregas.findByPedidoId(pedidoId).orElseThrow(EntregaServicio::sinRepartidor);
		exigirPropia(entrega, usuario);
		return vistas(List.of(entrega)).getFirst();
	}

	@Override
	@Transactional
	public EntregaVista marcarEnCamino(Long pedidoId, UsuarioActual usuario) {
		Entrega entrega = bloquearPropia(pedidoId, usuario);
		pedidos.marcarEnCamino(pedidoId, usuario.id());
		return vistas(List.of(entrega)).getFirst();
	}

	@Override
	@Transactional
	public EntregaVista marcarEntregada(Long pedidoId, UsuarioActual usuario) {
		Entrega entrega = bloquearPropia(pedidoId, usuario);
		pedidos.marcarEntregado(pedidoId, usuario.id());
		entrega.marcarEntregada(reloj.instant());
		return vistas(List.of(entrega)).getFirst();
	}

	/**
	 * RN13: el pedido vuelve a Listo con la observación, para reasignarlo o reintentar.
	 */
	@Override
	@Transactional
	public EntregaVista registrarFallo(Long pedidoId, String observacion, UsuarioActual usuario) {
		Entrega entrega = bloquearPropia(pedidoId, usuario);
		pedidos.registrarEntregaFallida(pedidoId, usuario.id(), observacion);
		entrega.registrarFallo(observacion);
		return vistas(List.of(entrega)).getFirst();
	}

	private Entrega bloquearPropia(Long pedidoId, UsuarioActual usuario) {
		Entrega entrega = entregas.bloquearDePedido(pedidoId).orElseThrow(EntregaServicio::sinRepartidor);
		exigirPropia(entrega, usuario);
		return entrega;
	}

	/**
	 * Un repartidor solo ve y actualiza sus entregas (RF41, RNF16). El intento queda registrado como acceso
	 * denegado.
	 */
	private static void exigirPropia(Entrega entrega, UsuarioActual usuario) {
		if (usuario.rol() == RolBase.REPARTIDOR && !entrega.esDe(usuario.id())) {
			throw new AccessDeniedException("La entrega es de otro repartidor");
		}
	}

	private IPedidos.DatosEntrega datos(Long pedidoId) {
		return pedidos.datosEntrega(List.of(pedidoId)).stream().findFirst()
			.orElseThrow(() -> new ReglaNegocioException(HttpStatus.NOT_FOUND, "PEDIDO_NO_ENCONTRADO",
					"No existe ese pedido."));
	}

	/**
	 * Las entregas de pedidos cancelados no aparecen.
	 */
	private List<EntregaVista> vistas(List<Entrega> lista) {
		Map<Long, IPedidos.DatosEntrega> porPedido = pedidos
			.datosEntrega(lista.stream().map(Entrega::getPedidoId).toList()).stream()
			.collect(Collectors.toMap(IPedidos.DatosEntrega::pedidoId, Function.identity()));
		Set<Long> personas = new HashSet<>();
		lista.forEach(e -> e.getAsignaciones().forEach(a -> {
			personas.add(a.getRepartidorId());
			personas.add(a.getAsignadoPor());
		}));
		porPedido.values().forEach(p -> personas.add(p.clienteId()));
		Map<Long, String> nombres = autenticacion.nombres(personas);
		return lista.stream()
			.filter(e -> porPedido.get(e.getPedidoId()).estado() != EstadoPedido.CANCELADO)
			.map(e -> {
				IPedidos.DatosEntrega p = porPedido.get(e.getPedidoId());
				return new EntregaVista(p.pedidoId(), p.estado(), nombres.get(p.clienteId()), p.direccion(), p.telefono(),
						p.horarioEntrega(), p.productos(), p.total(),
						new EntregaVista.Persona(e.getRepartidorId(), nombres.get(e.getRepartidorId())),
						e.getObservacionFallo(),
						e.getAsignaciones().stream()
							.map(a -> new EntregaVista.Asignacion(nombres.get(a.getRepartidorId()),
									nombres.get(a.getAsignadoPor()), a.getFecha()))
							.toList());
			})
			.toList();
	}

	private static ReglaNegocioException sinRepartidor() {
		return new ReglaNegocioException(HttpStatus.CONFLICT, "SIN_REPARTIDOR",
				"Este pedido todavía no tiene repartidor asignado.");
	}

}
