/**
 * Módulo Cobros: provee IPagos. Llama a IPedidos para confirmar el pedido al aprobar un pago.
 * Solo se expone lo que está en este paquete; los subpaquetes son internos.
 */
@ApplicationModule(displayName = "Cobros", allowedDependencies = { "seguridad", "ecommerce" })
package bo.dulcesaromas.cobros;

import org.springframework.modulith.ApplicationModule;
