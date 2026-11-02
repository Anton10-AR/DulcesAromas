package bo.dulcesaromas.seguridad.cuentas;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidadorContrasena implements ConstraintValidator<ContrasenaValida, String> {

	private static final int LARGO_MINIMO = 8;

	/** BCrypt solo usa los primeros 72 bytes; una contraseña más larga se rechaza en vez de truncarla. */
	private static final int BYTES_MAXIMOS = 72;

	/**
	 * @return qué le falta a la contraseña, o vacío si es válida
	 */
	public static Optional<String> problema(String contrasena) {
		if (contrasena == null || contrasena.length() < LARGO_MINIMO
				|| contrasena.chars().noneMatch(Character::isLetter) || contrasena.chars().noneMatch(Character::isDigit)) {
			return Optional.of("Usa al menos 8 caracteres, con al menos una letra y un número.");
		}
		if (contrasena.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS) {
			return Optional.of("Es demasiado larga. Usa como máximo 72 caracteres sin tildes.");
		}
		return Optional.empty();
	}

	@Override
	public boolean isValid(String contrasena, ConstraintValidatorContext contexto) {
		Optional<String> problema = problema(contrasena);
		problema.ifPresent(mensaje -> {
			contexto.disableDefaultConstraintViolation();
			contexto.buildConstraintViolationWithTemplate(mensaje).addConstraintViolation();
		});
		return problema.isEmpty();
	}

}
