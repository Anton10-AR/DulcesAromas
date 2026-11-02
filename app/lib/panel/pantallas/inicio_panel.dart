import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/sesion.dart';
import '../../comun/widgets.dart';

/// Inicio del Panel con lo pendiente que el rol puede atender (CU10).
class InicioPanel extends StatefulWidget {
  const InicioPanel({super.key});

  @override
  State<InicioPanel> createState() => _InicioPanelState();
}

class _InicioPanelState extends State<InicioPanel> {
  late Future<List<(String, int, String)>> _pendientes;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  void _cargar() {
    final api = context.read<Api>();
    final usuario = context.read<Sesion>().usuario!;
    Future<(String, int, String)> contar(String titulo, String ruta, String destino, [Map<String, String>? p]) =>
        api.get(ruta, p).then((j) => (titulo, (j as List).length, destino));
    setState(() {
      _pendientes = [
        if (usuario.puede('PEDIDOS_PREPARAR'))
          contar('Pedidos confirmados por preparar', '/api/pedidos', '/pedidos', {'estado': 'CONFIRMADO'}),
        if (usuario.puede('COBRANZA_VERIFICAR')) contar('Pagos por verificar', '/api/pagos/en-verificacion', '/pagos'),
        if (usuario.puede('DISTRIBUCION_ASIGNAR'))
          contar('Pedidos listos para entregar', '/api/pedidos', '/pedidos', {'estado': 'LISTO'}),
        if (usuario.puede('DISTRIBUCION_ENTREGAS'))
          contar('Mis entregas abiertas', '/api/entregas/mias', '/mis-entregas'),
      ].wait;
    });
  }

  @override
  Widget build(BuildContext context) {
    final usuario = context.watch<Sesion>().usuario!;
    final textos = Theme.of(context).textTheme;
    return RefreshIndicator(
      onRefresh: () async => _cargar(),
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text('Hola, ${usuario.nombre}', style: textos.headlineSmall),
          const SizedBox(height: 16),
          Cargador(
            futuro: _pendientes,
            reintentar: _cargar,
            constructor: (context, pendientes) => Wrap(
              spacing: 16,
              runSpacing: 16,
              children: [
                for (final (titulo, cantidad, destino) in pendientes)
                  SizedBox(
                    width: 260,
                    child: Card(
                      child: InkWell(
                        onTap: () => context.go(destino),
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('$cantidad', style: textos.displaySmall),
                              Text(titulo, style: textos.titleMedium),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
