package bo.dulcesaromas.ecommerce;

import bo.dulcesaromas.PruebaPedidos;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU20: cancelar un pedido antes de que se prepare (CU16, RN14, D8).
 */
class CancelacionTests extends PruebaPedidos {

	private ResultActions cancelar(String token, long pedido, String motivo) throws Exception {
		return mvc.perform(con(token, post("/api/pedidos/" + pedido + "/cancelar"))
			.content(motivo == null ? "{}" : "{\"motivo\":\"" + motivo + "\"}"));
	}

	@Test
	void elClienteCancelaUnPedidoPendienteConMotivo() throws Exception {
		long pedido = crearPedido(cliente, pedidoSimple(torta));

		cancelar(cliente, pedido, null).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.motivo").isNotEmpty());
		cancelar(cliente, pedido, "Me equivoqué de fecha").andExpect(status().isOk())
			.andExpect(jsonPath("$.pedido.estado").value("CANCELADO"))
			.andExpect(jsonPath("$.pedido.motivoCancelacion").value("Me equivoqué de fecha"))
			.andExpect(jsonPath("$.pedido.historial[1].usuario").value("ana@correo.bo"))
			.andExpect(jsonPath("$.aviso").value(nullValue()));
	}

	@Test
	void siYaEstabaPagadoAvisaQueElReembolsoEsFueraDelSistema() throws Exception {
		long pedido = crearPedido(cliente, pedidoSimple(torta));
		pagarYAprobar(pedido);

		cancelar(cliente, pedido, "Ya no lo necesito").andExpect(status().isOk())
			.andExpect(jsonPath("$.pedido.estado").value("CANCELADO"))
			.andExpect(jsonPath("$.aviso").value(containsString("reembolso")));
	}

	@Test
	void rn14NoSeCancelaUnPedidoEnPreparacion() throws Exception {
		long pedido = crearPedido(cliente, pedidoSimple(torta));
		pagarYAprobar(pedido);
		mvc.perform(con(admin, post("/api/pedidos/" + pedido + "/en-preparacion"))).andExpect(status().isOk());

		cancelar(cliente, pedido, "Tarde").andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("TRANSICION_NO_PERMITIDA"));
	}

	@Test
	void elPersonalCancelaSoloConElPermiso() throws Exception {
		long pedido = crearPedido(cliente, pedidoSimple(torta));
		crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		crearPersonal("repartidor@dulces.bo", "REPARTIDOR");
		crearCliente("otro@correo.bo");

		cancelar(iniciarSesion("otro@correo.bo"), pedido, "No es mío").andExpect(status().isNotFound());
		cancelar(iniciarSesion("repartidor@dulces.bo"), pedido, "Sin permiso").andExpect(status().isForbidden());
		cancelar(iniciarSesion("vendedor@dulces.bo"), pedido, "El cliente llamó").andExpect(status().isOk())
			.andExpect(jsonPath("$.pedido.historial[1].usuario").value("vendedor@dulces.bo"));
	}

}
