import 'dart:async';

import 'package:decimal/decimal.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/impresion/ticket.dart';
import '../../comun/modelos.dart';
import '../../comun/sesion.dart';
import '../../comun/widgets.dart';
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

  /// El cobro por QR que se muestra, y la consulta que espera a que la pasarela lo confirme.
  _CobroQr? _cobroQr;
  ErrorApi? _errorQr;
  Timer? _esperaQr;
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
    _esperaQr?.cancel();
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

  /// Cambia de método. Con QR abre un cobro nuevo y espera a que la pasarela lo confirme; con otro, deja de esperar.
  void _elegir(MetodoPago metodo) {
    if (!mounted || _metodo == metodo) return;
    _esperaQr?.cancel();
    setState(() {
      _metodo = metodo;
      _cobroQr = null;
      _errorQr = null;
    });
    if (metodo.esQr) _crearCobroQr();
  }

  Future<void> _crearCobroQr() async {
    _esperaQr?.cancel();
    setState(() {
      _cobroQr = null;
      _errorQr = null;
    });
    try {
      final cobro = _CobroQr.deJson(await _api.post('/api/pagos/qr', {'pedidoId': widget.pedidoId}) as Json);
      if (!mounted) return;
      setState(() => _cobroQr = cobro);
      // La pasarela avisa al sistema; aquí se consulta cada 3 segundos hasta ver el pago aprobado.
      _esperaQr = Timer.periodic(const Duration(seconds: 3), (_) => Api.enSegundoPlano(_revisarPagoQr));
    } on ErrorApi catch (e) {
      if (mounted) setState(() => _errorQr = e);
    }
  }

  Future<void> _revisarPagoQr() async {
    if (!mounted) return;
    if (_cobroQr != null && DateTime.now().isAfter(_cobroQr!.vence)) {
      _esperaQr?.cancel();
      setState(() {});
      return;
    }
    try {
      final pagos = lista(await _api.get('/api/pagos', {'pedido': '${widget.pedidoId}'}), Pago.deJson);
      final pagado = pagos.where((p) => p.estado == 'PAGADO').firstOrNull;
      if (pagado == null || !mounted) return;
      _esperaQr?.cancel();
      setState(() => _aprobado = pagado);
    } on ErrorApi {
      // Se intenta otra vez en la próxima vuelta.
    }
  }

  /// La dirección de la pasarela en este mismo sitio. En la web, el sitio desde el que se abrió la Tienda (así también
  /// funciona en local); en Android, el sitio publicado.
  String _urlPasarela(String codigo) {
    const sitio = String.fromEnvironment('SITIO_URL', defaultValue: 'https://dulces-aromas.onrender.com');
    return '${kIsWeb ? Uri.base.origin : sitio}/pasarela/$codigo';
  }

  /// El QR del cobro, al estilo de los QR de cobro de los negocios: solo el QR, el monto y el pedido.
  List<Widget> _pagoConQr() {
    final textos = Theme.of(context).textTheme;
    if (_errorQr != null) {
      return [
        const SizedBox(height: 16),
        AvisoError(_errorQr!.mensaje),
        OutlinedButton(onPressed: _crearCobroQr, child: const Text('Intentar de nuevo')),
      ];
    }
    final cobro = _cobroQr;
    if (cobro == null) return const [SizedBox(height: 24), Center(child: CircularProgressIndicator())];
    if (DateTime.now().isAfter(cobro.vence)) {
      return [
        const SizedBox(height: 24),
        Text('Este QR venció.', style: textos.titleMedium, textAlign: TextAlign.center),
        const SizedBox(height: 8),
        FilledButton.icon(
          onPressed: _crearCobroQr,
          icon: const Icon(Icons.refresh),
          label: const Text('Generar otro QR'),
        ),
      ];
    }
    // Blanco y café oscuro, también en el tema oscuro: así lo lee cualquier cámara.
    const cafe = Color(0xFF3A2A23);
    return [
      const SizedBox(height: 20),
      Center(
        child: Container(
          width: 320,
          padding: const EdgeInsets.fromLTRB(24, 20, 24, 20),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(20),
            boxShadow: const [BoxShadow(color: Color(0x33000000), blurRadius: 16, offset: Offset(0, 6))],
          ),
          child: Column(
            children: [
              Text(
                'Dulces Aromas',
                style: textos.titleLarge?.copyWith(color: cafe, fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 12),
              QrImageView(
                data: _urlPasarela(cobro.codigo),
                size: 240,
                backgroundColor: Colors.white,
                eyeStyle: const QrEyeStyle(eyeShape: QrEyeShape.square, color: cafe),
                dataModuleStyle: const QrDataModuleStyle(dataModuleShape: QrDataModuleShape.square, color: cafe),
                semanticsLabel: 'Código QR para pagar el pedido #${cobro.pedidoId}',
              ),
              const SizedBox(height: 12),
              Text(bolivianos(cobro.monto), style: textos.titleLarge?.copyWith(color: cafe)),
              Text(
                'Pedido #${cobro.pedidoId} · vence a las ${fechaHora(cobro.vence).substring(11)}',
                style: textos.bodySmall?.copyWith(color: const Color(0xFF5C463C)),
              ),
            ],
          ),
        ),
      ),
      const SizedBox(height: 16),
      Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2)),
          const SizedBox(width: 12),
          Text('Esperando el pago…', style: textos.titleMedium),
        ],
      ),
      const SizedBox(height: 4),
      Text('Escanéalo con la cámara de tu celular.', style: textos.bodyMedium, textAlign: TextAlign.center),
    ];
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
    // El ticket es del tótem: en el celular, el seguimiento ya hace de comprobante.
    final conTicket = context.watch<ModoTotem>().activo && BotonTicket.seOfrece(pago);
    const iconoSeguir = Icon(Icons.receipt_long_outlined);
    const textoSeguir = Text('Seguir mi pedido');
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
              // En el tótem, el seguimiento queda en pantalla hasta que vuelve solo al catálogo por inactividad.
              Wrap(
                alignment: WrapAlignment.center,
                spacing: 12,
                runSpacing: 12,
                children: [
                  if (conTicket)
                    BotonTicket(
                      principal: true,
                      pedido: () async => Pedido.deJson(await _api.get('/api/pedidos/${widget.pedidoId}') as Json),
                      pago: pago,
                    ),
                  if (conTicket)
                    OutlinedButton.icon(onPressed: _volverAlPedido, icon: iconoSeguir, label: textoSeguir)
                  else
                    FilledButton.icon(onPressed: _volverAlPedido, icon: iconoSeguir, label: textoSeguir),
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
    // En el tótem se paga sin comprobante: queda elegido el primer método automático (QR o tarjeta).
    if (_metodo == null && context.read<ModoTotem>().activo) {
      final automatico = metodos.where((m) => m.automatico).firstOrNull;
      if (automatico != null) WidgetsBinding.instance.addPostFrameCallback((_) => _elegir(automatico));
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
                      avatar: m.esQr
                          ? const Icon(Icons.qr_code_2)
                          : (m.automatico ? const Icon(Icons.credit_card) : null),
                      label: Text(m.nombre),
                      selected: _metodo == m,
                      // Mientras el lector espera la tarjeta, no se cambia de método.
                      onSelected: _fase != null ? null : (_) => _elegir(m),
                    ),
                ],
              ),
              if (_error?.campos['metodoPagoId'] != null)
                Text(_error!.campos['metodoPagoId']!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
              // El QR por cobro se dibuja abajo; el QR fijo que subía el administrador ya no se usa.
              if (_metodo != null && !_metodo!.esQr) ...[
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
              if (_metodo?.esQr ?? false)
                ..._pagoConQr()
              else if (_metodo?.automatico ?? false)
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

/// Un cobro por QR: el código secreto que va en el QR, el monto y hasta cuándo vale.
class _CobroQr {
  _CobroQr(this.codigo, this.pedidoId, this.monto, this.vence);

  factory _CobroQr.deJson(Json j) =>
      _CobroQr(j['codigo'] as String, j['pedidoId'] as int, decimalDe(j['monto']), instante(j['vence'])!);

  final String codigo;
  final int pedidoId;
  final Decimal monto;
  final DateTime vence;
}
