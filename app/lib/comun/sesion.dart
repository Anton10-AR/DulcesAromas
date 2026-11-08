import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'api.dart';

/// El usuario con sesión iniciada, como lo devuelve la API.
class Usuario {
  Usuario({
    required this.id,
    required this.nombre,
    required this.correo,
    required this.tipo,
    this.rol,
    required this.permisos,
  });

  factory Usuario.deJson(Map<String, dynamic> j) => Usuario(
    id: j['id'] as int,
    nombre: j['nombre'] as String,
    correo: j['correo'] as String,
    tipo: j['tipo'] as String,
    rol: j['rol'] as String?,
    permisos: Set<String>.from(j['permisos'] as List),
  );

  final int id;
  final String nombre;
  final String correo;
  final String tipo;
  final String? rol;
  final Set<String> permisos;

  bool get esCliente => tipo == 'CLIENTE';

  /// El menú muestra solo lo que el rol permite (HU06); el servidor valida igual cada acción.
  bool puede(String permiso) => permisos.contains(permiso);
}

/// Sesión de la app (CU01). Guarda el token en el dispositivo para no pedir la contraseña cada vez.
class Sesion extends ChangeNotifier {
  Sesion(this.api, {required this.claveGuardado}) {
    api.alPerderSesion = _perdida;
  }

  final Api api;

  /// Clave distinta para la Tienda y el Panel, para que no compartan sesión en el mismo navegador.
  final String claveGuardado;

  Usuario? usuario;
  bool debeCambiarContrasena = false;
  bool cargando = true;

  /// La API cerró la sesión: en el Panel, tras 30 minutos sin uso (RNF06). Se muestra al volver a ingresar.
  bool vencida = false;

  bool get iniciada => usuario != null;

  /// Recupera la sesión guardada, si sigue vigente. Solo la olvida si la API dice que venció (401): sin conexión, o
  /// mientras la API despierta en la nube, la conserva para la próxima vez que se abra la app.
  Future<void> restaurar() async {
    final preferencias = await SharedPreferences.getInstance();
    api.token = preferencias.getString(claveGuardado);
    if (api.token != null) {
      try {
        usuario = Usuario.deJson(await api.get('/api/auth/yo') as Map<String, dynamic>);
      } on ErrorApi catch (e) {
        if (e.estado == 401) {
          await _olvidar();
        } else {
          api.token = null;
        }
      }
    }
    cargando = false;
    notifyListeners();
  }

  Future<void> iniciar(String correo, String contrasena) async {
    await _abrir(await api.post('/api/auth/login', {'correo': correo, 'contrasena': contrasena}));
  }

  /// HU15: al terminar el registro, el cliente queda con la sesión iniciada.
  Future<void> registrarse(String nombre, String correo, String contrasena) async {
    await _abrir(await api.post('/api/clientes', {'nombre': nombre, 'correo': correo, 'contrasena': contrasena}));
  }

  /// Vuelve a leer los permisos, que el administrador puede haber cambiado (HU05).
  Future<void> actualizar() async {
    usuario = Usuario.deJson(await api.get('/api/auth/yo') as Map<String, dynamic>);
    notifyListeners();
  }

  void contrasenaCambiada() {
    debeCambiarContrasena = false;
    notifyListeners();
  }

  Future<void> cerrar() async {
    try {
      await api.post('/api/auth/logout');
    } on ErrorApi {
      // Aunque el servidor no responda, la sesión se olvida en el dispositivo.
    }
    await _olvidar();
    notifyListeners();
  }

  Future<void> _abrir(dynamic respuesta) async {
    final json = respuesta as Map<String, dynamic>;
    api.token = json['token'] as String;
    vencida = false;
    usuario = Usuario.deJson(json['usuario'] as Map<String, dynamic>);
    debeCambiarContrasena = json['debeCambiarContrasena'] as bool? ?? false;
    final preferencias = await SharedPreferences.getInstance();
    await preferencias.setString(claveGuardado, api.token!);
    notifyListeners();
  }

  void _perdida() {
    vencida = true;
    _olvidar().then((_) => notifyListeners());
  }

  Future<void> _olvidar() async {
    api.token = null;
    usuario = null;
    debeCambiarContrasena = false;
    final preferencias = await SharedPreferences.getInstance();
    await preferencias.remove(claveGuardado);
  }
}
