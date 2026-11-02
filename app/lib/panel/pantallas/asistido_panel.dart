import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/widgets.dart';
import '../../tienda/carrito.dart';
import 'formulario.dart';

/// Pedido por teléfono o mostrador a nombre de un cliente (CU17, HU21, D7). Rigen las mismas reglas que en la
/// Tienda; las valida el servidor.
class AsistidoPanel extends StatefulWidget {
  const AsistidoPanel({super.key});

  @override
  State<AsistidoPanel> createState() => _AsistidoPanelState();
}

class _ClienteElegido {
  _ClienteElegido(this.id, this.nombre, this.correo, this.telefono);

  factory _ClienteElegido.deJson(Json j) =>
      _ClienteElegido(j['id'] as int, j['nombre'] as String, j['correo'] as String, j['telefono'] as String?);

  final int id;
  final String nombre;
  final String correo;
  final String? telefono;
}

class _AsistidoPanelState extends State<AsistidoPanel> {
  final _busqueda = TextEditingController();
  Timer? _espera;
  List<_ClienteElegido> _encontrados = [];
  _ClienteElegido? _cliente;

  List<Producto> _productos = [];
  final List<LineaCarrito> _lineas = [];

  String _tipo = 'RECOJO';
  final _direccion = TextEditingController();
  final _telefono = TextEditingController();
  DateTime? _dia;
  TimeOfDay? _hora;

  Future<Cotizacion>? _cotizacion;
  ErrorApi? _error;
  bool _enviando = false;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    _api.get('/api/catalogo/productos').then((j) => setState(() => _productos = lista(j, Producto.deJson)));
  }

  @override
  void dispose() {
    _espera?.cancel();
    for (final c in [_busqueda, _direccion, _telefono]) {
      c.dispose();
    }
    super.dispose();
  }

  void _buscar(String texto) {
    _espera?.cancel();
    _espera = Timer(const Duration(milliseconds: 350), () async {
      final j = await _api.get('/api/clientes', {'buscar': texto.trim()});
      if (mounted) setState(() => _encontrados = lista(j, _ClienteElegido.deJson));
    });
  }

  void _elegir(_ClienteElegido c) {
    setState(() {
      _cliente = c;
      if (_telefono.text.isEmpty) _telefono.text = c.telefono ?? '';
    });
  }

  /// CU17, alt. 1: si no existe, se registra; el cliente define su contraseña después con «¿Olvidó su contraseña?».
  Future<void> _registrarCliente() async {
    final nombre = CampoDialogo('nombre', 'Nombre');
    final correo = CampoDialogo('correo', 'Correo', teclado: TextInputType.emailAddress);
    final telefono = CampoDialogo('telefono', 'Teléfono', teclado: TextInputType.phone);
    final nuevo = await formularioEnDialogo(
      context,
      titulo: 'Registrar cliente',
      campos: [nombre, correo, telefono],
      confirmar: 'Registrar',
      enviar: () => _api.post('/api/clientes/asistidos', {
        'nombre': nombre.texto,
        'correo': correo.texto,
        'telefono': telefono.texto,
      }),
    );
    if (nuevo is Map) _elegir(_ClienteElegido.deJson(nuevo as Json));
  }

  Future<void> _agregarProducto() async {
    final linea = await showDialog<LineaCarrito>(
      context: context,
      builder: (_) => _DialogoLinea(productos: _productos),
    );
    if (linea == null) return;
    setState(() => _lineas.add(linea));
    _cotizar();
  }

  Map<String, dynamic> get _solicitud => {
    'lineas': _lineas.map((l) => l.aSolicitud()).toList(),
    'tipoEntrega': _tipo,
    'direccion': _tipo == 'DELIVERY' ? _direccion.text.trim() : null,
    'telefono': _telefono.text.trim(),
    'horarioEntrega': _dia == null || _hora == null
        ? null
        : instanteLaPaz(DateTime(_dia!.year, _dia!.month, _dia!.day, _hora!.hour, _hora!.minute)),
  };

  void _cotizar() {
    setState(() {
      _cotizacion = _lineas.isEmpty
          ? null
          : _api.post('/api/pedidos/cotizacion', _solicitud).then((j) => Cotizacion.deJson(j as Json));
    });
  }

  Future<void> _confirmar() async {
    setState(() {
      _enviando = true;
      _error = null;
    });
    try {
      final pedido =
          await _api.post('/api/pedidos/asistidos', {'clienteId': _cliente?.id, 'pedido': _solicitud}) as Json;
      if (mounted) context.go('/pedidos/${pedido['id']}');
    } on ErrorApi catch (e) {
      setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    final errores = _error?.campos.map((k, v) => MapEntry(k.replaceFirst('pedido.', ''), v));
    final localizaciones = MaterialLocalizations.of(context);
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Contenido(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('1. Cliente', style: textos.titleLarge),
            const SizedBox(height: 8),
            if (_cliente != null)
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.person),
                title: Text(_cliente!.nombre),
                subtitle: Text('${_cliente!.correo} · ${_cliente!.telefono ?? ''}'),
                trailing: TextButton(onPressed: () => setState(() => _cliente = null), child: const Text('Cambiar')),
              )
            else ...[
              if (errores?['clienteId'] != null) AvisoError(errores!['clienteId']!),
              TextField(
                controller: _busqueda,
                onChanged: _buscar,
                decoration: const InputDecoration(
                  prefixIcon: Icon(Icons.search),
                  labelText: 'Buscar por correo o teléfono',
                ),
              ),
              for (final c in _encontrados)
                ListTile(
                  title: Text(c.nombre),
                  subtitle: Text('${c.correo} · ${c.telefono ?? ''}'),
                  onTap: () => _elegir(c),
                ),
              Align(
                alignment: Alignment.centerLeft,
                child: TextButton.icon(
                  onPressed: _registrarCliente,
                  icon: const Icon(Icons.person_add_alt),
                  label: const Text('Registrar cliente nuevo'),
                ),
              ),
            ],
            const Divider(height: 32),
            Text('2. Productos', style: textos.titleLarge),
            for (var i = 0; i < _lineas.length; i++)
              ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text('${_lineas[i].cantidad} × ${_lineas[i].nombre}'),
                subtitle: Text(
                  [
                    ..._lineas[i].nombresOpciones,
                    if (_lineas[i].mensaje != null) '«${_lineas[i].mensaje}»',
                  ].join(' · '),
                ),
                trailing: IconButton(
                  tooltip: 'Quitar',
                  onPressed: () {
                    setState(() => _lineas.removeAt(i));
                    _cotizar();
                  },
                  icon: const Icon(Icons.delete_outline),
                ),
              ),
            Align(
              alignment: Alignment.centerLeft,
              child: OutlinedButton.icon(
                onPressed: _productos.isEmpty ? null : _agregarProducto,
                icon: const Icon(Icons.add),
                label: const Text('Agregar producto'),
              ),
            ),
            const Divider(height: 32),
            Text('3. Entrega', style: textos.titleLarge),
            const SizedBox(height: 8),
            SegmentedButton<String>(
              segments: const [
                ButtonSegment(value: 'RECOJO', label: Text('Recojo en tienda')),
                ButtonSegment(value: 'DELIVERY', label: Text('Delivery')),
              ],
              selected: {_tipo},
              onSelectionChanged: (s) {
                _tipo = s.first;
                _cotizar();
              },
            ),
            const SizedBox(height: 16),
            if (_tipo == 'DELIVERY')
              Campo(
                controlador: _direccion,
                etiqueta: 'Dirección de entrega',
                nombre: 'direccion',
                errores: errores,
                lineas: 2,
              ),
            Campo(
              controlador: _telefono,
              etiqueta: 'Teléfono',
              nombre: 'telefono',
              errores: errores,
              teclado: TextInputType.phone,
            ),
            Wrap(
              spacing: 8,
              children: [
                OutlinedButton.icon(
                  onPressed: () async {
                    final hoy = DateTime.now();
                    final dia = await showDatePicker(
                      context: context,
                      firstDate: DateTime(hoy.year, hoy.month, hoy.day),
                      lastDate: hoy.add(const Duration(days: 60)),
                    );
                    if (dia != null) setState(() => _dia = dia);
                  },
                  icon: const Icon(Icons.calendar_today_outlined),
                  label: Text(_dia == null ? 'Día' : localizaciones.formatShortDate(_dia!)),
                ),
                OutlinedButton.icon(
                  onPressed: () async {
                    final hora = await showTimePicker(
                      context: context,
                      initialTime: const TimeOfDay(hour: 10, minute: 0),
                    );
                    if (hora != null) setState(() => _hora = hora);
                  },
                  icon: const Icon(Icons.schedule),
                  label: Text(_hora == null ? 'Hora' : localizaciones.formatTimeOfDay(_hora!)),
                ),
              ],
            ),
            if (errores?['horarioEntrega'] != null)
              Text(errores!['horarioEntrega']!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
            const Divider(height: 32),
            if (_cotizacion != null)
              Cargador(
                futuro: _cotizacion!,
                reintentar: _cotizar,
                constructor: (context, c) => Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    if (!c.confirmable) AvisoError('No están disponibles: ${c.noDisponibles.join(', ')}.'),
                    Row(
                      children: [
                        Text('Total', style: textos.titleMedium),
                        const Spacer(),
                        Text(bolivianos(c.total), style: textos.titleLarge),
                      ],
                    ),
                  ],
                ),
              ),
            const SizedBox(height: 16),
            if (_error != null && _error!.campos.isEmpty) AvisoError(_error!.mensaje),
            FilledButton(
              onPressed: _enviando || _lineas.isEmpty ? null : _confirmar,
              child: const Text('Registrar pedido asistido'),
            ),
          ],
        ),
      ),
    );
  }
}

/// Elegir un producto con sus opciones, mensaje y cantidad.
class _DialogoLinea extends StatefulWidget {
  const _DialogoLinea({required this.productos});

  final List<Producto> productos;

  @override
  State<_DialogoLinea> createState() => _DialogoLineaState();
}

class _DialogoLineaState extends State<_DialogoLinea> {
  Producto? _producto;
  final Map<String, Opcion> _elegidas = {};
  final _mensaje = TextEditingController();
  int _cantidad = 1;

  @override
  void dispose() {
    _mensaje.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final p = _producto;
    return AlertDialog(
      title: const Text('Agregar producto'),
      content: SizedBox(
        width: 460,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              DropdownButtonFormField<Producto>(
                initialValue: p,
                isExpanded: true,
                decoration: const InputDecoration(labelText: 'Producto'),
                items: [
                  for (final pr in widget.productos)
                    DropdownMenuItem(
                      value: pr,
                      enabled: pr.disponible,
                      child: Text('${pr.nombre} · ${bolivianos(pr.precio)}${pr.disponible ? '' : ' (no disponible)'}'),
                    ),
                ],
                onChanged: (v) => setState(() {
                  _producto = v;
                  _elegidas.clear();
                }),
              ),
              if (p != null)
                for (final grupo in p.opcionesPorTipo.entries) ...[
                  const SizedBox(height: 12),
                  Text(grupo.key),
                  Wrap(
                    spacing: 8,
                    children: [
                      for (final o in grupo.value)
                        ChoiceChip(
                          label: Text('${o.nombre} (+${bolivianos(o.recargo)})'),
                          selected: _elegidas[grupo.key] == o,
                          onSelected: (m) => setState(() => m ? _elegidas[grupo.key] = o : _elegidas.remove(grupo.key)),
                        ),
                    ],
                  ),
                ],
              if (p != null && p.personalizable) ...[
                const SizedBox(height: 12),
                TextField(
                  controller: _mensaje,
                  maxLength: 24,
                  decoration: const InputDecoration(labelText: 'Mensaje sobre la torta'),
                ),
              ],
              const SizedBox(height: 12),
              Row(
                children: [
                  const Text('Cantidad'),
                  IconButton(
                    onPressed: _cantidad > 1 ? () => setState(() => _cantidad--) : null,
                    icon: const Icon(Icons.remove),
                  ),
                  Text('$_cantidad'),
                  IconButton(
                    onPressed: _cantidad < 99 ? () => setState(() => _cantidad++) : null,
                    icon: const Icon(Icons.add),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
        FilledButton(
          onPressed: p == null
              ? null
              : () {
                  final opciones = p.opciones.where((o) => _elegidas[o.tipo] == o).toList();
                  Navigator.pop(
                    context,
                    LineaCarrito(
                      productoId: p.id,
                      nombre: p.nombre,
                      cantidad: _cantidad,
                      opciones: opciones.map((o) => o.id!).toList(),
                      nombresOpciones: opciones.map((o) => o.nombre).toList(),
                      mensaje: _mensaje.text.trim().isEmpty ? null : _mensaje.text.trim(),
                    ),
                  );
                },
          child: const Text('Agregar'),
        ),
      ],
    );
  }
}
