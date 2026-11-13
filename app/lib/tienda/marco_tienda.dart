import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../comun/formato.dart';
import '../comun/marca.dart';
import '../comun/sesion.dart';
import 'carrito.dart';

/// Hasta este ancho la Tienda lleva la barra inferior; más ancho, el carrito y la cuenta van en la barra de arriba.
const anchoBarraInferior = 720.0;

/// Las secciones de la barra inferior y las rutas que caen en cada una.
const _secciones = [
  ('/', ['/', '/producto']),
  ('/carrito', ['/carrito', '/confirmar']),
  ('/pedidos', ['/pedidos']),
  ('/perfil', ['/perfil']),
];

/// Pantallas sin barra inferior: ingresar, registrarse y recuperar la contraseña, para no distraer del formulario.
const _sinBarra = ['/ingresar', '/registro', '/recuperar'];

/// Si la Tienda usa la barra inferior en este ancho.
bool conBarraInferior(BuildContext context) => MediaQuery.sizeOf(context).width < anchoBarraInferior;

/// Marco de la Tienda: en el teléfono, una barra inferior siempre visible con el catálogo, el carrito, los pedidos y
/// la cuenta, para ir de una sección a otra sin buscar la flecha de arriba (RNF01, RNF12).
class MarcoTienda extends StatelessWidget {
  const MarcoTienda({super.key, required this.ruta, required this.child});

  final String ruta;
  final Widget child;

  int? get _actual {
    for (final (i, (_, prefijos)) in _secciones.indexed) {
      final coincide = prefijos.any((p) => p == '/' ? ruta == '/' : ruta.startsWith(p));
      if (coincide) return i;
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    if (!conBarraInferior(context) || _sinBarra.any(ruta.startsWith)) return child;
    final carrito = context.watch<Carrito>();
    final usuario = context.watch<Sesion>().usuario;
    final actual = _actual;
    return Scaffold(
      body: child,
      // Una línea suave la separa del contenido, que tiene el mismo fondo.
      bottomNavigationBar: DecoratedBox(
        position: DecorationPosition.foreground,
        decoration: BoxDecoration(
          border: Border(top: BorderSide(color: Theme.of(context).colorScheme.outlineVariant)),
        ),
        child: NavigationBar(
          selectedIndex: actual ?? 0,
          // Fuera de las cuatro secciones (no debería pasar), ninguna se marca como elegida.
          indicatorColor: actual == null ? Colors.transparent : null,
          labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
          onDestinationSelected: (i) => context.go(_secciones[i].$1),
          destinations: [
            const NavigationDestination(
              icon: Icon(Icons.storefront_outlined),
              selectedIcon: Icon(Icons.storefront),
              label: 'Catálogo',
            ),
            NavigationDestination(
              tooltip: 'Carrito, ${cantidadProductos(carrito.unidades)}',
              icon: Badge(
                isLabelVisible: carrito.unidades > 0,
                label: Text('${carrito.unidades}'),
                child: const Icon(Icons.shopping_bag_outlined),
              ),
              selectedIcon: Badge(
                isLabelVisible: carrito.unidades > 0,
                label: Text('${carrito.unidades}'),
                child: const Icon(Icons.shopping_bag),
              ),
              label: 'Carrito',
            ),
            const NavigationDestination(
              icon: Icon(Icons.receipt_long_outlined),
              selectedIcon: Icon(Icons.receipt_long),
              label: 'Mis pedidos',
            ),
            if (usuario != null)
              NavigationDestination(tooltip: 'Mi cuenta', icon: CirculoIniciales(usuario.nombre), label: 'Mi cuenta')
            else
              const NavigationDestination(
                icon: Icon(Icons.person_outline),
                selectedIcon: Icon(Icons.person),
                label: 'Ingresar',
              ),
          ],
        ),
      ),
    );
  }
}
