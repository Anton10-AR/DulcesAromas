import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/sesion.dart';
import '../../comun/widgets.dart';

/// Registrarse como cliente (CU11, HU15). Al terminar queda con la sesión iniciada.
class RegistroPantalla extends StatefulWidget {
  const RegistroPantalla({super.key, this.volver});

  final String? volver;

  @override
  State<RegistroPantalla> createState() => _RegistroPantallaState();
}

class _RegistroPantallaState extends State<RegistroPantalla> {
  final _nombre = TextEditingController();
  final _correo = TextEditingController();
  final _contrasena = TextEditingController();
  ErrorApi? _error;
  bool _enviando = false;

  @override
  void dispose() {
    _nombre.dispose();
    _correo.dispose();
    _contrasena.dispose();
    super.dispose();
  }

  Future<void> _registrar() async {
    setState(() {
      _enviando = true;
      _error = null;
    });
    try {
      await context.read<Sesion>().registrarse(_nombre.text.trim(), _correo.text.trim(), _contrasena.text);
      if (mounted) context.go(widget.volver ?? '/');
    } on ErrorApi catch (e) {
      setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final correoRegistrado = _error?.codigo == 'CORREO_REGISTRADO';
    return Scaffold(
      appBar: AppBar(title: const Text('Crear una cuenta')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Contenido(
          ancho: 440,
          child: AutofillGroup(
            child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
              if (_error != null && _error!.campos.isEmpty) AvisoError(_error!.mensaje),
              // HU15: si el correo ya existe, se ofrece iniciar sesión o recuperar la contraseña.
              if (correoRegistrado)
                Wrap(spacing: 8, children: [
                  OutlinedButton(onPressed: () => context.go('/ingresar'), child: const Text('Iniciar sesión')),
                  TextButton(onPressed: () => context.push('/recuperar'), child: const Text('Recuperar contraseña')),
                ]),
              if (correoRegistrado) const SizedBox(height: 16),
              Campo(
                  controlador: _nombre,
                  etiqueta: 'Nombre',
                  nombre: 'nombre',
                  errores: _error?.campos,
                  autocompletar: const [AutofillHints.name]),
              Campo(
                  controlador: _correo,
                  etiqueta: 'Correo',
                  nombre: 'correo',
                  errores: _error?.campos,
                  teclado: TextInputType.emailAddress,
                  autocompletar: const [AutofillHints.email]),
              Campo(
                  controlador: _contrasena,
                  etiqueta: 'Contraseña',
                  nombre: 'contrasena',
                  errores: _error?.campos,
                  oculto: true,
                  ayuda: 'Al menos 8 caracteres, con una letra y un número.',
                  autocompletar: const [AutofillHints.newPassword]),
              FilledButton(onPressed: _enviando ? null : _registrar, child: const Text('Crear cuenta')),
            ]),
          ),
        ),
      ),
    );
  }
}
