package bo.dulcesaromas.seguridad;

import bo.dulcesaromas.PruebaIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU03 y HU04: cuentas de personal y asignación de rol (CU09, RN01, RN02, RN03, RN16).
 */
class PersonalTests extends PruebaIntegracion {

	private long adminId;

	private String admin;

	@BeforeEach
	void administrador() throws Exception {
		adminId = crearPersonal("admin@dulces.bo", "ADMINISTRADOR");
		admin = iniciarSesion("admin@dulces.bo");
	}

	private ResultActions editar(String token, long id, String correo, String rol) throws Exception {
		return mvc.perform(con(token, put("/api/personal/" + id))
			.content("{\"nombre\":\"Nombre\",\"correo\":\"%s\",\"rol\":\"%s\"}".formatted(correo, rol)));
	}

	private ResultActions desactivar(String token, long id) throws Exception {
		return mvc.perform(con(token, post("/api/personal/" + id + "/desactivar")));
	}

	@Test
	void creaUnaCuentaConNombreCorreoRolYContrasenaInicial() throws Exception {
		mvc.perform(con(admin, post("/api/personal")).content("""
				{"nombre":"Luz","correo":"luz@dulces.bo","telefono":"70000000","rol":"REPARTIDOR","contrasenaInicial":"Inicial123"}"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.rol").value("REPARTIDOR"))
			.andExpect(jsonPath("$.activo").value(true));

		intentarIniciarSesion("luz@dulces.bo", "Inicial123").andExpect(status().isOk())
			.andExpect(jsonPath("$.usuario.rol").value("REPARTIDOR"));
		assertThat(registrosDeAuditoria("CUENTA_CREADA")).isEqualTo(1);
	}

	@Test
	void laContrasenaInicialCumpleLaPolitica() throws Exception {
		mvc.perform(con(admin, post("/api/personal")).content("""
				{"nombre":"Luz","correo":"luz@dulces.bo","rol":"REPARTIDOR","contrasenaInicial":"corta"}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.contrasenaInicial").isNotEmpty());
	}

	@Test
	void rn01NoSeRepiteUnCorreoNiSiquieraDeUnCliente() throws Exception {
		crearCliente("cliente@correo.bo");
		long vendedor = crearPersonal("vendedor@dulces.bo", "VENDEDOR");

		mvc.perform(con(admin, post("/api/personal")).content("""
				{"nombre":"Otro","correo":"Cliente@Correo.bo","rol":"VENDEDOR","contrasenaInicial":"Inicial123"}"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("CORREO_REGISTRADO"));
		editar(admin, vendedor, "admin@dulces.bo", "VENDEDOR").andExpect(status().isConflict());
	}

	@Test
	void rn02AlDesactivarPierdeElAccesoYConservaSuHistorial() throws Exception {
		long vendedor = crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		String sesionDelVendedor = iniciarSesion("vendedor@dulces.bo");

		desactivar(admin, vendedor).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));

		mvc.perform(con(sesionDelVendedor, get("/api/auth/yo"))).andExpect(status().isUnauthorized());
		intentarIniciarSesion("vendedor@dulces.bo", CONTRASENA).andExpect(status().isUnauthorized());
		mvc.perform(con(admin, get("/api/personal/" + vendedor))).andExpect(status().isOk())
			.andExpect(jsonPath("$.correo").value("vendedor@dulces.bo"));
		assertThat(registrosDeAuditoria("CUENTA_DESACTIVADA")).isEqualTo(1);

		mvc.perform(con(admin, post("/api/personal/" + vendedor + "/activar"))).andExpect(status().isOk());
		iniciarSesion("vendedor@dulces.bo");
	}

	@Test
	void rn03NadieDesactivaSuPropiaCuenta() throws Exception {
		crearPersonal("otro-admin@dulces.bo", "ADMINISTRADOR");

		desactivar(admin, adminId).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("PROPIA_CUENTA"));
	}

	@Test
	void rn03SiempreQuedaAlMenosUnAdministradorActivo() throws Exception {
		// Un vendedor al que se le dio el permiso de Seguridad intenta dejar el sistema sin administradores.
		jdbc.update("insert into rol_permiso (rol, permiso) values ('VENDEDOR', 'SEGURIDAD')");
		crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		String vendedor = iniciarSesion("vendedor@dulces.bo");

		desactivar(vendedor, adminId).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("ULTIMO_ADMINISTRADOR"));
		editar(vendedor, adminId, "admin@dulces.bo", "VENDEDOR").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("ULTIMO_ADMINISTRADOR"));

		long otroAdmin = crearPersonal("otro-admin@dulces.bo", "ADMINISTRADOR");
		desactivar(admin, otroAdmin).andExpect(status().isOk());
	}

	@Test
	void asignarOCambiarElRolRigeEnLaSiguienteSolicitud() throws Exception {
		long vendedor = crearPersonal("vendedor@dulces.bo", "VENDEDOR");
		String sesionDelVendedor = iniciarSesion("vendedor@dulces.bo");

		editar(admin, vendedor, "vendedor@dulces.bo", "REPARTIDOR").andExpect(status().isOk())
			.andExpect(jsonPath("$.rol").value("REPARTIDOR"));

		mvc.perform(con(sesionDelVendedor, get("/api/auth/yo"))).andExpect(jsonPath("$.rol").value("REPARTIDOR"))
			.andExpect(jsonPath("$.permisos").value(contains("DISTRIBUCION_ENTREGAS")));
		assertThat(registrosDeAuditoria("ROL_CAMBIADO")).isEqualTo(1);
	}

	@Test
	void unAdministradorNoCambiaSuPropioRol() throws Exception {
		crearPersonal("otro-admin@dulces.bo", "ADMINISTRADOR");

		editar(admin, adminId, "admin@dulces.bo", "VENDEDOR").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.codigo").value("PROPIO_ROL"));
	}

	@Test
	void rn16UnClienteNoPuedeRecibirUnRolDePersonal() throws Exception {
		long cliente = crearCliente("cliente@correo.bo");

		editar(admin, cliente, "cliente@correo.bo", "VENDEDOR").andExpect(status().isNotFound());
		mvc.perform(con(admin, get("/api/personal"))).andExpect(jsonPath("$[*].correo")
			.value(not(hasItem("cliente@correo.bo"))));
		assertThat(jdbc.queryForObject("select tipo from usuario where id = ?", String.class, cliente))
			.isEqualTo("CLIENTE");
	}

}
