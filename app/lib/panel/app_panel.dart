import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../comun/api.dart';
import '../comun/perfil.dart';
import '../comun/sesion.dart';
import '../comun/tema.dart';
import '../comun/widgets.dart';
import 'pantallas/asistido_panel.dart';
import 'pantallas/catalogo_panel.dart';
import 'pantallas/configuracion_panel.dart';
import 'pantallas/entregas_panel.dart';
import 'pantallas/ingresar_panel.dart';
import 'pantallas/inicio_panel.dart';
import 'pantallas/inventario_panel.dart';
import 'pantallas/mis_entregas_panel.dart';
import 'pantallas/pagos_panel.dart';
import 'pantallas/pedido_panel.dart';
import 'pantallas/pedidos_panel.dart';
import 'pantallas/producto_panel.dart';
import 'pantallas/reportes_panel.dart';
import 'pantallas/seguridad_panel.dart';

/// Una sección del menú del Panel y los permisos que la muestran. Mostrarla o no es comodidad: el servidor
/// valida igual cada acción (HU06, RNF07).
class Seccion {
  const Seccion(this.ruta, this.titulo, this.icono, this.permisos);

  final String ruta;
  final String titulo;
  final IconData icono;
  final List<String> permisos;

  bool visiblePara(Usuario usuario) => permisos.isEmpty || permisos.any(usuario.puede);
}

const secciones = [
  Seccion('/', 'Inicio', Icons.home_outlined, []),
  Seccion('/pedidos', 'Pedidos', Icons.receipt_long_outlined, [
    'PEDIDOS_CONSULTAR',
    'PEDIDOS_PREPARAR',
    'DISTRIBUCION_ASIGNAR',
  ]),
  Seccion('/pagos', 'Verificar pagos', Icons.fact_check_outlined, ['COBRANZA_VERIFICAR']),
  Seccion('/entregas', 'Entregas', Icons.local_shipping_outlined, ['DISTRIBUCION_ASIGNAR']),
  Seccion('/mis-entregas', 'Mis entregas', Icons.delivery_dining_outlined, ['DISTRIBUCION_ENTREGAS']),
  Seccion('/catalogo', 'Catálogo', Icons.cake_outlined, ['CATALOGO_GESTIONAR', 'CATALOGO_DISPONIBILIDAD']),
  Seccion('/inventario', 'Inventario', Icons.inventory_2_outlined, ['INVENTARIO_CONSULTAR', 'INVENTARIO_MOVIMIENTOS']),
  Seccion('/reportes', 'Reportes', Icons.bar_chart_outlined, ['REPORTES']),
  Seccion('/seguridad', 'Seguridad', Icons.admin_panel_settings_outlined, ['SEGURIDAD']),
  Seccion('/configuracion', 'Configuración', Icons.settings_outlined, ['CONFIGURACION']),
  Seccion('/perfil', 'Mi perfil', Icons.person_outline, []),
];

GoRouter rutasPanel(Sesion sesion) {
  return GoRouter(
    refreshListenable: sesion,
    redirect: (context, estado) {
      if (sesion.cargando) return null;
      final enIngreso = estado.matchedLocation == '/ingresar';
      if (!sesion.iniciada) return enIngreso ? null : '/ingresar';
      if (enIngreso) return '/';
      // CU12: tras un restablecimiento, primero hay que cambiar la contraseña.
      if (sesion.debeCambiarContrasena && estado.matchedLocation != '/perfil') return '/perfil';
      return null;
    },
    routes: [
      GoRoute(path: '/ingresar', builder: (_, _) => const IngresarPanel()),
      ShellRoute(
        builder: (context, estado, hijo) => MarcoPanel(ruta: estado.matchedLocation, child: hijo),
        routes: [
          GoRoute(path: '/', builder: (_, _) => const InicioPanel()),
          GoRoute(path: '/pedidos', builder: (_, _) => const PedidosPanel()),
          // Antes que /pedidos/:id, que tomaría «asistido» como un número.
          GoRoute(path: '/pedidos/asistido', builder: (_, _) => const AsistidoPanel()),
          GoRoute(
            path: '/pedidos/:id',
            builder: (_, e) => PedidoPanel(id: int.parse(e.pathParameters['id']!)),
          ),
          GoRoute(path: '/pagos', builder: (_, _) => const PagosPanel()),
          GoRoute(path: '/entregas', builder: (_, _) => const EntregasPanel()),
          GoRoute(path: '/mis-entregas', builder: (_, _) => const MisEntregasPanel()),
          GoRoute(path: '/catalogo', builder: (_, _) => const CatalogoPanel()),
          GoRoute(path: '/catalogo/nuevo', builder: (_, _) => const ProductoPanel()),
          GoRoute(
            path: '/catalogo/:id',
            builder: (_, e) => ProductoPanel(id: int.parse(e.pathParameters['id']!)),
          ),
          GoRoute(path: '/inventario', builder: (_, _) => const InventarioPanel()),
          GoRoute(path: '/reportes', builder: (_, _) => const ReportesPanel()),
          GoRoute(path: '/seguridad', builder: (_, _) => const SeguridadPanel()),
          GoRoute(path: '/configuracion', builder: (_, _) => const ConfiguracionPanel()),
          GoRoute(
            path: '/perfil',
            builder: (context, _) => PantallaPerfil(conDireccion: false, alCerrarSesion: () => context.go('/ingresar')),
          ),
        ],
      ),
    ],
  );
}

/// El Panel del personal (web).
class AppPanel extends StatefulWidget {
  const AppPanel({super.key, required this.api});

  final Api api;

  @override
  State<AppPanel> createState() => _AppPanelState();
}

class _AppPanelState extends State<AppPanel> {
  late final Sesion _sesion = Sesion(widget.api, claveGuardado: 'sesion_panel');
  late final GoRouter _rutas = rutasPanel(_sesion);

  /// Las pantallas esperan a la sesión guardada, para no pedir datos sin token al recargar una ruta.
  late final Future<void> _listo = _sesion.restaurar();

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        Provider.value(value: widget.api),
        ChangeNotifierProvider.value(value: _sesion),
      ],
      child: FutureBuilder(
        future: _listo,
        builder: (context, estado) => estado.connectionState != ConnectionState.done
            ? const Arrancando()
            : MaterialApp.router(
                title: 'Dulces Aromas · Panel',
                debugShowCheckedModeBanner: false,
                theme: temaClaro(),
                darkTheme: temaOscuro(),
                themeMode: ThemeMode.system,
                routerConfig: _rutas,
              ),
      ),
    );
  }
}

/// Barra lateral con las secciones que el rol permite; en pantallas angostas pasa a un menú (RNF12).
class MarcoPanel extends StatelessWidget {
  const MarcoPanel({super.key, required this.ruta, required this.child});

  final String ruta;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final usuario = context.watch<Sesion>().usuario;
    if (usuario == null) return const SizedBox.shrink();
    final visibles = secciones.where((s) => s.visiblePara(usuario)).toList();
    final actual = visibles.lastIndexWhere((s) => s.ruta == '/' ? ruta == '/' : ruta.startsWith(s.ruta));
    final titulo = actual >= 0 ? visibles[actual].titulo : 'Panel';
    final ancho = MediaQuery.sizeOf(context).width >= 900;

    void ir(int i) => context.go(visibles[i].ruta);

    if (ancho) {
      return Scaffold(
        body: Row(
          children: [
            NavigationRail(
              extended: true,
              minExtendedWidth: 220,
              selectedIndex: actual < 0 ? null : actual,
              onDestinationSelected: ir,
              leading: Padding(
                padding: const EdgeInsets.symmetric(vertical: 16),
                child: Text('Dulces Aromas', style: Theme.of(context).textTheme.titleLarge),
              ),
              destinations: [
                for (final s in visibles) NavigationRailDestination(icon: Icon(s.icono), label: Text(s.titulo)),
              ],
            ),
            const VerticalDivider(width: 1),
            Expanded(
              child: Scaffold(
                appBar: AppBar(title: Text(titulo)),
                body: child,
              ),
            ),
          ],
        ),
      );
    }
    return Scaffold(
      appBar: AppBar(title: Text(titulo)),
      drawer: NavigationDrawer(
        selectedIndex: actual < 0 ? null : actual,
        onDestinationSelected: (i) {
          Navigator.pop(context);
          ir(i);
        },
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: Text('Dulces Aromas', style: Theme.of(context).textTheme.titleLarge),
          ),
          for (final s in visibles) NavigationDrawerDestination(icon: Icon(s.icono), label: Text(s.titulo)),
        ],
      ),
      body: child,
    );
  }
}
