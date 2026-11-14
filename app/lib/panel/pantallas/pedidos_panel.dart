import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/sesion.dart';
import '../../comun/widgets.dart';

/// Lista de pedidos del Panel, del horario de entrega más próximo al más lejano (CU10, HU14).
class PedidosPanel extends StatefulWidget {
  const PedidosPanel({super.key});

  @override
  State<PedidosPanel> createState() => _PedidosPanelState();
}

class _PedidosPanelState extends State<PedidosPanel> {
  /// Nulo: todos los que siguen en curso.
  String? _estado;
  late Future<List<PedidoResumen>> _pedidos;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  void _cargar() {
    setState(() {
      _pedidos = context
          .read<Api>()
          .get('/api/pedidos', {'estado': _estado})
          .then((j) => lista(j, PedidoResumen.deJson));
    });
  }

  @override
  Widget build(BuildContext context) {
    final asistido = context.watch<Sesion>().usuario!.puede('PEDIDOS_CONSULTAR');
    return Column(
      children: [
        if (asistido)
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
            child: Align(
              alignment: Alignment.centerRight,
              child: FilledButton.icon(
                onPressed: () => context.go('/pedidos/asistido'),
                icon: const Icon(Icons.support_agent),
                label: const Text('Nuevo pedido asistido'),
              ),
            ),
          ),
        SizedBox(
          height: 56,
          child: ListView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            children: [_filtro('En curso', null), for (final e in nombresEstadoPedido.entries) _filtro(e.value, e.key)],
          ),
        ),
        Expanded(
          child: RefreshIndicator(
            onRefresh: () async => _cargar(),
            child: Cargador(
              futuro: _pedidos,
              reintentar: _cargar,
              constructor: (context, pedidos) {
                if (pedidos.isEmpty) return const Center(child: Text('No hay pedidos en este estado.'));
                return ListaEnTarjeta(
                  padding: const EdgeInsets.all(16),
                  itemCount: pedidos.length,
                  itemBuilder: (_, i) {
                    final p = pedidos[i];
                    return ListTile(
                      onTap: () => context.go('/pedidos/${p.id}'),
                      title: Text(
                        'Pedido #${p.id} · ${p.tipoEntrega == 'DELIVERY' ? 'Delivery' : 'Recojo'}'
                        '${p.asistido ? ' · Asistido' : ''}',
                      ),
                      subtitle: Text(
                        [
                          ?p.cliente,
                          'Entrega: ${fechaHora(p.horarioEntrega)}',
                          cantidadProductos(p.productos),
                        ].join(' · '),
                      ),
                      trailing: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [EtiquetaEstado(p.estado), const SizedBox(height: 4), Text(bolivianos(p.total))],
                      ),
                    );
                  },
                );
              },
            ),
          ),
        ),
      ],
    );
  }

  Widget _filtro(String nombre, String? estado) => Padding(
    padding: const EdgeInsets.only(right: 8),
    child: ChoiceChip(
      label: Text(nombre),
      selected: _estado == estado,
      onSelected: (_) {
        _estado = estado;
        _cargar();
      },
    ),
  );
}
