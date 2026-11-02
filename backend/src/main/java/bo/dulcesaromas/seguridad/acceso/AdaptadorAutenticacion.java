package bo.dulcesaromas.seguridad.acceso;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import bo.dulcesaromas.seguridad.IAutenticacion;
import bo.dulcesaromas.seguridad.RolBase;
import bo.dulcesaromas.seguridad.TipoUsuario;
import bo.dulcesaromas.seguridad.UsuarioActual;
import bo.dulcesaromas.seguridad.cuentas.Usuario;
import bo.dulcesaromas.seguridad.cuentas.UsuarioRepositorio;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class AdaptadorAutenticacion implements IAutenticacion {

	private final UsuarioRepositorio usuarios;

	AdaptadorAutenticacion(UsuarioRepositorio usuarios) {
		this.usuarios = usuarios;
	}

	@Override
	public UsuarioActual usuarioActual() {
		var autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioActual usuario) {
			return usuario;
		}
		throw new IllegalStateException("La solicitud no tiene sesión");
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Long, String> nombres(Collection<Long> usuarioIds) {
		return usuarios.findAllById(usuarioIds).stream().collect(Collectors.toMap(Usuario::getId, Usuario::getNombre));
	}

	@Override
	@Transactional(readOnly = true)
	public List<UsuarioResumen> personalActivo(RolBase rol) {
		return usuarios.personalActivo(rol).stream().map(p -> new UsuarioResumen(p.getId(), p.getNombre())).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<UsuarioResumen> clienteActivo(Long clienteId) {
		return usuarios.findById(clienteId)
			.filter(u -> u.getTipo() == TipoUsuario.CLIENTE && u.isActivo())
			.map(u -> new UsuarioResumen(u.getId(), u.getNombre()));
	}

}
