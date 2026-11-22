import 'package:flutter/material.dart';

import '../../comun/formato.dart';
import '../../comun/modelos.dart';

/// Los pasos que recorre un pedido, en orden (tabla 2.2). El recojo no pasa por En camino.
List<String> pasosDelPedido({required bool delivery}) => [
  'PENDIENTE',
  'CONFIRMADO',
  'EN_PREPARACION',
  'LISTO',
  if (delivery) 'EN_CAMINO',
  'ENTREGADO',
];

IconData _icono(String estado, bool delivery) => switch (estado) {
  'PENDIENTE' => Icons.receipt_long_outlined,
  'CONFIRMADO' => Icons.verified_outlined,
  'EN_PREPARACION' => Icons.soup_kitchen_outlined,
  'LISTO' => delivery ? Icons.inventory_2_outlined : Icons.storefront_outlined,
  'EN_CAMINO' => Icons.delivery_dining_outlined,
  'CANCELADO' => Icons.close,
  _ => Icons.home_outlined,
};

String _nombre(String estado, bool delivery) =>
    estado == 'LISTO' && !delivery ? 'Listo para recoger' : nombresEstadoPedido[estado] ?? estado;

/// El seguimiento del pedido como un stepper vertical (CU15, HU19): cada paso con la fecha y hora en que ocurrió,
/// el actual con lo que sigue, y los que faltan en gris. Reemplaza la lista del historial.
class SeguimientoPedido extends StatelessWidget {
  const SeguimientoPedido({
    super.key,
    required this.estado,
    required this.delivery,
    required this.historial,
    required this.horarioEntrega,
    this.mensaje = '',
    this.motivoCancelacion,
  });

  final String estado;
  final bool delivery;
  final List<Cambio> historial;
  final DateTime horarioEntrega;

  /// Qué sigue, en palabras del cliente; va bajo el paso actual.
  final String mensaje;
  final String? motivoCancelacion;

  @override
  Widget build(BuildContext context) {
    final pasos = pasosDelPedido(delivery: delivery);
    final cancelado = estado == 'CANCELADO';
    // Un pedido cancelado muestra los pasos que alcanzó y termina en Cancelado.
    final hasta = cancelado
        ? pasos.indexOf(historial.lastWhere((c) => c.nuevo == 'CANCELADO', orElse: _sinCambio).anterior ?? 'PENDIENTE')
        : pasos.indexOf(estado);
    final visibles = cancelado ? [...pasos.take(hasta + 1), 'CANCELADO'] : pasos;
    final actual = cancelado ? visibles.length - 1 : hasta;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        for (var i = 0; i < visibles.length; i++)
          _Paso(
            estado: visibles[i],
            delivery: delivery,
            etapa: i < actual ? _Etapa.cumplido : (i == actual ? _Etapa.actual : _Etapa.pendiente),
            cambio: i <= actual ? _ultimoCambio(visibles[i]) : null,
            ultimo: i == visibles.length - 1,
            lineaCumplida: i < actual,
            detalle: [
              if (i == actual && cancelado && motivoCancelacion != null) 'Motivo: $motivoCancelacion',
              if (i == visibles.length - 1 && !cancelado && i != actual)
                '${delivery ? 'Entrega pedida' : 'Recojo pedido'}: ${fechaHora(horarioEntrega)}',
            ],
            mensaje: i == actual ? mensaje : '',
          ),
      ],
    );
  }

  /// El último paso por ese estado: tras una entrega fallida el pedido vuelve a Listo y cuenta la fecha nueva.
  Cambio? _ultimoCambio(String estado) => historial.where((c) => c.nuevo == estado).lastOrNull;

  static Cambio _sinCambio() => Cambio(null, 'CANCELADO', DateTime.now(), null, null);
}

enum _Etapa { cumplido, actual, pendiente }

class _Paso extends StatelessWidget {
  const _Paso({
    required this.estado,
    required this.delivery,
    required this.etapa,
    required this.cambio,
    required this.ultimo,
    required this.lineaCumplida,
    required this.detalle,
    required this.mensaje,
  });

  final String estado;
  final bool delivery;
  final _Etapa etapa;
  final Cambio? cambio;
  final bool ultimo;
  final bool lineaCumplida;
  final List<String> detalle;
  final String mensaje;

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;
    final textos = Theme.of(context).textTheme;
    final cancelado = estado == 'CANCELADO';
    final pendiente = etapa == _Etapa.pendiente;
    final relleno = cancelado
        ? colores.errorContainer
        : (pendiente ? colores.surfaceContainerHighest : colores.primary);
    final tinta = cancelado ? colores.onErrorContainer : (pendiente ? colores.onSurfaceVariant : colores.onPrimary);
    final fecha = cambio == null ? null : fechaHora(cambio!.fecha);
    return MergeSemantics(
      child: IntrinsicHeight(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            SizedBox(
              width: 40,
              child: Column(
                children: [
                  Container(
                    width: 34,
                    height: 34,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: relleno,
                      border: etapa == _Etapa.actual && !cancelado
                          ? Border.all(color: colores.primaryContainer, width: 3)
                          : null,
                    ),
                    child: Icon(
                      etapa == _Etapa.cumplido ? Icons.check : _icono(estado, delivery),
                      size: 18,
                      color: tinta,
                    ),
                  ),
                  if (!ultimo)
                    Expanded(
                      child: Container(
                        width: lineaCumplida ? 3 : 2,
                        margin: const EdgeInsets.symmetric(vertical: 4),
                        color: lineaCumplida ? colores.primary : colores.outlineVariant,
                      ),
                    ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Padding(
                padding: EdgeInsets.only(top: 6, bottom: ultimo ? 0 : 18),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _nombre(estado, delivery),
                      style: (etapa == _Etapa.actual ? textos.titleMedium : textos.bodyLarge)?.copyWith(
                        color: pendiente ? colores.onSurfaceVariant : colores.onSurface,
                        fontWeight: etapa == _Etapa.actual ? FontWeight.w700 : null,
                      ),
                    ),
                    if (fecha != null || cambio?.observacion != null)
                      Text(
                        [?fecha, ?cambio?.observacion].join(' · '),
                        style: textos.bodySmall?.copyWith(color: colores.onSurfaceVariant),
                      ),
                    for (final d in detalle)
                      Text(d, style: textos.bodySmall?.copyWith(color: colores.onSurfaceVariant)),
                    if (mensaje.isNotEmpty) ...[
                      const SizedBox(height: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                        decoration: BoxDecoration(
                          color: colores.secondaryContainer,
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Text(mensaje, style: textos.bodyMedium?.copyWith(color: colores.onSecondaryContainer)),
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// La versión corta para la lista de pedidos: una barra por paso, en color hasta el actual.
class BarraProgreso extends StatelessWidget {
  const BarraProgreso({super.key, required this.estado, required this.delivery});

  final String estado;
  final bool delivery;

  @override
  Widget build(BuildContext context) {
    final pasos = pasosDelPedido(delivery: delivery);
    final actual = pasos.indexOf(estado);
    if (actual < 0) return const SizedBox.shrink();
    final colores = Theme.of(context).colorScheme;
    return Semantics(
      label: 'Paso ${actual + 1} de ${pasos.length}',
      excludeSemantics: true,
      child: Row(
        children: [
          for (var i = 0; i < pasos.length; i++) ...[
            if (i > 0) const SizedBox(width: 4),
            Expanded(
              child: Container(
                height: 6,
                decoration: BoxDecoration(
                  color: i <= actual ? colores.primary : colores.outlineVariant,
                  borderRadius: BorderRadius.circular(3),
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
