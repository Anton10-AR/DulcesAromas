import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/sesion.dart';
import '../../comun/tema.dart';
import '../../comun/widgets.dart';
import 'comunes.dart';
import '../recuerdo_portada.dart';
import 'portada.dart';

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

  /// Las categorías en el orden de los chips, para ordenar las secciones del catálogo.
  List<Categoria> _ordenCategorias = const [];
  late Future<List<Producto>> _productos;
  final _desplazamiento = ScrollController();
  bool _portadaPasada = false;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    _categorias = _api.get('/api/catalogo/categorias').then((j) {
      // En el orden en que el negocio las creó: primero las tortas, que son lo principal, y no por abecedario.
      final categorias = lista(j, Categoria.deJson)..sort((a, b) => a.id.compareTo(b.id));
      if (mounted) setState(() => _ordenCategorias = categorias);
      return categorias;
    });
    _buscar();
  }

  @override
  void dispose() {
    _espera?.cancel();
    _busqueda.dispose();
    _desplazamiento.dispose();
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

  /// La portada completa se ve en la primera visita. Después, o con la sesión iniciada, va una portada baja: quien ya
  /// conoce la tienda ve los productos sin deslizar.
  bool _portadaBaja(BuildContext context) =>
      context.read<RecuerdoPortada>().vistaAntes || context.read<Sesion>().iniciada;

  /// Alto de la portada: la completa ocupa la mitad de la pantalla, entre 320 y 460 px; la baja, una franja.
  double _altoPortada(BuildContext context) {
    final tamano = MediaQuery.sizeOf(context);
    if (_portadaBaja(context)) return tamano.width >= Portada.anchoMosaico ? 260 : 190;
    return (tamano.height * 0.5).clamp(320.0, 460.0);
  }

  /// El nombre vuelve a la barra cuando la portada, que lo muestra, sale de la pantalla.
  bool _alDesplazar(ScrollNotification aviso) {
    if (aviso.depth != 0 || aviso.metrics.axis != Axis.vertical) return false;
    final pasada = aviso.metrics.pixels > _altoPortada(context) - kToolbarHeight;
    if (pasada != _portadaPasada) setState(() => _portadaPasada = pasada);
    return false;
  }

  void _verCatalogo() {
    final destino = _altoPortada(context);
    if (MediaQuery.disableAnimationsOf(context)) {
      _desplazamiento.jumpTo(destino);
    } else {
      _desplazamiento.animateTo(destino, duration: const Duration(milliseconds: 450), curve: Curves.easeOutCubic);
    }
  }

  @override
  Widget build(BuildContext context) {
    // Al iniciar o cerrar sesión cambia la portada: el catálogo se vuelve a dibujar.
    context.watch<Sesion>();
    return Scaffold(
      appBar: AppBar(title: _portadaPasada ? const Text('Dulces Aromas') : null, actions: accionesTienda(context)),
      body: NotificationListener<ScrollNotification>(
        onNotification: _alDesplazar,
        // Deslizar hacia abajo vuelve a pedir el catálogo: precios, disponibilidad e imágenes nuevas.
        child: RefreshIndicator(
          onRefresh: _actualizar,
          // Las secciones van sueltas en la lista, no en un SliverMainAxisGroup: dentro de un grupo, las tarjetas de
          // abajo no se construían aunque estuvieran en pantalla.
          child: LayoutBuilder(
            builder: (context, limites) => FutureBuilder<List<Producto>>(
              future: _productos,
              builder: (context, estado) => CustomScrollView(
                controller: _desplazamiento,
                physics: const AlwaysScrollableScrollPhysics(),
                slivers: [
                  SliverToBoxAdapter(
                    child: Portada(
                      alto: _altoPortada(context),
                      baja: _portadaBaja(context),
                      alVerCatalogo: _verCatalogo,
                    ),
                  ),
                  SliverToBoxAdapter(
                    child: Contenido(
                      ancho: 1100,
                      child: Column(
                        children: [
                          Padding(
                            padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
                            child: TextField(
                              controller: _busqueda,
                              onChanged: _alEscribir,
                              textInputAction: TextInputAction.search,
                              decoration: InputDecoration(
                                prefixIcon: const Icon(Icons.search),
                                labelText: 'Buscar por nombre',
                                filled: true,
                                fillColor: Theme.of(context).cardTheme.color,
                                border: OutlineInputBorder(borderRadius: BorderRadius.circular(28)),
                                enabledBorder: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(28),
                                  borderSide: BorderSide(color: Theme.of(context).colorScheme.outlineVariant),
                                ),
                                focusedBorder: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(28),
                                  borderSide: BorderSide(color: Theme.of(context).colorScheme.primary, width: 2),
                                ),
                              ),
                            ),
                          ),
                          FutureBuilder(
                            future: _categorias,
                            // Si no entran en una fila, bajan a la siguiente: ninguna queda cortada.
                            builder: (context, estado) => Padding(
                              padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                              child: Align(
                                alignment: Alignment.centerLeft,
                                child: Wrap(
                                  runSpacing: 8,
                                  children: [
                                    _chip('Todo', null),
                                    for (final c in estado.data ?? const <Categoria>[]) _chip(c.nombre, c.id),
                                  ],
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  if (estado.hasError || !estado.hasData || estado.data!.isEmpty)
                    SliverToBoxAdapter(child: _sinProductos(estado))
                  else
                    ..._catalogo(estado.data!, limites.maxWidth),
                  // Con pocos productos, un espacio al final deja subir la portada del todo con «Ver catálogo».
                  SliverLayoutBuilder(
                    builder: (context, limites) {
                      final falta =
                          _altoPortada(context) + limites.viewportMainAxisExtent - limites.precedingScrollExtent;
                      return SliverToBoxAdapter(child: SizedBox(height: falta > 0 ? falta : 0));
                    },
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  /// Cargando, el error con su botón para reintentar, o la búsqueda sin resultados.
  Widget _sinProductos(AsyncSnapshot<List<Producto>> estado) {
    final Widget hijo;
    if (estado.hasError) {
      final error = estado.error;
      hijo = Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          AvisoError(error is ErrorApi ? error.mensaje : 'Ocurrió un error inesperado.'),
          OutlinedButton(onPressed: _buscar, child: const Text('Reintentar')),
        ],
      );
    } else if (!estado.hasData) {
      hijo = const CircularProgressIndicator();
    } else {
      // CU04, alt. 1: sin resultados, se sugiere quitar los filtros.
      hijo = Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Text('No encontramos productos con esa búsqueda.'),
          const SizedBox(height: 8),
          OutlinedButton(onPressed: _quitarFiltros, child: const Text('Quitar filtros')),
        ],
      );
    }
    return Padding(
      padding: const EdgeInsets.all(32),
      child: Center(child: hijo),
    );
  }

  /// Sin búsqueda ni categoría elegida, el catálogo va por secciones, una por categoría; si no, una sola rejilla.
  List<Widget> _catalogo(List<Producto> productos, double ancho) {
    // Columnas de hasta 260 px dentro de un ancho máximo de 1100. Cada tarjeta mide su imagen cuadrada más el
    // espacio del texto, así todas las imágenes tienen la misma altura y ninguna se recorta (RNF12).
    const espacio = 16.0;
    final lateral = ancho > 1100 ? (ancho - 1100) / 2 + espacio : espacio;
    final util = ancho - 2 * lateral;
    // Al arrancar en Android, el primer cuadro puede llegar con ancho 0 y la cuadrícula falla.
    if (util <= 32) return const [];
    final columnas = ((util + espacio) / (260 + espacio)).ceil().clamp(1, 12);
    final anchoTarjeta = (util - espacio * (columnas - 1)) / columnas;
    final alto = anchoTarjeta + MediaQuery.textScalerOf(context).scale(_TarjetaProducto.altoTexto);
    Widget rejilla(List<Producto> lista) => SliverPadding(
      padding: EdgeInsets.fromLTRB(lateral, 8, lateral, espacio),
      sliver: SliverGrid.builder(
        gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: columnas,
          mainAxisSpacing: espacio,
          crossAxisSpacing: espacio,
          mainAxisExtent: alto,
        ),
        itemCount: lista.length,
        itemBuilder: (_, i) => _TarjetaProducto(lista[i], alVolver: _buscar),
      ),
    );

    if (_categoria != null || _busqueda.text.trim().isNotEmpty) return [rejilla(productos)];
    final grupos = <int, List<Producto>>{};
    for (final p in productos) {
      grupos.putIfAbsent(p.categoria.id, () => []).add(p);
    }
    int orden(int id) {
      final i = _ordenCategorias.indexWhere((c) => c.id == id);
      return i < 0 ? _ordenCategorias.length + id : i;
    }

    final ids = grupos.keys.toList()..sort((a, b) => orden(a).compareTo(orden(b)));
    return [
      for (final id in ids) ...[
        SliverToBoxAdapter(
          child: _EncabezadoSeccion(
            categoria: grupos[id]!.first.categoria,
            cantidad: grupos[id]!.length,
            alVerSolo: () {
              _categoria = id;
              _buscar();
            },
          ),
        ),
        rejilla(grupos[id]!),
      ],
    ];
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
      avatar: _categoria == id ? null : Icon(iconoCategoria(nombre), size: 18),
      label: Text(nombre),
      selected: _categoria == id,
      onSelected: (_) {
        _categoria = id;
        _buscar();
      },
    ),
  );
}

/// Ícono de una categoría, por su nombre. Las categorías las define el negocio: si no se reconoce, un pastel.
IconData iconoCategoria(String nombre) {
  final n = nombre.toLowerCase();
  if (n.contains('bebida') || n.contains('café')) return Icons.local_cafe_outlined;
  if (n.contains('galleta') || n.contains('masita')) return Icons.cookie_outlined;
  if (n.contains('pan')) return Icons.bakery_dining_outlined;
  if (n.contains('evento')) return Icons.celebration_outlined;
  if (n.contains('postre')) return Icons.icecream_outlined;
  if (n == 'todo') return Icons.apps;
  return Icons.cake_outlined;
}

/// El título de una sección del catálogo: la categoría, cuántos productos tiene y un atajo para ver solo esa.
class _EncabezadoSeccion extends StatelessWidget {
  const _EncabezadoSeccion({required this.categoria, required this.cantidad, required this.alVerSolo});

  final Categoria categoria;
  final int cantidad;
  final VoidCallback alVerSolo;

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    final colores = Theme.of(context).colorScheme;
    return Contenido(
      ancho: 1100,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 16, 8, 4),
        child: Row(
          children: [
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(color: colores.secondaryContainer, borderRadius: BorderRadius.circular(12)),
              child: Icon(iconoCategoria(categoria.nombre), color: colores.onSecondaryContainer, size: 22),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Semantics(header: true, child: Text(categoria.nombre, style: textos.headlineSmall)),
                  Text(cantidadProductos(cantidad), style: textos.bodySmall),
                ],
              ),
            ),
            TextButton(onPressed: alVerSolo, child: const Text('Ver solo esta')),
          ],
        ),
      ),
    );
  }
}

class _TarjetaProducto extends StatefulWidget {
  const _TarjetaProducto(this.producto, {required this.alVolver});

  final Producto producto;

  /// Alto bajo la imagen: el nombre en hasta dos líneas, la descripción en dos y el precio con su botón.
  static const altoTexto = 168.0;

  /// Al volver del detalle, el catálogo se actualiza: pudo cambiar la imagen o la disponibilidad.
  final VoidCallback alVolver;

  @override
  State<_TarjetaProducto> createState() => _TarjetaProductoState();
}

class _TarjetaProductoState extends State<_TarjetaProducto> {
  /// Con el ratón encima, la tarjeta sube un poco y la foto se acerca, salvo que el dispositivo pida menos movimiento.
  bool _encima = false;

  @override
  Widget build(BuildContext context) {
    final producto = widget.producto;
    final textos = Theme.of(context).textTheme;
    final colores = Theme.of(context).colorScheme;
    final quieto = MediaQuery.disableAnimationsOf(context);
    final elevada = _encima && !quieto;
    const duracion = Duration(milliseconds: 220);
    return AnimatedSlide(
      offset: elevada ? const Offset(0, -0.015) : Offset.zero,
      duration: duracion,
      curve: Curves.easeOut,
      child: Card(
        margin: EdgeInsets.zero,
        elevation: elevada ? 10 : null,
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onHover: (encima) => setState(() => _encima = encima),
          onTap: () async {
            await context.push('/producto/${producto.id}');
            widget.alVolver();
          },
          child: Padding(
            padding: const EdgeInsets.all(8),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // La foto, con su propio borde redondeado dentro de la tarjeta.
                AspectRatio(
                  aspectRatio: 1,
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(12),
                    child: Stack(
                      fit: StackFit.expand,
                      children: [
                        AnimatedScale(
                          scale: elevada ? 1.06 : 1,
                          duration: duracion,
                          curve: Curves.easeOut,
                          child: Opacity(
                            opacity: producto.disponible ? 1 : 0.45,
                            child: ImagenProducto(ruta: producto.imagen, descripcion: producto.nombre),
                          ),
                        ),
                        // Un velo abajo de la foto, para que se funda con la tarjeta.
                        const Positioned(
                          left: 0,
                          right: 0,
                          bottom: 0,
                          height: 48,
                          child: IgnorePointer(
                            child: DecoratedBox(
                              decoration: BoxDecoration(
                                gradient: LinearGradient(
                                  begin: Alignment.topCenter,
                                  end: Alignment.bottomCenter,
                                  colors: [Color(0x00000000), Color(0x33000000)],
                                ),
                              ),
                            ),
                          ),
                        ),
                        // HU07: se nota de lejos que no se puede pedir.
                        if (!producto.disponible)
                          const Positioned(left: 8, top: 8, child: _NoDisponible())
                        else if (producto.personalizable)
                          const Positioned(left: 8, top: 8, child: _Personalizable()),
                      ],
                    ),
                  ),
                ),
                // Si el texto no cabe (letra grande en el dispositivo), se acorta con «…» en vez de desbordar.
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(6, 12, 2, 2),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(producto.nombre, style: textos.titleMedium, maxLines: 2, overflow: TextOverflow.ellipsis),
                        const SizedBox(height: 4),
                        Flexible(
                          child: Text(
                            producto.descripcion,
                            style: textos.bodySmall,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        const Spacer(),
                        Row(
                          children: [
                            Expanded(
                              child: Text(
                                bolivianos(producto.precio),
                                style: textos.titleLarge?.copyWith(color: colores.secondary),
                                maxLines: 1,
                              ),
                            ),
                            // Decoración: toda la tarjeta abre el producto.
                            ExcludeSemantics(
                              child: AnimatedContainer(
                                duration: duracion,
                                width: 38,
                                height: 38,
                                decoration: BoxDecoration(
                                  color: elevada ? colores.primary : colores.secondaryContainer,
                                  shape: BoxShape.circle,
                                ),
                                child: Icon(
                                  Icons.arrow_forward,
                                  size: 20,
                                  color: elevada ? colores.onPrimary : colores.onSecondaryContainer,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Se puede elegir tamaño, relleno o mensaje.
class _Personalizable extends StatelessWidget {
  const _Personalizable();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(color: Colores.cafe, borderRadius: BorderRadius.circular(16)),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.auto_awesome, size: 14, color: Colores.rosaClaro),
          const SizedBox(width: 4),
          Text('Personalizable', style: Theme.of(context).textTheme.labelLarge?.copyWith(color: Colors.white)),
        ],
      ),
    );
  }
}

class _NoDisponible extends StatelessWidget {
  const _NoDisponible();

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: colores.surface,
        border: Border.all(color: colores.error),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Text('No disponible', style: Theme.of(context).textTheme.labelLarge?.copyWith(color: colores.error)),
    );
  }
}
