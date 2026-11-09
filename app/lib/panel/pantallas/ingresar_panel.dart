import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/logo.dart';
import '../../comun/marca.dart';
import '../../comun/sesion.dart';
import '../../comun/tema.dart';
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
        setState(
          () => _error = ErrorApi(403, 'CUENTA_DE_CLIENTE', 'Esta cuenta es de cliente. Usa la Tienda para pedir.'),
        );
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
    final formulario = Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Contenido(
          ancho: 400,
          child: AutofillGroup(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text('Ingresar al Panel', style: textos.headlineMedium),
                const SizedBox(height: 4),
                Text('Usa la cuenta que te dio el administrador.', style: textos.bodyMedium),
                const SizedBox(height: 28),
                if (sesion.vencida && _error == null)
                  const AvisoError('Tu sesión se cerró tras 30 minutos sin uso. Ingresa de nuevo.'),
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
                const SizedBox(height: 16),
                // D5: al personal la contraseña se la restablece el administrador.
                Text(
                  '¿Olvidaste tu contraseña? Pide al administrador que la restablezca.',
                  style: textos.bodySmall,
                  textAlign: TextAlign.center,
                ),
              ],
            ),
          ),
        ),
      ),
    );
    return Scaffold(
      body: LayoutBuilder(
        builder: (context, limites) {
          // En pantallas anchas, las fotos a la izquierda y el formulario a la derecha; en angostas, una franja arriba.
          if (limites.maxWidth >= 900) {
            return Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Expanded(flex: 5, child: _Fotos(anchoLogo: 380)),
                Expanded(flex: 4, child: formulario),
              ],
            );
          }
          return Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const SizedBox(height: 230, child: _Fotos(anchoLogo: 260)),
              Expanded(child: formulario),
            ],
          );
        },
      ),
    );
  }
}

/// Cuatro fotos de postres, oscurecidas, con el logo y «Panel del personal» encima.
class _Fotos extends StatelessWidget {
  const _Fotos({required this.anchoLogo});

  final double anchoLogo;

  @override
  Widget build(BuildContext context) {
    final fotos = fotosPortada.take(4).map((f) => f.$1).toList();
    Widget foto(String id) => Expanded(child: Image.asset(rutaFoto(id), fit: BoxFit.cover));
    return Stack(
      fit: StackFit.expand,
      children: [
        ExcludeSemantics(
          child: Column(
            children: [
              for (final fila in [fotos.sublist(0, 2), fotos.sublist(2)])
                Expanded(
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [for (final id in fila) foto(id)],
                  ),
                ),
            ],
          ),
        ),
        veloSobreFotos,
        Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                SizedBox(
                  width: anchoLogo,
                  child: const LogoDulcesAromas(color: Colors.white, acento: Colores.rosaClaro),
                ),
                const SizedBox(height: 12),
                Text(
                  'PANEL DEL PERSONAL',
                  style: Theme.of(context).textTheme.labelLarge?.copyWith(color: Colors.white, letterSpacing: 2),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
