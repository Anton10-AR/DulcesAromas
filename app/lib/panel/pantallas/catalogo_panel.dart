import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/sesion.dart';
import '../../comun/widgets.dart';
import 'dialogos.dart';

/// Catálogo en el Panel: productos y categorías (CU02, CU14, HU08, HU18). El Vendedor solo cambia la
/// disponibilidad.
class CatalogoPanel extends StatelessWidget {
  const CatalogoPanel({super.key});

  @override
  Widget build(BuildContext context) {
    final gestiona = context.watch<Sesion>().usuario!.puede('CATALOGO_GESTIONAR');
    return DefaultTabController(
      length: 2,
      child: Column(
        children: [
          const TabBar(
            tabs: [
              Tab(text: 'Productos'),
              Tab(text: 'Categorías'),
            ],
          ),
          Expanded(
            child: TabBarView(
              children: [
                _Productos(gestiona: gestiona),
                _Categorias(gestiona: gestiona),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _Productos extends StatefulWidget {
  const _Productos({required this.gestiona});

  final bool gestiona;

  @override
  State<_Productos> createState() => _ProductosState();
}

class _ProductosState extends State<_Productos> {
  late Future<List<Producto>> _productos;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  void _cargar() {
    setState(() {
      _productos = _api.get('/api/productos').then((j) => lista(j, Producto.deJson));
    });
  }

  /// D2: la disponibilidad es un indicador manual.
  Future<void> _disponibilidad(Producto p, bool disponible) async {
    try {
      await _api.put('/api/productos/${p.id}/disponibilidad', {'disponible': disponible});
    } on ErrorApi catch (e) {
      if (mounted) avisar(context, e.mensaje);
    }
    _cargar();
  }

  @override
  Widget build(BuildContext context) {
    final puedeDisponibilidad = context.watch<Sesion>().usuario!.puede('CATALOGO_DISPONIBILIDAD');
    return Scaffold(
      floatingActionButton: widget.gestiona
          ? FloatingActionButton.extended(
              onPressed: () => context.go('/catalogo/nuevo'),
              icon: const Icon(Icons.add),
              label: const Text('Nuevo producto'),
            )
          : null,
      body: Cargador(
        futuro: _productos,
        reintentar: _cargar,
        constructor: (context, productos) {
          if (productos.isEmpty) return const Center(child: Text('Todavía no hay productos.'));
          return ListView.separated(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 88),
            itemCount: productos.length,
            separatorBuilder: (_, _) => const Divider(height: 1),
            itemBuilder: (_, i) {
              final p = productos[i];
              return ListTile(
                onTap: widget.gestiona ? () => context.go('/catalogo/${p.id}') : null,
                title: Text('${p.nombre}${p.activo ? '' : ' (desactivado)'}'),
                subtitle: Text('${p.categoria.nombre} · ${bolivianos(p.precio)}'),
                trailing: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(p.disponible ? 'Disponible' : 'No disponible'),
                    Switch(value: p.disponible, onChanged: puedeDisponibilidad ? (v) => _disponibilidad(p, v) : null),
                  ],
                ),
              );
            },
          );
        },
      ),
    );
  }
}

class _Categorias extends StatefulWidget {
  const _Categorias({required this.gestiona});

  final bool gestiona;

  @override
  State<_Categorias> createState() => _CategoriasState();
}

class _CategoriasState extends State<_Categorias> {
  late Future<List<CategoriaPanel>> _categorias;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  void _cargar() {
    setState(() {
      _categorias = _api.get('/api/categorias').then((j) => lista(j, CategoriaPanel.deJson));
    });
  }

  Future<void> _ejecutar(Future<dynamic> Function() llamada) async {
    try {
      await llamada();
    } on ErrorApi catch (e) {
      if (mounted) avisar(context, e.campos.values.firstOrNull ?? e.mensaje);
    }
    _cargar();
  }

  Future<void> _nombre({CategoriaPanel? categoria}) async {
    final nombre = await pedirTexto(
      context,
      titulo: categoria == null ? 'Nueva categoría' : 'Renombrar categoría',
      etiqueta: 'Nombre',
      confirmar: 'Guardar',
      maximo: 60,
    );
    if (nombre == null) return;
    await _ejecutar(
      () => categoria == null
          ? _api.post('/api/categorias', {'nombre': nombre})
          : _api.put('/api/categorias/${categoria.id}', {'nombre': nombre}),
    );
  }

  /// HU18: si tiene productos, el servidor dice cuántos afecta y se pide confirmación.
  Future<void> _desactivar(CategoriaPanel c) async {
    try {
      await _api.post('/api/categorias/${c.id}/desactivar');
    } on ErrorApi catch (e) {
      if (e.codigo != 'CONFIRMACION_REQUERIDA' || !mounted) {
        if (mounted) avisar(context, e.mensaje);
        return;
      }
      final ok = await confirmar(context, titulo: 'Desactivar «${c.nombre}»', mensaje: e.mensaje, accion: 'Desactivar');
      if (ok) await _ejecutar(() => _api.post('/api/categorias/${c.id}/desactivar?confirmar=true'));
    }
    _cargar();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      floatingActionButton: widget.gestiona
          ? FloatingActionButton.extended(
              onPressed: () => _nombre(),
              icon: const Icon(Icons.add),
              label: const Text('Nueva categoría'),
            )
          : null,
      body: Cargador(
        futuro: _categorias,
        reintentar: _cargar,
        constructor: (context, categorias) => ListView.separated(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 88),
          itemCount: categorias.length,
          separatorBuilder: (_, _) => const Divider(height: 1),
          itemBuilder: (_, i) {
            final c = categorias[i];
            return ListTile(
              title: Text('${c.nombre}${c.activa ? '' : ' (desactivada)'}'),
              subtitle: Text('${c.productosActivos} productos activos'),
              trailing: widget.gestiona
                  ? Wrap(
                      children: [
                        IconButton(
                          tooltip: 'Renombrar',
                          onPressed: () => _nombre(categoria: c),
                          icon: const Icon(Icons.edit_outlined),
                        ),
                        c.activa
                            ? TextButton(onPressed: () => _desactivar(c), child: const Text('Desactivar'))
                            : TextButton(
                                onPressed: () => _ejecutar(() => _api.post('/api/categorias/${c.id}/activar')),
                                child: const Text('Activar'),
                              ),
                      ],
                    )
                  : null,
            );
          },
        ),
      ),
    );
  }
}
