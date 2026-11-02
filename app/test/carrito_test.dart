import 'package:dulces_aromas/tienda/carrito.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));

  LineaCarrito torta({int cantidad = 1, List<int> opciones = const [], String? mensaje}) =>
      LineaCarrito(productoId: 1, nombre: 'Torta', cantidad: cantidad, opciones: opciones, mensaje: mensaje);

  test('el mismo producto con las mismas opciones se junta en una línea', () {
    final carrito = Carrito()
      ..agregar(torta(opciones: [2, 3]))
      ..agregar(torta(cantidad: 2, opciones: [3, 2]));

    expect(carrito.lineas, hasLength(1));
    expect(carrito.unidades, 3);
  });

  test('otras opciones u otro mensaje van en otra línea', () {
    final carrito = Carrito()
      ..agregar(torta(opciones: [2]))
      ..agregar(torta(opciones: [3]))
      ..agregar(torta(opciones: [2], mensaje: 'Feliz día'));

    expect(carrito.lineas, hasLength(3));
  });

  test('la cantidad va de 1 a 99 y en 0 se quita la línea', () {
    final carrito = Carrito()..agregar(torta(cantidad: 98));

    carrito.cambiarCantidad(0, 150);
    expect(carrito.lineas.single.cantidad, 99);

    carrito.cambiarCantidad(0, 0);
    expect(carrito.vacio, isTrue);
  });

  test('HU10: el carrito se conserva en el dispositivo, con o sin sesión', () async {
    Carrito().agregar(torta(cantidad: 2, opciones: [5], mensaje: 'Hola'));
    await Future<void>.delayed(Duration.zero);

    final recuperado = Carrito();
    await recuperado.cargar();

    expect(recuperado.lineas.single.cantidad, 2);
    expect(recuperado.aSolicitud().single, {'productoId': 1, 'cantidad': 2, 'opciones': [5], 'mensaje': 'Hola'});
  });
}
