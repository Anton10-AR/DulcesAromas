package bo.dulcesaromas.ecommerce.catalogo;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import bo.dulcesaromas.compartido.TipoArchivo;

/**
 * Recorta una imagen JPG o PNG a un cuadrado, al centro. Antes aplica la orientación EXIF que traen las fotos
 * de celular: al volver a codificar la imagen esa etiqueta se pierde, y sin girarla quedaría acostada.
 */
final class RecorteCuadrado {

	/** Etiqueta EXIF de orientación. */
	private static final int ORIENTACION = 0x0112;

	private RecorteCuadrado() {
	}

	/**
	 * @return la imagen recortada, codificada en el mismo formato, o nulo si no se pudo leer
	 */
	static byte[] recortar(byte[] datos, TipoArchivo formato) throws IOException {
		BufferedImage original = ImageIO.read(new ByteArrayInputStream(datos));
		if (original == null) {
			return null;
		}
		BufferedImage derecha = formato == TipoArchivo.JPEG ? orientar(original, orientacionExif(datos)) : original;
		int lado = Math.min(derecha.getWidth(), derecha.getHeight());
		BufferedImage cuadrada = derecha.getSubimage((derecha.getWidth() - lado) / 2, (derecha.getHeight() - lado) / 2,
				lado, lado);
		// JPG no admite transparencia: se dibuja sobre un lienzo RGB.
		BufferedImage lienzo = new BufferedImage(lado, lado,
				formato == TipoArchivo.JPEG ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = lienzo.createGraphics();
		g.drawImage(cuadrada, 0, 0, null);
		g.dispose();
		var salida = new ByteArrayOutputStream();
		ImageIO.write(lienzo, formato == TipoArchivo.JPEG ? "jpg" : "png", salida);
		return salida.toByteArray();
	}

	/**
	 * Gira o refleja la imagen según la orientación EXIF (1 a 8), para que quede como se ve en el celular.
	 */
	static BufferedImage orientar(BufferedImage img, int orientacion) {
		int w = img.getWidth();
		int h = img.getHeight();
		AffineTransform t = switch (orientacion) {
			case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0);
			case 3 -> new AffineTransform(-1, 0, 0, -1, w, h);
			case 4 -> new AffineTransform(1, 0, 0, -1, 0, h);
			case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
			case 6 -> new AffineTransform(0, 1, -1, 0, h, 0);
			case 7 -> new AffineTransform(0, -1, -1, 0, h, w);
			case 8 -> new AffineTransform(0, -1, 1, 0, 0, w);
			default -> null;
		};
		if (t == null) {
			return img;
		}
		boolean gira = orientacion >= 5;
		BufferedImage resultado = new BufferedImage(gira ? h : w, gira ? w : h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = resultado.createGraphics();
		g.drawImage(img, t, null);
		g.dispose();
		return resultado;
	}

	/**
	 * Lee la orientación del bloque EXIF (APP1) de un JPG. Devuelve 1, sin giro, si no la trae o no se entiende.
	 */
	static int orientacionExif(byte[] d) {
		int i = 2;
		while (i + 4 <= d.length && (d[i] & 0xFF) == 0xFF) {
			int marcador = d[i + 1] & 0xFF;
			int largo = ((d[i + 2] & 0xFF) << 8) | (d[i + 3] & 0xFF);
			if (marcador == 0xDA || largo < 2) {
				return 1;
			}
			int inicio = i + 4;
			if (marcador == 0xE1 && inicio + 14 <= d.length && d[inicio] == 'E' && d[inicio + 1] == 'x'
					&& d[inicio + 2] == 'i' && d[inicio + 3] == 'f') {
				return orientacionTiff(d, inicio + 6, Math.min(d.length, i + 2 + largo));
			}
			i += 2 + largo;
		}
		return 1;
	}

	private static int orientacionTiff(byte[] d, int tiff, int fin) {
		boolean intel = d[tiff] == 'I';
		int ifd = tiff + leer(d, tiff + 4, 4, intel);
		if (ifd + 2 > fin || ifd < tiff) {
			return 1;
		}
		int entradas = leer(d, ifd, 2, intel);
		for (int e = 0; e < entradas; e++) {
			int pos = ifd + 2 + e * 12;
			if (pos + 12 > fin) {
				return 1;
			}
			if (leer(d, pos, 2, intel) == ORIENTACION) {
				int valor = leer(d, pos + 8, 2, intel);
				return valor >= 1 && valor <= 8 ? valor : 1;
			}
		}
		return 1;
	}

	private static int leer(byte[] d, int pos, int bytes, boolean intel) {
		int valor = 0;
		for (int k = 0; k < bytes; k++) {
			int b = d[pos + (intel ? bytes - 1 - k : k)] & 0xFF;
			valor = (valor << 8) | b;
		}
		return valor;
	}

}
