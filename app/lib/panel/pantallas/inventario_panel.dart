import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/sesion.dart';
import '../../comun/tema.dart';
import '../../comun/widgets.dart';
import '../modelos_panel.dart';
import 'formulario.dart';

/// Inventario de insumos (CU03, HU09). El administrador registra movimientos; el Vendedor solo consulta.
class InventarioPanel extends StatefulWidget {
  const InventarioPanel({super.key});

  @override
  State<InventarioPanel> createState() => _InventarioPanelState();
}

class _InventarioPanelState extends State<InventarioPanel> {
  late Future<List<ItemInventario>> _items;
  bool _soloAlertas = false;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  void _cargar() {
    setState(() {
      _items = _api
          .get(_soloAlertas ? '/api/inventario/alertas' : '/api/inventario')
          .then((j) => lista(j, ItemInventario.deJson));
    });
  }

  Future<void> _item({ItemInventario? item}) async {
    final nombre = CampoDialogo('nombre', 'Nombre', valor: item?.nombre ?? '');
    final unidad = CampoDialogo(
      'unidad',
      'Unidad',
      valor: item?.unidad ?? '',
      ayuda: 'Por ejemplo kg, litros o unidades.',
    );
    final minimo = CampoDialogo(
      'minimo',
      'Mínimo',
      valor: item?.minimo.toString() ?? '',
      teclado: const TextInputType.numberWithOptions(decimal: true),
      ayuda: 'Al llegar a este stock aparece una alerta.',
    );
    Map<String, dynamic> datos() => {
      'nombre': nombre.texto,
      'unidad': unidad.texto,
      'minimo': minimo.texto.replaceAll(',', '.'),
    };
    final hecho = await formularioEnDialogo(
      context,
      titulo: item == null ? 'Nuevo insumo' : 'Editar insumo',
      campos: [nombre, unidad, minimo],
      enviar: () =>
          item == null ? _api.post('/api/inventario', datos()) : _api.put('/api/inventario/${item.id}', datos()),
    );
    if (hecho != null) _cargar();
  }

  /// RN09: una salida no supera el stock; el servidor la rechaza y el stock nunca queda negativo.
  Future<void> _movimiento(ItemInventario item) async {
    var tipo = 'ENTRADA';
    final cantidad = CampoDialogo(
      'cantidad',
      'Cantidad (${item.unidad})',
      teclado: const TextInputType.numberWithOptions(decimal: true),
    );
    final motivo = CampoDialogo('motivo', 'Motivo');
    final respuesta = await formularioEnDialogo(
      context,
      titulo: 'Movimiento de ${item.nombre}',
      campos: [cantidad, motivo],
      confirmar: 'Registrar',
      extra: [
        StatefulBuilder(
          builder: (_, actualizar) => Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SegmentedButton<String>(
                segments: const [
                  ButtonSegment(value: 'ENTRADA', label: Text('Entrada')),
                  ButtonSegment(value: 'SALIDA', label: Text('Salida')),
                  ButtonSegment(value: 'AJUSTE', label: Text('Ajuste')),
                ],
                selected: {tipo},
                onSelectionChanged: (s) => actualizar(() => tipo = s.first),
              ),
              const SizedBox(height: 8),
              Text(
                tipo == 'AJUSTE'
                    ? 'En un ajuste, la cantidad es el stock contado.'
                    : 'Stock actual: ${item.stock} ${item.unidad}',
              ),
            ],
          ),
        ),
      ],
      enviar: () => _api.post('/api/inventario/${item.id}/movimientos', {
        'tipo': tipo,
        'cantidad': cantidad.texto.replaceAll(',', '.'),
        'motivo': motivo.texto,
      }),
    );
    if (respuesta is Map && mounted) {
      final actualizado = ItemInventario.deJson(respuesta as Json);
      avisar(
        context,
        actualizado.alerta
            ? '${actualizado.nombre} llegó a su mínimo: ${actualizado.stock} ${actualizado.unidad}.'
            : 'Movimiento registrado.',
      );
      _cargar();
    }
  }

  /// RF31: el historial de movimientos lo ve el administrador.
  Future<void> _historial(ItemInventario item) async {
    final movimientos = lista(await _api.get('/api/inventario/${item.id}/movimientos'), Movimiento.deJson);
    if (!mounted) return;
    await showDialog<void>(
      context: context,
      builder: (contexto) => AlertDialog(
        title: Text('Historial de ${item.nombre}'),
        content: SizedBox(
          width: 520,
          child: movimientos.isEmpty
              ? const Text('Todavía no hay movimientos.')
              : ListView(
                  shrinkWrap: true,
                  children: [
                    for (final m in movimientos)
                      ListTile(
                        title: Text(
                          '${m.tipo[0]}${m.tipo.substring(1).toLowerCase()}: ${m.cantidad} ${item.unidad}'
                          ' → stock ${m.stockResultante}',
                        ),
                        subtitle: Text('${fechaHora(m.fecha)} · ${m.usuario ?? ''} · ${m.motivo}'),
                      ),
                  ],
                ),
        ),
        actions: [FilledButton(onPressed: () => Navigator.pop(contexto), child: const Text('Cerrar'))],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final registra = context.watch<Sesion>().usuario!.puede('INVENTARIO_MOVIMIENTOS');
    final error = Theme.of(context).colorScheme.error;
    return Scaffold(
      floatingActionButton: registra
          ? FloatingActionButton.extended(
              onPressed: () => _item(),
              icon: const Icon(Icons.add),
              label: const Text('Nuevo insumo'),
            )
          : null,
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
            child: Row(
              children: [
                FilterChip(
                  label: const Text('Solo alertas'),
                  selected: _soloAlertas,
                  onSelected: (v) {
                    _soloAlertas = v;
                    _cargar();
                  },
                ),
              ],
            ),
          ),
          Expanded(
            child: Cargador(
              futuro: _items,
              reintentar: _cargar,
              constructor: (context, items) {
                if (items.isEmpty) {
                  return Center(child: Text(_soloAlertas ? 'No hay alertas.' : 'Todavía no hay insumos.'));
                }
                return ListaEnTarjeta(
                  padding: const EdgeInsets.fromLTRB(16, 16, 16, 88),
                  itemCount: items.length,
                  itemBuilder: (_, i) {
                    final item = items[i];
                    return FilaConAcciones(
                      inicio: item.alerta
                          ? Icon(Icons.warning_amber, color: error, semanticLabel: 'En el mínimo')
                          : null,
                      titulo: Text(item.nombre),
                      subtitulo: Text(
                        'Stock: ${item.stock} ${item.unidad} · Mínimo: ${item.minimo}'
                        '${item.alerta ? ' · En el mínimo' : ''}',
                      ),
                      acciones: [
                        if (registra) ...[
                          IconButton(
                            tooltip: 'Historial',
                            onPressed: () => _historial(item),
                            icon: const Icon(Icons.history),
                          ),
                          IconButton(
                            tooltip: 'Editar',
                            onPressed: () => _item(item: item),
                            icon: const Icon(Icons.edit_outlined),
                          ),
                          FilledButton.tonal(
                            style: estiloTonal(context),
                            onPressed: () => _movimiento(item),
                            child: const Text('Movimiento'),
                          ),
                        ],
                      ],
                    );
                  },
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
