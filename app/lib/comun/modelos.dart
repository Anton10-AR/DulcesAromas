import 'package:decimal/decimal.dart';

import 'formato.dart';

/// Modelos con la forma que devuelve la API. Solo leen JSON; los montos los calcula siempre el servidor.

typedef Json = Map<String, dynamic>;

List<T> lista<T>(Object? json, T Function(Json) de) => (json as List).map((e) => de(e as Json)).toList();

DateTime? instante(Object? json) => json == null ? null : DateTime.parse(json as String);

class Categoria {
  Categoria(this.id, this.nombre);

  factory Categoria.deJson(Json j) => Categoria(j['id'] as int, j['nombre'] as String);

  final int id;
  final String nombre;
}

class Opcion {
  Opcion(this.id, this.tipo, this.nombre, this.recargo);

  factory Opcion.deJson(Json j) =>
      Opcion(j['id'] as int?, j['tipo'] as String, j['nombre'] as String, decimalDe(j['recargo']));

  final int? id;
  final String tipo;
  final String nombre;
  final Decimal recargo;
}

class Producto {
  Producto({
    required this.id,
    required this.nombre,
    required this.descripcion,
    required this.precio,
    required this.categoria,
    required this.activo,
    required this.disponible,
    required this.personalizable,
    this.imagen,
    required this.opciones,
  });

  factory Producto.deJson(Json j) => Producto(
    id: j['id'] as int,
    nombre: j['nombre'] as String,
    descripcion: j['descripcion'] as String,
    precio: decimalDe(j['precio']),
    categoria: Categoria.deJson(j['categoria'] as Json),
    activo: j['activo'] as bool,
    disponible: j['disponible'] as bool,
    personalizable: j['personalizable'] as bool,
    imagen: j['imagen'] as String?,
    opciones: lista(j['opciones'], Opcion.deJson),
  );

  final int id;
  final String nombre;
  final String descripcion;
  final Decimal precio;
  final Categoria categoria;
  final bool activo;
  final bool disponible;
  final bool personalizable;
  final String? imagen;
  final List<Opcion> opciones;

  /// Las opciones agrupadas por tipo, en el orden en que las definió el administrador.
  Map<String, List<Opcion>> get opcionesPorTipo {
    final grupos = <String, List<Opcion>>{};
    for (final o in opciones) {
      grupos.putIfAbsent(o.tipo, () => []).add(o);
    }
    return grupos;
  }
}

class LineaVista {
  LineaVista({
    required this.productoId,
    required this.nombre,
    required this.cantidad,
    required this.precioUnitario,
    required this.opciones,
    this.mensaje,
    required this.subtotal,
    required this.disponible,
  });

  factory LineaVista.deJson(Json j) => LineaVista(
    productoId: j['productoId'] as int,
    nombre: j['nombre'] as String,
    cantidad: j['cantidad'] as int,
    precioUnitario: decimalDe(j['precioUnitario']),
    opciones: lista(j['opciones'], Opcion.deJson),
    mensaje: j['mensaje'] as String?,
    subtotal: decimalDe(j['subtotal']),
    disponible: j['disponible'] as bool,
  );

  final int productoId;
  final String nombre;
  final int cantidad;
  final Decimal precioUnitario;
  final List<Opcion> opciones;
  final String? mensaje;
  final Decimal subtotal;
  final bool disponible;
}

/// El carrito con precios y disponibilidad actuales, calculado por el servidor (RN08, RN11).
class Cotizacion {
  Cotizacion(this.lineas, this.costoEnvio, this.total, this.confirmable, this.noDisponibles);

  factory Cotizacion.deJson(Json j) => Cotizacion(
    lista(j['lineas'], LineaVista.deJson),
    decimalDe(j['costoEnvio']),
    decimalDe(j['total']),
    j['confirmable'] as bool,
    List<String>.from(j['noDisponibles'] as List),
  );

  final List<LineaVista> lineas;
  final Decimal costoEnvio;
  final Decimal total;
  final bool confirmable;
  final List<String> noDisponibles;
}

const nombresEstadoPedido = {
  'PENDIENTE': 'Pendiente',
  'CONFIRMADO': 'Confirmado',
  'EN_PREPARACION': 'En preparación',
  'LISTO': 'Listo',
  'EN_CAMINO': 'En camino',
  'ENTREGADO': 'Entregado',
  'CANCELADO': 'Cancelado',
};

const nombresEstadoPago = {
  'PENDIENTE_DE_PAGO': 'Pendiente de pago',
  'EN_VERIFICACION': 'En verificación',
  'PAGADO': 'Pagado',
  'RECHAZADO': 'Rechazado',
};

class PedidoResumen {
  PedidoResumen({
    required this.id,
    required this.estado,
    required this.tipoEntrega,
    required this.fechaCreacion,
    required this.horarioEntrega,
    required this.productos,
    required this.total,
    required this.asistido,
  });

  factory PedidoResumen.deJson(Json j) => PedidoResumen(
    id: j['id'] as int,
    estado: j['estado'] as String,
    tipoEntrega: j['tipoEntrega'] as String,
    fechaCreacion: instante(j['fechaCreacion'])!,
    horarioEntrega: instante(j['horarioEntrega'])!,
    productos: j['productos'] as int,
    total: decimalDe(j['total']),
    asistido: j['asistido'] as bool,
  );

  final int id;
  final String estado;
  final String tipoEntrega;
  final DateTime fechaCreacion;
  final DateTime horarioEntrega;
  final int productos;
  final Decimal total;
  final bool asistido;
}

class Cambio {
  Cambio(this.anterior, this.nuevo, this.fecha, this.usuario, this.observacion);

  factory Cambio.deJson(Json j) => Cambio(
    j['anterior'] as String?,
    j['nuevo'] as String,
    instante(j['fecha'])!,
    j['usuario'] as String?,
    j['observacion'] as String?,
  );

  final String? anterior;
  final String nuevo;
  final DateTime fecha;
  final String? usuario;
  final String? observacion;
}

class Pedido {
  Pedido({
    required this.id,
    required this.estado,
    required this.tipoEntrega,
    this.direccion,
    required this.telefono,
    required this.horarioEntrega,
    required this.lineas,
    required this.costoEnvio,
    required this.total,
    required this.fechaCreacion,
    required this.asistido,
    this.motivoCancelacion,
    required this.historial,
  });

  factory Pedido.deJson(Json j) => Pedido(
    id: j['id'] as int,
    estado: j['estado'] as String,
    tipoEntrega: j['tipoEntrega'] as String,
    direccion: j['direccion'] as String?,
    telefono: j['telefono'] as String,
    horarioEntrega: instante(j['horarioEntrega'])!,
    lineas: lista(j['lineas'], LineaVista.deJson),
    costoEnvio: decimalDe(j['costoEnvio']),
    total: decimalDe(j['total']),
    fechaCreacion: instante(j['fechaCreacion'])!,
    asistido: j['asistido'] as bool,
    motivoCancelacion: j['motivoCancelacion'] as String?,
    historial: lista(j['historial'], Cambio.deJson),
  );

  final int id;
  final String estado;
  final String tipoEntrega;
  final String? direccion;
  final String telefono;
  final DateTime horarioEntrega;
  final List<LineaVista> lineas;
  final Decimal costoEnvio;
  final Decimal total;
  final DateTime fechaCreacion;
  final bool asistido;
  final String? motivoCancelacion;
  final List<Cambio> historial;

  bool get esDelivery => tipoEntrega == 'DELIVERY';

  /// RN14: solo se cancela antes de la preparación.
  bool get cancelable => estado == 'PENDIENTE' || estado == 'CONFIRMADO';
}

class Verificacion {
  Verificacion(this.decision, this.observacion, this.verificador, this.fecha);

  factory Verificacion.deJson(Json j) => Verificacion(
    j['decision'] as String,
    j['observacion'] as String?,
    j['verificador'] as String?,
    instante(j['fecha'])!,
  );

  final String decision;
  final String? observacion;
  final String? verificador;
  final DateTime fecha;
}

class Pago {
  Pago({
    required this.id,
    required this.pedidoId,
    required this.monto,
    required this.estado,
    this.metodo,
    this.referencia,
    this.registrado,
    this.comprobante,
    this.verificacion,
  });

  factory Pago.deJson(Json j) => Pago(
    id: j['id'] as int,
    pedidoId: j['pedidoId'] as int,
    monto: decimalDe(j['monto']),
    estado: j['estado'] as String,
    metodo: j['metodo'] as String?,
    referencia: j['referencia'] as String?,
    registrado: instante(j['registrado']),
    comprobante: j['comprobante'] as String?,
    verificacion: j['verificacion'] == null ? null : Verificacion.deJson(j['verificacion'] as Json),
  );

  final int id;
  final int pedidoId;
  final Decimal monto;
  final String estado;
  final String? metodo;
  final String? referencia;
  final DateTime? registrado;
  final String? comprobante;
  final Verificacion? verificacion;
}

/// El estado del cobro de un pedido, para Mis pedidos (HU19).
class Cobro {
  Cobro(this.pedidoId, this.pagoId, this.estado, this.monto, this.observacionRechazo);

  factory Cobro.deJson(Json j) => Cobro(
    j['pedidoId'] as int,
    j['pagoId'] as int,
    j['estado'] as String,
    decimalDe(j['monto']),
    j['observacionRechazo'] as String?,
  );

  final int pedidoId;
  final int pagoId;
  final String estado;
  final Decimal monto;
  final String? observacionRechazo;
}

class CategoriaPanel {
  CategoriaPanel(this.id, this.nombre, this.activa, this.productosActivos);

  factory CategoriaPanel.deJson(Json j) =>
      CategoriaPanel(j['id'] as int, j['nombre'] as String, j['activa'] as bool, j['productosActivos'] as int);

  final int id;
  final String nombre;
  final bool activa;
  final int productosActivos;
}

class Persona {
  Persona(this.id, this.nombre);

  factory Persona.deJson(Json j) => Persona(j['id'] as int, j['nombre'] as String? ?? '');

  final int id;
  final String nombre;
}

/// Una entrega de un pedido con delivery, con lo que necesita el repartidor (CU08).
class Entrega {
  Entrega({
    required this.pedidoId,
    required this.estado,
    this.cliente,
    this.direccion,
    required this.telefono,
    required this.horarioEntrega,
    required this.productos,
    required this.total,
    required this.repartidor,
    this.observacionFallo,
  });

  factory Entrega.deJson(Json j) => Entrega(
    pedidoId: j['pedidoId'] as int,
    estado: j['estado'] as String,
    cliente: j['cliente'] as String?,
    direccion: j['direccion'] as String?,
    telefono: j['telefono'] as String,
    horarioEntrega: instante(j['horarioEntrega'])!,
    productos: List<String>.from(j['productos'] as List),
    total: decimalDe(j['total']),
    repartidor: Persona.deJson(j['repartidor'] as Json),
    observacionFallo: j['observacionFallo'] as String?,
  );

  final int pedidoId;
  final String estado;
  final String? cliente;
  final String? direccion;
  final String telefono;
  final DateTime horarioEntrega;
  final List<String> productos;
  final Decimal total;
  final Persona repartidor;
  final String? observacionFallo;
}

class MetodoPago {
  MetodoPago(this.id, this.nombre, this.instrucciones, this.activo, this.qr);

  factory MetodoPago.deJson(Json j) => MetodoPago(
    j['id'] as int,
    j['nombre'] as String,
    j['instrucciones'] as String,
    j['activo'] as bool,
    j['qr'] as String?,
  );

  final int id;
  final String nombre;
  final String instrucciones;
  final bool activo;
  final String? qr;
}
