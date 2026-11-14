import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/widgets.dart';
import '../carrito.dart';
import 'catalogo_pantalla.dart' show iconoCategoria;
import 'comunes.dart';

/// Detalle de un producto con sus opciones de personalización (CU04, CU05).
class ProductoPantalla extends StatefulWidget {
  const ProductoPantalla({super.key, required this.id});

  final int id;

  @override
  State<ProductoPantalla> createState() => _ProductoPantallaState();
}

class _ProductoPantallaState extends State<ProductoPantalla> {
  late Future<Producto> _producto;

  /// Otros productos de la misma categoría, para «Más de …».
  late Future<List<Producto>> _parecidos;

  /// Opción elegida de cada tipo; como máximo una por tipo. Solo el tamaño es obligatorio.
  final Map<String, Opcion> _elegidas = {};
  final _mensaje = TextEditingController();
  int _cantidad = 1;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  @override
  void dispose() {
    _mensaje.dispose();
    super.dispose();
  }

  void _cargar() {
    final api = context.read<Api>();
    setState(() {
      _producto = api.get('/api/catalogo/productos/${widget.id}').then((j) => Producto.deJson(j as Json));
      _parecidos = _producto.then(
        (p) => api
            .get('/api/catalogo/productos', {'categoria': '${p.categoria.id}'})
            .then(
              (j) => lista(j, Producto.deJson).where((o) => o.id != p.id && o.categoria.id == p.categoria.id).toList(),
            ),
      );
    });
  }

  void _agregar(Producto p) {
    final opciones = p.opciones.where((o) => _elegidas[o.tipo] == o).toList();
    context.read<Carrito>().agregar(
      LineaCarrito(
        productoId: p.id,
        nombre: p.nombre,
        cantidad: _cantidad,
        opciones: opciones.map((o) => o.id!).toList(),
        nombresOpciones: opciones.map((o) => o.nombre).toList(),
        mensaje: _mensaje.text.trim().isEmpty ? null : _mensaje.text.trim(),
      ),
    );
    final detalle = [
      '$_cantidad × ${p.nombre}',
      if (opciones.isNotEmpty) '(${opciones.map((o) => o.nombre).join(', ')})',
    ].join(' ');
    // Qué sigue: ir a pagar o volver al catálogo, sin buscar la flecha de arriba. Al cerrarla, sigue en el producto.
    showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (hoja) => _Agregado(
        detalle: detalle,
        alVerCarrito: () {
          Navigator.pop(hoja);
          context.go('/carrito');
        },
        alSeguir: () {
          Navigator.pop(hoja);
          context.go('/');
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(actions: accionesTienda(context)),
      bottomNavigationBar: _barraAgregar(),
      body: Cargador(
        futuro: _producto,
        reintentar: _cargar,
        constructor: (context, p) {
          final textos = Theme.of(context).textTheme;
          // En pantallas anchas, la imagen a la izquierda y las opciones a la derecha: se elige sin bajar (RNF12).
          final ancha = _ancha(context);
          final pantalla = MediaQuery.sizeOf(context);
          final imagen = ClipRRect(
            borderRadius: BorderRadius.circular(12),
            child: ancha
                ? AspectRatio(
                    aspectRatio: 1,
                    child: ImagenProducto(ruta: p.imagen, descripcion: p.nombre),
                  )
                // En el teléfono, una foto más baja deja ver el nombre, el precio y las opciones sin deslizar.
                : SizedBox(
                    width: double.infinity,
                    height: (pantalla.height * 0.32).clamp(180.0, pantalla.width - 32),
                    child: ImagenProducto(ruta: p.imagen, descripcion: p.nombre),
                  ),
          );
          final colores = Theme.of(context).colorScheme;
          final personalizar = [
            for (final grupo in p.opcionesPorTipo.entries) _grupoOpciones(grupo.key, grupo.value),
            if (p.personalizable)
              Campo(
                controlador: _mensaje,
                etiqueta: 'Mensaje sobre la torta (opcional)',
                nombre: 'mensaje',
                ayuda: 'Hasta 24 caracteres, sin costo adicional.',
                maximo: 24,
              ),
          ];
          final detalles = Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(iconoCategoria(p.categoria.nombre), size: 18, color: colores.secondary),
                  const SizedBox(width: 6),
                  Text(
                    p.categoria.nombre.toUpperCase(),
                    style: textos.labelLarge?.copyWith(color: colores.secondary, letterSpacing: 1.1),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(p.nombre, style: textos.headlineMedium),
              const SizedBox(height: 4),
              Text(bolivianos(p.precio), style: textos.headlineSmall?.copyWith(color: colores.secondary)),
              const SizedBox(height: 12),
              Text(p.descripcion, style: textos.bodyLarge),
              const SizedBox(height: 20),
              if (personalizar.isNotEmpty)
                Card(
                  margin: const EdgeInsets.only(bottom: 20),
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 4),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Icon(Icons.auto_awesome, size: 20, color: colores.secondary),
                            const SizedBox(width: 8),
                            Text('Personalízalo', style: textos.titleLarge),
                          ],
                        ),
                        const SizedBox(height: 12),
                        ...personalizar,
                      ],
                    ),
                  ),
                ),
              Row(
                children: [
                  Text('Cantidad', style: textos.titleMedium),
                  const SizedBox(width: 12),
                  IconButton.outlined(
                    tooltip: 'Quitar uno',
                    onPressed: _cantidad > 1 ? () => setState(() => _cantidad--) : null,
                    icon: const Icon(Icons.remove),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: Text('$_cantidad', style: textos.titleMedium),
                  ),
                  IconButton.outlined(
                    tooltip: 'Agregar uno',
                    onPressed: _cantidad < Carrito.cantidadMaxima ? () => setState(() => _cantidad++) : null,
                    icon: const Icon(Icons.add),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              // En el teléfono, el botón va fijo abajo (bottomNavigationBar).
              if (ancha) ...[_botonAgregar(p), const SizedBox(height: 24)],
              const _ComoRecibirlo(),
            ],
          );
          final parecidos = _Parecidos(futuro: _parecidos, categoria: p.categoria.nombre);
          return SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: ancha
                ? Contenido(
                    ancho: 1100,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Expanded(flex: 5, child: imagen),
                            const SizedBox(width: 32),
                            Expanded(flex: 6, child: detalles),
                          ],
                        ),
                        parecidos,
                      ],
                    ),
                  )
                : Contenido(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [imagen, const SizedBox(height: 16), detalles, parecidos],
                    ),
                  ),
          );
        },
      ),
    );
  }

  bool _ancha(BuildContext context) => MediaQuery.sizeOf(context).width >= 720;

  /// El aviso de no disponible o de falta el tamaño, y el botón para agregar.
  Widget _botonAgregar(Producto p) {
    final textos = Theme.of(context).textTheme;
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // HU07: un producto no disponible no se puede agregar al carrito.
        if (!p.disponible) const AvisoError('Este producto no está disponible por ahora.'),
        if (p.disponible && p.faltaTamano(_elegidas))
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Text('Elige un tamaño para agregarlo al carrito.', style: textos.bodyMedium),
          ),
        FilledButton.icon(
          onPressed: p.disponible && !p.faltaTamano(_elegidas) ? () => _agregar(p) : null,
          icon: const Icon(Icons.add_shopping_cart),
          label: const Text('Agregar al carrito'),
        ),
      ],
    );
  }

  /// En el teléfono, «Agregar al carrito» queda siempre a la vista, sobre la barra inferior.
  Widget _barraAgregar() {
    return FutureBuilder(
      future: _producto,
      builder: (context, estado) {
        if (_ancha(context) || !estado.hasData) return const SizedBox.shrink();
        final colores = Theme.of(context).colorScheme;
        return DecoratedBox(
          decoration: BoxDecoration(
            color: colores.surface,
            border: Border(top: BorderSide(color: colores.outlineVariant)),
          ),
          child: SafeArea(
            top: false,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
              // Centrado y con el ancho máximo del contenido, pero solo tan alto como el botón.
              child: Center(
                heightFactor: 1,
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: anchoMaximo),
                  child: _botonAgregar(estado.data!),
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _grupoOpciones(String tipo, List<Opcion> opciones) {
    final textos = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(esTamano(tipo) ? '$tipo (elige uno)' : '$tipo (opcional)', style: textos.titleMedium),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final o in opciones)
                ChoiceChip(
                  label: Text(o.recargo.sign == 0 ? o.nombre : '${o.nombre} (+${bolivianos(o.recargo)})'),
                  selected: _elegidas[tipo] == o,
                  onSelected: (marcada) => setState(() => marcada ? _elegidas[tipo] = o : _elegidas.remove(tipo)),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

/// La hoja que aparece al agregar un producto al carrito.
class _Agregado extends StatelessWidget {
  const _Agregado({required this.detalle, required this.alVerCarrito, required this.alSeguir});

  final String detalle;
  final VoidCallback alVerCarrito;
  final VoidCallback alSeguir;

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    final colores = Theme.of(context).colorScheme;
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Semantics(
              liveRegion: true,
              child: Row(
                children: [
                  Icon(Icons.check_circle_outline, color: colores.primary),
                  const SizedBox(width: 12),
                  Expanded(child: Text('Se agregó al carrito', style: textos.titleLarge)),
                ],
              ),
            ),
            const SizedBox(height: 8),
            Text(detalle, style: textos.bodyLarge),
            const SizedBox(height: 24),
            FilledButton.icon(
              onPressed: alVerCarrito,
              icon: const Icon(Icons.shopping_bag_outlined),
              label: const Text('Ver carrito'),
            ),
            const SizedBox(height: 8),
            OutlinedButton.icon(
              onPressed: alSeguir,
              icon: const Icon(Icons.storefront_outlined),
              label: const Text('Seguir comprando'),
            ),
          ],
        ),
      ),
    );
  }
}

/// Cómo se recibe y se paga un pedido, en tres líneas, para no tener que preguntarlo.
class _ComoRecibirlo extends StatelessWidget {
  const _ComoRecibirlo();

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    final colores = Theme.of(context).colorScheme;
    Widget linea(IconData icono, String titulo, String detalle) => Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icono, size: 22, color: colores.secondary),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(titulo, style: textos.titleSmall),
                Text(detalle, style: textos.bodySmall),
              ],
            ),
          ),
        ],
      ),
    );
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Divider(),
        const SizedBox(height: 8),
        linea(
          Icons.storefront_outlined,
          'Recojo o delivery',
          'Recógelo en la tienda o recíbelo en casa, con tarifa fija.',
        ),
        linea(Icons.qr_code_2, 'Pago por QR, transferencia o depósito', 'Subes el comprobante y lo verificamos.'),
        linea(Icons.timeline, 'Sigue tu pedido', 'En «Mis pedidos» ves en qué paso está.'),
      ],
    );
  }
}

/// Otros productos de la misma categoría, en una fila que se desliza.
class _Parecidos extends StatelessWidget {
  const _Parecidos({required this.futuro, required this.categoria});

  final Future<List<Producto>> futuro;
  final String categoria;

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    final colores = Theme.of(context).colorScheme;
    return FutureBuilder(
      future: futuro,
      builder: (context, estado) {
        final productos = estado.data ?? const <Producto>[];
        if (productos.isEmpty) return const SizedBox.shrink();
        return Padding(
          padding: const EdgeInsets.only(top: 32),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Semantics(header: true, child: Text('Más de $categoria', style: textos.headlineSmall)),
              const SizedBox(height: 12),
              SizedBox(
                height: 150 + MediaQuery.textScalerOf(context).scale(86),
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  itemCount: productos.length,
                  separatorBuilder: (_, _) => const SizedBox(width: 12),
                  itemBuilder: (context, i) {
                    final p = productos[i];
                    return SizedBox(
                      width: 160,
                      child: Card(
                        margin: EdgeInsets.zero,
                        clipBehavior: Clip.antiAlias,
                        child: InkWell(
                          onTap: () => context.push('/producto/${p.id}'),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              SizedBox(
                                height: 150,
                                child: ImagenProducto(ruta: p.imagen, descripcion: p.nombre),
                              ),
                              Padding(
                                padding: const EdgeInsets.fromLTRB(10, 8, 10, 0),
                                child: Text(
                                  p.nombre,
                                  style: textos.titleSmall,
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              const Spacer(),
                              Padding(
                                padding: const EdgeInsets.fromLTRB(10, 0, 10, 10),
                                child: Text(
                                  bolivianos(p.precio),
                                  style: textos.titleSmall?.copyWith(color: colores.secondary),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    );
                  },
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}
