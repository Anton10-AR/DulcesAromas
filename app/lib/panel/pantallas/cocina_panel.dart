import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/widgets.dart';

/// Un pedido como lo ve la cocina: qué hay que hacer y para cuándo, sin datos personales ni montos (RNF16).
class PedidoCocina {
  PedidoCocina({
    required this.id,
    required this.estado,
    required this.tipoEntrega,
    required this.horarioEntrega,
    required this.lineas,
  });

  factory PedidoCocina.deJson(Json j) => PedidoCocina(
    id: j['id'] as int,
    estado: j['estado'] as String,
    tipoEntrega: j['tipoEntrega'] as String,
    horarioEntrega: instante(j['horarioEntrega'])!,
    lineas: lista(j['lineas'], LineaCocina.deJson),
  );

  final int id;
  final String estado;
  final String tipoEntrega;
  final DateTime horarioEntrega;
  final List<LineaCocina> lineas;
}

class LineaCocina {
  LineaCocina({required this.producto, required this.cantidad, required this.opciones, this.mensaje});

  factory LineaCocina.deJson(Json j) => LineaCocina(
    producto: j['producto'] as String,
    cantidad: j['cantidad'] as int,
    opciones: (j['opciones'] as List).cast<String>(),
    mensaje: j['mensaje'] as String?,
  );

  final String producto;
  final int cantidad;
  final List<String> opciones;
  final String? mensaje;
}

/// Cocina: los pedidos por preparar, del horario de entrega más cercano al más lejano. Quien prepara marca En
/// preparación y Listo (CU08, HU13, D3).
class CocinaPanel extends StatefulWidget {
  const CocinaPanel({super.key});

  @override
  State<CocinaPanel> createState() => _CocinaPanelState();
}

class _CocinaPanelState extends State<CocinaPanel> {
  late Future<List<PedidoCocina>> _pedidos;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  void _cargar() {
    setState(() {
      _pedidos = _api.get('/api/cocina').then((j) => lista(j, PedidoCocina.deJson));
    });
  }

  Future<void> _avanzar(PedidoCocina p, String paso, String hecho) async {
    try {
      await _api.post('/api/cocina/${p.id}/$paso');
      if (mounted) avisar(context, hecho);
    } on ErrorApi catch (e) {
      if (mounted) avisar(context, e.mensaje);
    }
    _cargar();
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: () async => _cargar(),
      child: Cargador(
        futuro: _pedidos,
        reintentar: _cargar,
        constructor: (context, pedidos) {
          if (pedidos.isEmpty) {
            return ListView(
              children: const [
                Padding(
                  padding: EdgeInsets.all(32),
                  child: Center(child: Text('No hay pedidos por preparar.')),
                ),
              ],
            );
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: pedidos.length,
            itemBuilder: (_, i) => Contenido(child: _tarjeta(pedidos[i])),
          );
        },
      ),
    );
  }

  Widget _tarjeta(PedidoCocina p) {
    final textos = Theme.of(context).textTheme;
    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Wrap(
              spacing: 12,
              runSpacing: 8,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                Text('Pedido #${p.id}', style: textos.titleMedium),
                EtiquetaEstado(p.estado),
              ],
            ),
            const SizedBox(height: 4),
            Text(
              '${p.tipoEntrega == 'DELIVERY' ? 'Delivery' : 'Recojo'} · Para el ${fechaHora(p.horarioEntrega)}',
              style: textos.bodyMedium,
            ),
            const Divider(height: 24),
            for (final l in p.lineas)
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('${l.cantidad} × ${l.producto}', style: textos.bodyLarge),
                    if (l.opciones.isNotEmpty) Text(l.opciones.join(' · '), style: textos.bodyMedium),
                    if (l.mensaje != null) Text('Mensaje sobre la torta: «${l.mensaje}»', style: textos.bodyMedium),
                  ],
                ),
              ),
            const SizedBox(height: 8),
            if (p.estado == 'CONFIRMADO')
              FilledButton.icon(
                onPressed: () => _avanzar(p, 'en-preparacion', 'Pedido #${p.id} en preparación.'),
                icon: const Icon(Icons.soup_kitchen_outlined),
                label: const Text('Empezar a preparar'),
              ),
            if (p.estado == 'EN_PREPARACION')
              FilledButton.icon(
                onPressed: () => _avanzar(p, 'listo', 'Pedido #${p.id} listo.'),
                icon: const Icon(Icons.check),
                label: const Text('Marcar listo'),
              ),
          ],
        ),
      ),
    );
  }
}
