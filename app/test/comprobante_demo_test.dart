import 'dart:ui' as ui;

import 'package:decimal/decimal.dart';
import 'package:dulces_aromas/tienda/comprobante_demo.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('la referencia de demostración lleva el pedido y un código', () {
    expect(referenciaDemo(25), matches(RegExp(r'^DEMO-00025-[0-9A-F]{4}$')));
  });

  test('las compilaciones normales no ofrecen generar comprobantes', () {
    // Las pruebas corren sin --dart-define=DEMO=true, como la Tienda publicada.
    expect(modoDemostracion, isFalse);
  });

  testWidgets('el comprobante de demostración es una imagen PNG de 600 × 920', (tester) async {
    final bytes = await tester.runAsync(
      () => comprobanteDemo(
        cliente: 'Lucía Torrez Arancibia',
        monto: Decimal.parse('137.50'),
        pedidoId: 25,
        metodo: 'QR',
        referencia: 'DEMO-00025-7F3A',
        cuando: DateTime.utc(2026, 11, 20, 18, 30),
      ),
    );
    expect(bytes!.sublist(0, 8), [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]);
    final codec = await tester.runAsync(() => ui.instantiateImageCodec(bytes));
    final cuadro = await tester.runAsync(() => codec!.getNextFrame());
    expect((cuadro!.image.width, cuadro.image.height), (600, 920));
  });
}
