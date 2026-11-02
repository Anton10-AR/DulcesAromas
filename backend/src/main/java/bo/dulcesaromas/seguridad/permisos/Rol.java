package bo.dulcesaromas.seguridad.permisos;

import java.util.HashSet;
import java.util.Set;

import bo.dulcesaromas.seguridad.Permiso;
import bo.dulcesaromas.seguridad.RolBase;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * Uno de los roles fijos con los permisos que el administrador le dio (RF25).
 */
@Entity
@Table(name = "rol")
public class Rol {

	@Id
	@Enumerated(EnumType.STRING)
	private RolBase codigo;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "rol_permiso", joinColumns = @JoinColumn(name = "rol"))
	@Column(name = "permiso")
	@Enumerated(EnumType.STRING)
	private Set<Permiso> permisos = new HashSet<>();

	protected Rol() {
	}

	void reemplazarPermisos(Set<Permiso> nuevos) {
		permisos.clear();
		permisos.addAll(nuevos);
	}

	public RolBase getCodigo() {
		return codigo;
	}

	public Set<Permiso> getPermisos() {
		return permisos;
	}

}
