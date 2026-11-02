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

    expect(find.byTooltip('Carrito, 1 productos'), findsOneWidget);
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
}
