package bo.dulcesaromas.seguridad.cuentas;

import java.time.Instant;
import java.util.List;

import bo.dulcesaromas.seguridad.IAutenticacion;
import bo.dulcesaromas.seguridad.RolBase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cuentas de personal (HU03) y asignación de rol (HU04).
 */
@RestController
@RequestMapping("/api/personal")
@PreAuthorize("hasAuthority('SEGURIDAD')")
class PersonalControlador {

	private final PersonalServicio servicio;

	private final IAutenticacion autenticacion;

	PersonalControlador(PersonalServicio servicio, IAutenticacion autenticacion) {
		this.servicio = servicio;
		this.autenticacion = autenticacion;
	}

	@GetMapping
	List<CuentaPersonal> listar() {
		return servicio.listar().stream().map(CuentaPersonal::de).toList();
	}

	@GetMapping("/{id}")
	CuentaPersonal obtener(@PathVariable Long id) {
		return CuentaPersonal.de(servicio.obtener(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	CuentaPersonal crear(@Valid @RequestBody NuevaCuenta datos) {
		return CuentaPersonal.de(servicio.crear(datos.nombre(), datos.correo(), datos.telefono(), datos.rol(),
				datos.contrasenaInicial(), actor()));
	}

	@PutMapping("/{id}")
	CuentaPersonal editar(@PathVariable Long id, @Valid @RequestBody CambiosCuenta datos) {
		return CuentaPersonal.de(
				servicio.editar(id, datos.nombre(), datos.correo(), datos.telefono(), datos.rol(), actor()));
	}

	@PostMapping("/{id}/activar")
	CuentaPersonal activar(@PathVariable Long id) {
		return CuentaPersonal.de(servicio.activar(id, actor()));
	}

	@PostMapping("/{id}/desactivar")
	CuentaPersonal desactivar(@PathVariable Long id) {
		return CuentaPersonal.de(servicio.desactivar(id, actor()));
	}

	private Long actor() {
		return autenticacion.usuarioActual().id();
	}

	record NuevaCuenta(@NotBlank(message = "Escribe el nombre.") @Size(max = 100, message = "Usa como máximo 100 caracteres.") String nombre,
			@NotBlank(message = "Escribe el correo.") @Email(message = "Escribe un correo válido, como nombre@ejemplo.com.") @Size(max = 254) String correo,
			@Size(max = 20, message = "Usa como máximo 20 caracteres.") String telefono,
			@NotNull(message = "Elige un rol.") RolBase rol,
			@ContrasenaValida String contrasenaInicial) {
	}

	record CambiosCuenta(@NotBlank(message = "Escribe el nombre.") @Size(max = 100, message = "Usa como máximo 100 caracteres.") String nombre,
			@NotBlank(message = "Escribe el correo.") @Email(message = "Escribe un correo válido, como nombre@ejemplo.com.") @Size(max = 254) String correo,
			@Size(max = 20, message = "Usa como máximo 20 caracteres.") String telefono,
			@NotNull(message = "Elige un rol.") RolBase rol) {
	}

	record CuentaPersonal(Long id, String nombre, String correo, String telefono, RolBase rol, boolean activo,
			Instant bloqueadoHasta, Instant creado) {

		static CuentaPersonal de(Personal p) {
			return new CuentaPersonal(p.getId(), p.getNombre(), p.getCorreo(), p.getTelefono(),
					p.getRol().getCodigo(), p.isActivo(), p.getBloqueadoHasta(), p.getCreado());
		}

	}

}
