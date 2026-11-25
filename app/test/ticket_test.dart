import 'dart:convert';

import 'package:dulces_aromas/comun/api.dart';
import 'package:dulces_aromas/comun/impresion/impresora.dart';
import 'package:dulces_aromas/comun/impresion/ticket.dart';
import 'package:dulces_aromas/comun/modelos.dart';
import 'package:dulces_aromas/panel/app_panel.dart';
import 'package:dulces_aromas/tienda/app_tienda.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';

http.Response json(Object cuerpo, [int estado = 200]) =>
    http.Response(jsonEncode(cuerpo), estado, headers: {'content-type': 'application/json; charset=utf-8'});

/// Anota lo que se mandaría a imprimir.
class ImpresoraDePrueba extends Impresora {
  final impreso = <String>[];

  @override
  bool get disponible => true;

  @override
  void imprimir(String html) => impreso.add(html);
}

Map<String, Object?> pedidoJson({
  String estado = 'CONFIRMADO',
  bool delivery = false,
  String nombre = 'Torta de fresa',
}) => {
  'id': 7,
  'cliente': 'Lucía Torrez',
  'estado': estado,
  'tipoEntrega': delivery ? 'DELIVERY' : 'RECOJO',
  'direccion': delivery ? 'Calle Bolívar 123' : null,
  'telefono': '70012345',
  'horarioEntrega': '2026-11-27T14:00:00Z',
  'lineas': [
    {
      'productoId': 1,
      'nombre': nombre,
      'cantidad': 2,
      'precioUnitario': 120,
      'opciones': [
        {'id': 3, 'tipo': 'TAMANO', 'nombre': 'Grande', 'recargo': 30},
      ],
      'mensaje': 'Feliz día',
      'subtotal': 300,
      'disponible': true,
    },
  ],
  'costoEnvio': delivery ? 15 : 0,
  'total': delivery ? 315 : 300,
  'fechaCreacion': '2026-11-26T14:00:00Z',
  'asistido': false,
  'motivoCancelacion': null,
  'historial': [],
};

const pagado = {'id': 1, 'pedidoId': 7, 'monto': 300, 'estado': 'PAGADO', 'metodo': 'QR', 'referencia': 'QR-ABCD1234'};
const sinPagar = {'id': 1, 'pedidoId': 7, 'monto': 300, 'estado': 'PENDIENTE_DE_PAGO'};

Api apiCon({required Map<String, Object?> pago, String estado = 'CONFIRMADO', bool personal = false}) => Api(
  base: 'http://api',
  cliente: MockClient(
    (solicitud) async => switch (solicitud.url.path) {
      '/api/auth/yo' => json({
        'id': 9,
        'nombre': 'Luz',
        'correo': 'luz@example.com',
        'tipo': personal ? 'PERSONAL' : 'CLIENTE',
        'rol': personal ? 'VENDEDOR' : null,
        'permisos': personal ? ['PEDIDOS_CONSULTAR'] : [],
      }),
      '/api/pedidos/7' => json(pedidoJson(estado: estado)),
      '/api/pagos' => json([pago]),
      '/api/metodos-pago' => json([
        {'id': 4, 'nombre': 'Tarjeta de débito', 'instrucciones': '', 'activo': true, 'qr': null, 'automatico': true},
      ]),
      '/api/pagos/tarjeta' => json(pagado),
      _ => json([]),
    },
  ),
);

void main() {
  late ImpresoraDePrueba impresora;

  setUp(() {
    impresora = ImpresoraDePrueba();
    Impresora.actual = impresora;
  });
  tearDown(() => Impresora.actual = const Impresora());

  Future<void> abrir(WidgetTester tester, Widget app, String ruta) async {
    tester.view.physicalSize = const Size(1080, 1920);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(app);
    await tester.pumpAndSettle();
    (tester.widget<MaterialApp>(find.byType(MaterialApp)).routerConfig! as GoRouter).go(ruta);
    await tester.pumpAndSettle();
  }

  Future<void> abrirTienda(WidgetTester tester, Api api, String ruta, {bool totem = true}) {
    SharedPreferences.setMockInitialValues({'sesion_tienda': 't', 'modo_totem': totem});
    return abrir(tester, AppTienda(api: api), ruta);
  }

  Future<void> abrirPanel(WidgetTester tester, Api api) {
    SharedPreferences.setMockInitialValues({'sesion_panel': 't'});
    return abrir(tester, AppPanel(api: api), '/pedidos/7');
  }

  group('el contenido', () {
    String ticket({bool delivery = false, String nombre = 'Torta de fresa'}) =>
        ticketHtml(Pedido.deJson(pedidoJson(delivery: delivery, nombre: nombre)), Pago.deJson(pagado));

    test('lleva el pedido, los productos con sus opciones, el total, el pago y el recojo en hora de La Paz', () {
      final html = ticket();
      for (final dato in [
        '#7',
        '2 × Torta de fresa',
        'Grande',
        'Feliz día',
        'Bs 300.00',
        'QR-ABCD1234',
        'Recojo',
        '27/11/2026 10:00',
        'No es factura',
        'no se cobró dinero real',
      ]) {
        expect(html, contains(dato));
      }
      expect(html, isNot(contains('Envío')));
    });

    test('no lleva nombre, teléfono ni dirección', () {
      final html = ticket(delivery: true);
      expect(html, contains('Bs 15.00'));
      for (final dato in ['Lucía', '70012345', 'Bolívar']) {
        expect(html, isNot(contains(dato)));
      }
    });

    test('lo que escribe una persona no se interpreta como HTML', () {
      final html = ticket(nombre: '<img src=x onerror=alert(1)>');
      expect(html, isNot(contains('<img')));
      expect(html, contains('&lt;img'));
    });
  });

  testWidgets('en el tótem se imprime al pulsar el botón de «¡Pago aprobado!», no solo', (tester) async {
    await abrirTienda(tester, apiCon(pago: sinPagar), '/pedidos/7/pagar');
    await tester.tap(find.textContaining('con tarjeta').last);
    await tester.pump(const Duration(seconds: 3));
    await tester.pumpAndSettle();

    expect(find.text('¡Pago aprobado!'), findsOneWidget);
    expect(impresora.impreso, isEmpty);
    await tester.tap(find.text('Imprimir ticket'));
    await tester.pumpAndSettle();
    expect(impresora.impreso.single, contains('Torta de fresa'));
    expect(find.text('Seguir mi pedido'), findsOneWidget);
  });

  testWidgets('en el seguimiento del tótem se vuelve a imprimir', (tester) async {
    await abrirTienda(tester, apiCon(pago: pagado), '/pedidos/7');
    await tester.tap(find.text('Imprimir ticket'));
    await tester.pumpAndSettle();
    expect(impresora.impreso, hasLength(1));
  });

  testWidgets('sin pagar no hay ticket', (tester) async {
    await abrirTienda(tester, apiCon(pago: sinPagar, estado: 'PENDIENTE'), '/pedidos/7');
    expect(find.text('Nuevo pedido'), findsOneWidget);
    expect(find.text('Imprimir ticket'), findsNothing);
  });

  testWidgets('fuera del tótem, la Tienda no ofrece el ticket', (tester) async {
    await abrirTienda(tester, apiCon(pago: pagado), '/pedidos/7', totem: false);
    expect(find.text('Pedido #7'), findsOneWidget);
    expect(find.text('Imprimir ticket'), findsNothing);
  });

  testWidgets('donde no se puede imprimir, no hay botón', (tester) async {
    Impresora.actual = const Impresora();
    await abrirTienda(tester, apiCon(pago: pagado), '/pedidos/7');
    expect(find.text('Nuevo pedido'), findsOneWidget);
    expect(find.text('Imprimir ticket'), findsNothing);
  });

  testWidgets('el Panel imprime el ticket de un pedido pagado', (tester) async {
    await abrirPanel(tester, apiCon(pago: pagado, personal: true));
    await tester.tap(find.text('Imprimir ticket'));
    await tester.pumpAndSettle();
    expect(impresora.impreso.single, contains('#7'));
  });

  testWidgets('el Panel no imprime el ticket de un pedido cancelado', (tester) async {
    await abrirPanel(tester, apiCon(pago: pagado, estado: 'CANCELADO', personal: true));
    expect(find.textContaining('Pedido #7'), findsWidgets);
    expect(find.text('Imprimir ticket'), findsNothing);
  });
}
