package bo.dulcesaromas.cobros.pagos;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface VerificacionRepositorio extends JpaRepository<Verificacion, Long> {

	List<Verificacion> findByPagoIdIn(Collection<Long> pagoIds);

}
