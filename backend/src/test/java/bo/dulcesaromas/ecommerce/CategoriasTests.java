package bo.dulcesaromas.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU18: crear, editar y desactivar categorías (CU14, RNF18).
 */
class CategoriasTests extends PruebaCatalogo {

	private ResultActions desactivar(long id, boolean confirmar) throws Exception {
		return mvc.perform(con(admin, post("/api/categorias/" + id + "/desactivar?confirmar=" + confirmar)));
	}

	@Test
	void creaUnaCategoriaConSuNombre() throws Exception {
		mvc.perform(con(admin, post("/api/categorias")).content("{\"nombre\":\"  Tortas  \"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.nombre").value("Tortas"))
			.andExpect(jsonPath("$.activa").value(true));
	}

	@Test
	void elNombreNoSeRepiteAunqueCambienMayusculasOTildes() throws Exception {
		crearCategoria("Galletas");
		long postres = crearCategoria("Postres");

		mvc.perform(con(admin, post("/api/categorias")).content("{\"nombre\":\"GALLETAS\"}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("CATEGORIA_REPETIDA"));
		mvc.perform(con(admin, put("/api/categorias/" + postres)).content("{\"nombre\":\"Gallétas\"}"))
			.andExpect(status().isConflict());
	}

	@Test
	void editaElNombre() throws Exception {
		long id = crearCategoria("Cupcake");

		mvc.perform(con(admin, put("/api/categorias/" + id)).content("{\"nombre\":\"Cupcakes\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nombre").value("Cupcakes"));
	}

	@Test
	void alDesactivarUnaCategoriaConProductosDiceCuantosAfectaYPideConfirmacion() throws Exception {
		long tortas = crearCategoria("Tortas");
		crearProducto(tortas, "Torta de chocolate");
		crearProducto(tortas, "Torta de vainilla");

		desactivar(tortas, false).andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("CONFIRMACION_REQUERIDA"))
			.andExpect(jsonPath("$.mensaje").value(containsString("2 productos activos")));
		mvc.perform(get("/api/catalogo/productos")).andExpect(jsonPath("$.length()").value(2));

		desactivar(tortas, true).andExpect(status().isOk()).andExpect(jsonPath("$.activa").value(false));

		mvc.perform(get("/api/catalogo/productos")).andExpect(jsonPath("$.length()").value(0));
		mvc.perform(get("/api/catalogo/categorias")).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void unaCategoriaSinProductosSeDesactivaSinConfirmar() throws Exception {
		long id = crearCategoria("Especiales");

		desactivar(id, false).andExpect(status().isOk());
		mvc.perform(con(admin, post("/api/categorias/" + id + "/activar"))).andExpect(jsonPath("$.activa").value(true));
	}

	@Test
	void elPanelMuestraCuantosProductosActivosTieneCadaCategoria() throws Exception {
		long tortas = crearCategoria("Tortas");
		crearCategoria("Galletas");
		crearProducto(tortas, "Torta de chocolate");

		mvc.perform(con(admin, get("/api/categorias"))).andExpect(status().isOk())
			.andExpect(jsonPath("$[0].nombre").value("Galletas"))
			.andExpect(jsonPath("$[0].productosActivos").value(0))
			.andExpect(jsonPath("$[1].productosActivos").value(1));
	}

	@Test
	void elVendedorNoGestionaCategorias() throws Exception {
		crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		String vendedor = iniciarSesion("vendedor@dulces.bo");

		mvc.perform(con(vendedor, post("/api/categorias")).content("{\"nombre\":\"Panes\"}"))
			.andExpect(status().isForbidden());
		mvc.perform(con(vendedor, get("/api/categorias"))).andExpect(status().isOk());
	}

}
