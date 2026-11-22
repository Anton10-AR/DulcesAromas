import 'dart:async';

import 'package:flutter/widgets.dart';

import 'api.dart';

/// Vuelve a consultar el servidor cada [intervalo] mientras la pantalla está abierta, y apenas la app vuelve al
/// frente. Así el cliente ve cambiar el estado de su pedido sin deslizar para actualizar (CU15). No es una
/// notificación: solo funciona con la pantalla abierta, y las notificaciones automáticas siguen fuera del alcance.
/// Estas consultas no cuentan como uso de la sesión ([Api.enSegundoPlano]).
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
    if (mounted && siguePorCambiar) Api.enSegundoPlano(actualizarEnSilencio);
  }

  /// Espera [consulta] y, si respondió, la muestra con [asignar]. Si falló, deja lo que ya se ve.
  Future<void> reemplazarSiResponde<F>(Future<F> consulta, void Function(Future<F>) asignar) async {
    try {
      await consulta;
    } catch (_) {
      return;
    }
    if (!mounted) return;
    // En bloque: [asignar] guarda un Future, y la forma con flecha se lo devolvería a setState, que lo rechaza.
    setState(() {
      asignar(consulta);
    });
  }

  @override
  void dispose() {
    _reloj?.cancel();
    _ciclo?.dispose();
    super.dispose();
  }
}
