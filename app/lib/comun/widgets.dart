import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter/services.dart';

import 'api.dart';
import 'modelos.dart';
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

/// Dibuja un marco bien visible alrededor del control que tiene el foco cuando se navega con el teclado (RNF20).
/// Material 3 solo lo sombrea levemente. Con el mouse o el dedo no se dibuja nada.
class IndicadorDeFoco extends StatefulWidget {
  const IndicadorDeFoco({super.key, required this.child});

  final Widget child;

  static const llave = ValueKey('indicador-de-foco');

  @override
  State<IndicadorDeFoco> createState() => _IndicadorDeFocoState();
}

class _IndicadorDeFocoState extends State<IndicadorDeFoco> with SingleTickerProviderStateMixin {
  late final Ticker _cuadros = createTicker((_) => _medir());
  Rect? _marco;

  /// Si la última acción fue una tecla. Un clic o un toque lo apaga, como :focus-visible en la web.
  bool _conTeclado = false;

  @override
  void initState() {
    super.initState();
    FocusManager.instance.addListener(_actualizar);
    HardwareKeyboard.instance.addHandler(_tecla);
    GestureBinding.instance.pointerRouter.addGlobalRoute(_puntero);
  }

  @override
  void dispose() {
    FocusManager.instance.removeListener(_actualizar);
    HardwareKeyboard.instance.removeHandler(_tecla);
    GestureBinding.instance.pointerRouter.removeGlobalRoute(_puntero);
    _cuadros.dispose();
    super.dispose();
  }

  bool _tecla(KeyEvent evento) {
    if (evento is KeyDownEvent && !_conTeclado) {
      _conTeclado = true;
      _actualizar();
    }
    return false;
  }

  void _puntero(PointerEvent evento) {
    if (evento is PointerDownEvent && _conTeclado) {
      _conTeclado = false;
      _actualizar();
    }
  }

  /// Mientras se usa el teclado, se mide en cada cuadro: el control puede moverse al desplazar la pantalla.
  void _actualizar() {
    if (_conTeclado && !_cuadros.isActive) _cuadros.start();
    if (!_conTeclado && _cuadros.isActive) _cuadros.stop();
    _medir();
  }

  void _medir() {
    if (!mounted) return;
    Rect? marco;
    final nodo = FocusManager.instance.primaryFocus;
    final pantalla = MediaQuery.sizeOf(context);
    if (_conTeclado && nodo != null && nodo is! FocusScopeNode && nodo.context != null) {
      final caja = nodo.context!.findRenderObject();
      if (caja is RenderBox && caja.attached && caja.hasSize) {
        final rect = caja.localToGlobal(Offset.zero) & caja.size;
        // Un nodo que ocupa casi toda la pantalla (una página entera) no es un control: no se marca.
        if (rect.width * rect.height < pantalla.width * pantalla.height * 0.6) marco = rect;
      }
    }
    if (marco != _marco) setState(() => _marco = marco);
  }

  @override
  Widget build(BuildContext context) {
    final oscuro = Theme.of(context).brightness == Brightness.dark;
    return Stack(
      children: [
        widget.child,
        if (_marco != null)
          Positioned.fromRect(
            rect: _marco!.inflate(3),
            child: IgnorePointer(
              child: DecoratedBox(
                key: IndicadorDeFoco.llave,
                decoration: BoxDecoration(
                  // El café o el texto claro contrastan más de 3:1 con el fondo, como pide RNF20 para el foco.
                  border: Border.all(color: oscuro ? Colores.textoOscuro : Colores.cafe, width: 3),
                  borderRadius: BorderRadius.circular(10),
                ),
              ),
            ),
          ),
      ],
    );
  }
}

/// Si el dispositivo pide menos movimiento (en Android, «Quitar animaciones»; en el navegador, «reducir
/// movimiento»), las pantallas cambian sin deslizarse (Visión 6.1).
class MenosMovimiento extends StatelessWidget {
  const MenosMovimiento({super.key, required this.child});

  final Widget child;

  static final _sinTransiciones = PageTransitionsTheme(
    builders: {for (final plataforma in TargetPlatform.values) plataforma: const _SinTransicion()},
  );

  @override
  Widget build(BuildContext context) {
    if (!MediaQuery.disableAnimationsOf(context)) return child;
    return Theme(
      data: Theme.of(context).copyWith(pageTransitionsTheme: _sinTransiciones),
      child: child,
    );
  }
}

class _SinTransicion extends PageTransitionsBuilder {
  const _SinTransicion();

  @override
  Widget buildTransitions<T>(
    PageRoute<T> route,
    BuildContext context,
    Animation<double> animation,
    Animation<double> secondaryAnimation,
    Widget child,
  ) => child;
}

/// Un enlace «Saltar al contenido principal», el primero al recorrer con Tab: lleva el foco directo al contenido, sin
/// pasar por todo el menú (Visión 6.1, RNF20). Solo se ve mientras tiene el foco.
class SaltoAlContenido extends StatefulWidget {
  const SaltoAlContenido({super.key, required this.contenido, required this.marco});

  final Widget contenido;

  /// Arma la página (menú, barra…) alrededor del contenido que recibe.
  final Widget Function(Widget contenido) marco;

  static const texto = 'Saltar al contenido principal';

  @override
  State<SaltoAlContenido> createState() => _SaltoAlContenidoState();
}

class _SaltoAlContenidoState extends State<SaltoAlContenido> {
  final _destino = FocusNode(debugLabel: 'contenido principal', skipTraversal: true);
  final _enlace = FocusNode(debugLabel: 'saltar al contenido');
  bool _visible = false;

  @override
  void dispose() {
    _destino.dispose();
    _enlace.dispose();
    super.dispose();
  }

  /// En una página recién abierta todavía no hay ningún control con foco, solo la página. El primer Tab lleva al
  /// enlace, y desde ahí sigue el recorrido normal. Sin esto, Tab saltaría al contenido o no iría a ningún lado.
  KeyEventResult _alTeclear(FocusNode _, KeyEvent evento) {
    final actual = FocusManager.instance.primaryFocus;
    if (evento is KeyDownEvent && evento.logicalKey == LogicalKeyboardKey.tab && actual is FocusScopeNode) {
      _enlace.requestFocus();
      return KeyEventResult.handled;
    }
    return KeyEventResult.ignored;
  }

  @override
  Widget build(BuildContext context) {
    final contenido = Focus(
      focusNode: _destino,
      child: Semantics(container: true, label: 'Contenido principal', child: widget.contenido),
    );
    return Focus(
      canRequestFocus: false,
      skipTraversal: true,
      onKeyEvent: _alTeclear,
      child: FocusTraversalGroup(
        policy: OrderedTraversalPolicy(),
        child: Stack(
          children: [
            FocusTraversalOrder(
              order: const NumericFocusOrder(1),
              child: FocusTraversalGroup(child: widget.marco(contenido)),
            ),
            Positioned(
              left: 8,
              top: 8,
              child: FocusTraversalOrder(
                order: const NumericFocusOrder(0),
                child: Opacity(
                  opacity: _visible ? 1 : 0,
                  child: IgnorePointer(
                    ignoring: !_visible,
                    child: Focus(
                      canRequestFocus: false,
                      skipTraversal: true,
                      onFocusChange: (conFoco) => setState(() => _visible = conFoco),
                      child: FilledButton(
                        focusNode: _enlace,
                        onPressed: _destino.requestFocus,
                        child: const Text(SaltoAlContenido.texto),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Una lista dentro de una tarjeta blanca, con una línea suave entre filas, como en el resto del Panel. Se desliza
/// como una lista normal, así funciona con «deslizar para actualizar».
class ListaEnTarjeta extends StatelessWidget {
  const ListaEnTarjeta({super.key, this.padding, required this.itemCount, required this.itemBuilder});

  final EdgeInsetsGeometry? padding;
  final int itemCount;
  final IndexedWidgetBuilder itemBuilder;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: padding,
      children: [
        if (itemCount > 0)
          Card(
            margin: EdgeInsets.zero,
            clipBehavior: Clip.antiAlias,
            child: ListView.separated(
              shrinkWrap: true,
              primary: false,
              physics: const NeverScrollableScrollPhysics(),
              padding: EdgeInsets.zero,
              itemCount: itemCount,
              separatorBuilder: (_, _) => const Divider(height: 1),
              itemBuilder: itemBuilder,
            ),
          ),
      ],
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

/// Etiqueta de estado de un pedido, un pago o una cuenta. El color dice en qué punto está (ver [Tono]).
class Etiqueta extends StatelessWidget {
  const Etiqueta(this.texto, {super.key, this.tono = Tono.neutro});

  final String texto;
  final Tono tono;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final colores = tema.colorScheme;
    final oscuro = tema.brightness == Brightness.dark;
    final relleno = oscuro ? tono.rellenoOscuro : tono.relleno;
    final texto = (oscuro ? tono.textoOscuro : tono.texto) ?? colores.onSurfaceVariant;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: relleno,
        border: Border.all(color: relleno ?? colores.outline),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Text(this.texto, style: tema.textTheme.labelLarge?.copyWith(color: texto)),
    );
  }
}

/// El estado de un pedido como etiqueta de color.
class EtiquetaEstado extends StatelessWidget {
  const EtiquetaEstado(this.estado, {super.key});

  final String estado;

  static const tonos = {
    'PENDIENTE': Tono.rojo,
    'CONFIRMADO': Tono.azul,
    'EN_PREPARACION': Tono.ambar,
    'LISTO': Tono.verde,
    'EN_CAMINO': Tono.azul,
    'ENTREGADO': Tono.verde,
    'CANCELADO': Tono.neutro,
  };

  @override
  Widget build(BuildContext context) =>
      Etiqueta(nombresEstadoPedido[estado] ?? estado, tono: tonos[estado] ?? Tono.neutro);
}

/// El estado de un pago como etiqueta de color, con «Pago:» delante.
class EtiquetaPago extends StatelessWidget {
  const EtiquetaPago(this.estado, {super.key});

  final String estado;

  static const tonos = {
    'PENDIENTE_DE_PAGO': Tono.rojo,
    'EN_VERIFICACION': Tono.ambar,
    'PAGADO': Tono.verde,
    'RECHAZADO': Tono.neutro,
  };

  @override
  Widget build(BuildContext context) => Etiqueta(textos[estado] ?? estado, tono: tonos[estado] ?? Tono.neutro);

  /// Cortas, para que entren en la misma fila que la etiqueta del pedido en un teléfono.
  static const textos = {
    'PENDIENTE_DE_PAGO': 'Pago pendiente',
    'EN_VERIFICACION': 'Pago en verificación',
    'PAGADO': 'Pagado',
    'RECHAZADO': 'Pago rechazado',
  };
}

void avisar(BuildContext context, String mensaje) {
  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(mensaje)));
}
