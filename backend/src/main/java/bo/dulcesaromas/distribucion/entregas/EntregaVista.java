package bo.dulcesaromas.distribucion.entregas;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import bo.dulcesaromas.ecommerce.EstadoPedido;

/**
 * Una entrega con lo que hace falta para llevarla. Solo la ve el personal que asigna o el repartidor a quien
 * está asignada (RNF16).
 */
record EntregaVista(Long pedidoId, EstadoPedido estado, String cliente, String direccion, String telefono,
		Instant horarioEntrega, List<String> productos, BigDecimal total, Persona repartidor,
		String observacionFallo, List<Asignacion> asignaciones) {

	record Persona(Long id, String nombre) {
	}

	record Asignacion(String repartidor, String asignadoPor, Instant fecha) {
	}

}
