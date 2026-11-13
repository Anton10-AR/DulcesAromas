package bo.dulcesaromas.distribucion;

import java.util.List;

import bo.dulcesaromas.seguridad.IAutenticacion;
import bo.dulcesaromas.seguridad.UsuarioActual;

/**
 * Lo que Distribución ofrece: asignar repartidores y entregar los pedidos con delivery (CU08, HU13). El pedido cambia
 * de estado por IPedidos, en la misma transacción que la entrega.
 */
public interface IEntregas {

	/**
	 * El personal activo con rol Repartidor.
	 */
	List<IAutenticacion.UsuarioResumen> repartidores();

	/**
	 * Asigna el repartidor o lo cambia, hasta que el pedido sale. Solo para delivery.
	 */
	EntregaVista asignar(Long pedidoId, Long repartidorId, UsuarioActual actor);

	/**
	 * Los delivery por asignar o en curso.
	 */
	List<EntregaVista> abiertas();

	/**
	 * Las entregas del repartidor; nunca las de otro (RNF16).
	 */
	List<EntregaVista> de(UsuarioActual repartidor);

	EntregaVista obtener(Long pedidoId, UsuarioActual usuario);

	/**
	 * Listo → En camino.
	 */
	EntregaVista marcarEnCamino(Long pedidoId, UsuarioActual usuario);

	/**
	 * En camino → Entregado.
	 */
	EntregaVista marcarEntregada(Long pedidoId, UsuarioActual usuario);

	/**
	 * En camino → Listo, con observación obligatoria (RN13).
	 */
	EntregaVista registrarFallo(Long pedidoId, String observacion, UsuarioActual usuario);

}
