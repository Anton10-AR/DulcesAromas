package bo.dulcesaromas.cobros;

import java.time.Duration;

import bo.dulcesaromas.PruebaPedidos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU14: consultar pedidos y reportes de ventas (CU10, RF23, RNF19).
 */
class ReportesTests extends PruebaPedidos {

	private long pagado;

	private long pendiente;

	private long cancelado;

	private long deNoche;

	/**
	 * El 02/11 en La Paz: uno pagado (Bs 120), uno pendiente (Bs 15), uno pagado y cancelado (Bs 120) y uno a las
	 * 23:30, que en UTC ya es el 03/11 (Bs 15).
	 */
	@BeforeEach
	void pedidosDelDia() throws Exception {
		pagado = crearPedido(cliente, pedidoSimple(torta));
		pagarYAprobar(pagado);
		pendiente = crearPedido(cliente, pedidoSimple(galletas));
		cancelado = crearPedido(cliente, pedidoSimple(torta));
		pagarYAprobar(cancelado);
		mvc.perform(con(cliente, post("/api/pedidos/" + cancelado + "/cancelar")).content("{\"motivo\":\"Viaje\"}"))
			.andExpect(status().isOk());
		reloj.adelantar(Duration.ofHours(13).plusMinutes(30));
		deNoche = crearPedido(cliente, pedidoSimple(galletas).replace(MANANA, "2026-11-04T10:00:00-04:00"));
		// Tras 13 horas sin uso, la sesión del Panel ya se cerró (RNF06).
		admin = iniciarSesion("admin@dulces.bo");
	}

	private long transferencia() {
		return jdbc.queryForObject("select id from metodo_pago where nombre_clave = 'transferencia'", Long.class);
	}

	@Test
	void rnf19ElReporteTotalizaLosPedidosDelDiaEnLaPaz() throws Exception {
		mvc.perform(con(admin, get("/api/reportes/ventas").param("desde", "2026-11-02").param("hasta", "2026-11-02")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cantidadPedidos").value(4))
			.andExpect(jsonPath("$.montoPedidos").value(270.00))
			.andExpect(jsonPath("$.montoVendido").value(120.00))
			.andExpect(jsonPath("$.pedidos[0].pedidoId").value(deNoche));
		mvc.perform(con(admin, get("/api/reportes/ventas").param("desde", "2026-11-03").param("hasta", "2026-11-03")))
			.andExpect(jsonPath("$.cantidadPedidos").value(0));
	}

	@Test
	void filtraPorEstadoYPorMetodoDePago() throws Exception {
		mvc.perform(con(admin, get("/api/reportes/ventas").param("desde", "2026-11-02").param("hasta", "2026-11-02")
			.param("estado", "CONFIRMADO")))
			.andExpect(jsonPath("$.pedidos[*].pedidoId", containsInAnyOrder((int) pagado)));
		mvc.perform(con(admin, get("/api/reportes/ventas").param("desde", "2026-11-02").param("hasta", "2026-11-02")
			.param("metodoPago", String.valueOf(transferencia()))))
			.andExpect(jsonPath("$.pedidos[*].pedidoId", containsInAnyOrder((int) pagado, (int) cancelado)))
			.andExpect(jsonPath("$.pedidos[0].metodo").value("Transferencia"))
			.andExpect(jsonPath("$.montoVendido").value(120.00));
	}

	@Test
	void elVendedorNoVeMontosDeVentas() throws Exception {
		crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		String vendedor = iniciarSesion("vendedor@dulces.bo");

		mvc.perform(con(vendedor, get("/api/reportes/ventas").param("desde", "2026-11-02").param("hasta", "2026-11-02")))
			.andExpect(status().isForbidden());
		mvc.perform(con(vendedor, get("/api/pedidos").param("desde", "2026-11-02").param("hasta", "2026-11-02")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", containsInAnyOrder((int) pagado, (int) pendiente, (int) cancelado, (int) deNoche)));
	}

	@Test
	void elRangoEsDeHasta12Meses() throws Exception {
		mvc.perform(con(admin, get("/api/reportes/ventas").param("desde", "2026-11-02").param("hasta", "2026-11-01")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.codigo").value("RANGO_INVALIDO"));
		mvc.perform(con(admin, get("/api/reportes/ventas").param("desde", "2025-01-01").param("hasta", "2026-11-02")))
			.andExpect(status().isBadRequest());
	}

}
