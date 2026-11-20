import 'dart:convert';

import 'package:dulces_aromas/comun/api.dart';
import 'package:dulces_aromas/comun/tema.dart';
import 'package:dulces_aromas/panel/pantallas/estimacion_panel.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:provider/provider.dart';

void main() {
  Future<void> mostrar(WidgetTester tester, Map<String, Object?> estimacion) async {
    final api = Api(
      base: 'http://api',
      cliente: MockClient((solicitud) async {
        expect(solicitud.url.path, '/api/pedidos/7/estimacion');
        return http.Response(jsonEncode(estimacion), 200, headers: {'content-type': 'application/json; charset=utf-8'});
      }),
    );
    await tester.pumpWidget(
      Provider.value(
        value: api,
        child: MaterialApp(
          theme: temaClaro(),
          home: const Scaffold(body: EstimacionPedido(pedidoId: 7)),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('HU23: muestra lo que falta y si el pedido llega a tiempo', (tester) async {
    await mostrar(tester, {
      'insumos': [
        {'nombre': 'Chocolate', 'unidad': 'kg', 'necesita': 1.2, 'comprometido': 0.9, 'stock': 1, 'falta': 1.1},
        {'nombre': 'Harina', 'unidad': 'kg', 'necesita': 2, 'comprometido': 1.5, 'stock': 5, 'falta': 0},
      ],
      'sinReceta': ['Galletas de avena'],
      'minutosPedido': 240,
      'minutosCola': 180,
      'horarioEntrega': '2026-11-21T14:00:00Z',
      'listoEstimado': '2026-11-20T21:00:00Z',
      'alcanzaElTiempo': true,
      'alcanzanLosInsumos': false,
    });

    expect(find.text('Insumos: faltan'), findsOneWidget);
    expect(find.text('Llega a tiempo'), findsOneWidget);
    expect(find.textContaining('Este pedido: 4 h · En cola: 3 h'), findsOneWidget);
    expect(find.textContaining('Chocolate: necesita 1.2 kg · en cola 0.9 · hay 1 · faltan 1.1 kg'), findsOneWidget);
    expect(find.textContaining('faltan', findRichText: true), findsNWidgets(2));
    expect(find.text('Sin receta, no estimados: Galletas de avena'), findsOneWidget);
  });

  testWidgets('sin recetas, avisa que no hay nada que estimar', (tester) async {
    await mostrar(tester, {
      'insumos': [],
      'sinReceta': ['Flan casero'],
      'minutosPedido': 0,
      'minutosCola': 60,
      'horarioEntrega': '2026-11-21T14:00:00Z',
      'listoEstimado': '2026-11-20T15:00:00Z',
      'alcanzaElTiempo': true,
      'alcanzanLosInsumos': true,
    });

    expect(find.textContaining('ningún producto de este pedido tiene receta'), findsOneWidget);
  });

  test('las duraciones se leen en horas y minutos', () {
    expect(duracion(45), '45 min');
    expect(duracion(120), '2 h');
    expect(duracion(270), '4 h 30 min');
  });
}
