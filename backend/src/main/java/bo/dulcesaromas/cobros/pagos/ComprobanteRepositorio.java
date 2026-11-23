package bo.dulcesaromas.cobros.pagos;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ComprobanteRepositorio extends JpaRepository<Comprobante, Long> {

	/** Cuáles de esos pagos tienen comprobante, sin leer los archivos: los pagos con tarjeta no lo tienen. */
	@Query("select c.pagoId from Comprobante c where c.pagoId in :pagos")
	List<Long> conComprobante(Collection<Long> pagos);

}
