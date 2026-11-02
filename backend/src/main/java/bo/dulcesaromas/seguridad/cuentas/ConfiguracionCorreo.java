package bo.dulcesaromas.seguridad.cuentas;

import bo.dulcesaromas.seguridad.acceso.PropiedadesSeguridad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Elige cómo sale el correo de recuperación (CU12): por la API de Brevo si hay BREVO_CLAVE (en la nube), si no por
 * SMTP con MAIL_HOST, y si tampoco, se escribe en el registro (solo desarrollo).
 */
@Configuration
class ConfiguracionCorreo {

	private static final Logger log = LoggerFactory.getLogger(ConfiguracionCorreo.class);

	@Bean
	EnviadorCorreo enviadorCorreo(PropiedadesSeguridad propiedades, ObjectProvider<JavaMailSender> smtp) {
		PropiedadesSeguridad.Recuperacion recuperacion = propiedades.recuperacion();
		if (StringUtils.hasText(recuperacion.brevoClave())) {
			log.info("El correo de recuperación se envía por la API de Brevo, desde {}.", recuperacion.remitente());
			return new EnviadorCorreoBrevo(RestClient.builder(), recuperacion.brevoClave(), recuperacion.remitente());
		}
		return new EnviadorCorreoSmtp(smtp, propiedades);
	}

}
