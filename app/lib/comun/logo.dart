import 'package:flutter/material.dart';

/// Logo de Dulces Aromas (propuesta C, «Firma»): tres hilos de aroma, el nombre en Fraunces cursiva y el lema entre
/// dos líneas. Se dibuja con widgets, así se ve nítido en cualquier tamaño y toma los colores del lugar donde va. Se
/// escala para caber en el ancho disponible; nunca se corta.
class LogoDulcesAromas extends StatelessWidget {
  const LogoDulcesAromas({super.key, required this.color, required this.acento, this.conLema = true});

  /// Color del nombre, del aroma y del lema.
  final Color color;

  /// Color de las líneas y el punto junto al lema.
  final Color acento;

  final bool conLema;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'Dulces Aromas',
      header: true,
      excludeSemantics: true,
      child: FittedBox(
        fit: BoxFit.contain,
        child: MediaQuery.withNoTextScaling(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              CustomPaint(size: const Size(44, 34), painter: _Aroma(color)),
              const SizedBox(height: 2),
              Text(
                'Dulces Aromas',
                maxLines: 1,
                style: TextStyle(
                  fontFamily: 'Fraunces',
                  fontStyle: FontStyle.italic,
                  fontSize: 50,
                  height: 1.1,
                  color: color,
                  fontVariations: const [FontVariation('wght', 600)],
                ),
              ),
              if (conLema) ...[
                const SizedBox(height: 10),
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(width: 58, height: 2, color: acento),
                    const SizedBox(width: 10),
                    Container(
                      width: 7,
                      height: 7,
                      decoration: BoxDecoration(color: acento, shape: BoxShape.circle),
                    ),
                    const SizedBox(width: 10),
                    Container(width: 58, height: 2, color: acento),
                  ],
                ),
                const SizedBox(height: 10),
                Text(
                  'MOMENTOS PARA COMPARTIR',
                  maxLines: 1,
                  style: TextStyle(
                    fontFamily: 'WorkSans',
                    fontSize: 13,
                    letterSpacing: 3.6,
                    color: color,
                    fontVariations: const [FontVariation('wght', 600)],
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

/// Tres hilos de aroma que suben, como el vapor de algo recién horneado.
class _Aroma extends CustomPainter {
  _Aroma(this.color);

  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final pincel = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3.2
      ..strokeCap = StrokeCap.round;
    // Cada hilo mide 30 de alto; el del centro arranca 4 más arriba.
    for (final (x, y) in [(10.0, 32.0), (22.0, 28.0), (34.0, 32.0)]) {
      final hilo = Path()
        ..moveTo(x, y)
        ..relativeCubicTo(-6, -6, 6, -11, 0, -17)
        ..relativeCubicTo(-5, -5, 4, -9, 0, -13);
      canvas.drawPath(hilo, pincel);
    }
  }

  @override
  bool shouldRepaint(_Aroma anterior) => anterior.color != color;
}
