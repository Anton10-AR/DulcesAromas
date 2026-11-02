package bo.dulcesaromas.seguridad.cuentas;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByCorreo(String correo);

	boolean existsByCorreo(String correo);

	boolean existsByCorreoAndIdNot(String correo, Long id);

}
