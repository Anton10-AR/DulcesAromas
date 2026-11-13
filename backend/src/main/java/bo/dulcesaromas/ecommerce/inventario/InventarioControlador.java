package bo.dulcesaromas.ecommerce.inventario;

import java.math.BigDecimal;
import java.util.List;

import bo.dulcesaromas.ecommerce.IStock;
import bo.dulcesaromas.ecommerce.IStock.Insumo;
import bo.dulcesaromas.ecommerce.TipoMovimiento;
import bo.dulcesaromas.seguridad.IAutenticacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
 * Inventario de insumos (HU09). El administrador registra movimientos; el Vendedor solo consulta existencias y
 * alertas.
 */
@RestController
@RequestMapping("/api/inventario")
class InventarioControlador {

	private final IStock servicio;

	private final IAutenticacion autenticacion;

	InventarioControlador(IStock servicio, IAutenticacion autenticacion) {
		this.servicio = servicio;
		this.autenticacion = autenticacion;
	}

	@GetMapping
	@PreAuthorize("hasAnyAuthority('INVENTARIO_CONSULTAR', 'INVENTARIO_MOVIMIENTOS')")
	List<Insumo> listar() {
		return servicio.insumos();
	}

	/**
	 * Insumos en su mínimo o por debajo (RN09).
	 */
	@GetMapping("/alertas")
	@PreAuthorize("hasAnyAuthority('INVENTARIO_CONSULTAR', 'INVENTARIO_MOVIMIENTOS')")
	List<Insumo> alertas() {
		return servicio.alertas();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('INVENTARIO_MOVIMIENTOS')")
	Insumo crear(@Valid @RequestBody DatosItem datos) {
		return servicio.crear(datos.nombre(), datos.unidad(), datos.minimo());
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('INVENTARIO_MOVIMIENTOS')")
	Insumo editar(@PathVariable Long id, @Valid @RequestBody DatosItem datos) {
		return servicio.editar(id, datos.nombre(), datos.unidad(), datos.minimo());
	}

	@PostMapping("/{id}/movimientos")
	@PreAuthorize("hasAuthority('INVENTARIO_MOVIMIENTOS')")
	Insumo registrarMovimiento(@PathVariable Long id, @Valid @RequestBody DatosMovimiento datos) {
		return servicio.registrarMovimiento(id, datos.tipo(), datos.cantidad(), datos.motivo(),
				autenticacion.usuarioActual().id());
	}

	@GetMapping("/{id}/movimientos")
	@PreAuthorize("hasAuthority('INVENTARIO_MOVIMIENTOS')")
	List<IStock.Movimiento> historial(@PathVariable Long id) {
		return servicio.historial(id);
	}

	record DatosItem(@NotBlank(message = "Escribe el nombre.") @Size(max = 60, message = "Usa como máximo 60 caracteres.") String nombre,
			@NotBlank(message = "Escribe la unidad, por ejemplo kg o unidades.") @Size(max = 20, message = "Usa como máximo 20 caracteres.") String unidad,
			@NotNull(message = "Escribe el mínimo.") @DecimalMin(value = "0", message = "El mínimo no puede ser negativo.")
			@Digits(integer = 9, fraction = 3, message = "Usa hasta 3 decimales.") BigDecimal minimo) {
	}

	/**
	 * @param cantidad para un ajuste, el stock contado
	 */
	record DatosMovimiento(@NotNull(message = "Elige entrada, salida o ajuste.") TipoMovimiento tipo,
			@NotNull(message = "Escribe la cantidad.") @DecimalMin(value = "0", message = "La cantidad no puede ser negativa.")
			@Digits(integer = 9, fraction = 3, message = "Usa hasta 3 decimales.") BigDecimal cantidad,
			@NotBlank(message = "Escribe el motivo.") @Size(max = 255, message = "Usa como máximo 255 caracteres.") String motivo) {
	}


}
