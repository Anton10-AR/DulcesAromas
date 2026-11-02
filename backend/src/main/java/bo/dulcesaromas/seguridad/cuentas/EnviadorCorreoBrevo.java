package bo.dulcesaromas.seguridad.cuentas;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Envía por la API web de Brevo (HTTPS), para los servicios en la nube que bloquean los puertos SMTP, como el plan
 * gratuito de Render. La clave sale de la variable BREVO_CLAVE; el remitente, de MAIL_REMITENTE, y debe estar
 * verificado en Brevo.
 */
class EnviadorCorreoBrevo implements EnviadorCorreo {

	static final String API = "https://api.brevo.com/v3";

	static final String NOMBRE_REMITENTE = "Dulces Aromas";

	private final RestClient cliente;

	private final String remitente;

	EnviadorCorreoBrevo(RestClient.Builder constructor, String clave, String remitente) {
		this.cliente = constructor.baseUrl(API).defaultHeader("api-key", clave).build();
		this.remitente = remitente;
	}

	/**
	 * Un error de Brevo (clave inválida, remitente sin verificar, límite diario) se lanza como cualquier falla de
	 * envío, igual que con SMTP.
	 */
	@Override
	public void enviar(String para, String asunto, String texto) {
		cliente.post()
			.uri("/smtp/email")
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.APPLICATION_JSON)
			.body(Map.of("sender", Map.of("name", NOMBRE_REMITENTE, "email", remitente), "to",
					List.of(Map.of("email", para)), "subject", asunto, "textContent", texto))
			.retrieve()
			.toBodilessEntity();
	}

}
