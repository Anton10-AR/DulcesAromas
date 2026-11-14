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

  /// Tarjetas blancas con un borde suave sobre el fondo crema, como el «panel» de la landing.
  static const tarjeta = Color(0xFFFFFFFF);
  static const borde = Color(0xFFE6D8CA);

  // Tema oscuro.
  static const fondoOscuro = Color(0xFF1E1714);
  static const textoOscuro = Color(0xFFEDE0D6);
  static const superficieOscura = Color(0xFF2B211D);
  static const textoSuaveOscuro = Color(0xFFC9B8AC);
  static const errorOscuro = Color(0xFFFFB4AB);
  static const bordeOscuro = Color(0xFF3D2F29);

  // Degradado del botón principal: de izquierda a derecha. El texto llega a 4.5:1 sobre los dos extremos.
  static const botonClaro = (Color(0xFF4A342C), Color(0xFF7A5241));
  static const botonOscuro = (Color(0xFFE8AFC0), Color(0xFFD98FA6));

  /// Al presionarlo, el botón se oscurece un poco.
  static const botonClaroPresionado = (Color(0xFF3A2822), Color(0xFF5E3F33));
  static const botonOscuroPresionado = (Color(0xFFDB98AB), Color(0xFFC97D95));
}

/// Color de una etiqueta de estado: el texto y el relleno de cada tono, en los dos temas. Todos llegan a 4.5:1.
enum Tono {
  /// Espera algo del cliente: Pendiente, Pendiente de pago.
  rojo(Color(0xFFAB4C42), Color(0xFFFAEAE8), Color(0xFFD2928B), Color(0xFF462E29)),

  /// En manos del negocio, sin preparar todavía o en el camino: Confirmado, En camino.
  azul(Color(0xFF516C96), Color(0xFFEBEFF6), Color(0xFF93A6C5), Color(0xFF3A3639)),

  /// En proceso: En preparación, En verificación.
  ambar(Color(0xFF8D6628), Color(0xFFFBF1E1), Color(0xFFCFA156), Color(0xFF483725)),

  /// Listo o terminado: Listo, Entregado, Pagado, Activo.
  verde(Color(0xFF547357), Color(0xFFEAF2EA), Color(0xFF8FAD92), Color(0xFF39382E)),

  /// Cerrado: Cancelado, Rechazado, Desactivado. Solo contorno.
  neutro(null, null, null, null);

  const Tono(this.texto, this.relleno, this.textoOscuro, this.rellenoOscuro);

  final Color? texto;
  final Color? relleno;
  final Color? textoOscuro;
  final Color? rellenoOscuro;
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
    // Las líneas divisorias, suaves como el borde de las tarjetas.
    outlineVariant: Colores.borde,
  );
  return _tema(esquema, _textos(Colores.texto, Colores.cafeSuave), tarjeta: Colores.tarjeta, borde: Colores.borde);
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
    outlineVariant: Colores.bordeOscuro,
  );
  return _tema(
    esquema,
    _textos(Colores.textoOscuro, Colores.textoSuaveOscuro),
    tarjeta: Colores.superficieOscura,
    borde: Colores.bordeOscuro,
  );
}

ThemeData _tema(ColorScheme esquema, TextTheme textos, {required Color tarjeta, required Color borde}) {
  return ThemeData(
    colorScheme: esquema,
    textTheme: textos,
    fontFamily: _fuenteTexto,
    // Transparente: debajo de todas las pantallas va el fondo decorado (FondoDecorado, en el builder de la app).
    scaffoldBackgroundColor: Colors.transparent,
    appBarTheme: AppBarTheme(
      backgroundColor: esquema.surface,
      foregroundColor: esquema.onSurface,
      titleTextStyle: textos.titleLarge,
      elevation: 0,
      scrolledUnderElevation: 1,
    ),
    // Tarjetas con una sombra suave y cálida (café en el tema claro), para que se separen del fondo.
    cardTheme: CardThemeData(
      color: tarjeta,
      surfaceTintColor: Colors.transparent,
      elevation: 3,
      shadowColor: esquema.brightness == Brightness.dark
          ? Colors.black.withValues(alpha: 0.55)
          : Colores.cafe.withValues(alpha: 0.16),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(18),
        side: BorderSide(color: borde),
      ),
    ),
    // Chips en píldora; el elegido, con el rosa de la marca.
    chipTheme: ChipThemeData(
      shape: StadiumBorder(side: BorderSide(color: borde)),
      side: BorderSide(color: borde),
      backgroundColor: tarjeta,
      selectedColor: esquema.secondaryContainer,
      labelStyle: textos.labelLarge?.copyWith(color: esquema.onSurface),
      secondaryLabelStyle: textos.labelLarge?.copyWith(color: esquema.onSecondaryContainer),
      checkmarkColor: esquema.onSecondaryContainer,
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
    ),
    // El menú del Panel: la sección actual con el rosa de la marca.
    navigationRailTheme: NavigationRailThemeData(
      backgroundColor: tarjeta,
      indicatorColor: esquema.secondaryContainer,
      selectedIconTheme: IconThemeData(color: esquema.onSecondaryContainer),
      selectedLabelTextStyle: textos.labelLarge?.copyWith(color: esquema.onSurface),
      unselectedLabelTextStyle: textos.bodyMedium?.copyWith(color: esquema.onSurfaceVariant),
      unselectedIconTheme: IconThemeData(color: esquema.onSurfaceVariant),
    ),
    navigationDrawerTheme: NavigationDrawerThemeData(indicatorColor: esquema.secondaryContainer),
    navigationBarTheme: NavigationBarThemeData(indicatorColor: esquema.secondaryContainer),
    // Las ayudas y los errores largos pasan a otra línea en vez de cortarse con «…» en pantallas angostas (RNF12).
    inputDecorationTheme: const InputDecorationTheme(border: OutlineInputBorder(), helperMaxLines: 3, errorMaxLines: 3),
    filledButtonTheme: FilledButtonThemeData(style: _botonPrincipal(esquema, textos)),
    // El botón con borde, en píldora y con un borde del color principal.
    outlinedButtonTheme: OutlinedButtonThemeData(
      style:
          OutlinedButton.styleFrom(
            minimumSize: const Size(48, 48),
            padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 12),
            textStyle: textos.labelLarge,
            shape: const StadiumBorder(),
            foregroundColor: esquema.primary,
          ).copyWith(
            side: WidgetStateProperty.resolveWith(
              (estados) => BorderSide(
                color: estados.contains(WidgetState.disabled)
                    ? esquema.onSurface.withValues(alpha: 0.12)
                    : esquema.primary.withValues(alpha: estados.contains(WidgetState.hovered) ? 1 : 0.7),
                width: 1.5,
              ),
            ),
          ),
    ),
    // El botón de texto, en píldora y con el rosa de la marca.
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(
        minimumSize: const Size(48, 40),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        textStyle: textos.labelLarge,
        shape: const StadiumBorder(),
        foregroundColor: esquema.secondary,
      ),
    ),
  );
}

/// El botón principal: una píldora con un degradado (café en el tema claro, rosa en el oscuro) y una sombra que crece
/// al pasar el ratón o al llegar con el teclado. Al presionarlo se oscurece; deshabilitado, queda gris y plano.
/// El estilo de FilledButton.tonal con su color explícito: así conserva su relleno suave y no toma el degradado del
/// botón principal.
ButtonStyle estiloTonal(BuildContext context) {
  final colores = Theme.of(context).colorScheme;
  return FilledButton.styleFrom(
    backgroundColor: colores.secondaryContainer,
    foregroundColor: colores.onSecondaryContainer,
  );
}

ButtonStyle _botonPrincipal(ColorScheme esquema, TextTheme textos) {
  final oscuro = esquema.brightness == Brightness.dark;
  final normal = oscuro ? Colores.botonOscuro : Colores.botonClaro;
  final presionado = oscuro ? Colores.botonOscuroPresionado : Colores.botonClaroPresionado;
  bool activo(Set<WidgetState> estados) => !estados.contains(WidgetState.disabled);
  return FilledButton.styleFrom(
    minimumSize: const Size(48, 50),
    padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
    textStyle: textos.labelLarge,
    shape: const StadiumBorder(),
  ).copyWith(
    // Transparente para que se vea el degradado; deshabilitado, el gris de siempre.
    backgroundColor: WidgetStateProperty.resolveWith((estados) => activo(estados) ? Colors.transparent : null),
    elevation: WidgetStateProperty.resolveWith((estados) {
      if (!activo(estados) || estados.contains(WidgetState.pressed)) return 0;
      return estados.contains(WidgetState.hovered) || estados.contains(WidgetState.focused) ? 6 : 2;
    }),
    shadowColor: WidgetStatePropertyAll(
      oscuro ? Colors.black.withValues(alpha: 0.6) : Colores.cafe.withValues(alpha: 0.35),
    ),
    // La tinta del toque queda debajo del degradado; el cambio de color al presionar la reemplaza.
    overlayColor: const WidgetStatePropertyAll(Colors.transparent),
    backgroundBuilder: (context, estados, hijo) {
      if (!activo(estados)) return hijo ?? const SizedBox.shrink();
      // Un botón con color propio (como «Ver catálogo» en la portada o los tonales) lo conserva.
      final boton = context.widget;
      if (boton is ButtonStyleButton && boton.style?.backgroundColor != null) return hijo ?? const SizedBox.shrink();
      final (inicio, fin) = estados.contains(WidgetState.pressed) ? presionado : normal;
      final encima = estados.contains(WidgetState.hovered);
      return DecoratedBox(
        decoration: ShapeDecoration(
          shape: const StadiumBorder(),
          gradient: LinearGradient(colors: encima ? [fin, inicio] : [inicio, fin]),
        ),
        child: hijo,
      );
    },
  );
}
