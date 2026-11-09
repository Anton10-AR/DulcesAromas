import 'package:flutter/material.dart';

/// Fotos de postres de la portada de la Tienda y del ingreso al Panel, en `assets/postres/`.
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

/// Oscurece una foto para que el logo blanco se lea sobre cualquiera.
const veloSobreFotos = DecoratedBox(
  decoration: BoxDecoration(
    gradient: LinearGradient(
      begin: Alignment.topCenter,
      end: Alignment.bottomCenter,
      colors: [Color(0x8C1E1714), Color(0xC71E1714)],
    ),
  ),
);

/// Iniciales para el círculo de la cuenta: «Luz Morejon» lleva LM.
String iniciales(String nombre) {
  final palabras = nombre.trim().split(RegExp(r'\s+')).where((p) => p.isNotEmpty).toList();
  return palabras.take(2).map((p) => p[0].toUpperCase()).join();
}

/// El círculo rosa con las iniciales de la cuenta, en la barra de la Tienda y del Panel.
class CirculoIniciales extends StatelessWidget {
  const CirculoIniciales(this.nombre, {super.key});

  final String nombre;

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;
    return ExcludeSemantics(
      child: CircleAvatar(
        radius: 16,
        backgroundColor: colores.secondaryContainer,
        child: Text(
          iniciales(nombre),
          style: Theme.of(context).textTheme.labelLarge?.copyWith(color: colores.onSecondaryContainer),
        ),
      ),
    );
  }
}
