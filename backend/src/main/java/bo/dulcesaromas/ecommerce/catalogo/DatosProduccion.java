package bo.dulcesaromas.ecommerce.catalogo;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Lo que el administrador define para producir un producto (CU02): cuántas unidades se aceptan por pedido (RN18) y la
 * receta aproximada con su tiempo (RF45, D12). Todo es opcional: sin máximo rige el tope de 99 por línea, y un
 * producto sin receta no entra en la estimación.
 */
record DatosProduccion(
		@Min(value = 1, message = "El máximo va de 1 a 99.") @Max(value = 99, message = "El máximo va de 1 a 99.") Integer maximoPorPedido,
		@Min(value = 1, message = "Usa de 1 a 1440 minutos.") @Max(value = 1440, message = "Usa de 1 a 1440 minutos.") Integer minutosPorUnidad,
		@Size(max = 30, message = "Usa como máximo 30 insumos.") List<@Valid DatosIngrediente> receta) {

	List<DatosIngrediente> recetaOVacia() {
		return receta == null ? List.of() : receta;
	}

	record DatosIngrediente(@NotNull(message = "Elige el insumo.") Long insumoId,
			@NotNull(message = "Escribe la cantidad.") @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor que cero.")
			@Digits(integer = 9, fraction = 3, message = "Usa una cantidad con hasta 3 decimales.") BigDecimal cantidad) {
	}

}
