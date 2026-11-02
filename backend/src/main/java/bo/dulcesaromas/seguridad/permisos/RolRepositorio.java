package bo.dulcesaromas.seguridad.permisos;

import bo.dulcesaromas.seguridad.RolBase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepositorio extends JpaRepository<Rol, RolBase> {

}
