package bo.dulcesaromas.ecommerce;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU07: consultar el catálogo, buscar y filtrar por categoría (CU04, RN15).
 */
class CatalogoTests extends PruebaCatalogo {

	private long tortas;

	private long galletas;

	@BeforeEach
	void catalogo() throws Exception {
		tortas = crearCategoria("Tortas");
		galletas = crearCategoria("Galletas");
		crearProducto(tortas, "Tórta de chocolate");
		crearProducto(tortas, "TORTA tres leches");
		crearProducto(galletas, "Galletas de avena");
	}

	@Test
	void rn15SinIniciarSesionSeVenLosProductosActivosConSusDatos() throws Exception {
		long desactivado = crearProducto(galletas, "Galleta vieja");
		mvc.perform(con(admin, post("/api/productos/" + desactivado + "/desactivar"))).andExpect(status().isOk());

		mvc.perform(get("/api/catalogo/productos")).andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(3))
			.andExpect(jsonPath("$[0].nombre").value("Galletas de avena"))
			.andExpect(jsonPath("$[0].descripcion").value("Hecha en casa"))
			.andExpect(jsonPath("$[0].precio").value(50.00))
			.andExpect(jsonPath("$[0].disponible").value(true))
			.andExpect(jsonPath("$[0].categoria.nombre").value("Galletas"));
		mvc.perform(get("/api/catalogo/categorias")).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].nombre", contains("Galletas", "Tortas")));
	}

	@Test
	void buscaPorNombreSinImportarMayusculasNiTildes() throws Exception {
		mvc.perform(get("/api/catalogo/productos").param("buscar", "torta"))
			.andExpect(jsonPath("$[*].nombre", containsInAnyOrder("Tórta de chocolate", "TORTA tres leches")));
		mvc.perform(get("/api/catalogo/productos").param("buscar", "  LECHES "))
			.andExpect(jsonPath("$[*].nombre", contains("TORTA tres leches")));
	}

	@Test
	void losComodinesDeLaBusquedaSeTomanComoTexto() throws Exception {
		mvc.perform(get("/api/catalogo/productos").param("buscar", "%")).andExpect(jsonPath("$.length()").value(0));
		mvc.perform(get("/api/catalogo/productos").param("buscar", "_")).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void filtraPorCategoriaYCombinaConLaBusqueda() throws Exception {
		mvc.perform(get("/api/catalogo/productos").param("categoria", String.valueOf(galletas)))
			.andExpect(jsonPath("$[*].nombre", contains("Galletas de avena")));
		mvc.perform(get("/api/catalogo/productos").param("categoria", String.valueOf(galletas)).param("buscar", "torta"))
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void unProductoNoDisponibleSeMuestraComoNoDisponible() throws Exception {
		long id = crearProducto(tortas, "Torta de fresa");
		mvc.perform(con(admin, put("/api/productos/" + id + "/disponibilidad")).content("{\"disponible\":false}"))
			.andExpect(status().isOk());

		mvc.perform(get("/api/catalogo/productos/" + id)).andExpect(status().isOk())
			.andExpect(jsonPath("$.disponible").value(false));
	}

	@Test
	void elDetalleMuestraLasOpcionesDePersonalizacion() throws Exception {
		String cuerpo = intentarCrearProducto("""
				{"nombre":"Torta decorada","descripcion":"A pedido","precio":150,"categoriaId":%d,"disponible":true,
				 "personalizable":true,"opciones":[{"tipo":"Tamaño","nombre":"Mediana","recargo":20}]}""".formatted(tortas))
			.andReturn().getResponse().getContentAsString();
		long id = ((Number) JsonPath.read(cuerpo, "$.id")).longValue();

		mvc.perform(get("/api/catalogo/productos/" + id)).andExpect(status().isOk())
			.andExpect(jsonPath("$.personalizable").value(true))
			.andExpect(jsonPath("$.opciones[0].nombre").value("Mediana"))
			.andExpect(jsonPath("$.opciones[0].recargo").value(20));
	}

	@Test
	void unProductoInexistenteDevuelve404() throws Exception {
		mvc.perform(get("/api/catalogo/productos/9999")).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.codigo").value("PRODUCTO_NO_ENCONTRADO"));
	}

}
