package bo.dulcesaromas.seguridad.auditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuditoriaRepositorio extends JpaRepository<Auditoria, Long> {

	@EntityGraph(attributePaths = "usuario")
	Page<Auditoria> findAllByOrderByFechaDescIdDesc(Pageable pagina);

}
