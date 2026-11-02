package bo.dulcesaromas.distribucion;

import bo.dulcesaromas.PruebaPedidos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU13: preparar, asignar y entregar (CU08, RN05, RN13, RNF16, D3).
 */
class EntregasTests extends PruebaPedidos {

	private long repartidorA;

	private long repartidorB;

	private String vendedor;

	@BeforeEach
	void personal() throws Exception {
		repartidorA = crearPersonal("rosa@dulces.bo", "REPARTIDOR");
		repartidorB = crearPersonal("beto@dulces.bo", "REPARTIDOR");
		crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		vendedor = iniciarSesion("vendedor@dulces.bo");
	}

	private ResultActions accion(String token, String ruta) throws Exception {
		return mvc.perform(con(token, post(ruta)));
	}

	private ResultActions asignar(String token, long pedido, long repartidor) throws Exception {
		return mvc.perform(con(token, put("/api/entregas/" + pedido + "/repartidor"))
			.content("{\"repartidorId\":" + repartidor + "}"));
	}

	/**
	 * Un pedido con delivery, pagado y preparado: queda Listo.
	 */
	private long pedidoListo() throws Exception {
		long pedido = crearPedido(cliente, pedidoDelivery());
		pagarYAprobar(pedido);
		accion(admin, "/api/pedidos/" + pedido + "/en-preparacion").andExpect(status().isOk());
		accion(admin, "/api/pedidos/" + pedido + "/listo").andExpect(status().isOk());
		return pedido;
	}

	private String estado(long pedido) {
		return jdbc.queryForObject("select estado from pedido where id = ?", String.class, pedido);
	}

	@Test
	void rn05SoloSePreparaUnPedidoConElPagoAprobado() throws Exception {
		long pedido = crearPedido(cliente, pedidoDelivery());

		accion(admin, "/api/pedidos/" + pedido + "/en-preparacion").andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("TRANSICION_NO_PERMITIDA"));

		pagarYAprobar(pedido);
		accion(admin, "/api/pedidos/" + pedido + "/en-preparacion").andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("EN_PREPARACION"));
		accion(admin, "/api/pedidos/" + pedido + "/listo").andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("LISTO"));
	}

	@Test
	void d3ElVendedorMarcaLaPreparacionSoloConElPermiso() throws Exception {
		long pedido = crearPedido(cliente, pedidoDelivery());
		pagarYAprobar(pedido);

		accion(vendedor, "/api/pedidos/" + pedido + "/en-preparacion").andExpect(status().isForbidden());

		jdbc.update("insert into rol_permiso (rol, permiso) values ('VENDEDOR', 'PEDIDOS_PREPARAR')");
		accion(vendedor, "/api/pedidos/" + pedido + "/en-preparacion").andExpect(status().isOk());
	}

	@Test
	void rnf16ElRepartidorVeSoloSusEntregasConDireccionYTelefono() throws Exception {
		long pedido = pedidoListo();
		asignar(vendedor, pedido, repartidorA).andExpect(status().isOk())
			.andExpect(jsonPath("$.repartidor.nombre").value("rosa@dulces.bo"));
		String rosa = iniciarSesion("rosa@dulces.bo");
		String beto = iniciarSesion("beto@dulces.bo");

		mvc.perform(con(rosa, get("/api/entregas/mias"))).andExpect(status().isOk())
			.andExpect(jsonPath("$[0].pedidoId").value(pedido))
			.andExpect(jsonPath("$[0].direccion").value("Calle Junín 123"))
			.andExpect(jsonPath("$[0].telefono").value("70012345"))
			.andExpect(jsonPath("$[0].cliente").value("ana@correo.bo"))
			.andExpect(jsonPath("$[0].productos", contains("2 × Torta de chocolate (Grande)")));
		mvc.perform(con(beto, get("/api/entregas/mias"))).andExpect(jsonPath("$.length()").value(0));

		mvc.perform(con(beto, get("/api/entregas/" + pedido))).andExpect(status().isForbidden());
		accion(beto, "/api/entregas/" + pedido + "/en-camino").andExpect(status().isForbidden());
		mvc.perform(con(beto, get("/api/entregas"))).andExpect(status().isForbidden());
		mvc.perform(con(beto, get("/api/pedidos/" + pedido))).andExpect(status().isForbidden());
		assertThat(registrosDeAuditoria("ACCESO_DENEGADO")).isEqualTo(4);
	}

	@Test
	void elRepartidorLlevaElPedidoHastaEntregado() throws Exception {
		long pedido = pedidoListo();
		asignar(vendedor, pedido, repartidorA).andExpect(status().isOk());
		String rosa = iniciarSesion("rosa@dulces.bo");

		accion(rosa, "/api/entregas/" + pedido + "/entregado").andExpect(status().isConflict());
		accion(rosa, "/api/entregas/" + pedido + "/en-camino").andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("EN_CAMINO"));
		accion(rosa, "/api/entregas/" + pedido + "/entregado").andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("ENTREGADO"));

		mvc.perform(con(rosa, get("/api/entregas/mias"))).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void rn13UnaEntregaFallidaExigeObservacionYVuelveAListo() throws Exception {
		long pedido = pedidoListo();
		asignar(vendedor, pedido, repartidorA).andExpect(status().isOk());
		String rosa = iniciarSesion("rosa@dulces.bo");
		accion(rosa, "/api/entregas/" + pedido + "/en-camino").andExpect(status().isOk());

		mvc.perform(con(rosa, post("/api/entregas/" + pedido + "/fallida")).content("{\"observacion\":\" \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.observacion").isNotEmpty());
		mvc.perform(con(rosa, post("/api/entregas/" + pedido + "/fallida")).content("{\"observacion\":\"No había nadie\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("LISTO"))
			.andExpect(jsonPath("$.observacionFallo").value("No había nadie"));

		// Vuelve a Listo para reasignarlo o reintentar.
		asignar(vendedor, pedido, repartidorB).andExpect(status().isOk());
		mvc.perform(con(admin, get("/api/pedidos/" + pedido)))
			.andExpect(jsonPath("$.historial[-1:].observacion", contains("No había nadie")));
	}

	@Test
	void elRepartidorSeCambiaAntesDeQueElPedidoSalgaYQuedaRegistrado() throws Exception {
		long pedido = pedidoListo();
		asignar(vendedor, pedido, repartidorA).andExpect(status().isOk());

		asignar(admin, pedido, repartidorB).andExpect(status().isOk())
			.andExpect(jsonPath("$.repartidor.id").value(repartidorB))
			.andExpect(jsonPath("$.asignaciones[*].repartidor", contains("rosa@dulces.bo", "beto@dulces.bo")))
			.andExpect(jsonPath("$.asignaciones[1].asignadoPor").value("admin@dulces.bo"));

		accion(iniciarSesion("beto@dulces.bo"), "/api/entregas/" + pedido + "/en-camino").andExpect(status().isOk());
		asignar(vendedor, pedido, repartidorA).andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("ASIGNACION_NO_PERMITIDA"));
	}

	@Test
	void soloSeAsignaAUnRepartidorActivoYAUnPedidoPagado() throws Exception {
		long pendiente = crearPedido(cliente, pedidoDelivery());
		asignar(vendedor, pendiente, repartidorA).andExpect(status().isConflict());

		long pedido = pedidoListo();
		long vendedorId = idDe("vendedor@dulces.bo");
		asignar(vendedor, pedido, vendedorId).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.campos.repartidorId").isNotEmpty());
		jdbc.update("update usuario set activo = false where id = ?", repartidorA);
		asignar(vendedor, pedido, repartidorA).andExpect(status().isUnprocessableContent());

		mvc.perform(con(vendedor, get("/api/entregas/repartidores")))
			.andExpect(jsonPath("$[*].nombre", contains("beto@dulces.bo")));
	}

	@Test
	void unPedidoNoSaleSinRepartidor() throws Exception {
		long pedido = pedidoListo();

		accion(admin, "/api/entregas/" + pedido + "/en-camino").andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("SIN_REPARTIDOR"));
	}

	@Test
	void elRecojoLoCierraElVendedorSinRepartidor() throws Exception {
		long pedido = crearPedido(cliente, pedidoSimple(torta));
		pagarYAprobar(pedido);
		accion(admin, "/api/pedidos/" + pedido + "/en-preparacion").andExpect(status().isOk());

		accion(vendedor, "/api/pedidos/" + pedido + "/retirado").andExpect(status().isConflict());
		accion(admin, "/api/pedidos/" + pedido + "/listo").andExpect(status().isOk());
		asignar(vendedor, pedido, repartidorA).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("SOLO_DELIVERY"));
		accion(iniciarSesion("rosa@dulces.bo"), "/api/pedidos/" + pedido + "/retirado").andExpect(status().isForbidden());

		accion(vendedor, "/api/pedidos/" + pedido + "/retirado").andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("ENTREGADO"));
	}

	@Test
	void unDeliveryNoSeCierraComoRecojo() throws Exception {
		long pedido = pedidoListo();

		accion(vendedor, "/api/pedidos/" + pedido + "/retirado").andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("NO_ES_RECOJO"));
		assertThat(estado(pedido)).isEqualTo("LISTO");
	}

	@Test
	void cadaCambioQuedaEnElHistorialConQuienLoHizo() throws Exception {
		long pedido = pedidoListo();
		asignar(vendedor, pedido, repartidorA).andExpect(status().isOk());
		String rosa = iniciarSesion("rosa@dulces.bo");
		accion(rosa, "/api/entregas/" + pedido + "/en-camino").andExpect(status().isOk());
		accion(rosa, "/api/entregas/" + pedido + "/entregado").andExpect(status().isOk());

		mvc.perform(con(admin, get("/api/pedidos/" + pedido)))
			.andExpect(jsonPath("$.historial[*].nuevo",
					contains("PENDIENTE", "CONFIRMADO", "EN_PREPARACION", "LISTO", "EN_CAMINO", "ENTREGADO")))
			.andExpect(jsonPath("$.historial[*].usuario", contains("ana@correo.bo", "admin@dulces.bo",
					"admin@dulces.bo", "admin@dulces.bo", "rosa@dulces.bo", "rosa@dulces.bo")));
	}

}
