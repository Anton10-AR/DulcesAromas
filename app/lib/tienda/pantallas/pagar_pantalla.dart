import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/widgets.dart';

/// Registrar el pago con su comprobante (CU06, HU11).
class PagarPantalla extends StatefulWidget {
  const PagarPantalla({super.key, required this.pedidoId});

  final int pedidoId;

  @override
  State<PagarPantalla> createState() => _PagarPantallaState();
}

class _PagarPantallaState extends State<PagarPantalla> {
  late Future<(List<Pago>, List<MetodoPago>)> _datos;
  MetodoPago? _metodo;
  final _referencia = TextEditingController();
  String? _nombreComprobante;
  List<int>? _comprobante;
  ErrorApi? _error;
  bool _enviando = false;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  @override
  void dispose() {
    _referencia.dispose();
    super.dispose();
  }

  void _cargar() {
    setState(() {
      _datos = (
        _api.get('/api/pagos', {'pedido': '${widget.pedidoId}'}).then((j) => lista(j, Pago.deJson)),
        _api.get('/api/metodos-pago').then((j) => lista(j, MetodoPago.deJson)),
      ).wait;
    });
  }

  /// RNF09: JPG, PNG o PDF de hasta 5 MB. El servidor lo vuelve a validar.
  Future<void> _elegirComprobante() async {
    final archivo = await FilePicker.pickFile(
        type: FileType.custom, allowedExtensions: const ['jpg', 'jpeg', 'png', 'pdf']);
    if (archivo == null) return;
    final bytes = await archivo.readAsBytes();
    if (bytes.length > 5 * 1024 * 1024) {
      setState(() => _error = ErrorApi(0, 'COMPROBANTE_INVALIDO', 'El comprobante pesa más de 5 MB. Redúcelo e inténtalo de nuevo.'));
      return;
    }
    setState(() {
      _nombreComprobante = archivo.name;
      _comprobante = bytes;
      _error = null;
    });
  }

  Future<void> _registrar() async {
    final campos = <String, String>{
      if (_metodo == null) 'metodoPagoId': 'Elige el método de pago.',
      if (_comprobante == null) 'comprobante': 'Adjunta el comprobante.',
    };
    if (campos.isNotEmpty) {
      setState(() => _error = ErrorApi(400, 'DATOS_INVALIDOS', 'Revisa los campos marcados.', campos));
      return;
    }
    setState(() {
      _enviando = true;
      _error = null;
    });
    try {
      await _api.multipart('POST', '/api/pagos',
          campos: {
            'pedidoId': '${widget.pedidoId}',
            'metodoPagoId': '${_metodo!.id}',
            'referencia': _referencia.text.trim(),
          },
          nombreArchivo: 'comprobante',
          bytes: _comprobante!,
          archivo: _nombreComprobante!);
      if (!mounted) return;
      avisar(context, 'Recibimos tu pago. Lo verificaremos pronto.');
      context.go('/pedidos/${widget.pedidoId}');
    } on ErrorApi catch (e) {
      setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Pagar el pedido #${widget.pedidoId}')),
      body: Cargador(
        futuro: _datos,
        reintentar: _cargar,
        constructor: (context, datos) {
          final (pagos, metodos) = datos;
          final vigente = pagos.last;
          if (vigente.estado != 'PENDIENTE_DE_PAGO') {
            return Center(
              child: Column(mainAxisSize: MainAxisSize.min, children: [
                Text('Este pedido ya no espera un pago: ${nombresEstadoPago[vigente.estado]}.'),
                const SizedBox(height: 8),
                OutlinedButton(
                    onPressed: () => context.go('/pedidos/${widget.pedidoId}'), child: const Text('Ver el pedido')),
              ]),
            );
          }
          final rechazado = pagos.length > 1 ? pagos[pagos.length - 2] : null;
          return _formulario(vigente, rechazado, metodos);
        },
      ),
    );
  }

  Widget _formulario(Pago vigente, Pago? rechazado, List<MetodoPago> metodos) {
    final textos = Theme.of(context).textTheme;
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Contenido(
        child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Text('Monto a pagar', style: textos.titleMedium),
          Text(bolivianos(vigente.monto), style: textos.headlineMedium),
          const SizedBox(height: 16),
          // HU11: si el pago anterior fue rechazado, el cliente ve por qué y registra otro comprobante.
          if (rechazado?.verificacion?.observacion != null)
            AvisoError('Rechazamos tu comprobante anterior: ${rechazado!.verificacion!.observacion}'),
          if (_error != null && _error!.campos.isEmpty) AvisoError(_error!.mensaje),
          if (metodos.isEmpty)
            const AvisoError('Por ahora no hay métodos de pago disponibles. Comunícate con la pastelería.')
          else ...[
            Text('1. Paga con tu banco', style: textos.titleMedium),
            const SizedBox(height: 8),
            Wrap(spacing: 8, children: [
              for (final m in metodos)
                ChoiceChip(label: Text(m.nombre), selected: _metodo == m, onSelected: (_) => setState(() => _metodo = m)),
            ]),
            if (_error?.campos['metodoPagoId'] != null)
              Text(_error!.campos['metodoPagoId']!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
            if (_metodo != null) ...[
              const SizedBox(height: 12),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    SelectableText(_metodo!.instrucciones, style: textos.bodyLarge),
                    if (_metodo!.qr != null) ...[
                      const SizedBox(height: 12),
                      Center(
                        child: Image.network(_api.absoluta(_metodo!.qr!),
                            width: 240, height: 240, semanticLabel: 'Código QR para pagar'),
                      ),
                    ],
                  ]),
                ),
              ),
            ],
            const SizedBox(height: 24),
            Text('2. Registra tu pago', style: textos.titleMedium),
            const SizedBox(height: 8),
            Campo(
                controlador: _referencia,
                etiqueta: 'Referencia o número de operación',
                nombre: 'referencia',
                errores: _error?.campos,
                maximo: 60),
            OutlinedButton.icon(
              onPressed: _elegirComprobante,
              icon: const Icon(Icons.attach_file),
              label: Text(_nombreComprobante ?? 'Adjuntar comprobante (JPG, PNG o PDF, hasta 5 MB)'),
            ),
            if (_error?.campos['comprobante'] != null)
              Text(_error!.campos['comprobante']!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
            const SizedBox(height: 24),
            FilledButton(onPressed: _enviando ? null : _registrar, child: const Text('Registrar pago')),
          ],
        ]),
      ),
    );
  }
}
