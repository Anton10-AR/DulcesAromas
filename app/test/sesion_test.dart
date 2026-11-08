import 'package:dulces_aromas/comun/api.dart';
import 'package:dulces_aromas/comun/sesion.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({'sesion_tienda': 'abc'}));

  Future<String?> guardada() async => (await SharedPreferences.getInstance()).getString('sesion_tienda');

  test('sin conexión al abrir la app, la sesión guardada no se borra', () async {
    final api = Api(cliente: MockClient((_) => throw http.ClientException('caído')), base: 'http://api');
    final sesion = Sesion(api, claveGuardado: 'sesion_tienda');

    await sesion.restaurar();

    expect(sesion.iniciada, isFalse);
    expect(sesion.cargando, isFalse);
    expect(await guardada(), 'abc');
  });

  test('si la API dice que la sesión venció, se olvida', () async {
    final api = Api(
      cliente: MockClient((_) async => http.Response('{"codigo":"SIN_SESION","mensaje":"Inicia sesión."}', 401)),
      base: 'http://api',
    );
    final sesion = Sesion(api, claveGuardado: 'sesion_tienda');

    await sesion.restaurar();

    expect(sesion.iniciada, isFalse);
    expect(await guardada(), isNull);
  });
}
