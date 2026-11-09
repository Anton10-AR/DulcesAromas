import 'dart:math';

import 'package:flutter/material.dart';

import '../../comun/logo.dart';
import '../../comun/tema.dart';

/// Fotos de la portada, en `assets/postres/`.
const fotosPortada = [
  ('pie-limon', 'Pie de limón'),
  ('rollos-canela', 'Rollos de canela'),
  ('tarta-maracuya', 'Tarta de maracuyá'),
  ('galletas-chispas', 'Galletas con chispas de chocolate'),
  ('flan', 'Flan casero'),
  ('alfajores', 'Alfajores de maicena'),
  ('budin-naranja', 'Budín de naranja'),
  ('tiramisu-vaso', 'Tiramisú en vaso'),
];

String rutaFoto(String id) => 'assets/postres/$id.jpg';

/// Lo primero que ve el cliente en la Tienda: el logo sobre fotos de postres y un botón que baja al catálogo. En el
/// teléfono, una foto a la vez que se cambia deslizando (no avanza sola, para quien prefiere menos movimiento); en
/// pantallas anchas, las ocho en mosaico.
class Portada extends StatefulWidget {
  const Portada({super.key, required this.alto, required this.alVerCatalogo});

  final double alto;
  final VoidCallback alVerCatalogo;

  /// Desde este ancho las fotos van en mosaico.
  static const anchoMosaico = 720.0;

  @override
  State<Portada> createState() => _PortadaState();
}

class _PortadaState extends State<Portada> {
  int _actual = 0;

  @override
  Widget build(BuildContext context) {
    final ancha = MediaQuery.sizeOf(context).width >= Portada.anchoMosaico;
    return SizedBox(
      height: widget.alto,
      child: Stack(
        fit: StackFit.expand,
        children: [
          // Las fotos son decoración: el lector de pantalla no las anuncia.
          ExcludeSemantics(child: ancha ? _mosaico() : _carrusel()),
          // Oscurece la foto para que el logo blanco se lea sobre cualquiera.
          const IgnorePointer(
            child: DecoratedBox(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [Color(0x8C1E1714), Color(0xC71E1714)],
                ),
              ),
            ),
          ),
          Center(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // Ancho fijo: el logo se agranda o achica hasta llenarlo.
                  SizedBox(
                    width: min(ancha ? 460.0 : 300.0, MediaQuery.sizeOf(context).width - 48),
                    child: const LogoDulcesAromas(color: Colors.white, acento: Colores.rosaClaro),
                  ),
                  const SizedBox(height: 24),
                  FilledButton(
                    style: FilledButton.styleFrom(
                      backgroundColor: Colores.fondo,
                      foregroundColor: Colores.cafe,
                      padding: const EdgeInsets.symmetric(horizontal: 28),
                    ),
                    onPressed: widget.alVerCatalogo,
                    child: const Text('Ver catálogo'),
                  ),
                ],
              ),
            ),
          ),
          if (!ancha)
            Positioned(
              left: 0,
              right: 0,
              bottom: 14,
              child: ExcludeSemantics(
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    for (var i = 0; i < fotosPortada.length; i++)
                      Container(
                        width: 7,
                        height: 7,
                        margin: const EdgeInsets.symmetric(horizontal: 3),
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: i == _actual ? Colors.white : Colors.white54,
                        ),
                      ),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _carrusel() => PageView.builder(
    itemCount: fotosPortada.length,
    onPageChanged: (i) => setState(() => _actual = i),
    itemBuilder: (_, i) => Image.asset(rutaFoto(fotosPortada[i].$1), fit: BoxFit.cover),
  );

  Widget _mosaico() => Column(
    children: [
      for (final fila in [fotosPortada.sublist(0, 4), fotosPortada.sublist(4)])
        Expanded(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [for (final (id, _) in fila) Expanded(child: Image.asset(rutaFoto(id), fit: BoxFit.cover))],
          ),
        ),
    ],
  );
}
