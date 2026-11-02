import 'dart:convert';

import 'package:http/http.dart' as http;

/// Dirección de la API. Se cambia al compilar: `--dart-define=API_URL=https://...`. En el emulador de Android,
/// la máquina local es `http://10.0.2.2:8080`.
const String urlApi = String.fromEnvironment('API_URL', defaultValue: 'http://localhost:8080');

/// Error que devuelve la API: un código para reaccionar, un mensaje para mostrar y, si el formulario tiene
/// errores, el mensaje de cada campo (RNF01).
class ErrorApi implements Exception {
  ErrorApi(this.estado, this.codigo, this.mensaje, [this.campos = const {}]);

  final int estado;
  final String codigo;
  final String mensaje;
  final Map<String, String> campos;

  static const sinConexion = 'SIN_CONEXION';

  @override
  String toString() => mensaje;
}

/// Cliente de la API REST. Manda el token de la sesión en `Authorization: Bearer` (CU01).
class Api {
  Api({http.Client? cliente, this.base = urlApi}) : _cliente = cliente ?? http.Client();

  final http.Client _cliente;
  final String base;
  String? token;

  /// Se llama cuando la API responde 401 con sesión: venció por inactividad o se cerró en otro lado.
  void Function()? alPerderSesion;

  Uri uri(String ruta, [Map<String, String?>? parametros]) {
    final limpios = {
      for (final e in (parametros ?? const <String, String?>{}).entries)
        if (e.value != null && e.value!.isNotEmpty) e.key: e.value!,
    };
    return Uri.parse('$base$ruta').replace(queryParameters: limpios.isEmpty ? null : limpios);
  }

  /// Ruta absoluta de un recurso que la API devuelve como ruta relativa, como una imagen.
  String absoluta(String ruta) => '$base$ruta';

  Map<String, String> get _cabeceras => {
    'Content-Type': 'application/json',
    if (token != null) 'Authorization': 'Bearer $token',
  };

  Future<dynamic> get(String ruta, [Map<String, String?>? parametros]) =>
      _enviar(() => _cliente.get(uri(ruta, parametros), headers: _cabeceras));

  Future<dynamic> post(String ruta, [Object? cuerpo]) =>
      _enviar(() => _cliente.post(uri(ruta), headers: _cabeceras, body: cuerpo == null ? null : jsonEncode(cuerpo)));

  Future<dynamic> put(String ruta, Object cuerpo) =>
      _enviar(() => _cliente.put(uri(ruta), headers: _cabeceras, body: jsonEncode(cuerpo)));

  Future<dynamic> delete(String ruta) => _enviar(() => _cliente.delete(uri(ruta), headers: _cabeceras));

  /// Un archivo protegido, como un comprobante: con el token de la sesión, a diferencia de una imagen pública.
  Future<({List<int> bytes, String tipo})> archivo(String ruta) async {
    final http.Response respuesta;
    try {
      respuesta = await _cliente.get(uri(ruta), headers: _cabeceras);
    } catch (_) {
      throw ErrorApi(
        0,
        ErrorApi.sinConexion,
        'No hay conexión con el servidor. Revisa tu internet e inténtalo de nuevo.',
      );
    }
    if (respuesta.statusCode != 200) {
      throw _error(respuesta.statusCode, utf8.decode(respuesta.bodyBytes, allowMalformed: true));
    }
    return (bytes: respuesta.bodyBytes, tipo: respuesta.headers['content-type'] ?? '');
  }

  /// Formulario con un archivo, como el comprobante de pago.
  Future<dynamic> multipart(
    String metodo,
    String ruta, {
    Map<String, String> campos = const {},
    required String nombreArchivo,
    required List<int> bytes,
    required String archivo,
  }) {
    return _enviar(() async {
      final solicitud = http.MultipartRequest(metodo, uri(ruta))
        ..fields.addAll(campos)
        ..files.add(http.MultipartFile.fromBytes(nombreArchivo, bytes, filename: archivo));
      if (token != null) solicitud.headers['Authorization'] = 'Bearer $token';
      return http.Response.fromStream(await _cliente.send(solicitud));
    });
  }

  Future<dynamic> _enviar(Future<http.Response> Function() llamada) async {
    final http.Response respuesta;
    try {
      respuesta = await llamada();
    } catch (_) {
      throw ErrorApi(
        0,
        ErrorApi.sinConexion,
        'No hay conexión con el servidor. Revisa tu internet e inténtalo de nuevo.',
      );
    }
    final texto = utf8.decode(respuesta.bodyBytes, allowMalformed: true);
    if (respuesta.statusCode >= 200 && respuesta.statusCode < 300) {
      return texto.isEmpty ? null : jsonDecode(texto);
    }
    if (respuesta.statusCode == 401 && token != null) {
      alPerderSesion?.call();
    }
    throw _error(respuesta.statusCode, texto);
  }

  static ErrorApi _error(int estado, String texto) {
    try {
      final json = jsonDecode(texto) as Map<String, dynamic>;
      final campos = (json['campos'] as Map<String, dynamic>? ?? {}).map((k, v) => MapEntry(k, v.toString()));
      return ErrorApi(
        estado,
        json['codigo'] as String? ?? 'ERROR',
        json['mensaje'] as String? ?? 'Ocurrió un error.',
        campos,
      );
    } catch (_) {
      return ErrorApi(estado, 'ERROR', 'Ocurrió un error inesperado ($estado). Inténtalo de nuevo.');
    }
  }
}
