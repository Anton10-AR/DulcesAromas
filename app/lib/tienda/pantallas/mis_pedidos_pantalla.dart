import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/widgets.dart';

/// Mis pedidos, del más reciente al más antiguo, con el estado del pedido y del pago (CU15, HU19).
class MisPedidosPantalla extends StatefulWidget {
  const MisPedidosPantalla({super.key});

  @override
  State<MisPedidosPantalla> createState() => _MisPedidosPantallaState();
}

class _MisPedidosPantallaState extends State<MisPedidosPantalla> {
  late Future<(List<PedidoResumen>, List<Cobro>)> _datos;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  void _cargar() {
    final api = context.read<Api>();
    setState(() {
      _datos = (
        api.get('/api/pedidos/mios').then((j) => lista(j, PedidoResumen.deJson)),
        api.get('/api/pagos/mios').then((j) => lista(j, Cobro.deJson)),
      ).wait;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Mis pedidos')),
      body: RefreshIndicator(
        onRefresh: () async => _cargar(),
        child: Cargador(
          futuro: _datos,
          reintentar: _cargar,
          constructor: (context, datos) {
            final (pedidos, cobros) = datos;
            if (pedidos.isEmpty) {
              // CU15, alt. 1.
              return Center(
                child: Column(mainAxisSize: MainAxisSize.min, children: [
                  const Text('Todavía no hiciste pedidos.'),
                  const SizedBox(height: 8),
                  FilledButton(onPressed: () => context.go('/'), child: const Text('Ver el catálogo')),
                ]),
              );
            }
            final cobroDe = {for (final c in cobros) c.pedidoId: c};
            return ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: pedidos.length,
              itemBuilder: (_, i) => Contenido(child: _TarjetaPedido(pedidos[i], cobroDe[pedidos[i].id])),
            );
          },
        ),
      ),
    );
  }
}

class _TarjetaPedido extends StatelessWidget {
  const _TarjetaPedido(this.pedido, this.cobro);

  final PedidoResumen pedido;
  final Cobro? cobro;

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    final debePagar = pedido.estado == 'PENDIENTE' && cobro?.estado == 'PENDIENTE_DE_PAGO';
    return Card(
      child: InkWell(
        onTap: () => context.push('/pedidos/${pedido.id}'),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              Text('Pedido #${pedido.id}', style: textos.titleMedium),
              const Spacer(),
              Text(bolivianos(pedido.total), style: textos.titleMedium),
            ]),
            Text('${fecha(pedido.fechaCreacion)} · ${pedido.productos} productos', style: textos.bodySmall),
            const SizedBox(height: 8),
            Wrap(spacing: 8, runSpacing: 8, children: [
              Etiqueta(nombresEstadoPedido[pedido.estado]!, resaltada: true),
              if (cobro != null) Etiqueta('Pago: ${nombresEstadoPago[cobro!.estado]}'),
            ]),
            // HU19: si el pago fue rechazado, se ve la observación y se puede volver a pagar.
            if (cobro?.observacionRechazo != null) ...[
              const SizedBox(height: 8),
              Text('Pago rechazado: ${cobro!.observacionRechazo}',
                  style: TextStyle(color: Theme.of(context).colorScheme.error)),
            ],
            if (debePagar) ...[
              const SizedBox(height: 8),
              FilledButton(
                onPressed: () => context.push('/pedidos/${pedido.id}/pagar'),
                child: Text(cobro?.observacionRechazo != null ? 'Volver a pagar' : 'Pagar'),
              ),
            ],
          ]),
        ),
      ),
    );
  }
}
