package bo.dulcesaromas.ecommerce.catalogo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RNF10 con imágenes WebP, que Java no sabe leer: se arman a mano las cabeceras de los tres tipos.
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
		assertThat(ValidadorImagen.validar(vp8(800, 800))).isEqualTo("image/webp");
		assertThat(ValidadorImagen.validar(vp8l(1024, 1024))).isEqualTo("image/webp");
		assertThat(ValidadorImagen.validar(vp8x(2000, 2000))).isEqualTo("image/webp");
	}

	@Test
	void rechazaWebpNoCuadradasOPequenas() {
		for (byte[] mala : new byte[][] { vp8(800, 600), vp8l(799, 799), vp8x(1000, 1001) }) {
			assertThatThrownBy(() -> ValidadorImagen.validar(mala)).isInstanceOf(ReglaNegocioException.class);
		}
	}

	@Test
	void elMensajeDiceLasMedidasDeLaImagen() {
		assertThatThrownBy(() -> ValidadorImagen.validar(vp8l(1200, 800))).hasMessageContaining("1200 × 800 px");
	}

}
