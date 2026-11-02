package bo.dulcesaromas.cobros;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import bo.dulcesaromas.PruebaPedidos;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU11 y HU12: registrar un pago con su comprobante y verificarlo (CU06, CU07, RN05, RN06, RN07, RNF09, RNF15).
 */
class PagosTests extends PruebaPedidos {

	private static final byte[] PDF = "%PDF-1.7\n1 0 obj\n".getBytes();

	private long transferencia;

	private long pedido;

	@BeforeEach
	void cobroAbierto() throws Exception {
		transferencia = jdbc.queryForObject("select id from metodo_pago where nombre_clave = 'transferencia'", Long.class);
		mvc.perform(con(admin, put("/api/metodos-pago/" + transferencia))
			.content("{\"nombre\":\"Transferencia\",\"instrucciones\":\"Banco Unión, cuenta 1-234567, Dulces Aromas\"}"))
			.andExpect(status().isOk());
		mvc.perform(con(admin, post("/api/metodos-pago/" + transferencia + "/activar"))).andExpect(status().isOk());
		pedido = crearPedido(cliente, pedidoSimple(torta));
	}

	private ResultActions registrarPago(String token, long pedidoId, String referencia, byte[] comprobante)
			throws Exception {
		var solicitud = multipart("/api/pagos").param("pedidoId", String.valueOf(pedidoId))
			.param("metodoPagoId", String.valueOf(transferencia))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
		if (referencia != null) {
			solicitud.param("referencia", referencia);
		}
		if (comprobante != null) {
			solicitud.file(new MockMultipartFile("comprobante", "comprobante", "application/octet-stream", comprobante));
		}
		return mvc.perform(solicitud);
	}

	private long pagoVigente() {
		return jdbc.queryForObject("select max(id) from pago where pedido_id = ?", Long.class, pedido);
	}

	private String estadoDelPedido() {
		return jdbc.queryForObject("select estado from pedido where id = ?", String.class, pedido);
	}

	private ResultActions aprobar(String token, long pagoId) throws Exception {
		return mvc.perform(con(token, post("/api/pagos/" + pagoId + "/aprobar")));
	}

	private ResultActions rechazar(String token, long pagoId, String observacion) throws Exception {
		return mvc.perform(con(token, post("/api/pagos/" + pagoId + "/rechazar"))
			.content("{\"observacion\":%s}".formatted(observacion == null ? "null" : "\"" + observacion + "\"")));
	}

	@Test
	void elClienteVeLosMetodosActivosConLosDatosParaPagar() throws Exception {
		mvc.perform(con(cliente, get("/api/metodos-pago"))).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].nombre", contains("Transferencia")))
			.andExpect(jsonPath("$[0].instrucciones").value("Banco Unión, cuenta 1-234567, Dulces Aromas"));
	}

	@Test
	void alRegistrarElComprobanteElPagoPasaAEnVerificacionYElPedidoSiguePendiente() throws Exception {
		registrarPago(cliente, pedido, "OP-778899", PDF).andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("EN_VERIFICACION"))
			.andExpect(jsonPath("$.metodo").value("Transferencia"))
			.andExpect(jsonPath("$.monto").value(120.00));

		assertThat(estadoDelPedido()).isEqualTo("PENDIENTE");
		mvc.perform(con(cliente, get("/api/pagos/" + pagoVigente() + "/comprobante"))).andExpect(status().isOk())
			.andExpect(content().bytes(PDF));
	}

	@Test
	void rnf09ElComprobanteEsJpgPngOPdfDeHasta5Mb() throws Exception {
		byte[] grande = new byte[5 * 1024 * 1024 + 1];
		System.arraycopy(PDF, 0, grande, 0, PDF.length);

		for (byte[] malo : new byte[][] { "GIF89a".getBytes(), grande, new byte[0] }) {
			registrarPago(cliente, pedido, "OP-1", malo).andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.codigo").value("COMPROBANTE_INVALIDO"));
		}
		assertThat(jdbc.queryForObject("select estado from pago", String.class)).isEqualTo("PENDIENTE_DE_PAGO");
	}

	@Test
	void faltaLaReferenciaOElComprobante() throws Exception {
		registrarPago(cliente, pedido, null, PDF).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.referencia").isNotEmpty());
		registrarPago(cliente, pedido, "OP-1", null).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.comprobante").isNotEmpty());
	}

	@Test
	void unClienteNoPagaNiVeElPedidoDeOtro() throws Exception {
		crearCliente("otro@correo.bo");
		String otro = iniciarSesion("otro@correo.bo");

		registrarPago(otro, pedido, "OP-1", PDF).andExpect(status().isNotFound());
		mvc.perform(con(otro, get("/api/pagos").param("pedido", String.valueOf(pedido)))).andExpect(status().isNotFound());
	}

	@Test
	void noSeRegistraOtroComprobanteMientrasHayUnoEnVerificacion() throws Exception {
		registrarPago(cliente, pedido, "OP-1", PDF).andExpect(status().isOk());

		registrarPago(cliente, pedido, "OP-2", PDF).andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("PAGO_NO_PENDIENTE"));
	}

	@Test
	void rn05AlAprobarElPagoQuedaPagadoYElPedidoConfirmado() throws Exception {
		registrarPago(cliente, pedido, "OP-1", PDF).andExpect(status().isOk());
		long pago = pagoVigente();
		mvc.perform(con(admin, get("/api/pagos/en-verificacion"))).andExpect(jsonPath("$[0].id").value(pago))
			.andExpect(jsonPath("$[0].comprobante").value("/api/pagos/" + pago + "/comprobante"));

		aprobar(admin, pago).andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("PAGADO"))
			.andExpect(jsonPath("$.verificacion.decision").value("APROBADO"))
			.andExpect(jsonPath("$.verificacion.verificador").value("admin@dulces.bo"));

		assertThat(estadoDelPedido()).isEqualTo("CONFIRMADO");
		mvc.perform(con(cliente, get("/api/pedidos/" + pedido)))
			.andExpect(jsonPath("$.historial[1].anterior").value("PENDIENTE"))
			.andExpect(jsonPath("$.historial[1].nuevo").value("CONFIRMADO"));
	}

	@Test
	void rn07RechazarExigeObservacionYReabreElCobro() throws Exception {
		registrarPago(cliente, pedido, "OP-1", PDF).andExpect(status().isOk());
		long rechazado = pagoVigente();

		rechazar(admin, rechazado, null).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.observacion").isNotEmpty());
		rechazar(admin, rechazado, "El monto no coincide").andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("RECHAZADO"))
			.andExpect(jsonPath("$.verificacion.observacion").value("El monto no coincide"));

		mvc.perform(con(cliente, get("/api/pagos").param("pedido", String.valueOf(pedido))))
			.andExpect(jsonPath("$[*].estado", contains("RECHAZADO", "PENDIENTE_DE_PAGO")))
			.andExpect(jsonPath("$[0].verificacion.observacion").value("El monto no coincide"));
		assertThat(estadoDelPedido()).isEqualTo("PENDIENTE");

		// HU11: después del rechazo, el cliente registra otro comprobante.
		registrarPago(cliente, pedido, "OP-2", PDF).andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("EN_VERIFICACION"));
	}

	@Test
	void rnf15UnPagoYaDecididoNoSeVuelveADecidir() throws Exception {
		registrarPago(cliente, pedido, "OP-1", PDF).andExpect(status().isOk());
		long pago = pagoVigente();
		aprobar(admin, pago).andExpect(status().isOk());

		rechazar(admin, pago, "Me equivoqué").andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("PAGO_YA_DECIDIDO"));
		aprobar(admin, pago).andExpect(status().isConflict());
		assertThat(jdbc.queryForObject("select count(*) from verificacion", Integer.class)).isEqualTo(1);
	}

	@Test
	void rnf15DosPersonasNoDecidenElMismoPagoALaVez() throws Exception {
		registrarPago(cliente, pedido, "OP-1", PDF).andExpect(status().isOk());
		long pago = pagoVigente();
		crearPersonal("admin2@dulces.bo", "ADMINISTRADOR");
		String otroAdmin = iniciarSesion("admin2@dulces.bo");
		CountDownLatch salida = new CountDownLatch(1);
		List<Callable<Integer>> decisiones = List.of(
				() -> { salida.await(); return aprobar(admin, pago).andReturn().getResponse().getStatus(); },
				() -> { salida.await(); return rechazar(otroAdmin, pago, "No llegó").andReturn().getResponse().getStatus(); });

		List<Integer> estados = new ArrayList<>();
		try (var hilos = Executors.newFixedThreadPool(2)) {
			List<Future<Integer>> resultados = decisiones.stream().map(hilos::submit).toList();
			salida.countDown();
			for (Future<Integer> r : resultados) {
				estados.add(r.get());
			}
		}

		assertThat(estados).containsExactlyInAnyOrder(200, 409);
		assertThat(jdbc.queryForObject("select count(*) from verificacion", Integer.class)).isEqualTo(1);
	}

	@Test
	void soloQuienTieneElPermisoVerificaPagos() throws Exception {
		registrarPago(cliente, pedido, "OP-1", PDF).andExpect(status().isOk());
		crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		String vendedor = iniciarSesion("vendedor@dulces.bo");

		aprobar(vendedor, pagoVigente()).andExpect(status().isForbidden());
		aprobar(cliente, pagoVigente()).andExpect(status().isForbidden());
		mvc.perform(con(vendedor, get("/api/pagos/en-verificacion"))).andExpect(status().isForbidden());

		jdbc.update("insert into rol_permiso (rol, permiso) values ('VENDEDOR', 'COBRANZA_VERIFICAR')");
		aprobar(vendedor, pagoVigente()).andExpect(status().isOk());
	}

	@Test
	void rn06QuienRegistroUnPedidoAsistidoNoVerificaSuPago() throws Exception {
		long vendedorId = crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		jdbc.update("insert into rol_permiso (rol, permiso) values ('VENDEDOR', 'COBRANZA_VERIFICAR')");
		jdbc.update("update pedido set registrado_por = ? where id = ?", vendedorId, pedido);
		registrarPago(cliente, pedido, "OP-1", PDF).andExpect(status().isOk());

		aprobar(iniciarSesion("vendedor@dulces.bo"), pagoVigente()).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.codigo").value("VERIFICACION_PROPIA"));
		aprobar(admin, pagoVigente()).andExpect(status().isOk());
	}

	@Test
	void siElPedidoYaNoEstaPendienteLaAprobacionNoSeAplica() throws Exception {
		registrarPago(cliente, pedido, "OP-1", PDF).andExpect(status().isOk());
		jdbc.update("update pedido set estado = 'CANCELADO' where id = ?", pedido);

		aprobar(admin, pagoVigente()).andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("TRANSICION_NO_PERMITIDA"));

		assertThat(jdbc.queryForObject("select estado from pago where id = ?", String.class, pagoVigente()))
			.isEqualTo("EN_VERIFICACION");
		assertThat(jdbc.queryForObject("select count(*) from verificacion", Integer.class)).isZero();
	}

	@Test
	void rnf18ElAdministradorAgregaUnMetodoDePagoSinCambiarCodigo() throws Exception {
		long nuevo = id(mvc.perform(con(admin, post("/api/metodos-pago")).content("{\"nombre\":\"Tigo Money\",\"instrucciones\":\"\"}")));

		mvc.perform(con(admin, post("/api/metodos-pago/" + nuevo + "/activar"))).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("INSTRUCCIONES_REQUERIDAS"));
		mvc.perform(con(admin, put("/api/metodos-pago/" + nuevo)).content("{\"nombre\":\"Tigo Money\",\"instrucciones\":\"Número 70000000\"}"))
			.andExpect(status().isOk());
		mvc.perform(con(admin, post("/api/metodos-pago/" + nuevo + "/activar"))).andExpect(status().isOk());

		mvc.perform(con(cliente, get("/api/metodos-pago"))).andExpect(jsonPath("$[*].nombre", contains("Transferencia", "Tigo Money")));
		mvc.perform(con(admin, post("/api/metodos-pago")).content("{\"nombre\":\"tigo money\",\"instrucciones\":\"\"}"))
			.andExpect(status().isConflict());
		mvc.perform(con(cliente, post("/api/metodos-pago")).content("{\"nombre\":\"Otro\",\"instrucciones\":\"\"}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void elQrDeUnMetodoSeSubeYSeVeSinSesion() throws Exception {
		long qr = jdbc.queryForObject("select id from metodo_pago where nombre_clave = 'qr'", Long.class);
		byte[] png = { (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10 };

		String ruta = JsonPath.read(mvc.perform(multipart(HttpMethod.PUT, "/api/metodos-pago/" + qr + "/qr")
			.file(new MockMultipartFile("archivo", "qr.png", "image/png", png))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.qr");

		mvc.perform(get(ruta)).andExpect(status().isOk()).andExpect(content().bytes(png));
	}

}
