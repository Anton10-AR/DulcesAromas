package bo.dulcesaromas.compartido;

import org.springframework.http.HttpStatus;

/**
 * Una operación que viola una regla de negocio. El código lo usa la interfaz para reaccionar;
 * el mensaje se muestra tal cual al usuario.
 */
public class ReglaNegocioException extends RuntimeException {

	private final HttpStatus estado;

	private final String codigo;

	public ReglaNegocioException(HttpStatus estado, String codigo, String mensaje) {
		super(mensaje);
		this.estado = estado;
		this.codigo = codigo;
	}

	public HttpStatus getEstado() {
		return estado;
	}

	public String getCodigo() {
		return codigo;
	}

}
