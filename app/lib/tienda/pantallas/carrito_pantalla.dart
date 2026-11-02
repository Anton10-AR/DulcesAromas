import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/widgets.dart';
import '../carrito.dart';

/// Carrito con el total calculado por el servidor (CU05, RN08, RN11).
class CarritoPantalla extends StatefulWidget {
  const CarritoPantalla({super.key});

  @override
  State<CarritoPantalla> createState() => _CarritoPantallaState();
}

class _CarritoPantallaState extends State<CarritoPantalla> {
  Future<Cotizacion>? _cotizacion;

  @override
  void initState() {
    super.initState();
    _cotizar();
  }

  void _cotizar() {
    final carrito = context.read<Carrito>();
    setState(() {
      _cotizacion = carrito.vacio
          ? null
          : context
                .read<Api>()
                .post('/api/pedidos/cotizacion', {'lineas': carrito.aSolicitud()})
                .then((j) => Cotizacion.deJson(j as Json));
    });
  }

  void _cambiar(int indice, int cantidad) {
    context.read<Carrito>().cambiarCantidad(indice, cantidad);
    _cotizar();
  }

  @override
  Widget build(BuildContext context) {
    final carrito = context.watch<Carrito>();
    return Scaffold(
      appBar: AppBar(
        title: const Text('Tu carrito'),
        actions: [
          // Si un producto del carrito se eliminó del catálogo, la cotización falla; así se puede empezar de nuevo.
          if (!carrito.vacio)
            TextButton(
              onPressed: () {
                context.read<Carrito>().vaciar();
                _cotizar();
              },
              child: const Text('Vaciar'),
            ),
        ],
      ),
      body: carrito.vacio
          ? Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Text('Tu carrito está vacío.'),
                  const SizedBox(height: 8),
                  FilledButton(onPressed: () => context.go('/'), child: const Text('Ver el catálogo')),
                ],
              ),
            )
          : Cargador(
              futuro: _cotizacion!,
              reintentar: _cotizar,
              constructor: (context, cotizacion) => _contenido(carrito, cotizacion),
            ),
    );
  }

  Widget _contenido(Carrito carrito, Cotizacion cotizacion) {
    final textos = Theme.of(context).textTheme;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Contenido(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // CU05, alt. 1: un producto dejó de estar disponible; hay que quitarlo para confirmar (RN08).
              if (!cotizacion.confirmable)
                AvisoError('Ya no están disponibles: ${cotizacion.noDisponibles.join(', ')}. Quítalos para continuar.'),
              for (var i = 0; i < carrito.lineas.length && i < cotizacion.lineas.length; i++)
                _linea(i, carrito.lineas[i], cotizacion.lineas[i]),
              const Divider(height: 32),
              Row(
                children: [
                  Text('Total sin envío', style: textos.titleMedium),
                  const Spacer(),
                  Text(bolivianos(cotizacion.total), style: textos.titleLarge),
                ],
              ),
              Text('El envío se suma si eliges delivery.', style: textos.bodySmall),
              const SizedBox(height: 24),
              FilledButton(
                onPressed: cotizacion.confirmable ? () => context.push('/confirmar') : null,
                child: const Text('Continuar'),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _linea(int indice, LineaCarrito linea, LineaVista vista) {
    final textos = Theme.of(context).textTheme;
    final detalles = [...linea.nombresOpciones, if (linea.mensaje != null) 'Mensaje: «${linea.mensaje}»'];
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(child: Text(vista.nombre, style: textos.titleMedium)),
                Text(bolivianos(vista.subtotal), style: textos.titleMedium),
              ],
            ),
            if (detalles.isNotEmpty) Text(detalles.join(' · '), style: textos.bodySmall),
            if (!vista.disponible) Text('No disponible', style: TextStyle(color: Theme.of(context).colorScheme.error)),
            Row(
              children: [
                IconButton(
                  tooltip: 'Quitar uno',
                  onPressed: () => _cambiar(indice, linea.cantidad - 1),
                  icon: const Icon(Icons.remove),
                ),
                Text('${linea.cantidad}', style: textos.titleMedium),
                IconButton(
                  tooltip: 'Agregar uno',
                  onPressed: linea.cantidad < Carrito.cantidadMaxima
                      ? () => _cambiar(indice, linea.cantidad + 1)
                      : null,
                  icon: const Icon(Icons.add),
                ),
                const Spacer(),
                TextButton.icon(
                  onPressed: () {
                    context.read<Carrito>().quitar(indice);
                    _cotizar();
                  },
                  icon: const Icon(Icons.delete_outline),
                  label: const Text('Quitar'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
