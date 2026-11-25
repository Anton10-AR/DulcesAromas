import 'dart:convert';

import 'package:flutter/material.dart';

import '../api.dart';
import '../formato.dart';
import '../modelos.dart';
import '../widgets.dart';
import 'impresora.dart';

/// El ticket de un pedido pagado. No lleva nombre, teléfono ni dirección, porque el papel puede quedar a la vista.
///
/// No fija el tamaño del papel: ocupa el ancho que dé la impresora, hasta 80 mm, y se acomoda a un rollo de 58 mm o a
/// una hoja. Deja 5 mm a cada lado porque las térmicas no imprimen el borde del rollo; si la impresora ya lo descuenta y
/// da 48 mm, usa casi todo. Todo va en negro, porque las térmicas no tienen grises.
String ticketHtml(Pedido pedido, Pago pago) {
  const escapar = HtmlEscape(HtmlEscapeMode.element);
  String fila(String texto, String valor, {String clase = ''}) =>
      '<div class="fila $clase"><span>${escapar.convert(texto)}</span><span>${escapar.convert(valor)}</span></div>';
  String parrafo(String texto, {String clase = ''}) => '<p class="$clase">${escapar.convert(texto)}</p>';

  final cuerpo = StringBuffer()
    ..write(parrafo('Dulces Aromas', clase: 'marca'))
    ..write(parrafo('Pastelería · Sucre, Bolivia', clase: 'centro'))
    ..write('<hr>')
    ..write(parrafo('Pedido', clase: 'centro'))
    ..write(parrafo('#${pedido.id}', clase: 'numero'))
    ..write(parrafo(fechaHora(pedido.fechaCreacion), clase: 'centro'))
    ..write('<hr>');
  for (final l in pedido.lineas) {
    cuerpo
      ..write('<div class="linea">')
      ..write(fila('${l.cantidad} × ${l.nombre}', bolivianos(l.subtotal)));
    for (final o in l.opciones) {
      cuerpo.write(parrafo(o.nombre, clase: 'detalle'));
    }
    if (l.mensaje != null) cuerpo.write(parrafo('Mensaje: «${l.mensaje}»', clase: 'detalle'));
    cuerpo.write('</div>');
  }
  cuerpo.write('<hr>');
  if (pedido.esDelivery) cuerpo.write(fila('Envío', bolivianos(pedido.costoEnvio)));
  cuerpo
    ..write(fila('TOTAL', bolivianos(pedido.total), clase: 'total'))
    ..write('<hr>')
    ..write(fila('Pago', 'Pagado'));
  if (pago.metodo != null) cuerpo.write(fila('Método', pago.metodo!));
  if (pago.referencia != null) cuerpo.write(fila('Referencia', pago.referencia!));
  cuerpo
    ..write(fila(pedido.esDelivery ? 'Delivery' : 'Recojo', fechaHora(pedido.horarioEntrega)))
    ..write('<hr>')
    ..write(parrafo('¡Gracias por tu compra!', clase: 'centro'))
    ..write(parrafo('Comprobante de pedido. No es factura.', clase: 'pie'))
    ..write(parrafo('Demostración académica: no se cobró dinero real.', clase: 'pie'));
  return '<style>$_estilo</style><div class="ticket">$cuerpo</div>';
}

const _estilo = '''
@page { margin: 0; }
.ticket {
  box-sizing: border-box; width: 100%; max-width: 80mm; margin: 0 auto; padding: 3mm 5mm 6mm;
  font: 10pt/1.3 Arial, Roboto, sans-serif; color: #000; background: #fff;
}
@media (max-width: 52mm) { .ticket { padding-left: 1mm; padding-right: 1mm; } }
.ticket p { margin: 0; overflow-wrap: anywhere; }
.ticket hr { border: 0; border-top: 1px dashed #000; margin: 2mm 0; }
.ticket .marca { font: 700 17pt/1.2 Georgia, serif; text-align: center; }
.ticket .numero { font-size: 30pt; font-weight: 700; line-height: 1.1; text-align: center; }
.ticket .centro, .ticket .pie { text-align: center; }
.ticket .pie { font-size: 8pt; margin-top: 1mm; }
.ticket .linea { break-inside: avoid; margin-bottom: 1.5mm; }
.ticket .detalle { padding-left: 4mm; font-size: 9pt; }
.ticket .fila { display: flex; justify-content: space-between; gap: 3mm; }
.ticket .fila span { overflow-wrap: anywhere; }
.ticket .fila span:last-child { text-align: right; flex: none; max-width: 65%; }
.ticket .total { font-size: 13pt; font-weight: 700; }
''';

/// «Imprimir ticket» de un pedido ya pagado. Si la pantalla no tiene el pedido, se consulta al pulsar.
class BotonTicket extends StatefulWidget {
  const BotonTicket({super.key, required this.pedido, required this.pago, this.principal = false});

  final Future<Pedido> Function() pedido;
  final Pago pago;
  final bool principal;

  /// El ticket acredita el pago: solo se ofrece con el pago Pagado, y donde se puede imprimir.
  static bool seOfrece(Pago? pago, {String? estadoPedido}) =>
      Impresora.actual.disponible && pago?.estado == 'PAGADO' && estadoPedido != 'CANCELADO';

  @override
  State<BotonTicket> createState() => _BotonTicketState();
}

class _BotonTicketState extends State<BotonTicket> {
  bool _ocupado = false;

  Future<void> _imprimir() async {
    setState(() => _ocupado = true);
    try {
      Impresora.actual.imprimir(ticketHtml(await widget.pedido(), widget.pago));
    } on ErrorApi catch (e) {
      if (mounted) avisar(context, e.mensaje);
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    const icono = Icon(Icons.print_outlined);
    const texto = Text('Imprimir ticket');
    final accion = _ocupado ? null : _imprimir;
    return widget.principal
        ? FilledButton.icon(onPressed: accion, icon: icono, label: texto)
        : OutlinedButton.icon(onPressed: accion, icon: icono, label: texto);
  }
}
