import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/widgets.dart';
import '../carrito.dart';

/// Confirmar el pedido con recojo o delivery (CU05, HU10, RN12).
class ConfirmarPantalla extends StatefulWidget {
  const ConfirmarPantalla({super.key});

  @override
  State<ConfirmarPantalla> createState() => _ConfirmarPantallaState();
}

class _ConfirmarPantallaState extends State<ConfirmarPantalla> {
  /// Sin valor inicial: el cliente elige a propósito, para no confirmar un recojo sin darse cuenta.
  String? _tipo;
  final _direccion = TextEditingController();
  final _telefono = TextEditingController();
  DateTime? _dia;
  TimeOfDay? _hora;
  Future<Cotizacion>? _cotizacion;
  ErrorApi? _error;
  bool _enviando = false;

  @override
  void initState() {
    super.initState();
    _cotizar();
  }

  @override
  void dispose() {
    _direccion.dispose();
    _telefono.dispose();
    super.dispose();
  }

  void _cotizar() {
    setState(() {
      _cotizacion = context
          .read<Api>()
          .post('/api/pedidos/cotizacion', {'lineas': context.read<Carrito>().aSolicitud(), 'tipoEntrega': _tipo})
          .then((j) => Cotizacion.deJson(j as Json));
    });
  }

  String? get _horario {
    if (_dia == null || _hora == null) return null;
    return instanteLaPaz(DateTime(_dia!.year, _dia!.month, _dia!.day, _hora!.hour, _hora!.minute));
  }

  Future<void> _elegirDia() async {
    final primero = primerDiaDeEntrega();
    final dia = await showDatePicker(
      context: context,
      initialDate: _dia ?? primero,
      firstDate: primero,
      lastDate: primero.add(const Duration(days: 60)),
      helpText: 'Día de entrega',
    );
    if (dia != null) setState(() => _dia = dia);
  }

  Future<void> _elegirHora() async {
    final hora = await showTimePicker(
      context: context,
      initialTime: _hora ?? const TimeOfDay(hour: 10, minute: 0),
      helpText: 'Hora de entrega',
    );
    if (hora != null) setState(() => _hora = hora);
  }

  Future<void> _confirmar() async {
    setState(() {
      _enviando = true;
      _error = null;
    });
    final carrito = context.read<Carrito>();
    try {
      final pedido = await context.read<Api>().post('/api/pedidos', {
        'lineas': carrito.aSolicitud(),
        'tipoEntrega': _tipo,
        'direccion': _tipo == 'DELIVERY' ? _direccion.text.trim() : null,
        'telefono': _telefono.text.trim(),
        'horarioEntrega': _horario,
      }) as Json;
      carrito.vaciar();
      if (mounted) context.go('/pedidos/${pedido['id']}/pagar');
    } on ErrorApi catch (e) {
      setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    final localizaciones = MaterialLocalizations.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Confirmar pedido')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Contenido(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('¿Cómo lo recibes?', style: textos.titleMedium),
              const SizedBox(height: 8),
              SegmentedButton<String>(
                segments: const [
                  ButtonSegment(
                    value: 'RECOJO',
                    label: Text('Recojo en tienda'),
                    icon: Icon(Icons.storefront_outlined),
                  ),
                  ButtonSegment(value: 'DELIVERY', label: Text('Delivery'), icon: Icon(Icons.delivery_dining_outlined)),
                ],
                emptySelectionAllowed: true,
                selected: {?_tipo},
                onSelectionChanged: (s) {
                  if (s.isEmpty) return;
                  _tipo = s.first;
                  _cotizar();
                },
              ),
              if (_error?.campos['tipoEntrega'] != null)
                Padding(
                  padding: const EdgeInsets.only(top: 4),
                  child: Text(
                    _error!.campos['tipoEntrega']!,
                    style: TextStyle(color: Theme.of(context).colorScheme.error),
                  ),
                ),
              const SizedBox(height: 16),
              if (_error != null && _error!.campos.isEmpty) AvisoError(_error!.mensaje),
              if (_error?.codigo == 'PRODUCTOS_NO_DISPONIBLES')
                Padding(
                  padding: const EdgeInsets.only(bottom: 16),
                  child: OutlinedButton(
                    onPressed: () => context.go('/carrito'),
                    child: const Text('Volver al carrito'),
                  ),
                ),
              // RN12: el delivery exige dirección; el recojo no.
              if (_tipo == 'DELIVERY')
                Campo(
                  controlador: _direccion,
                  etiqueta: 'Dirección de entrega',
                  nombre: 'direccion',
                  errores: _error?.campos,
                  lineas: 2,
                  autocompletar: const [AutofillHints.fullStreetAddress],
                ),
              Campo(
                controlador: _telefono,
                etiqueta: 'Teléfono de contacto',
                nombre: 'telefono',
                errores: _error?.campos,
                teclado: TextInputType.phone,
                autocompletar: const [AutofillHints.telephoneNumber],
              ),
              Text(switch (_tipo) {
                'DELIVERY' => 'Horario de entrega',
                'RECOJO' => 'Horario de recojo',
                _ => 'Horario',
              }, style: textos.titleMedium),
              Text(horarioDeAtencion, style: textos.bodySmall),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  OutlinedButton.icon(
                    onPressed: _elegirDia,
                    icon: const Icon(Icons.calendar_today_outlined),
                    label: Text(_dia == null ? 'Elegir día' : localizaciones.formatShortDate(_dia!)),
                  ),
                  OutlinedButton.icon(
                    onPressed: _elegirHora,
                    icon: const Icon(Icons.schedule),
                    label: Text(_hora == null ? 'Elegir hora' : localizaciones.formatTimeOfDay(_hora!)),
                  ),
                ],
              ),
              if (_error?.campos['horarioEntrega'] != null)
                Padding(
                  padding: const EdgeInsets.only(top: 4),
                  child: Text(
                    _error!.campos['horarioEntrega']!,
                    style: TextStyle(color: Theme.of(context).colorScheme.error),
                  ),
                ),
              const Divider(height: 32),
              if (_cotizacion != null)
                Cargador(
                  futuro: _cotizacion!,
                  reintentar: _cotizar,
                  constructor: (context, c) => Column(
                    children: [
                      if (c.costoEnvio.sign > 0)
                        Row(children: [const Text('Envío'), const Spacer(), Text(bolivianos(c.costoEnvio))]),
                      const SizedBox(height: 4),
                      Row(
                        children: [
                          Text('Total', style: textos.titleMedium),
                          const Spacer(),
                          Text(bolivianos(c.total), style: textos.titleLarge),
                        ],
                      ),
                      if (_tipo == null)
                        Align(
                          alignment: Alignment.centerRight,
                          child: Text('El envío se suma si eliges delivery.', style: textos.bodySmall),
                        ),
                    ],
                  ),
                ),
              const SizedBox(height: 24),
              FilledButton(onPressed: _enviando ? null : _confirmar, child: const Text('Confirmar pedido')),
              const SizedBox(height: 8),
              Text('Después de confirmar, registras el pago con tu comprobante.', style: textos.bodySmall),
            ],
          ),
        ),
      ),
    );
  }
}
