package bo.dulcesaromas.seguridad.cuentas;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Buscar o registrar al cliente de un pedido asistido (CU17, HU21). Lo usa quien tiene el permiso de pedidos.
 */
@RestController
@RequestMapping("/api/clientes")
@PreAuthorize("hasAuthority('PEDIDOS_CONSULTAR')")
class ClientesControlador {

	private static final int RESULTADOS = 20;

	private final ClienteRepositorio clientes;

	private final UsuarioRepositorio usuarios;

	private final PasswordEncoder codificador;

	private final Clock reloj;

	ClientesControlador(ClienteRepositorio clientes, UsuarioRepositorio usuarios, PasswordEncoder codificador,
			Clock reloj) {
		this.clientes = clientes;
		this.usuarios = usuarios;
		this.codificador = codificador;
		this.reloj = reloj;
	}

	/**
	 * Por correo o teléfono. Solo clientes activos.
	 */
	@GetMapping
	@Transactional(readOnly = true)
	List<ClienteVista> buscar(@RequestParam String buscar) {
		String texto = buscar.strip();
		if (texto.length() < 3) {
			return List.of();
		}
		return clientes.findByCorreoContainingOrTelefonoContainingOrderByNombreAsc(Usuario.normalizarCorreo(texto),
				texto, Limit.of(RESULTADOS)).stream().filter(Usuario::isActivo).map(ClienteVista::de).toList();
	}

	/**
	 * La cuenta nace sin contraseña que alguien conozca: el cliente la define con «¿Olvidó su contraseña?» (CU12).
	 */
	@PostMapping("/asistidos")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	ClienteVista registrar(@Valid @RequestBody DatosCliente datos) {
		if (usuarios.existsByCorreo(Usuario.normalizarCorreo(datos.correo()))) {
			throw new ReglaNegocioException(HttpStatus.CONFLICT, "CORREO_REGISTRADO",
					"Ya existe una cuenta con este correo. Búscala por su correo.");
		}
		Cliente cliente = clientes.save(new Cliente(datos.nombre(), datos.correo(), datos.telefono().strip(),
				codificador.encode(UUID.randomUUID().toString()), reloj.instant()));
		return ClienteVista.de(cliente);
	}

	record DatosCliente(@NotBlank(message = "Escribe el nombre.") @Size(max = 100, message = "Usa como máximo 100 caracteres.") String nombre,
			@NotBlank(message = "Escribe el correo.") @Email(message = "Escribe un correo válido, como nombre@ejemplo.com.") @Size(max = 254) String correo,
			@NotBlank(message = "Escribe el teléfono.") @Pattern(regexp = "[+0-9 ()-]{7,20}", message = "Escribe un teléfono válido, por ejemplo 70012345.") String telefono) {
	}

	record ClienteVista(Long id, String nombre, String correo, String telefono) {

		static ClienteVista de(Cliente c) {
			return new ClienteVista(c.getId(), c.getNombre(), c.getCorreo(), c.getTelefono());
		}

	}

}
