package bo.dulcesaromas.seguridad;

import java.util.Collection;
import java.util.Map;

/**
 * Lo que el módulo Seguridad ofrece a los demás módulos.
 */
public interface IAutenticacion {

	/**
	 * El usuario de la solicitud en curso. Falla si la solicitud no tiene sesión.
	 */
	UsuarioActual usuarioActual();

	/**
	 * Nombres de los usuarios indicados, por id, para mostrar quién hizo algo. Los ids inexistentes no aparecen.
	 */
	Map<Long, String> nombres(Collection<Long> usuarioIds);

}
