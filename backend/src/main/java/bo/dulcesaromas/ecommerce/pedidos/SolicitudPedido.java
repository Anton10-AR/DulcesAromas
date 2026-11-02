package bo.dulcesaromas.ecommerce.pedidos;

import java.time.OffsetDateTime;
import java.util.List;

import bo.dulcesaromas.ecommerce.TipoEntrega;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * El carrito que manda la Tienda, para cotizarlo o para confirmarlo como pedido (CU05). El carrito vive en la
 * app, así que se conserva sin sesión y al iniciarla (HU10).
 */
record SolicitudPedido(
		@NotEmpty(message = "Agrega al menos un producto.") @Size(max = 50, message = "Usa como máximo 50 productos distintos.")
		List<@Valid @NotNull LineaSolicitud> lineas,
		TipoEntrega tipoEntrega,
		@Size(max = 255, message = "Usa como máximo 255 caracteres.") String direccion,
		@Pattern(regexp = "[+0-9 ()-]{7,20}", message = "Escribe un teléfono válido, por ejemplo 70012345.") String telefono,
		OffsetDateTime horarioEntrega) {

	/**
	 * @param opciones ids de las opciones de personalización elegidas, como máximo una de cada tipo
	 */
	record LineaSolicitud(
			@NotNull(message = "Elige un producto.") Long productoId,
			@NotNull(message = "Indica la cantidad.") @Min(value = 1, message = "La cantidad mínima es 1.")
			@Max(value = 99, message = "La cantidad máxima es 99.") Integer cantidad,
			@Size(max = 10, message = "Usa como máximo 10 opciones.") List<@NotNull Long> opciones,
			@Size(max = 24, message = "El mensaje sobre la torta admite hasta 24 caracteres.") String mensaje) {

		List<Long> opcionesOVacio() {
			return opciones == null ? List.of() : opciones;
		}

	}

}
