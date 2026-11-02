package bo.dulcesaromas.seguridad;

import bo.dulcesaromas.PruebaIntegracion;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Solo el Panel web configurado en CORS_ORIGENES llama a la API desde un navegador.
 */
class CorsTests extends PruebaIntegracion {

	@Test
	void elPanelConfiguradoPuedeLlamarALaApi() throws Exception {
		mvc.perform(options("/api/personal").header(HttpHeaders.ORIGIN, "http://localhost:8081")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:8081"));
	}

	@Test
	void otroOrigenNoPuede() throws Exception {
		mvc.perform(options("/api/personal").header(HttpHeaders.ORIGIN, "https://sitio-malicioso.com")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(status().isForbidden())
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

}
