package bo.dulcesaromas.cobros.pagos;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface CobroQrRepositorio extends JpaRepository<CobroQr, String> {

	/**
	 * Bloquea el cobro hasta el fin de la transacción: dos confirmaciones del mismo QR no se aplican dos veces.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from CobroQr c where c.codigo = :codigo")
	Optional<CobroQr> bloquear(String codigo);

}
