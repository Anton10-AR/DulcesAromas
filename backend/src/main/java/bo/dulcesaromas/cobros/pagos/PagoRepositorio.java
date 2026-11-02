package bo.dulcesaromas.cobros.pagos;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface PagoRepositorio extends JpaRepository<Pago, Long> {

	List<Pago> findByPedidoIdOrderByIdAsc(Long pedidoId);

	List<Pago> findByEstadoOrderByRegistradoAsc(EstadoPago estado);

	/**
	 * Bloquea el pago hasta el fin de la transacción: dos personas no deciden el mismo pago a la vez (RNF15).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Pago p where p.id = :id")
	Optional<Pago> bloquear(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Pago p where p.pedidoId = :pedidoId and p.estado = :estado")
	Optional<Pago> bloquearDePedidoEn(Long pedidoId, EstadoPago estado);

}
