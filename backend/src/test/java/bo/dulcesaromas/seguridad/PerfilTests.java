package bo.dulcesaromas.seguridad;

import bo.dulcesaromas.PruebaIntegracion;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU17: ver y actualizar los datos propios y cambiar la contraseña (CU13, RNF03).
 */
class PerfilTests extends PruebaIntegracion {

	private ResultActions cambiarContrasena(String token, String actual, String nueva) throws Exception {
		return mvc.perform(con(token, put("/api/perfil/contrasena"))
			.content("{\"actual\":\"%s\",\"nueva\":\"%s\"}".formatted(actual, nueva)));
	}

	@Test
	void elClienteEditaNombreTelefonoYDireccionPeroNoElCorreo() throws Exception {
		crearCliente("ana@correo.bo");
		String ana = iniciarSesion("ana@correo.bo");

		mvc.perform(con(ana, put("/api/perfil")).content("""
				{"nombre":"Ana María","telefono":"70012345","direccionHabitual":"Calle Junín 123","correo":"otro@correo.bo"}"""))
			.andExpect(status().isOk());

		mvc.perform(con(ana, get("/api/perfil"))).andExpect(status().isOk())
			.andExpect(jsonPath("$.nombre").value("Ana María"))
			.andExpect(jsonPath("$.telefono").value("70012345"))
			.andExpect(jsonPath("$.direccionHabitual").value("Calle Junín 123"))
			.andExpect(jsonPath("$.correo").value("ana@correo.bo"));
	}

	@Test
	void elPersonalEditaSuNombreYTelefonoSinDireccion() throws Exception {
		crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		String vendedor = iniciarSesion("vendedor@dulces.bo");

		mvc.perform(con(vendedor, put("/api/perfil")).content("{\"nombre\":\"Luz\",\"telefono\":\"\",\"direccionHabitual\":\"X\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nombre").value("Luz"))
			.andExpect(jsonPath("$.telefono").value(nullValue()))
			.andExpect(jsonPath("$.direccionHabitual").value(nullValue()));
	}

	@Test
	void paraCambiarLaContrasenaHayQueIngresarLaActual() throws Exception {
		crearCliente("ana@correo.bo");
		String ana = iniciarSesion("ana@correo.bo");

		cambiarContrasena(ana, "Equivocada1", "NuevaClave9").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.campos.actual").isNotEmpty());
		intentarIniciarSesion("ana@correo.bo", CONTRASENA).andExpect(status().isOk());
	}

	@Test
	void rnf03LaNuevaCumpleLosRequisitos() throws Exception {
		crearCliente("ana@correo.bo");

		cambiarContrasena(iniciarSesion("ana@correo.bo"), CONTRASENA, "sinnumeros").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.nueva").isNotEmpty());
	}

	@Test
	void alCambiarlaSeCierranLasOtrasSesionesYEstaSigue() throws Exception {
		crearCliente("ana@correo.bo");
		String esta = iniciarSesion("ana@correo.bo");
		String otra = iniciarSesion("ana@correo.bo");

		cambiarContrasena(esta, CONTRASENA, "NuevaClave9").andExpect(status().isNoContent());

		mvc.perform(con(esta, get("/api/auth/yo"))).andExpect(status().isOk());
		mvc.perform(con(otra, get("/api/auth/yo"))).andExpect(status().isUnauthorized());
		intentarIniciarSesion("ana@correo.bo", "NuevaClave9").andExpect(status().isOk());
	}

	@Test
	void trasUnRestablecimientoElCambioQuitaElAviso() throws Exception {
		crearPersonal("admin@dulces.bo", "ADMINISTRADOR");
		long vendedorId = crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		mvc.perform(con(iniciarSesion("admin@dulces.bo"), post("/api/personal/" + vendedorId + "/contrasena"))
			.content("{\"contrasenaNueva\":\"" + CONTRASENA + "\"}"))
			.andExpect(status().isNoContent());
		String vendedor = iniciarSesion("vendedor@dulces.bo");
		mvc.perform(con(vendedor, get("/api/perfil"))).andExpect(jsonPath("$.debeCambiarContrasena").value(true));

		cambiarContrasena(vendedor, CONTRASENA, "MiClave2026").andExpect(status().isNoContent());

		mvc.perform(con(vendedor, get("/api/perfil"))).andExpect(jsonPath("$.debeCambiarContrasena").value(false));
	}

}
