import 'dart:convert';

import 'package:dulces_aromas/comun/api.dart';
import 'package:dulces_aromas/tienda/app_tienda.dart';
import 'package:dulces_aromas/tienda/totem.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:shared_preferences/shared_preferences.dart';

http.Response json(Object cuerpo, [int estado = 200]) =>
    http.Response(jsonEncode(cuerpo), estado, headers: {'content-type': 'application/json; charset=utf-8'});

/// Una API con la sesión del tótem, un pedido Pendiente y la tarjeta como método automático.
Api apiDelTotem({required List<String> pagos}) {
  return Api(
    base: 'http://api',
    cliente: MockClient((solicitud) async {
      pagos.add('${solicitud.method} ${solicitud.url.path}');
      return switch (solicitud.url.path) {
        '/api/auth/yo' => json({
          'id': 9,
          'nombre': 'Tótem de la feria',
          'correo': 'totem@example.com',
          'tipo': 'CLIENTE',
          'rol': null,
          'permisos': [],
        }),
        '/api/pagos' when solicitud.method == 'GET' => json([
          {'id': 1, 'pedidoId': 7, 'monto': 120, 'estado': 'PENDIENTE_DE_PAGO'},
        ]),
        '/api/metodos-pago' => json([
          {'id': 2, 'nombre': 'Transferencia', 'instrucciones': 'Cuenta 1-234', 'activo': true, 'qr': null},
          {
            'id': 4,
            'nombre': 'Tarjeta de débito',
            'instrucciones': 'Acerca o inserta tu tarjeta.',
            'activo': true,
            'qr': null,
            'automatico': true,
          },
        ]),
        '/api/pagos/tarjeta' => json({
          'id': 1,
          'pedidoId': 7,
          'monto': 120,
          'estado': 'PAGADO',
          'metodo': 'Tarjeta de débito',
          'referencia': 'SIM-1A2B3C',
        }),
        _ => json([]),
      };
    }),
  );
}

void main() {
  Future<GoRouter> abrir(WidgetTester tester, Api api, {bool totem = true}) async {
    SharedPreferences.setMockInitialValues({'sesion_tienda': 't', 'modo_totem': totem});
    tester.view.physicalSize = const Size(1080, 1920);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(AppTienda(api: api));
    await tester.pumpAndSettle();
    return tester.widget<MaterialApp>(find.byType(MaterialApp)).routerConfig! as GoRouter;
  }

  testWidgets('en el tótem la tarjeta ya viene elegida y el pago se aprueba solo', (tester) async {
    final pedidos = <String>[];
    final rutas = await abrir(tester, apiDelTotem(pagos: pedidos));
    rutas.go('/pedidos/7/pagar');
    await tester.pumpAndSettle();

    await tester.tap(find.text('Pagar Bs 120.00 con tarjeta'));
    await tester.pump();
    expect(find.text('Acerca o inserta tu tarjeta en el lector'), findsOneWidget);

    await tester.pump(const Duration(seconds: 3));
    await tester.pumpAndSettle();
    expect(find.text('¡Pago aprobado!'), findsOneWidget);
    expect(find.text('Tarjeta de débito · Autorización SIM-1A2B3C'), findsOneWidget);
    expect(pedidos, contains('POST /api/pagos/tarjeta'));
    // Sin comprobante ni referencia: no se registró un pago manual.
    expect(pedidos, isNot(contains('POST /api/pagos')));

    // Un solo botón: el seguimiento del pedido, que queda en pantalla.
    expect(find.text('Terminar'), findsNothing);
    await tester.tap(find.text('Seguir mi pedido'));
    await tester.pumpAndSettle();
    expect(rutas.routerDelegate.currentConfiguration.uri.path, '/pedidos/7');
  });

  testWidgets('desde el seguimiento, «Nuevo pedido» deja el tótem listo para el siguiente visitante', (tester) async {
    final api = Api(
      base: 'http://api',
      cliente: MockClient((solicitud) async {
        return switch (solicitud.url.path) {
          '/api/auth/yo' => json({
            'id': 9,
            'nombre': 'Tótem',
            'correo': 't@example.com',
            'tipo': 'CLIENTE',
            'rol': null,
            'permisos': [],
          }),
          '/api/pedidos/7' => json({
            'id': 7,
            'estado': 'CONFIRMADO',
            'tipoEntrega': 'RECOJO',
            'direccion': null,
            'telefono': '70012345',
            'horarioEntrega': '2026-11-27T14:00:00Z',
            'lineas': [],
            'costoEnvio': 0,
            'total': 120,
            'fechaCreacion': '2026-11-26T14:00:00Z',
            'asistido': false,
            'motivoCancelacion': null,
            'historial': [],
          }),
          '/api/pagos' => json([
            {'id': 1, 'pedidoId': 7, 'monto': 120, 'estado': 'PAGADO'},
          ]),
          _ => json([]),
        };
      }),
    );
    final rutas = await abrir(tester, api);
    rutas.go('/pedidos/7');
    await tester.pumpAndSettle();

    expect(find.text('Muestra el número #7 en el stand para recibir tu pedido.'), findsOneWidget);
    await tester.tap(find.text('Nuevo pedido'));
    await tester.pumpAndSettle();
    expect(rutas.routerDelegate.currentConfiguration.uri.path, '/');
  });

  testWidgets('el tótem no muestra la cuenta y vuelve al catálogo tras un rato sin uso', (tester) async {
    final rutas = await abrir(tester, apiDelTotem(pagos: []));
    expect(find.byTooltip('Mi cuenta'), findsNothing);

    rutas.go('/carrito');
    await tester.pumpAndSettle();
    await tester.pump(const Duration(seconds: 76));
    expect(find.textContaining('¿Sigues ahí?'), findsOneWidget);

    await tester.pump(const Duration(seconds: 16));
    await tester.pumpAndSettle();
    expect(rutas.routerDelegate.currentConfiguration.uri.path, '/');
    expect(find.textContaining('¿Sigues ahí?'), findsNothing);
  });

  testWidgets('sin el modo tótem, la Tienda muestra la cuenta y no se reinicia sola', (tester) async {
    final rutas = await abrir(tester, apiDelTotem(pagos: []), totem: false);
    expect(find.byTooltip('Mi cuenta'), findsOneWidget);

    rutas.go('/carrito');
    await tester.pumpAndSettle();
    await tester.pump(const Duration(seconds: 120));
    expect(rutas.routerDelegate.currentConfiguration.uri.path, '/carrito');
  });

  group('el primer horario de recojo', () {
    // Las horas son de La Paz (UTC−4).
    DateTime enLaPaz(int dia, int hora, [int minuto = 0]) => DateTime.utc(2026, 11, dia, hora + 4, minuto);

    test('es 24 horas después, redondeado a la media hora', () {
      // Miércoles 25/11 a las 10:10 → jueves 26/11 a las 10:30.
      expect(primerHorarioDeRecojo(enLaPaz(25, 10, 10)), DateTime(2026, 11, 26, 10, 30));
    });

    test('pasa a la apertura del día siguiente si cae después del cierre', () {
      // Miércoles 25/11 a las 19:50 → jueves 26/11 a las 20:00, ya cerrado → viernes 27/11 a las 9:00.
      expect(primerHorarioDeRecojo(enLaPaz(25, 19, 50)), DateTime(2026, 11, 27, 9));
    });

    test('el domingo abre a las 10:00', () {
      // Sábado 28/11 a las 7:00 → domingo 29/11 a las 7:00, antes de abrir → 10:00.
      expect(primerHorarioDeRecojo(enLaPaz(28, 7)), DateTime(2026, 11, 29, 10));
    });
  });

  testWidgets('con QR, el tótem muestra el QR del cobro y pasa solo a «¡Pago aprobado!» cuando la pasarela confirma', (
    tester,
  ) async {
    var pagado = false;
    final api = Api(
      base: 'http://api',
      cliente: MockClient((solicitud) async {
        return switch (solicitud.url.path) {
          '/api/auth/yo' => json({
            'id': 9,
            'nombre': 'Tótem',
            'correo': 't@example.com',
            'tipo': 'CLIENTE',
            'rol': null,
            'permisos': [],
          }),
          '/api/pagos' => json([
            {
              'id': 1,
              'pedidoId': 7,
              'monto': 120,
              'estado': pagado ? 'PAGADO' : 'PENDIENTE_DE_PAGO',
              if (pagado) 'metodo': 'QR',
              if (pagado) 'referencia': 'QR-ABCD1234',
            },
          ]),
          '/api/metodos-pago' => json([
            {
              'id': 1,
              'nombre': 'QR',
              'instrucciones': '',
              'activo': true,
              'qr': null,
              'automatico': true,
              'canal': 'QR',
            },
          ]),
          '/api/pagos/qr' => json({
            'codigo': 'codigo-secreto',
            'pedidoId': 7,
            'monto': 120,
            'vence': DateTime.now().toUtc().add(const Duration(minutes: 15)).toIso8601String(),
          }),
          _ => json([]),
        };
      }),
    );
    final rutas = await abrir(tester, api);
    rutas.go('/pedidos/7/pagar');
    // Sin pumpAndSettle: mientras espera el pago, el indicador gira siempre.
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));
    await tester.pump(const Duration(milliseconds: 500));

    expect(find.byType(QrImageView), findsOneWidget);
    expect(tester.widget<QrImageView>(find.byType(QrImageView)).semanticsLabel, contains('pedido #7'));
    expect(find.text('Esperando el pago…'), findsOneWidget);

    // El visitante confirma en la pasarela, desde su celular.
    pagado = true;
    await tester.pump(const Duration(seconds: 4));
    await tester.pump(const Duration(milliseconds: 500));
    expect(find.text('¡Pago aprobado!'), findsOneWidget);
    expect(find.text('QR · Autorización QR-ABCD1234'), findsOneWidget);
  });

  testWidgets('en la pantalla de pago el tótem espera más antes de volver al inicio', (tester) async {
    final rutas = await abrir(tester, apiDelTotem(pagos: []));
    rutas.go('/pedidos/7/pagar');
    await tester.pumpAndSettle();

    // A los 90 segundos todavía está en el pago: el visitante puede estar pagando con su celular.
    await tester.pump(const Duration(seconds: 91));
    expect(rutas.routerDelegate.currentConfiguration.uri.path, '/pedidos/7/pagar');
    expect(find.textContaining('¿Sigues ahí?'), findsNothing);

    // Pasados 5 minutos sin tocar la pantalla, avisa y vuelve al catálogo.
    await tester.pump(const Duration(seconds: 211));
    expect(find.textContaining('¿Sigues ahí?'), findsOneWidget);
    await tester.pump(const Duration(seconds: 16));
    await tester.pumpAndSettle();
    expect(rutas.routerDelegate.currentConfiguration.uri.path, '/');
  });
}
