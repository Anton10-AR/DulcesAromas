package bo.dulcesaromas.compartido;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Una operación que viola una regla de negocio. El código lo usa la interfaz para reaccionar;
 * el mensaje se muestra tal cual al usuario. {@code campos} nombra los campos que hay que corregir (RNF01).
 */
public class ReglaNegocioException extends RuntimeException {

	private final HttpStatus estado;

	private final String codigo;

	private final Map<String, String> campos;

	public ReglaNegocioException(HttpStatus estado, String codigo, String mensaje) {
		this(estado, codigo, mensaje, Map.of());
	}

	public ReglaNegocioException(HttpStatus estado, String codigo, String mensaje, Map<String, String> campos) {
		super(mensaje);
		this.estado = estado;
		this.codigo = codigo;
		this.campos = Map.copyOf(campos);
	}

	public HttpStatus getEstado() {
		return estado;
	}

	public String getCodigo() {
		return codigo;
	}

	public Map<String, String> getCampos() {
		return campos;
	}

}
