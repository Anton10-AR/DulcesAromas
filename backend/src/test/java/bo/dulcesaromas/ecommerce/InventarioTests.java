package bo.dulcesaromas.ecommerce;

import bo.dulcesaromas.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU09: inventario de insumos con entradas, salidas, ajustes y alertas (CU03, RN09).
 */
class InventarioTests extends PruebaIntegracion {

	private String admin;

	private long harina;

	@BeforeEach
	void insumo() throws Exception {
		crearPersonal("admin@dulces.bo", "ADMINISTRADOR");
		admin = iniciarSesion("admin@dulces.bo");
		String cuerpo = mvc.perform(con(admin, post("/api/inventario"))
			.content("{\"nombre\":\"Harina\",\"unidad\":\"kg\",\"minimo\":5}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.stock").value(0))
			.andReturn().getResponse().getContentAsString();
		harina = ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
	}

	private ResultActions movimiento(String token, String tipo, String cantidad, String motivo) throws Exception {
		return mvc.perform(con(token, post("/api/inventario/" + harina + "/movimientos"))
			.content("{\"tipo\":\"%s\",\"cantidad\":%s,\"motivo\":\"%s\"}".formatted(tipo, cantidad, motivo)));
	}

	@Test
	void cadaMovimientoTieneCantidadYMotivo() throws Exception {
		movimiento(admin, "ENTRADA", "20", "Compra del lunes").andExpect(status().isOk())
			.andExpect(jsonPath("$.stock").value(20));
		movimiento(admin, "SALIDA", "3.5", "Tortas del día").andExpect(status().isOk())
			.andExpect(jsonPath("$.stock").value(16.5));

		movimiento(admin, "ENTRADA", "0", "Nada").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.cantidad").isNotEmpty());
		movimiento(admin, "ENTRADA", "1", "").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.motivo").isNotEmpty());
	}

	@Test
	void rn09UnaSalidaNoSuperaElStock() throws Exception {
		movimiento(admin, "ENTRADA", "2", "Compra").andExpect(status().isOk());

		movimiento(admin, "SALIDA", "2.001", "Demasiado").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));

		mvc.perform(con(admin, get("/api/inventario"))).andExpect(jsonPath("$[0].stock").value(2));
	}

	@Test
	void rn09AlLlegarAlMinimoApareceUnaAlerta() throws Exception {
		movimiento(admin, "ENTRADA", "8", "Compra").andExpect(status().isOk()).andExpect(jsonPath("$.alerta").value(false));
		mvc.perform(con(admin, get("/api/inventario/alertas"))).andExpect(jsonPath("$.length()").value(0));

		movimiento(admin, "SALIDA", "3", "Producción").andExpect(status().isOk()).andExpect(jsonPath("$.alerta").value(true));

		mvc.perform(con(admin, get("/api/inventario/alertas"))).andExpect(jsonPath("$[*].nombre", contains("Harina")));
	}

	@Test
	void elAjusteCorrigeElStockAlValorContado() throws Exception {
		movimiento(admin, "ENTRADA", "10", "Compra").andExpect(status().isOk());

		movimiento(admin, "AJUSTE", "7.25", "Conteo semanal").andExpect(status().isOk())
			.andExpect(jsonPath("$.stock").value(7.25));
	}

	@Test
	void elAdministradorVeElHistorialDeMovimientos() throws Exception {
		movimiento(admin, "ENTRADA", "10", "Compra").andExpect(status().isOk());
		movimiento(admin, "SALIDA", "4", "Producción").andExpect(status().isOk());

		mvc.perform(con(admin, get("/api/inventario/" + harina + "/movimientos"))).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].tipo", contains("SALIDA", "ENTRADA")))
			.andExpect(jsonPath("$[0].stockResultante").value(6))
			.andExpect(jsonPath("$[0].motivo").value("Producción"))
			.andExpect(jsonPath("$[0].usuario").value("admin@dulces.bo"));
	}

	@Test
	void elVendedorSoloConsultaExistenciasYAlertas() throws Exception {
		crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		String vendedor = iniciarSesion("vendedor@dulces.bo");

		mvc.perform(con(vendedor, get("/api/inventario"))).andExpect(status().isOk());
		mvc.perform(con(vendedor, get("/api/inventario/alertas"))).andExpect(status().isOk());
		movimiento(vendedor, "ENTRADA", "1", "Intento").andExpect(status().isForbidden());
		mvc.perform(con(vendedor, get("/api/inventario/" + harina + "/movimientos"))).andExpect(status().isForbidden());
		mvc.perform(con(vendedor, post("/api/inventario")).content("{\"nombre\":\"Azúcar\",\"unidad\":\"kg\",\"minimo\":1}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void elNombreDelInsumoNoSeRepite() throws Exception {
		mvc.perform(con(admin, post("/api/inventario")).content("{\"nombre\":\"HARINA\",\"unidad\":\"kg\",\"minimo\":1}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("INSUMO_REPETIDO"));
	}

}
