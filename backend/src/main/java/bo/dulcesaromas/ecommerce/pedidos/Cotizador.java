package bo.dulcesaromas.ecommerce.pedidos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import bo.dulcesaromas.compartido.ReglaNegocioException;
import bo.dulcesaromas.ecommerce.IProductos;
import bo.dulcesaromas.ecommerce.IProductos.Opcion;
import bo.dulcesaromas.ecommerce.IProductos.ProductoParaPedido;
import bo.dulcesaromas.ecommerce.TipoEntrega;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Arma las líneas de un carrito con los precios y recargos actuales y calcula el total (RN11). Los productos los da
 * el catálogo por IProductos.
 */
@Component
class Cotizador {

	private static final BigDecimal CERO = new BigDecimal("0.00");

	private final IProductos productos;

	private final ConfiguracionRepositorio configuracion;

	Cotizador(IProductos productos, ConfiguracionRepositorio configuracion) {
		this.productos = productos;
		this.configuracion = configuracion;
	}

	/**
	 * @param bloquear bloquea los productos hasta el fin de la transacción, para confirmar el pedido
	 */
	Cotizacion cotizar(SolicitudPedido solicitud, boolean bloquear) {
		Set<Long> ids = solicitud.lineas().stream().map(SolicitudPedido.LineaSolicitud::productoId).collect(Collectors.toSet());
		Map<Long, ProductoParaPedido> porId = productos.paraPedido(ids, bloquear).stream()
			.collect(Collectors.toMap(ProductoParaPedido::id, Function.identity()));
		List<LineaCotizada> lineas = new ArrayList<>();
		for (var linea : solicitud.lineas()) {
			ProductoParaPedido producto = porId.get(linea.productoId());
			if (producto == null) {
				throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "PRODUCTO_NO_ENCONTRADO",
						"Uno de los productos del carrito ya no existe. Quítalo para continuar.");
			}
			lineas.add(new LineaCotizada(new LineaPedido(producto.id(), producto.nombre(), producto.precio(),
					linea.cantidad(), mensaje(producto, linea.mensaje()), opciones(producto, linea.opcionesOVacio())),
					producto.disponible()));
		}
		TipoEntrega tipo = solicitud.tipoEntrega() == null ? TipoEntrega.RECOJO : solicitud.tipoEntrega();
		BigDecimal envio = tipo == TipoEntrega.DELIVERY ? configuracion.actual().getTarifaEnvio() : CERO;
		BigDecimal total = lineas.stream().map(l -> l.linea().getSubtotal()).reduce(envio, BigDecimal::add);
		return new Cotizacion(lineas, envio, total);
	}

	/**
	 * Las opciones deben ser del producto, y como máximo una de cada tipo (un tamaño, un sabor).
	 */
	private static List<OpcionElegida> opciones(ProductoParaPedido producto, List<Long> elegidas) {
		Set<Long> ids = new HashSet<>(elegidas);
		List<Opcion> delProducto = producto.opciones();
		if (!delProducto.stream().map(Opcion::id).collect(Collectors.toSet()).containsAll(ids)) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "OPCION_INVALIDA",
					"Una de las opciones elegidas ya no está disponible para «" + producto.nombre() + "».");
		}
		Set<String> tipos = new HashSet<>();
		List<OpcionElegida> resultado = new ArrayList<>();
		for (Opcion opcion : delProducto) {
			if (!ids.contains(opcion.id())) {
				continue;
			}
			if (!tipos.add(opcion.tipo())) {
				throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "OPCION_REPETIDA",
						"Elige solo una opción de " + opcion.tipo() + " para «" + producto.nombre() + "».");
			}
			resultado.add(new OpcionElegida(opcion.tipo(), opcion.nombre(), opcion.recargo()));
		}
		boolean tieneTamanos = delProducto.stream().anyMatch(o -> esTamano(o.tipo()));
		if (tieneTamanos && tipos.stream().noneMatch(Cotizador::esTamano)) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "TAMANO_REQUERIDO",
					"Elige el tamaño de «" + producto.nombre() + "».");
		}
		return resultado;
	}

	/**
	 * Si el producto ofrece tamaños, hay que elegir uno: sin él, el pedido de una torta queda incompleto (decisión del
	 * equipo del 08/11/2026, sección 10 de los casos de uso). Las demás opciones siguen siendo optativas.
	 */
	static boolean esTamano(String tipo) {
		return tipo != null && tipo.strip().toLowerCase(Locale.ROOT).replace('ñ', 'n').equals("tamano");
	}

	/**
	 * El mensaje va sobre una torta, así que solo lo admite un producto personalizable (RN17).
	 */
	private static String mensaje(ProductoParaPedido producto, String mensaje) {
		if (!StringUtils.hasText(mensaje)) {
			return null;
		}
		if (!producto.personalizable()) {
			throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "MENSAJE_NO_PERMITIDO",
					"«" + producto.nombre() + "» no admite un mensaje escrito.");
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
