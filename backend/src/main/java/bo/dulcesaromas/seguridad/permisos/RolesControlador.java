package bo.dulcesaromas.seguridad.permisos;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import bo.dulcesaromas.seguridad.IAutenticacion;
import bo.dulcesaromas.seguridad.Permiso;
import bo.dulcesaromas.seguridad.RolBase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Roles fijos y sus permisos (HU04, HU05). A propósito no hay forma de crear ni borrar roles (D6).
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAuthority('SEGURIDAD')")
class RolesControlador {

	private final RolServicio servicio;

	private final IAutenticacion autenticacion;

	RolesControlador(RolServicio servicio, IAutenticacion autenticacion) {
		this.servicio = servicio;
		this.autenticacion = autenticacion;
	}

	@GetMapping("/permisos")
	List<PermisoVista> permisos() {
		return Arrays.stream(Permiso.values()).map(PermisoVista::de).toList();
	}

	@GetMapping("/roles")
	List<RolVista> roles() {
		return servicio.listar().stream().map(RolVista::de).toList();
	}

	@PutMapping("/roles/{codigo}/permisos")
	RolVista editarPermisos(@PathVariable RolBase codigo, @Valid @RequestBody CambioPermisos cambio) {
		return RolVista.de(servicio.editarPermisos(codigo, cambio.permisos(), autenticacion.usuarioActual().id()));
	}

	record CambioPermisos(@NotNull(message = "Indica la lista de permisos, aunque esté vacía.") Set<Permiso> permisos) {
	}

	record PermisoVista(Permiso codigo, String modulo, String accion) {

		static PermisoVista de(Permiso permiso) {
			return new PermisoVista(permiso, permiso.getModulo(), permiso.getAccion());
		}

	}

	record RolVista(RolBase codigo, String nombre, String descripcion, List<Permiso> permisos) {

		static RolVista de(Rol rol) {
			RolBase codigo = rol.getCodigo();
			return new RolVista(codigo, codigo.getNombre(), codigo.getDescripcion(),
					rol.getPermisos().stream().sorted().toList());
		}

	}

}
