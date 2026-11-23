package bo.dulcesaromas.cobros;

import java.time.Duration;

import bo.dulcesaromas.PruebaPedidos;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pago por QR verificado automáticamente: cada cobro tiene un código de un solo uso que vence a los 15 minutos; la
 * pasarela simulada lo confirma y el sistema aprueba el pago solo (RN05). La pasarela no muestra datos del cliente.
 */
class PagoQrTests extends PruebaPedidos {

	@BeforeEach
	void activarQr() {
		jdbc.update("update metodo_pago set activo = true where canal = 'QR'");
	}

	@Test
	void alConfirmarEnLaPasarelaElPedidoQuedaConfirmadoSinVerificacionManual() throws Exception {
		long pedido = crearPedido(cliente, pedidoSimple(torta));
		String codigo = cobroQr(pedido);

		// Quien escanea el QR no tiene sesión y solo ve el comercio, el pedido y el monto.
		mvc.perform(get("/api/pasarela/" + codigo))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.comercio").value("Dulces Aromas"))
			.andExpect(jsonPath("$.pedidoId").value(pedido))
			.andExpect(jsonPath("$.monto").value(120))
			.andExpect(jsonPath("$.estado").value("PENDIENTE"))
			.andExpect(jsonPath("$.cliente").doesNotExist())
			.andExpect(jsonPath("$.telefono").doesNotExist());

		mvc.perform(post("/api/pasarela/" + codigo + "/confirmar"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("PAGADO"))
			.andExpect(jsonPath("$.referencia", matchesPattern("QR-[A-Z0-9]{8}")));

		assertThat(estado(pedido)).isEqualTo("CONFIRMADO");
		mvc.perform(con(cliente, get("/api/pagos").param("pedido", String.valueOf(pedido))))
			.andExpect(jsonPath("$[0].estado").value("PAGADO"))
			.andExpect(jsonPath("$[0].metodo").value("QR"))
			.andExpect(jsonPath("$[0].verificacion.verificador").value("Sistema (procesador de pagos)"));
		mvc.perform(con(admin, get("/api/pagos/en-verificacion"))).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void unQrSePagaUnaSolaVez() throws Exception {
		long pedido = crearPedido(cliente, pedidoSimple(torta));
		String codigo = cobroQr(pedido);
		mvc.perform(post("/api/pasarela/" + codigo + "/confirmar")).andExpect(status().isOk());

		mvc.perform(post("/api/pasarela/" + codigo + "/confirmar"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("COBRO_USADO"));
		mvc.perform(get("/api/pasarela/" + codigo)).andExpect(jsonPath("$.estado").value("PAGADO"));
	}

	@Test
	void unQrVencidoNoSirveYSePideOtro() throws Exception {
		long pedido = crearPedido(cliente, pedidoSimple(torta));
		String viejo = cobroQr(pedido);

		reloj.adelantar(Duration.ofMinutes(15));
		mvc.perform(get("/api/pasarela/" + viejo)).andExpect(jsonPath("$.estado").value("VENCIDO"));
		mvc.perform(post("/api/pasarela/" + viejo + "/confirmar"))
			.andExpect(status().isGone())
			.andExpect(jsonPath("$.codigo").value("COBRO_VENCIDO"));
		assertThat(estado(pedido)).isEqualTo("PENDIENTE");

		// Con un QR nuevo, sí.
		cliente = iniciarSesion("ana@correo.bo");
		mvc.perform(post("/api/pasarela/" + cobroQr(pedido) + "/confirmar")).andExpect(status().isOk());
		assertThat(estado(pedido)).isEqualTo("CONFIRMADO");
	}

	@Test
	void unCodigoInventadoNoAbreNada() throws Exception {
		mvc.perform(get("/api/pasarela/no-existe")).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.codigo").value("COBRO_NO_ENCONTRADO"));
		mvc.perform(post("/api/pasarela/no-existe/confirmar")).andExpect(status().isNotFound());
	}

	@Test
	void unPedidoCanceladoCierraSuQr() throws Exception {
		long pedido = crearPedido(cliente, pedidoSimple(torta));
		String codigo = cobroQr(pedido);
		mvc.perform(con(cliente, post("/api/pedidos/" + pedido + "/cancelar")).content("{\"motivo\":\"Me equivoqué\"}"))
			.andExpect(status().isOk());

		mvc.perform(get("/api/pasarela/" + codigo)).andExpect(jsonPath("$.estado").value("CERRADO"));
		mvc.perform(post("/api/pasarela/" + codigo + "/confirmar")).andExpect(status().isConflict());
	}

	@Test
	void soloElTitularAbreUnCobroYElQrNoSePagaConComprobante() throws Exception {
		long pedido = crearPedido(cliente, pedidoSimple(torta));
		crearCliente("otro@correo.bo");
		mvc.perform(con(iniciarSesion("otro@correo.bo"), post("/api/pagos/qr")).content("{\"pedidoId\":%d}".formatted(pedido)))
			.andExpect(status().isNotFound());

		long qr = jdbc.queryForObject("select id from metodo_pago where canal = 'QR'", Long.class);
		mvc.perform(multipart("/api/pagos").param("pedidoId", String.valueOf(pedido))
			.param("metodoPagoId", String.valueOf(qr)).param("referencia", "OP-1")
			.file(new MockMultipartFile("comprobante", "c.pdf", "application/pdf", "%PDF-1.7".getBytes()))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
			.andExpect(jsonPath("$.codigo").value("METODO_AUTOMATICO"));
	}

	private String cobroQr(long pedido) throws Exception {
		String cuerpo = mvc.perform(con(cliente, post("/api/pagos/qr")).content("{\"pedidoId\":%d}".formatted(pedido)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.monto").value(120))
			.andExpect(jsonPath("$.vence").exists())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(cuerpo, "$.codigo");
	}

	private String estado(long pedido) {
		return jdbc.queryForObject("select estado from pedido where id = ?", String.class, pedido);
	}

}
