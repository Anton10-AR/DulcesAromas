import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Un producto en el carrito con sus opciones y su mensaje. Los precios no se guardan: el total siempre lo
/// calcula el servidor con los precios actuales (RN11).
class LineaCarrito {
  LineaCarrito({required this.productoId, required this.nombre, required this.cantidad, this.opciones = const [],
      this.nombresOpciones = const [], this.mensaje});

  factory LineaCarrito.deJson(Map<String, dynamic> j) => LineaCarrito(
        productoId: j['productoId'] as int,
        nombre: j['nombre'] as String,
        cantidad: j['cantidad'] as int,
        opciones: List<int>.from(j['opciones'] as List),
        nombresOpciones: List<String>.from(j['nombresOpciones'] as List),
        mensaje: j['mensaje'] as String?,
      );

  final int productoId;
  final String nombre;
  int cantidad;
  final List<int> opciones;
  final List<String> nombresOpciones;
  final String? mensaje;

  /// Dos líneas con el mismo producto, opciones y mensaje se juntan en una.
  bool esIgualA(LineaCarrito otra) =>
      productoId == otra.productoId &&
      setEquals(opciones.toSet(), otra.opciones.toSet()) &&
      (mensaje ?? '') == (otra.mensaje ?? '');

  Map<String, dynamic> aJson() => {
        'productoId': productoId,
        'nombre': nombre,
        'cantidad': cantidad,
        'opciones': opciones,
        'nombresOpciones': nombresOpciones,
        'mensaje': mensaje,
      };

  /// La línea como la espera la API.
  Map<String, dynamic> aSolicitud() => {
        'productoId': productoId,
        'cantidad': cantidad,
        'opciones': opciones,
        if (mensaje != null && mensaje!.isNotEmpty) 'mensaje': mensaje,
      };
}

/// El carrito vive en el dispositivo, así que se conserva sin sesión y al iniciarla (HU10, RN15).
class Carrito extends ChangeNotifier {
  static const _clave = 'carrito';

  /// Cantidad máxima de una línea, igual que en la API.
  static const cantidadMaxima = 99;

  final List<LineaCarrito> lineas = [];

  int get unidades => lineas.fold(0, (suma, l) => suma + l.cantidad);

  bool get vacio => lineas.isEmpty;

  Future<void> cargar() async {
    final preferencias = await SharedPreferences.getInstance();
    final guardado = preferencias.getString(_clave);
    if (guardado != null) {
      lineas
        ..clear()
        ..addAll((jsonDecode(guardado) as List).map((e) => LineaCarrito.deJson(e as Map<String, dynamic>)));
      notifyListeners();
    }
  }

  void agregar(LineaCarrito nueva) {
    final igual = lineas.where((l) => l.esIgualA(nueva)).firstOrNull;
    if (igual != null) {
      igual.cantidad = (igual.cantidad + nueva.cantidad).clamp(1, cantidadMaxima);
    } else {
      lineas.add(nueva);
    }
    _cambio();
  }

  void cambiarCantidad(int indice, int cantidad) {
    if (cantidad < 1) {
      lineas.removeAt(indice);
    } else {
      lineas[indice].cantidad = cantidad.clamp(1, cantidadMaxima);
    }
    _cambio();
  }

  void quitar(int indice) {
    lineas.removeAt(indice);
    _cambio();
  }

  void vaciar() {
    lineas.clear();
    _cambio();
  }

  List<Map<String, dynamic>> aSolicitud() => lineas.map((l) => l.aSolicitud()).toList();

  void _cambio() {
    notifyListeners();
    SharedPreferences.getInstance()
        .then((p) => p.setString(_clave, jsonEncode(lineas.map((l) => l.aJson()).toList())));
  }
}
