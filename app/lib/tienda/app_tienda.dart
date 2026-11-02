import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../comun/api.dart';
import '../comun/sesion.dart';
import '../comun/tema.dart';
import 'carrito.dart';
import 'pantallas/carrito_pantalla.dart';
import 'pantallas/catalogo_pantalla.dart';
import 'pantallas/confirmar_pantalla.dart';
import 'pantallas/ingresar_pantalla.dart';
import 'pantallas/mis_pedidos_pantalla.dart';
import 'pantallas/pagar_pantalla.dart';
import 'pantallas/pedido_pantalla.dart';
import 'pantallas/perfil_pantalla.dart';
import 'pantallas/producto_pantalla.dart';
import 'pantallas/recuperar_pantalla.dart';
import 'pantallas/registro_pantalla.dart';

/// Rutas que exigen sesión de cliente. Sin ella, se pide iniciar sesión y luego se vuelve aquí (RN15).
bool _esPrivada(String ruta) =>
    ruta.startsWith('/confirmar') || ruta.startsWith('/pedidos') || ruta.startsWith('/perfil');

GoRouter rutasTienda(Sesion sesion) {
  return GoRouter(
    refreshListenable: sesion,
    redirect: (context, estado) {
      final ruta = estado.matchedLocation;
      if (sesion.cargando) return null;
      if (!sesion.iniciada && _esPrivada(ruta)) {
        // HU02: sin sesión, ni el botón Atrás muestra una página privada.
        return Uri(path: '/ingresar', queryParameters: {'volver': estado.uri.toString()}).toString();
      }
      return null;
    },
    routes: [
      GoRoute(path: '/', builder: (_, _) => const CatalogoPantalla()),
      GoRoute(
        path: '/producto/:id',
        builder: (_, e) => ProductoPantalla(id: int.parse(e.pathParameters['id']!)),
      ),
      GoRoute(path: '/carrito', builder: (_, _) => const CarritoPantalla()),
      GoRoute(
        path: '/ingresar',
        builder: (_, e) => IngresarPantalla(volver: e.uri.queryParameters['volver']),
      ),
      GoRoute(
        path: '/registro',
        builder: (_, e) => RegistroPantalla(volver: e.uri.queryParameters['volver']),
      ),
      GoRoute(
        path: '/recuperar',
        builder: (_, e) => RecuperarPantalla(token: e.uri.queryParameters['token']),
      ),
      GoRoute(path: '/confirmar', builder: (_, _) => const ConfirmarPantalla()),
      GoRoute(path: '/pedidos', builder: (_, _) => const MisPedidosPantalla()),
      GoRoute(
        path: '/pedidos/:id',
        builder: (_, e) => PedidoPantalla(id: int.parse(e.pathParameters['id']!)),
      ),
      GoRoute(
        path: '/pedidos/:id/pagar',
        builder: (_, e) => PagarPantalla(pedidoId: int.parse(e.pathParameters['id']!)),
      ),
      GoRoute(path: '/perfil', builder: (_, _) => const PerfilPantalla()),
    ],
  );
}

/// La Tienda de los clientes (Flutter, móvil).
class AppTienda extends StatefulWidget {
  const AppTienda({super.key, required this.api});

  final Api api;

  @override
  State<AppTienda> createState() => _AppTiendaState();
}

class _AppTiendaState extends State<AppTienda> {
  late final Sesion _sesion = Sesion(widget.api, claveGuardado: 'sesion_tienda');
  late final Carrito _carrito = Carrito();
  late final GoRouter _rutas = rutasTienda(_sesion);

  @override
  void initState() {
    super.initState();
    _sesion.restaurar();
    _carrito.cargar();
  }

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        Provider.value(value: widget.api),
        ChangeNotifierProvider.value(value: _sesion),
        ChangeNotifierProvider.value(value: _carrito),
      ],
      child: MaterialApp.router(
        title: 'Dulces Aromas',
        debugShowCheckedModeBanner: false,
        theme: temaClaro(),
        darkTheme: temaOscuro(),
        // Sigue la preferencia del dispositivo.
        themeMode: ThemeMode.system,
        routerConfig: _rutas,
      ),
    );
  }
}
