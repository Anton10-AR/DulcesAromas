package bo.dulcesaromas.seguridad.acceso;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

/**
 * Tokens al azar para sesiones y enlaces de recuperación. En la base solo se guarda su hash, así que una copia
 * de la base no permite suplantar a nadie.
 */
public final class Tokens {

	private static final SecureRandom ALEATORIO = new SecureRandom();

	private static final String PREFIJO = "Bearer ";

	private Tokens() {
	}

	public static String nuevo() {
		byte[] bytes = new byte[32];
		ALEATORIO.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/**
	 * El token de la cabecera {@code Authorization: Bearer}, si la solicitud trae uno.
	 */
	public static Optional<String> deSolicitud(HttpServletRequest solicitud) {
		String cabecera = solicitud.getHeader(HttpHeaders.AUTHORIZATION);
		if (cabecera == null || !cabecera.startsWith(PREFIJO) || cabecera.length() == PREFIJO.length()) {
			return Optional.empty();
		}
		return Optional.of(cabecera.substring(PREFIJO.length()).strip());
	}

	public static String hash(String token) {
		try {
			byte[] resumen = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(resumen);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
