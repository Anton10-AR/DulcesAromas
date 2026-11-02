package bo.dulcesaromas.ecommerce.pedidos;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface PedidoRepositorio extends JpaRepository<Pedido, Long> {

	/**
	 * Bloquea el pedido hasta el fin de la transacción, para que dos cambios de estado no se pisen.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Pedido p where p.id = :id")
	Optional<Pedido> bloquear(Long id);

}
