package bo.dulcesaromas.ecommerce;

import java.time.Duration;

import bo.dulcesaromas.PruebaPedidos;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU19: el cliente ve sus pedidos y el estado de cada uno y de su pago (CU15, RNF16).
 */
class MisPedidosTests extends PruebaPedidos {

	@Test
	void veSusPedidosDelMasRecienteAlMasAntiguo() throws Exception {
		long primero = crearPedido(cliente, pedidoSimple(galletas));
		reloj.adelantar(Duration.ofMinutes(5));
		long segundo = crearPedido(cliente, pedidoDelivery());

		mvc.perform(con(cliente, get("/api/pedidos/mios"))).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", contains((int) segundo, (int) primero)))
			.andExpect(jsonPath("$[0].estado").value("PENDIENTE"))
			.andExpect(jsonPath("$[0].productos").value(2))
			.andExpect(jsonPath("$[0].total").value(330.00));
	}

	@Test
	void soloVeLosSuyos() throws Exception {
		crearPedido(cliente, pedidoSimple(galletas));
		crearCliente("otro@correo.bo");
		String otro = iniciarSesion("otro@correo.bo");

		mvc.perform(con(otro, get("/api/pedidos/mios"))).andExpect(jsonPath("$.length()").value(0));
		mvc.perform(con(otro, get("/api/pagos/mios"))).andExpect(jsonPath("$.length()").value(0));
		mvc.perform(con(admin, get("/api/pedidos/mios"))).andExpect(status().isForbidden());
	}

	@Test
	void veElEstadoDelPagoDeCadaPedido() throws Exception {
		long pagado = crearPedido(cliente, pedidoSimple(galletas));
		pagarYAprobar(pagado);
		long sinPagar = crearPedido(cliente, pedidoSimple(torta));

		mvc.perform(con(cliente, get("/api/pagos/mios"))).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].pedidoId", contains((int) sinPagar, (int) pagado)))
			.andExpect(jsonPath("$[*].estado", contains("PENDIENTE_DE_PAGO", "PAGADO")))
			.andExpect(jsonPath("$[1].observacionRechazo").value(nullValue()));
	}

	@Test
	void siElPagoFueRechazadoVeLaObservacionYPuedeVolverAPagar() throws Exception {
		long pedido = crearPedido(cliente, pedidoSimple(galletas));
		pagarYAprobar(crearPedido(cliente, pedidoSimple(torta)));
		long transferencia = jdbc.queryForObject("select id from metodo_pago where nombre_clave = 'transferencia'", Long.class);
		mvc.perform(multipart("/api/pagos").param("pedidoId", String.valueOf(pedido))
			.param("metodoPagoId", String.valueOf(transferencia)).param("referencia", "OP-9")
			.file(new MockMultipartFile("comprobante", "c.pdf", "application/pdf", "%PDF-1.7".getBytes()))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)).andExpect(status().isOk());
		long pago = jdbc.queryForObject("select max(id) from pago where pedido_id = ?", Long.class, pedido);
		mvc.perform(con(admin, post("/api/pagos/" + pago + "/rechazar")).content("{\"observacion\":\"Comprobante ilegible\"}"))
			.andExpect(status().isOk());

		mvc.perform(con(cliente, get("/api/pagos/mios")))
			.andExpect(jsonPath("$[?(@.pedidoId == %d)].estado".formatted(pedido), contains("PENDIENTE_DE_PAGO")))
			.andExpect(jsonPath("$[?(@.pedidoId == %d)].observacionRechazo".formatted(pedido),
					contains("Comprobante ilegible")));
	}

	@Test
	void elPanelListaLosPedidosPorEstado() throws Exception {
		long confirmado = crearPedido(cliente, pedidoSimple(galletas));
		pagarYAprobar(confirmado);
		crearPedido(cliente, pedidoSimple(torta));
		crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		crearPersonal("repartidor@dulces.bo", "REPARTIDOR");

		mvc.perform(con(iniciarSesion("vendedor@dulces.bo"), get("/api/pedidos").param("estado", "CONFIRMADO")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", contains((int) confirmado)));
		mvc.perform(con(admin, get("/api/pedidos"))).andExpect(jsonPath("$.length()").value(2));
		mvc.perform(con(iniciarSesion("repartidor@dulces.bo"), get("/api/pedidos"))).andExpect(status().isForbidden());
	}

}
