import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/marca.dart';
import '../../comun/preferencia_tema.dart';
import '../../comun/formato.dart';
import '../../comun/sesion.dart';
import '../carrito.dart';
import '../marco_tienda.dart';

/// Acciones de la barra superior de la Tienda: el tema y, en pantallas anchas, el carrito con su cantidad y la
/// cuenta. En el teléfono, el carrito y la cuenta están en la barra inferior (`MarcoTienda`).
List<Widget> accionesTienda(BuildContext context) {
  final carrito = context.watch<Carrito>();
  final sesion = context.watch<Sesion>();
  if (conBarraInferior(context)) return const [BotonTema()];
  return [
    const BotonTema(),
    IconButton(
      tooltip: 'Carrito, ${cantidadProductos(carrito.unidades)}',
      onPressed: () => context.push('/carrito'),
      icon: Badge(
        isLabelVisible: carrito.unidades > 0,
        label: Text('${carrito.unidades}'),
        child: const Icon(Icons.shopping_bag_outlined),
      ),
    ),
    if (sesion.iniciada)
      PopupMenuButton<String>(
        tooltip: 'Mi cuenta',
        icon: CirculoIniciales(sesion.usuario!.nombre),
        onSelected: (ruta) => context.push(ruta),
        itemBuilder: (_) => const [
          PopupMenuItem(value: '/pedidos', child: Text('Mis pedidos')),
          PopupMenuItem(value: '/perfil', child: Text('Mi perfil')),
        ],
      )
    else
      TextButton(onPressed: () => context.push('/ingresar'), child: const Text('Ingresar')),
  ];
}

/// Imagen de un producto, o un marcador si no tiene.
class ImagenProducto extends StatelessWidget {
  const ImagenProducto({super.key, required this.ruta, required this.descripcion});

  final String? ruta;
  final String descripcion;

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;
    final marcador = Container(
      color: colores.secondaryContainer,
      child: Center(child: Icon(Icons.cake_outlined, size: 48, color: colores.onSecondaryContainer)),
    );
    if (ruta == null) return marcador;
    return Image.network(
      context.read<Api>().absoluta(ruta!),
      fit: BoxFit.cover,
      semanticLabel: descripcion,
      errorBuilder: (_, _, _) => marcador,
    );
  }
}
