/**
 * Módulo Seguridad: cuentas, sesiones, roles fijos con permisos editables y auditoría.
 * Provee IAutenticacion. Solo se expone lo que está en este paquete; los subpaquetes son internos.
 */
@ApplicationModule(displayName = "Seguridad", allowedDependencies = { "compartido" })
package bo.dulcesaromas.seguridad;

import org.springframework.modulith.ApplicationModule;
