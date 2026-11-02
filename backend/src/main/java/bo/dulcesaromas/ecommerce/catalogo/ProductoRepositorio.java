package bo.dulcesaromas.ecommerce.catalogo;

import java.util.Collection;
import java.util.List;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ProductoRepositorio extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

	long countByCategoriaIdAndActivoTrue(Long categoriaId);

	/**
	 * Pares (id de categoría, cantidad de productos activos).
	 */
	@Query("select p.categoria.id, count(p) from Producto p where p.activo = true group by p.categoria.id")
	List<Object[]> contarActivosPorCategoria();

	/**
	 * Bloquea los productos hasta el fin de la transacción, para que nadie cambie su disponibilidad mientras se
	 * confirma un pedido que los incluye (RN08, RNF14).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Producto p where p.id in :ids order by p.id")
	List<Producto> bloquearParaPedido(Collection<Long> ids);

}
