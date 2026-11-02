package bo.dulcesaromas.seguridad.cuentas;

import java.util.List;
import java.util.Optional;

import bo.dulcesaromas.seguridad.RolBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByCorreo(String correo);

	boolean existsByCorreo(String correo);

	boolean existsByCorreoAndIdNot(String correo, Long id);

	@Query("select p from Personal p where p.activo = true and p.rol.codigo = :rol order by p.nombre")
	List<Personal> personalActivo(RolBase rol);

}
