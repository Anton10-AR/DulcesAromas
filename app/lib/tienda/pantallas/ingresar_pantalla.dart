import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/sesion.dart';
import '../../comun/widgets.dart';

/// Iniciar sesión en la Tienda (CU01, HU01). Al entrar vuelve a donde estaba, con el carrito intacto (HU10).
class IngresarPantalla extends StatefulWidget {
  const IngresarPantalla({super.key, this.volver});

  final String? volver;

  @override
  State<IngresarPantalla> createState() => _IngresarPantallaState();
}

class _IngresarPantallaState extends State<IngresarPantalla> {
  final _correo = TextEditingController();
  final _contrasena = TextEditingController();
  ErrorApi? _error;
  bool _enviando = false;

  @override
  void dispose() {
    _correo.dispose();
    _contrasena.dispose();
    super.dispose();
  }

  Future<void> _ingresar() async {
    setState(() {
      _enviando = true;
      _error = null;
    });
    final sesion = context.read<Sesion>();
    try {
      await sesion.iniciar(_correo.text.trim(), _contrasena.text);
      if (!mounted) return;
      if (!sesion.usuario!.esCliente) {
        await sesion.cerrar();
        setState(
          () => _error = ErrorApi(
            403,
            'CUENTA_DE_PERSONAL',
            'Esta cuenta es del personal. Ingresa desde el Panel de administración.',
          ),
        );
        return;
      }
      context.go(sesion.debeCambiarContrasena ? '/perfil' : (widget.volver ?? '/'));
    } on ErrorApi catch (e) {
      setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final volver = widget.volver == null ? '' : '?volver=${Uri.encodeQueryComponent(widget.volver!)}';
    return Scaffold(
      appBar: AppBar(title: const Text('Ingresar')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Contenido(
          ancho: 440,
          child: AutofillGroup(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                if (widget.volver != null)
                  const Padding(
                    padding: EdgeInsets.only(bottom: 16),
                    child: Text('Para hacer tu pedido, ingresa o crea una cuenta. Tu carrito se conserva.'),
                  ),
                if (_error != null && _error!.campos.isEmpty) AvisoError(_error!.mensaje),
                Campo(
                  controlador: _correo,
                  etiqueta: 'Correo',
                  nombre: 'correo',
                  errores: _error?.campos,
                  teclado: TextInputType.emailAddress,
                  autocompletar: const [AutofillHints.email],
                ),
                Campo(
                  controlador: _contrasena,
                  etiqueta: 'Contraseña',
                  nombre: 'contrasena',
                  errores: _error?.campos,
                  oculto: true,
                  autocompletar: const [AutofillHints.password],
                ),
                FilledButton(onPressed: _enviando ? null : _ingresar, child: const Text('Ingresar')),
                const SizedBox(height: 8),
                TextButton(onPressed: () => context.push('/recuperar'), child: const Text('¿Olvidaste tu contraseña?')),
                const Divider(height: 32),
                OutlinedButton(onPressed: () => context.go('/registro$volver'), child: const Text('Crear una cuenta')),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
