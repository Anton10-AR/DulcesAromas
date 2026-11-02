package bo.dulcesaromas.ecommerce.catalogo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

interface ProductoRepositorio extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

	long countByCategoriaIdAndActivoTrue(Long categoriaId);

	/**
	 * Pares (id de categoría, cantidad de productos activos).
	 */
	@Query("select p.categoria.id, count(p) from Producto p where p.activo = true group by p.categoria.id")
	List<Object[]> contarActivosPorCategoria();

}
