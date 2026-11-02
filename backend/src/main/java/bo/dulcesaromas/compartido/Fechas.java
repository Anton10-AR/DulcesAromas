package bo.dulcesaromas.compartido;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Las fechas del negocio son de La Paz (RNF19): un día va de su medianoche a la siguiente en esa zona.
 */
public final class Fechas {

	public static final ZoneId ZONA = ZoneId.of("America/La_Paz");

	private Fechas() {
	}

	public static Instant inicioDe(LocalDate dia) {
		return dia.atStartOfDay(ZONA).toInstant();
	}

	/**
	 * El primer instante del día siguiente, para usarlo como límite exclusivo.
	 */
	public static Instant finDe(LocalDate dia) {
		return inicioDe(dia.plusDays(1));
	}

}
