package bo.dulcesaromas.ecommerce.pedidos;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import bo.dulcesaromas.ecommerce.EstadoPedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface PedidoRepositorio extends JpaRepository<Pedido, Long> {

	List<Pedido> findByClienteIdOrderByFechaCreacionDescIdDesc(Long clienteId);

	List<Pedido> findByEstadoInOrderByHorarioEntregaAscIdAsc(Collection<EstadoPedido> estados);

	/**
	 * Bloquea el pedido hasta el fin de la transacción, para que dos cambios de estado no se pisen.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Pedido p where p.id = :id")
	Optional<Pedido> bloquear(Long id);

}
