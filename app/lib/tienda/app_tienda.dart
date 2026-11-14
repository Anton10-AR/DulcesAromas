import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../comun/api.dart';
import '../comun/preferencia_tema.dart';
import '../comun/sesion.dart';
import '../comun/tema.dart';
import '../comun/widgets.dart';
import 'carrito.dart';
import 'marco_tienda.dart';
import 'recuerdo_portada.dart';
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
    // Las rutas cuelgan del catálogo (y el pago, del pedido), así que al llegar a una con go() queda debajo la
    // pantalla de la que sale: siempre hay flecha para volver y el botón Atrás no cierra la app.
    routes: [
      // El marco pone la barra inferior; adentro, las rutas siguen colgando del catálogo como antes.
      ShellRoute(
        builder: (context, estado, hijo) => MarcoTienda(ruta: estado.matchedLocation, child: hijo),
        routes: [
          GoRoute(
            path: '/',
            builder: (_, _) => const CatalogoPantalla(),
            routes: [
              GoRoute(
                path: 'producto/:id',
                builder: (_, e) => ProductoPantalla(id: int.parse(e.pathParameters['id']!)),
              ),
              GoRoute(path: 'carrito', builder: (_, _) => const CarritoPantalla()),
              GoRoute(
                path: 'ingresar',
                builder: (_, e) => IngresarPantalla(volver: e.uri.queryParameters['volver']),
              ),
              GoRoute(
                path: 'registro',
                builder: (_, e) => RegistroPantalla(volver: e.uri.queryParameters['volver']),
              ),
              GoRoute(
                path: 'recuperar',
                builder: (_, e) => RecuperarPantalla(token: e.uri.queryParameters['token']),
              ),
              GoRoute(path: 'confirmar', builder: (_, _) => const ConfirmarPantalla()),
              GoRoute(
                path: 'pedidos',
                builder: (_, _) => const MisPedidosPantalla(),
                routes: [
                  GoRoute(
                    path: ':id',
                    builder: (_, e) => PedidoPantalla(id: int.parse(e.pathParameters['id']!)),
                    routes: [
                      GoRoute(
                        path: 'pagar',
                        builder: (_, e) => PagarPantalla(pedidoId: int.parse(e.pathParameters['id']!)),
                      ),
                    ],
                  ),
                ],
              ),
              GoRoute(path: 'perfil', builder: (_, _) => const PerfilPantalla()),
            ],
          ),
        ],
      ),
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
  late final PreferenciaTema _tema = PreferenciaTema(claveGuardado: 'tema_tienda');
  late final RecuerdoPortada _portada = RecuerdoPortada();
  late final GoRouter _rutas = rutasTienda(_sesion);

  /// Las pantallas esperan a la sesión y al carrito guardados. Si no, al abrir o recargar una ruta como /pedidos o
  /// /carrito en la web, la pantalla pediría datos sin token o mostraría el carrito vacío.
  late final Future<void> _listo = Future.wait([
    _sesion.restaurar(),
    _carrito.cargar(),
    _tema.cargar(),
    _portada.cargar(),
  ]);

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        Provider.value(value: widget.api),
        ChangeNotifierProvider.value(value: _sesion),
        ChangeNotifierProvider.value(value: _carrito),
        ChangeNotifierProvider.value(value: _tema),
        Provider.value(value: _portada),
      ],
      child: FutureBuilder(
        future: _listo,
        builder: (context, estado) => estado.connectionState != ConnectionState.done
            ? const Arrancando()
            : MaterialApp.router(
                title: 'Dulces Aromas',
                debugShowCheckedModeBanner: false,
                theme: temaClaro(),
                darkTheme: temaOscuro(),
                // El del dispositivo, salvo que la persona elija otro con el botón de la barra.
                themeMode: context.watch<PreferenciaTema>().modo,
                routerConfig: _rutas,
                // RNF20: marco visible alrededor del control con foco al usar el teclado.
                builder: (context, hijo) => MenosMovimiento(child: IndicadorDeFoco(child: hijo!)),
              ),
      ),
    );
  }
}
