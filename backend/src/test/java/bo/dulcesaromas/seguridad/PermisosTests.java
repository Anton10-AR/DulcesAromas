package bo.dulcesaromas.seguridad;

import bo.dulcesaromas.PruebaIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU04 y HU05: roles base y sus permisos (CU09, RN04, D6).
 */
class PermisosTests extends PruebaIntegracion {

	private String admin;

	@BeforeEach
	void administrador() throws Exception {
		crearPersonal("admin@dulces.bo", "ADMINISTRADOR");
		admin = iniciarSesion("admin@dulces.bo");
	}

	@Test
	void muestraLosTresRolesConSuDescripcionYSusPermisos() throws Exception {
		mvc.perform(con(admin, get("/api/roles"))).andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(3))
			.andExpect(jsonPath("$[0].codigo").value("ADMINISTRADOR"))
			.andExpect(jsonPath("$[1].nombre").value("Vendedor"))
			.andExpect(jsonPath("$[1].descripcion").isNotEmpty())
			.andExpect(jsonPath("$[1].permisos", not(hasItem("COBRANZA_VERIFICAR"))))
			.andExpect(jsonPath("$[2].permisos[0]").value("DISTRIBUCION_ENTREGAS"));
	}

	@Test
	void d6NoExisteLaOpcionDeCrearRoles() throws Exception {
		mvc.perform(con(admin, post("/api/roles")).content("{\"codigo\":\"PRODUCCION\"}"))
			.andExpect(status().is4xxClientError());

		assertThat(jdbc.queryForObject("select count(*) from rol", Integer.class)).isEqualTo(3);
	}

	@Test
	void unCambioDePermisosRigeAntesDelSiguienteInicioDeSesion() throws Exception {
		crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		String vendedor = iniciarSesion("vendedor@dulces.bo");

		mvc.perform(con(admin, put("/api/roles/VENDEDOR/permisos"))
			.content("{\"permisos\":[\"PEDIDOS_CONSULTAR\",\"COBRANZA_VERIFICAR\"]}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.permisos.length()").value(2));

		mvc.perform(con(vendedor, get("/api/auth/yo")))
			.andExpect(jsonPath("$.permisos", hasItem("COBRANZA_VERIFICAR")))
			.andExpect(jsonPath("$.permisos", not(hasItem("PEDIDOS_CANCELAR"))));
		assertThat(jdbc.queryForObject("select detalle from auditoria where accion = 'PERMISOS_EDITADOS'", String.class))
			.contains("Agregados: COBRANZA_VERIFICAR").contains("PEDIDOS_CANCELAR");
	}

	@Test
	void rn04ElAdministradorSiempreConservaElAccesoASeguridad() throws Exception {
		mvc.perform(con(admin, put("/api/roles/ADMINISTRADOR/permisos")).content("{\"permisos\":[\"REPORTES\"]}"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("ADMINISTRADOR_SIN_SEGURIDAD"));

		mvc.perform(con(admin, get("/api/personal"))).andExpect(status().isOk());
		assertThat(registrosDeAuditoria("PERMISOS_EDITADOS")).isZero();
	}

	@Test
	void unPermisoDesconocidoSeRechaza() throws Exception {
		mvc.perform(con(admin, put("/api/roles/VENDEDOR/permisos")).content("{\"permisos\":[\"BORRAR_TODO\"]}"))
			.andExpect(status().isBadRequest());
	}

}
