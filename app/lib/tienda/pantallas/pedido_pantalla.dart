import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/actualizacion.dart';
import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/widgets.dart';
import '../carrito.dart';
import '../totem.dart';
import 'comunes.dart';
import 'progreso_pedido.dart';

/// Detalle de un pedido del cliente: productos, entrega, pago e historial (CU15). Desde aquí se paga o se
/// cancela (CU16).
class PedidoPantalla extends StatefulWidget {
  const PedidoPantalla({super.key, required this.id});

  final int id;

  @override
  State<PedidoPantalla> createState() => _PedidoPantallaState();
}

class _PedidoPantallaState extends State<PedidoPantalla> with ActualizacionPeriodica {
  late Future<(Pedido, List<Pago>)> _datos;

  /// Entregado o Cancelado: ya no cambia, así que deja de consultar.
  bool _terminado = false;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  /// Al volver de la pantalla de pago se recarga: el pago pudo pasar a En verificación (HU11).
  Future<void> _pagar(int pedidoId) async {
    await context.push('/pedidos/$pedidoId/pagar');
    if (mounted) _cargar();
  }

  Future<(Pedido, List<Pago>)> _consultar() => (
    _api.get('/api/pedidos/${widget.id}').then((j) => Pedido.deJson(j as Json)),
    _api.get('/api/pagos', {'pedido': '${widget.id}'}).then((j) => lista(j, Pago.deJson)),
  ).wait;

  void _cargar() {
    setState(() {
      _datos = _consultar();
    });
  }

  @override
  bool get siguePorCambiar => !_terminado;

  /// CU15: el estado del pedido y del pago se ve cambiar sin deslizar para actualizar.
  @override
  Future<void> actualizarEnSilencio() async {
    try {
      final nuevos = await _consultar();
      if (!mounted) return;
      setState(() {
        _datos = Future.value(nuevos);
      });
    } catch (_) {
      // Sin conexión o con la API despertando: se intenta en la próxima vuelta y queda lo que ya se ve.
    }
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
      appBar: AppBar(title: Text('Pedido #${widget.id}'), actions: accionesTienda(context)),
      body: Cargador(
        futuro: _datos,
        reintentar: _cargar,
        constructor: (context, datos) {
          final (pedido, pagos) = datos;
          _terminado = const ['ENTREGADO', 'CANCELADO'].contains(pedido.estado);
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

  /// Tótem: vacía el carrito y vuelve al catálogo para el siguiente visitante.
  void _nuevoPedido() {
    context.read<Carrito>().vaciar();
    context.go('/');
  }

  Widget _detalle(Pedido pedido, List<Pago> pagos) {
    final textos = Theme.of(context).textTheme;
    final colores = Theme.of(context).colorScheme;
    final vigente = pagos.lastOrNull;
    final debePagar = pedido.estado == 'PENDIENTE' && vigente?.estado == 'PENDIENTE_DE_PAGO';
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // Resumen: estados, total y cuándo se pidió.
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [EtiquetaEstado(pedido.estado), if (vigente != null) EtiquetaPago(vigente.estado)],
              ),
            ),
            const SizedBox(width: 12),
            Text(bolivianos(pedido.total), style: textos.headlineSmall),
          ],
        ),
        const SizedBox(height: 4),
        Text('Pedido el ${fechaHora(pedido.fechaCreacion)}', style: textos.bodySmall),
        const SizedBox(height: 16),
        // Tótem: el visitante termina aquí; el siguiente empieza su propio pedido con un toque.
        if (context.watch<ModoTotem>().activo) ...[
          FilledButton.icon(
            onPressed: _nuevoPedido,
            style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(56)),
            icon: const Icon(Icons.add_shopping_cart),
            label: const Text('Nuevo pedido'),
          ),
          const SizedBox(height: 4),
          Text(
            'Muestra el número #${pedido.id} en el stand para recibir tu pedido.',
            style: textos.bodyMedium,
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 16),
        ],
        _Seccion(
          titulo: 'Seguimiento',
          children: [
            SeguimientoPedido(
              estado: pedido.estado,
              delivery: pedido.esDelivery,
              historial: pedido.historial,
              horarioEntrega: pedido.horarioEntrega,
              mensaje: queSigue(pedido.estado, vigente?.estado, delivery: pedido.esDelivery),
              motivoCancelacion: pedido.motivoCancelacion,
            ),
            if (debePagar) ...[
              const SizedBox(height: 12),
              FilledButton.icon(
                onPressed: () => _pagar(pedido.id),
                icon: const Icon(Icons.payments_outlined),
                label: const Text('Pagar'),
              ),
            ],
          ],
        ),
        _Seccion(
          titulo: 'Productos',
          children: [
            for (final l in pedido.lineas)
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: CircleAvatar(
                  backgroundColor: colores.secondaryContainer,
                  foregroundColor: colores.onSecondaryContainer,
                  child: Text('${l.cantidad}'),
                ),
                title: Text(l.nombre),
                subtitle: l.opciones.isEmpty && l.mensaje == null
                    ? null
                    : Text(
                        [
                          ...l.opciones.map((o) => o.nombre),
                          if (l.mensaje != null) 'Mensaje: «${l.mensaje}»',
                        ].join(' · '),
                      ),
                // El precio de la línea, del mismo tamaño que el envío y el total.
                trailing: Text(bolivianos(l.subtotal), style: textos.bodyLarge),
              ),
            const Divider(),
            if (pedido.esDelivery)
              Row(children: [const Text('Envío'), const Spacer(), Text(bolivianos(pedido.costoEnvio))]),
            Row(
              children: [
                Text('Total', style: textos.titleMedium),
                const Spacer(),
                Text(bolivianos(pedido.total), style: textos.titleMedium),
              ],
            ),
          ],
        ),
        _Seccion(
          titulo: pedido.esDelivery ? 'Delivery' : 'Recojo en tienda',
          children: [
            _Dato(
              icono: pedido.esDelivery ? Icons.place_outlined : Icons.storefront_outlined,
              texto: pedido.direccion ?? 'Sucursal centro',
            ),
            _Dato(icono: Icons.phone_outlined, texto: pedido.telefono),
            _Dato(icono: Icons.schedule_outlined, texto: fechaHora(pedido.horarioEntrega)),
          ],
        ),
        if (pedido.cancelable) ...[
          const SizedBox(height: 8),
          Align(
            alignment: Alignment.centerLeft,
            child: OutlinedButton.icon(
              onPressed: _cancelar,
              icon: const Icon(Icons.close),
              label: const Text('Cancelar pedido'),
            ),
          ),
        ],
      ],
    );
  }
}

/// Una tarjeta con título, para cada parte del pedido.
class _Seccion extends StatelessWidget {
  const _Seccion({required this.titulo, required this.children});

  final String titulo;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(titulo, style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 12),
            ...children,
          ],
        ),
      ),
    );
  }
}

/// Un dato de la entrega con su ícono.
class _Dato extends StatelessWidget {
  const _Dato({required this.icono, required this.texto});

  final IconData icono;
  final String texto;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icono, size: 20, color: Theme.of(context).colorScheme.onSurfaceVariant),
          const SizedBox(width: 12),
          Expanded(child: Text(texto, style: Theme.of(context).textTheme.bodyLarge)),
        ],
      ),
    );
  }
}

/// Qué sigue con el pedido, en palabras del cliente: qué tiene que hacer él o qué está haciendo la pastelería.
String queSigue(String estado, String? pago, {required bool delivery}) => switch (estado) {
  'PENDIENTE' when pago == 'EN_VERIFICACION' =>
    'Estamos revisando tu pago. Cuando lo aprobemos, tu pedido quedará confirmado.',
  'PENDIENTE' => 'Paga y registra el comprobante para que confirmemos tu pedido.',
  'CONFIRMADO' => 'Tu pago está aprobado. Pronto empezamos a preparar tu pedido.',
  'EN_PREPARACION' => 'Estamos preparando tu pedido.',
  'LISTO' when delivery => 'Tu pedido está listo y pronto sale a entregarse.',
  'LISTO' => 'Tu pedido está listo: puedes pasar a recogerlo.',
  'EN_CAMINO' => 'Tu pedido va en camino.',
  'ENTREGADO' => 'Pedido entregado. ¡Gracias por tu compra!',
  'CANCELADO' => 'Este pedido se canceló.',
  _ => '',
};
