package bo.dulcesaromas.distribucion.entregas;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface EntregaRepositorio extends JpaRepository<Entrega, Long> {

	List<Entrega> findByFechaEntregaIsNullOrderByIdAsc();

	List<Entrega> findByRepartidorIdAndFechaEntregaIsNullOrderByIdAsc(Long repartidorId);

	Optional<Entrega> findByPedidoId(Long pedidoId);

	/**
	 * Bloquea la entrega hasta el fin de la transacción, para que nadie cambie el repartidor mientras sale.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from Entrega e where e.pedidoId = :pedidoId")
	Optional<Entrega> bloquearDePedido(Long pedidoId);

}
