package bo.dulcesaromas.seguridad.cuentas;

import java.util.List;

import bo.dulcesaromas.seguridad.RolBase;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface PersonalRepositorio extends JpaRepository<Personal, Long> {

	List<Personal> findAllByOrderByNombreAsc();

	boolean existsByActivoTrueAndRolCodigo(RolBase rol);

	/**
	 * Bloquea las filas de los administradores activos hasta el fin de la transacción, para que dos
	 * administradores no se desactiven entre sí a la vez y dejen el sistema sin ninguno (RN03).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Personal p where p.activo = true and p.rol.codigo = bo.dulcesaromas.seguridad.RolBase.ADMINISTRADOR")
	List<Personal> bloquearAdministradoresActivos();

}
