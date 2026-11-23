import 'dart:convert';

import 'package:dulces_aromas/comun/api.dart';
import 'package:dulces_aromas/comun/tema.dart';
import 'package:dulces_aromas/tienda/pantallas/pasarela_pantalla.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:provider/provider.dart';

http.Response json(Object cuerpo, [int estado = 200]) =>
    http.Response(jsonEncode(cuerpo), estado, headers: {'content-type': 'application/json; charset=utf-8'});

void main() {
  Map<String, Object?> cobro(String estado) => {
    'comercio': 'Dulces Aromas',
    'pedidoId': 7,
    'monto': 120,
    'vence': '2026-11-26T14:15:00Z',
    'estado': estado,
    'referencia': estado == 'PAGADO' ? 'QR-ABCD1234' : null,
  };

  Future<List<String>> mostrar(WidgetTester tester, String estado) async {
    final llamadas = <String>[];
    final api = Api(
      base: 'http://api',
      cliente: MockClient((solicitud) async {
        llamadas.add('${solicitud.method} ${solicitud.url.path}');
        if (solicitud.method == 'POST') return json(cobro('PAGADO'));
        return json(cobro(estado));
      }),
    );
    await tester.pumpWidget(
      Provider.value(
        value: api,
        child: MaterialApp(
          theme: temaClaro(),
          home: const PasarelaPantalla(codigo: 'codigo-secreto'),
        ),
      ),
    );
    await tester.pumpAndSettle();
    return llamadas;
  }

  testWidgets('la pasarela muestra solo el comercio, el pedido y el monto, y al confirmar avisa al sistema', (
    tester,
  ) async {
    final llamadas = await mostrar(tester, 'PENDIENTE');

    expect(find.text('SIMULACIÓN · No se mueve dinero real'), findsOneWidget);
    expect(find.text('Dulces Aromas'), findsOneWidget);
    expect(find.text('Pedido #7'), findsOneWidget);
    expect(find.text('Bs 120.00'), findsOneWidget);

    await tester.tap(find.text('Confirmar pago'));
    await tester.pumpAndSettle();
    expect(llamadas, contains('POST /api/pasarela/codigo-secreto/confirmar'));
    expect(find.text('Pago confirmado'), findsOneWidget);
    expect(find.textContaining('Referencia: QR-ABCD1234'), findsOneWidget);
  });

  testWidgets('cancelar no avisa nada al sistema', (tester) async {
    final llamadas = await mostrar(tester, 'PENDIENTE');
    await tester.tap(find.text('Cancelar'));
    await tester.pumpAndSettle();

    expect(find.text('Pago cancelado'), findsOneWidget);
    expect(llamadas.where((l) => l.startsWith('POST')), isEmpty);
  });

  testWidgets('un QR vencido no se puede confirmar', (tester) async {
    await mostrar(tester, 'VENCIDO');

    expect(find.text('Este QR venció'), findsOneWidget);
    expect(find.text('Confirmar pago'), findsNothing);
  });
}
