package bo.dulcesaromas.ecommerce;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;

import javax.imageio.ImageIO;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU08: gestionar productos (CU02, RN10, RNF10, D9).
 */
class ProductosTests extends PruebaCatalogo {

	private long tortas;

	@BeforeEach
	void categoria() throws Exception {
		tortas = crearCategoria("Tortas");
	}

	private String personalizable(String opciones) {
		return """
				{"nombre":"Torta de cumpleaños","descripcion":"Tres pisos","precio":120.00,"categoriaId":%d,
				 "disponible":true,"personalizable":true,"opciones":%s}""".formatted(tortas, opciones);
	}

	@Test
	void d9RegistraUnProductoConOpcionesDePersonalizacionYSuRecargoFijo() throws Exception {
		intentarCrearProducto(personalizable("""
				[{"tipo":"Tamaño","nombre":"Grande","recargo":40.00},{"tipo":"Relleno","nombre":"Dulce de leche","recargo":0}]"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.precio").value(120.00))
			.andExpect(jsonPath("$.categoria.nombre").value("Tortas"))
			.andExpect(jsonPath("$.opciones.length()").value(2))
			.andExpect(jsonPath("$.opciones[0].tipo").value("Tamaño"))
			.andExpect(jsonPath("$.opciones[0].recargo").value(40.00));
	}

	@Test
	void alEditarSeReemplazanLasOpciones() throws Exception {
		String cuerpo = intentarCrearProducto(personalizable("[{\"tipo\":\"Tamaño\",\"nombre\":\"Grande\",\"recargo\":40}]"))
			.andReturn().getResponse().getContentAsString();
		long id = ((Number) JsonPath.read(cuerpo, "$.id")).longValue();

		mvc.perform(con(admin, put("/api/productos/" + id)).content(personalizable(
				"[{\"tipo\":\"Sabor\",\"nombre\":\"Chocolate\",\"recargo\":10},{\"tipo\":\"Sabor\",\"nombre\":\"Fresa\",\"recargo\":10}]")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.opciones.length()").value(2))
			.andExpect(jsonPath("$.opciones[1].nombre").value("Fresa"));
		assertThat(jdbc.queryForObject("select count(*) from opcion_personalizacion", Integer.class)).isEqualTo(2);
	}

	@Test
	void faltanDatosOElPrecioNoEsValido() throws Exception {
		intentarCrearProducto(producto(tortas, "Torta", "0")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.precio").isNotEmpty());
		intentarCrearProducto(producto(tortas, "Torta", "10.555")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.precio").isNotEmpty());
		intentarCrearProducto("{\"precio\":10,\"categoriaId\":%d}".formatted(tortas)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.nombre").isNotEmpty())
			.andExpect(jsonPath("$.campos.descripcion").isNotEmpty())
			.andExpect(jsonPath("$.campos.disponible").isNotEmpty());
		intentarCrearProducto(personalizable("[{\"tipo\":\"Tamaño\",\"nombre\":\"Grande\",\"recargo\":-1}]"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos['opciones[0].recargo']").isNotEmpty());
	}

	@Test
	void losMontosLleganComoTextoParaNoPerderExactitud() throws Exception {
		intentarCrearProducto(personalizable("[{\"tipo\":\"Tamaño\",\"nombre\":\"Grande\",\"recargo\":\"0.10\"}]")
			.replace("\"precio\":120.00", "\"precio\":\"120.20\""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.precio").value(120.20))
			.andExpect(jsonPath("$.opciones[0].recargo").value(0.10));
		assertThat(jdbc.queryForObject("select precio from producto", BigDecimal.class))
			.isEqualByComparingTo("120.20");
	}

	@Test
	void lasOpcionesSoloVanEnUnProductoPersonalizable() throws Exception {
		intentarCrearProducto(personalizable("[]")).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("OPCIONES_REQUERIDAS"));
		intentarCrearProducto(personalizable("[{\"tipo\":\"Tamaño\",\"nombre\":\"Grande\",\"recargo\":40}]")
			.replace("\"personalizable\":true", "\"personalizable\":false"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("PRODUCTO_NO_PERSONALIZABLE"));
	}

	@Test
	void noSeRegistraEnUnaCategoriaDesactivada() throws Exception {
		long viejas = crearCategoria("Viejas");
		mvc.perform(con(admin, post("/api/categorias/" + viejas + "/desactivar"))).andExpect(status().isOk());

		intentarCrearProducto(producto(viejas, "Torta", "50")).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("CATEGORIA_INACTIVA"));
	}

	@Test
	void rn10UnProductoSinPedidosSePuedeBorrar() throws Exception {
		long id = crearProducto(tortas, "Torta creada por error");
		subirImagen(admin, id, imagen("png", 800, 800)).andExpect(status().isOk());

		mvc.perform(con(admin, delete("/api/productos/" + id))).andExpect(status().isNoContent());

		mvc.perform(con(admin, get("/api/productos/" + id))).andExpect(status().isNotFound());
		assertThat(jdbc.queryForObject("select count(*) from imagen_producto", Integer.class)).isZero();
	}

	@Test
	void unProductoDesactivadoNoSeVeEnLaTienda() throws Exception {
		long id = crearProducto(tortas, "Torta de chocolate");

		mvc.perform(con(admin, post("/api/productos/" + id + "/desactivar"))).andExpect(status().isOk())
			.andExpect(jsonPath("$.activo").value(false));

		mvc.perform(con(admin, get("/api/productos/" + id))).andExpect(status().isOk());
		mvc.perform(get("/api/catalogo/productos/" + id)).andExpect(status().isNotFound());
		assertThat(jdbc.queryForObject("select count(*) from producto", Integer.class)).isEqualTo(1);
	}

	@Test
	void elVendedorSoloCambiaLaDisponibilidad() throws Exception {
		long id = crearProducto(tortas, "Torta de chocolate");
		crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		String vendedor = iniciarSesion("vendedor@dulces.bo");

		mvc.perform(con(vendedor, put("/api/productos/" + id + "/disponibilidad")).content("{\"disponible\":false}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.disponible").value(false));

		mvc.perform(con(vendedor, put("/api/productos/" + id)).content(producto(tortas, "Otra", "1")))
			.andExpect(status().isForbidden());
		mvc.perform(con(vendedor, post("/api/productos")).content(producto(tortas, "Otra", "1")))
			.andExpect(status().isForbidden());
		mvc.perform(con(vendedor, post("/api/productos/" + id + "/desactivar"))).andExpect(status().isForbidden());
		mvc.perform(con(vendedor, delete("/api/productos/" + id))).andExpect(status().isForbidden());
		subirImagen(vendedor, id, imagen("png", 800, 800)).andExpect(status().isForbidden());
	}

	@Test
	void elRepartidorNoVeElCatalogoDelPanel() throws Exception {
		crearPersonal("repartidor@dulces.bo", "REPARTIDOR");
		String repartidor = iniciarSesion("repartidor@dulces.bo");

		mvc.perform(con(repartidor, get("/api/productos"))).andExpect(status().isForbidden());
	}

	@Test
	void rnf10GuardaUnaImagenCuadradaDe800PxOMasYLaSirveSinSesion() throws Exception {
		long id = crearProducto(tortas, "Torta de chocolate");
		byte[] png = imagen("png", 800, 800);

		String ruta = JsonPath.read(subirImagen(admin, id, png).andExpect(status().isOk())
			.andExpect(jsonPath("$.imagen").value(startsWith("/api/catalogo/productos/" + id + "/imagen?v=")))
			.andReturn().getResponse().getContentAsString(), "$.imagen");

		mvc.perform(get(ruta)).andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "image/png"))
			.andExpect(content().bytes(png));
	}

	@Test
	void rnf10UnaImagenNoCuadradaSeRecortaAlCentro() throws Exception {
		long id = crearProducto(tortas, "Torta de chocolate");

		String ruta = JsonPath.read(subirImagen(admin, id, imagen("jpg", 1600, 1200)).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString(), "$.imagen");

		byte[] guardada = mvc.perform(get(ruta)).andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "image/jpeg"))
			.andReturn().getResponse().getContentAsByteArray();
		var leida = ImageIO.read(new ByteArrayInputStream(guardada));
		assertThat(leida.getWidth()).isEqualTo(1200);
		assertThat(leida.getHeight()).isEqualTo(1200);
	}

	@Test
	void rnf10RechazaImagenesQueNoCumplen() throws Exception {
		long id = crearProducto(tortas, "Torta de chocolate");
		byte[] demasiadoGrande = new byte[10 * 1024 * 1024 + 1];
		System.arraycopy(imagen("png", 800, 800), 0, demasiadoGrande, 0, 8);

		for (byte[] mala : new byte[][] { imagen("png", 1200, 700), imagen("jpg", 600, 600), imagen("gif", 900, 900),
				"no soy una imagen".getBytes(), demasiadoGrande }) {
			subirImagen(admin, id, mala).andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.codigo").value("IMAGEN_INVALIDA"));
		}
		mvc.perform(con(admin, get("/api/productos/" + id))).andExpect(jsonPath("$.imagen").doesNotExist());
	}

}
