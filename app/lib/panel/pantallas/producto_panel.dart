import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/modelos.dart';
import '../../comun/widgets.dart';

/// Registrar o editar un producto con sus opciones e imagen (CU02, HU08).
class ProductoPanel extends StatefulWidget {
  const ProductoPanel({super.key, this.id});

  /// Nulo para un producto nuevo.
  final int? id;

  @override
  State<ProductoPanel> createState() => _ProductoPanelState();
}

class _FilaOpcion {
  _FilaOpcion([Opcion? o])
    : tipo = TextEditingController(text: o?.tipo),
      nombre = TextEditingController(text: o?.nombre),
      recargo = TextEditingController(text: o?.recargo.toString() ?? '0');

  final TextEditingController tipo;
  final TextEditingController nombre;
  final TextEditingController recargo;

  void dispose() {
    tipo.dispose();
    nombre.dispose();
    recargo.dispose();
  }
}

class _ProductoPanelState extends State<ProductoPanel> {
  final _nombre = TextEditingController();
  final _descripcion = TextEditingController();
  final _precio = TextEditingController();
  final List<_FilaOpcion> _opciones = [];
  List<CategoriaPanel> _categorias = [];
  int? _categoria;
  bool _disponible = true;
  bool _personalizable = false;
  Producto? _producto;
  ErrorApi? _error;
  bool _cargado = false;
  bool _enviando = false;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  @override
  void dispose() {
    for (final c in [_nombre, _descripcion, _precio]) {
      c.dispose();
    }
    for (final o in _opciones) {
      o.dispose();
    }
    super.dispose();
  }

  Future<void> _cargar() async {
    final categorias = lista(await _api.get('/api/categorias'), CategoriaPanel.deJson);
    Producto? producto;
    if (widget.id != null) producto = Producto.deJson(await _api.get('/api/productos/${widget.id}') as Json);
    setState(() {
      _categorias = categorias;
      _mostrar(producto);
      _cargado = true;
    });
  }

  void _mostrar(Producto? p) {
    _producto = p;
    if (p == null) return;
    _nombre.text = p.nombre;
    _descripcion.text = p.descripcion;
    _precio.text = p.precio.toString();
    _categoria = p.categoria.id;
    _disponible = p.disponible;
    _personalizable = p.personalizable;
    for (final o in _opciones) {
      o.dispose();
    }
    _opciones
      ..clear()
      ..addAll(p.opciones.map(_FilaOpcion.new));
  }

  /// Los montos van como texto, para que el servidor los lea como decimales exactos.
  Map<String, dynamic> get _datos => {
    'nombre': _nombre.text.trim(),
    'descripcion': _descripcion.text.trim(),
    'precio': _precio.text.trim().replaceAll(',', '.'),
    'categoriaId': _categoria,
    'disponible': _disponible,
    'personalizable': _personalizable,
    'opciones': _personalizable
        ? [
            for (final o in _opciones)
              {
                'tipo': o.tipo.text.trim(),
                'nombre': o.nombre.text.trim(),
                'recargo': o.recargo.text.trim().replaceAll(',', '.'),
              },
          ]
        : [],
  };

  Future<void> _guardar() async {
    setState(() {
      _enviando = true;
      _error = null;
    });
    try {
      final json = _producto == null
          ? await _api.post('/api/productos', _datos)
          : await _api.put('/api/productos/${_producto!.id}', _datos);
      final guardado = Producto.deJson(json as Json);
      if (!mounted) return;
      avisar(context, 'Producto guardado.');
      if (_producto == null) {
        context.go('/catalogo/${guardado.id}');
      } else {
        setState(() => _mostrar(guardado));
      }
    } on ErrorApi catch (e) {
      setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  /// RNF10: JPG, PNG o WebP, cuadrada, desde 800 px y hasta 2 MB; el servidor la valida.
  Future<void> _subirImagen() async {
    final archivo = await FilePicker.pickFile(
      type: FileType.custom,
      allowedExtensions: const ['jpg', 'jpeg', 'png', 'webp'],
    );
    if (archivo == null) return;
    try {
      final json = await _api.multipart(
        'PUT',
        '/api/productos/${_producto!.id}/imagen',
        nombreArchivo: 'archivo',
        bytes: await archivo.readAsBytes(),
        archivo: archivo.name,
      );
      setState(() => _mostrar(Producto.deJson(json as Json)));
      if (mounted) avisar(context, 'Imagen actualizada.');
    } on ErrorApi catch (e) {
      if (mounted) avisar(context, e.mensaje);
    }
  }

  /// RN10: un producto se desactiva; nunca se borra.
  Future<void> _activo(bool activo) async {
    try {
      final json = await _api.post('/api/productos/${_producto!.id}/${activo ? 'activar' : 'desactivar'}');
      setState(() => _mostrar(Producto.deJson(json as Json)));
    } on ErrorApi catch (e) {
      if (mounted) avisar(context, e.mensaje);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!_cargado) return const Center(child: CircularProgressIndicator());
    final textos = Theme.of(context).textTheme;
    final errores = _error?.campos;
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Contenido(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(_producto == null ? 'Nuevo producto' : _producto!.nombre, style: textos.headlineSmall),
            const SizedBox(height: 16),
            if (_error != null && (_error!.campos.isEmpty || _error!.codigo != 'DATOS_INVALIDOS'))
              AvisoError(_error!.mensaje),
            Campo(controlador: _nombre, etiqueta: 'Nombre', nombre: 'nombre', errores: errores),
            Campo(
              controlador: _descripcion,
              etiqueta: 'Descripción',
              nombre: 'descripcion',
              errores: errores,
              lineas: 3,
            ),
            Campo(
              controlador: _precio,
              etiqueta: 'Precio (Bs)',
              nombre: 'precio',
              errores: errores,
              teclado: const TextInputType.numberWithOptions(decimal: true),
            ),
            DropdownButtonFormField<int>(
              initialValue: _categoria,
              decoration: InputDecoration(labelText: 'Categoría', errorText: errores?['categoriaId']),
              items: [
                for (final c in _categorias.where((c) => c.activa || c.id == _categoria))
                  DropdownMenuItem(value: c.id, child: Text(c.nombre)),
              ],
              onChanged: (v) => setState(() => _categoria = v),
            ),
            SwitchListTile(
              title: const Text('Disponible'),
              value: _disponible,
              onChanged: (v) => setState(() => _disponible = v),
            ),
            SwitchListTile(
              title: const Text('Personalizable'),
              subtitle: const Text('Tiene opciones como tamaño o sabor y admite un mensaje sobre la torta.'),
              value: _personalizable,
              onChanged: (v) => setState(() => _personalizable = v),
            ),
            if (_personalizable) ..._filasOpciones(errores),
            const SizedBox(height: 16),
            FilledButton(onPressed: _enviando ? null : _guardar, child: const Text('Guardar')),
            if (_producto != null) ...[
              const Divider(height: 32),
              Text('Imagen', style: textos.titleLarge),
              const SizedBox(height: 8),
              if (_producto!.imagen != null)
                Align(
                  alignment: Alignment.centerLeft,
                  child: Image.network(_api.absoluta(_producto!.imagen!), width: 200, height: 200),
                ),
              const SizedBox(height: 8),
              OutlinedButton.icon(
                onPressed: _subirImagen,
                icon: const Icon(Icons.image_outlined),
                label: const Text('Cambiar imagen (cuadrada, desde 800 px, hasta 2 MB)'),
              ),
              const Divider(height: 32),
              _producto!.activo
                  ? OutlinedButton(onPressed: () => _activo(false), child: const Text('Desactivar producto'))
                  : FilledButton(onPressed: () => _activo(true), child: const Text('Activar producto')),
            ],
          ],
        ),
      ),
    );
  }

  /// D9: cada opción tiene un recargo fijo; el cliente elige como máximo una de cada tipo.
  List<Widget> _filasOpciones(Map<String, String>? errores) {
    return [
      Text('Opciones de personalización', style: Theme.of(context).textTheme.titleMedium),
      const SizedBox(height: 8),
      for (var i = 0; i < _opciones.length; i++)
        Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(child: _campoOpcion(_opciones[i].tipo, 'Tipo', 'opciones[$i].tipo', errores)),
              const SizedBox(width: 8),
              Expanded(child: _campoOpcion(_opciones[i].nombre, 'Opción', 'opciones[$i].nombre', errores)),
              const SizedBox(width: 8),
              SizedBox(
                width: 110,
                child: _campoOpcion(_opciones[i].recargo, 'Recargo', 'opciones[$i].recargo', errores),
              ),
              IconButton(
                tooltip: 'Quitar opción',
                onPressed: () => setState(() => _opciones.removeAt(i).dispose()),
                icon: const Icon(Icons.close),
              ),
            ],
          ),
        ),
      Align(
        alignment: Alignment.centerLeft,
        child: TextButton.icon(
          onPressed: () => setState(() => _opciones.add(_FilaOpcion())),
          icon: const Icon(Icons.add),
          label: const Text('Agregar opción'),
        ),
      ),
    ];
  }

  Widget _campoOpcion(TextEditingController c, String etiqueta, String nombre, Map<String, String>? errores) =>
      TextField(
        controller: c,
        decoration: InputDecoration(labelText: etiqueta, errorText: errores?[nombre], errorMaxLines: 3),
      );
}
