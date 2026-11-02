package bo.dulcesaromas.seguridad;

/**
 * Lo que el módulo Seguridad ofrece a los demás módulos.
 */
public interface IAutenticacion {

	/**
	 * El usuario de la solicitud en curso. Falla si la solicitud no tiene sesión.
	 */
	UsuarioActual usuarioActual();

}
