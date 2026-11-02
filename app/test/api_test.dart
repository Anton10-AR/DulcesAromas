import 'dart:convert';

import 'package:dulces_aromas/comun/api.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

void main() {
  test('un error de la API trae su código, su mensaje y el error de cada campo', () async {
    final api = Api(
      cliente: MockClient(
        (_) async => http.Response(
          jsonEncode({
            'codigo': 'DATOS_INVALIDOS',
            'mensaje': 'Revisa los campos marcados.',
            'campos': {'correo': 'Escribe un correo válido.'},
          }),
          400,
          headers: {'content-type': 'application/json; charset=utf-8'},
        ),
      ),
      base: 'http://api',
    );

    await expectLater(
      api.post('/api/clientes', {}),
      throwsA(
        isA<ErrorApi>()
            .having((e) => e.codigo, 'codigo', 'DATOS_INVALIDOS')
            .having((e) => e.campos['correo'], 'campo', 'Escribe un correo válido.'),
      ),
    );
  });

  test('manda el token y, si la sesión vence, avisa para volver a ingresar', () async {
    String? cabecera;
    final api = Api(
      cliente: MockClient((solicitud) async {
        cabecera = solicitud.headers['Authorization'];
        return http.Response(
          '{"codigo":"SIN_SESION","mensaje":"Inicia sesión para continuar."}',
          401,
          headers: {'content-type': 'application/json; charset=utf-8'},
        );
      }),
      base: 'http://api',
    )..token = 'abc';
    var perdida = false;
    api.alPerderSesion = () => perdida = true;

    await expectLater(api.get('/api/perfil'), throwsA(isA<ErrorApi>()));

    expect(cabecera, 'Bearer abc');
    expect(perdida, isTrue);
  });

  test('sin conexión da un mensaje entendible', () async {
    final api = Api(cliente: MockClient((_) => throw http.ClientException('caído')), base: 'http://api');

    await expectLater(
      api.get('/api/catalogo/productos'),
      throwsA(isA<ErrorApi>().having((e) => e.codigo, 'codigo', ErrorApi.sinConexion)),
    );
  });

  test('los parámetros vacíos no se mandan', () {
    final api = Api(base: 'http://api');

    expect(
      api.uri('/api/catalogo/productos', {'buscar': '', 'categoria': null}).toString(),
      'http://api/api/catalogo/productos',
    );
    expect(
      api.uri('/api/catalogo/productos', {'buscar': 'torta'}).toString(),
      'http://api/api/catalogo/productos?buscar=torta',
    );
  });
}
