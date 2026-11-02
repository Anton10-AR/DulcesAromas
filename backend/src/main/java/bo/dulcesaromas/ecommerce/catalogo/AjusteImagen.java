package bo.dulcesaromas.ecommerce.catalogo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import bo.dulcesaromas.compartido.TipoArchivo;

/**
 * Deja una imagen JPG o PNG lista para el catálogo (RNF10): cuadrada, recortada al centro, y de 1200 px por lado
 * como máximo. Aplica la orientación EXIF que traen las fotos de celular: al volver a codificar la imagen esa
 * etiqueta se pierde, y sin girarla quedaría acostada.
 *
 * <p>
 * Para que una foto de celular de 10 MB no ocupe cientos de MB de memoria, solo se decodifica el cuadrado central
 * y, si es muy grande, con submuestreo: se lee como mucho a 2400 px por lado y luego se reduce con suavizado. El
 * cuadrado central es el mismo antes y después de girar, así que se puede recortar antes de aplicar el EXIF.
 */
final class AjusteImagen {

	/** Lado máximo de la imagen guardada. */
	static final int LADO_MAXIMO = 1200;

	/** Una PNG (por ejemplo, una foto guardada como PNG) que pese más que esto se guarda como JPG. */
	static final int BYTES_PNG_MAXIMOS = 2 * 1024 * 1024;

	private static final int LADO_DE_LECTURA = 2 * LADO_MAXIMO;

	private static final float CALIDAD_JPG = 0.85f;

	/** Etiqueta EXIF de orientación. */
	private static final int ORIENTACION = 0x0112;

	private AjusteImagen() {
	}

	/**
	 * @param ancho ancho de la imagen según su cabecera
	 * @param alto alto de la imagen según su cabecera
	 * @return la imagen cuadrada y reducida, o nulo si no se pudo leer
	 */
	static ValidadorImagen.Imagen ajustar(byte[] datos, TipoArchivo formato, int ancho, int alto) throws IOException {
		BufferedImage cuadrada = leerCuadradoCentral(datos, ancho, alto);
		if (cuadrada == null) {
			return null;
		}
		if (formato == TipoArchivo.JPEG) {
			cuadrada = orientar(cuadrada, orientacionExif(datos));
		}
		boolean transparente = formato == TipoArchivo.PNG && cuadrada.getColorModel().hasAlpha();
		BufferedImage lista = reducir(cuadrada, Math.min(cuadrada.getWidth(), LADO_MAXIMO), transparente);
		if (formato == TipoArchivo.PNG) {
			byte[] png = codificarPng(lista);
			if (png.length <= BYTES_PNG_MAXIMOS) {
				return new ValidadorImagen.Imagen(TipoArchivo.PNG.getTipoContenido(), png);
			}
		}
		return new ValidadorImagen.Imagen(TipoArchivo.JPEG.getTipoContenido(), codificarJpg(lista));
	}

	private static BufferedImage leerCuadradoCentral(byte[] datos, int ancho, int alto) throws IOException {
		try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(datos))) {
			var lectores = ImageIO.getImageReaders(entrada);
			if (!lectores.hasNext()) {
				return null;
			}
			ImageReader lector = lectores.next();
			try {
				lector.setInput(entrada, true, true);
				int lado = Math.min(ancho, alto);
				int paso = Math.max(1, (lado + LADO_DE_LECTURA - 1) / LADO_DE_LECTURA);
				ImageReadParam parametros = lector.getDefaultReadParam();
				parametros.setSourceRegion(new Rectangle((ancho - lado) / 2, (alto - lado) / 2, lado, lado));
				parametros.setSourceSubsampling(paso, paso, 0, 0);
				return lector.read(0, parametros);
			}
			finally {
				lector.dispose();
			}
		}
	}

	/**
	 * Reduce a {@code lado} px en pasos de a la mitad como mucho, para que el suavizado no pierda detalle. Siempre
	 * dibuja sobre un lienzo nuevo, así que la imagen queda en un tipo que ImageIO sabe escribir.
	 */
	private static BufferedImage reducir(BufferedImage img, int lado, boolean transparente) {
		BufferedImage actual = img;
		do {
			int siguiente = Math.max(lado, actual.getWidth() / 2);
			BufferedImage lienzo = new BufferedImage(siguiente, siguiente,
					transparente ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
			Graphics2D g = lienzo.createGraphics();
			if (!transparente) {
				// JPG no admite transparencia: lo transparente queda blanco.
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, siguiente, siguiente);
			}
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.drawImage(actual, 0, 0, siguiente, siguiente, null);
			g.dispose();
			actual = lienzo;
		}
		while (actual.getWidth() > lado);
		return actual;
	}

	private static byte[] codificarPng(BufferedImage img) throws IOException {
		var salida = new ByteArrayOutputStream();
		ImageIO.write(img, "png", salida);
		return salida.toByteArray();
	}

	private static byte[] codificarJpg(BufferedImage img) throws IOException {
		BufferedImage rgb = img;
		if (img.getColorModel().hasAlpha()) {
			rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = rgb.createGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, img.getWidth(), img.getHeight());
			g.drawImage(img, 0, 0, null);
			g.dispose();
		}
		ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpg").next();
		var salida = new ByteArrayOutputStream();
		try (ImageOutputStream destino = ImageIO.createImageOutputStream(salida)) {
			escritor.setOutput(destino);
			ImageWriteParam parametros = escritor.getDefaultWriteParam();
			parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			parametros.setCompressionQuality(CALIDAD_JPG);
			escritor.write(null, new IIOImage(rgb, null, null), parametros);
		}
		finally {
			escritor.dispose();
		}
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
