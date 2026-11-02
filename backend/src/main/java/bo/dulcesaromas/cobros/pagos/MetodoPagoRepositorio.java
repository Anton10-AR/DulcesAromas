package bo.dulcesaromas.cobros.pagos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface MetodoPagoRepositorio extends JpaRepository<MetodoPago, Long> {

	List<MetodoPago> findAllByOrderByIdAsc();

	List<MetodoPago> findByActivoTrueOrderByIdAsc();

	boolean existsByNombreClave(String nombreClave);

	boolean existsByNombreClaveAndIdNot(String nombreClave, Long id);

}
