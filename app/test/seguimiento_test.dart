import 'package:dulces_aromas/comun/modelos.dart';
import 'package:dulces_aromas/comun/tema.dart';
import 'package:dulces_aromas/tienda/pantallas/progreso_pedido.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  // 02/11/2026 a las 12:19, 12:40 y 13:05 en La Paz (UTC−4).
  final pendiente = Cambio(null, 'PENDIENTE', DateTime.utc(2026, 11, 2, 16, 19), null, null);
  final confirmado = Cambio('PENDIENTE', 'CONFIRMADO', DateTime.utc(2026, 11, 2, 16, 40), 'Admin', null);
  final preparando = Cambio('CONFIRMADO', 'EN_PREPARACION', DateTime.utc(2026, 11, 2, 17, 5), 'Ana', null);
  final entrega = DateTime.utc(2026, 11, 6, 16);

  Future<void> mostrar(WidgetTester tester, SeguimientoPedido seguimiento) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: temaClaro(),
        home: Scaffold(body: SingleChildScrollView(child: seguimiento)),
      ),
    );
  }

  testWidgets('CU15: cada paso cumplido lleva su fecha y el actual dice qué sigue', (tester) async {
    await mostrar(
      tester,
      SeguimientoPedido(
        estado: 'EN_PREPARACION',
        delivery: true,
        historial: [pendiente, confirmado, preparando],
        horarioEntrega: entrega,
        mensaje: 'Estamos preparando tu pedido.',
      ),
    );

    for (final paso in ['Pendiente', 'Confirmado', 'En preparación', 'Listo', 'En camino', 'Entregado']) {
      expect(find.text(paso), findsOneWidget);
    }
    expect(find.text('02/11/2026 12:19'), findsOneWidget);
    expect(find.text('02/11/2026 13:05'), findsOneWidget);
    expect(find.text('Estamos preparando tu pedido.'), findsOneWidget);
    expect(find.text('Entrega pedida: 06/11/2026 12:00'), findsOneWidget);
    expect(find.byIcon(Icons.check), findsNWidgets(2), reason: 'Pendiente y Confirmado están cumplidos');
  });

  testWidgets('el recojo no pasa por En camino', (tester) async {
    await mostrar(
      tester,
      SeguimientoPedido(estado: 'PENDIENTE', delivery: false, historial: [pendiente], horarioEntrega: entrega),
    );

    expect(find.text('En camino'), findsNothing);
    expect(find.text('Listo para recoger'), findsOneWidget);
    expect(find.text('Recojo pedido: 06/11/2026 12:00'), findsOneWidget);
  });

  testWidgets('un pedido cancelado termina en Cancelado, con su motivo', (tester) async {
    await mostrar(
      tester,
      SeguimientoPedido(
        estado: 'CANCELADO',
        delivery: true,
        historial: [
          pendiente,
          confirmado,
          Cambio('CONFIRMADO', 'CANCELADO', DateTime.utc(2026, 11, 2, 18), null, null),
        ],
        horarioEntrega: entrega,
        motivoCancelacion: 'Cambié de fecha',
      ),
    );

    expect(find.text('Cancelado'), findsOneWidget);
    expect(find.text('Motivo: Cambié de fecha'), findsOneWidget);
    expect(find.text('En preparación'), findsNothing);
    expect(find.text('Entregado'), findsNothing);
  });

  testWidgets('RN13: tras una entrega fallida, Listo muestra la observación', (tester) async {
    await mostrar(
      tester,
      SeguimientoPedido(
        estado: 'LISTO',
        delivery: true,
        historial: [
          pendiente,
          confirmado,
          preparando,
          Cambio('EN_PREPARACION', 'LISTO', DateTime.utc(2026, 11, 2, 18), 'Ana', null),
          Cambio('LISTO', 'EN_CAMINO', DateTime.utc(2026, 11, 2, 19), 'Marco', null),
          Cambio('EN_CAMINO', 'LISTO', DateTime.utc(2026, 11, 2, 20), 'Marco', 'No había nadie en casa'),
        ],
        horarioEntrega: entrega,
      ),
    );

    expect(find.text('02/11/2026 16:00 · No había nadie en casa'), findsOneWidget);
    expect(find.text('02/11/2026 15:00'), findsNothing, reason: 'En camino vuelve a estar pendiente');
  });
}
