package bo.dulcesaromas.seguridad.permisos;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.seguridad.Permiso;
import bo.dulcesaromas.seguridad.RolBase;
import bo.dulcesaromas.seguridad.auditoria.AccionAuditada;
import bo.dulcesaromas.seguridad.auditoria.Auditor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RolServicio {

	private final RolRepositorio roles;

	private final Auditor auditor;

	RolServicio(RolRepositorio roles, Auditor auditor) {
		this.roles = roles;
		this.auditor = auditor;
	}

	@Transactional(readOnly = true)
	List<Rol> listar() {
		return roles.findAll().stream().sorted(Comparator.comparing(Rol::getCodigo)).toList();
	}

	/**
	 * Reemplaza los permisos del rol. Rige desde la siguiente solicitud de cada usuario con ese rol (HU05).
	 */
	@Transactional
	Rol editarPermisos(RolBase codigo, Set<Permiso> nuevos, Long actorId) {
		if (codigo == RolBase.ADMINISTRADOR && !nuevos.contains(Permiso.SEGURIDAD)) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "ADMINISTRADOR_SIN_SEGURIDAD",
					"El administrador siempre conserva el acceso a Seguridad.");
		}
		// Usuarios y permisos son solo del administrador (matriz de permisos; decisión del equipo, sección 10.1).
		if (codigo != RolBase.ADMINISTRADOR && nuevos.contains(Permiso.SEGURIDAD)) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "SEGURIDAD_SOLO_ADMINISTRADOR",
					"El acceso a Seguridad es solo del administrador.");
		}
		Rol rol = roles.findById(codigo).orElseThrow();
		Set<Permiso> anteriores = copia(rol.getPermisos());
		rol.reemplazarPermisos(nuevos);
		auditor.registrar(actorId, AccionAuditada.PERMISOS_EDITADOS, "Rol " + codigo.getNombre() + ". Agregados: "
				+ nombres(diferencia(nuevos, anteriores)) + ". Quitados: " + nombres(diferencia(anteriores, nuevos)) + ".");
		return rol;
	}

	private static Set<Permiso> copia(Collection<Permiso> permisos) {
		Set<Permiso> resultado = EnumSet.noneOf(Permiso.class);
		resultado.addAll(permisos);
		return resultado;
	}

	private static Set<Permiso> diferencia(Set<Permiso> a, Set<Permiso> b) {
		Set<Permiso> resultado = copia(a);
		resultado.removeAll(b);
		return resultado;
	}

	private static String nombres(Set<Permiso> permisos) {
		return permisos.isEmpty() ? "ninguno" : permisos.stream().map(Permiso::name).collect(Collectors.joining(", "));
	}

}
