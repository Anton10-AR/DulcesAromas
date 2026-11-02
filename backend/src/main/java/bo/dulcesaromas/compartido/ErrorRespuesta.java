package bo.dulcesaromas.compartido;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Cuerpo de toda respuesta de error. {@code campos} nombra cada campo inválido y cómo corregirlo (RNF01).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorRespuesta(String codigo, String mensaje, Map<String, String> campos) {

	public ErrorRespuesta(String codigo, String mensaje) {
		this(codigo, mensaje, Map.of());
	}

}
