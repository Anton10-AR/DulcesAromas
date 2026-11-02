package bo.dulcesaromas.ecommerce.catalogo;

import java.text.Normalizer;
import java.util.Locale;

final class Textos {

	private Textos() {
	}

	/**
	 * Quita espacios sobrantes.
	 */
	static String limpio(String texto) {
		return texto.strip().replaceAll("\\s+", " ");
	}

	/**
	 * El texto sin mayúsculas, tildes ni espacios sobrantes, para comparar y buscar.
	 */
	static String clave(String texto) {
		String sinTildes = Normalizer.normalize(limpio(texto), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return sinTildes.toLowerCase(Locale.ROOT);
	}

}
