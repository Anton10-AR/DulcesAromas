package bo.dulcesaromas.ecommerce;

import bo.dulcesaromas.PruebaPedidos;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU10: armar el carrito y confirmar el pedido (CU05, RN08, RN11, RN12, RN15, RN17).
 */
class PedidosTests extends PruebaPedidos {

	/** 2 tortas Grande con Fresa y 3 galletas, con delivery. */
	private String carrito() {
		return """
				{"lineas":[{"productoId":%d,"cantidad":2,"opciones":[%d,%d],"mensaje":"Feliz cumple, Ana"},
				           {"productoId":%d,"cantidad":3}],
				 "tipoEntrega":"DELIVERY","direccion":"Calle Junín 123","telefono":"70012345",
				 "horarioEntrega":"%s"}""".formatted(torta, grande, fresa, galletas, MANANA);
	}

	private int filas(String tabla) {
		return jdbc.queryForObject("select count(*) from " + tabla, Integer.class);
	}

	@Test
	void rn11ElTotalSumaPrecioPorCantidadRecargosPorUnidadYEnvio() throws Exception {
		// Torta: (120 + 40 + 10) × 2 = 340. Galletas: 15 × 3 = 45. Envío: 10. Total: 395.
		mvc.perform(post("/api/pedidos/cotizacion").contentType(MediaType.APPLICATION_JSON).content(carrito()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.lineas[0].subtotal").value(340.00))
			.andExpect(jsonPath("$.lineas[0].opciones[*].nombre", contains("Grande", "Fresa")))
			.andExpect(jsonPath("$.lineas[1].subtotal").value(45.00))
			.andExpect(jsonPath("$.costoEnvio").value(10.00))
			.andExpect(jsonPath("$.total").value(395.00))
			.andExpect(jsonPath("$.confirmable").value(true));
	}

	@Test
	void elRecojoNoPagaEnvio() throws Exception {
		mvc.perform(post("/api/pedidos/cotizacion").contentType(MediaType.APPLICATION_JSON).content(pedidoSimple(galletas)))
			.andExpect(jsonPath("$.costoEnvio").value(0))
			.andExpect(jsonPath("$.total").value(15.00));
	}

	@Test
	void d4LaTarifaDeEnvioLaCambiaElAdministrador() throws Exception {
		mvc.perform(con(admin, put("/api/configuracion")).content("{\"tarifaEnvio\":15.50}")).andExpect(status().isOk());

		mvc.perform(post("/api/pedidos/cotizacion").contentType(MediaType.APPLICATION_JSON).content(carrito()))
			.andExpect(jsonPath("$.costoEnvio").value(15.50))
			.andExpect(jsonPath("$.total").value(400.50));
		mvc.perform(con(cliente, put("/api/configuracion")).content("{\"tarifaEnvio\":0}")).andExpect(status().isForbidden());
	}

	@Test
	void elPedidoNacePendienteConSuCobroPendienteDePago() throws Exception {
		pedir(cliente, carrito()).andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("PENDIENTE"))
			.andExpect(jsonPath("$.total").value(395.00))
			.andExpect(jsonPath("$.lineas[0].mensaje").value("Feliz cumple, Ana"))
			.andExpect(jsonPath("$.historial[0].nuevo").value("PENDIENTE"));

		var pago = jdbc.queryForMap("select estado, monto from pago");
		assertThat(pago.get("estado")).isEqualTo("PENDIENTE_DE_PAGO");
		assertThat(pago.get("monto").toString()).isEqualTo("395.00");
	}

	@Test
	void rn15ParaPedirHaceFaltaUnaCuentaDeCliente() throws Exception {
		mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(carrito()))
			.andExpect(status().isUnauthorized());
		pedir(admin, carrito()).andExpect(status().isForbidden());
	}

	@Test
	void rn08NoSeConfirmaConProductosNoDisponibles() throws Exception {
		mvc.perform(con(admin, put("/api/productos/" + galletas + "/disponibilidad")).content("{\"disponible\":false}"))
			.andExpect(status().isOk());

		mvc.perform(post("/api/pedidos/cotizacion").contentType(MediaType.APPLICATION_JSON).content(carrito()))
			.andExpect(jsonPath("$.confirmable").value(false))
			.andExpect(jsonPath("$.lineas[1].disponible").value(false))
			.andExpect(jsonPath("$.noDisponibles", contains("Galletas de avena")));
		pedir(cliente, carrito()).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("PRODUCTOS_NO_DISPONIBLES"))
			.andExpect(jsonPath("$.mensaje").value(containsString("Galletas de avena")));

		assertThat(filas("pedido")).isZero();
		assertThat(filas("pago")).isZero();
	}

	@Test
	void rn08UnProductoDesactivadoTampocoSePide() throws Exception {
		mvc.perform(con(admin, post("/api/productos/" + torta + "/desactivar"))).andExpect(status().isOk());

		pedir(cliente, pedidoSimple(torta)).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("PRODUCTOS_NO_DISPONIBLES"));
	}

	@Test
	void rn12ElDeliveryExigeDireccionTelefonoYHorario() throws Exception {
		pedir(cliente, """
				{"lineas":[{"productoId":%d,"cantidad":1}],"tipoEntrega":"DELIVERY"}""".formatted(galletas))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.direccion").isNotEmpty())
			.andExpect(jsonPath("$.campos.telefono").isNotEmpty())
			.andExpect(jsonPath("$.campos.horarioEntrega").isNotEmpty());
	}

	@Test
	void rn12ElRecojoNoExigeDireccionPeroSiTelefonoYHorario() throws Exception {
		pedir(cliente, pedidoSimple(galletas)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.direccion").doesNotExist());

		pedir(cliente, """
				{"lineas":[{"productoId":%d,"cantidad":1}],"tipoEntrega":"RECOJO"}""".formatted(galletas))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.telefono").isNotEmpty())
			.andExpect(jsonPath("$.campos.horarioEntrega").isNotEmpty())
			.andExpect(jsonPath("$.campos.direccion").doesNotExist());
	}

	@Test
	void rn10UnProductoConPedidosNoSeBorraSoloSeDesactiva() throws Exception {
		crearPedido(cliente, pedidoSimple(galletas));

		mvc.perform(con(admin, delete("/api/productos/" + galletas))).andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("PRODUCTO_CON_PEDIDOS"));
		mvc.perform(con(admin, post("/api/productos/" + galletas + "/desactivar"))).andExpect(status().isOk());

		assertThat(filas("producto")).isEqualTo(2);
	}

	@Test
	void elHorarioEsConAlMenos24HorasDeAnticipacion() throws Exception {
		// El reloj de prueba marca el lunes 02/11 a las 10:00 en La Paz.
		pedir(cliente, pedidoSimple(galletas).replace(MANANA, "2026-11-03T09:30:00-04:00"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.horarioEntrega").value(containsString("24 horas")));
		pedir(cliente, pedidoSimple(galletas)).andExpect(status().isCreated());
	}

	@Test
	void elHorarioCaeDentroDeLaAtencion() throws Exception {
		for (String fueraDeHorario : new String[] { "2026-11-04T08:59:00-04:00", "2026-11-04T20:30:00-04:00",
				"2026-11-08T15:00:00-04:00" }) {
			pedir(cliente, pedidoSimple(galletas).replace(MANANA, fueraDeHorario)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.horarioEntrega").value(containsString("horario de atención")));
		}
		// Domingo 08/11 a las 11:00 y miércoles 04/11 a las 20:00 sí.
		pedir(cliente, pedidoSimple(galletas).replace(MANANA, "2026-11-08T11:00:00-04:00")).andExpect(status().isCreated());
		pedir(cliente, pedidoSimple(galletas).replace(MANANA, "2026-11-04T20:00:00-04:00")).andExpect(status().isCreated());
	}

	@Test
	void elHorarioDeEntregaNoPuedeHaberPasado() throws Exception {
		pedir(cliente, pedidoSimple(galletas).replace(MANANA, "2026-11-02T09:00:00-04:00"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.horarioEntrega").isNotEmpty());
	}

	@Test
	void rn17ElMensajeTieneHasta24CaracteresYNoTieneRecargo() throws Exception {
		String veinticuatro = "Feliz cumpleaños, Lucía!";
		assertThat(veinticuatro).hasSize(24);
		String linea = "{\"lineas\":[{\"productoId\":%d,\"cantidad\":1,\"opciones\":[%s],\"mensaje\":\"%s\"}]}";

		mvc.perform(post("/api/pedidos/cotizacion").contentType(MediaType.APPLICATION_JSON)
			.content(linea.formatted(torta, pequena, veinticuatro)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.total").value(120.00));
		mvc.perform(post("/api/pedidos/cotizacion").contentType(MediaType.APPLICATION_JSON)
			.content(linea.formatted(torta, pequena, veinticuatro + "!")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos['lineas[0].mensaje']").isNotEmpty());
		mvc.perform(post("/api/pedidos/cotizacion").contentType(MediaType.APPLICATION_JSON)
			.content(linea.formatted(galletas, "", "Hola")))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("MENSAJE_NO_PERMITIDO"));
	}

	@Test
	void seEligeComoMaximoUnaOpcionDeCadaTipo() throws Exception {
		String linea = "{\"lineas\":[{\"productoId\":%d,\"cantidad\":1,\"opciones\":[%s]}]}";

		mvc.perform(post("/api/pedidos/cotizacion").contentType(MediaType.APPLICATION_JSON)
			.content(linea.formatted(torta, grande + "," + mediana)))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("OPCION_REPETIDA"));
		mvc.perform(post("/api/pedidos/cotizacion").contentType(MediaType.APPLICATION_JSON)
			.content(linea.formatted(galletas, String.valueOf(grande))))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("OPCION_INVALIDA"));
		mvc.perform(post("/api/pedidos/cotizacion").contentType(MediaType.APPLICATION_JSON)
			.content(linea.formatted(torta, String.valueOf(pequena))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.total").value(120.00));
	}

	@Test
	void elPersonalVeElNombreDelClienteEnLaListaYEnElDetalle() throws Exception {
		long pedido = crearPedido(cliente, pedidoSimple(torta));

		mvc.perform(con(admin, get("/api/pedidos"))).andExpect(status().isOk())
			.andExpect(jsonPath("$[0].cliente").value("ana@correo.bo"));
		mvc.perform(con(admin, get("/api/pedidos/" + pedido))).andExpect(status().isOk())
			.andExpect(jsonPath("$.cliente").value("ana@correo.bo"));
	}

	@Test
	void siElProductoTieneTamanosHayQueElegirUno() throws Exception {
		String linea = "{\"lineas\":[{\"productoId\":%d,\"cantidad\":1,\"opciones\":[%s]}]}";

		mvc.perform(post("/api/pedidos/cotizacion").contentType(MediaType.APPLICATION_JSON)
			.content(linea.formatted(torta, "")))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("TAMANO_REQUERIDO"));
		mvc.perform(post("/api/pedidos/cotizacion").contentType(MediaType.APPLICATION_JSON)
			.content(linea.formatted(torta, String.valueOf(fresa))))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("TAMANO_REQUERIDO"));
		pedir(cliente, """
				{"lineas":[{"productoId":%d,"cantidad":1,"opciones":[%d]}],"tipoEntrega":"RECOJO",
				 "telefono":"70012345","horarioEntrega":"%s"}""".formatted(torta, fresa, MANANA))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("TAMANO_REQUERIDO"));
		// Un producto sin tamaños no pide ninguno.
		mvc.perform(post("/api/pedidos/cotizacion").contentType(MediaType.APPLICATION_JSON)
			.content(linea.formatted(galletas, "")))
			.andExpect(status().isOk());
	}

	@Test
	void elPedidoGuardaCopiaDelPrecioYDeLasOpciones() throws Exception {
		long pedido = crearPedido(cliente, carrito());

		mvc.perform(con(admin, put("/api/productos/" + torta)).content("""
				{"nombre":"Torta nueva","descripcion":"Otra","precio":999,"categoriaId":%d,"disponible":true,
				 "personalizable":true,"opciones":[{"tipo":"Tamaño","nombre":"Gigante","recargo":500}]}"""
			.formatted(jdbc.queryForObject("select id from categoria", Long.class))))
			.andExpect(status().isOk());

		mvc.perform(con(cliente, get("/api/pedidos/" + pedido))).andExpect(status().isOk())
			.andExpect(jsonPath("$.lineas[0].nombre").value("Torta de chocolate"))
			.andExpect(jsonPath("$.lineas[0].precioUnitario").value(120.00))
			.andExpect(jsonPath("$.lineas[0].opciones[0].nombre").value("Grande"))
			.andExpect(jsonPath("$.lineas[0].opciones[0].recargo").value(40.00))
			.andExpect(jsonPath("$.total").value(395.00));
	}

	@Test
	void unClienteSoloVeSusPedidosYElPersonalNecesitaPermiso() throws Exception {
		long pedido = crearPedido(cliente, pedidoSimple(galletas));
		crearCliente("otro@correo.bo");
		crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		crearPersonal("repartidor@dulces.bo", "REPARTIDOR");

		mvc.perform(con(iniciarSesion("otro@correo.bo"), get("/api/pedidos/" + pedido))).andExpect(status().isNotFound());
		mvc.perform(con(iniciarSesion("vendedor@dulces.bo"), get("/api/pedidos/" + pedido))).andExpect(status().isOk());
		mvc.perform(con(iniciarSesion("repartidor@dulces.bo"), get("/api/pedidos/" + pedido)))
			.andExpect(status().isForbidden());
	}

}
