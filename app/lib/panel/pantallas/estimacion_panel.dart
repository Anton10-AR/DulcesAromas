import 'package:decimal/decimal.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/tema.dart';
import '../../comun/widgets.dart';

/// Lo que requiere un pedido según las recetas aproximadas, sumado a la cola de la cocina (RF46, HU23). Solo informa:
/// no descuenta stock ni impide aprobar (D12).
class Estimacion {
  Estimacion({
    required this.insumos,
    required this.sinReceta,
    required this.minutosPedido,
    required this.minutosCola,
    required this.horarioEntrega,
    required this.listoEstimado,
    required this.alcanzaElTiempo,
    required this.alcanzanLosInsumos,
  });

  factory Estimacion.deJson(Json j) => Estimacion(
    insumos: lista(j['insumos'], InsumoEstimado.deJson),
    sinReceta: List<String>.from(j['sinReceta'] as List),
    minutosPedido: j['minutosPedido'] as int,
    minutosCola: j['minutosCola'] as int,
    horarioEntrega: instante(j['horarioEntrega'])!,
    listoEstimado: instante(j['listoEstimado'])!,
    alcanzaElTiempo: j['alcanzaElTiempo'] as bool,
    alcanzanLosInsumos: j['alcanzanLosInsumos'] as bool,
  );

  final List<InsumoEstimado> insumos;
  final List<String> sinReceta;
  final int minutosPedido;
  final int minutosCola;
  final DateTime horarioEntrega;
  final DateTime listoEstimado;
  final bool alcanzaElTiempo;
  final bool alcanzanLosInsumos;

  /// Si algún producto del pedido tiene receta o tiempo: si no, no hay nada que estimar.
  bool get hayDatos => insumos.isNotEmpty || minutosPedido > 0;
}

class InsumoEstimado {
  InsumoEstimado(this.nombre, this.unidad, this.necesita, this.comprometido, this.stock, this.falta);

  factory InsumoEstimado.deJson(Json j) => InsumoEstimado(
    j['nombre'] as String,
    j['unidad'] as String,
    decimalDe(j['necesita']),
    decimalDe(j['comprometido']),
    decimalDe(j['stock']),
    decimalDe(j['falta']),
  );

  final String nombre;
  final String unidad;
  final Decimal necesita;
  final Decimal comprometido;
  final Decimal stock;
  final Decimal falta;
}

/// «4 h 30 min», «45 min».
String duracion(int minutos) {
  final horas = minutos ~/ 60;
  final resto = minutos % 60;
  if (horas == 0) return '$resto min';
  return resto == 0 ? '$horas h' : '$horas h $resto min';
}

/// El bloque «Insumos y tiempo» de un pedido, para decidir antes de aprobar su pago (CU07).
class EstimacionPedido extends StatefulWidget {
  const EstimacionPedido({super.key, required this.pedidoId});

  final int pedidoId;

  @override
  State<EstimacionPedido> createState() => _EstimacionPedidoState();
}

class _EstimacionPedidoState extends State<EstimacionPedido> {
  late final Future<Estimacion> _estimacion = context
      .read<Api>()
      .get('/api/pedidos/${widget.pedidoId}/estimacion')
      .then((j) => Estimacion.deJson(j as Json));

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    return FutureBuilder(
      future: _estimacion,
      builder: (context, estado) {
        if (estado.hasError) {
          return Text('No se pudo estimar los insumos y el tiempo.', style: textos.bodySmall);
        }
        if (!estado.hasData) return const LinearProgressIndicator();
        final e = estado.data!;
        if (!e.hayDatos) {
          return Text(
            'Insumos y tiempo: ningún producto de este pedido tiene receta todavía (se carga en Catálogo).',
            style: textos.bodySmall,
          );
        }
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Wrap(
              spacing: 8,
              runSpacing: 4,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                Text('Insumos y tiempo (estimado)', style: textos.titleSmall),
                if (e.insumos.isNotEmpty)
                  Etiqueta(
                    e.alcanzanLosInsumos ? 'Insumos: alcanzan' : 'Insumos: faltan',
                    tono: e.alcanzanLosInsumos ? Tono.verde : Tono.rojo,
                  ),
                if (e.minutosPedido > 0)
                  Etiqueta(
                    e.alcanzaElTiempo ? 'Llega a tiempo' : 'No llega a tiempo',
                    tono: e.alcanzaElTiempo ? Tono.verde : Tono.rojo,
                  ),
              ],
            ),
            const SizedBox(height: 4),
            if (e.minutosPedido > 0)
              Text(
                'Este pedido: ${duracion(e.minutosPedido)} · En cola: ${duracion(e.minutosCola)} · '
                'Listo aprox.: ${fechaHora(e.listoEstimado)} · Entrega: ${fechaHora(e.horarioEntrega)}',
                style: textos.bodyMedium,
              ),
            for (final i in e.insumos)
              Text(
                '${i.nombre}: necesita ${i.necesita} ${i.unidad} · en cola ${i.comprometido} · hay ${i.stock}'
                '${i.falta > Decimal.zero ? ' · faltan ${i.falta} ${i.unidad}' : ''}',
                style: textos.bodyMedium?.copyWith(
                  color: i.falta > Decimal.zero ? Theme.of(context).colorScheme.error : null,
                  fontWeight: i.falta > Decimal.zero ? FontWeight.w600 : null,
                ),
              ),
            if (e.sinReceta.isNotEmpty)
              Text('Sin receta, no estimados: ${e.sinReceta.join(', ')}', style: textos.bodySmall),
            Text(
              'Cuenta los pedidos Confirmados y En preparación, como si la cocina trabajara sin parar desde ahora. '
              'Las recetas son aproximadas y no descuentan stock.',
              style: textos.bodySmall,
            ),
          ],
        );
      },
    );
  }
}
