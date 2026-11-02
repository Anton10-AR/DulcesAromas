import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/widgets.dart';
import '../modelos_panel.dart';

/// Reporte de pedidos y ventas por fecha, estado y método de pago (CU10, HU14). Solo con el permiso de
/// reportes: el Vendedor no ve montos de ventas.
class ReportesPanel extends StatefulWidget {
  const ReportesPanel({super.key});

  @override
  State<ReportesPanel> createState() => _ReportesPanelState();
}

class _ReportesPanelState extends State<ReportesPanel> {
  late DateTime _desde;
  late DateTime _hasta;
  String? _estado;
  int? _metodo;
  List<MetodoPago> _metodos = [];
  Future<Reporte>? _reporte;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    final hoy = aLaPaz(DateTime.now());
    _hasta = DateTime(hoy.year, hoy.month, hoy.day);
    _desde = DateTime(hoy.year, hoy.month, 1);
    _api.get('/api/metodos-pago').then((j) => setState(() => _metodos = lista(j, MetodoPago.deJson)));
    _generar();
  }

  String _dia(DateTime d) => DateFormat('yyyy-MM-dd').format(d);

  void _generar() {
    setState(() {
      _reporte = _api
          .get('/api/reportes/ventas', {
            'desde': _dia(_desde),
            'hasta': _dia(_hasta),
            'estado': _estado,
            'metodoPago': _metodo?.toString(),
          })
          .then((j) => Reporte.deJson(j as Json));
    });
  }

  Future<void> _elegirRango() async {
    final rango = await showDateRangePicker(
      context: context,
      firstDate: DateTime(2025),
      lastDate: DateTime.now().add(const Duration(days: 1)),
      initialDateRange: DateTimeRange(start: _desde, end: _hasta),
      helpText: 'Período del reporte',
    );
    if (rango == null) return;
    _desde = rango.start;
    _hasta = rango.end;
    _generar();
  }

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    final formato = DateFormat('dd/MM/yyyy');
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Wrap(
          spacing: 16,
          runSpacing: 16,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            OutlinedButton.icon(
              onPressed: _elegirRango,
              icon: const Icon(Icons.date_range),
              label: Text('${formato.format(_desde)} – ${formato.format(_hasta)}'),
            ),
            SizedBox(
              width: 200,
              child: DropdownButtonFormField<String?>(
                initialValue: _estado,
                decoration: const InputDecoration(labelText: 'Estado'),
                items: [
                  const DropdownMenuItem(value: null, child: Text('Todos')),
                  for (final e in nombresEstadoPedido.entries) DropdownMenuItem(value: e.key, child: Text(e.value)),
                ],
                onChanged: (v) {
                  _estado = v;
                  _generar();
                },
              ),
            ),
            SizedBox(
              width: 200,
              child: DropdownButtonFormField<int?>(
                initialValue: _metodo,
                decoration: const InputDecoration(labelText: 'Método de pago'),
                items: [
                  const DropdownMenuItem(value: null, child: Text('Todos')),
                  for (final m in _metodos) DropdownMenuItem(value: m.id, child: Text(m.nombre)),
                ],
                onChanged: (v) {
                  _metodo = v;
                  _generar();
                },
              ),
            ),
          ],
        ),
        const SizedBox(height: 16),
        if (_reporte != null)
          Cargador(
            futuro: _reporte!,
            reintentar: _generar,
            constructor: (context, r) => Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Wrap(
                  spacing: 16,
                  runSpacing: 16,
                  children: [
                    _total('Pedidos', '${r.cantidadPedidos}'),
                    _total('Monto de los pedidos', bolivianos(r.montoPedidos)),
                    _total('Vendido (pagado y no cancelado)', bolivianos(r.montoVendido)),
                  ],
                ),
                const SizedBox(height: 16),
                // CU10, alt. 1: sin datos en el rango, se indica.
                if (r.pedidos.isEmpty) const Text('No hay pedidos en este período.'),
                for (final f in r.pedidos)
                  ListTile(
                    onTap: () => context.go('/pedidos/${f.pedidoId}'),
                    title: Text('Pedido #${f.pedidoId} · ${nombresEstadoPedido[f.estado]}'),
                    subtitle: Text('${fecha(f.fecha)} · ${f.pagado ? 'Pagado por ${f.metodo}' : 'Sin pago aprobado'}'),
                    trailing: Text(bolivianos(f.total), style: textos.titleSmall),
                  ),
              ],
            ),
          ),
      ],
    );
  }

  Widget _total(String titulo, String valor) => SizedBox(
    width: 240,
    child: Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(valor, style: Theme.of(context).textTheme.headlineSmall),
            Text(titulo),
          ],
        ),
      ),
    ),
  );
}
