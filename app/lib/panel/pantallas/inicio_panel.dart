import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/marca.dart';
import '../../comun/modelos.dart';
import '../../comun/sesion.dart';
import '../../comun/tema.dart';
import '../../comun/widgets.dart';
import '../app_panel.dart';
import '../modelos_panel.dart';

/// Algo que espera al personal: cuántos hay y adónde ir para atenderlos.
class Pendiente {
  const Pendiente(this.titulo, this.cantidad, this.destino, this.icono, this.tono);

  final String titulo;
  final int cantidad;
  final String destino;
  final IconData icono;

  /// Color cuando hay algo que atender; sin pendientes, la tarjeta queda neutra.
  final Tono tono;
}

/// Un acceso rápido del inicio: a dónde lleva y para qué sirve.
class Acceso {
  const Acceso(this.ruta, this.titulo, this.descripcion, this.icono);

  final String ruta;
  final String titulo;
  final String descripcion;
  final IconData icono;
}

/// Qué se hace en cada sección, para los accesos rápidos.
const _descripciones = {
  '/pedidos': 'Ver, preparar y entregar pedidos.',
  '/cocina': 'Lo que hay que preparar y para cuándo.',
  '/pagos': 'Aprobar o rechazar comprobantes.',
  '/entregas': 'Asignar repartidores a los delivery.',
  '/mis-entregas': 'Tus entregas, con dirección y teléfono.',
  '/catalogo': 'Productos, precios y disponibilidad.',
  '/inventario': 'Insumos, movimientos y alertas.',
  '/reportes': 'Pedidos y ventas por fecha.',
  '/seguridad': 'Cuentas del personal y permisos.',
  '/configuracion': 'Tarifa de envío y métodos de pago.',
};

/// Los accesos rápidos que el rol puede usar: el pedido asistido y las secciones del menú, salvo Inicio y Mi perfil.
List<Acceso> accesosPara(Usuario usuario) => [
  if (usuario.puede('PEDIDOS_CONSULTAR'))
    const Acceso(
      '/pedidos/asistido',
      'Nuevo pedido asistido',
      'Por teléfono o en mostrador.',
      Icons.add_shopping_cart_outlined,
    ),
  for (final s in secciones)
    if (_descripciones.containsKey(s.ruta) && s.visiblePara(usuario))
      Acceso(s.ruta, s.titulo, _descripciones[s.ruta]!, s.icono),
];

/// Inicio del Panel: saludo con la fecha, lo de hoy, lo pendiente que el rol puede atender y accesos rápidos (CU10).
class InicioPanel extends StatefulWidget {
  const InicioPanel({super.key});

  @override
  State<InicioPanel> createState() => _InicioPanelState();
}

class _InicioPanelState extends State<InicioPanel> {
  late Future<List<Pendiente>> _pendientes;

  /// Pedidos y ventas de hoy; solo con el permiso de reportes, porque muestra montos (HU14).
  Future<Reporte>? _hoy;

  /// Los pedidos más recientes, para quien puede ver la lista de pedidos.
  Future<List<PedidoResumen>>? _ultimos;

  /// Cuántos pedidos recientes se muestran.
  static const cantidadUltimos = 5;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  void _cargar() {
    final api = context.read<Api>();
    final usuario = context.read<Sesion>().usuario!;
    Future<Pendiente> contar(
      String titulo,
      String ruta,
      String destino,
      IconData icono,
      Tono tono, [
      Map<String, String>? p,
    ]) => api.get(ruta, p).then((j) => Pendiente(titulo, (j as List).length, destino, icono, tono));
    final ahora = aLaPaz(DateTime.now());
    final hoy = DateFormat('yyyy-MM-dd').format(ahora);
    final haceUnMes = DateFormat('yyyy-MM-dd').format(ahora.subtract(const Duration(days: 30)));
    final vePedidos = ['PEDIDOS_CONSULTAR', 'DISTRIBUCION_ASIGNAR'].any(usuario.puede);
    setState(() {
      _pendientes = [
        if (usuario.puede('COBRANZA_VERIFICAR'))
          contar('Pagos por verificar', '/api/pagos/en-verificacion', '/pagos', Icons.fact_check_outlined, Tono.ambar),
        // Cocina trae los Confirmados y los En preparación; por preparar son los que nadie empezó.
        if (usuario.puede('PEDIDOS_PREPARAR'))
          api
              .get('/api/cocina')
              .then(
                (j) => Pendiente(
                  'Pedidos por preparar',
                  (j as List).where((p) => (p as Json)['estado'] == 'CONFIRMADO').length,
                  '/cocina',
                  Icons.soup_kitchen_outlined,
                  Tono.azul,
                ),
              ),
        if (usuario.puede('DISTRIBUCION_ASIGNAR'))
          contar('Pedidos listos para entregar', '/api/pedidos', '/pedidos', Icons.inventory_outlined, Tono.verde, {
            'estado': 'LISTO',
          }),
        // Solo el Repartidor tiene entregas propias; el administrador las ve todas en Entregas.
        if (usuario.rol == 'REPARTIDOR' && usuario.puede('DISTRIBUCION_ENTREGAS'))
          contar(
            'Mis entregas abiertas',
            '/api/entregas/mias',
            '/mis-entregas',
            Icons.delivery_dining_outlined,
            Tono.azul,
          ),
      ].wait;
      // La lista del Panel viene ordenada por horario de entrega; aquí importan los recién registrados.
      _ultimos = vePedidos
          ? api.get('/api/pedidos', {'desde': haceUnMes, 'hasta': hoy}).then((j) {
              final pedidos = lista(j, PedidoResumen.deJson)
                ..sort((a, b) => b.fechaCreacion.compareTo(a.fechaCreacion));
              return pedidos.take(cantidadUltimos).toList();
            })
          : null;
      _hoy = usuario.puede('REPORTES')
          ? api.get('/api/reportes/ventas', {'desde': hoy, 'hasta': hoy}).then((j) => Reporte.deJson(j as Json))
          : null;
    });
  }

  @override
  Widget build(BuildContext context) {
    final usuario = context.watch<Sesion>().usuario!;
    final textos = Theme.of(context).textTheme;
    final colores = Theme.of(context).colorScheme;
    final primerNombre = usuario.nombre.trim().split(RegExp(r'\s+')).first;
    final accesos = accesosPara(usuario);

    Widget titulo(String texto) => Padding(
      padding: const EdgeInsets.only(top: 28, bottom: 12),
      child: Text(
        texto.toUpperCase(),
        style: textos.labelLarge?.copyWith(color: colores.onSurfaceVariant, letterSpacing: 1.2),
      ),
    );

    return RefreshIndicator(
      onRefresh: () async => _cargar(),
      child: ListView(
        padding: const EdgeInsets.fromLTRB(24, 24, 24, 16),
        children: [
          Contenido(
            ancho: 1100,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _Bienvenida(nombre: primerNombre, rol: usuario.rol),
                if (_hoy != null) ...[titulo('Hoy'), _Hoy(futuro: _hoy!, reintentar: _cargar)],
                FutureBuilder(
                  future: _pendientes,
                  builder: (context, estado) {
                    if (estado.hasData && estado.data!.isEmpty) return const SizedBox.shrink();
                    return Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        titulo('Pendientes'),
                        Cargador(
                          futuro: _pendientes,
                          reintentar: _cargar,
                          constructor: (context, pendientes) =>
                              _Rejilla(anchoMinimo: 230, hijos: [for (final p in pendientes) _TarjetaPendiente(p)]),
                        ),
                      ],
                    );
                  },
                ),
                if (_ultimos != null) ...[
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Expanded(child: titulo('Últimos pedidos')),
                      Padding(
                        padding: const EdgeInsets.only(bottom: 4),
                        child: TextButton(onPressed: () => context.go('/pedidos'), child: const Text('Ver todos')),
                      ),
                    ],
                  ),
                  _Ultimos(futuro: _ultimos!, reintentar: _cargar),
                ],
                if (accesos.isNotEmpty) ...[
                  titulo('Accesos rápidos'),
                  _Rejilla(anchoMinimo: 260, hijos: [for (final a in accesos) _TarjetaAcceso(a)]),
                ],
                const SizedBox(height: 32),
                const Divider(),
                const SizedBox(height: 8),
                Text('Dulces Aromas · Momentos para compartir', style: textos.bodySmall),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Tarjetas del mismo ancho, en tantas columnas como quepan; en el teléfono, una debajo de otra (RNF12).
class _Rejilla extends StatelessWidget {
  const _Rejilla({required this.anchoMinimo, required this.hijos});

  final double anchoMinimo;
  final List<Widget> hijos;

  @override
  Widget build(BuildContext context) {
    const espacio = 16.0;
    return LayoutBuilder(
      builder: (context, limites) {
        final columnas = ((limites.maxWidth + espacio) / (anchoMinimo + espacio)).floor().clamp(1, 4);
        final ancho = (limites.maxWidth - espacio * (columnas - 1)) / columnas;
        return Wrap(
          spacing: espacio,
          runSpacing: espacio,
          children: [for (final h in hijos) SizedBox(width: ancho, child: h)],
        );
      },
    );
  }
}

/// Un ícono sobre un cuadro de color suave, como en las etiquetas de estado.
class _Icono extends StatelessWidget {
  const _Icono(this.icono, this.tono);

  final IconData icono;
  final Tono tono;

  @override
  Widget build(BuildContext context) {
    final oscuro = Theme.of(context).brightness == Brightness.dark;
    final colores = Theme.of(context).colorScheme;
    final relleno = (oscuro ? tono.rellenoOscuro : tono.relleno) ?? colores.secondaryContainer;
    final color = (oscuro ? tono.textoOscuro : tono.texto) ?? colores.onSecondaryContainer;
    return Container(
      width: 44,
      height: 44,
      decoration: BoxDecoration(color: relleno, borderRadius: BorderRadius.circular(10)),
      child: Icon(icono, color: color),
    );
  }
}

class _TarjetaPendiente extends StatelessWidget {
  const _TarjetaPendiente(this.pendiente);

  final Pendiente pendiente;

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    final hay = pendiente.cantidad > 0;
    return Card(
      margin: EdgeInsets.zero,
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => context.go(pendiente.destino),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              _Icono(pendiente.icono, hay ? pendiente.tono : Tono.neutro),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('${pendiente.cantidad}', style: textos.headlineSmall),
                    Text(pendiente.titulo, style: textos.bodyMedium),
                    if (!hay) Text('Nada pendiente', style: textos.bodySmall),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _TarjetaAcceso extends StatelessWidget {
  const _TarjetaAcceso(this.acceso);

  final Acceso acceso;

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    return Card(
      margin: EdgeInsets.zero,
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => context.go(acceso.ruta),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              _Icono(acceso.icono, Tono.neutro),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(acceso.titulo, style: textos.titleMedium),
                    Text(acceso.descripcion, style: textos.bodySmall),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right),
            ],
          ),
        ),
      ),
    );
  }
}

/// Pedidos registrados hoy y lo vendido: pagado y no cancelado. Los montos los calcula el servidor.
class _Hoy extends StatelessWidget {
  const _Hoy({required this.futuro, required this.reintentar});

  final Future<Reporte> futuro;
  final VoidCallback reintentar;

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    Widget cifra(String valor, String etiqueta, IconData icono, Tono tono) => Card(
      margin: EdgeInsets.zero,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            _Icono(icono, tono),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(valor, style: textos.headlineSmall),
                  Text(etiqueta, style: textos.bodyMedium),
                ],
              ),
            ),
          ],
        ),
      ),
    );
    return Cargador(
      futuro: futuro,
      reintentar: reintentar,
      constructor: (context, r) => _Rejilla(
        anchoMinimo: 230,
        hijos: [
          cifra('${r.cantidadPedidos}', 'Pedidos registrados hoy', Icons.receipt_long_outlined, Tono.azul),
          cifra(bolivianos(r.montoVendido), 'Cobrado de los pedidos de hoy', Icons.payments_outlined, Tono.verde),
        ],
      ),
    );
  }
}

/// Los pedidos registrados más recientemente, como en la lista de Pedidos.
class _Ultimos extends StatelessWidget {
  const _Ultimos({required this.futuro, required this.reintentar});

  final Future<List<PedidoResumen>> futuro;
  final VoidCallback reintentar;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: EdgeInsets.zero,
      clipBehavior: Clip.antiAlias,
      child: Cargador(
        futuro: futuro,
        reintentar: reintentar,
        constructor: (context, pedidos) {
          if (pedidos.isEmpty) {
            return const Padding(padding: EdgeInsets.all(20), child: Text('No hay pedidos en los últimos 30 días.'));
          }
          return Column(
            children: [
              for (final (i, p) in pedidos.indexed) ...[
                if (i > 0) const Divider(height: 1),
                ListTile(
                  onTap: () => context.go('/pedidos/${p.id}'),
                  title: Text(['Pedido #${p.id}', ?p.cliente].join(' · ')),
                  subtitle: Text(
                    [
                      'Registrado: ${fechaHora(p.fechaCreacion)}',
                      p.tipoEntrega == 'DELIVERY' ? 'Delivery' : 'Recojo',
                      cantidadProductos(p.productos),
                    ].join(' · '),
                  ),
                  // En una fila: en una columna, la etiqueta y el monto no caben en el alto de la fila.
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(bolivianos(p.total), style: Theme.of(context).textTheme.titleSmall),
                      const SizedBox(width: 12),
                      EtiquetaEstado(p.estado),
                    ],
                  ),
                ),
              ],
            ],
          );
        },
      ),
    );
  }
}

/// Nombre del rol para el saludo.
const _nombresRol = {
  'ADMINISTRADOR': 'Administrador',
  'VENDEDOR': 'Vendedor',
  'REPARTIDOR': 'Repartidor',
  'REPOSTERO': 'Repostero',
};

/// El saludo del inicio, en una franja café con fotos de postres a la derecha en pantallas anchas.
class _Bienvenida extends StatelessWidget {
  const _Bienvenida({required this.nombre, required this.rol});

  final String nombre;
  final String? rol;

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    return ClipRRect(
      borderRadius: BorderRadius.circular(20),
      child: DecoratedBox(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            colors: [Colores.cafe, Color(0xFF6E4B3E)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
        ),
        child: LayoutBuilder(
          builder: (context, limites) {
            final conFotos = limites.maxWidth >= 640;
            return Row(
              children: [
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(28, 26, 20, 26),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (_nombresRol[rol] case final r?)
                          Container(
                            margin: const EdgeInsets.only(bottom: 10),
                            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
                            decoration: BoxDecoration(
                              color: Colores.rosaClaro,
                              borderRadius: BorderRadius.circular(16),
                            ),
                            child: Text(r, style: textos.labelLarge?.copyWith(color: Colores.texto)),
                          ),
                        Text('Hola, $nombre', style: textos.headlineMedium?.copyWith(color: Colors.white)),
                        const SizedBox(height: 4),
                        Text(
                          '${fechaLarga(DateTime.now())} · Sucre',
                          style: textos.bodyLarge?.copyWith(color: const Color(0xFFF3E6DC)),
                        ),
                      ],
                    ),
                  ),
                ),
                // Las fotos son decoración: el lector de pantalla no las anuncia.
                if (conFotos)
                  ExcludeSemantics(
                    child: Padding(
                      padding: const EdgeInsets.all(14),
                      child: Row(
                        children: [
                          for (final (id, _) in fotosPortada.take(3))
                            Padding(
                              padding: const EdgeInsets.only(left: 10),
                              child: ClipRRect(
                                borderRadius: BorderRadius.circular(14),
                                child: Image.asset(rutaFoto(id), width: 96, height: 112, fit: BoxFit.cover),
                              ),
                            ),
                        ],
                      ),
                    ),
                  ),
              ],
            );
          },
        ),
      ),
    );
  }
}
