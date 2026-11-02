package bo.dulcesaromas.seguridad.cuentas;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

interface ClienteRepositorio extends JpaRepository<Cliente, Long> {

	/**
	 * Clientes cuyo correo o teléfono contiene el texto, para el pedido asistido (CU17).
	 */
	List<Cliente> findByCorreoContainingOrTelefonoContainingOrderByNombreAsc(String correo, String telefono,
			Limit limite);

}
