import 'package:decimal/decimal.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/widgets.dart';

/// Un cobro como lo ve la pasarela: el comercio, el pedido y el monto. Nunca trae datos del cliente.
class CobroPasarela {
  CobroPasarela(this.comercio, this.pedidoId, this.monto, this.vence, this.estado, this.referencia);

  factory CobroPasarela.deJson(Json j) => CobroPasarela(
    j['comercio'] as String,
    j['pedidoId'] as int,
    decimalDe(j['monto']),
    instante(j['vence'])!,
    j['estado'] as String,
    j['referencia'] as String?,
  );

  final String comercio;
  final int pedidoId;
  final Decimal monto;
  final DateTime vence;

  /// PENDIENTE, PAGADO, VENCIDO o CERRADO.
  final String estado;
  final String? referencia;
}

/// `/pasarela/<código>`: la pasarela de pagos simulada que se abre al escanear el QR de un cobro con el celular. Hace
/// de banco: el visitante confirma y el sistema verifica el pago solo. Es pública y no mueve dinero real.
class PasarelaPantalla extends StatefulWidget {
  const PasarelaPantalla({super.key, required this.codigo});

  final String codigo;

  @override
  State<PasarelaPantalla> createState() => _PasarelaPantallaState();
}

class _PasarelaPantallaState extends State<PasarelaPantalla> {
  late Future<CobroPasarela> _cobro;
  bool _enviando = false;
  bool _cancelado = false;
  ErrorApi? _error;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  void _cargar() {
    setState(() {
      _cobro = _api.get('/api/pasarela/${widget.codigo}').then((j) => CobroPasarela.deJson(j as Json));
    });
  }

  Future<void> _confirmar() async {
    setState(() {
      _enviando = true;
      _error = null;
    });
    try {
      final cobro = CobroPasarela.deJson(await _api.post('/api/pasarela/${widget.codigo}/confirmar') as Json);
      if (!mounted) return;
      setState(() {
        _cobro = Future.value(cobro);
      });
    } on ErrorApi catch (e) {
      if (!mounted) return;
      setState(() => _error = e);
      _cargar();
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Pasarela de pago'), automaticallyImplyLeading: false),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Contenido(
          ancho: 520,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const _AvisoSimulacion(),
              const SizedBox(height: 16),
              Cargador(futuro: _cobro, reintentar: _cargar, constructor: (context, cobro) => _contenido(cobro)),
              const SizedBox(height: 24),
              Text(
                'Esta página simula la pasarela de pagos de un banco para la feria EXPOSOFT 26. Al confirmar, avisa '
                'al sistema de Dulces Aromas y el pago se verifica solo. No se cobra ni se transfiere dinero.',
                style: textos.bodySmall,
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _contenido(CobroPasarela cobro) {
    final textos = Theme.of(context).textTheme;
    final colores = Theme.of(context).colorScheme;
    if (cobro.estado == 'PAGADO') {
      return _Resultado(
        icono: Icons.check_circle,
        color: colores.primary,
        titulo: 'Pago confirmado',
        texto:
            'Vuelve a la pantalla de Dulces Aromas: tu pedido #${cobro.pedidoId} ya está confirmado.'
            '${cobro.referencia == null ? '' : '\nReferencia: ${cobro.referencia}'}',
      );
    }
    if (_cancelado) {
      return const _Resultado(
        icono: Icons.cancel_outlined,
        titulo: 'Pago cancelado',
        texto: 'No se registró ningún pago. Puedes cerrar esta página.',
      );
    }
    if (cobro.estado == 'VENCIDO' || cobro.estado == 'CERRADO') {
      return _Resultado(
        icono: Icons.timer_off_outlined,
        titulo: cobro.estado == 'VENCIDO' ? 'Este QR venció' : 'Este cobro ya no está disponible',
        texto: cobro.estado == 'VENCIDO'
            ? 'Pide un QR nuevo en la pantalla de pago de Dulces Aromas.'
            : 'El pedido ya no espera este pago.',
      );
    }
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _Fila('Pagar a', cobro.comercio),
            _Fila('Concepto', 'Pedido #${cobro.pedidoId}'),
            const Divider(height: 24),
            Text('Monto', style: textos.bodyMedium),
            Text(bolivianos(cobro.monto), style: textos.headlineMedium),
            const SizedBox(height: 4),
            Text('Válido hasta las ${fechaHora(cobro.vence).substring(11)}', style: textos.bodySmall),
            if (_error != null) ...[const SizedBox(height: 12), AvisoError(_error!.mensaje)],
            const SizedBox(height: 20),
            FilledButton(
              onPressed: _enviando ? null : _confirmar,
              style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(56)),
              child: Text(_enviando ? 'Confirmando…' : 'Confirmar pago'),
            ),
            const SizedBox(height: 8),
            TextButton(
              onPressed: _enviando ? null : () => setState(() => _cancelado = true),
              child: const Text('Cancelar'),
            ),
          ],
        ),
      ),
    );
  }
}

/// La franja que deja claro, siempre a la vista, que no hay dinero de por medio.
class _AvisoSimulacion extends StatelessWidget {
  const _AvisoSimulacion();

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(color: colores.tertiaryContainer, borderRadius: BorderRadius.circular(12)),
      child: Row(
        children: [
          Icon(Icons.info_outline, color: colores.onTertiaryContainer),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              'SIMULACIÓN · No se mueve dinero real',
              style: Theme.of(context).textTheme.titleSmall
                  ?.copyWith(color: colores.onTertiaryContainer, fontWeight: FontWeight.w700),
            ),
          ),
        ],
      ),
    );
  }
}

class _Fila extends StatelessWidget {
  const _Fila(this.etiqueta, this.valor);

  final String etiqueta;
  final String valor;

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          Expanded(child: Text(etiqueta, style: textos.bodyMedium)),
          Text(valor, style: textos.titleMedium),
        ],
      ),
    );
  }
}

class _Resultado extends StatelessWidget {
  const _Resultado({required this.icono, required this.titulo, required this.texto, this.color});

  final IconData icono;
  final String titulo;
  final String texto;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          children: [
            Icon(icono, size: 72, color: color ?? Theme.of(context).colorScheme.onSurfaceVariant),
            const SizedBox(height: 12),
            Text(titulo, style: textos.headlineSmall, textAlign: TextAlign.center),
            const SizedBox(height: 8),
            Text(texto, style: textos.bodyLarge, textAlign: TextAlign.center),
          ],
        ),
      ),
    );
  }
}
