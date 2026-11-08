import 'dart:typed_data';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/widgets.dart';
import 'dialogos.dart';

/// Pagos en verificación: comparar el comprobante con el pedido y decidir (CU07, HU12).
class PagosPanel extends StatefulWidget {
  const PagosPanel({super.key});

  @override
  State<PagosPanel> createState() => _PagosPanelState();
}

class _PagosPanelState extends State<PagosPanel> {
  late Future<List<Pago>> _pagos;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  void _cargar() {
    setState(() {
      _pagos = _api.get('/api/pagos/en-verificacion').then((j) => lista(j, Pago.deJson));
    });
  }

  /// Si otra persona ya decidió el pago, el servidor lo rechaza y la lista se actualiza (RNF15).
  Future<void> _decidir(Pago pago, {required bool aprobar}) async {
    String? observacion;
    if (aprobar) {
      final ok = await confirmar(
        context,
        titulo: '¿Aprobar el pago?',
        mensaje: 'El pedido #${pago.pedidoId} pasará a Confirmado y se podrá preparar.',
        accion: 'Aprobar',
      );
      if (!ok) return;
    } else {
      // RN07: rechazar exige una observación que verá el cliente.
      observacion = await pedirTexto(
        context,
        titulo: 'Rechazar el pago',
        etiqueta: 'Motivo (lo verá el cliente)',
        confirmar: 'Rechazar',
      );
      if (observacion == null) return;
    }
    try {
      await (aprobar
          ? _api.post('/api/pagos/${pago.id}/aprobar')
          : _api.post('/api/pagos/${pago.id}/rechazar', {'observacion': observacion}));
      if (mounted) avisar(context, aprobar ? 'Pago aprobado. El pedido quedó Confirmado.' : 'Pago rechazado.');
    } on ErrorApi catch (e) {
      if (mounted) avisar(context, e.mensaje);
    }
    _cargar();
  }

  Future<void> _verComprobante(Pago pago) async {
    try {
      final archivo = await _api.archivo(pago.comprobante!);
      if (!mounted) return;
      await showDialog<void>(
        context: context,
        builder: (contexto) => AlertDialog(
          title: Text('Comprobante del pedido #${pago.pedidoId}'),
          content: archivo.tipo.startsWith('image/')
              ? InteractiveViewer(child: Image.memory(Uint8List.fromList(archivo.bytes)))
              : const Text('El comprobante es un PDF. Descárgalo para revisarlo.'),
          actions: [
            TextButton(
              onPressed: () => FilePicker.saveFile(
                fileName: 'comprobante-pedido-${pago.pedidoId}${archivo.tipo.contains('pdf') ? '.pdf' : ''}',
                bytes: Uint8List.fromList(archivo.bytes),
                mimeType: archivo.tipo,
              ),
              child: const Text('Descargar'),
            ),
            FilledButton(onPressed: () => Navigator.pop(contexto), child: const Text('Cerrar')),
          ],
        ),
      );
    } on ErrorApi catch (e) {
      if (mounted) avisar(context, e.mensaje);
    }
  }

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    return RefreshIndicator(
      onRefresh: () async => _cargar(),
      child: Cargador(
        futuro: _pagos,
        reintentar: _cargar,
        constructor: (context, pagos) {
          if (pagos.isEmpty) return const Center(child: Text('No hay pagos por verificar.'));
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: pagos.length,
            itemBuilder: (_, i) {
              final p = pagos[i];
              return Contenido(
                ancho: 900,
                child: Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            TextButton(
                              onPressed: () => context.go('/pedidos/${p.pedidoId}'),
                              child: Text('Pedido #${p.pedidoId}'),
                            ),
                            const Spacer(),
                            Text(bolivianos(p.monto), style: textos.titleLarge),
                          ],
                        ),
                        if (p.cliente != null) Text('Cliente: ${p.cliente}', style: textos.titleMedium),
                        Text('${p.metodo} · Referencia: ${p.referencia}'),
                        if (p.registrado != null)
                          Text('Registrado: ${fechaHora(p.registrado!)}', style: textos.bodySmall),
                        const SizedBox(height: 12),
                        Wrap(
                          spacing: 8,
                          runSpacing: 8,
                          children: [
                            OutlinedButton.icon(
                              onPressed: () => _verComprobante(p),
                              icon: const Icon(Icons.receipt_outlined),
                              label: const Text('Ver comprobante'),
                            ),
                            FilledButton(onPressed: () => _decidir(p, aprobar: true), child: const Text('Aprobar')),
                            OutlinedButton(onPressed: () => _decidir(p, aprobar: false), child: const Text('Rechazar')),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
              );
            },
          );
        },
      ),
    );
  }
}
