package bo.dulcesaromas.ecommerce;

import bo.dulcesaromas.PruebaPedidos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU21: el vendedor registra un pedido a nombre de un cliente (CU17, RN06, D7).
 */
class PedidoAsistidoTests extends PruebaPedidos {

	private String vendedor;

	@BeforeEach
	void vendedor() throws Exception {
		crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		vendedor = iniciarSesion("vendedor@dulces.bo");
		jdbc.update("update usuario set telefono = '70055511' where id = ?", clienteId);
	}

	private ResultActions pedirAsistido(long cliente, String pedido) throws Exception {
		return mvc.perform(con(vendedor, post("/api/pedidos/asistidos"))
			.content("{\"clienteId\":%d,\"pedido\":%s}".formatted(cliente, pedido)));
	}

	private ResultActions registrarPago(String token, long pedido) throws Exception {
		long transferencia = jdbc.queryForObject("select id from metodo_pago where nombre_clave = 'transferencia'", Long.class);
		jdbc.update("update metodo_pago set instrucciones = 'Cuenta 1', activo = true where id = ?", transferencia);
		return mvc.perform(multipart("/api/pagos").param("pedidoId", String.valueOf(pedido))
			.param("metodoPagoId", String.valueOf(transferencia)).param("referencia", "MOSTRADOR-1")
			.file(new MockMultipartFile("comprobante", "c.pdf", "application/pdf", "%PDF-1.7".getBytes()))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	@Test
	void buscaAlClientePorCorreoOTelefono() throws Exception {
		mvc.perform(con(vendedor, get("/api/clientes").param("buscar", "ANA@corr")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].correo", contains("ana@correo.bo")));
		mvc.perform(con(vendedor, get("/api/clientes").param("buscar", "5551")))
			.andExpect(jsonPath("$[*].telefono", contains("70055511")));
		mvc.perform(con(vendedor, get("/api/clientes").param("buscar", "%%%")))
			.andExpect(jsonPath("$.length()").value(0));
		mvc.perform(con(cliente, get("/api/clientes").param("buscar", "ana"))).andExpect(status().isForbidden());
	}

	@Test
	void registraAUnClienteNuevoQueDefinePorSiMismoSuContrasena() throws Exception {
		long nuevo = id(mvc.perform(con(vendedor, post("/api/clientes/asistidos"))
			.content("{\"nombre\":\"Juan Pérez\",\"correo\":\"juan@correo.bo\",\"telefono\":\"71234567\"}")));

		assertThat(jdbc.queryForObject("select tipo from usuario where id = ?", String.class, nuevo)).isEqualTo("CLIENTE");
		mvc.perform(post("/api/auth/recuperacion").contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"juan@correo.bo\"}"))
			.andExpect(status().isAccepted());
		assertThat(correos.enviados()).singleElement().satisfies(c -> assertThat(c.para()).isEqualTo("juan@correo.bo"));

		mvc.perform(con(vendedor, post("/api/clientes/asistidos"))
			.content("{\"nombre\":\"Otra Ana\",\"correo\":\"ana@correo.bo\",\"telefono\":\"71234567\"}"))
			.andExpect(status().isConflict());
	}

	@Test
	void elPedidoQuedaMarcadoComoAsistidoYConElNombreDeQuienLoRegistro() throws Exception {
		long pedido = id(pedirAsistido(clienteId, pedidoSimple(torta))
			.andExpect(jsonPath("$.asistido").value(true))
			.andExpect(jsonPath("$.estado").value("PENDIENTE"))
			.andExpect(jsonPath("$.historial[0].usuario").value("vendedor@dulces.bo")));

		mvc.perform(con(cliente, get("/api/pedidos/" + pedido))).andExpect(status().isOk());
		mvc.perform(con(cliente, get("/api/pedidos/mios"))).andExpect(jsonPath("$[0].asistido").value(true));
	}

	@Test
	void seAplicanLasMismasReglasQueEnLaTienda() throws Exception {
		mvc.perform(con(admin, put("/api/productos/" + torta + "/disponibilidad")).content("{\"disponible\":false}"))
			.andExpect(status().isOk());

		pedirAsistido(clienteId, pedidoSimple(torta)).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("PRODUCTOS_NO_DISPONIBLES"));
		pedirAsistido(clienteId, "{\"lineas\":[{\"productoId\":%d,\"cantidad\":1}],\"tipoEntrega\":\"DELIVERY\"}"
			.formatted(galletas))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.direccion").isNotEmpty());
		pedirAsistido(9999, pedidoSimple(galletas)).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.campos.clienteId").isNotEmpty());
	}

	@Test
	void rn06ElVendedorRegistraElPagoPeroNoLoVerifica() throws Exception {
		jdbc.update("insert into rol_permiso (rol, permiso) values ('VENDEDOR', 'COBRANZA_VERIFICAR')");
		long pedido = id(pedirAsistido(clienteId, pedidoSimple(torta)));

		registrarPago(vendedor, pedido).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_VERIFICACION"));
		long pago = jdbc.queryForObject("select max(id) from pago where pedido_id = ?", Long.class, pedido);

		mvc.perform(con(vendedor, post("/api/pagos/" + pago + "/aprobar"))).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.codigo").value("VERIFICACION_PROPIA"));
		mvc.perform(con(admin, post("/api/pagos/" + pago + "/aprobar"))).andExpect(status().isOk());
	}

	@Test
	void elPersonalSoloRegistraElPagoDeUnPedidoAsistido() throws Exception {
		long pedido = crearPedido(cliente, pedidoSimple(torta));

		registrarPago(vendedor, pedido).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("NO_ES_ASISTIDO"));
	}

}
