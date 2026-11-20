import 'dart:async';

import 'package:flutter/widgets.dart';

/// Vuelve a consultar el servidor cada [intervalo] mientras la pantalla está abierta, y apenas la app vuelve al
/// frente. Así el cliente ve cambiar el estado de su pedido sin deslizar para actualizar (CU15). No es una
/// notificación: solo funciona con la pantalla abierta, y las notificaciones automáticas siguen fuera del alcance.
mixin ActualizacionPeriodica<T extends StatefulWidget> on State<T> {
  static const intervalo = Duration(seconds: 20);

  Timer? _reloj;
  AppLifecycleListener? _ciclo;

  /// Trae los datos de nuevo sin mostrar el indicador de carga. Si falla, deja lo que ya se ve.
  Future<void> actualizarEnSilencio();

  /// Falso cuando ya no hay nada que pueda cambiar, por ejemplo un pedido Entregado.
  bool get siguePorCambiar => true;

  @override
  void initState() {
    super.initState();
    _reloj = Timer.periodic(intervalo, (_) => _actualizar());
    _ciclo = AppLifecycleListener(onResume: _actualizar);
  }

  void _actualizar() {
    if (mounted && siguePorCambiar) actualizarEnSilencio();
  }

  @override
  void dispose() {
    _reloj?.cancel();
    _ciclo?.dispose();
    super.dispose();
  }
}
