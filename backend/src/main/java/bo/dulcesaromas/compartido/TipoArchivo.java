package bo.dulcesaromas.compartido;

import java.util.Optional;

/**
 * Formatos de archivo que acepta el sistema, reconocidos por sus primeros bytes y no por el nombre del archivo.
 */
public enum TipoArchivo {

	PNG("image/png"), JPEG("image/jpeg"), WEBP("image/webp"), PDF("application/pdf");

	private final String tipoContenido;

	TipoArchivo(String tipoContenido) {
		this.tipoContenido = tipoContenido;
	}

	public String getTipoContenido() {
		return tipoContenido;
	}

	public static Optional<TipoArchivo> detectar(byte[] d) {
		if (empiezaCon(d, 0, 0x89, 'P', 'N', 'G')) {
			return Optional.of(PNG);
		}
		if (empiezaCon(d, 0, 0xFF, 0xD8, 0xFF)) {
			return Optional.of(JPEG);
		}
		if (empiezaCon(d, 0, 'R', 'I', 'F', 'F') && empiezaCon(d, 8, 'W', 'E', 'B', 'P')) {
			return Optional.of(WEBP);
		}
		if (empiezaCon(d, 0, '%', 'P', 'D', 'F', '-')) {
			return Optional.of(PDF);
		}
		return Optional.empty();
	}

	private static boolean empiezaCon(byte[] d, int desde, int... esperado) {
		if (d.length < desde + esperado.length) {
			return false;
		}
		for (int i = 0; i < esperado.length; i++) {
			if ((d[desde + i] & 0xFF) != esperado[i]) {
				return false;
			}
		}
		return true;
	}

}
