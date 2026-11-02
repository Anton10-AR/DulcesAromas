import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/modelos.dart';
import '../../comun/sesion.dart';
import '../../comun/widgets.dart';

/// Mi perfil: datos propios, cambio de contraseña y cerrar sesión (CU13, HU17, HU02).
class PantallaPerfil extends StatefulWidget {
  const PantallaPerfil({super.key, this.conDireccion = true, this.alCerrarSesion});

  /// La dirección habitual solo la tiene un cliente.
  final bool conDireccion;
  final VoidCallback? alCerrarSesion;

  @override
  State<PantallaPerfil> createState() => _PantallaPerfilState();
}

class _PantallaPerfilState extends State<PantallaPerfil> {
  final _nombre = TextEditingController();
  final _telefono = TextEditingController();
  final _direccion = TextEditingController();
  final _actual = TextEditingController();
  final _nueva = TextEditingController();
  String _correo = '';
  bool _cargado = false;
  ErrorApi? _errorDatos;
  ErrorApi? _errorContrasena;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    _api.get('/api/perfil').then((j) {
      final perfil = j as Json;
      setState(() {
        _correo = perfil['correo'] as String;
        _nombre.text = perfil['nombre'] as String;
        _telefono.text = perfil['telefono'] as String? ?? '';
        _direccion.text = perfil['direccionHabitual'] as String? ?? '';
        _cargado = true;
      });
    });
  }

  @override
  void dispose() {
    for (final c in [_nombre, _telefono, _direccion, _actual, _nueva]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _guardarDatos() async {
    setState(() => _errorDatos = null);
    try {
      await _api.put('/api/perfil', {
        'nombre': _nombre.text.trim(),
        'telefono': _telefono.text.trim(),
        'direccionHabitual': _direccion.text.trim(),
      });
      if (mounted) avisar(context, 'Guardamos tus datos.');
    } on ErrorApi catch (e) {
      setState(() => _errorDatos = e);
    }
  }

  Future<void> _cambiarContrasena() async {
    setState(() => _errorContrasena = null);
    try {
      await _api.put('/api/perfil/contrasena', {'actual': _actual.text, 'nueva': _nueva.text});
      _actual.clear();
      _nueva.clear();
      if (!mounted) return;
      context.read<Sesion>().contrasenaCambiada();
      avisar(context, 'Cambiamos tu contraseña. Se cerraron tus otras sesiones.');
    } on ErrorApi catch (e) {
      setState(() => _errorContrasena = e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    final sesion = context.watch<Sesion>();
    if (!_cargado) return const Center(child: CircularProgressIndicator());
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Contenido(
        ancho: 520,
        child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          if (sesion.debeCambiarContrasena)
            const AvisoError('El administrador restableció tu contraseña. Cámbiala para continuar.'),
          Text('Tus datos', style: textos.titleLarge),
          Text(_correo, style: textos.bodySmall),
          const SizedBox(height: 16),
          if (_errorDatos != null && _errorDatos!.campos.isEmpty) AvisoError(_errorDatos!.mensaje),
          Campo(controlador: _nombre, etiqueta: 'Nombre', nombre: 'nombre', errores: _errorDatos?.campos),
          Campo(
              controlador: _telefono,
              etiqueta: 'Teléfono',
              nombre: 'telefono',
              errores: _errorDatos?.campos,
              teclado: TextInputType.phone),
          if (widget.conDireccion)
            Campo(
                controlador: _direccion,
                etiqueta: 'Dirección habitual',
                nombre: 'direccionHabitual',
                errores: _errorDatos?.campos,
                lineas: 2),
          FilledButton(onPressed: _guardarDatos, child: const Text('Guardar datos')),
          const Divider(height: 48),
          Text('Cambiar contraseña', style: textos.titleLarge),
          const SizedBox(height: 16),
          if (_errorContrasena != null && _errorContrasena!.campos.isEmpty) AvisoError(_errorContrasena!.mensaje),
          Campo(
              controlador: _actual,
              etiqueta: 'Contraseña actual',
              nombre: 'actual',
              errores: _errorContrasena?.campos,
              oculto: true),
          Campo(
              controlador: _nueva,
              etiqueta: 'Contraseña nueva',
              nombre: 'nueva',
              errores: _errorContrasena?.campos,
              oculto: true,
              ayuda: 'Al menos 8 caracteres, con una letra y un número.'),
          FilledButton(onPressed: _cambiarContrasena, child: const Text('Cambiar contraseña')),
          const Divider(height: 48),
          OutlinedButton.icon(
            onPressed: () async {
              await context.read<Sesion>().cerrar();
              widget.alCerrarSesion?.call();
            },
            icon: const Icon(Icons.logout),
            label: const Text('Cerrar sesión'),
          ),
        ]),
      ),
    );
  }
}

class PerfilPantalla extends StatelessWidget {
  const PerfilPantalla({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Mi perfil')),
      // HU02: al cerrar sesión vuelve al inicio, sin páginas privadas en el historial.
      body: PantallaPerfil(alCerrarSesion: () => context.go('/')),
    );
  }
}
