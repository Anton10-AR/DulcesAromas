package bo.dulcesaromas.cobros;

import bo.dulcesaromas.PruebaPedidos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pago con tarjeta de débito en el tótem: el procesador (simulado) aprueba el cobro y el sistema lo verifica solo,
 * sin comprobante ni revisión manual (RN05).
 */
class PagoConTarjetaTests extends PruebaPedidos {

	/** Como los demás métodos, la tarjeta nace desactivada; el administrador la activa. */
	@BeforeEach
	void activarTarjeta() {
		jdbc.update("update metodo_pago set activo = true where automatico");
	}

	@Test
	void conTarjetaElPedidoQuedaConfirmadoSinVerificacionManual() throws Exception {
		long pedido = crearPedido(cliente, pedidoSimple(torta));

		mvc.perform(con(cliente, post("/api/pagos/tarjeta")).content("{\"pedidoId\":%d}".formatted(pedido)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("PAGADO"))
			.andExpect(jsonPath("$.metodo").value("Tarjeta de débito"))
			.andExpect(jsonPath("$.referencia", startsWith("SIM-")))
			.andExpect(jsonPath("$.comprobante").doesNotExist())
			.andExpect(jsonPath("$.verificacion.verificador").value("Sistema (procesador de pagos)"));

		assertThat(jdbc.queryForObject("select estado from pedido where id = ?", String.class, pedido))
			.isEqualTo("CONFIRMADO");
		// No queda nada por verificar a mano.
		mvc.perform(con(admin, get("/api/pagos/en-verificacion"))).andExpect(jsonPath("$.length()").value(0));
		// Y no se puede pagar dos veces.
		mvc.perform(con(cliente, post("/api/pagos/tarjeta")).content("{\"pedidoId\":%d}".formatted(pedido)))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("PEDIDO_NO_PENDIENTE"));
	}

	@Test
	void laTarjetaNoSePagaConComprobante() throws Exception {
		long pedido = crearPedido(cliente, pedidoSimple(torta));
		long tarjeta = jdbc.queryForObject("select id from metodo_pago where automatico", Long.class);

		mvc.perform(multipart("/api/pagos").param("pedidoId", String.valueOf(pedido))
			.param("metodoPagoId", String.valueOf(tarjeta)).param("referencia", "OP-1")
			.file(new MockMultipartFile("comprobante", "c.pdf", "application/pdf", "%PDF-1.7".getBytes()))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("METODO_AUTOMATICO"));
	}

	@Test
	void soloElTitularPagaYSoloSiLaTarjetaEstaActiva() throws Exception {
		long pedido = crearPedido(cliente, pedidoSimple(torta));
		crearCliente("otro@correo.bo");
		String otro = iniciarSesion("otro@correo.bo");

		mvc.perform(con(otro, post("/api/pagos/tarjeta")).content("{\"pedidoId\":%d}".formatted(pedido)))
			.andExpect(status().isNotFound());
		mvc.perform(con(admin, post("/api/pagos/tarjeta")).content("{\"pedidoId\":%d}".formatted(pedido)))
			.andExpect(status().isForbidden());

		jdbc.update("update metodo_pago set activo = false where automatico");
		mvc.perform(con(cliente, post("/api/pagos/tarjeta")).content("{\"pedidoId\":%d}".formatted(pedido)))
			.andExpect(jsonPath("$.codigo").value("METODO_NO_DISPONIBLE"));
		assertThat(jdbc.queryForObject("select estado from pedido where id = ?", String.class, pedido))
			.isEqualTo("PENDIENTE");
	}

	@Test
	void laTiendaVeLaTarjetaComoMetodoAutomatico() throws Exception {
		mvc.perform(con(cliente, get("/api/metodos-pago")))
			.andExpect(jsonPath("$[?(@.nombre == 'Tarjeta de débito')].automatico").value(true));
	}

}
