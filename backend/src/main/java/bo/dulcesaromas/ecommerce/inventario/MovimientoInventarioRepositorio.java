package bo.dulcesaromas.ecommerce.inventario;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface MovimientoInventarioRepositorio extends JpaRepository<MovimientoInventario, Long> {

	List<MovimientoInventario> findByItemIdOrderByFechaDescIdDesc(Long itemId);

}
