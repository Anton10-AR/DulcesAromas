package bo.dulcesaromas.seguridad.cuentas;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface RecuperacionContrasenaRepositorio extends JpaRepository<RecuperacionContrasena, Long> {

	/**
	 * Bloquea el enlace, para que dos usos simultáneos no lo aprovechen dos veces.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from RecuperacionContrasena r where r.tokenHash = :tokenHash")
	Optional<RecuperacionContrasena> bloquear(String tokenHash);

	@Modifying
	@Query("update RecuperacionContrasena r set r.usada = true where r.usuarioId = :usuarioId and r.usada = false")
	void anularDe(Long usuarioId);

}
