import 'dart:convert';

import 'package:dulces_aromas/comun/api.dart';
import 'package:dulces_aromas/comun/widgets.dart';
import 'package:dulces_aromas/panel/app_panel.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';

http.Response json(Object cuerpo, [int estado = 200]) =>
    http.Response(jsonEncode(cuerpo), estado, headers: {'content-type': 'application/json; charset=utf-8'});

/// Una API falsa en la que ingresa una cuenta con el tipo, el rol y los permisos indicados.
Api apiCon({
  required String tipo,
  String? rol,
  List<String> permisos = const [],
  bool sesionVencida = false,
  Map<String, Object> respuestas = const {},
}) {
  return Api(
    base: 'http://api',
    cliente: MockClient((solicitud) async {
      final ruta = solicitud.url.path;
      if (ruta == '/api/auth/login') {
        return json({
          'token': 't',
          'usuario': {
            'id': 1,
            'nombre': 'Luz',
            'correo': 'luz@dulces.bo',
            'tipo': tipo,
            'rol': rol,
            'permisos': permisos,
          },
          'debeCambiarContrasena': false,
        });
      }
      if (sesionVencida) return json({'codigo': 'SIN_SESION', 'mensaje': 'Inicia sesión para continuar.'}, 401);
      if (respuestas.containsKey(ruta)) {
        final respuesta = respuestas[ruta]!;
        return respuesta is http.Response ? respuesta : json(respuesta);
      }
      return switch (ruta) {
        '/api/auth/logout' => http.Response('', 204),
        _ => json([]),
      };
    }),
  );
}

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));

  Future<void> ingresar(WidgetTester tester, Api api) async {
    tester.view.physicalSize = const Size(1400, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(AppPanel(api: api));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Correo'), 'luz@dulces.bo');
    await tester.enterText(find.widgetWithText(TextField, 'Contraseña'), 'Secreta123');
    await tester.tap(find.widgetWithText(FilledButton, 'Ingresar'));
    await tester.pumpAndSettle();
  }

  testWidgets('HU06: el Vendedor ve pedidos, entregas y catálogo, pero no la verificación de pagos', (tester) async {
    await ingresar(
      tester,
      apiCon(
        tipo: 'PERSONAL',
        rol: 'VENDEDOR',
        permisos: [
          'INICIO_PENDIENTES',
          'CATALOGO_DISPONIBILIDAD',
          'PEDIDOS_CONSULTAR',
          'PEDIDOS_CANCELAR',
          'DISTRIBUCION_ASIGNAR',
        ],
      ),
    );

    expect(find.text('Hola, Luz'), findsOneWidget);
    for (final visible in ['Pedidos', 'Entregas', 'Catálogo', 'Mi perfil']) {
      expect(find.text(visible), findsWidgets, reason: visible);
    }
    expect(find.text('Verificar pagos'), findsNothing);
    expect(find.text('Mis entregas'), findsNothing);
  });

  testWidgets('RNF20: el primer Tab ofrece saltar al contenido, sin recorrer el menú', (tester) async {
    await ingresar(
      tester,
      apiCon(tipo: 'PERSONAL', rol: 'VENDEDOR', permisos: ['INICIO_PENDIENTES', 'PEDIDOS_CONSULTAR']),
    );
    double opacidad() => tester
        .widget<Opacity>(find.ancestor(of: find.text(SaltoAlContenido.texto), matching: find.byType(Opacity)))
        .opacity;
    expect(opacidad(), 0);

    await tester.sendKeyEvent(LogicalKeyboardKey.tab);
    await tester.pump();
    expect(opacidad(), 1);

    await tester.sendKeyEvent(LogicalKeyboardKey.enter);
    await tester.pump();
    expect(FocusManager.instance.primaryFocus?.debugLabel, 'contenido principal');
    expect(opacidad(), 0);

    // El siguiente Tab cae dentro del contenido: a la derecha del menú lateral y debajo de la barra.
    await tester.sendKeyEvent(LogicalKeyboardKey.tab);
    await tester.pump();
    final caja = FocusManager.instance.primaryFocus!.context!.findRenderObject()! as RenderBox;
    final posicion = caja.localToGlobal(Offset.zero);
    expect(posicion.dx, greaterThan(220));
    expect(posicion.dy, greaterThanOrEqualTo(56));
  });

  testWidgets('HU06: el Repartidor solo ve sus entregas', (tester) async {
    await ingresar(tester, apiCon(tipo: 'PERSONAL', rol: 'REPARTIDOR', permisos: ['DISTRIBUCION_ENTREGAS']));

    expect(find.text('Mis entregas'), findsOneWidget);
    for (final oculto in ['Pedidos', 'Verificar pagos', 'Entregas', 'Catálogo']) {
      expect(find.text(oculto), findsNothing, reason: oculto);
    }
  });

  testWidgets('una cuenta de cliente no entra al Panel', (tester) async {
    await ingresar(tester, apiCon(tipo: 'CLIENTE'));

    expect(find.text('Esta cuenta es de cliente. Usa la Tienda para pedir.'), findsOneWidget);
    expect(find.text('Hola, Luz'), findsNothing);
  });

  testWidgets('RNF06: si la sesión venció, vuelve al ingreso y lo explica', (tester) async {
    await ingresar(
      tester,
      apiCon(tipo: 'PERSONAL', rol: 'ADMINISTRADOR', permisos: ['PEDIDOS_PREPARAR'], sesionVencida: true),
    );

    expect(find.text('Tu sesión se cerró tras 30 minutos sin uso. Ingresa de nuevo.'), findsOneWidget);
  });

  const todosLosPermisos = [
    'INICIO_VENTAS',
    'INICIO_PENDIENTES',
    'CATALOGO_GESTIONAR',
    'CATALOGO_DISPONIBILIDAD',
    'INVENTARIO_CONSULTAR',
    'INVENTARIO_MOVIMIENTOS',
    'PEDIDOS_CONSULTAR',
    'PEDIDOS_CANCELAR',
    'PEDIDOS_PREPARAR',
    'COBRANZA_REGISTRAR',
    'COBRANZA_VERIFICAR',
    'DISTRIBUCION_ASIGNAR',
    'DISTRIBUCION_ENTREGAS',
    'REPORTES',
    'CONFIGURACION',
    'SEGURIDAD',
  ];

  testWidgets('HU06: el administrador ve también inventario, reportes, seguridad y configuración', (tester) async {
    await ingresar(tester, apiCon(tipo: 'PERSONAL', rol: 'ADMINISTRADOR', permisos: todosLosPermisos));

    for (final seccion in ['Inventario', 'Reportes', 'Seguridad', 'Configuración', 'Verificar pagos']) {
      expect(find.text(seccion), findsOneWidget, reason: seccion);
    }
  });

  testWidgets('RN04: Seguridad es solo del administrador y no se le quita', (tester) async {
    await ingresar(
      tester,
      apiCon(
        tipo: 'PERSONAL',
        rol: 'ADMINISTRADOR',
        permisos: todosLosPermisos,
        respuestas: {
          '/api/roles': [
            {
              'codigo': 'ADMINISTRADOR',
              'nombre': 'Administrador',
              'descripcion': 'Acceso total.',
              'permisos': ['SEGURIDAD', 'REPORTES'],
            },
            {
              'codigo': 'VENDEDOR',
              'nombre': 'Vendedor',
              'descripcion': 'Atiende pedidos.',
              'permisos': ['REPORTES'],
            },
          ],
          '/api/permisos': [
            {'codigo': 'SEGURIDAD', 'modulo': 'Seguridad', 'accion': 'Usuarios y permisos'},
            {'codigo': 'REPORTES', 'modulo': 'Reportes', 'accion': 'Reportes de pedidos y ventas'},
          ],
        },
      ),
    );
    await tester.tap(find.text('Seguridad'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Roles y permisos'));
    await tester.pumpAndSettle();

    final seguridad = tester.widgetList<CheckboxListTile>(
      find.widgetWithText(CheckboxListTile, 'Seguridad: Usuarios y permisos'),
    );
    final reportes = tester.widgetList<CheckboxListTile>(
      find.widgetWithText(CheckboxListTile, 'Reportes: Reportes de pedidos y ventas'),
    );
    expect(seguridad.map((c) => c.value), [true, false]);
    expect(seguridad.every((c) => c.onChanged == null), isTrue);
    expect(reportes.every((c) => c.onChanged != null), isTrue);
  });

  testWidgets('RN09: el inventario marca los insumos en su mínimo', (tester) async {
    await ingresar(
      tester,
      apiCon(
        tipo: 'PERSONAL',
        rol: 'VENDEDOR',
        permisos: ['INVENTARIO_CONSULTAR'],
        respuestas: {
          '/api/inventario': [
            {'id': 1, 'nombre': 'Harina', 'unidad': 'kg', 'stock': 3, 'minimo': 5, 'alerta': true},
            {'id': 2, 'nombre': 'Azúcar', 'unidad': 'kg', 'stock': 20, 'minimo': 5, 'alerta': false},
          ],
        },
      ),
    );
    await tester.tap(find.text('Inventario'));
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.warning_amber), findsOneWidget);
    expect(find.textContaining('Stock: 3 kg · Mínimo: 5 · En el mínimo'), findsOneWidget);
    // El Vendedor solo consulta: no ve el botón de movimientos.
    expect(find.text('Movimiento'), findsNothing);
  });

  testWidgets('al renombrar una categoría, el cuadro trae el nombre actual', (tester) async {
    await ingresar(
      tester,
      apiCon(
        tipo: 'PERSONAL',
        rol: 'ADMINISTRADOR',
        permisos: ['CATALOGO_GESTIONAR'],
        respuestas: {
          '/api/categorias': [
            {'id': 1, 'nombre': 'Tortas', 'activa': true, 'productosActivos': 2},
          ],
        },
      ),
    );
    await tester.tap(find.text('Catálogo'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Categorías'));
    await tester.pumpAndSettle();

    await tester.tap(find.byTooltip('Renombrar'));
    await tester.pumpAndSettle();

    expect(find.widgetWithText(TextField, 'Tortas'), findsOneWidget);
  });

  testWidgets('CU08: un pedido con recojo muestra al cliente y explica que no lleva repartidor', (tester) async {
    await ingresar(
      tester,
      apiCon(
        tipo: 'PERSONAL',
        rol: 'VENDEDOR',
        permisos: ['PEDIDOS_CONSULTAR', 'DISTRIBUCION_ASIGNAR'],
        respuestas: {
          '/api/entregas/5': json({
            'codigo': 'SIN_REPARTIDOR',
            'mensaje': 'Este pedido todavía no tiene repartidor asignado.',
          }, 409),
          '/api/pedidos/5': {
            'id': 5,
            'cliente': 'Lucía Torrez',
            'estado': 'LISTO',
            'tipoEntrega': 'RECOJO',
            'direccion': null,
            'telefono': '70012345',
            'horarioEntrega': '2026-11-03T14:00:00Z',
            'lineas': [],
            'costoEnvio': 0,
            'total': 120,
            'fechaCreacion': '2026-11-02T14:00:00Z',
            'asistido': false,
            'motivoCancelacion': null,
            'historial': [],
          },
        },
      ),
    );
    final router = tester.widget<MaterialApp>(find.byType(MaterialApp)).routerConfig!;
    (router as GoRouter).go('/pedidos/5');
    await tester.pumpAndSettle();

    expect(find.text('Cliente: Lucía Torrez'), findsOneWidget);
    expect(find.textContaining('Recojo en tienda: no lleva repartidor.'), findsOneWidget);
    expect(find.text('El cliente lo retiró'), findsOneWidget);
    expect(find.text('Elegir repartidor'), findsNothing);

    // La flecha vuelve a la lista de pedidos.
    await tester.tap(find.byType(BackButton));
    await tester.pumpAndSettle();
    expect(router.routerDelegate.currentConfiguration.uri.path, '/pedidos');
  });
}
