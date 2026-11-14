package bo.dulcesaromas.ecommerce;

import bo.dulcesaromas.PruebaPedidos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El Repostero prepara los pedidos desde Cocina y ve solo lo necesario para preparar (CU08, HU13, RNF16, D3).
 */
class CocinaTests extends PruebaPedidos {

	private String repostero;

	@BeforeEach
	void crearRepostero() throws Exception {
		crearPersonal("repostero@dulces.bo", "REPOSTERO");
		repostero = iniciarSesion("repostero@dulces.bo");
	}

	@Test
	void d3ElReposteroVeLosPedidosConfirmadosYLosPrepara() throws Exception {
		long pedido = crearPedido(cliente, pedidoDelivery());
		long sinPagar = crearPedido(cliente, pedidoSimple(galletas));
		pagarYAprobar(pedido);

		// Solo lo que se puede preparar: el pedido sin pagar no aparece (RN05).
		mvc.perform(con(repostero, get("/api/cocina"))).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", contains((int) pedido)))
			.andExpect(jsonPath("$[0].estado").value("CONFIRMADO"))
			.andExpect(jsonPath("$[0].lineas[0].cantidad").value(2))
			.andExpect(jsonPath("$[0].lineas[0].opciones[0]").value("Grande"));

		mvc.perform(con(repostero, post("/api/cocina/" + pedido + "/en-preparacion"))).andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("EN_PREPARACION"));
		mvc.perform(con(repostero, post("/api/cocina/" + pedido + "/listo"))).andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("LISTO"));

		// Listo sale de Cocina: ya no hay nada que preparar.
		mvc.perform(con(repostero, get("/api/cocina"))).andExpect(jsonPath("$", hasSize(0)));
		mvc.perform(con(repostero, post("/api/cocina/" + sinPagar + "/en-preparacion"))).andExpect(status().isConflict());
	}

	@Test
	void rnf16CocinaNoMuestraDatosPersonalesNiMontosDelCliente() throws Exception {
		long pedido = crearPedido(cliente, pedidoDelivery());
		pagarYAprobar(pedido);

		mvc.perform(con(repostero, get("/api/cocina"))).andExpect(status().isOk())
			.andExpect(content().string(not(containsString("70012345"))))
			.andExpect(content().string(not(containsString("Junín"))))
			.andExpect(jsonPath("$[0].cliente").doesNotExist())
			.andExpect(jsonPath("$[0].total").doesNotExist());
	}

	@Test
	void rnf16ElReposteroNoVeLaListaNiElDetalleCompletoDeLosPedidos() throws Exception {
		long pedido = crearPedido(cliente, pedidoDelivery());
		pagarYAprobar(pedido);

		mvc.perform(con(repostero, get("/api/pedidos"))).andExpect(status().isForbidden());
		mvc.perform(con(repostero, get("/api/pedidos/" + pedido))).andExpect(status().isForbidden());
		// El paso de /api/pedidos devuelve el pedido completo: el Repostero usa el de Cocina.
		mvc.perform(con(repostero, post("/api/pedidos/" + pedido + "/en-preparacion"))).andExpect(status().isForbidden());
	}

	@Test
	void elReposteroNoCobraNiCancelaNiAsignaRepartidor() throws Exception {
		long pedido = crearPedido(cliente, pedidoDelivery());

		mvc.perform(con(repostero, get("/api/pagos/en-verificacion"))).andExpect(status().isForbidden());
		mvc.perform(con(repostero, post("/api/pedidos/" + pedido + "/cancelar")).content("{\"motivo\":\"Prueba\"}"))
			.andExpect(status().isForbidden());
		mvc.perform(con(repostero, get("/api/entregas"))).andExpect(status().isForbidden());
		mvc.perform(con(repostero, get("/api/reportes/ventas").param("desde", "2026-11-01").param("hasta", "2026-11-02")))
			.andExpect(status().isForbidden());
	}

	@Test
	void d3ElVendedorNoUsaCocinaSinElPermisoDePreparar() throws Exception {
		crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		String vendedor = iniciarSesion("vendedor@dulces.bo");

		mvc.perform(con(vendedor, get("/api/cocina"))).andExpect(status().isForbidden());

		jdbc.update("insert into rol_permiso (rol, permiso) values ('VENDEDOR', 'PEDIDOS_PREPARAR')");
		mvc.perform(con(vendedor, get("/api/cocina"))).andExpect(status().isOk());
	}

	@Test
	void elReposteroConsultaElInventarioPeroNoRegistraMovimientos() throws Exception {
		mvc.perform(con(repostero, get("/api/inventario"))).andExpect(status().isOk());
		mvc.perform(con(repostero, post("/api/inventario")).content("{\"nombre\":\"Harina\",\"unidad\":\"kg\",\"minimo\":1}"))
			.andExpect(status().isForbidden());
	}

}
