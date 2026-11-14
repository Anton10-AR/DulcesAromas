import 'package:shared_preferences/shared_preferences.dart';

/// Si la portada completa ya se mostró en este dispositivo, en una visita anterior. Desde la segunda visita el
/// catálogo muestra una portada baja, para que los productos aparezcan sin deslizar.
class RecuerdoPortada {
  RecuerdoPortada({this.claveGuardado = 'portada_vista'});

  final String claveGuardado;

  /// Se vio en una visita anterior. Durante esta visita no cambia, así la portada no se achica a la vista.
  bool vistaAntes = false;

  /// Lee si ya se vio y deja anotado que esta visita la mostró. Si el dispositivo no deja guardar, cada visita es la
  /// primera, y la Tienda funciona igual.
  Future<void> cargar() async {
    try {
      final preferencias = await SharedPreferences.getInstance();
      vistaAntes = preferencias.getBool(claveGuardado) ?? false;
      if (!vistaAntes) await preferencias.setBool(claveGuardado, true);
    } on Object {
      vistaAntes = false;
    }
  }
}
