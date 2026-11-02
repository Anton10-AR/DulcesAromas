package bo.dulcesaromas.seguridad;

import bo.dulcesaromas.PruebaIntegracion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU06: cada usuario ve y ejecuta solo lo que su rol permite (CU01, CU09, RNF07).
 */
class ControlAccesoTests extends PruebaIntegracion {

	@Test
	void sinSesionNoSeAccedeAlPanel() throws Exception {
		mvc.perform(get("/api/personal")).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.codigo").value("SIN_SESION"));
	}

	@Test
	void unaRutaNoPermitidaDevuelveAccesoDenegadoYQuedaRegistrada() throws Exception {
		long vendedorId = crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		String vendedor = iniciarSesion("vendedor@dulces.bo");

		mvc.perform(con(vendedor, get("/api/personal"))).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"))
			.andExpect(jsonPath("$.mensaje").value("Acceso denegado."));

		var registro = jdbc.queryForMap("select usuario_id, detalle from auditoria where accion = 'ACCESO_DENEGADO'");
		assertThat(registro).containsEntry("usuario_id", vendedorId).containsEntry("detalle", "GET /api/personal");
	}

	@Test
	void elServidorValidaCadaAccionNoSoloElMenu() throws Exception {
		crearCliente("cliente@correo.bo");
		crearPersonal("repartidor@dulces.bo", "REPARTIDOR");
		String cliente = iniciarSesion("cliente@correo.bo");
		String repartidor = iniciarSesion("repartidor@dulces.bo");

		for (String token : new String[] { cliente, repartidor }) {
			mvc.perform(con(token, put("/api/roles/REPARTIDOR/permisos")).content("{\"permisos\":[\"SEGURIDAD\"]}"))
				.andExpect(status().isForbidden());
			mvc.perform(con(token, get("/api/auditoria"))).andExpect(status().isForbidden());
		}
		assertThat(registrosDeAuditoria("ACCESO_DENEGADO")).isEqualTo(4);
		assertThat(jdbc.queryForList("select permiso from rol_permiso where rol = 'REPARTIDOR'", String.class))
			.containsExactly("DISTRIBUCION_ENTREGAS");
	}

	@Test
	void laSesionTraeLosPermisosParaArmarElMenu() throws Exception {
		crearPersonal("repartidor@dulces.bo", "REPARTIDOR");
		String repartidor = iniciarSesion("repartidor@dulces.bo");

		mvc.perform(con(repartidor, get("/api/auth/yo"))).andExpect(status().isOk())
			.andExpect(jsonPath("$.permisos", containsInAnyOrder("DISTRIBUCION_ENTREGAS")));
	}

	@Test
	void elAdministradorConsultaLaAuditoria() throws Exception {
		crearPersonal("admin@dulces.bo", "ADMINISTRADOR");
		String admin = iniciarSesion("admin@dulces.bo");
		mvc.perform(con(admin, put("/api/roles/REPARTIDOR/permisos")).content("{\"permisos\":[]}"))
			.andExpect(status().isOk());

		mvc.perform(con(admin, get("/api/auditoria"))).andExpect(status().isOk())
			.andExpect(jsonPath("$.registros[0].accion").value("PERMISOS_EDITADOS"))
			.andExpect(jsonPath("$.registros[0].usuario").value("admin@dulces.bo"));
	}

}
