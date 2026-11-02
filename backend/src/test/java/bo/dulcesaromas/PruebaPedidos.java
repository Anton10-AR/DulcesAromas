package bo.dulcesaromas;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas de pedidos y cobros. Deja un catálogo con:
 * <ul>
 * <li>una torta personalizable de Bs 120.00 con Tamaño Grande (+40), Tamaño Mediana (+20) y Relleno Fresa (+10),</li>
 * <li>galletas de Bs 15.00 sin personalización,</li>
 * </ul>
 * un administrador, un cliente con sesión y la tarifa de envío inicial de Bs 10.00.
 */
public abstract class PruebaPedidos extends PruebaIntegracion {

	/** Mañana a las 10:00 en La Paz; el reloj de prueba marca hoy a las 10:00. */
	protected static final String MANANA = "2026-11-03T10:00:00-04:00";

	protected String admin;

	protected String cliente;

	protected long clienteId;

	protected long torta;

	protected long galletas;

	protected long grande;

	protected long mediana;

	protected long fresa;

	@BeforeEach
	void catalogoYCuentas() throws Exception {
		crearPersonal("admin@dulces.bo", "ADMINISTRADOR");
		admin = iniciarSesion("admin@dulces.bo");
		clienteId = crearCliente("ana@correo.bo");
		cliente = iniciarSesion("ana@correo.bo");

		long tortas = id(mvc.perform(con(admin, post("/api/categorias")).content("{\"nombre\":\"Tortas\"}")));
		String cuerpo = mvc.perform(con(admin, post("/api/productos")).content("""
				{"nombre":"Torta de chocolate","descripcion":"Húmeda","precio":120.00,"categoriaId":%d,
				 "disponible":true,"personalizable":true,"opciones":[
				   {"tipo":"Tamaño","nombre":"Grande","recargo":40},
				   {"tipo":"Tamaño","nombre":"Mediana","recargo":20},
				   {"tipo":"Relleno","nombre":"Fresa","recargo":10}]}""".formatted(tortas)))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		torta = numero(cuerpo, "$.id");
		grande = numero(cuerpo, "$.opciones[0].id");
		mediana = numero(cuerpo, "$.opciones[1].id");
		fresa = numero(cuerpo, "$.opciones[2].id");
		galletas = id(mvc.perform(con(admin, post("/api/productos")).content("""
				{"nombre":"Galletas de avena","descripcion":"Docena","precio":15.00,"categoriaId":%d,
				 "disponible":true,"personalizable":false}""".formatted(tortas))));
	}

	/**
	 * Un pedido con recojo de una torta sin opciones, con todos los datos de entrega.
	 */
	protected static String pedidoSimple(long productoId) {
		return """
				{"lineas":[{"productoId":%d,"cantidad":1}],"tipoEntrega":"RECOJO",
				 "telefono":"70012345","horarioEntrega":"%s"}""".formatted(productoId, MANANA);
	}

	/**
	 * Un pedido con delivery de dos tortas Grande, del {@link #cliente}.
	 */
	protected String pedidoDelivery() {
		return """
				{"lineas":[{"productoId":%d,"cantidad":2,"opciones":[%d]}],"tipoEntrega":"DELIVERY",
				 "direccion":"Calle Junín 123","telefono":"70012345","horarioEntrega":"%s"}""".formatted(torta, grande, MANANA);
	}

	/**
	 * El cliente paga por transferencia y el administrador aprueba: el pedido queda Confirmado.
	 */
	protected void pagarYAprobar(long pedidoId) throws Exception {
		long transferencia = jdbc.queryForObject("select id from metodo_pago where nombre_clave = 'transferencia'",
				Long.class);
		jdbc.update("update metodo_pago set instrucciones = 'Cuenta 1-234567', activo = true where id = ?", transferencia);
		mvc.perform(multipart("/api/pagos").param("pedidoId", String.valueOf(pedidoId))
			.param("metodoPagoId", String.valueOf(transferencia)).param("referencia", "OP-1")
			.file(new MockMultipartFile("comprobante", "c.pdf", "application/pdf", "%PDF-1.7".getBytes()))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
			.andExpect(status().isOk());
		long pago = jdbc.queryForObject("select max(id) from pago where pedido_id = ?", Long.class, pedidoId);
		mvc.perform(con(admin, post("/api/pagos/" + pago + "/aprobar"))).andExpect(status().isOk());
	}

	protected ResultActions pedir(String token, String json) throws Exception {
		return mvc.perform(con(token, post("/api/pedidos")).content(json));
	}

	protected long crearPedido(String token, String json) throws Exception {
		return id(pedir(token, json));
	}

	protected static long id(ResultActions respuesta) throws Exception {
		return numero(respuesta.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
	}

	protected static long numero(String json, String ruta) {
		return ((Number) JsonPath.read(json, ruta)).longValue();
	}

}
