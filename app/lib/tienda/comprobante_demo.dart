import 'dart:math';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:decimal/decimal.dart';
import 'package:flutter/material.dart';

import '../comun/formato.dart';
import '../comun/tema.dart';

/// Si la app se compiló para una demostración (`--dart-define=DEMO=true`). Solo entonces la pantalla de pago ofrece
/// generar un comprobante. La Tienda web publicada y la APK de demostración se compilan así; una APK normal no.
const modoDemostracion = bool.fromEnvironment('DEMO');

/// Una referencia de demostración para el pedido, como la que daría un banco: `DEMO-00025-7F3A`.
String referenciaDemo(int pedidoId) {
  final codigo = Random().nextInt(0x10000).toRadixString(16).toUpperCase().padLeft(4, '0');
  return 'DEMO-${pedidoId.toString().padLeft(5, '0')}-$codigo';
}

/// Dibuja el comprobante de un pago para una demostración en vivo: quién pagó, cuánto, a qué pedido, con qué método y
/// cuándo, junto a un QR. El QR tiene la forma de uno de pago pero no se puede escanear: sus módulos salen de un
/// generador sembrado con la referencia, así que cambia en cada pago y nadie puede pagar a él. Devuelve un PNG.
Future<Uint8List> comprobanteDemo({
  required String cliente,
  required Decimal monto,
  required int pedidoId,
  required String metodo,
  required String referencia,
  required DateTime cuando,
}) async {
  const ancho = 600.0, alto = 920.0;
  final grabadora = ui.PictureRecorder();
  final lienzo = Canvas(grabadora, const Rect.fromLTWH(0, 0, ancho, alto));
  lienzo.drawRect(const Rect.fromLTWH(0, 0, ancho, alto), Paint()..color = Colors.white);
  lienzo.drawRect(const Rect.fromLTWH(0, 0, ancho, 104), Paint()..color = Colores.cafe);
  lienzo.drawRect(const Rect.fromLTWH(0, 104, ancho, 44), Paint()..color = Colores.rosaClaro);

  void texto(String palabras, double y, {double tamano = 24, bool titulo = false, Color color = Colores.texto}) {
    final pintor = TextPainter(
      text: TextSpan(
        text: palabras,
        style: TextStyle(
          fontFamily: titulo ? 'Fraunces' : 'WorkSans',
          fontSize: tamano,
          color: color,
          fontVariations: [FontVariation('wght', titulo ? 600 : 500)],
        ),
      ),
      textAlign: TextAlign.center,
      textDirection: TextDirection.ltr,
    )..layout(maxWidth: ancho - 48);
    pintor.paint(lienzo, Offset((ancho - pintor.width) / 2, y));
  }

  texto('Comprobante de pago', 30, tamano: 36, titulo: true, color: Colors.white);
  texto('DATOS DE DEMOSTRACIÓN · NO ES UN PAGO REAL', 114, tamano: 18, color: Colores.texto);

  // El QR: 25 × 25 módulos con los tres cuadros de las esquinas, como uno de verdad.
  const modulos = 25, lado = 10.0;
  const izquierda = (ancho - modulos * lado) / 2, arriba = 176.0;
  final tinta = Paint()..color = Colores.cafe;
  void celda(int x, int y) =>
      lienzo.drawRect(Rect.fromLTWH(izquierda + x * lado, arriba + y * lado, lado, lado), tinta);
  const esquinas = [(0, 0), (modulos - 7, 0), (0, modulos - 7)];
  bool enEsquina(int x, int y) => esquinas.any((e) => x >= e.$1 - 1 && x <= e.$1 + 7 && y >= e.$2 - 1 && y <= e.$2 + 7);
  final azar = Random(referencia.hashCode);
  for (var y = 0; y < modulos; y++) {
    for (var x = 0; x < modulos; x++) {
      if (!enEsquina(x, y) && azar.nextBool()) celda(x, y);
    }
  }
  for (final (ex, ey) in esquinas) {
    for (var y = 0; y < 7; y++) {
      for (var x = 0; x < 7; x++) {
        if (x == 0 || x == 6 || y == 0 || y == 6 || (x >= 2 && x <= 4 && y >= 2 && y <= 4)) celda(ex + x, ey + y);
      }
    }
  }
  texto('QR de demostración: no se puede escanear', arriba + modulos * lado + 12, tamano: 16, color: Colores.cafeSuave);

  // Los datos del pago, cada uno con su etiqueta.
  var y = 498.0;
  for (final (etiqueta, valor) in [
    ('De', cliente),
    ('Para', 'Dulces Aromas'),
    ('Monto', bolivianos(monto)),
    ('Pedido', '#$pedidoId'),
    ('Método', metodo),
    ('Referencia', referencia),
    ('Fecha', fechaHora(cuando)),
  ]) {
    texto(etiqueta.toUpperCase(), y, tamano: 14, color: Colores.cafeSuave);
    texto(valor, y + 18, tamano: etiqueta == 'Monto' ? 32 : 22, titulo: etiqueta == 'Monto');
    y += etiqueta == 'Monto' ? 68 : 56;
  }

  final imagen = await grabadora.endRecording().toImage(ancho.toInt(), alto.toInt());
  final datos = await imagen.toByteData(format: ui.ImageByteFormat.png);
  imagen.dispose();
  return datos!.buffer.asUint8List();
}
