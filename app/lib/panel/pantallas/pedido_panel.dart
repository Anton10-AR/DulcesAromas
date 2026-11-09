import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/sesion.dart';
import '../../comun/widgets.dart';
import 'dialogos.dart';
import 'formulario.dart';

/// Detalle de un pedido en el Panel con las acciones que permite el rol (CU08, CU10, CU16).
class PedidoPanel extends StatefulWidget {
  const PedidoPanel({super.key, required this.id});

  final int id;

  @override
  State<PedidoPanel> createState() => _PedidoPanelState();
}

class _PedidoPanelState extends State<PedidoPanel> {
  late Future<(Pedido, List<Pago>, Entrega?)> _datos;
  List<Persona>? _repartidores;
  bool _ocupado = false;

  Api get _api => context.read<Api>();

  Usuario get _usuario => context.read<Sesion>().usuario!;

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
        _entrega(),
      ).wait;
    });
    if (_usuario.puede('DISTRIBUCION_ASIGNAR') && _repartidores == null) {
      _api.get('/api/entregas/repartidores').then((j) => setState(() => _repartidores = lista(j, Persona.deJson)));
    }
  }

  /// La entrega del pedido, si tiene repartidor y el usuario puede verla.
  Future<Entrega?> _entrega() async {
    if (!_usuario.puede('DISTRIBUCION_ASIGNAR')) return null;
    try {
      return Entrega.deJson(await _api.get('/api/entregas/${widget.id}') as Json);
    } on ErrorApi catch (e) {
      if (e.codigo == 'SIN_REPARTIDOR') return null;
      rethrow;
    }
  }

  Future<void> _accion(Future<dynamic> Function() llamada, String hecho) async {
    setState(() => _ocupado = true);
    try {
      final respuesta = await llamada();
      if (!mounted) return;
      final aviso = respuesta is Map ? respuesta['aviso'] as String? : null;
      avisar(context, aviso ?? hecho);
      _cargar();
    } on ErrorApi catch (e) {
      if (mounted) avisar(context, e.campos.values.firstOrNull ?? e.mensaje);
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  Future<void> _cancelar() async {
    final motivo = await pedirTexto(
      context,
      titulo: '¿Cancelar el pedido #${widget.id}?',
      etiqueta: 'Motivo',
      confirmar: 'Cancelar pedido',
      maximo: 255,
    );
    if (motivo != null) {
      await _accion(() => _api.post('/api/pedidos/${widget.id}/cancelar', {'motivo': motivo}), 'Pedido cancelado.');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Cargador(
      futuro: _datos,
      reintentar: _cargar,
      constructor: (context, datos) {
        final (pedido, pagos, entrega) = datos;
        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Contenido(
              ancho: 900,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _encabezado(pedido, pagos),
                  const SizedBox(height: 16),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: _acciones(pedido, entrega, pagos.lastOrNull?.estado == 'PENDIENTE_DE_PAGO'),
                  ),
                  if (pedido.esDelivery && _usuario.puede('DISTRIBUCION_ASIGNAR')) _repartidor(pedido, entrega),
                  // CU08: el recojo no lleva repartidor; se cierra cuando el cliente lo retira.
                  if (!pedido.esDelivery && ['CONFIRMADO', 'EN_PREPARACION', 'LISTO'].contains(pedido.estado))
                    Padding(
                      padding: const EdgeInsets.only(top: 16),
                      child: Row(
                        children: [
                          const Icon(Icons.storefront_outlined),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              'Recojo en tienda: no lleva repartidor. Ciérralo con «El cliente lo retiró» '
                              'cuando esté Listo y lo recojan.',
                            ),
                          ),
                        ],
                      ),
                    ),
                  const Divider(height: 32),
                  _productos(pedido),
                  const Divider(height: 32),
                  _historial(pedido),
                ],
              ),
            ),
          ],
        );
      },
    );
  }

  Widget _encabezado(Pedido pedido, List<Pago> pagos) {
    final textos = Theme.of(context).textTheme;
    final pago = pagos.lastOrNull;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Pedido #${pedido.id}${pedido.asistido ? ' · Asistido' : ''}', style: textos.headlineSmall),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [EtiquetaEstado(pedido.estado), if (pago != null) EtiquetaPago(pago.estado)],
        ),
        const SizedBox(height: 8),
        if (pedido.cliente != null) Text('Cliente: ${pedido.cliente}', style: textos.titleMedium),
        Text('${pedido.esDelivery ? 'Delivery' : 'Recojo en tienda'} · ${fechaHora(pedido.horarioEntrega)}'),
        if (pedido.direccion != null) Text(pedido.direccion!),
        Text('Teléfono: ${pedido.telefono}'),
        if (pedido.motivoCancelacion != null) Text('Motivo de la cancelación: ${pedido.motivoCancelacion}'),
      ],
    );
  }

  /// Solo las acciones que el rol permite y el estado admite; el servidor las valida igual (RNF07).
  List<Widget> _acciones(Pedido p, Entrega? entrega, bool pagoPendiente) {
    final u = _usuario;
    Widget boton(String texto, Future<dynamic> Function() llamada, String hecho) =>
        FilledButton(onPressed: _ocupado ? null : () => _accion(llamada, hecho), child: Text(texto));
    return [
      if (p.estado == 'CONFIRMADO' && u.puede('PEDIDOS_PREPARAR'))
        boton(
          'Marcar en preparación',
          () => _api.post('/api/pedidos/${p.id}/en-preparacion'),
          'Pedido en preparación.',
        ),
      if (p.estado == 'EN_PREPARACION' && u.puede('PEDIDOS_PREPARAR'))
        boton('Marcar listo', () => _api.post('/api/pedidos/${p.id}/listo'), 'Pedido listo.'),
      if (p.estado == 'LISTO' && !p.esDelivery && u.puede('DISTRIBUCION_ASIGNAR'))
        boton('El cliente lo retiró', () => _api.post('/api/pedidos/${p.id}/retirado'), 'Pedido entregado.'),
      if (p.estado == 'LISTO' && p.esDelivery && entrega != null && u.puede('DISTRIBUCION_ASIGNAR'))
        boton('Sale en camino', () => _api.post('/api/entregas/${p.id}/en-camino'), 'Pedido en camino.'),
      if (p.asistido && p.estado == 'PENDIENTE' && pagoPendiente && u.puede('COBRANZA_REGISTRAR'))
        FilledButton.tonal(onPressed: _ocupado ? null : _registrarPago, child: const Text('Registrar pago')),
      if (p.cancelable && u.puede('PEDIDOS_CANCELAR'))
        OutlinedButton(onPressed: _ocupado ? null : _cancelar, child: const Text('Cancelar pedido')),
    ];
  }

  /// CU17, alt. 2: en un pedido asistido, el vendedor registra el pago recibido; otra persona lo verifica (RN06).
  Future<void> _registrarPago() async {
    final metodos = lista(await _api.get('/api/metodos-pago'), MetodoPago.deJson);
    if (!mounted) return;
    if (metodos.isEmpty) {
      avisar(context, 'No hay métodos de pago activos. Actívalos en Configuración.');
      return;
    }
    MetodoPago metodo = metodos.first;
    String? nombreArchivo;
    List<int>? bytes;
    final referencia = CampoDialogo('referencia', 'Referencia o número de operación');
    final hecho = await formularioEnDialogo(
      context,
      titulo: 'Registrar el pago del pedido #${widget.id}',
      campos: [referencia],
      confirmar: 'Registrar',
      extra: [
        StatefulBuilder(
          builder: (_, actualizar) => Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              DropdownButtonFormField<MetodoPago>(
                initialValue: metodo,
                decoration: const InputDecoration(labelText: 'Método'),
                items: [for (final m in metodos) DropdownMenuItem(value: m, child: Text(m.nombre))],
                onChanged: (m) => actualizar(() => metodo = m!),
              ),
              const SizedBox(height: 12),
              OutlinedButton.icon(
                onPressed: () async {
                  final archivo = await FilePicker.pickFile(
                    type: FileType.custom,
                    allowedExtensions: const ['jpg', 'jpeg', 'png', 'pdf'],
                  );
                  if (archivo == null) return;
                  final leidos = await archivo.readAsBytes();
                  actualizar(() {
                    nombreArchivo = archivo.name;
                    bytes = leidos;
                  });
                },
                icon: const Icon(Icons.attach_file),
                label: Text(nombreArchivo ?? 'Adjuntar comprobante (JPG, PNG o PDF, hasta 5 MB)'),
              ),
            ],
          ),
        ),
      ],
      enviar: () {
        if (bytes == null) {
          throw ErrorApi(400, 'ARCHIVO_FALTANTE', 'Adjunta el comprobante.');
        }
        return _api.multipart(
          'POST',
          '/api/pagos',
          campos: {'pedidoId': '${widget.id}', 'metodoPagoId': '${metodo.id}', 'referencia': referencia.texto},
          nombreArchivo: 'comprobante',
          bytes: bytes!,
          archivo: nombreArchivo!,
        );
      },
    );
    if (hecho != null && mounted) {
      avisar(context, 'Pago registrado. Otra persona con permiso debe verificarlo.');
      _cargar();
    }
  }

  /// Asignar o cambiar el repartidor hasta que el pedido sale (CU08).
  Widget _repartidor(Pedido pedido, Entrega? entrega) {
    final asignable = ['CONFIRMADO', 'EN_PREPARACION', 'LISTO'].contains(pedido.estado);
    return Padding(
      padding: const EdgeInsets.only(top: 16),
      child: Row(
        children: [
          const Text('Repartidor: '),
          const SizedBox(width: 8),
          if (!asignable || _repartidores == null)
            Text(entrega?.repartidor.nombre ?? 'Sin asignar')
          else
            DropdownButton<int>(
              value: entrega?.repartidor.id,
              hint: const Text('Elegir repartidor'),
              items: [for (final r in _repartidores!) DropdownMenuItem(value: r.id, child: Text(r.nombre))],
              onChanged: _ocupado
                  ? null
                  : (id) => _accion(
                      () => _api.put('/api/entregas/${pedido.id}/repartidor', {'repartidorId': id}),
                      'Repartidor asignado.',
                    ),
            ),
          if (entrega?.observacionFallo != null) ...[
            const SizedBox(width: 16),
            Flexible(child: Text('Última entrega fallida: ${entrega!.observacionFallo}')),
          ],
        ],
      ),
    );
  }

  Widget _productos(Pedido pedido) {
    final textos = Theme.of(context).textTheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text('Productos', style: textos.titleLarge),
        for (final l in pedido.lineas)
          ListTile(
            contentPadding: EdgeInsets.zero,
            title: Text('${l.cantidad} × ${l.nombre}'),
            subtitle: l.opciones.isEmpty && l.mensaje == null
                ? null
                : Text(
                    [
                      ...l.opciones.map((o) => '${o.tipo}: ${o.nombre}'),
                      if (l.mensaje != null) 'Mensaje: «${l.mensaje}»',
                    ].join(' · '),
                  ),
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
      ],
    );
  }

  /// Cada cambio de estado con quién lo hizo y cuándo (RF43).
  Widget _historial(Pedido pedido) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text('Historial', style: Theme.of(context).textTheme.titleLarge),
        for (final c in pedido.historial)
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: const Icon(Icons.circle, size: 12),
            title: Text(nombresEstadoPedido[c.nuevo]!),
            subtitle: Text(
              [
                fechaHora(c.fecha),
                if (c.usuario != null) c.usuario!,
                if (c.observacion != null) c.observacion!,
              ].join(' · '),
            ),
          ),
      ],
    );
  }
}
