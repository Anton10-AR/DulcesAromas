import 'package:flutter/material.dart';

import 'api.dart';
import 'tema.dart';

/// Lo que se ve mientras la app recupera la sesión guardada, antes de mostrar cualquier pantalla. No usa
/// MaterialApp: su navegador reemplazaría la dirección del navegador (/pedidos, /carrito…) antes de que el router
/// la lea.
class Arrancando extends StatelessWidget {
  const Arrancando({super.key});

  @override
  Widget build(BuildContext context) {
    final oscuro = View.of(context).platformDispatcher.platformBrightness == Brightness.dark;
    return Directionality(
      textDirection: TextDirection.ltr,
      child: ColoredBox(
        color: oscuro ? Colores.fondoOscuro : Colores.fondo,
        child: Center(child: CircularProgressIndicator(color: oscuro ? Colores.textoOscuro : Colores.cafe)),
      ),
    );
  }
}

/// Fila de una lista con acciones a la derecha. En pantallas angostas las acciones bajan debajo del texto, para que
/// el nombre no quede partido letra por letra (RNF12).
class FilaConAcciones extends StatelessWidget {
  const FilaConAcciones({super.key, this.inicio, required this.titulo, this.subtitulo, required this.acciones});

  final Widget? inicio;
  final Widget titulo;
  final Widget? subtitulo;
  final List<Widget> acciones;

  /// Desde este ancho las acciones caben a la derecha del texto.
  static const anchoEnUnaFila = 560.0;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, limites) {
        if (acciones.isEmpty || limites.maxWidth >= anchoEnUnaFila) {
          return ListTile(
            leading: inicio,
            title: titulo,
            subtitle: subtitulo,
            trailing: acciones.isEmpty ? null : Wrap(crossAxisAlignment: WrapCrossAlignment.center, children: acciones),
          );
        }
        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            ListTile(leading: inicio, title: titulo, subtitle: subtitulo),
            Padding(
              padding: const EdgeInsets.fromLTRB(8, 0, 8, 8),
              child: Wrap(
                alignment: WrapAlignment.end,
                crossAxisAlignment: WrapCrossAlignment.center,
                spacing: 4,
                children: acciones,
              ),
            ),
          ],
        );
      },
    );
  }
}

/// Ancho máximo del contenido, para que en pantallas anchas no se estire (RNF12).
const double anchoMaximo = 720;

/// Centra el contenido y limita su ancho; entre 360 y 1920 px no hay scroll horizontal (RNF12).
class Contenido extends StatelessWidget {
  const Contenido({super.key, required this.child, this.ancho = anchoMaximo});

  final Widget child;
  final double ancho;

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.topCenter,
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: ancho),
        child: child,
      ),
    );
  }
}

/// Campo de texto que muestra el error que mandó el servidor para ese campo (RNF01).
class Campo extends StatelessWidget {
  const Campo({
    super.key,
    required this.controlador,
    required this.etiqueta,
    required this.nombre,
    this.errores,
    this.oculto = false,
    this.teclado,
    this.ayuda,
    this.lineas = 1,
    this.maximo,
    this.autocompletar,
  });

  final TextEditingController controlador;
  final String etiqueta;

  /// Nombre del campo en la API, para buscar su error.
  final String nombre;
  final Map<String, String>? errores;
  final bool oculto;
  final TextInputType? teclado;
  final String? ayuda;
  final int lineas;
  final int? maximo;
  final Iterable<String>? autocompletar;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: TextField(
        controller: controlador,
        obscureText: oculto,
        keyboardType: teclado,
        maxLines: oculto ? 1 : lineas,
        maxLength: maximo,
        autofillHints: autocompletar,
        decoration: InputDecoration(
          labelText: etiqueta,
          helperText: ayuda,
          errorText: errores?[nombre],
          errorMaxLines: 3,
        ),
      ),
    );
  }
}

/// Mensaje de error en un recuadro, para errores que no son de un campo.
class AvisoError extends StatelessWidget {
  const AvisoError(this.mensaje, {super.key});

  final String mensaje;

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;
    return Semantics(
      liveRegion: true,
      child: Container(
        width: double.infinity,
        margin: const EdgeInsets.only(bottom: 16),
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          border: Border.all(color: colores.error),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          children: [
            Icon(Icons.error_outline, color: colores.error),
            const SizedBox(width: 8),
            Expanded(
              child: Text(mensaje, style: TextStyle(color: colores.error)),
            ),
          ],
        ),
      ),
    );
  }
}

/// Muestra un futuro: cargando, error con botón para reintentar, o el contenido.
class Cargador<T> extends StatelessWidget {
  const Cargador({super.key, required this.futuro, required this.constructor, required this.reintentar});

  final Future<T> futuro;
  final Widget Function(BuildContext, T) constructor;
  final VoidCallback reintentar;

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<T>(
      future: futuro,
      builder: (context, estado) {
        if (estado.hasError) {
          final error = estado.error;
          return Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  AvisoError(error is ErrorApi ? error.mensaje : 'Ocurrió un error inesperado.'),
                  OutlinedButton(onPressed: reintentar, child: const Text('Reintentar')),
                ],
              ),
            ),
          );
        }
        if (!estado.hasData) {
          return const Center(child: CircularProgressIndicator());
        }
        return constructor(context, estado.data as T);
      },
    );
  }
}

/// Etiqueta de estado de un pedido o un pago.
class Etiqueta extends StatelessWidget {
  const Etiqueta(this.texto, {super.key, this.resaltada = false});

  final String texto;
  final bool resaltada;

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: resaltada ? colores.secondaryContainer : null,
        border: Border.all(color: colores.outline),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Text(texto, style: Theme.of(context).textTheme.labelLarge?.copyWith(color: colores.onSurface)),
    );
  }
}

void avisar(BuildContext context, String mensaje) {
  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(mensaje)));
}
