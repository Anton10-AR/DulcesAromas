import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../comun/marca.dart';
import '../comun/sesion.dart';
import '../comun/widgets.dart';
import 'modelos_panel.dart';

/// La cuenta con la que se ingresó, arriba a la derecha: iniciales, nombre y rol. Al tocarla se abre un menú con
/// el perfil y el cierre de sesión (HU02, HU17).
class MenuUsuario extends StatelessWidget {
  const MenuUsuario({super.key});

  /// Desde este ancho se ven el nombre y el rol junto al círculo; más angosto, solo el círculo.
  static const anchoConNombre = 600.0;

  @override
  Widget build(BuildContext context) {
    final usuario = context.watch<Sesion>().usuario;
    if (usuario == null) return const SizedBox.shrink();
    final textos = Theme.of(context).textTheme;
    final rol = nombresRol[usuario.rol] ?? '';
    final primerNombre = usuario.nombre.trim().split(RegExp(r'\s+')).first;
    final conNombre = MediaQuery.sizeOf(context).width >= anchoConNombre;

    final circulo = CirculoIniciales(usuario.nombre);

    return PopupMenuButton<String>(
      tooltip: 'Cuenta de ${usuario.nombre}',
      position: PopupMenuPosition.under,
      onSelected: (opcion) async {
        if (opcion == 'perfil') {
          context.go('/perfil');
        } else if (opcion == 'salir') {
          // Al cerrar la sesión, el router lleva solo a la pantalla de ingreso.
          await context.read<Sesion>().cerrar();
        }
      },
      itemBuilder: (_) => [
        PopupMenuItem<String>(
          enabled: false,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(usuario.nombre, style: textos.titleSmall),
              Text(usuario.correo, style: textos.bodySmall),
              if (rol.isNotEmpty) ...[const SizedBox(height: 6), Etiqueta(rol)],
            ],
          ),
        ),
        const PopupMenuDivider(),
        const PopupMenuItem(
          value: 'perfil',
          child: ListTile(
            leading: Icon(Icons.person_outline),
            title: Text('Mi perfil'),
            contentPadding: EdgeInsets.zero,
          ),
        ),
        const PopupMenuItem(
          value: 'salir',
          child: ListTile(leading: Icon(Icons.logout), title: Text('Cerrar sesión'), contentPadding: EdgeInsets.zero),
        ),
      ],
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            circulo,
            if (conNombre) ...[
              const SizedBox(width: 8),
              Text(primerNombre, style: textos.titleSmall),
              if (rol.isNotEmpty) ...[const SizedBox(width: 8), Etiqueta(rol)],
            ],
            const Icon(Icons.arrow_drop_down),
          ],
        ),
      ),
    );
  }
}
