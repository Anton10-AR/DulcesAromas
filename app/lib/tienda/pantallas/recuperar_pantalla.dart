import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/widgets.dart';

/// Recuperar la contraseña (CU12, HU16). Sin token, pide el correo; con el token del enlace, pide la nueva.
class RecuperarPantalla extends StatefulWidget {
  const RecuperarPantalla({super.key, this.token});

  final String? token;

  @override
  State<RecuperarPantalla> createState() => _RecuperarPantallaState();
}

class _RecuperarPantallaState extends State<RecuperarPantalla> {
  final _texto = TextEditingController();
  ErrorApi? _error;
  String? _listo;
  bool _enviando = false;

  bool get _definiendo => widget.token != null;

  @override
  void dispose() {
    _texto.dispose();
    super.dispose();
  }

  Future<void> _enviar() async {
    setState(() {
      _enviando = true;
      _error = null;
    });
    final api = context.read<Api>();
    try {
      if (_definiendo) {
        await api.post('/api/auth/recuperacion/confirmar', {'token': widget.token, 'contrasenaNueva': _texto.text});
        setState(() => _listo = 'Listo. Ya puedes ingresar con tu contraseña nueva.');
      } else {
        final respuesta = await api.post('/api/auth/recuperacion', {'correo': _texto.text.trim()}) as Map;
        setState(() => _listo = respuesta['mensaje'] as String);
      }
    } on ErrorApi catch (e) {
      setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(_definiendo ? 'Contraseña nueva' : 'Recuperar contraseña')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Contenido(
          ancho: 440,
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            if (_listo != null) ...[
              Text(_listo!, style: Theme.of(context).textTheme.bodyLarge),
              const SizedBox(height: 16),
              FilledButton(onPressed: () => context.go('/ingresar'), child: const Text('Ir a ingresar')),
            ] else ...[
              Text(_definiendo
                  ? 'Escribe tu contraseña nueva.'
                  : 'Escribe tu correo y te enviaremos un enlace para definir una contraseña nueva.'),
              const SizedBox(height: 16),
              if (_error != null && _error!.campos.isEmpty) AvisoError(_error!.mensaje),
              // CU12, alt. 2: si el enlace venció o ya se usó, se pide otro.
              if (_error?.codigo == 'ENLACE_VENCIDO')
                Padding(
                  padding: const EdgeInsets.only(bottom: 16),
                  child: OutlinedButton(onPressed: () => context.go('/recuperar'), child: const Text('Pedir otro enlace')),
                ),
              Campo(
                controlador: _texto,
                etiqueta: _definiendo ? 'Contraseña nueva' : 'Correo',
                nombre: _definiendo ? 'contrasenaNueva' : 'correo',
                errores: _error?.campos,
                oculto: _definiendo,
                teclado: _definiendo ? null : TextInputType.emailAddress,
                ayuda: _definiendo ? 'Al menos 8 caracteres, con una letra y un número.' : null,
              ),
              FilledButton(onPressed: _enviando ? null : _enviar, child: Text(_definiendo ? 'Guardar' : 'Enviar enlace')),
            ],
          ]),
        ),
      ),
    );
  }
}
