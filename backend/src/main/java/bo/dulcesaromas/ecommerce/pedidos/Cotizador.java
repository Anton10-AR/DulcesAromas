package bo.dulcesaromas.ecommerce.pedidos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.ecommerce.catalogo.OpcionPersonalizacion;
import bo.dulcesaromas.ecommerce.catalogo.Producto;
import bo.dulcesaromas.ecommerce.catalogo.ProductoRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Arma las líneas de un carrito con los precios y recargos actuales y calcula el total (RN11).
 */
@Component
class Cotizador {

	private static final BigDecimal CERO = new BigDecimal("0.00");

	private final ProductoRepositorio productos;

	private final ConfiguracionRepositorio configuracion;

	Cotizador(ProductoRepositorio productos, ConfiguracionRepositorio configuracion) {
		this.productos = productos;
		this.configuracion = configuracion;
	}

	/**
	 * @param bloquear bloquea los productos hasta el fin de la transacción, para confirmar el pedido
	 */
	Cotizacion cotizar(SolicitudPedido solicitud, boolean bloquear) {
		Set<Long> ids = solicitud.lineas().stream().map(SolicitudPedido.LineaSolicitud::productoId).collect(Collectors.toSet());
		Map<Long, Producto> porId = (bloquear ? productos.bloquearParaPedido(ids) : productos.findAllById(ids)).stream()
			.collect(Collectors.toMap(Producto::getId, Function.identity()));
		List<LineaCotizada> lineas = new ArrayList<>();
		for (var linea : solicitud.lineas()) {
			Producto producto = porId.get(linea.productoId());
			if (producto == null) {
				throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "PRODUCTO_NO_ENCONTRADO",
						"Uno de los productos del carrito ya no existe. Quítalo para continuar.");
			}
			lineas.add(new LineaCotizada(new LineaPedido(producto.getId(), producto.getNombre(), producto.getPrecio(),
					linea.cantidad(), mensaje(producto, linea.mensaje()), opciones(producto, linea.opcionesOVacio())),
					producto.esVisible() && producto.isDisponible()));
		}
		TipoEntrega tipo = solicitud.tipoEntrega() == null ? TipoEntrega.RECOJO : solicitud.tipoEntrega();
		BigDecimal envio = tipo == TipoEntrega.DELIVERY ? configuracion.actual().getTarifaEnvio() : CERO;
		BigDecimal total = lineas.stream().map(l -> l.linea().getSubtotal()).reduce(envio, BigDecimal::add);
		return new Cotizacion(lineas, envio, total);
	}

	/**
	 * Las opciones deben ser del producto, y como máximo una de cada tipo (un tamaño, un sabor).
	 */
	private static List<OpcionElegida> opciones(Producto producto, List<Long> elegidas) {
		Set<Long> ids = new HashSet<>(elegidas);
		List<OpcionPersonalizacion> delProducto = producto.getOpciones();
		if (!delProducto.stream().map(OpcionPersonalizacion::getId).collect(Collectors.toSet()).containsAll(ids)) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "OPCION_INVALIDA",
					"Una de las opciones elegidas ya no está disponible para «" + producto.getNombre() + "».");
		}
		Set<String> tipos = new HashSet<>();
		List<OpcionElegida> resultado = new ArrayList<>();
		for (OpcionPersonalizacion opcion : delProducto) {
			if (!ids.contains(opcion.getId())) {
				continue;
			}
			if (!tipos.add(opcion.getTipo())) {
				throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "OPCION_REPETIDA",
						"Elige solo una opción de " + opcion.getTipo() + " para «" + producto.getNombre() + "».");
			}
			resultado.add(new OpcionElegida(opcion.getTipo(), opcion.getNombre(), opcion.getRecargo()));
		}
		return resultado;
	}

	/**
	 * El mensaje va sobre una torta, así que solo lo admite un producto personalizable (RN17).
	 */
	private static String mensaje(Producto producto, String mensaje) {
		if (!StringUtils.hasText(mensaje)) {
			return null;
		}
		if (!producto.isPersonalizable()) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "MENSAJE_NO_PERMITIDO",
					"«" + producto.getNombre() + "» no admite un mensaje escrito.");
		}
		return mensaje.strip();
	}

	record LineaCotizada(LineaPedido linea, boolean disponible) {
	}

	record Cotizacion(List<LineaCotizada> lineas, BigDecimal costoEnvio, BigDecimal total) {

		List<String> noDisponibles() {
			return lineas.stream().filter(l -> !l.disponible()).map(l -> l.linea().getNombreProducto()).distinct().toList();
		}

	}

}
