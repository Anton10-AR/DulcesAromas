package bo.dulcesaromas.ecommerce.pedidos;

import java.math.BigDecimal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tarifa fija de envío (D4).
 */
@RestController
@RequestMapping("/api/configuracion")
@PreAuthorize("hasAuthority('CONFIGURACION')")
class ConfiguracionControlador {

	private final PedidoServicio servicio;

	ConfiguracionControlador(PedidoServicio servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	DatosConfiguracion obtener() {
		return new DatosConfiguracion(servicio.tarifaEnvio());
	}

	@PutMapping
	DatosConfiguracion cambiar(@Valid @RequestBody DatosConfiguracion datos) {
		return new DatosConfiguracion(servicio.cambiarTarifaEnvio(datos.tarifaEnvio()));
	}

	record DatosConfiguracion(
			@NotNull(message = "Escribe la tarifa de envío.") @DecimalMin(value = "0.00", message = "La tarifa no puede ser negativa.")
			@Digits(integer = 8, fraction = 2, message = "Usa una tarifa con hasta 2 decimales.") BigDecimal tarifaEnvio) {
	}

}
