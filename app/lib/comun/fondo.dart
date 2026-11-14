import 'package:flutter/material.dart';

import 'tema.dart';

/// El fondo de las pantallas: el color del tema con un brillo rosa arriba a la derecha, un tono cálido abajo a la
/// izquierda y una trama de puntos muy suave, para que no se vea plano. Es decoración: el texto va sobre tarjetas o
/// sobre zonas donde el fondo apenas cambia, así que el contraste se mantiene (RNF20).
class FondoDecorado extends StatelessWidget {
  const FondoDecorado({super.key, required this.child});

  final Widget child;

  /// El color de fondo del tema.
  static Color base(bool oscuro) => oscuro ? Colores.fondoOscuro : Colores.fondo;

  /// El brillo rosa de arriba a la derecha, en su centro, donde es más intenso.
  static Color brillo(bool oscuro) =>
      oscuro ? Colores.rosa.withValues(alpha: 0.24) : Colores.rosaMedio.withValues(alpha: 0.38);

  /// El tono cálido de abajo a la izquierda, en su centro.
  static Color calido(bool oscuro) =>
      oscuro ? const Color(0xFF7A5241).withValues(alpha: 0.38) : const Color(0xFFE9B98F).withValues(alpha: 0.45);

  @override
  Widget build(BuildContext context) {
    final oscuro = Theme.of(context).brightness == Brightness.dark;
    final base = FondoDecorado.base(oscuro);
    final brillo = FondoDecorado.brillo(oscuro);
    final calido = FondoDecorado.calido(oscuro);
    final puntos = (oscuro ? Colores.textoOscuro : Colores.cafe).withValues(alpha: oscuro ? 0.06 : 0.08);
    return DecoratedBox(
      decoration: BoxDecoration(color: base),
      child: Stack(
        fit: StackFit.passthrough,
        children: [
          Positioned.fill(
            child: IgnorePointer(
              child: RepaintBoundary(
                child: CustomPaint(
                  painter: _Decoracion(brillo: brillo, calido: calido, puntos: puntos),
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

class _Decoracion extends CustomPainter {
  const _Decoracion({required this.brillo, required this.calido, required this.puntos});

  final Color brillo;
  final Color calido;
  final Color puntos;

  /// Distancia entre los puntos de la trama.
  static const paso = 24.0;

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
  }

  @override
  bool shouldRepaint(_Decoracion antes) => antes.brillo != brillo || antes.calido != calido || antes.puntos != puntos;
}
