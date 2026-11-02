package bo.dulcesaromas.cobros.pagos;

import java.util.Set;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.compartido.TipoArchivo;
import org.springframework.http.HttpStatus;

final class ValidadorArchivos {

	private static final int MB = 1024 * 1024;

	private ValidadorArchivos() {
	}

	/**
	 * Comprobante de pago: JPG, PNG o PDF de hasta 5 MB (RNF09).
	 */
	static Archivo comprobante(byte[] datos) {
		return validar(datos, Set.of(TipoArchivo.JPEG, TipoArchivo.PNG, TipoArchivo.PDF), 5, "COMPROBANTE_INVALIDO",
				"Adjunta el comprobante en JPG, PNG o PDF.");
	}

	/**
	 * Imagen del QR de un método de pago: JPG o PNG de hasta 2 MB.
	 */
	static Archivo qr(byte[] datos) {
		return validar(datos, Set.of(TipoArchivo.JPEG, TipoArchivo.PNG), 2, "QR_INVALIDO",
				"Usa una imagen JPG o PNG del QR.");
	}

	private static Archivo validar(byte[] datos, Set<TipoArchivo> permitidos, int megas, String codigo,
			String formatoValido) {
		if (datos.length == 0) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, codigo, "El archivo está vacío.");
		}
		if (datos.length > megas * MB) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, codigo,
					"El archivo pesa más de " + megas + " MB. Redúcelo e inténtalo de nuevo.");
		}
		TipoArchivo tipo = TipoArchivo.detectar(datos).filter(permitidos::contains)
			.orElseThrow(() -> new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, codigo, formatoValido));
		return new Archivo(tipo.getTipoContenido(), datos);
	}

}
