import 'package:decimal/decimal.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import 'estimacion_panel.dart';

/// Lo que lleva un pedido según las recetas aproximadas: por cada línea, los insumos ya multiplicados por su cantidad
/// y su tamaño, y los minutos (D12). Sin datos del cliente ni montos (RNF16).
class RecetaPedido {
  RecetaPedido(this.lineas, this.minutosTotal);

  factory RecetaPedido.deJson(Json j) => RecetaPedido(lista(j['lineas'], LineaReceta.deJson), j['minutosTotal'] as int);

  final List<LineaReceta> lineas;
  final int minutosTotal;
}

class LineaReceta {
  LineaReceta(this.producto, this.cantidad, this.opciones, this.unidades, this.minutos, this.ingredientes);

  factory LineaReceta.deJson(Json j) => LineaReceta(
    j['producto'] as String,
    j['cantidad'] as int,
    List<String>.from(j['opciones'] as List),
    decimalDe(j['unidades']),
    j['minutos'] as int?,
    lista(j['ingredientes'], (i) => (i['nombre'] as String, i['unidad'] as String, decimalDe(i['cantidad']))),
  );

  final String producto;
  final int cantidad;
  final List<String> opciones;

  /// La cantidad por el factor del tamaño: 2 tortas Grandes de factor 1.5 cuentan como 3.
  final Decimal unidades;
  final int? minutos;
  final List<(String, String, Decimal)> ingredientes;

  bool get tieneReceta => minutos != null || ingredientes.isNotEmpty;
}

/// Abre la receta del pedido en un cuadro, para prepararlo en Cocina (permiso «Consultar las recetas aproximadas»).
Future<void> mostrarReceta(BuildContext context, int pedidoId) {
  final receta = context.read<Api>().get('/api/cocina/$pedidoId/receta').then((j) => RecetaPedido.deJson(j as Json));
  return showDialog<void>(
    context: context,
    builder: (contexto) => AlertDialog(
      title: Text('Receta del pedido #$pedidoId'),
      content: SizedBox(
        width: 520,
        child: FutureBuilder(
          future: receta,
          builder: (contexto, estado) {
            if (estado.hasError) {
              final error = estado.error;
              return Text(error is ErrorApi ? error.mensaje : 'No se pudo cargar la receta.');
            }
            if (!estado.hasData) return const LinearProgressIndicator();
            return SingleChildScrollView(child: _Contenido(estado.data!));
          },
        ),
      ),
      actions: [FilledButton(onPressed: () => Navigator.pop(contexto), child: const Text('Cerrar'))],
    ),
  );
}

class _Contenido extends StatelessWidget {
  const _Contenido(this.receta);

  final RecetaPedido receta;

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    final suave = textos.bodySmall?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        for (final l in receta.lineas) ...[
          Text('${l.cantidad} × ${l.producto}', style: textos.titleMedium),
          if (l.opciones.isNotEmpty) Text(l.opciones.join(' · '), style: textos.bodyMedium),
          const SizedBox(height: 4),
          if (!l.tieneReceta)
            Text('Sin receta cargada.', style: suave)
          else ...[
            if (l.unidades != Decimal.fromInt(l.cantidad))
              Text('Cuenta como ${l.unidades} unidades por el tamaño.', style: suave),
            for (final (nombre, unidad, cantidad) in l.ingredientes)
              Padding(
                padding: const EdgeInsets.only(left: 8, top: 2),
                child: Text('• $nombre: $cantidad $unidad', style: textos.bodyLarge),
              ),
            if (l.minutos != null)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text('Tiempo: ${duracion(l.minutos!)}', style: textos.bodyMedium),
              ),
          ],
          const Divider(height: 24),
        ],
        if (receta.minutosTotal > 0) Text('Tiempo total: ${duracion(receta.minutosTotal)}', style: textos.titleSmall),
        const SizedBox(height: 4),
        Text('Las recetas son aproximadas y no descuentan stock.', style: suave),
      ],
    );
  }
}
