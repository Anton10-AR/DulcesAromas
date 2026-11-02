package bo.dulcesaromas.ecommerce.pedidos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.ecommerce.EstadoPedido;
import bo.dulcesaromas.ecommerce.TipoEntrega;
import org.junit.jupiter.api.Test;

import static bo.dulcesaromas.ecommerce.EstadoPedido.CANCELADO;
import static bo.dulcesaromas.ecommerce.EstadoPedido.CONFIRMADO;
import static bo.dulcesaromas.ecommerce.EstadoPedido.EN_CAMINO;
import static bo.dulcesaromas.ecommerce.EstadoPedido.EN_PREPARACION;
import static bo.dulcesaromas.ecommerce.EstadoPedido.ENTREGADO;
import static bo.dulcesaromas.ecommerce.EstadoPedido.LISTO;
import static bo.dulcesaromas.ecommerce.EstadoPedido.PENDIENTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tabla 2.2 de los casos de uso: solo esas transiciones del pedido son posibles.
 */
class PedidoTests {

	private static final Instant AHORA = Instant.parse("2026-11-02T14:00:00Z");

	private static Pedido pedido(TipoEntrega tipo) {
		var linea = new LineaPedido(1L, "Torta", new BigDecimal("100.00"), 1, null, List.of());
		return new Pedido(1L, null, tipo, "Calle 1", "70012345", AHORA, new BigDecimal("0.00"), List.of(linea), 1L, AHORA);
	}

	@Test
	void nacePendienteConSuPrimerRegistroEnElHistorial() {
		Pedido p = pedido(TipoEntrega.RECOJO);

		assertThat(p.getEstado()).isEqualTo(PENDIENTE);
		assertThat(p.getHistorial()).singleElement().satisfies(h -> {
			assertThat(h.getEstadoAnterior()).isNull();
			assertThat(h.getEstadoNuevo()).isEqualTo(PENDIENTE);
		});
	}

	@Test
	void rn05SoloElPagoAprobadoLlevaAConfirmado() {
		Pedido p = pedido(TipoEntrega.RECOJO);

		assertThatThrownBy(() -> p.cambiarEstado(CONFIRMADO, 1L, null, AHORA)).isInstanceOf(ReglaNegocioException.class)
			.hasMessageContaining("se aprueba su pago");

		p.confirmarPorPago(2L, AHORA);
		assertThat(p.getEstado()).isEqualTo(CONFIRMADO);
	}

	@Test
	void elDeliveryRecorreTodosLosEstados() {
		Pedido p = pedido(TipoEntrega.DELIVERY);
		p.confirmarPorPago(2L, AHORA);
		p.cambiarEstado(EN_PREPARACION, 2L, null, AHORA);
		p.cambiarEstado(LISTO, 2L, null, AHORA);

		assertThat(p.puedePasarA(ENTREGADO)).isFalse();
		p.cambiarEstado(EN_CAMINO, 3L, null, AHORA);
		p.cambiarEstado(ENTREGADO, 3L, null, AHORA);

		assertThat(p.getHistorial()).hasSize(6);
	}

	@Test
	void elRecojoPasaDeListoAEntregadoSinIrEnCamino() {
		Pedido p = pedido(TipoEntrega.RECOJO);
		p.confirmarPorPago(2L, AHORA);
		p.cambiarEstado(EN_PREPARACION, 2L, null, AHORA);
		p.cambiarEstado(LISTO, 2L, null, AHORA);

		assertThatThrownBy(() -> p.cambiarEstado(EN_CAMINO, 2L, null, AHORA)).isInstanceOf(ReglaNegocioException.class);
		p.cambiarEstado(ENTREGADO, 2L, null, AHORA);
		assertThat(p.getEstado()).isEqualTo(ENTREGADO);
	}

	@Test
	void rn13UnaEntregaFallidaVuelveAListoConObservacion() {
		Pedido p = pedido(TipoEntrega.DELIVERY);
		p.confirmarPorPago(2L, AHORA);
		p.cambiarEstado(EN_PREPARACION, 2L, null, AHORA);
		p.cambiarEstado(LISTO, 2L, null, AHORA);
		p.cambiarEstado(EN_CAMINO, 3L, null, AHORA);

		assertThatThrownBy(() -> p.cambiarEstado(LISTO, 3L, " ", AHORA)).isInstanceOf(ReglaNegocioException.class);
		p.cambiarEstado(LISTO, 3L, "No había nadie", AHORA);
		assertThat(p.getHistorial().getLast().getObservacion()).isEqualTo("No había nadie");
	}

	@Test
	void rn14SoloSeCancelaAntesDeLaPreparacionYConMotivo() {
		Pedido pendiente = pedido(TipoEntrega.RECOJO);
		assertThatThrownBy(() -> pendiente.cambiarEstado(CANCELADO, 1L, null, AHORA))
			.isInstanceOf(ReglaNegocioException.class);
		pendiente.cambiarEstado(CANCELADO, 1L, "Me equivoqué", AHORA);
		assertThat(pendiente.getMotivoCancelacion()).isEqualTo("Me equivoqué");

		Pedido enPreparacion = pedido(TipoEntrega.RECOJO);
		enPreparacion.confirmarPorPago(2L, AHORA);
		enPreparacion.cambiarEstado(EN_PREPARACION, 2L, null, AHORA);
		assertThatThrownBy(() -> enPreparacion.cambiarEstado(CANCELADO, 1L, "Tarde", AHORA))
			.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void cualquierOtraTransicionSeRechaza() {
		Pedido p = pedido(TipoEntrega.DELIVERY);

		for (var estado : List.of(EN_PREPARACION, LISTO, EN_CAMINO, ENTREGADO, PENDIENTE)) {
			assertThat(p.puedePasarA(estado)).as(estado.name()).isFalse();
		}
		p.cambiarEstado(CANCELADO, 1L, "Ya no lo quiero", AHORA);
		for (var estado : EstadoPedido.values()) {
			assertThat(p.puedePasarA(estado)).as("desde Cancelado a " + estado).isFalse();
		}
	}

}
