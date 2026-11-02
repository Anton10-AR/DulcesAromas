package bo.dulcesaromas.ecommerce.inventario;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface ItemInventarioRepositorio extends JpaRepository<ItemInventario, Long> {

	List<ItemInventario> findAllByOrderByNombreAsc();

	boolean existsByNombreClave(String nombreClave);

	boolean existsByNombreClaveAndIdNot(String nombreClave, Long id);

	/**
	 * Bloquea el insumo hasta el fin de la transacción: dos salidas a la vez no dejan el stock negativo (RNF14).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from ItemInventario i where i.id = :id")
	Optional<ItemInventario> bloquear(Long id);

}
