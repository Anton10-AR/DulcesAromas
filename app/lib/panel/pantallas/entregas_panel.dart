import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/actualizacion.dart';
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

class _EntregasPanelState extends State<EntregasPanel> with ActualizacionPeriodica {
  late Future<List<Entrega>> _entregas;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  Future<List<Entrega>> _consultar() => context.read<Api>().get('/api/entregas').then((j) => lista(j, Entrega.deJson));

  void _cargar() {
    setState(() {
      _entregas = _consultar();
    });
  }

  /// La lista cambia sola cuando otro usuario mueve un pedido, sin recargar la página.
  @override
  Future<void> actualizarEnSilencio() => reemplazarSiResponde(_consultar(), (f) => _entregas = f);

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
              child: Text(
                'No hay entregas abiertas. Asigna repartidores desde el detalle de cada pedido con delivery.',
              ),
            );
          }
          return ListaEnTarjeta(
            padding: const EdgeInsets.all(16),
            itemCount: entregas.length,
            itemBuilder: (_, i) {
              final e = entregas[i];
              return ListTile(
                onTap: () => context.go('/pedidos/${e.pedidoId}'),
                title: Text('Pedido #${e.pedidoId} · ${e.repartidor.nombre}'),
                subtitle: Text(
                  [
                    '${fechaHora(e.horarioEntrega)} · ${e.direccion ?? ''}',
                    if (e.observacionFallo != null) 'Entrega fallida: ${e.observacionFallo}',
                  ].join('\n'),
                ),
                isThreeLine: e.observacionFallo != null,
                trailing: EtiquetaEstado(e.estado),
              );
            },
          );
        },
      ),
    );
  }
}
