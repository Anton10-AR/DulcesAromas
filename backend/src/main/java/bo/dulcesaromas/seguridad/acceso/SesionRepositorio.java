package bo.dulcesaromas.seguridad.acceso;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface SesionRepositorio extends JpaRepository<Sesion, Long> {

	Optional<Sesion> findByTokenHash(String tokenHash);

	@Modifying
	@Query("delete from Sesion s where s.tokenHash = :tokenHash")
	void borrarPorToken(String tokenHash);

	@Modifying
	@Query("delete from Sesion s where s.usuario.id = :usuarioId")
	void borrarDeUsuario(Long usuarioId);

	@Modifying
	@Query("delete from Sesion s where s.usuario.id = :usuarioId and s.tokenHash <> :tokenHash")
	void borrarDeUsuarioSalvo(Long usuarioId, String tokenHash);

	@Modifying
	@Query("delete from Sesion s where s.ultimoUso < :limite")
	int borrarSinUsoDesde(Instant limite);

}
