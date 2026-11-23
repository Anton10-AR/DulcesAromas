import 'dart:typed_data';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/sesion.dart';
import '../../comun/widgets.dart';
import '../carrito.dart';
import '../comprobante_demo.dart';
import '../totem.dart';
import 'comunes.dart';

/// Registrar el pago con su comprobante (CU06, HU11), o pagar con tarjeta de débito: el procesador aprueba el cobro y el
/// sistema lo verifica solo, sin comprobante ni revisión manual (tótem de la feria).
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

  /// El comprobante generado para una demostración, para mostrarlo antes de registrar el pago.
  Uint8List? _vistaPrevia;
  ErrorApi? _error;
  bool _enviando = false;

  /// El pago con tarjeta: leyendo la tarjeta, esperando al procesador, o aprobado.
  _FaseTarjeta? _fase;
  Pago? _aprobado;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    // En una demostración la referencia ya viene escrita; el comprobante generado lleva la misma.
    if (modoDemostracion) _referencia.text = referenciaDemo(widget.pedidoId);
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
      type: FileType.custom,
      allowedExtensions: const ['jpg', 'jpeg', 'png', 'pdf'],
    );
    if (archivo == null) return;
    final bytes = await archivo.readAsBytes();
    if (bytes.length > 5 * 1024 * 1024) {
      setState(
        () => _error = ErrorApi(
          0,
          'COMPROBANTE_INVALIDO',
          'El comprobante pesa más de 5 MB. Redúcelo e inténtalo de nuevo.',
        ),
      );
      return;
    }
    setState(() {
      _nombreComprobante = archivo.name;
      _comprobante = bytes;
      _vistaPrevia = null;
      _error = null;
    });
  }

  /// Solo en una compilación de demostración: dibuja el comprobante de este pago con el cliente, el monto del
  /// servidor y el método elegido, lo adjunta, completa la referencia si falta y descarga la imagen.
  Future<void> _generarDemo(Pago vigente) async {
    if (_metodo == null) {
      setState(
        () => _error = ErrorApi(400, 'DATOS_INVALIDOS', 'Revisa los campos marcados.', {
          'metodoPagoId': 'Elige el método de pago.',
        }),
      );
      return;
    }
    if (_referencia.text.trim().isEmpty) _referencia.text = referenciaDemo(widget.pedidoId);
    final cliente = context.read<Sesion>().usuario?.nombre ?? 'Cliente';
    final imagen = await comprobanteDemo(
      cliente: cliente,
      monto: vigente.monto,
      pedidoId: widget.pedidoId,
      metodo: _metodo!.nombre,
      referencia: _referencia.text.trim(),
      cuando: DateTime.now(),
    );
    if (!mounted) return;
    final nombre = 'comprobante-pedido-${widget.pedidoId}.png';
    setState(() {
      _nombreComprobante = nombre;
      _comprobante = imagen;
      _vistaPrevia = imagen;
      _error = null;
    });
    // En la web se descarga directo; en Android, el sistema pregunta dónde guardarla. Ya quedó adjunta, así que si
    // la descarga falla o se cancela, el pago se registra igual.
    try {
      await FilePicker.saveFile(fileName: nombre, bytes: imagen, mimeType: 'image/png');
    } catch (_) {}
  }

  /// Vuelve cerrando esta pantalla, para que el detalle del pedido sepa que debe recargarse. Si se entró por
  /// enlace directo, no hay pantalla debajo y se va al pedido.
  void _volverAlPedido() {
    if (context.canPop()) {
      context.pop();
    } else {
      context.go('/pedidos/${widget.pedidoId}');
    }
  }

  /// Pago con tarjeta: el lector espera la tarjeta y el procesador aprueba el cobro. El procesador de la feria es una
  /// simulación: no se lee ni se envía ningún dato de la tarjeta.
  Future<void> _pagarConTarjeta() async {
    setState(() {
      _fase = _FaseTarjeta.leyendo;
      _error = null;
    });
    await Future<void>.delayed(const Duration(milliseconds: 2500));
    if (!mounted) return;
    setState(() => _fase = _FaseTarjeta.procesando);
    try {
      final json = await _api.post('/api/pagos/tarjeta', {'pedidoId': widget.pedidoId});
      if (!mounted) return;
      setState(() {
        _aprobado = Pago.deJson(json as Json);
        _fase = null;
      });
    } on ErrorApi catch (e) {
      if (mounted) {
        setState(() {
          _error = e;
          _fase = null;
        });
      }
    }
  }

  /// Tótem: deja el carrito vacío y vuelve al catálogo para el siguiente visitante.
  void _nuevaCompra() {
    context.read<Carrito>().vaciar();
    context.go('/');
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
      await _api.multipart(
        'POST',
        '/api/pagos',
        campos: {
          'pedidoId': '${widget.pedidoId}',
          'metodoPagoId': '${_metodo!.id}',
          'referencia': _referencia.text.trim(),
        },
        nombreArchivo: 'comprobante',
        bytes: _comprobante!,
        archivo: _nombreComprobante!,
      );
      if (!mounted) return;
      avisar(context, 'Recibimos tu pago. Lo verificaremos pronto.');
      _volverAlPedido();
    } on ErrorApi catch (e) {
      setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Pagar el pedido #${widget.pedidoId}'), actions: accionesTienda(context)),
      body: _aprobado != null
          ? _pagoAprobado(_aprobado!)
          : Cargador(
              futuro: _datos,
              reintentar: _cargar,
              constructor: (context, datos) {
                final (pagos, metodos) = datos;
                final vigente = pagos.last;
                if (vigente.estado != 'PENDIENTE_DE_PAGO') {
                  return Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text('Este pedido ya no espera un pago: ${nombresEstadoPago[vigente.estado]}.'),
                        const SizedBox(height: 8),
                        OutlinedButton(onPressed: _volverAlPedido, child: const Text('Ver el pedido')),
                      ],
                    ),
                  );
                }
                final rechazado = pagos.length > 1 ? pagos[pagos.length - 2] : null;
                return _formulario(vigente, rechazado, metodos);
              },
            ),
    );
  }

  /// El pago con tarjeta se aprobó: el pedido ya está confirmado, sin esperar a nadie.
  Widget _pagoAprobado(Pago pago) {
    final textos = Theme.of(context).textTheme;
    final colores = Theme.of(context).colorScheme;
    final totem = context.read<ModoTotem>().activo;
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Contenido(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.check_circle, size: 96, color: colores.primary),
              const SizedBox(height: 16),
              Text('¡Pago aprobado!', style: textos.headlineMedium, textAlign: TextAlign.center),
              const SizedBox(height: 8),
              Text(
                'Tu pedido #${widget.pedidoId} quedó confirmado y ya pasó a la cocina.',
                style: textos.titleMedium,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 16),
              Text(bolivianos(pago.monto), style: textos.headlineSmall),
              Text('${pago.metodo} · Autorización ${pago.referencia}', style: textos.bodyMedium),
              const SizedBox(height: 4),
              Text('Procesador de demostración: no se cobró dinero real.', style: textos.bodySmall),
              const SizedBox(height: 24),
              Wrap(
                alignment: WrapAlignment.center,
                spacing: 12,
                runSpacing: 12,
                children: [
                  OutlinedButton.icon(
                    onPressed: _volverAlPedido,
                    icon: const Icon(Icons.receipt_long_outlined),
                    label: const Text('Seguir mi pedido'),
                  ),
                  if (totem)
                    FilledButton.icon(
                      onPressed: _nuevaCompra,
                      icon: const Icon(Icons.storefront_outlined),
                      label: const Text('Terminar'),
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// El lector de tarjeta: el botón para pagar y, mientras tanto, lo que está pasando.
  List<Widget> _pagoConTarjeta(Pago vigente) {
    final textos = Theme.of(context).textTheme;
    final colores = Theme.of(context).colorScheme;
    if (_fase == null) {
      return [
        const SizedBox(height: 24),
        FilledButton.icon(
          onPressed: _pagarConTarjeta,
          style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(64)),
          icon: const Icon(Icons.credit_card, size: 28),
          label: Text('Pagar ${bolivianos(vigente.monto)} con tarjeta'),
        ),
      ];
    }
    final leyendo = _fase == _FaseTarjeta.leyendo;
    return [
      const SizedBox(height: 24),
      Card(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            children: [
              Icon(leyendo ? Icons.contactless_outlined : Icons.sync, size: 72, color: colores.primary),
              const SizedBox(height: 12),
              Text(
                leyendo ? 'Acerca o inserta tu tarjeta en el lector' : 'Procesando el pago con el banco…',
                style: textos.titleLarge,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 16),
              const LinearProgressIndicator(),
            ],
          ),
        ),
      ),
    ];
  }

  Widget _formulario(Pago vigente, Pago? rechazado, List<MetodoPago> metodos) {
    final textos = Theme.of(context).textTheme;
    // En el tótem se paga con tarjeta: queda elegida si está activa.
    if (_metodo == null && context.read<ModoTotem>().activo) {
      _metodo = metodos.where((m) => m.automatico).firstOrNull;
    }
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Contenido(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
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
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  for (final m in metodos)
                    ChoiceChip(
                      avatar: m.automatico ? const Icon(Icons.credit_card) : null,
                      label: Text(m.nombre),
                      selected: _metodo == m,
                      // Mientras el lector espera la tarjeta, no se cambia de método.
                      onSelected: _fase != null ? null : (_) => setState(() => _metodo = m),
                    ),
                ],
              ),
              if (_error?.campos['metodoPagoId'] != null)
                Text(_error!.campos['metodoPagoId']!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
              if (_metodo != null) ...[
                const SizedBox(height: 12),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        SelectableText(_metodo!.instrucciones, style: textos.bodyLarge),
                        if (_metodo!.qr != null) ...[
                          const SizedBox(height: 12),
                          Center(
                            child: Image.network(
                              _api.absoluta(_metodo!.qr!),
                              width: 240,
                              height: 240,
                              semanticLabel: 'Código QR para pagar',
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                ),
              ],
              if (_metodo?.automatico ?? false)
                ..._pagoConTarjeta(vigente)
              else ...[
                const SizedBox(height: 24),
                Text('2. Registra tu pago', style: textos.titleMedium),
                const SizedBox(height: 8),
                Campo(
                  controlador: _referencia,
                  etiqueta: 'Referencia o número de operación',
                  nombre: 'referencia',
                  errores: _error?.campos,
                  maximo: 60,
                ),
                OutlinedButton.icon(
                  onPressed: _elegirComprobante,
                  icon: const Icon(Icons.attach_file),
                  label: Text(_nombreComprobante ?? 'Adjuntar comprobante (JPG, PNG o PDF, hasta 5 MB)'),
                ),
                if (modoDemostracion) ...[
                  const SizedBox(height: 8),
                  OutlinedButton.icon(
                    onPressed: () => _generarDemo(vigente),
                    icon: const Icon(Icons.qr_code_2),
                    label: const Text('Generar comprobante de demostración'),
                  ),
                ],
                if (_vistaPrevia != null) ...[
                  const SizedBox(height: 12),
                  Center(
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(12),
                      child: Image.memory(
                        _vistaPrevia!,
                        height: 420,
                        semanticLabel: 'Comprobante de demostración generado',
                      ),
                    ),
                  ),
                ],
                if (_error?.campos['comprobante'] != null)
                  Text(_error!.campos['comprobante']!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
                const SizedBox(height: 24),
                FilledButton(onPressed: _enviando ? null : _registrar, child: const Text('Registrar pago')),
              ],
            ],
          ],
        ),
      ),
    );
  }
}

enum _FaseTarjeta { leyendo, procesando }
