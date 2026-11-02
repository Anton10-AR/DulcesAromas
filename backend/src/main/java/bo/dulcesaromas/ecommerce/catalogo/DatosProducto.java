package bo.dulcesaromas.ecommerce.catalogo;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Lo que el administrador completa al registrar o editar un producto (CU02).
 */
record DatosProducto(
		@NotBlank(message = "Escribe el nombre.") @Size(max = 100, message = "Usa como máximo 100 caracteres.") String nombre,
		@NotBlank(message = "Escribe una descripción.") @Size(max = 1000, message = "Usa como máximo 1000 caracteres.") String descripcion,
		@NotNull(message = "Escribe el precio.") @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero.")
		@Digits(integer = 8, fraction = 2, message = "Usa un precio con hasta 2 decimales.") BigDecimal precio,
		@NotNull(message = "Elige una categoría.") Long categoriaId,
		@NotNull(message = "Indica si está disponible.") Boolean disponible,
		@NotNull(message = "Indica si es personalizable.") Boolean personalizable,
		@Size(max = 50, message = "Usa como máximo 50 opciones.") List<@Valid DatosOpcion> opciones) {

	List<DatosOpcion> opcionesOVacio() {
		return opciones == null ? List.of() : opciones;
	}

	record DatosOpcion(
			@NotBlank(message = "Escribe el tipo, por ejemplo Tamaño o Sabor.") @Size(max = 40, message = "Usa como máximo 40 caracteres.") String tipo,
			@NotBlank(message = "Escribe el nombre de la opción.") @Size(max = 60, message = "Usa como máximo 60 caracteres.") String nombre,
			@NotNull(message = "Escribe el recargo; puede ser 0.") @DecimalMin(value = "0.00", message = "El recargo no puede ser negativo.")
			@Digits(integer = 8, fraction = 2, message = "Usa un recargo con hasta 2 decimales.") BigDecimal recargo) {
	}

}
