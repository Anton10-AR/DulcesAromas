package bo.dulcesaromas.ecommerce.pedidos;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Optional;

import bo.dulcesaromas.compartido.Fechas;

/**
 * Cuándo se puede entregar o recoger un pedido: dentro del horario de atención (lunes a sábado de 9:00 a 20:00 y
 * domingo de 10:00 a 14:00, según la Visión) y con al menos 24 horas de anticipación, para que dé tiempo de
 * preparar. Lo decidió el equipo (casos de uso, sección 10.1).
 */
final class HorarioAtencion {

	static final Duration ANTICIPACION = Duration.ofHours(24);

	static final String DESCRIPCION = "lunes a sábado de 9:00 a 20:00 y domingo de 10:00 a 14:00";

	private HorarioAtencion() {
	}

	/**
	 * @return qué le falta al horario, o vacío si sirve
	 */
	static Optional<String> problema(Instant horario, Instant ahora) {
		if (!horario.isAfter(ahora)) {
			return Optional.of("Elige un horario que todavía no haya pasado.");
		}
		if (horario.isBefore(ahora.plus(ANTICIPACION))) {
			return Optional.of("Elige un horario con al menos 24 horas de anticipación, para que podamos prepararlo.");
		}
		ZonedDateTime enLaPaz = horario.atZone(Fechas.ZONA);
		LocalTime hora = enLaPaz.toLocalTime();
		boolean domingo = enLaPaz.getDayOfWeek() == DayOfWeek.SUNDAY;
		LocalTime apertura = domingo ? LocalTime.of(10, 0) : LocalTime.of(9, 0);
		LocalTime cierre = domingo ? LocalTime.of(14, 0) : LocalTime.of(20, 0);
		if (hora.isBefore(apertura) || hora.isAfter(cierre)) {
			return Optional.of("Elige un horario de atención: " + DESCRIPCION + ".");
		}
		return Optional.empty();
	}

}
