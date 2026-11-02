import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/widgets.dart';

/// Entregas abiertas con su repartidor, para quien asigna (CU08). Se asigna desde el detalle del pedido.
class EntregasPanel extends StatefulWidget {
  const EntregasPanel({super.key});

  @override
  State<EntregasPanel> createState() => _EntregasPanelState();
}

class _EntregasPanelState extends State<EntregasPanel> {
  late Future<List<Entrega>> _entregas;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  void _cargar() {
    setState(() => _entregas = context.read<Api>().get('/api/entregas').then((j) => lista(j, Entrega.deJson)));
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: () async => _cargar(),
      child: Cargador(
        futuro: _entregas,
        reintentar: _cargar,
        constructor: (context, entregas) {
          if (entregas.isEmpty) {
            return const Center(
                child: Text('No hay entregas abiertas. Asigna repartidores desde el detalle de cada pedido con delivery.'));
          }
          return ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: entregas.length,
            separatorBuilder: (_, _) => const Divider(height: 1),
            itemBuilder: (_, i) {
              final e = entregas[i];
              return ListTile(
                onTap: () => context.go('/pedidos/${e.pedidoId}'),
                title: Text('Pedido #${e.pedidoId} · ${e.repartidor.nombre}'),
                subtitle: Text([
                  '${fechaHora(e.horarioEntrega)} · ${e.direccion ?? ''}',
                  if (e.observacionFallo != null) 'Entrega fallida: ${e.observacionFallo}',
                ].join('\n')),
                isThreeLine: e.observacionFallo != null,
                trailing: Text(nombresEstadoPedido[e.estado]!),
              );
            },
          );
        },
      ),
    );
  }
}
