package bo.dulcesaromas.seguridad;

import bo.dulcesaromas.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU15: registrarse como cliente (CU11, RN01, RN16, RNF03, RNF04).
 */
class RegistroClienteTests extends PruebaIntegracion {

	private ResultActions registrar(String correo, String contrasena) throws Exception {
		return mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"Ana Pérez\",\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, contrasena)));
	}

	@Test
	void alRegistrarseQuedaComoClienteConLaSesionIniciada() throws Exception {
		String cuerpo = registrar("ana@correo.bo", "Torta2026").andExpect(status().isCreated())
			.andExpect(jsonPath("$.usuario.tipo").value("CLIENTE"))
			.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(cuerpo, "$.token");

		mvc.perform(con(token, get("/api/auth/yo"))).andExpect(status().isOk())
			.andExpect(jsonPath("$.correo").value("ana@correo.bo"))
			.andExpect(jsonPath("$.rol").value(nullValue()));
	}

	@Test
	void rn01SiElCorreoYaExisteOfreceIniciarSesionORecuperarLaContrasena() throws Exception {
		crearPersonal("ana@correo.bo", "VENDEDOR");

		registrar("ANA@correo.bo", "Torta2026").andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("CORREO_REGISTRADO"))
			.andExpect(jsonPath("$.mensaje").value(containsString("recuperar tu contraseña")));
	}

	@Test
	void rnf03LaContrasenaNecesitaOchoCaracteresConLetraYNumero() throws Exception {
		for (String mala : new String[] { "Corta1", "SoloLetras", "12345678" }) {
			registrar("ana@correo.bo", mala).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.contrasena").isNotEmpty());
		}
		assertThat(jdbc.queryForObject("select count(*) from usuario", Integer.class)).isZero();
	}

	@Test
	void unErrorDeFormularioNombraElCampo() throws Exception {
		registrar("no-es-un-correo", "Torta2026").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.correo").isNotEmpty());
	}

	@Test
	void rnf04LaContrasenaSeGuardaConHashYSal() throws Exception {
		registrar("ana@correo.bo", "Torta2026").andExpect(status().isCreated());
		registrar("beto@correo.bo", "Torta2026").andExpect(status().isCreated());

		var hashes = jdbc.queryForList("select contrasena_hash from usuario order by id", String.class);
		assertThat(hashes).allSatisfy(h -> assertThat(h).startsWith("$2").doesNotContain("Torta2026"));
		assertThat(hashes.get(0)).isNotEqualTo(hashes.get(1));
	}

	@Test
	void rn16UnClienteRegistradoNoTieneRolDePersonal() throws Exception {
		registrar("ana@correo.bo", "Torta2026").andExpect(status().isCreated());

		assertThat(jdbc.queryForMap("select tipo, rol from usuario where correo = 'ana@correo.bo'"))
			.containsEntry("tipo", "CLIENTE").containsEntry("rol", null);
	}

}
