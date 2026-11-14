import 'dart:math';

import 'package:dulces_aromas/comun/fondo.dart';
import 'package:dulces_aromas/comun/tema.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

/// Contraste según WCAG: (L1 + 0.05) / (L2 + 0.05).
double contraste(Color a, Color b) {
  final la = a.computeLuminance();
  final lb = b.computeLuminance();
  return (max(la, lb) + 0.05) / (min(la, lb) + 0.05);
}

/// RNF20: el texto tiene un contraste de 4.5:1 o más en los dos temas.
void main() {
  final pares = <String, (Color, Color)>{
    'texto sobre fondo claro': (Colores.texto, Colores.fondo),
    'rosa de texto sobre fondo claro': (Colores.rosaTexto, Colores.fondo),
    'café suave sobre fondo claro': (Colores.cafeSuave, Colores.fondo),
    'botón café': (Colors.white, Colores.cafe),
    'error sobre fondo claro': (Colores.error, Colores.fondo),
    'texto sobre rosa claro': (Colores.texto, Colores.rosaClaro),
    'texto sobre fondo oscuro': (Colores.textoOscuro, Colores.fondoOscuro),
    'texto suave sobre fondo oscuro': (Colores.textoSuaveOscuro, Colores.fondoOscuro),
    'botón rosa en tema oscuro': (Colores.fondoOscuro, Colores.rosaMedio),
    'error sobre fondo oscuro': (Colores.errorOscuro, Colores.fondoOscuro),
    'café suave sobre tarjeta blanca': (Colores.cafeSuave, Colores.tarjeta),
    'texto suave sobre tarjeta oscura': (Colores.textoSuaveOscuro, Colores.superficieOscura),
    'rosa de texto sobre tarjeta blanca': (Colores.rosaTexto, Colores.tarjeta),
    for (final tono in Tono.values.where((t) => t.texto != null)) ...{
      'etiqueta ${tono.name} en tema claro': (tono.texto!, tono.relleno!),
      'etiqueta ${tono.name} en tema oscuro': (tono.textoOscuro!, tono.rellenoOscuro!),
    },
  };

  for (final par in pares.entries) {
    test('${par.key} llega a 4.5:1', () {
      expect(contraste(par.value.$1, par.value.$2), greaterThanOrEqualTo(4.5));
    });
  }

  // El fondo decorado tiñe el fondo; donde más lo hace, en el centro del brillo y del tono cálido, el texto sigue
  // llegando a 4.5:1.
  for (final oscuro in [false, true]) {
    final tema = oscuro ? 'oscuro' : 'claro';
    final textos = oscuro
        ? {'texto': Colores.textoOscuro, 'texto suave': Colores.textoSuaveOscuro, 'error': Colores.errorOscuro}
        : {
            'texto': Colores.texto,
            'rosa de texto': Colores.rosaTexto,
            'café suave': Colores.cafeSuave,
            'error': Colores.error,
          };
    final trazo = FondoDecorado.trazo(oscuro);
    for (final (zona, mancha, conTrazo) in [
      ('el brillo rosa', FondoDecorado.brillo(oscuro), false),
      ('el tono cálido', FondoDecorado.calido(oscuro), false),
      ('un dibujo sobre el brillo rosa', FondoDecorado.brillo(oscuro), true),
      ('un dibujo sobre el tono cálido', FondoDecorado.calido(oscuro), true),
    ]) {
      final tenido = Color.alphaBlend(mancha, FondoDecorado.base(oscuro));
      final fondo = conTrazo ? Color.alphaBlend(trazo, tenido) : tenido;
      for (final texto in textos.entries) {
        test('RNF20: ${texto.key} sobre $zona del fondo $tema llega a 4.5:1', () {
          expect(contraste(texto.value, fondo), greaterThanOrEqualTo(4.5));
        });
      }
    }
  }

  // El texto del botón principal sobre los extremos de su degradado, normal y presionado.
  for (final (nombre, texto, colores) in [
    ('claro', Colors.white, Colores.botonClaro),
    ('claro presionado', Colors.white, Colores.botonClaroPresionado),
    ('oscuro', Colores.fondoOscuro, Colores.botonOscuro),
    ('oscuro presionado', Colores.fondoOscuro, Colores.botonOscuroPresionado),
  ]) {
    test('RNF20: el texto del botón principal $nombre llega a 4.5:1 en todo el degradado', () {
      expect(contraste(texto, colores.$1), greaterThanOrEqualTo(4.5));
      expect(contraste(texto, colores.$2), greaterThanOrEqualTo(4.5));
    });
  }

  test('el rosa y el café originales no alcanzan para texto pequeño, por eso se oscurecen', () {
    expect(contraste(Colores.rosa, Colores.fondo), lessThan(4.5));
    expect(contraste(const Color(0xFF8A6C5C), Colores.fondo), lessThan(4.5));
  });
}
