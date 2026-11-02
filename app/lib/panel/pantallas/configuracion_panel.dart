import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/widgets.dart';
import 'formulario.dart';

/// Tarifa fija de envío (D4) y métodos de pago con sus datos para pagar (RNF18, CU06).
class ConfiguracionPanel extends StatefulWidget {
  const ConfiguracionPanel({super.key});

  @override
  State<ConfiguracionPanel> createState() => _ConfiguracionPanelState();
}

class _ConfiguracionPanelState extends State<ConfiguracionPanel> {
  late Future<(String, List<MetodoPago>)> _datos;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  void _cargar() {
    setState(() {
      _datos = (
        _api.get('/api/configuracion').then((j) => decimalDe((j as Json)['tarifaEnvio']).toString()),
        _api.get('/api/metodos-pago/gestion').then((j) => lista(j, MetodoPago.deJson)),
      ).wait;
    });
  }

  Future<void> _tarifa(String actual) async {
    final tarifa = CampoDialogo(
      'tarifaEnvio',
      'Tarifa de envío (Bs)',
      valor: actual,
      teclado: const TextInputType.numberWithOptions(decimal: true),
    );
    final hecho = await formularioEnDialogo(
      context,
      titulo: 'Tarifa de envío',
      campos: [tarifa],
      enviar: () => _api.put('/api/configuracion', {'tarifaEnvio': tarifa.texto.replaceAll(',', '.')}),
    );
    if (hecho != null) _cargar();
  }

  Future<void> _metodo({MetodoPago? metodo}) async {
    final nombre = CampoDialogo('nombre', 'Nombre', valor: metodo?.nombre ?? '');
    final instrucciones = CampoDialogo(
      'instrucciones',
      'Datos para pagar',
      valor: metodo?.instrucciones ?? '',
      ayuda: 'Banco, número de cuenta y titular. Los ve el cliente al pagar.',
    );
    Map<String, dynamic> datos() => {'nombre': nombre.texto, 'instrucciones': instrucciones.texto};
    final hecho = await formularioEnDialogo(
      context,
      titulo: metodo == null ? 'Nuevo método de pago' : 'Editar ${metodo.nombre}',
      campos: [nombre, instrucciones],
      enviar: () => metodo == null
          ? _api.post('/api/metodos-pago', datos())
          : _api.put('/api/metodos-pago/${metodo.id}', datos()),
    );
    if (hecho != null) _cargar();
  }

  Future<void> _ejecutar(Future<dynamic> Function() llamada, String hecho) async {
    try {
      await llamada();
      if (mounted) avisar(context, hecho);
    } on ErrorApi catch (e) {
      if (mounted) avisar(context, e.mensaje);
    }
    _cargar();
  }

  Future<void> _qr(MetodoPago metodo) async {
    final archivo = await FilePicker.pickFile(type: FileType.custom, allowedExtensions: const ['jpg', 'jpeg', 'png']);
    if (archivo == null) return;
    final bytes = await archivo.readAsBytes();
    await _ejecutar(
      () => _api.multipart(
        'PUT',
        '/api/metodos-pago/${metodo.id}/qr',
        nombreArchivo: 'archivo',
        bytes: bytes,
        archivo: archivo.name,
      ),
      'QR actualizado.',
    );
  }

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    return Cargador(
      futuro: _datos,
      reintentar: _cargar,
      constructor: (context, datos) {
        final (tarifa, metodos) = datos;
        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Contenido(
              ancho: 900,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text('Envío', style: textos.titleLarge),
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    title: Text('Tarifa fija de delivery: ${bolivianos(decimalDe(tarifa))}'),
                    trailing: OutlinedButton(onPressed: () => _tarifa(tarifa), child: const Text('Cambiar')),
                  ),
                  const Divider(height: 32),
                  Row(
                    children: [
                      Text('Métodos de pago', style: textos.titleLarge),
                      const Spacer(),
                      FilledButton.icon(
                        onPressed: () => _metodo(),
                        icon: const Icon(Icons.add),
                        label: const Text('Nuevo método'),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Un método nuevo nace desactivado. Escribe los datos para pagar y actívalo para que el cliente lo vea.',
                    style: textos.bodySmall,
                  ),
                  for (final m in metodos)
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Text(m.nombre, style: textos.titleMedium),
                                const SizedBox(width: 8),
                                Etiqueta(m.activo ? 'Activo' : 'Desactivado', resaltada: m.activo),
                              ],
                            ),
                            const SizedBox(height: 4),
                            Text(m.instrucciones.isEmpty ? 'Sin datos para pagar.' : m.instrucciones),
                            if (m.qr != null) ...[
                              const SizedBox(height: 8),
                              Image.network(
                                _api.absoluta(m.qr!),
                                width: 120,
                                height: 120,
                                semanticLabel: 'QR de ${m.nombre}',
                              ),
                            ],
                            const SizedBox(height: 8),
                            Wrap(
                              spacing: 8,
                              runSpacing: 8,
                              children: [
                                OutlinedButton(
                                  onPressed: () => _metodo(metodo: m),
                                  child: const Text('Editar'),
                                ),
                                OutlinedButton(
                                  onPressed: () => _qr(m),
                                  child: Text(m.qr == null ? 'Subir QR' : 'Cambiar QR'),
                                ),
                                m.activo
                                    ? TextButton(
                                        onPressed: () => _ejecutar(
                                          () => _api.post('/api/metodos-pago/${m.id}/desactivar'),
                                          'Método desactivado.',
                                        ),
                                        child: const Text('Desactivar'),
                                      )
                                    : FilledButton.tonal(
                                        onPressed: () => _ejecutar(
                                          () => _api.post('/api/metodos-pago/${m.id}/activar'),
                                          'Método activado.',
                                        ),
                                        child: const Text('Activar'),
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
          ],
        );
      },
    );
  }
}
