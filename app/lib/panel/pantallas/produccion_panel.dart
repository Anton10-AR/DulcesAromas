import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/tema.dart';
import '../../comun/widgets.dart';
import '../modelos_panel.dart';

/// El máximo por pedido (RN18) y la receta aproximada de un producto, con sus minutos por unidad (RF45, D12). Se
/// guarda aparte de los datos que ve el cliente, porque la receta no se publica en el catálogo.
class ProduccionPanel extends StatefulWidget {
  const ProduccionPanel({super.key, required this.productoId});

  final int productoId;

  @override
  State<ProduccionPanel> createState() => _ProduccionPanelState();
}

class _Ingrediente {
  _Ingrediente({this.insumoId, String cantidad = ''}) : cantidad = TextEditingController(text: cantidad);

  int? insumoId;
  final TextEditingController cantidad;
}

class _ProduccionPanelState extends State<ProduccionPanel> {
  final _maximo = TextEditingController();
  final _minutos = TextEditingController();
  final List<_Ingrediente> _receta = [];
  List<ItemInventario> _insumos = [];
  bool _cargado = false;
  bool _enviando = false;
  ErrorApi? _error;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  @override
  void dispose() {
    _maximo.dispose();
    _minutos.dispose();
    for (final i in _receta) {
      i.cantidad.dispose();
    }
    super.dispose();
  }

  Future<void> _cargar() async {
    try {
      final insumos = lista(await _api.get('/api/inventario'), ItemInventario.deJson);
      final produccion = await _api.get('/api/productos/${widget.productoId}/produccion') as Json;
      if (!mounted) return;
      setState(() {
        _insumos = insumos;
        _mostrar(produccion);
        _cargado = true;
      });
    } on ErrorApi catch (e) {
      if (mounted) setState(() => _error = e);
    }
  }

  void _mostrar(Json p) {
    _maximo.text = p['maximoPorPedido']?.toString() ?? '';
    _minutos.text = p['minutosPorUnidad']?.toString() ?? '';
    for (final i in _receta) {
      i.cantidad.dispose();
    }
    _receta
      ..clear()
      ..addAll([
        for (final i in p['receta'] as List)
          _Ingrediente(insumoId: (i as Json)['insumoId'] as int, cantidad: decimalDe(i['cantidad']).toString()),
      ]);
  }

  Future<void> _guardar() async {
    setState(() {
      _enviando = true;
      _error = null;
    });
    try {
      final json = await _api.put('/api/productos/${widget.productoId}/produccion', {
        'maximoPorPedido': int.tryParse(_maximo.text.trim()),
        'minutosPorUnidad': int.tryParse(_minutos.text.trim()),
        'receta': [
          for (final i in _receta)
            if (i.insumoId != null) {'insumoId': i.insumoId, 'cantidad': i.cantidad.text.trim().replaceAll(',', '.')},
        ],
      });
      if (!mounted) return;
      setState(() => _mostrar(json as Json));
      avisar(context, 'Producción guardada.');
    } on ErrorApi catch (e) {
      setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    if (!_cargado) {
      return _error == null ? const LinearProgressIndicator() : AvisoError(_error!.mensaje);
    }
    final errores = _error?.campos;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text('Producción', style: textos.titleLarge),
        const SizedBox(height: 4),
        Text(
          'El máximo limita cuántas unidades acepta un pedido. La receta es aproximada: solo sirve para estimar insumos '
          'y tiempo al verificar un pago, y no descuenta stock.',
          style: textos.bodyMedium,
        ),
        const SizedBox(height: 12),
        if (_error != null && (_error!.campos.isEmpty || _error!.codigo != 'DATOS_INVALIDOS'))
          AvisoError(_error!.mensaje),
        Wrap(
          spacing: 12,
          children: [
            SizedBox(
              width: 220,
              child: Campo(
                controlador: _maximo,
                etiqueta: 'Máximo por pedido',
                nombre: 'maximoPorPedido',
                errores: errores,
                teclado: TextInputType.number,
                ayuda: 'Vacío: hasta 99',
              ),
            ),
            SizedBox(
              width: 220,
              child: Campo(
                controlador: _minutos,
                etiqueta: 'Minutos por unidad',
                nombre: 'minutosPorUnidad',
                errores: errores,
                teclado: TextInputType.number,
              ),
            ),
          ],
        ),
        Text('Insumos por unidad', style: textos.titleMedium),
        if (errores?['receta'] != null)
          Text(errores!['receta']!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
        const SizedBox(height: 8),
        if (_insumos.isEmpty) const Text('Primero registra los insumos en Inventario.'),
        for (var i = 0; i < _receta.length; i++)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: DropdownButtonFormField<int>(
                    initialValue: _receta[i].insumoId,
                    isExpanded: true,
                    decoration: const InputDecoration(labelText: 'Insumo'),
                    items: [
                      for (final insumo in _insumos)
                        DropdownMenuItem(value: insumo.id, child: Text('${insumo.nombre} (${insumo.unidad})')),
                    ],
                    onChanged: (v) => setState(() => _receta[i].insumoId = v),
                  ),
                ),
                const SizedBox(width: 8),
                SizedBox(
                  width: 120,
                  child: TextField(
                    controller: _receta[i].cantidad,
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    decoration: InputDecoration(
                      labelText: 'Cantidad',
                      suffixText: _insumos.where((x) => x.id == _receta[i].insumoId).firstOrNull?.unidad,
                      errorText: errores?['receta[$i].cantidad'],
                      errorMaxLines: 3,
                    ),
                  ),
                ),
                IconButton(
                  tooltip: 'Quitar insumo',
                  onPressed: () => setState(() => _receta.removeAt(i).cantidad.dispose()),
                  icon: const Icon(Icons.close),
                ),
              ],
            ),
          ),
        Align(
          alignment: Alignment.centerLeft,
          child: TextButton.icon(
            onPressed: _insumos.isEmpty ? null : () => setState(() => _receta.add(_Ingrediente())),
            icon: const Icon(Icons.add),
            label: const Text('Agregar insumo'),
          ),
        ),
        const SizedBox(height: 8),
        FilledButton.tonal(
          style: estiloTonal(context),
          onPressed: _enviando ? null : _guardar,
          child: const Text('Guardar producción'),
        ),
      ],
    );
  }
}
