package bo.dulcesaromas;

import java.util.ArrayList;
import java.util.List;

import bo.dulcesaromas.seguridad.cuentas.EnviadorCorreo;

/**
 * Guarda los correos en vez de enviarlos, para que la prueba los lea.
 */
public class CorreosDePrueba implements EnviadorCorreo {

	private final List<Correo> enviados = new ArrayList<>();

	@Override
	public synchronized void enviar(String para, String asunto, String texto) {
		enviados.add(new Correo(para, asunto, texto));
	}

	public synchronized List<Correo> enviados() {
		return List.copyOf(enviados);
	}

	synchronized void vaciar() {
		enviados.clear();
	}

	public record Correo(String para, String asunto, String texto) {
	}

}
