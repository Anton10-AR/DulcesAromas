import 'package:dulces_aromas/comun/widgets.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  Future<void> dibujar(WidgetTester tester, double ancho) async {
    tester.view.physicalSize = Size(ancho, 800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: ListView(
            children: [
              FilaConAcciones(
                titulo: const Text('Chocolate de cobertura'),
                subtitulo: const Text('Stock: 2 kg · Mínimo: 3 · En el mínimo'),
                acciones: [
                  IconButton(tooltip: 'Historial', onPressed: () {}, icon: const Icon(Icons.history)),
                  FilledButton.tonal(onPressed: () {}, child: const Text('Movimiento')),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  testWidgets('RNF12: a 360 px las acciones bajan debajo del nombre y el nombre usa todo el ancho', (tester) async {
    await dibujar(tester, 360);

    final nombre = tester.getRect(find.text('Chocolate de cobertura'));
    final boton = tester.getRect(find.text('Movimiento'));
    expect(boton.top, greaterThan(nombre.bottom));
    // Antes, con las acciones al lado, al nombre le quedaban unos 30 px y se partía letra por letra.
    expect(nombre.width, greaterThan(250));
    expect(tester.takeException(), isNull);
  });

  testWidgets('RNF20: con el teclado, el control con foco lleva un marco visible; con el mouse, no', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        builder: (context, hijo) => IndicadorDeFoco(child: hijo!),
        home: Scaffold(
          body: Column(
            children: [
              FilledButton(onPressed: () {}, child: const Text('Primero')),
              FilledButton(onPressed: () {}, child: const Text('Segundo')),
            ],
          ),
        ),
      ),
    );
    expect(find.byKey(IndicadorDeFoco.llave), findsNothing);

    await tester.sendKeyEvent(LogicalKeyboardKey.tab);
    await tester.pump();
    await tester.pump();
    final marco = tester.getRect(find.byKey(IndicadorDeFoco.llave));
    final boton = tester.getRect(find.widgetWithText(FilledButton, 'Primero'));
    expect(marco.contains(boton.center), isTrue);

    await tester.tap(find.text('Segundo'), kind: PointerDeviceKind.mouse);
    await tester.pump();
    await tester.pump();
    expect(find.byKey(IndicadorDeFoco.llave), findsNothing);
  });

  testWidgets('RNF12: en pantalla ancha las acciones quedan a la derecha del nombre', (tester) async {
    await dibujar(tester, 1200);

    final nombre = tester.getRect(find.text('Chocolate de cobertura'));
    final boton = tester.getRect(find.text('Movimiento'));
    expect(boton.left, greaterThan(nombre.right));
    expect(boton.top, lessThan(nombre.bottom));
  });
}
