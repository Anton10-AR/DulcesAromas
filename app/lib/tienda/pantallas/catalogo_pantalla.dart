import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/widgets.dart';
import 'comunes.dart';

/// Catálogo de la Tienda (CU04, HU07). Se ve sin iniciar sesión (RN15).
class CatalogoPantalla extends StatefulWidget {
  const CatalogoPantalla({super.key});

  @override
  State<CatalogoPantalla> createState() => _CatalogoPantallaState();
}

class _CatalogoPantallaState extends State<CatalogoPantalla> {
  final _busqueda = TextEditingController();
  Timer? _espera;
  int? _categoria;
  late Future<List<Categoria>> _categorias;
  late Future<List<Producto>> _productos;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    _categorias = _api.get('/api/catalogo/categorias').then((j) => lista(j, Categoria.deJson));
    _buscar();
  }

  @override
  void dispose() {
    _espera?.cancel();
    _busqueda.dispose();
    super.dispose();
  }

  void _buscar() {
    setState(() {
      _productos = _api
          .get('/api/catalogo/productos', {'buscar': _busqueda.text.trim(), 'categoria': _categoria?.toString()})
          .then((j) => lista(j, Producto.deJson));
    });
  }

  /// Busca mientras se escribe, sin una consulta por cada tecla.
  void _alEscribir(String _) {
    _espera?.cancel();
    _espera = Timer(const Duration(milliseconds: 350), _buscar);
  }

  void _quitarFiltros() {
    _busqueda.clear();
    _categoria = null;
    _buscar();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Dulces Aromas'), actions: accionesTienda(context)),
      body: Contenido(
        ancho: 1100,
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
              child: TextField(
                controller: _busqueda,
                onChanged: _alEscribir,
                textInputAction: TextInputAction.search,
                decoration: const InputDecoration(prefixIcon: Icon(Icons.search), labelText: 'Buscar por nombre'),
              ),
            ),
            FutureBuilder(
              future: _categorias,
              builder: (context, estado) => SizedBox(
                height: 56,
                child: ListView(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  children: [
                    _chip('Todo', null),
                    for (final c in estado.data ?? const <Categoria>[]) _chip(c.nombre, c.id),
                  ],
                ),
              ),
            ),
            Expanded(
              child: Cargador(
                futuro: _productos,
                reintentar: _buscar,
                constructor: (context, productos) {
                  if (productos.isEmpty) {
                    // CU04, alt. 1: sin resultados, se sugiere quitar los filtros.
                    return Center(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Text('No encontramos productos con esa búsqueda.'),
                          const SizedBox(height: 8),
                          OutlinedButton(onPressed: _quitarFiltros, child: const Text('Quitar filtros')),
                        ],
                      ),
                    );
                  }
                  return LayoutBuilder(
                    builder: (context, limites) {
                      // Al arrancar en Android, el primer cuadro puede llegar con ancho 0 y la cuadrícula falla.
                      if (limites.maxWidth <= 32) return const SizedBox.shrink();
                      // Deslizar hacia abajo vuelve a pedir el catálogo: precios, disponibilidad e imágenes nuevas.
                      return RefreshIndicator(
                        onRefresh: _actualizar,
                        child: GridView.builder(
                          physics: const AlwaysScrollableScrollPhysics(),
                          padding: const EdgeInsets.all(16),
                          gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                            maxCrossAxisExtent: 260,
                            mainAxisSpacing: 16,
                            crossAxisSpacing: 16,
                            childAspectRatio: 0.7,
                          ),
                          itemCount: productos.length,
                          itemBuilder: (_, i) => _TarjetaProducto(productos[i], alVolver: _buscar),
                        ),
                      );
                    },
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _actualizar() async {
    _buscar();
    try {
      await _productos;
    } on Object {
      // El error ya lo muestra el Cargador, con su botón para reintentar.
    }
  }

  Widget _chip(String nombre, int? id) => Padding(
    padding: const EdgeInsets.only(right: 8),
    child: ChoiceChip(
      label: Text(nombre),
      selected: _categoria == id,
      onSelected: (_) {
        _categoria = id;
        _buscar();
      },
    ),
  );
}

class _TarjetaProducto extends StatelessWidget {
  const _TarjetaProducto(this.producto, {required this.alVolver});

  final Producto producto;

  /// Al volver del detalle, el catálogo se actualiza: pudo cambiar la imagen o la disponibilidad.
  final VoidCallback alVolver;

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () async {
          await context.push('/producto/${producto.id}');
          alVolver();
        },
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // La imagen cede espacio al texto, para que la tarjeta no se desborde en pantallas angostas (RNF12).
            Expanded(
              child: ImagenProducto(ruta: producto.imagen, descripcion: producto.nombre),
            ),
            Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(producto.nombre, style: textos.titleMedium, maxLines: 2, overflow: TextOverflow.ellipsis),
                  const SizedBox(height: 4),
                  Text(bolivianos(producto.precio), style: textos.bodyLarge),
                  if (!producto.disponible) ...[
                    const SizedBox(height: 4),
                    Text('No disponible', style: textos.bodySmall),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
