import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../comun/perfil.dart';
import 'comunes.dart';

class PerfilPantalla extends StatelessWidget {
  const PerfilPantalla({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Mi perfil'), actions: accionesTienda(context)),
      // HU02: al cerrar sesión vuelve al inicio, sin páginas privadas en el historial.
      body: PantallaPerfil(alCerrarSesion: () => context.go('/')),
    );
  }
}
