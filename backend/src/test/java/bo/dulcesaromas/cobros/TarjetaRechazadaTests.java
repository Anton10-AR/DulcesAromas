package bo.dulcesaromas.cobros;

import bo.dulcesaromas.PruebaPedidos;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Si el procesador rechaza la tarjeta, nada cambia: el pago sigue Pendiente de pago y el pedido, Pendiente.
 */
@Import(TarjetaRechazadaTests.ProcesadorQueRechaza.class)
class TarjetaRechazadaTests extends PruebaPedidos {

	@Test
	void unaTarjetaRechazadaNoConfirmaElPedido() throws Exception {
		jdbc.update("update metodo_pago set activo = true where automatico");
		long pedido = crearPedido(cliente, pedidoSimple(torta));

		mvc.perform(con(cliente, post("/api/pagos/tarjeta")).content("{\"pedidoId\":%d}".formatted(pedido)))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("TARJETA_RECHAZADA"))
			.andExpect(jsonPath("$.mensaje", containsString("Fondos insuficientes")));

		assertThat(jdbc.queryForObject("select estado from pedido where id = ?", String.class, pedido))
			.isEqualTo("PENDIENTE");
		assertThat(jdbc.queryForObject("select estado from pago where pedido_id = ?", String.class, pedido))
			.isEqualTo("PENDIENTE_DE_PAGO");
	}

	@TestConfiguration
	static class ProcesadorQueRechaza {

		@Bean
		@Primary
		bo.dulcesaromas.cobros.pagos.ProcesadorRechazo procesadorQueRechaza() {
			return new bo.dulcesaromas.cobros.pagos.ProcesadorRechazo();
		}

	}

}
