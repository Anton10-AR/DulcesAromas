import 'package:decimal/decimal.dart';

import '../comun/formato.dart';
import '../comun/modelos.dart';

/// Modelos que solo usa el Panel.

class ItemInventario {
  ItemInventario(this.id, this.nombre, this.unidad, this.stock, this.minimo, this.alerta);

  factory ItemInventario.deJson(Json j) => ItemInventario(
    j['id'] as int,
    j['nombre'] as String,
    j['unidad'] as String,
    decimalDe(j['stock']),
    decimalDe(j['minimo']),
    j['alerta'] as bool,
  );

  final int id;
  final String nombre;
  final String unidad;
  final Decimal stock;
  final Decimal minimo;

  /// En el mínimo o por debajo (RN09).
  final bool alerta;
}

class Movimiento {
  Movimiento(this.tipo, this.cantidad, this.stockResultante, this.motivo, this.usuario, this.fecha);

  factory Movimiento.deJson(Json j) => Movimiento(
    j['tipo'] as String,
    decimalDe(j['cantidad']),
    decimalDe(j['stockResultante']),
    j['motivo'] as String,
    j['usuario'] as String?,
    instante(j['fecha'])!,
  );

  final String tipo;
  final Decimal cantidad;
  final Decimal stockResultante;
  final String motivo;
  final String? usuario;
  final DateTime fecha;
}

const nombresRol = {'ADMINISTRADOR': 'Administrador', 'VENDEDOR': 'Vendedor', 'REPARTIDOR': 'Repartidor'};

class CuentaPersonal {
  CuentaPersonal(this.id, this.nombre, this.correo, this.telefono, this.rol, this.activo, this.bloqueadoHasta);

  factory CuentaPersonal.deJson(Json j) => CuentaPersonal(
    j['id'] as int,
    j['nombre'] as String,
    j['correo'] as String,
    j['telefono'] as String?,
    j['rol'] as String,
    j['activo'] as bool,
    instante(j['bloqueadoHasta']),
  );

  final int id;
  final String nombre;
  final String correo;
  final String? telefono;
  final String rol;
  final bool activo;
  final DateTime? bloqueadoHasta;
}

class PermisoInfo {
  PermisoInfo(this.codigo, this.modulo, this.accion);

  factory PermisoInfo.deJson(Json j) =>
      PermisoInfo(j['codigo'] as String, j['modulo'] as String, j['accion'] as String);

  final String codigo;
  final String modulo;
  final String accion;
}

class Rol {
  Rol(this.codigo, this.nombre, this.descripcion, this.permisos);

  factory Rol.deJson(Json j) => Rol(
    j['codigo'] as String,
    j['nombre'] as String,
    j['descripcion'] as String,
    Set<String>.from(j['permisos'] as List),
  );

  final String codigo;
  final String nombre;
  final String descripcion;
  final Set<String> permisos;
}

class RegistroAuditoria {
  RegistroAuditoria(this.fecha, this.usuario, this.accion, this.detalle);

  factory RegistroAuditoria.deJson(Json j) =>
      RegistroAuditoria(instante(j['fecha'])!, j['usuario'] as String, j['accion'] as String, j['detalle'] as String);

  final DateTime fecha;
  final String usuario;
  final String accion;
  final String detalle;
}

class FilaReporte {
  FilaReporte(this.pedidoId, this.fecha, this.estado, this.total, this.metodo, this.pagado);

  factory FilaReporte.deJson(Json j) => FilaReporte(
    j['pedidoId'] as int,
    instante(j['fecha'])!,
    j['estado'] as String,
    decimalDe(j['total']),
    j['metodo'] as String?,
    j['pagado'] as bool,
  );

  final int pedidoId;
  final DateTime fecha;
  final String estado;
  final Decimal total;
  final String? metodo;
  final bool pagado;
}

class Reporte {
  Reporte(this.cantidadPedidos, this.montoPedidos, this.montoVendido, this.pedidos);

  factory Reporte.deJson(Json j) => Reporte(
    j['cantidadPedidos'] as int,
    decimalDe(j['montoPedidos']),
    decimalDe(j['montoVendido']),
    lista(j['pedidos'], FilaReporte.deJson),
  );

  final int cantidadPedidos;
  final Decimal montoPedidos;
  final Decimal montoVendido;
  final List<FilaReporte> pedidos;
}
