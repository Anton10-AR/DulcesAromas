import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// El tema que eligió la persona: el del dispositivo (por omisión), claro u oscuro (Visión 6.1). Se guarda en el
/// dispositivo; si no se puede guardar (por ejemplo, el navegador bloquea el almacenamiento), la elección vale hasta
/// cerrar la app y todo lo demás funciona igual.
class PreferenciaTema extends ChangeNotifier {
  PreferenciaTema({this.claveGuardado = 'tema'});

  final String claveGuardado;
  ThemeMode _modo = ThemeMode.system;

  ThemeMode get modo => _modo;

  Future<void> cargar() async {
    try {
      final guardado = (await SharedPreferences.getInstance()).getString(claveGuardado);
      _modo = ThemeMode.values.firstWhere((m) => m.name == guardado, orElse: () => ThemeMode.system);
    } on Object {
      _modo = ThemeMode.system;
    }
  }

  Future<void> elegir(ThemeMode modo) async {
    _modo = modo;
    notifyListeners();
    try {
      final preferencias = await SharedPreferences.getInstance();
      if (modo == ThemeMode.system) {
        await preferencias.remove(claveGuardado);
      } else {
        await preferencias.setString(claveGuardado, modo.name);
      }
    } on Object {
      // No se pudo guardar: la elección vale mientras la app siga abierta.
    }
  }
}

/// Botón de la barra para elegir el tema: del dispositivo, claro u oscuro.
class BotonTema extends StatelessWidget {
  const BotonTema({super.key});

  static const _opciones = [
    (ThemeMode.system, 'Como el dispositivo', Icons.brightness_auto_outlined),
    (ThemeMode.light, 'Claro', Icons.light_mode_outlined),
    (ThemeMode.dark, 'Oscuro', Icons.dark_mode_outlined),
  ];

  @override
  Widget build(BuildContext context) {
    final tema = context.watch<PreferenciaTema>();
    final actual = _opciones.firstWhere((o) => o.$1 == tema.modo);
    return PopupMenuButton<ThemeMode>(
      tooltip: 'Tema: ${actual.$2.toLowerCase()}',
      icon: Icon(actual.$3),
      initialValue: tema.modo,
      onSelected: tema.elegir,
      itemBuilder: (_) => [
        for (final (modo, nombre, _) in _opciones)
          CheckedPopupMenuItem(value: modo, checked: modo == tema.modo, child: Text(nombre)),
      ],
    );
  }
}
