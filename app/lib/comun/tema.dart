import 'package:flutter/material.dart';

/// Colores de la marca. Los que se usan en texto pequeño están oscurecidos para llegar a un contraste de 4.5:1
/// o más (RNF20); `test/tema_test.dart` lo comprueba.
abstract final class Colores {
  // Tema claro.
  static const fondo = Color(0xFFFBF4EC);
  static const texto = Color(0xFF3A2A23);
  static const cafe = Color(0xFF4A342C);
  static const rosaClaro = Color(0xFFEFC3CE);
  static const rosaMedio = Color(0xFFE19DB0);
  static const rosa = Color(0xFFB96C86);

  /// #B96C86 oscurecido para texto pequeño sobre el fondo claro.
  static const rosaTexto = Color(0xFF8E4762);

  /// #8A6C5C oscurecido para texto secundario sobre el fondo claro.
  static const cafeSuave = Color(0xFF6B5245);

  static const error = Color(0xFFA3262A);

  // Tema oscuro.
  static const fondoOscuro = Color(0xFF1E1714);
  static const textoOscuro = Color(0xFFEDE0D6);
  static const superficieOscura = Color(0xFF2B211D);
  static const textoSuaveOscuro = Color(0xFFC9B8AC);
  static const errorOscuro = Color(0xFFFFB4AB);
}

const _fuenteTexto = 'WorkSans';
const _fuenteTitulos = 'Fraunces';

TextTheme _textos(Color color, Color suave) {
  TextStyle titulo(double tamano) => TextStyle(
    fontFamily: _fuenteTitulos,
    fontSize: tamano,
    color: color,
    fontVariations: const [FontVariation('wght', 600)],
  );
  TextStyle cuerpo(double tamano, {double peso = 400, Color? c}) => TextStyle(
    fontFamily: _fuenteTexto,
    fontSize: tamano,
    color: c ?? color,
    fontVariations: [FontVariation('wght', peso)],
  );
  return TextTheme(
    displaySmall: titulo(34),
    headlineMedium: titulo(28),
    headlineSmall: titulo(24),
    titleLarge: titulo(20),
    titleMedium: cuerpo(16, peso: 600),
    titleSmall: cuerpo(14, peso: 600),
    bodyLarge: cuerpo(16),
    bodyMedium: cuerpo(14),
    bodySmall: cuerpo(12, c: suave),
    labelLarge: cuerpo(14, peso: 600),
  );
}

ThemeData temaClaro() {
  const esquema = ColorScheme(
    brightness: Brightness.light,
    primary: Colores.cafe,
    onPrimary: Colors.white,
    secondary: Colores.rosaTexto,
    onSecondary: Colors.white,
    secondaryContainer: Colores.rosaClaro,
    onSecondaryContainer: Colores.texto,
    tertiary: Colores.rosa,
    onTertiary: Colors.white,
    error: Colores.error,
    onError: Colors.white,
    surface: Colores.fondo,
    onSurface: Colores.texto,
    onSurfaceVariant: Colores.cafeSuave,
    outline: Colores.cafeSuave,
  );
  return _tema(esquema, _textos(Colores.texto, Colores.cafeSuave));
}

ThemeData temaOscuro() {
  const esquema = ColorScheme(
    brightness: Brightness.dark,
    primary: Colores.rosaMedio,
    onPrimary: Colores.fondoOscuro,
    secondary: Colores.rosaClaro,
    onSecondary: Colores.fondoOscuro,
    secondaryContainer: Color(0xFF5A3442),
    onSecondaryContainer: Colores.textoOscuro,
    tertiary: Colores.rosaMedio,
    onTertiary: Colores.fondoOscuro,
    error: Colores.errorOscuro,
    onError: Colores.fondoOscuro,
    surface: Colores.fondoOscuro,
    onSurface: Colores.textoOscuro,
    onSurfaceVariant: Colores.textoSuaveOscuro,
    outline: Colores.textoSuaveOscuro,
  );
  return _tema(esquema, _textos(Colores.textoOscuro, Colores.textoSuaveOscuro));
}

ThemeData _tema(ColorScheme esquema, TextTheme textos) {
  return ThemeData(
    colorScheme: esquema,
    textTheme: textos,
    fontFamily: _fuenteTexto,
    scaffoldBackgroundColor: esquema.surface,
    appBarTheme: AppBarTheme(
      backgroundColor: esquema.surface,
      foregroundColor: esquema.onSurface,
      titleTextStyle: textos.titleLarge,
      elevation: 0,
      scrolledUnderElevation: 1,
    ),
    // Las ayudas y los errores largos pasan a otra línea en vez de cortarse con «…» en pantallas angostas (RNF12).
    inputDecorationTheme: const InputDecorationTheme(border: OutlineInputBorder(), helperMaxLines: 3, errorMaxLines: 3),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(minimumSize: const Size(48, 48), textStyle: textos.labelLarge),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(minimumSize: const Size(48, 48), textStyle: textos.labelLarge),
    ),
  );
}
