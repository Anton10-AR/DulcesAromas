import 'dart:math';

import 'package:flutter/material.dart';

import 'tema.dart';

/// El fondo de las pantallas: el color del tema con un brillo rosa arriba a la derecha, un tono cálido abajo a la
/// izquierda, una trama de puntos y dibujos de línea de postres (cuatro a cada lado en pantallas anchas, tres en el
/// teléfono). Es decoración y queda fija mientras el contenido se desliza. Los colores están elegidos para que el
/// texto que cae encima, incluso sobre un trazo, llegue a 4.5:1 (RNF20); `tema_test` lo comprueba.
class FondoDecorado extends StatelessWidget {
  const FondoDecorado({super.key, required this.child, this.reservaIzquierda = 0});

  final Widget child;

  /// Ancho a la izquierda que tapa otra cosa (el menú lateral del Panel): los dibujos se reparten en el resto.
  final double reservaIzquierda;

  /// El color de fondo del tema.
  static Color base(bool oscuro) => oscuro ? Colores.fondoOscuro : Colores.fondo;

  /// El brillo rosa de arriba a la derecha, en su centro, donde es más intenso.
  static Color brillo(bool oscuro) =>
      oscuro ? Colores.rosa.withValues(alpha: 0.2) : Colores.rosaMedio.withValues(alpha: 0.12);

  /// El tono cálido de abajo a la izquierda, en su centro.
  static Color calido(bool oscuro) =>
      oscuro ? const Color(0xFF7A5241).withValues(alpha: 0.3) : const Color(0xFFE9B98F).withValues(alpha: 0.2);

  /// El trazo de los dibujos.
  static Color trazo(bool oscuro) =>
      oscuro ? Colores.rosaMedio.withValues(alpha: 0.16) : Colores.rosa.withValues(alpha: 0.18);

  /// Desde este ancho van cuatro dibujos a cada lado; por debajo, tres en total.
  static const anchoEscritorio = 900.0;

  @override
  Widget build(BuildContext context) {
    final oscuro = Theme.of(context).brightness == Brightness.dark;
    final puntos = (oscuro ? Colores.textoOscuro : Colores.cafe).withValues(alpha: oscuro ? 0.06 : 0.08);
    return DecoratedBox(
      decoration: BoxDecoration(color: base(oscuro)),
      child: Stack(
        fit: StackFit.passthrough,
        children: [
          Positioned.fill(
            child: IgnorePointer(
              child: RepaintBoundary(
                child: CustomPaint(
                  painter: _Decoracion(
                    brillo: brillo(oscuro),
                    calido: calido(oscuro),
                    puntos: puntos,
                    trazo: trazo(oscuro),
                    reservaIzquierda: reservaIzquierda,
                  ),
                ),
              ),
            ),
          ),
          child,
        ],
      ),
    );
  }
}

/// Un dibujo del fondo: cuál, dónde (en fracciones del área libre) y su giro en grados.
typedef _Dibujo = (void Function(Canvas, Paint) dibujo, double x, double y, double giro);

class _Decoracion extends CustomPainter {
  const _Decoracion({
    required this.brillo,
    required this.calido,
    required this.puntos,
    required this.trazo,
    required this.reservaIzquierda,
  });

  final Color brillo;
  final Color calido;
  final Color puntos;
  final Color trazo;
  final double reservaIzquierda;

  /// Distancia entre los puntos de la trama.
  static const paso = 24.0;

  /// En pantalla ancha, cuatro a cada lado, en los costados que el contenido (de hasta 1100 px) suele dejar libres.
  /// Se alternan un poco hacia adentro y hacia afuera para que no formen una columna.
  static const _escritorio = <_Dibujo>[
    (_torta, 0.05, 0.20, -10),
    (_taza, 0.085, 0.42, 8),
    (_galleta, 0.045, 0.64, -4),
    (_cupcake, 0.08, 0.86, 6),
    (_cupcake, 0.95, 0.18, 9),
    (_porcion, 0.915, 0.40, -8),
    (_dona, 0.955, 0.62, 5),
    (_torta, 0.92, 0.84, -6),
  ];

  /// En el teléfono, tres, repartidos de arriba abajo y alternando los lados; asoman entre las tarjetas.
  static const _telefono = <_Dibujo>[(_cupcake, 0.86, 0.24, 10), (_taza, 0.13, 0.56, -8), (_porcion, 0.84, 0.86, 6)];

  @override
  void paint(Canvas canvas, Size size) {
    final lado = size.longestSide.clamp(400.0, 1600.0);
    // Cada mancha se mantiene intensa hasta la mitad del radio y después se desvanece: se ve como un brillo y no
    // como una sombra difusa.
    void mancha(Offset centro, double radio, Color color) {
      final rect = Rect.fromCircle(center: centro, radius: radio);
      canvas.drawRect(
        Offset.zero & size,
        Paint()
          ..shader = RadialGradient(
            colors: [
              color,
              color.withValues(alpha: color.a * 0.6),
              color.withValues(alpha: 0),
            ],
            stops: const [0, 0.5, 1],
          ).createShader(rect),
      );
    }

    // El centro del brillo, un poco más abajo de la esquina para que no quede tapado por la barra superior.
    mancha(Offset(size.width, size.height * 0.12), lado * 0.55, brillo);
    mancha(Offset(0, size.height), lado * 0.5, calido);

    final pincel = Paint()..color = puntos;
    for (var y = paso / 2; y < size.height; y += paso) {
      // Filas alternadas corridas medio paso: la trama se ve tejida y no como una cuadrícula.
      final corrimiento = ((y / paso).floor().isEven) ? 0.0 : paso / 2;
      for (var x = paso / 2 + corrimiento; x < size.width; x += paso) {
        canvas.drawCircle(Offset(x, y), 1.3, pincel);
      }
    }

    // Los dibujos, en el área que no tapa el menú lateral.
    final libre = Rect.fromLTRB(min(reservaIzquierda, size.width), 0, size.width, size.height);
    if (libre.width < 200 || libre.height < 200) return;
    final escritorio = size.width >= FondoDecorado.anchoEscritorio;
    final dibujos = escritorio ? _escritorio : _telefono;
    // En pantalla ancha, cuatro a cada lado deben caber de arriba abajo sin tocarse.
    final tamano = escritorio
        ? min((libre.width * 0.07).clamp(72.0, 112.0), libre.height * 0.17)
        : (libre.width * 0.26).clamp(72.0, 110.0);
    final lapiz = Paint()
      ..color = trazo
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.8
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;
    for (final (dibujo, x, y, giro) in dibujos) {
      canvas.save();
      canvas.translate(libre.left + libre.width * x, libre.top + libre.height * y);
      canvas.rotate(giro * pi / 180);
      // Los dibujos están hechos en un cuadro de 64 × 64, centrado en el punto elegido.
      canvas.scale(tamano / 64);
      canvas.translate(-32, -32);
      dibujo(canvas, lapiz);
      canvas.restore();
    }
  }

  @override
  bool shouldRepaint(_Decoracion antes) =>
      antes.brillo != brillo ||
      antes.calido != calido ||
      antes.puntos != puntos ||
      antes.trazo != trazo ||
      antes.reservaIzquierda != reservaIzquierda;
}

// Los dibujos, en un cuadro de 64 × 64.

void _torta(Canvas canvas, Paint lapiz) {
  canvas.drawLine(const Offset(12, 54), const Offset(52, 54), lapiz);
  canvas.drawPath(
    Path()
      ..moveTo(14, 54)
      ..lineTo(14, 34)
      ..lineTo(50, 34)
      ..lineTo(50, 54)
      ..moveTo(18, 34)
      ..lineTo(18, 26)
      ..lineTo(46, 26)
      ..lineTo(46, 34)
      // La crema que chorrea por el piso de abajo.
      ..moveTo(14, 42)
      ..cubicTo(18, 45, 22, 45, 26, 42)
      ..cubicTo(30, 39, 34, 39, 38, 42)
      ..cubicTo(42, 45, 46, 45, 50, 42)
      // La vela y su llama.
      ..moveTo(32, 26)
      ..lineTo(32, 18)
      ..moveTo(32, 15)
      ..cubicTo(30, 13, 30, 10, 32, 8)
      ..cubicTo(34, 10, 34, 13, 32, 15),
    lapiz,
  );
}

void _cupcake(Canvas canvas, Paint lapiz) {
  canvas.drawPath(
    Path()
      // El pirotín, con sus pliegues.
      ..moveTo(18, 34)
      ..lineTo(22, 54)
      ..lineTo(42, 54)
      ..lineTo(46, 34)
      ..close()
      ..moveTo(26, 34)
      ..lineTo(27, 54)
      ..moveTo(32, 34)
      ..lineTo(32, 54)
      ..moveTo(38, 34)
      ..lineTo(37, 54)
      // La crema.
      ..moveTo(16, 34)
      ..cubicTo(14, 28, 19, 23, 24, 24)
      ..cubicTo(25, 18, 31, 15, 36, 18)
      ..cubicTo(41, 16, 47, 20, 46, 26)
      ..cubicTo(50, 28, 50, 32, 48, 34),
    lapiz,
  );
  canvas.drawCircle(const Offset(36, 13), 3, lapiz);
}

void _taza(Canvas canvas, Paint lapiz) {
  canvas.drawPath(
    Path()
      ..moveTo(12, 28)
      ..lineTo(46, 28)
      ..lineTo(46, 38)
      ..arcToPoint(const Offset(32, 52), radius: const Radius.circular(14))
      ..lineTo(26, 52)
      ..arcToPoint(const Offset(12, 38), radius: const Radius.circular(14))
      ..close()
      // El asa.
      ..moveTo(46, 32)
      ..lineTo(50, 32)
      ..arcToPoint(const Offset(50, 44), radius: const Radius.circular(6))
      ..lineTo(45, 44)
      // El vapor.
      ..moveTo(22, 22)
      ..cubicTo(20, 19, 24, 17, 22, 14)
      ..moveTo(30, 22)
      ..cubicTo(28, 19, 32, 17, 30, 14)
      ..moveTo(38, 22)
      ..cubicTo(36, 19, 40, 17, 38, 14),
    lapiz,
  );
  canvas.drawLine(const Offset(8, 57), const Offset(52, 57), lapiz);
}

void _dona(Canvas canvas, Paint lapiz) {
  canvas.drawCircle(const Offset(32, 32), 19, lapiz);
  canvas.drawCircle(const Offset(32, 32), 7, lapiz);
  // El glaseado ondulado y las chispas.
  canvas.drawPath(
    Path()
      ..moveTo(15, 28)
      ..cubicTo(18, 33, 21, 26, 24, 31)
      ..moveTo(40, 31)
      ..cubicTo(43, 26, 46, 33, 49, 28),
    lapiz,
  );
  for (final (a, b) in const [
    (Offset(24, 19), Offset(27, 21)),
    (Offset(37, 17), Offset(40, 19)),
    (Offset(44, 40), Offset(45, 43)),
    (Offset(31, 45), Offset(34, 46)),
    (Offset(19, 39), Offset(21, 42)),
  ]) {
    canvas.drawLine(a, b, lapiz);
  }
}

void _porcion(Canvas canvas, Paint lapiz) {
  canvas.drawPath(
    Path()
      // La porción vista de costado: la punta a la izquierda, la cara cortada a la derecha.
      ..moveTo(10, 46)
      ..lineTo(52, 30)
      ..lineTo(52, 50)
      ..lineTo(10, 50)
      ..close()
      // El relleno entre los dos bizcochos.
      ..moveTo(22, 46)
      ..lineTo(52, 40)
      // La crema de arriba y la cereza.
      ..moveTo(18, 43)
      ..cubicTo(26, 37, 34, 38, 42, 31)
      ..cubicTo(46, 28, 50, 28, 52, 30),
    lapiz,
  );
  canvas.drawCircle(const Offset(44, 24), 3.5, lapiz);
  canvas.drawPath(
    Path()
      ..moveTo(44, 20.5)
      ..cubicTo(45, 17, 47, 15, 50, 14),
    lapiz,
  );
  canvas.drawLine(const Offset(6, 54), const Offset(56, 54), lapiz);
}

void _galleta(Canvas canvas, Paint lapiz) {
  // Una galleta con chispas de chocolate, con un bocado.
  canvas.drawPath(
    Path()
      ..moveTo(44, 16)
      ..arcToPoint(const Offset(50, 30), radius: const Radius.circular(5), clockwise: false)
      ..arcToPoint(const Offset(44, 16), radius: const Radius.circular(19), largeArc: true),
    lapiz,
  );
  for (final centro in const [Offset(24, 24), Offset(36, 26), Offset(22, 38), Offset(34, 40), Offset(42, 36)]) {
    canvas.drawCircle(centro, 2.2, lapiz);
  }
}
