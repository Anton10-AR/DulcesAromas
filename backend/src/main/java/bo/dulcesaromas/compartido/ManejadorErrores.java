package bo.dulcesaromas.compartido;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Convierte las excepciones conocidas en {@link ErrorRespuesta}. No captura excepciones genéricas,
 * para que los accesos denegados lleguen a Spring Security y queden registrados.
 */
@RestControllerAdvice
class ManejadorErrores {

	@ExceptionHandler
	ResponseEntity<ErrorRespuesta> reglaNegocio(ReglaNegocioException ex) {
		return ResponseEntity.status(ex.getEstado())
			.body(new ErrorRespuesta(ex.getCodigo(), ex.getMessage(), ex.getCampos()));
	}

	@ExceptionHandler
	ResponseEntity<ErrorRespuesta> datosInvalidos(MethodArgumentNotValidException ex) {
		Map<String, String> campos = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
		return ResponseEntity.badRequest()
			.body(new ErrorRespuesta("DATOS_INVALIDOS", "Revisa los campos marcados.", campos));
	}

	@ExceptionHandler
	ResponseEntity<ErrorRespuesta> cuerpoIlegible(HttpMessageNotReadableException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(new ErrorRespuesta("SOLICITUD_INVALIDA", "La solicitud no tiene el formato esperado."));
	}

	@ExceptionHandler
	ResponseEntity<ErrorRespuesta> archivoFaltante(MissingServletRequestPartException ex) {
		return ResponseEntity.badRequest()
			.body(new ErrorRespuesta("ARCHIVO_FALTANTE", "Adjunta el archivo.", Map.of(ex.getRequestPartName(), "Adjunta el archivo.")));
	}

	@ExceptionHandler
	ResponseEntity<ErrorRespuesta> archivoDemasiadoGrande(MaxUploadSizeExceededException ex) {
		return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
			.body(new ErrorRespuesta("ARCHIVO_DEMASIADO_GRANDE", "El archivo es demasiado grande."));
	}

}
