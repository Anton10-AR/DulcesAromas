import 'impresora_sin_web.dart' if (dart.library.js_interop) 'impresora_web.dart' as plataforma;

/// Imprime por el diálogo del navegador. Fuera de la web no está disponible, y los botones de imprimir no se muestran.
class Impresora {
  const Impresora();

  /// La que usa la app; las pruebas la cambian.
  static Impresora actual = const Impresora();

  bool get disponible => plataforma.disponible;

  /// Abre el diálogo de impresión con este contenido en vez de la pantalla.
  void imprimir(String html) => plataforma.imprimir(html);
}
