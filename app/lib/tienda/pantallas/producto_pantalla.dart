import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/widgets.dart';
import '../carrito.dart';
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
    setState(() {
      _producto = context
          .read<Api>()
          .get('/api/catalogo/productos/${widget.id}')
          .then((j) => Producto.deJson(j as Json));
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
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('${p.nombre} se agregó al carrito.'),
        // Con un botón, Flutter deja el aviso fijo hasta tocarlo; aquí se va solo, como los demás.
        persist: false,
        duration: const Duration(seconds: 5),
        action: SnackBarAction(label: 'Ver carrito', onPressed: () => context.push('/carrito')),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(actions: accionesTienda(context)),
      body: Cargador(
        futuro: _producto,
        reintentar: _cargar,
        constructor: (context, p) {
          final textos = Theme.of(context).textTheme;
          final imagen = ClipRRect(
            borderRadius: BorderRadius.circular(12),
            child: AspectRatio(
              aspectRatio: 1,
              child: ImagenProducto(ruta: p.imagen, descripcion: p.nombre),
            ),
          );
          // En pantallas anchas, la imagen a la izquierda y las opciones a la derecha: se elige sin bajar (RNF12).
          final ancha = MediaQuery.sizeOf(context).width >= 720;
          final detalles = Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(p.nombre, style: textos.headlineMedium),
              const SizedBox(height: 4),
              Text(bolivianos(p.precio), style: textos.titleLarge),
              const SizedBox(height: 12),
              Text(p.descripcion, style: textos.bodyLarge),
              const SizedBox(height: 16),
              for (final grupo in p.opcionesPorTipo.entries) _grupoOpciones(grupo.key, grupo.value),
              if (p.personalizable)
                Campo(
                  controlador: _mensaje,
                  etiqueta: 'Mensaje sobre la torta (opcional)',
                  nombre: 'mensaje',
                  ayuda: 'Hasta 24 caracteres, sin costo adicional.',
                  maximo: 24,
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
              // HU07: un producto no disponible no se puede agregar al carrito.
              if (!p.disponible) const AvisoError('Este producto no está disponible por ahora.'),
              if (p.disponible && p.faltaTamano(_elegidas))
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: Text('Elige un tamaño para agregarlo al carrito.', style: textos.bodyMedium),
                ),
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: p.disponible && !p.faltaTamano(_elegidas) ? () => _agregar(p) : null,
                  icon: const Icon(Icons.add_shopping_cart),
                  label: const Text('Agregar al carrito'),
                ),
              ),
            ],
          );
          return SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: ancha
                ? Contenido(
                    ancho: 1100,
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(flex: 5, child: imagen),
                        const SizedBox(width: 32),
                        Expanded(flex: 6, child: detalles),
                      ],
                    ),
                  )
                : Contenido(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [imagen, const SizedBox(height: 16), detalles],
                    ),
                  ),
          );
        },
      ),
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
