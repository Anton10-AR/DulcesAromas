import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/widgets.dart';

/// Detalle de un pedido del cliente: productos, entrega, pago e historial (CU15). Desde aquí se paga o se
/// cancela (CU16).
class PedidoPantalla extends StatefulWidget {
  const PedidoPantalla({super.key, required this.id});

  final int id;

  @override
  State<PedidoPantalla> createState() => _PedidoPantallaState();
}

class _PedidoPantallaState extends State<PedidoPantalla> {
  late Future<(Pedido, List<Pago>)> _datos;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  void _cargar() {
    setState(() {
      _datos = (
        _api.get('/api/pedidos/${widget.id}').then((j) => Pedido.deJson(j as Json)),
        _api.get('/api/pagos', {'pedido': '${widget.id}'}).then((j) => lista(j, Pago.deJson)),
      ).wait;
    });
  }

  /// HU20: pide el motivo y confirmación. Si ya estaba pagado, avisa que el reembolso es fuera del sistema.
  Future<void> _cancelar() async {
    final motivo = TextEditingController();
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (contexto) => AlertDialog(
        title: const Text('¿Cancelar el pedido?'),
        content: TextField(
          controller: motivo,
          autofocus: true,
          maxLength: 255,
          decoration: const InputDecoration(labelText: 'Motivo'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(contexto, false), child: const Text('No, volver')),
          FilledButton(onPressed: () => Navigator.pop(contexto, true), child: const Text('Sí, cancelar')),
        ],
      ),
    );
    if (confirmado != true || !mounted) return;
    try {
      final respuesta = await _api.post('/api/pedidos/${widget.id}/cancelar', {'motivo': motivo.text.trim()}) as Json;
      if (!mounted) return;
      avisar(context, respuesta['aviso'] as String? ?? 'Cancelamos tu pedido.');
      _cargar();
    } on ErrorApi catch (e) {
      if (mounted) avisar(context, e.campos['motivo'] ?? e.mensaje);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Pedido #${widget.id}')),
      body: Cargador(
        futuro: _datos,
        reintentar: _cargar,
        constructor: (context, datos) {
          final (pedido, pagos) = datos;
          return RefreshIndicator(
            onRefresh: () async => _cargar(),
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [Contenido(child: _detalle(pedido, pagos))],
            ),
          );
        },
      ),
    );
  }

  Widget _detalle(Pedido pedido, List<Pago> pagos) {
    final textos = Theme.of(context).textTheme;
    final vigente = pagos.lastOrNull;
    final debePagar = pedido.estado == 'PENDIENTE' && vigente?.estado == 'PENDIENTE_DE_PAGO';
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            Etiqueta(nombresEstadoPedido[pedido.estado]!, resaltada: true),
            if (vigente != null) Etiqueta('Pago: ${nombresEstadoPago[vigente.estado]}'),
          ],
        ),
        const SizedBox(height: 16),
        if (pedido.motivoCancelacion != null) Text('Motivo de la cancelación: ${pedido.motivoCancelacion}'),
        if (debePagar)
          FilledButton(onPressed: () => context.push('/pedidos/${pedido.id}/pagar'), child: const Text('Pagar')),
        const SizedBox(height: 16),
        Text('Productos', style: textos.titleLarge),
        for (final l in pedido.lineas)
          ListTile(
            contentPadding: EdgeInsets.zero,
            title: Text('${l.cantidad} × ${l.nombre}'),
            subtitle: l.opciones.isEmpty && l.mensaje == null
                ? null
                : Text(
                    [...l.opciones.map((o) => o.nombre), if (l.mensaje != null) 'Mensaje: «${l.mensaje}»'].join(' · '),
                  ),
            // El precio de la línea, del mismo tamaño que el envío y el total.
            trailing: Text(bolivianos(l.subtotal), style: textos.bodyLarge),
          ),
        if (pedido.esDelivery)
          Row(children: [const Text('Envío'), const Spacer(), Text(bolivianos(pedido.costoEnvio))]),
        Row(
          children: [
            Text('Total', style: textos.titleMedium),
            const Spacer(),
            Text(bolivianos(pedido.total), style: textos.titleMedium),
          ],
        ),
        const Divider(height: 32),
        Text(pedido.esDelivery ? 'Delivery' : 'Recojo en tienda', style: textos.titleLarge),
        if (pedido.direccion != null) Text(pedido.direccion!),
        Text('Teléfono: ${pedido.telefono}'),
        Text('Horario: ${fechaHora(pedido.horarioEntrega)}'),
        const Divider(height: 32),
        Text('Historial', style: textos.titleLarge),
        for (final c in pedido.historial)
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: const Icon(Icons.circle, size: 12),
            title: Text(nombresEstadoPedido[c.nuevo]!),
            subtitle: Text([fechaHora(c.fecha), if (c.observacion != null) c.observacion!].join(' · ')),
          ),
        if (pedido.cancelable) ...[
          const SizedBox(height: 16),
          OutlinedButton(onPressed: _cancelar, child: const Text('Cancelar pedido')),
        ],
      ],
    );
  }
}
