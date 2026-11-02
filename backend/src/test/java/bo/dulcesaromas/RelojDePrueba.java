package bo.dulcesaromas;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Reloj que solo avanza cuando la prueba lo pide.
 */
public class RelojDePrueba extends Clock {

	private static final Instant INICIO = Instant.parse("2026-11-02T14:00:00Z");

	private Instant ahora = INICIO;

	public void adelantar(Duration duracion) {
		ahora = ahora.plus(duracion);
	}

	public void reiniciar() {
		ahora = INICIO;
	}

	@Override
	public Instant instant() {
		return ahora;
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zona) {
		return this;
	}

}
