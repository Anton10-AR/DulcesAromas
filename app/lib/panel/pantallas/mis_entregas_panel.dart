import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/widgets.dart';
import 'dialogos.dart';

/// Las entregas del repartidor: solo las suyas, con dirección y teléfono (CU08, RF41, RNF16).
class MisEntregasPanel extends StatefulWidget {
  const MisEntregasPanel({super.key});

  @override
  State<MisEntregasPanel> createState() => _MisEntregasPanelState();
}

class _MisEntregasPanelState extends State<MisEntregasPanel> {
  late Future<List<Entrega>> _entregas;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  void _cargar() {
    setState(() {
      _entregas = _api.get('/api/entregas/mias').then((j) => lista(j, Entrega.deJson));
    });
  }

  Future<void> _accion(Future<dynamic> Function() llamada, String hecho) async {
    try {
      await llamada();
      if (mounted) avisar(context, hecho);
    } on ErrorApi catch (e) {
      if (mounted) avisar(context, e.campos.values.firstOrNull ?? e.mensaje);
    }
    _cargar();
  }

  /// RN13: la entrega fallida exige observación y el pedido vuelve a Listo.
  Future<void> _fallida(Entrega e) async {
    final observacion = await pedirTexto(
      context,
      titulo: 'No se pudo entregar',
      etiqueta: '¿Qué pasó?',
      confirmar: 'Registrar entrega fallida',
    );
    if (observacion != null) {
      await _accion(
        () => _api.post('/api/entregas/${e.pedidoId}/fallida', {'observacion': observacion}),
        'Registramos la entrega fallida. El pedido volvió a Listo.',
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    return RefreshIndicator(
      onRefresh: () async => _cargar(),
      child: Cargador(
        futuro: _entregas,
        reintentar: _cargar,
        constructor: (context, entregas) {
          if (entregas.isEmpty) return const Center(child: Text('No tienes entregas asignadas.'));
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: entregas.length,
            itemBuilder: (_, i) {
              final e = entregas[i];
              return Contenido(
                child: Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Text('Pedido #${e.pedidoId}', style: textos.titleMedium),
                            const Spacer(),
                            EtiquetaEstado(e.estado),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Text(e.cliente ?? ''),
                        SelectableText(e.direccion ?? '', style: textos.bodyLarge),
                        SelectableText('Teléfono: ${e.telefono}'),
                        Text('Horario: ${fechaHora(e.horarioEntrega)}'),
                        const SizedBox(height: 8),
                        for (final p in e.productos) Text('• $p'),
                        if (e.observacionFallo != null)
                          Text('Intento anterior: ${e.observacionFallo}', style: textos.bodySmall),
                        const SizedBox(height: 12),
                        Wrap(
                          spacing: 8,
                          runSpacing: 8,
                          children: [
                            if (e.estado == 'LISTO')
                              FilledButton(
                                onPressed: () =>
                                    _accion(() => _api.post('/api/entregas/${e.pedidoId}/en-camino'), 'En camino.'),
                                child: const Text('Salgo en camino'),
                              ),
                            if (e.estado == 'EN_CAMINO') ...[
                              FilledButton(
                                onPressed: () => _accion(
                                  () => _api.post('/api/entregas/${e.pedidoId}/entregado'),
                                  'Pedido entregado.',
                                ),
                                child: const Text('Entregado'),
                              ),
                              OutlinedButton(onPressed: () => _fallida(e), child: const Text('No se pudo entregar')),
                            ],
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
