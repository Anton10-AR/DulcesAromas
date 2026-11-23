package bo.dulcesaromas.ecommerce;

import bo.dulcesaromas.PruebaPedidos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RN19 (HU24): pasar un pedido a En preparación descuenta los insumos de su receta; si alguno no alcanza, el pedido no
 * avanza y no se descuenta nada.
 */
class DescuentoInsumosTests extends PruebaPedidos {

	private long harina;

	private long chocolate;

	private String repostero;

	/** La torta lleva 0.5 kg de harina y 0.3 kg de chocolate por unidad; la Grande cuenta como 1.5 unidades. */
	@BeforeEach
	void recetaYRepostero() throws Exception {
		harina = insumo("Harina", "kg", "5");
		chocolate = insumo("Chocolate", "kg", "5");
		mvc.perform(con(admin, put("/api/productos/" + torta + "/produccion")).content("""
				{"minutosPorUnidad":60,"receta":[{"insumoId":%d,"cantidad":0.5},{"insumoId":%d,"cantidad":0.3}]}"""
			.formatted(harina, chocolate))).andExpect(status().isOk());
		jdbc.update("update opcion_personalizacion set factor = 1.5 where id = ?", grande);
		crearPersonal("repostero@dulces.bo", "REPOSTERO");
		repostero = iniciarSesion("repostero@dulces.bo");
	}

	@Test
	void rn19EmpezarAPrepararDescuentaLaRecetaYAvisaElMinimo() throws Exception {
		jdbc.update("update item_inventario set minimo = 4 where id = ?", harina);
		long pedido = crearPedido(cliente, pedidoDelivery());
		pagarYAprobar(pedido);
		assertThat(stock(harina)).as("aprobar el pago no descuenta").isEqualTo(5.0);

		mvc.perform(con(repostero, post("/api/cocina/" + pedido + "/en-preparacion"))).andExpect(status().isOk());

		// Dos tortas Grande cuentan como tres: 1.5 kg de harina y 0.9 kg de chocolate.
		assertThat(stock(harina)).isEqualTo(3.5);
		assertThat(stock(chocolate)).isEqualTo(4.1);
		mvc.perform(con(admin, get("/api/inventario/" + harina + "/movimientos")))
			.andExpect(jsonPath("$[0].tipo").value("SALIDA"))
			.andExpect(jsonPath("$[0].cantidad").value(1.5))
			.andExpect(jsonPath("$[0].motivo", containsString("Pedido #" + pedido)));
		// RN09: la harina quedó bajo su mínimo.
		mvc.perform(con(admin, get("/api/inventario/alertas"))).andExpect(jsonPath("$[*].nombre", hasItem("Harina")));
	}

	@Test
	void rn19SinStockElPedidoNoAvanzaYNoSeDescuentaNada() throws Exception {
		jdbc.update("update item_inventario set stock = 1 where id = ?", harina);
		long pedido = crearPedido(cliente, pedidoDelivery());
		pagarYAprobar(pedido);
		int movimientos = jdbc.queryForObject("select count(*) from movimiento_inventario", Integer.class);

		mvc.perform(con(repostero, post("/api/cocina/" + pedido + "/en-preparacion")))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"))
			.andExpect(jsonPath("$.mensaje", containsString("0.5 kg de Harina")));

		assertThat(estado(pedido)).isEqualTo("CONFIRMADO");
		assertThat(stock(chocolate)).as("tampoco se descuenta lo que sí alcanzaba").isEqualTo(5.0);
		assertThat(jdbc.queryForObject("select count(*) from movimiento_inventario", Integer.class)).isEqualTo(movimientos);

		// Con la compra registrada, ya se puede preparar.
		mvc.perform(con(admin, post("/api/inventario/" + harina + "/movimientos"))
			.content("{\"tipo\":\"ENTRADA\",\"cantidad\":2,\"motivo\":\"Compra\"}")).andExpect(status().isOk());
		mvc.perform(con(repostero, post("/api/cocina/" + pedido + "/en-preparacion"))).andExpect(status().isOk());
		assertThat(stock(harina)).isEqualTo(1.5);
	}

	@Test
	void rn19UnProductoSinRecetaNoDescuentaNiBloquea() throws Exception {
		long pedido = crearPedido(cliente, pedidoSimple(galletas));
		pagarYAprobar(pedido);
		int movimientos = jdbc.queryForObject("select count(*) from movimiento_inventario", Integer.class);

		mvc.perform(con(repostero, post("/api/cocina/" + pedido + "/en-preparacion"))).andExpect(status().isOk());

		assertThat(jdbc.queryForObject("select count(*) from movimiento_inventario", Integer.class)).isEqualTo(movimientos);
	}

	@Test
	void laEstimacionNoCuentaDosVecesLoQueYaSeDesconto() throws Exception {
		long enPreparacion = crearPedido(cliente, pedidoDelivery());
		pagarYAprobar(enPreparacion);
		mvc.perform(con(repostero, post("/api/cocina/" + enPreparacion + "/en-preparacion"))).andExpect(status().isOk());
		long nuevo = crearPedido(cliente, pedidoSimple(torta));

		// La harina del pedido En preparación ya salió del stock: no vuelve a contar como comprometida.
		mvc.perform(con(admin, get("/api/pedidos/" + nuevo + "/estimacion")))
			.andExpect(jsonPath("$.insumos[1].nombre").value("Harina"))
			.andExpect(jsonPath("$.insumos[1].comprometido").value(0))
			.andExpect(jsonPath("$.insumos[1].stock").value(3.5))
			// Pero su tiempo sigue ocupando la cocina.
			.andExpect(jsonPath("$.minutosCola").value(180));
	}

	private long insumo(String nombre, String unidad, String cantidad) throws Exception {
		long id = id(mvc.perform(con(admin, post("/api/inventario"))
			.content("{\"nombre\":\"%s\",\"unidad\":\"%s\",\"minimo\":0}".formatted(nombre, unidad))));
		mvc.perform(con(admin, post("/api/inventario/" + id + "/movimientos"))
			.content("{\"tipo\":\"ENTRADA\",\"cantidad\":%s,\"motivo\":\"Compra\"}".formatted(cantidad)))
			.andExpect(status().isOk());
		return id;
	}

	private double stock(long insumo) {
		return jdbc.queryForObject("select stock from item_inventario where id = ?", Double.class, insumo);
	}

	private String estado(long pedido) {
		return jdbc.queryForObject("select estado from pedido where id = ?", String.class, pedido);
	}

}
