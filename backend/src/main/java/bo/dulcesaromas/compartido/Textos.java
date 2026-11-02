package bo.dulcesaromas.compartido;

import java.text.Normalizer;
import java.util.Locale;

public final class Textos {

	private Textos() {
	}

	/**
	 * Quita espacios sobrantes.
	 */
	public static String limpio(String texto) {
		return texto.strip().replaceAll("\\s+", " ");
	}

	/**
	 * El texto sin mayúsculas, tildes ni espacios sobrantes, para comparar y buscar.
	 */
	public static String clave(String texto) {
		String sinTildes = Normalizer.normalize(limpio(texto), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return sinTildes.toLowerCase(Locale.ROOT);
	}

}
