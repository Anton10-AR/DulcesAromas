package bo.dulcesaromas.cobros.pagos;

import bo.dulcesaromas.cobros.IPagos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * La pasarela de pagos simulada de la feria: la página que se abre al escanear el QR de un cobro. Es pública (quien
 * escanea no tiene cuenta) y el código secreto del cobro es la llave. Solo muestra el comercio, el pedido y el monto.
 * Al confirmar hace lo que haría el aviso de un banco: el sistema verifica el pago solo. No se mueve dinero real.
 */
@RestController
@RequestMapping("/api/pasarela")
class PasarelaControlador {

	private final IPagos servicio;

	PasarelaControlador(IPagos servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/{codigo}")
	IPagos.CobroPasarela cobro(@PathVariable String codigo) {
		return servicio.cobroEnPasarela(codigo);
	}

	@PostMapping("/{codigo}/confirmar")
	IPagos.CobroPasarela confirmar(@PathVariable String codigo) {
		return servicio.confirmarEnPasarela(codigo);
	}

}
