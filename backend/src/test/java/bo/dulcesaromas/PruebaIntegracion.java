package bo.dulcesaromas;

import com.jayway.jsonpath.JsonPath;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas que recorren la API completa con una base H2 recién migrada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PruebaIntegracion.Configuracion.class)
public abstract class PruebaIntegracion {

	protected static final String CONTRASENA = "Secreta123";

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected JdbcTemplate jdbc;

	@Autowired
	protected RelojDePrueba reloj;

	@Autowired
	protected CorreosDePrueba correos;

	@Autowired
	private Flyway flyway;

	@Autowired
	private PasswordEncoder codificador;

	@BeforeEach
	void baseLimpia() {
		flyway.clean();
		flyway.migrate();
		reloj.reiniciar();
		correos.vaciar();
	}

	/**
	 * Crea una cuenta de personal directamente en la base y devuelve su id.
	 */
	protected long crearPersonal(String correo, String rol) {
		jdbc.update("""
				insert into usuario (tipo, nombre, correo, contrasena_hash, activo, intentos_fallidos, creado, rol)
				values ('PERSONAL', ?, ?, ?, true, 0, current_timestamp, ?)""", correo, correo,
				codificador.encode(CONTRASENA), rol);
		return idDe(correo);
	}

	protected long crearCliente(String correo) {
		jdbc.update("""
				insert into usuario (tipo, nombre, correo, contrasena_hash, activo, intentos_fallidos, creado)
				values ('CLIENTE', ?, ?, ?, true, 0, current_timestamp)""", correo, correo,
				codificador.encode(CONTRASENA));
		return idDe(correo);
	}

	protected long idDe(String correo) {
		return jdbc.queryForObject("select id from usuario where correo = ?", Long.class, correo);
	}

	protected ResultActions intentarIniciarSesion(String correo, String contrasena) throws Exception {
		return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, contrasena)));
	}

	/**
	 * @return el token de la sesión
	 */
	protected String iniciarSesion(String correo) throws Exception {
		String cuerpo = intentarIniciarSesion(correo, CONTRASENA).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(cuerpo, "$.token");
	}

	protected static MockHttpServletRequestBuilder con(String token, MockHttpServletRequestBuilder solicitud) {
		return solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
	}

	protected int registrosDeAuditoria(String accion) {
		return jdbc.queryForObject("select count(*) from auditoria where accion = ?", Integer.class, accion);
	}

	@TestConfiguration
	static class Configuracion {

		@Bean
		@Primary
		RelojDePrueba relojDePrueba() {
			return new RelojDePrueba();
		}

		@Bean
		@Primary
		CorreosDePrueba correosDePrueba() {
			return new CorreosDePrueba();
		}

	}

}
