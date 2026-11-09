import 'dart:math';

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

  test('el rosa y el café originales no alcanzan para texto pequeño, por eso se oscurecen', () {
    expect(contraste(Colores.rosa, Colores.fondo), lessThan(4.5));
    expect(contraste(const Color(0xFF8A6C5C), Colores.fondo), lessThan(4.5));
  });
}
