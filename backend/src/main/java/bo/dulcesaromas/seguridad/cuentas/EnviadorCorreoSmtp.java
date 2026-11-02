package bo.dulcesaromas.seguridad.cuentas;

import bo.dulcesaromas.seguridad.acceso.PropiedadesSeguridad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

/**
 * Envía por el servidor SMTP de {@code spring.mail.*} (variables MAIL_HOST, MAIL_USUARIO, MAIL_CONTRASENA). Sin
 * servidor configurado, como en desarrollo, escribe el correo en el registro en vez de enviarlo. Lo elige
 * {@link ConfiguracionCorreo} cuando no hay clave de Brevo.
 */
class EnviadorCorreoSmtp implements EnviadorCorreo {

	private static final Logger log = LoggerFactory.getLogger(EnviadorCorreoSmtp.class);

	private final ObjectProvider<JavaMailSender> servidor;

	private final PropiedadesSeguridad propiedades;

	EnviadorCorreoSmtp(ObjectProvider<JavaMailSender> servidor, PropiedadesSeguridad propiedades) {
		this.servidor = servidor;
		this.propiedades = propiedades;
	}

	@Override
	public void enviar(String para, String asunto, String texto) {
		JavaMailSender smtp = servidor.getIfAvailable();
		// spring.mail.host vacío igual crea el servidor; sin host no hay a dónde enviar.
		boolean sinHost = smtp instanceof JavaMailSenderImpl impl && !StringUtils.hasText(impl.getHost());
		if (smtp == null || sinHost) {
			log.warn("Sin servidor de correo configurado. Correo para {}: {}\n{}", para, asunto, texto);
			return;
		}
		var mensaje = new SimpleMailMessage();
		mensaje.setFrom(propiedades.recuperacion().remitente());
		mensaje.setTo(para);
		mensaje.setSubject(asunto);
		mensaje.setText(texto);
		smtp.send(mensaje);
	}

}
