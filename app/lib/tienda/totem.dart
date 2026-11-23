import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../comun/formato.dart';
import '../comun/sesion.dart';
import 'carrito.dart';

/// Modo tótem: la Tienda en una pantalla táctil pública, como la del stand de la feria EXPOSOFT. Se activa en el equipo
/// abriendo `/totem` y se recuerda en él. En este modo:
/// - el texto y los botones se ven más grandes;
/// - no se muestra la cuenta, para que ningún visitante cierre la sesión del tótem;
/// - el pedido es de recojo, con el primer horario válido ya elegido, para no escribir nada;
/// - tras un rato sin tocar la pantalla, vacía el carrito y vuelve al catálogo, listo para el siguiente visitante.
class ModoTotem extends ChangeNotifier {
  static const _clave = 'modo_totem';

  bool activo = false;

  Future<void> cargar() async {
    final preferencias = await SharedPreferences.getInstance();
    activo = preferencias.getBool(_clave) ?? false;
    notifyListeners();
  }

  Future<void> cambiar(bool valor) async {
    activo = valor;
    notifyListeners();
    final preferencias = await SharedPreferences.getInstance();
    await preferencias.setBool(_clave, valor);
  }
}

/// El primer momento en que se puede recoger un pedido: dentro del horario de atención y con 24 horas de anticipación,
/// redondeado a la media hora siguiente. Es la hora de La Paz, como la elegiría el cliente. El servidor lo valida igual.
DateTime primerHorarioDeRecojo([DateTime? ahora]) {
  var momento = aLaPaz(ahora ?? DateTime.now()).add(const Duration(hours: 24));
  momento = DateTime(momento.year, momento.month, momento.day, momento.hour, momento.minute);
  final resto = momento.minute % 30;
  if (resto != 0 || momento.second != 0) momento = momento.add(Duration(minutes: 30 - resto));
  for (var intento = 0; intento < 8; intento++) {
    final domingo = momento.weekday == DateTime.sunday;
    final apertura = DateTime(momento.year, momento.month, momento.day, domingo ? 10 : 9);
    final cierre = DateTime(momento.year, momento.month, momento.day, domingo ? 14 : 20);
    if (momento.isBefore(apertura)) return apertura;
    if (momento.isBefore(cierre)) return momento;
    final siguiente = momento.add(const Duration(days: 1));
    momento = DateTime(siguiente.year, siguiente.month, siguiente.day);
  }
  return momento;
}

/// Vigila que alguien esté usando el tótem. Tras [espera] sin tocar la pantalla avisa, y tras [aviso] más vacía el
/// carrito y vuelve al catálogo. Si ya está en el catálogo con el carrito vacío, no hace nada.
class VigiaTotem extends StatefulWidget {
  const VigiaTotem({
    super.key,
    required this.rutas,
    required this.child,
    this.espera = const Duration(seconds: 75),
    this.aviso = const Duration(seconds: 15),
  });

  final GoRouter rutas;
  final Widget child;
  final Duration espera;
  final Duration aviso;

  @override
  State<VigiaTotem> createState() => _VigiaTotemState();
}

class _VigiaTotemState extends State<VigiaTotem> {
  Timer? _reloj;
  int? _quedan;

  /// Si ya se le dio más tiempo a la pantalla de pago en esta espera.
  bool _extendido = false;

  /// En la pantalla de pago el visitante paga con su celular (el QR) y no toca el tótem: ahí se espera más.
  static const _esperaEnPago = Duration(minutes: 5);

  @override
  void initState() {
    super.initState();
    _reiniciar();
  }

  @override
  void dispose() {
    _reloj?.cancel();
    super.dispose();
  }

  void _reiniciar() {
    _reloj?.cancel();
    _extendido = false;
    if (_quedan != null) setState(() => _quedan = null);
    _reloj = Timer(widget.espera, _avisar);
  }

  String get _ruta => widget.rutas.routerDelegate.currentConfiguration.uri.path;

  bool get _hayAlgoQueLimpiar => _ruta != '/' || !context.read<Carrito>().vacio;

  void _avisar() {
    if (!mounted) return;
    if (!_hayAlgoQueLimpiar) {
      _reiniciar();
      return;
    }
    if (_ruta.endsWith('/pagar') && !_extendido) {
      _extendido = true;
      _reloj = Timer(_esperaEnPago - widget.espera, _avisar);
      return;
    }
    setState(() => _quedan = widget.aviso.inSeconds);
    _reloj = Timer.periodic(const Duration(seconds: 1), (reloj) {
      if (!mounted) return;
      final quedan = (_quedan ?? 1) - 1;
      if (quedan > 0) {
        setState(() => _quedan = quedan);
        return;
      }
      reloj.cancel();
      _volverAlInicio();
    });
  }

  void _volverAlInicio() {
    context.read<Carrito>().vaciar();
    // Cierra los cuadros abiertos (por ejemplo, el del pago) antes de volver al catálogo.
    widget.rutas.routerDelegate.navigatorKey.currentState?.popUntil((ruta) => ruta is! PopupRoute);
    widget.rutas.go('/');
    _reiniciar();
  }

  @override
  Widget build(BuildContext context) {
    final mq = MediaQuery.of(context);
    return Listener(
      behavior: HitTestBehavior.translucent,
      onPointerDown: (_) => _reiniciar(),
      child: MediaQuery(
        // Más grande, para tocar con el dedo y leer de pie.
        data: mq.copyWith(textScaler: const TextScaler.linear(1.2)),
        child: Stack(
          children: [
            widget.child,
            if (_quedan != null)
              Positioned(
                left: 16,
                right: 16,
                bottom: 24 + mq.padding.bottom,
                child: Center(
                  child: Material(
                    elevation: 6,
                    borderRadius: BorderRadius.circular(16),
                    color: Theme.of(context).colorScheme.inverseSurface,
                    child: InkWell(
                      borderRadius: BorderRadius.circular(16),
                      onTap: _reiniciar,
                      child: Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
                        child: Text(
                          '¿Sigues ahí? Volvemos al inicio en $_quedan s. Toca la pantalla para seguir.',
                          textAlign: TextAlign.center,
                          style: Theme.of(context).textTheme.titleMedium
                              ?.copyWith(color: Theme.of(context).colorScheme.onInverseSurface),
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

/// `/totem`: activa o desactiva el modo tótem en este equipo. Lo usa el equipo del stand antes de empezar.
class TotemPantalla extends StatelessWidget {
  const TotemPantalla({super.key});

  @override
  Widget build(BuildContext context) {
    final totem = context.watch<ModoTotem>();
    final sesion = context.watch<Sesion>();
    final textos = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Modo tótem')),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 520),
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(totem.activo ? 'El modo tótem está activo' : 'Modo tótem', style: textos.headlineSmall),
                const SizedBox(height: 12),
                const Text(
                  'Para una pantalla táctil pública: letra más grande, recojo con el primer horario libre, pago con '
                  'tarjeta y vuelta al catálogo tras 90 segundos sin uso. No muestra la cuenta, para que nadie cierre '
                  'la sesión.',
                ),
                const SizedBox(height: 16),
                if (!sesion.iniciada) ...[
                  const Text('Primero inicia sesión con la cuenta de cliente del tótem.'),
                  const SizedBox(height: 8),
                  OutlinedButton(
                    onPressed: () => context.go('/ingresar?volver=/totem'),
                    child: const Text('Iniciar sesión'),
                  ),
                  const SizedBox(height: 8),
                ] else
                  Text('Sesión del tótem: ${sesion.usuario!.nombre}', style: textos.bodyLarge),
                const SizedBox(height: 16),
                if (totem.activo) ...[
                  FilledButton(onPressed: () => context.go('/'), child: const Text('Ir al catálogo')),
                  const SizedBox(height: 8),
                  OutlinedButton(onPressed: () => totem.cambiar(false), child: const Text('Desactivar el modo tótem')),
                ] else
                  FilledButton(
                    onPressed: sesion.iniciada
                        ? () async {
                            await totem.cambiar(true);
                            if (context.mounted) context.go('/');
                          }
                        : null,
                    child: const Text('Activar y empezar'),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
