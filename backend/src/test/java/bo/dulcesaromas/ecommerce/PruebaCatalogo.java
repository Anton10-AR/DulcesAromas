package bo.dulcesaromas.ecommerce;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import bo.dulcesaromas.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas del catálogo: un administrador con sesión y ayudas para crear categorías y productos.
 */
abstract class PruebaCatalogo extends PruebaIntegracion {

	protected String admin;

	@BeforeEach
	void administrador() throws Exception {
		crearPersonal("admin@dulces.bo", "ADMINISTRADOR");
		admin = iniciarSesion("admin@dulces.bo");
	}

	protected long crearCategoria(String nombre) throws Exception {
		String cuerpo = mvc.perform(con(admin, post("/api/categorias")).content("{\"nombre\":\"%s\"}".formatted(nombre)))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
	}

	protected ResultActions intentarCrearProducto(String json) throws Exception {
		return mvc.perform(con(admin, post("/api/productos")).content(json));
	}

	/**
	 * Producto sin personalización, disponible, de Bs 50.00.
	 */
	protected long crearProducto(long categoriaId, String nombre) throws Exception {
		String cuerpo = intentarCrearProducto(producto(categoriaId, nombre, "50.00")).andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
	}

	protected static String producto(long categoriaId, String nombre, String precio) {
		return """
				{"nombre":"%s","descripcion":"Hecha en casa","precio":%s,"categoriaId":%d,
				 "disponible":true,"personalizable":false}""".formatted(nombre, precio, categoriaId);
	}

	protected ResultActions subirImagen(String token, long productoId, byte[] datos) throws Exception {
		return mvc.perform(multipart(HttpMethod.PUT, "/api/productos/" + productoId + "/imagen")
			.file(new MockMultipartFile("archivo", "imagen", "application/octet-stream", datos))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	protected static byte[] imagen(String formato, int ancho, int alto) throws IOException {
		var salida = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB), formato, salida);
		return salida.toByteArray();
	}

}
