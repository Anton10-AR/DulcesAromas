import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/sesion.dart';
import '../../comun/widgets.dart';

/// Ingreso al Panel, solo para el personal (CU01, HU01).
class IngresarPanel extends StatefulWidget {
  const IngresarPanel({super.key});

  @override
  State<IngresarPanel> createState() => _IngresarPanelState();
}

class _IngresarPanelState extends State<IngresarPanel> {
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
      if (sesion.usuario!.esCliente) {
        await sesion.cerrar();
        setState(() => _error = ErrorApi(403, 'CUENTA_DE_CLIENTE', 'Esta cuenta es de cliente. Usa la Tienda para pedir.'));
      }
      // Si entró, el router lo lleva al inicio.
    } on ErrorApi catch (e) {
      setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final sesion = context.watch<Sesion>();
    final textos = Theme.of(context).textTheme;
    return Scaffold(
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Contenido(
            ancho: 400,
            child: AutofillGroup(
              child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                Text('Dulces Aromas', style: textos.headlineMedium, textAlign: TextAlign.center),
                Text('Panel del personal', style: textos.bodyLarge, textAlign: TextAlign.center),
                const SizedBox(height: 32),
                if (sesion.vencida && _error == null)
                  const AvisoError('Tu sesión se cerró tras 30 minutos sin uso. Ingresa de nuevo.'),
                if (_error != null && _error!.campos.isEmpty) AvisoError(_error!.mensaje),
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
                    autocompletar: const [AutofillHints.password]),
                FilledButton(onPressed: _enviando ? null : _ingresar, child: const Text('Ingresar')),
                const SizedBox(height: 16),
                // D5: al personal la contraseña se la restablece el administrador.
                Text('¿Olvidaste tu contraseña? Pide al administrador que la restablezca.',
                    style: textos.bodySmall, textAlign: TextAlign.center),
              ]),
            ),
          ),
        ),
      ),
    );
  }
}
