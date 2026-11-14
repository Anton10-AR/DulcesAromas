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
      oscuro ? Colores.rosa.withValues(alpha: 0.2) : Colores.rosaClaro.withValues(alpha: 0.45);

  /// El tono cálido de abajo a la izquierda, en su centro.
  static Color calido(bool oscuro) =>
      oscuro ? const Color(0xFF6E4B3E).withValues(alpha: 0.32) : const Color(0xFFF2D9C2).withValues(alpha: 0.6);

  @override
  Widget build(BuildContext context) {
    final oscuro = Theme.of(context).brightness == Brightness.dark;
    final base = FondoDecorado.base(oscuro);
    final brillo = FondoDecorado.brillo(oscuro);
    final calido = FondoDecorado.calido(oscuro);
    final puntos = (oscuro ? Colores.textoOscuro : Colores.cafe).withValues(alpha: oscuro ? 0.045 : 0.05);
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
    final lado = size.shortestSide.clamp(320.0, 900.0);
    void mancha(Offset centro, double radio, Color color) {
      final rect = Rect.fromCircle(center: centro, radius: radio);
      canvas.drawRect(
        Offset.zero & size,
        Paint()..shader = RadialGradient(colors: [color, color.withValues(alpha: 0)]).createShader(rect),
      );
    }

    mancha(Offset(size.width, 0), lado * 0.9, brillo);
    mancha(Offset(0, size.height), lado * 0.8, calido);
    final pincel = Paint()..color = puntos;
    for (var y = paso / 2; y < size.height; y += paso) {
      // Filas alternadas corridas medio paso: la trama se ve tejida y no como una cuadrícula.
      final corrimiento = ((y / paso).floor().isEven) ? 0.0 : paso / 2;
      for (var x = paso / 2 + corrimiento; x < size.width; x += paso) {
        canvas.drawCircle(Offset(x, y), 1.1, pincel);
      }
    }
  }

  @override
  bool shouldRepaint(_Decoracion antes) => antes.brillo != brillo || antes.calido != calido || antes.puntos != puntos;
}
