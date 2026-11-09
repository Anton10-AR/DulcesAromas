import 'dart:convert';

import 'package:dulces_aromas/comun/api.dart';
import 'package:dulces_aromas/tienda/app_tienda.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Una API falsa con un catálogo de dos productos. [respuestas] cambia lo que contesta una ruta.
Api apiFalsa({Map<String, http.Response Function(http.Request)> respuestas = const {}}) {
  Map<String, dynamic> producto(int id, String nombre, {bool disponible = true}) => {
    'id': id,
    'nombre': nombre,
    'descripcion': 'Hecha en casa',
    'precio': 120.0,
    'categoria': {'id': 1, 'nombre': 'Tortas'},
    'activo': true,
    'disponible': disponible,
    'personalizable': false,
    'imagen': null,
    'opciones': [],
  };
  http.Response json(Object cuerpo, [int estado = 200]) =>
      http.Response(jsonEncode(cuerpo), estado, headers: {'content-type': 'application/json; charset=utf-8'});
  return Api(
    base: 'http://api',
    cliente: MockClient((solicitud) async {
      final ruta = solicitud.url.path;
      if (respuestas.containsKey(ruta)) return respuestas[ruta]!(solicitud);
      return switch (ruta) {
        '/api/catalogo/categorias' => json([
          {'id': 1, 'nombre': 'Tortas'},
        ]),
        '/api/catalogo/productos' => json([
          producto(1, 'Torta de chocolate'),
          producto(2, 'Torta de fresa', disponible: false),
        ]),
        '/api/catalogo/productos/1' => json(producto(1, 'Torta de chocolate')),
        '/api/catalogo/productos/2' => json(producto(2, 'Torta de fresa', disponible: false)),
        _ => json({'codigo': 'NO_ENCONTRADO', 'mensaje': 'No existe.'}, 404),
      };
    }),
  );
}

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));

  Future<void> abrir(WidgetTester tester, Api api) async {
    tester.view.physicalSize = const Size(1080, 2400);
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(AppTienda(api: api));
    await tester.pumpAndSettle();
  }

  testWidgets('RN15: el catálogo se ve sin iniciar sesión, con precio y disponibilidad', (tester) async {
    await abrir(tester, apiFalsa());

    expect(find.text('Torta de chocolate'), findsOneWidget);
    expect(find.text('Bs 120.00'), findsNWidgets(2));
    expect(find.text('No disponible'), findsOneWidget);
    expect(find.text('Ingresar'), findsOneWidget);
  });

  testWidgets('la portada muestra el logo y «Ver catálogo» baja a los productos', (tester) async {
    await abrir(tester, apiFalsa());

    expect(find.bySemanticsLabel('Dulces Aromas'), findsOneWidget);
    // Mientras se ve la portada, la barra no repite el nombre.
    expect(find.descendant(of: find.byType(AppBar), matching: find.text('Dulces Aromas')), findsNothing);

    await tester.tap(find.text('Ver catálogo'));
    await tester.pumpAndSettle();

    expect(find.descendant(of: find.byType(AppBar), matching: find.text('Dulces Aromas')), findsOneWidget);
    expect(tester.getRect(find.text('Buscar por nombre')).top, lessThan(200));
  });

  testWidgets('el botón de tema cambia a oscuro y la elección se guarda', (tester) async {
    await abrir(tester, apiFalsa());
    Brightness brillo() => Theme.of(tester.element(find.text('Torta de chocolate'))).brightness;
    expect(brillo(), Brightness.light);

    await tester.tap(find.byTooltip('Tema: como el dispositivo'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Oscuro'));
    await tester.pumpAndSettle();

    expect(brillo(), Brightness.dark);
    expect(find.byTooltip('Tema: oscuro'), findsOneWidget);
    expect((await SharedPreferences.getInstance()).getString('tema_tienda'), 'dark');
  });

  testWidgets('HU07: un producto no disponible no se puede agregar al carrito', (tester) async {
    await abrir(tester, apiFalsa());

    await tester.tap(find.text('Torta de fresa'));
    await tester.pumpAndSettle();

    expect(find.text('Este producto no está disponible por ahora.'), findsOneWidget);
    final boton = tester.widget<ButtonStyleButton>(
      find.ancestor(
        of: find.text('Agregar al carrito'),
        matching: find.byWidgetPredicate((w) => w is ButtonStyleButton),
      ),
    );
    expect(boton.onPressed, isNull);
  });

  testWidgets('agregar un producto suma al carrito', (tester) async {
    await abrir(tester, apiFalsa());

    await tester.tap(find.text('Torta de chocolate'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Agregar al carrito'));
    await tester.pumpAndSettle();

    expect(find.byTooltip('Carrito, 1 producto'), findsOneWidget);
  });

  testWidgets('RN15: confirmar el pedido sin sesión pide ingresar y conserva el carrito', (tester) async {
    await abrir(tester, apiFalsa());

    final router = tester.widget<MaterialApp>(find.byType(MaterialApp)).routerConfig!;
    (router as GoRouter).go('/confirmar');
    await tester.pumpAndSettle();

    expect(find.text('Para hacer tu pedido, ingresa o crea una cuenta. Tu carrito se conserva.'), findsOneWidget);
  });

  testWidgets('RNF01: el error de un campo aparece junto al campo', (tester) async {
    final api = apiFalsa(
      respuestas: {
        '/api/clientes': (_) => http.Response(
          jsonEncode({
            'codigo': 'DATOS_INVALIDOS',
            'mensaje': 'Revisa los campos marcados.',
            'campos': {'contrasena': 'Usa al menos 8 caracteres, con al menos una letra y un número.'},
          }),
          400,
          headers: {'content-type': 'application/json; charset=utf-8'},
        ),
      },
    );
    await abrir(tester, api);
    await tester.tap(find.text('Ingresar'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Crear una cuenta'));
    await tester.pumpAndSettle();

    await tester.enterText(find.widgetWithText(TextField, 'Contraseña'), 'corta');
    await tester.tap(find.text('Crear cuenta'));
    await tester.pumpAndSettle();

    expect(find.text('Usa al menos 8 caracteres, con al menos una letra y un número.'), findsOneWidget);
  });

  testWidgets('HU15: si el correo ya existe, ofrece iniciar sesión o recuperar la contraseña', (tester) async {
    final api = apiFalsa(
      respuestas: {
        '/api/clientes': (_) => http.Response(
          jsonEncode({
            'codigo': 'CORREO_REGISTRADO',
            'mensaje': 'Ya existe una cuenta con este correo. Puedes iniciar sesión o recuperar tu contraseña.',
          }),
          409,
          headers: {'content-type': 'application/json; charset=utf-8'},
        ),
      },
    );
    await abrir(tester, api);
    await tester.tap(find.text('Ingresar'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Crear una cuenta'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Crear cuenta'));
    await tester.pumpAndSettle();

    expect(find.text('Iniciar sesión'), findsOneWidget);
    expect(find.text('Recuperar contraseña'), findsOneWidget);
  });

  testWidgets('HU10: el cliente elige recojo o delivery; no viene uno marcado', (tester) async {
    SharedPreferences.setMockInitialValues({
      'sesion_tienda': 't',
      'carrito': jsonEncode([
        {'productoId': 1, 'nombre': 'Torta de chocolate', 'cantidad': 1, 'opciones': [], 'nombresOpciones': []},
      ]),
    });
    http.Response respuesta(Object cuerpo, [int estado = 200]) =>
        http.Response(jsonEncode(cuerpo), estado, headers: {'content-type': 'application/json; charset=utf-8'});
    final api = apiFalsa(
      respuestas: {
        '/api/auth/yo': (_) => respuesta({
          'id': 9,
          'nombre': 'Ana',
          'correo': 'ana@correo.bo',
          'tipo': 'CLIENTE',
          'rol': null,
          'permisos': [],
        }),
        '/api/pedidos/cotizacion': (_) =>
            respuesta({'lineas': [], 'costoEnvio': 0, 'total': 120, 'confirmable': true, 'noDisponibles': []}),
        '/api/pedidos': (_) => respuesta({
          'codigo': 'DATOS_INVALIDOS',
          'mensaje': 'Revisa los datos de entrega.',
          'campos': {'tipoEntrega': 'Elige recojo en tienda o delivery.'},
        }, 400),
      },
    );
    await abrir(tester, api);
    final router = tester.widget<MaterialApp>(find.byType(MaterialApp)).routerConfig!;
    (router as GoRouter).go('/confirmar');
    await tester.pumpAndSettle();

    final opciones = tester.widget<SegmentedButton<String>>(find.byType(SegmentedButton<String>));
    expect(opciones.selected, isEmpty);
    expect(find.text('El envío se suma si eliges delivery.'), findsOneWidget);

    await tester.tap(find.widgetWithText(FilledButton, 'Confirmar pedido'));
    await tester.pumpAndSettle();

    expect(find.text('Elige recojo en tienda o delivery.'), findsOneWidget);
  });

  testWidgets('desde el detalle de un pedido se puede volver, en vez de cerrar la app', (tester) async {
    SharedPreferences.setMockInitialValues({'sesion_tienda': 't'});
    http.Response respuesta(Object cuerpo) =>
        http.Response(jsonEncode(cuerpo), 200, headers: {'content-type': 'application/json; charset=utf-8'});
    final api = apiFalsa(
      respuestas: {
        '/api/auth/yo': (_) => respuesta({
          'id': 9,
          'nombre': 'Ana',
          'correo': 'ana@correo.bo',
          'tipo': 'CLIENTE',
          'rol': null,
          'permisos': [],
        }),
        '/api/pedidos/5': (_) => respuesta({
          'id': 5,
          'estado': 'ENTREGADO',
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
        }),
        '/api/pagos': (_) => respuesta([]),
        '/api/pedidos/mios': (_) => respuesta([]),
        '/api/pagos/mios': (_) => respuesta([]),
      },
    );
    await abrir(tester, api);
    final router = tester.widget<MaterialApp>(find.byType(MaterialApp)).routerConfig! as GoRouter;
    router.go('/pedidos/5');
    await tester.pumpAndSettle();

    await tester.tap(find.byType(BackButton));
    await tester.pumpAndSettle();
    expect(find.text('Mis pedidos'), findsOneWidget);

    await tester.tap(find.byType(BackButton));
    await tester.pumpAndSettle();
    expect(find.text('Dulces Aromas'), findsOneWidget);
  });

  testWidgets('el catálogo se actualiza al deslizar hacia abajo y al volver de un producto', (tester) async {
    var consultas = 0;
    final api = apiFalsa(
      respuestas: {
        '/api/catalogo/productos': (solicitud) {
          consultas++;
          return http.Response(
            jsonEncode([
              {
                'id': 1,
                'nombre': 'Torta de chocolate',
                'descripcion': 'Hecha en casa',
                'precio': 120.0,
                'categoria': {'id': 1, 'nombre': 'Tortas'},
                'activo': true,
                'disponible': true,
                'personalizable': false,
                'imagen': null,
                'opciones': [],
              },
            ]),
            200,
            headers: {'content-type': 'application/json; charset=utf-8'},
          );
        },
      },
    );
    await abrir(tester, api);
    expect(consultas, 1);

    await tester.fling(find.byType(CustomScrollView), const Offset(0, 400), 1000);
    await tester.pumpAndSettle();
    expect(consultas, 2);

    await tester.tap(find.text('Torta de chocolate'));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(BackButton));
    await tester.pumpAndSettle();
    expect(consultas, 3);
  });

  /// Abre la Tienda directo en [ruta], como al recargar esa página en el navegador.
  Future<void> abrirEn(WidgetTester tester, Api api, String ruta) async {
    tester.binding.platformDispatcher.defaultRouteNameTestValue = ruta;
    addTearDown(tester.binding.platformDispatcher.clearDefaultRouteNameTestValue);
    await abrir(tester, api);
  }

  http.Response json(Object cuerpo, [int estado = 200]) =>
      http.Response(jsonEncode(cuerpo), estado, headers: {'content-type': 'application/json; charset=utf-8'});

  testWidgets('al recargar Mis pedidos, la sesión guardada se conserva', (tester) async {
    SharedPreferences.setMockInitialValues({'sesion_tienda': 'abc'});
    // Como la API real: sin el token de la sesión, responde 401.
    http.Response conSesion(http.Request s, Object cuerpo) => s.headers['Authorization'] == 'Bearer abc'
        ? json(cuerpo)
        : json({'codigo': 'SIN_SESION', 'mensaje': 'Inicia sesión para continuar.'}, 401);
    final api = apiFalsa(
      respuestas: {
        '/api/auth/yo': (s) => conSesion(s, {
          'id': 6,
          'nombre': 'Lucía',
          'correo': 'lucia@example.com',
          'tipo': 'CLIENTE',
          'rol': null,
          'permisos': [],
        }),
        '/api/pedidos/mios': (s) => conSesion(s, []),
        '/api/pagos/mios': (s) => conSesion(s, []),
      },
    );

    await abrirEn(tester, api, '/pedidos');

    expect(find.text('Todavía no hiciste pedidos.'), findsOneWidget);
    final preferencias = await SharedPreferences.getInstance();
    expect(preferencias.getString('sesion_tienda'), 'abc');
  });

  testWidgets('si el producto tiene tamaños, hay que elegir uno para agregarlo al carrito', (tester) async {
    final api = apiFalsa(
      respuestas: {
        '/api/catalogo/productos/3': (_) => json({
          'id': 3,
          'nombre': 'Torta tres leches',
          'descripcion': 'Hecha en casa',
          'precio': 110.0,
          'categoria': {'id': 1, 'nombre': 'Tortas'},
          'activo': true,
          'disponible': true,
          'personalizable': true,
          'imagen': null,
          'opciones': [
            {'id': 1, 'tipo': 'Tamaño', 'nombre': 'Mediana', 'recargo': 0.0},
            {'id': 2, 'tipo': 'Relleno', 'nombre': 'Frutilla', 'recargo': 10.0},
          ],
        }),
      },
    );
    await abrirEn(tester, api, '/producto/3');
    ButtonStyleButton agregar() => tester.widget<ButtonStyleButton>(
      find.ancestor(
        of: find.text('Agregar al carrito'),
        matching: find.byWidgetPredicate((w) => w is ButtonStyleButton),
      ),
    );

    expect(find.text('Tamaño (elige uno)'), findsOneWidget);
    expect(find.text('Relleno (opcional)'), findsOneWidget);
    expect(agregar().onPressed, isNull);

    await tester.tap(find.text('Frutilla (+Bs 10.00)'));
    await tester.pumpAndSettle();
    expect(agregar().onPressed, isNull);

    await tester.tap(find.text('Mediana'));
    await tester.pumpAndSettle();
    expect(agregar().onPressed, isNotNull);
  });

  testWidgets('confirmar muestra los productos y trae el teléfono y la dirección del perfil', (tester) async {
    SharedPreferences.setMockInitialValues({
      'sesion_tienda': 'abc',
      'carrito': jsonEncode([
        {'productoId': 1, 'nombre': 'Torta de chocolate', 'cantidad': 2, 'opciones': [], 'nombresOpciones': []},
      ]),
    });
    final api = apiFalsa(
      respuestas: {
        '/api/auth/yo': (_) => json({
          'id': 6,
          'nombre': 'Lucía',
          'correo': 'lucia@example.com',
          'tipo': 'CLIENTE',
          'rol': null,
          'permisos': [],
        }),
        '/api/perfil': (_) =>
            json({'nombre': 'Lucía', 'telefono': '70112233', 'direccionHabitual': 'Calle Bolívar 345'}),
        '/api/pedidos/cotizacion': (_) => json({
          'lineas': [
            {
              'productoId': 1,
              'nombre': 'Torta de chocolate',
              'cantidad': 2,
              'precioUnitario': 120.0,
              'opciones': [
                {'id': 1, 'tipo': 'Tamaño', 'nombre': 'Mediana', 'recargo': 0.0},
              ],
              'mensaje': null,
              'subtotal': 240.0,
              'disponible': true,
            },
          ],
          'costoEnvio': 0.0,
          'total': 240.0,
          'confirmable': true,
          'noDisponibles': [],
        }),
      },
    );

    await abrirEn(tester, api, '/confirmar');

    expect(find.text('2 × Torta de chocolate'), findsOneWidget);
    expect(find.text('Mediana'), findsOneWidget);
    expect(find.widgetWithText(TextField, '70112233'), findsOneWidget);

    await tester.tap(find.text('Delivery'));
    await tester.pumpAndSettle();
    expect(find.widgetWithText(TextField, 'Calle Bolívar 345'), findsOneWidget);
  });

  testWidgets('al recargar el carrito, muestra los productos guardados', (tester) async {
    SharedPreferences.setMockInitialValues({
      'carrito': jsonEncode([
        {'productoId': 1, 'nombre': 'Torta de chocolate', 'cantidad': 2, 'opciones': [], 'nombresOpciones': []},
      ]),
    });
    final api = apiFalsa(
      respuestas: {
        '/api/pedidos/cotizacion': (_) => json({
          'lineas': [
            {
              'productoId': 1,
              'nombre': 'Torta de chocolate',
              'cantidad': 2,
              'precioUnitario': 120.0,
              'opciones': [],
              'mensaje': null,
              'subtotal': 240.0,
              'disponible': true,
            },
          ],
          'costoEnvio': 0.0,
          'total': 240.0,
          'confirmable': true,
          'noDisponibles': [],
        }),
      },
    );

    await abrirEn(tester, api, '/carrito');

    expect(find.text('Torta de chocolate'), findsOneWidget);
    expect(find.text('Bs 240.00'), findsNWidgets(2));
  });
}
