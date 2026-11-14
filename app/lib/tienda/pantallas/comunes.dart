import 'dart:math';

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
///
/// Se decodifica al tamaño en que se muestra y no a su tamaño original (hasta 1200 × 1200): así ocupa unas diez veces menos
/// memoria y todo el catálogo cabe en el caché de imágenes de Flutter. Si no, al deslizar se descartaban las primeras y
/// había que volver a cargarlas (en Android, a descargarlas).
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
    final url = context.read<Api>().absoluta(ruta!);
    return LayoutBuilder(
      builder: (context, limites) {
        // El lado mayor en píxeles reales, redondeado hacia arriba a un múltiplo de 128: al cambiar un poco el tamaño
        // de la ventana se reutiliza la misma imagen del caché.
        final lado = limites.biggest.isFinite ? max(limites.maxWidth, limites.maxHeight) : 1000.0;
        final pixeles = (lado * MediaQuery.devicePixelRatioOf(context)).clamp(128.0, 1024.0);
        final ancho = ((pixeles / 128).ceil() * 128);
        return Image.network(
          url,
          fit: BoxFit.cover,
          // La API guarda las fotos cuadradas (AjusteImagen): con el ancho alcanza, el alto es el mismo.
          cacheWidth: ancho,
          semanticLabel: descripcion,
          // Aparece con un fundido corto la primera vez; desde el caché, de inmediato.
          frameBuilder: (_, hijo, cuadro, sincronica) => sincronica
              ? hijo
              : AnimatedOpacity(
                  opacity: cuadro == null ? 0 : 1,
                  duration: const Duration(milliseconds: 200),
                  child: hijo,
                ),
          errorBuilder: (_, _, _) => marcador,
        );
      },
    );
  }
}
