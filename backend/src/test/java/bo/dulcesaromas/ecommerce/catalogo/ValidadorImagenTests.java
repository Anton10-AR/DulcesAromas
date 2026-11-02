package bo.dulcesaromas.ecommerce.catalogo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RNF10: las WebP, que Java no sabe leer, deben venir cuadradas (se arman a mano las cabeceras); las JPG y PNG
 * que no son cuadradas se recortan al centro, respetando la orientación EXIF de las fotos de celular.
 */
class ValidadorImagenTests {

	private static ByteBuffer webp(String fragmento) {
		ByteBuffer b = ByteBuffer.allocate(40).order(ByteOrder.LITTLE_ENDIAN);
		b.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(32).put("WEBP".getBytes(StandardCharsets.US_ASCII));
		b.put(fragmento.getBytes(StandardCharsets.US_ASCII)).putInt(20);
		return b;
	}

	private static byte[] vp8(int ancho, int alto) {
		ByteBuffer b = webp("VP8 ");
		b.position(23);
		b.put((byte) 0x9D).put((byte) 0x01).put((byte) 0x2A).putShort((short) ancho).putShort((short) alto);
		return b.array();
	}

	private static byte[] vp8l(int ancho, int alto) {
		ByteBuffer b = webp("VP8L");
		b.put((byte) 0x2F).putInt((ancho - 1) | (alto - 1) << 14);
		return b.array();
	}

	private static byte[] vp8x(int ancho, int alto) {
		ByteBuffer b = webp("VP8X");
		b.position(24);
		b.put((byte) (ancho - 1)).put((byte) ((ancho - 1) >> 8)).put((byte) ((ancho - 1) >> 16));
		b.put((byte) (alto - 1)).put((byte) ((alto - 1) >> 8)).put((byte) ((alto - 1) >> 16));
		return b.array();
	}

	@Test
	void aceptaWebpCuadradasDe800PxOMas() {
		assertThat(ValidadorImagen.preparar(vp8(800, 800)).tipo()).isEqualTo("image/webp");
		assertThat(ValidadorImagen.preparar(vp8l(1024, 1024)).tipo()).isEqualTo("image/webp");
		assertThat(ValidadorImagen.preparar(vp8x(2000, 2000)).tipo()).isEqualTo("image/webp");
	}

	@Test
	void rechazaWebpNoCuadradasOPequenas() {
		for (byte[] mala : new byte[][] { vp8(800, 600), vp8l(799, 799), vp8x(1000, 1001) }) {
			assertThatThrownBy(() -> ValidadorImagen.preparar(mala)).isInstanceOf(ReglaNegocioException.class);
		}
	}

	@Test
	void elMensajeDiceLasMedidasDeLaImagen() {
		assertThatThrownBy(() -> ValidadorImagen.preparar(vp8l(1200, 800))).hasMessageContaining("1200 × 800 px")
			.hasMessageContaining("Usa JPG o PNG");
	}

	private static BufferedImage leer(byte[] datos) throws IOException {
		return ImageIO.read(new ByteArrayInputStream(datos));
	}

	private static byte[] escribir(BufferedImage img, String formato) throws IOException {
		var salida = new ByteArrayOutputStream();
		ImageIO.write(img, formato, salida);
		return salida.toByteArray();
	}

	/**
	 * Imagen blanca con una franja de color a la izquierda, del ancho indicado.
	 */
	private static BufferedImage conFranja(int ancho, int alto, int franja, Color color) {
		var img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, ancho, alto);
		g.setColor(color);
		g.fillRect(0, 0, franja, alto);
		g.dispose();
		return img;
	}

	@Test
	void unaPngNoCuadradaSeRecortaAlCentro() throws IOException {
		// 1200 × 800 con una franja azul en los primeros 200 px: el recorte al centro empieza en x = 200.
		byte[] png = escribir(conFranja(1200, 800, 200, Color.BLUE), "png");

		ValidadorImagen.Imagen lista = ValidadorImagen.preparar(png);

		BufferedImage resultado = leer(lista.datos());
		assertThat(lista.tipo()).isEqualTo("image/png");
		assertThat(resultado.getWidth()).isEqualTo(800);
		assertThat(resultado.getHeight()).isEqualTo(800);
		assertThat(new Color(resultado.getRGB(0, 400))).isEqualTo(Color.WHITE);
	}

	@Test
	void unaImagenCuadradaSeGuardaTalCual() throws IOException {
		byte[] png = escribir(conFranja(900, 900, 100, Color.BLUE), "png");

		assertThat(ValidadorImagen.preparar(png).datos()).isSameAs(png);
	}

	@Test
	void elLadoMenorDebeMedirAlMenos800() throws IOException {
		byte[] png = escribir(conFranja(1200, 700, 10, Color.BLUE), "png");

		assertThatThrownBy(() -> ValidadorImagen.preparar(png)).hasMessageContaining("1200 × 700 px");
	}

	@Test
	void unaFotoDeCelularSeGiraSegunSuExifAntesDeRecortar() throws IOException {
		// Guardada acostada (1000 × 800) con la franja roja a la izquierda y EXIF 6: se ve parada (800 × 1000)
		// con la franja arriba. El recorte al centro quita 100 px arriba y abajo, así que quedan 100 px rojos.
		byte[] jpg = conExifOrientacion(escribir(conFranja(1000, 800, 200, Color.RED), "jpg"), 6);

		BufferedImage resultado = leer(ValidadorImagen.preparar(jpg).datos());

		assertThat(resultado.getWidth()).isEqualTo(800);
		assertThat(resultado.getHeight()).isEqualTo(800);
		Color arriba = new Color(resultado.getRGB(400, 40));
		Color centro = new Color(resultado.getRGB(400, 400));
		assertThat(arriba.getRed()).isGreaterThan(200);
		assertThat(arriba.getGreen()).isLessThan(80);
		assertThat(centro.getGreen()).isGreaterThan(200);
	}

	@Test
	void leeLaOrientacionExifEnAmbosOrdenesDeBytes() throws IOException {
		byte[] jpg = escribir(conFranja(10, 10, 1, Color.RED), "jpg");

		assertThat(RecorteCuadrado.orientacionExif(conExifOrientacion(jpg, 6))).isEqualTo(6);
		assertThat(RecorteCuadrado.orientacionExif(conExifOrientacionMotorola(jpg, 8))).isEqualTo(8);
		assertThat(RecorteCuadrado.orientacionExif(jpg)).isEqualTo(1);
	}

	/**
	 * Inserta un bloque EXIF (APP1, orden Intel) con la etiqueta de orientación, justo después del inicio del JPG.
	 */
	private static byte[] conExifOrientacion(byte[] jpg, int orientacion) {
		ByteBuffer tiff = ByteBuffer.allocate(26).order(ByteOrder.LITTLE_ENDIAN);
		tiff.put((byte) 'I').put((byte) 'I').putShort((short) 42).putInt(8);
		tiff.putShort((short) 1).putShort((short) 0x0112).putShort((short) 3).putInt(1).putShort((short) orientacion)
			.putShort((short) 0);
		return insertarApp1(jpg, tiff.array());
	}

	private static byte[] conExifOrientacionMotorola(byte[] jpg, int orientacion) {
		ByteBuffer tiff = ByteBuffer.allocate(26).order(ByteOrder.BIG_ENDIAN);
		tiff.put((byte) 'M').put((byte) 'M').putShort((short) 42).putInt(8);
		tiff.putShort((short) 1).putShort((short) 0x0112).putShort((short) 3).putInt(1).putShort((short) orientacion)
			.putShort((short) 0);
		return insertarApp1(jpg, tiff.array());
	}

	private static byte[] insertarApp1(byte[] jpg, byte[] tiff) {
		byte[] exif = "Exif\0\0".getBytes(StandardCharsets.ISO_8859_1);
		int largo = 2 + exif.length + tiff.length;
		ByteBuffer salida = ByteBuffer.allocate(jpg.length + 2 + largo);
		salida.put(jpg, 0, 2).put((byte) 0xFF).put((byte) 0xE1).putShort((short) largo).put(exif).put(tiff);
		salida.put(jpg, 2, jpg.length - 2);
		return salida.array();
	}

}
