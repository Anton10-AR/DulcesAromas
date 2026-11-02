package bo.dulcesaromas.seguridad;

import java.time.Duration;

import bo.dulcesaromas.PruebaIntegracion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU01 y HU02: iniciar y cerrar sesión (CU01, RN02, RNF05, RNF06).
 */
class InicioSesionTests extends PruebaIntegracion {

	@Test
	void elPersonalEntraAlPanelConLosPermisosDeSuRol() throws Exception {
		crearPersonal("vendedor@dulces.bo", "VENDEDOR");

		intentarIniciarSesion("Vendedor@Dulces.bo ", CONTRASENA).andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isNotEmpty())
			.andExpect(jsonPath("$.usuario.tipo").value("PERSONAL"))
			.andExpect(jsonPath("$.usuario.rol").value("VENDEDOR"))
			.andExpect(jsonPath("$.usuario.permisos", hasItem("PEDIDOS_CONSULTAR")));
	}

	@Test
	void elClienteEntraALaTiendaSinRolNiPermisos() throws Exception {
		crearCliente("cliente@correo.bo");

		intentarIniciarSesion("cliente@correo.bo", CONTRASENA).andExpect(status().isOk())
			.andExpect(jsonPath("$.usuario.tipo").value("CLIENTE"))
			.andExpect(jsonPath("$.usuario.rol").value(nullValue()))
			.andExpect(jsonPath("$.usuario.permisos").isEmpty());
	}

	@Test
	void conCredencialesIncorrectasElMensajeNoDiceQueDatoFallo() throws Exception {
		crearCliente("cliente@correo.bo");

		String contrasenaMala = intentarIniciarSesion("cliente@correo.bo", "Otra12345").andExpect(status().isUnauthorized())
			.andReturn().getResponse().getContentAsString();
		String correoInexistente = intentarIniciarSesion("nadie@correo.bo", CONTRASENA).andExpect(status().isUnauthorized())
			.andReturn().getResponse().getContentAsString();

		assertThat(contrasenaMala).isEqualTo(correoInexistente).contains("CREDENCIALES_INCORRECTAS");
	}

	@Test
	void rn02UnaCuentaDesactivadaNoEntraNiConLaContrasenaCorrecta() throws Exception {
		long id = crearPersonal("ex@dulces.bo", "VENDEDOR");
		jdbc.update("update usuario set activo = false where id = ?", id);

		intentarIniciarSesion("ex@dulces.bo", CONTRASENA).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.codigo").value("CREDENCIALES_INCORRECTAS"));
	}

	@Test
	void cincoIntentosFallidosSeguidosBloqueanLaCuentaQuinceMinutos() throws Exception {
		crearCliente("cliente@correo.bo");
		for (int i = 1; i <= 4; i++) {
			intentarIniciarSesion("cliente@correo.bo", "Mala12345").andExpect(status().isUnauthorized());
		}
		intentarIniciarSesion("cliente@correo.bo", "Mala12345").andExpect(status().isLocked())
			.andExpect(jsonPath("$.codigo").value("CUENTA_BLOQUEADA"));
		assertThat(registrosDeAuditoria("CUENTA_BLOQUEADA")).isEqualTo(1);

		reloj.adelantar(Duration.ofMinutes(14));
		intentarIniciarSesion("cliente@correo.bo", CONTRASENA).andExpect(status().isLocked());

		reloj.adelantar(Duration.ofMinutes(1));
		intentarIniciarSesion("cliente@correo.bo", CONTRASENA).andExpect(status().isOk());
	}

	@Test
	void unIngresoCorrectoReiniciaElConteoDeIntentos() throws Exception {
		crearCliente("cliente@correo.bo");
		for (int i = 1; i <= 4; i++) {
			intentarIniciarSesion("cliente@correo.bo", "Mala12345").andExpect(status().isUnauthorized());
		}
		iniciarSesion("cliente@correo.bo");

		intentarIniciarSesion("cliente@correo.bo", "Mala12345").andExpect(status().isUnauthorized());
	}

	@Test
	void alCerrarSesionElTokenDejaDeServir() throws Exception {
		crearCliente("cliente@correo.bo");
		String token = iniciarSesion("cliente@correo.bo");
		mvc.perform(con(token, get("/api/auth/yo"))).andExpect(status().isOk());

		mvc.perform(con(token, post("/api/auth/logout"))).andExpect(status().isNoContent());

		mvc.perform(con(token, get("/api/auth/yo"))).andExpect(status().isUnauthorized());
	}

	@Test
	void laSesionDelPanelSeCierraTras30MinutosSinUso() throws Exception {
		crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		String token = iniciarSesion("vendedor@dulces.bo");

		reloj.adelantar(Duration.ofMinutes(29));
		mvc.perform(con(token, get("/api/auth/yo"))).andExpect(status().isOk());

		// Cada uso reinicia el plazo: 29 minutos más todavía no la cierran, 30 sí.
		reloj.adelantar(Duration.ofMinutes(29));
		mvc.perform(con(token, get("/api/auth/yo"))).andExpect(status().isOk());

		reloj.adelantar(Duration.ofMinutes(30));
		mvc.perform(con(token, get("/api/auth/yo"))).andExpect(status().isUnauthorized());
	}

	@Test
	void laSesionDeLaTiendaNoSeCierraALos30Minutos() throws Exception {
		crearCliente("cliente@correo.bo");
		String token = iniciarSesion("cliente@correo.bo");

		reloj.adelantar(Duration.ofHours(2));

		mvc.perform(con(token, get("/api/auth/yo"))).andExpect(status().isOk());
	}

}
