package bo.dulcesaromas.seguridad.cuentas;

import java.time.Duration;

import bo.dulcesaromas.seguridad.acceso.PropiedadesSeguridad;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * CU12 en la nube: el correo de recuperación sale por la API HTTPS de Brevo, con la clave en la cabecera.
 */
class EnviadorCorreoBrevoTests {

	private final RestClient.Builder constructor = RestClient.builder();

	private final MockRestServiceServer brevo = MockRestServiceServer.bindTo(constructor).build();

	private final EnviadorCorreo enviador = new EnviadorCorreoBrevo(constructor, "clave-de-prueba",
			"pasteleria@ejemplo.bo");

	@Test
	void enviaElCorreoConLaClaveYElRemitente() {
		brevo.expect(requestTo(EnviadorCorreoBrevo.API + "/smtp/email"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header("api-key", "clave-de-prueba"))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.sender.email").value("pasteleria@ejemplo.bo"))
			.andExpect(jsonPath("$.sender.name").value("Dulces Aromas"))
			.andExpect(jsonPath("$.to[0].email").value("ana@correo.bo"))
			.andExpect(jsonPath("$.subject").value("Recupera tu contraseña"))
			.andExpect(jsonPath("$.textContent").value("Abre el enlace."))
			.andRespond(withSuccess("{\"messageId\":\"<1@brevo>\"}", MediaType.APPLICATION_JSON));

		enviador.enviar("ana@correo.bo", "Recupera tu contraseña", "Abre el enlace.");

		brevo.verify();
	}

	@Test
	void conClaveDeBrevoSeUsaBrevoYSinEllaSmtp() {
		var sinSmtp = new StaticListableBeanFactory().getBeanProvider(JavaMailSender.class);

		assertThat(new ConfiguracionCorreo().enviadorCorreo(propiedades("clave"), sinSmtp))
			.isInstanceOf(EnviadorCorreoBrevo.class);
		assertThat(new ConfiguracionCorreo().enviadorCorreo(propiedades(""), sinSmtp))
			.isInstanceOf(EnviadorCorreoSmtp.class);
	}

	private static PropiedadesSeguridad propiedades(String claveBrevo) {
		return new PropiedadesSeguridad(null, null, null,
				new PropiedadesSeguridad.Recuperacion(Duration.ofMinutes(60), "http://x", "a@b.bo", claveBrevo));
	}

	@Test
	void unErrorDeBrevoNoSeOculta() {
		brevo.expect(requestTo(EnviadorCorreoBrevo.API + "/smtp/email"))
			.andRespond(withStatus(HttpStatus.UNAUTHORIZED));

		assertThatThrownBy(() -> enviador.enviar("ana@correo.bo", "Asunto", "Texto"))
			.isInstanceOf(HttpClientErrorException.class);
	}

}
