package bo.dulcesaromas.seguridad;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bo.dulcesaromas.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU16: recuperar la contraseña (CU12, D5).
 */
class RecuperacionTests extends PruebaIntegracion {

	private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

	private ResultActions solicitar(String correo) throws Exception {
		return mvc.perform(post("/api/auth/recuperacion").contentType(MediaType.APPLICATION_JSON)
			.content("{\"correo\":\"" + correo + "\"}"));
	}

	private ResultActions confirmar(String token, String nueva) throws Exception {
		return mvc.perform(post("/api/auth/recuperacion/confirmar").contentType(MediaType.APPLICATION_JSON)
			.content("{\"token\":\"%s\",\"contrasenaNueva\":\"%s\"}".formatted(token, nueva)));
	}

	private String tokenDelCorreo() {
		Matcher m = TOKEN.matcher(correos.enviados().getLast().texto());
		assertThat(m.find()).isTrue();
		return m.group(1);
	}

	@Test
	void elClienteRecibeUnEnlaceYDefineUnaContrasenaNueva() throws Exception {
		crearCliente("ana@correo.bo");
		String sesionAnterior = iniciarSesion("ana@correo.bo");

		solicitar("Ana@Correo.bo").andExpect(status().isAccepted());
		assertThat(correos.enviados()).singleElement().satisfies(c -> {
			assertThat(c.para()).isEqualTo("ana@correo.bo");
			assertThat(c.texto()).contains("60 minutos");
		});

		confirmar(tokenDelCorreo(), "NuevaClave9").andExpect(status().isNoContent());

		intentarIniciarSesion("ana@correo.bo", "NuevaClave9").andExpect(status().isOk());
		intentarIniciarSesion("ana@correo.bo", CONTRASENA).andExpect(status().isUnauthorized());
		mvc.perform(con(sesionAnterior, get("/api/auth/yo"))).andExpect(status().isUnauthorized());
	}

	@Test
	void elMensajeEsIgualExistaONoElCorreo() throws Exception {
		crearCliente("ana@correo.bo");
		crearPersonal("vendedor@dulces.bo", "VENDEDOR");

		String existe = solicitar("ana@correo.bo").andReturn().getResponse().getContentAsString();
		String noExiste = solicitar("nadie@correo.bo").andExpect(status().isAccepted())
			.andReturn().getResponse().getContentAsString();
		String personal = solicitar("vendedor@dulces.bo").andExpect(status().isAccepted())
			.andReturn().getResponse().getContentAsString();

		assertThat(existe).isEqualTo(noExiste).isEqualTo(personal);
		// D5: al personal no se le envía correo; se la restablece el administrador.
		assertThat(correos.enviados()).hasSize(1);
	}

	@Test
	void elEnlaceEsDeUnSoloUso() throws Exception {
		crearCliente("ana@correo.bo");
		solicitar("ana@correo.bo");
		String token = tokenDelCorreo();

		confirmar(token, "NuevaClave9").andExpect(status().isNoContent());

		confirmar(token, "OtraClave9").andExpect(status().isGone())
			.andExpect(jsonPath("$.codigo").value("ENLACE_VENCIDO"));
	}

	@Test
	void elEnlaceVenceEn60Minutos() throws Exception {
		crearCliente("ana@correo.bo");
		solicitar("ana@correo.bo");

		reloj.adelantar(Duration.ofMinutes(60));

		confirmar(tokenDelCorreo(), "NuevaClave9").andExpect(status().isGone());
	}

	@Test
	void unEnlaceNuevoAnulaLosAnterioresAlUsarse() throws Exception {
		crearCliente("ana@correo.bo");
		solicitar("ana@correo.bo");
		String primero = tokenDelCorreo();
		solicitar("ana@correo.bo");

		confirmar(tokenDelCorreo(), "NuevaClave9").andExpect(status().isNoContent());

		confirmar(primero, "OtraClave9").andExpect(status().isGone());
	}

	@Test
	void laContrasenaNuevaCumpleLaPolitica() throws Exception {
		crearCliente("ana@correo.bo");
		solicitar("ana@correo.bo");

		confirmar(tokenDelCorreo(), "corta").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.contrasenaNueva").isNotEmpty());
	}

	@Test
	void d5ElAdministradorRestableceLaContrasenaDelPersonal() throws Exception {
		crearPersonal("admin@dulces.bo", "ADMINISTRADOR");
		long vendedorId = crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		String admin = iniciarSesion("admin@dulces.bo");
		String sesionDelVendedor = iniciarSesion("vendedor@dulces.bo");

		mvc.perform(con(admin, post("/api/personal/" + vendedorId + "/contrasena")).content("{\"contrasenaNueva\":\"Temporal123\"}"))
			.andExpect(status().isNoContent());

		mvc.perform(con(sesionDelVendedor, get("/api/auth/yo"))).andExpect(status().isUnauthorized());
		intentarIniciarSesion("vendedor@dulces.bo", "Temporal123").andExpect(status().isOk())
			.andExpect(jsonPath("$.debeCambiarContrasena").value(true));
		assertThat(registrosDeAuditoria("CONTRASENA_RESTABLECIDA")).isEqualTo(1);

		String vendedor = iniciarSesion("vendedor@dulces.bo", "Temporal123");
		mvc.perform(con(vendedor, post("/api/personal/" + vendedorId + "/contrasena")).content("{\"contrasenaNueva\":\"Otra12345\"}"))
			.andExpect(status().isForbidden());
	}

	private String iniciarSesion(String correo, String contrasena) throws Exception {
		return JsonPath.read(intentarIniciarSesion(correo, contrasena).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString(), "$.token");
	}

}
