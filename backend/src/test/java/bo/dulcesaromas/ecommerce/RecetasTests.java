package bo.dulcesaromas.ecommerce;

import bo.dulcesaromas.PruebaPedidos;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Máximo de unidades por pedido (RN18, HU22) y estimación de insumos y tiempo con recetas aproximadas (RF45, RF46,
 * HU23, D12).
 */
class RecetasTests extends PruebaPedidos {

	@Test
	void rn18UnPedidoNoSuperaElMaximoDelProductoSumandoSusLineas() throws Exception {
		mvc.perform(con(admin, put("/api/productos/" + torta + "/produccion")).content("{\"maximoPorPedido\":3}"))
			.andExpect(status().isOk());
		mvc.perform(get("/api/catalogo/productos/" + torta)).andExpect(jsonPath("$.maximoPorPedido").value(3));

		// Dos Grande y dos Pequeña: cuatro tortas en dos líneas.
		String cuatro = """
				{"lineas":[{"productoId":%d,"cantidad":2,"opciones":[%d]},{"productoId":%d,"cantidad":2,"opciones":[%d]}],
				 "tipoEntrega":"RECOJO","telefono":"70012345","horarioEntrega":"%s"}""".formatted(torta, grande, torta,
				pequena, MANANA);
		pedir(cliente, cuatro).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("MAXIMO_POR_PEDIDO"))
			.andExpect(jsonPath("$.mensaje", containsString("hasta 3")));
		mvc.perform(con(cliente, post("/api/pedidos/cotizacion")).content(cuatro))
			.andExpect(jsonPath("$.codigo").value("MAXIMO_POR_PEDIDO"));

		String tres = cuatro.replaceFirst("\"cantidad\":2", "\"cantidad\":1");
		pedir(cliente, tres).andExpect(status().isCreated());
		// Otro producto, sin máximo propio, no se ve afectado.
		pedir(cliente, pedidoSimple(galletas).replace("\"cantidad\":1", "\"cantidad\":30")).andExpect(status().isCreated());
	}

	@Test
	void laRecetaUsaInsumosDelInventarioUnaSolaVez() throws Exception {
		long harina = insumo("Harina", "kg", "0");
		String receta = "{\"minutosPorUnidad\":60,\"receta\":[{\"insumoId\":%d,\"cantidad\":0.5}%s]}";

		mvc.perform(con(admin, put("/api/productos/" + torta + "/produccion")).content(receta.formatted(9999, "")))
			.andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.codigo").value("INSUMO_NO_ENCONTRADO"));
		mvc.perform(con(admin, put("/api/productos/" + torta + "/produccion"))
			.content(receta.formatted(harina, ",{\"insumoId\":%d,\"cantidad\":1}".formatted(harina))))
			.andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.codigo").value("INSUMO_REPETIDO"));
		mvc.perform(con(admin, put("/api/productos/" + torta + "/produccion")).content("{\"minutosPorUnidad\":0}"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.minutosPorUnidad").exists());

		mvc.perform(con(admin, put("/api/productos/" + torta + "/produccion")).content(receta.formatted(harina, "")))
			.andExpect(status().isOk());
		// Guardarla de nuevo con el mismo insumo reemplaza la receta anterior.
		mvc.perform(con(admin, put("/api/productos/" + torta + "/produccion")).content(receta.formatted(harina, "")))
			.andExpect(status().isOk());
		mvc.perform(con(admin, get("/api/productos/" + torta + "/produccion")))
			.andExpect(jsonPath("$.minutosPorUnidad").value(60))
			.andExpect(jsonPath("$.receta[0].nombre").value("Harina"))
			.andExpect(jsonPath("$.receta[0].unidad").value("kg"))
			.andExpect(jsonPath("$.receta[0].cantidad").value(0.5));
		// La receta no se publica en el catálogo.
		mvc.perform(get("/api/catalogo/productos/" + torta)).andExpect(jsonPath("$.receta").doesNotExist());
	}

	@Test
	void laEstimacionSumaLaColaDeLaCocinaYAvisaLoQueFalta() throws Exception {
		long harina = insumo("Harina", "kg", "5");
		long chocolate = insumo("Chocolate", "kg", "1");
		mvc.perform(con(admin, put("/api/productos/" + torta + "/produccion")).content("""
				{"minutosPorUnidad":60,"receta":[{"insumoId":%d,"cantidad":0.5},{"insumoId":%d,"cantidad":0.3}]}"""
			.formatted(harina, chocolate))).andExpect(status().isOk());
		jdbc.update("update opcion_personalizacion set factor = 1.5 where id = ?", grande);

		// En la cola: dos tortas Grande, que cuentan como tres.
		long enCola = crearPedido(cliente, pedidoDelivery());
		pagarYAprobar(enCola);
		// El pedido a verificar: cuatro tortas Pequeña y unas galletas sin receta.
		long nuevo = crearPedido(cliente, """
				{"lineas":[{"productoId":%d,"cantidad":4,"opciones":[%d]},{"productoId":%d,"cantidad":1}],
				 "tipoEntrega":"RECOJO","telefono":"70012345","horarioEntrega":"%s"}""".formatted(torta, pequena,
				galletas, MANANA));

		mvc.perform(con(admin, get("/api/pedidos/" + nuevo + "/estimacion")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.insumos[*].nombre", contains("Chocolate", "Harina")))
			.andExpect(jsonPath("$.insumos[0].necesita").value(1.2))
			.andExpect(jsonPath("$.insumos[0].comprometido").value(0.9))
			.andExpect(jsonPath("$.insumos[0].falta").value(1.1))
			.andExpect(jsonPath("$.insumos[1].falta").value(0))
			.andExpect(jsonPath("$.sinReceta", contains("Galletas de avena")))
			.andExpect(jsonPath("$.minutosPedido").value(240))
			.andExpect(jsonPath("$.minutosCola").value(180))
			.andExpect(jsonPath("$.alcanzaElTiempo").value(true))
			.andExpect(jsonPath("$.alcanzanLosInsumos").value(false));

		// Solo informa: no descuenta stock ni impide aprobar (D12).
		pagarYAprobar(nuevo);
		assertThat(jdbc.queryForObject("select stock from item_inventario where id = ?", Double.class, chocolate))
			.isEqualTo(1.0);
		assertThat(jdbc.queryForObject("select estado from pedido where id = ?", String.class, nuevo))
			.isEqualTo("CONFIRMADO");
	}

	@Test
	void laEstimacionAvisaSiLaColaNoTerminaAntesDeLaEntrega() throws Exception {
		mvc.perform(con(admin, put("/api/productos/" + torta + "/produccion")).content("{\"minutosPorUnidad\":600}"))
			.andExpect(status().isOk());
		long pedido = crearPedido(cliente, pedidoDelivery());

		// Dos tortas de 10 horas: estarían listas mañana a las 6:00, antes de las 10:00.
		mvc.perform(con(admin, get("/api/pedidos/" + pedido + "/estimacion")))
			.andExpect(jsonPath("$.minutosPedido").value(1200))
			.andExpect(jsonPath("$.alcanzaElTiempo").value(true));
		reloj.adelantar(java.time.Duration.ofHours(5));
		// La sesión del Panel venció por inactividad (RNF06).
		admin = iniciarSesion("admin@dulces.bo");
		mvc.perform(con(admin, get("/api/pedidos/" + pedido + "/estimacion")))
			.andExpect(jsonPath("$.alcanzaElTiempo").value(false));
	}

	@Test
	void elClienteNoVeLaEstimacion() throws Exception {
		long pedido = crearPedido(cliente, pedidoSimple(torta));
		mvc.perform(con(cliente, get("/api/pedidos/" + pedido + "/estimacion"))).andExpect(status().isForbidden());
		mvc.perform(con(cliente, get("/api/productos/" + torta + "/produccion"))).andExpect(status().isForbidden());
	}

	private long insumo(String nombre, String unidad, String stock) throws Exception {
		long id = id(mvc.perform(con(admin, post("/api/inventario"))
			.content("{\"nombre\":\"%s\",\"unidad\":\"%s\",\"minimo\":0}".formatted(nombre, unidad))));
		if (!stock.equals("0")) {
			mvc.perform(con(admin, post("/api/inventario/" + id + "/movimientos"))
				.content("{\"tipo\":\"ENTRADA\",\"cantidad\":%s,\"motivo\":\"Compra\"}".formatted(stock)))
				.andExpect(status().isOk());
		}
		return id;
	}

}
