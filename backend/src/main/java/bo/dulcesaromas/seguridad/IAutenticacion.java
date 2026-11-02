package bo.dulcesaromas.seguridad;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

	/**
	 * Cuentas de personal activas con ese rol, por nombre. Por ejemplo, los repartidores a quienes asignar entregas.
	 */
	List<UsuarioResumen> personalActivo(RolBase rol);

	/**
	 * El cliente con ese id, si existe y está activo. Por ejemplo, el titular de un pedido asistido.
	 */
	Optional<UsuarioResumen> clienteActivo(Long clienteId);

	record UsuarioResumen(Long id, String nombre) {
	}

}
