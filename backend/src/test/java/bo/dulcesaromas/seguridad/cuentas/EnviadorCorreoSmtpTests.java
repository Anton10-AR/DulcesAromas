package bo.dulcesaromas.seguridad.cuentas;

import java.time.Duration;
import java.util.Map;

import bo.dulcesaromas.seguridad.acceso.PropiedadesSeguridad;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Sin MAIL_HOST, como en desarrollo, el correo se escribe en el registro en vez de fallar.
 */
class EnviadorCorreoSmtpTests {

	private static final PropiedadesSeguridad PROPIEDADES = new PropiedadesSeguridad(null, null, null,
			new PropiedadesSeguridad.Recuperacion(Duration.ofMinutes(60), "http://x", "a@b.bo", null));

	@Test
	void sinServidorDeCorreoNoFalla() {
		var sinServidor = new StaticListableBeanFactory().getBeanProvider(JavaMailSender.class);

		assertThatNoException().isThrownBy(
				() -> new EnviadorCorreoSmtp(sinServidor, PROPIEDADES).enviar("ana@correo.bo", "Asunto", "Texto"));
	}

	@Test
	void conElHostVacioNoIntentaConectarse() {
		var sinHost = new StaticListableBeanFactory(Map.of("correo", new JavaMailSenderImpl()))
			.getBeanProvider(JavaMailSender.class);

		assertThatNoException().isThrownBy(
				() -> new EnviadorCorreoSmtp(sinHost, PROPIEDADES).enviar("ana@correo.bo", "Asunto", "Texto"));
	}

}
