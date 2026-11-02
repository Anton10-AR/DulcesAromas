/**
 * Módulo E-commerce: catálogo, inventario y pedidos. Provee IProductos, IStock e IPedidos.
 * Solo se expone lo que está en este paquete; los subpaquetes son internos.
 */
@ApplicationModule(displayName = "E-commerce", allowedDependencies = { "seguridad" })
package bo.dulcesaromas.ecommerce;

import org.springframework.modulith.ApplicationModule;
