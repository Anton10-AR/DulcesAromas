package bo.dulcesaromas.ecommerce.catalogo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.compartido.TipoArchivo;
import org.springframework.http.HttpStatus;

/**
 * Imagen de producto: JPG, PNG o WebP, cuadrada, desde 800 px y hasta 2 MB (RNF10). Una JPG o PNG que no es
 * cuadrada se recorta al centro. El formato se reconoce por el contenido del archivo, no por su nombre, y las
 * medidas se leen de la cabecera antes de decodificar la imagen.
 */
final class ValidadorImagen {

	static final int BYTES_MAXIMOS = 2 * 1024 * 1024;

	static final int LADO_MINIMO = 800;

	/** Una imagen comprimida de 2 MB puede ocupar mucha memoria al abrirla; más allá de esto no se recorta. */
	private static final long PIXELES_MAXIMOS = 40_000_000L;

	/**
	 * La imagen lista para guardar: la original si ya era cuadrada, o el recorte al centro.
	 */
	record Imagen(String tipo, byte[] datos) {
	}

	private ValidadorImagen() {
	}

	static Imagen preparar(byte[] datos) {
		if (datos.length == 0) {
			throw invalida("Elige una imagen.");
		}
		if (datos.length > BYTES_MAXIMOS) {
			throw invalida("La imagen pesa más de 2 MB. Redúcela e inténtalo de nuevo.");
		}
		TipoArchivo formato = TipoArchivo.detectar(datos)
			.filter(t -> t != TipoArchivo.PDF)
			.orElseThrow(() -> invalida("Usa una imagen JPG, PNG o WebP."));
		String tipo = formato.getTipoContenido();
		int[] medidas = formato == TipoArchivo.WEBP ? medidasWebp(datos) : medidasImageIo(datos, tipo);
		int ancho = medidas[0];
		int alto = medidas[1];
		if (Math.min(ancho, alto) < LADO_MINIMO) {
			throw invalida("La imagen debe medir al menos 800 px por lado; esta mide %d × %d px.".formatted(ancho, alto));
		}
		if (ancho == alto) {
			return new Imagen(tipo, datos);
		}
		if (formato == TipoArchivo.WEBP) {
			throw invalida(("Una imagen WebP debe ser cuadrada; esta mide %d × %d px. Usa JPG o PNG y el sistema "
					+ "la recortará al centro.").formatted(ancho, alto));
		}
		if ((long) ancho * alto > PIXELES_MAXIMOS) {
			throw invalida("La imagen es demasiado grande para recortarla (%d × %d px). Redúcela e inténtalo de nuevo."
				.formatted(ancho, alto));
		}
		try {
			byte[] recortada = RecorteCuadrado.recortar(datos, formato);
			if (recortada == null) {
				throw ilegible();
			}
			return new Imagen(tipo, recortada);
		}
		catch (IOException | RuntimeException ex) {
			// Un EXIF o una imagen dañada no deben producir un error 500: se trata como imagen ilegible.
			throw ilegible();
		}
	}

	private static int[] medidasImageIo(byte[] datos, String tipo) {
		try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(datos))) {
			ImageReader lector = ImageIO.getImageReadersByMIMEType(tipo).next();
			try {
				lector.setInput(entrada, true, true);
				return new int[] { lector.getWidth(0), lector.getHeight(0) };
			}
			finally {
				lector.dispose();
			}
		}
		catch (IOException | RuntimeException ex) {
			throw ilegible();
		}
	}

	/**
	 * Lee las medidas de los tres tipos de cabecera WebP: con pérdida (VP8), sin pérdida (VP8L) y extendida (VP8X).
	 */
	private static int[] medidasWebp(byte[] d) {
		if (d.length < 30) {
			throw ilegible();
		}
		String fragmento = new String(Arrays.copyOfRange(d, 12, 16), StandardCharsets.US_ASCII);
		return switch (fragmento) {
			case "VP8 " -> {
				if (!empiezaCon(d, 23, 0x9D, 0x01, 0x2A)) {
					throw ilegible();
				}
				yield new int[] { (u16(d, 26) & 0x3FFF), (u16(d, 28) & 0x3FFF) };
			}
			case "VP8L" -> {
				if ((d[20] & 0xFF) != 0x2F) {
					throw ilegible();
				}
				int bits = (d[21] & 0xFF) | (d[22] & 0xFF) << 8 | (d[23] & 0xFF) << 16 | (d[24] & 0xFF) << 24;
				yield new int[] { (bits & 0x3FFF) + 1, ((bits >>> 14) & 0x3FFF) + 1 };
			}
			case "VP8X" -> new int[] { u24(d, 24) + 1, u24(d, 27) + 1 };
			default -> throw ilegible();
		};
	}

	private static int u16(byte[] d, int i) {
		return (d[i] & 0xFF) | (d[i + 1] & 0xFF) << 8;
	}

	private static int u24(byte[] d, int i) {
		return u16(d, i) | (d[i + 2] & 0xFF) << 16;
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

	private static ReglaNegocioException ilegible() {
		return invalida("No se pudo leer la imagen. Prueba con otro archivo JPG, PNG o WebP.");
	}

	private static ReglaNegocioException invalida(String mensaje) {
		return new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "IMAGEN_INVALIDA", mensaje);
	}

}
