import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../comun/api.dart';
import '../../comun/formato.dart';
import '../../comun/modelos.dart';
import '../../comun/sesion.dart';
import '../../comun/widgets.dart';
import '../modelos_panel.dart';
import 'dialogos.dart';
import 'formulario.dart';

/// Seguridad: cuentas del personal, permisos de los roles fijos y auditoría (CU09, HU03, HU04, HU05).
class SeguridadPanel extends StatelessWidget {
  const SeguridadPanel({super.key});

  @override
  Widget build(BuildContext context) {
    // En pantallas angostas las pestañas se desplazan, para que «Roles y permisos» no se corte (RNF12).
    final angosta = MediaQuery.sizeOf(context).width < 480;
    return DefaultTabController(
      length: 3,
      child: Column(
        children: [
          TabBar(
            isScrollable: angosta,
            tabAlignment: angosta ? TabAlignment.start : null,
            tabs: const [
              Tab(text: 'Personal'),
              Tab(text: 'Roles y permisos'),
              Tab(text: 'Auditoría'),
            ],
          ),
          const Expanded(child: TabBarView(children: [_Personal(), _Roles(), _Auditoria()])),
        ],
      ),
    );
  }
}

class _Personal extends StatefulWidget {
  const _Personal();

  @override
  State<_Personal> createState() => _PersonalState();
}

class _PersonalState extends State<_Personal> {
  late Future<List<CuentaPersonal>> _cuentas;

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  void _cargar() {
    setState(() {
      _cuentas = _api.get('/api/personal').then((j) => lista(j, CuentaPersonal.deJson));
    });
  }

  /// Crear una cuenta (HU03) o editarla y cambiar su rol (HU04). No hay roles nuevos (D6).
  Future<void> _cuenta({CuentaPersonal? cuenta}) async {
    var rol = cuenta?.rol ?? 'VENDEDOR';
    final nombre = CampoDialogo('nombre', 'Nombre', valor: cuenta?.nombre ?? '');
    final correo = CampoDialogo('correo', 'Correo', valor: cuenta?.correo ?? '', teclado: TextInputType.emailAddress);
    final telefono = CampoDialogo(
      'telefono',
      'Teléfono (opcional)',
      valor: cuenta?.telefono ?? '',
      teclado: TextInputType.phone,
    );
    final contrasena = CampoDialogo(
      'contrasenaInicial',
      'Contraseña inicial',
      oculto: true,
      ayuda: 'Al menos 8 caracteres, con una letra y un número.',
    );
    Map<String, dynamic> datos() => {
      'nombre': nombre.texto,
      'correo': correo.texto,
      'telefono': telefono.texto.isEmpty ? null : telefono.texto,
      'rol': rol,
      if (cuenta == null) 'contrasenaInicial': contrasena.controlador.text,
    };
    final hecho = await formularioEnDialogo(
      context,
      titulo: cuenta == null ? 'Nueva cuenta de personal' : 'Editar ${cuenta.nombre}',
      campos: [nombre, correo, telefono, if (cuenta == null) contrasena],
      extra: [
        StatefulBuilder(
          builder: (_, actualizar) => DropdownButtonFormField<String>(
            initialValue: rol,
            decoration: const InputDecoration(labelText: 'Rol'),
            items: [for (final r in nombresRol.entries) DropdownMenuItem(value: r.key, child: Text(r.value))],
            onChanged: (v) => actualizar(() => rol = v!),
          ),
        ),
      ],
      enviar: () =>
          cuenta == null ? _api.post('/api/personal', datos()) : _api.put('/api/personal/${cuenta.id}', datos()),
    );
    if (hecho != null) _cargar();
  }

  /// RN03: el servidor impide desactivar la propia cuenta o al último administrador.
  Future<void> _activa(CuentaPersonal c, bool activa) async {
    if (!activa &&
        !await confirmar(
          context,
          titulo: '¿Desactivar a ${c.nombre}?',
          mensaje: 'Perderá el acceso enseguida. Su historial se conserva.',
          accion: 'Desactivar',
        )) {
      return;
    }
    try {
      await _api.post('/api/personal/${c.id}/${activa ? 'activar' : 'desactivar'}');
    } on ErrorApi catch (e) {
      if (mounted) avisar(context, e.mensaje);
    }
    _cargar();
  }

  /// D5: el administrador restablece la contraseña; la persona la cambia al ingresar.
  Future<void> _restablecer(CuentaPersonal c) async {
    final nueva = CampoDialogo(
      'contrasenaNueva',
      'Contraseña temporal',
      oculto: true,
      ayuda: 'Al ingresar, deberá cambiarla. Sus sesiones abiertas se cerrarán.',
    );
    final hecho = await formularioEnDialogo(
      context,
      titulo: 'Restablecer la contraseña de ${c.nombre}',
      campos: [nueva],
      confirmar: 'Restablecer',
      enviar: () => _api.post('/api/personal/${c.id}/contrasena', {'contrasenaNueva': nueva.controlador.text}),
    );
    if (hecho != null && mounted) avisar(context, 'Contraseña restablecida.');
  }

  @override
  Widget build(BuildContext context) {
    final propia = context.watch<Sesion>().usuario!.id;
    return Scaffold(
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _cuenta(),
        icon: const Icon(Icons.person_add_alt),
        label: const Text('Nueva cuenta'),
      ),
      body: Cargador(
        futuro: _cuentas,
        reintentar: _cargar,
        constructor: (context, cuentas) => ListView.separated(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 88),
          itemCount: cuentas.length,
          separatorBuilder: (_, _) => const Divider(height: 1),
          itemBuilder: (_, i) {
            final c = cuentas[i];
            return FilaConAcciones(
              titulo: Text('${c.nombre}${c.activo ? '' : ' (desactivada)'}'),
              subtitulo: Text(
                '${nombresRol[c.rol]} · ${c.correo}'
                '${c.bloqueadoHasta != null && c.bloqueadoHasta!.isAfter(DateTime.now()) ? ' · Bloqueada hasta ${fechaHora(c.bloqueadoHasta!)}' : ''}',
              ),
              acciones: [
                IconButton(
                  tooltip: 'Editar',
                  onPressed: () => _cuenta(cuenta: c),
                  icon: const Icon(Icons.edit_outlined),
                ),
                IconButton(
                  tooltip: 'Restablecer contraseña',
                  onPressed: () => _restablecer(c),
                  icon: const Icon(Icons.key_outlined),
                ),
                if (c.id != propia)
                  c.activo
                      ? TextButton(onPressed: () => _activa(c, false), child: const Text('Desactivar'))
                      : TextButton(onPressed: () => _activa(c, true), child: const Text('Activar')),
              ],
            );
          },
        ),
      ),
    );
  }
}

class _Roles extends StatefulWidget {
  const _Roles();

  @override
  State<_Roles> createState() => _RolesState();
}

class _RolesState extends State<_Roles> {
  late Future<(List<Rol>, List<PermisoInfo>)> _datos;
  final Map<String, Set<String>> _editados = {};

  Api get _api => context.read<Api>();

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  void _cargar() {
    _editados.clear();
    setState(() {
      _datos = (
        _api.get('/api/roles').then((j) => lista(j, Rol.deJson)),
        _api.get('/api/permisos').then((j) => lista(j, PermisoInfo.deJson)),
      ).wait;
    });
  }

  /// HU05: el cambio rige en la siguiente solicitud de cada usuario con ese rol.
  Future<void> _guardar(Rol rol) async {
    try {
      await _api.put('/api/roles/${rol.codigo}/permisos', {'permisos': _editados[rol.codigo]!.toList()});
      if (!mounted) return;
      avisar(context, 'Permisos de ${rol.nombre} guardados.');
      // Si cambió el rol propio, el menú se actualiza.
      await context.read<Sesion>().actualizar();
    } on ErrorApi catch (e) {
      if (mounted) avisar(context, e.mensaje);
    }
    _cargar();
  }

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    return Cargador(
      futuro: _datos,
      reintentar: _cargar,
      constructor: (context, datos) {
        final (roles, permisos) = datos;
        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            for (final rol in roles)
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(rol.nombre, style: textos.titleLarge),
                      Text(rol.descripcion, style: textos.bodySmall),
                      const SizedBox(height: 8),
                      for (final p in permisos)
                        CheckboxListTile(
                          dense: true,
                          title: Text('${p.modulo}: ${p.accion}'),
                          value: (_editados[rol.codigo] ?? rol.permisos).contains(p.codigo),
                          // Seguridad es solo del administrador y nunca se le quita (RN04, sección 10.1).
                          onChanged: p.codigo == 'SEGURIDAD'
                              ? null
                              : (marcado) => setState(() {
                                  final actuales = _editados.putIfAbsent(rol.codigo, () => {...rol.permisos});
                                  marcado! ? actuales.add(p.codigo) : actuales.remove(p.codigo);
                                }),
                        ),
                      if (_editados.containsKey(rol.codigo))
                        Align(
                          alignment: Alignment.centerRight,
                          child: FilledButton(
                            onPressed: () => _guardar(rol),
                            child: Text('Guardar permisos de ${rol.nombre}'),
                          ),
                        ),
                    ],
                  ),
                ),
              ),
          ],
        );
      },
    );
  }
}

class _Auditoria extends StatefulWidget {
  const _Auditoria();

  @override
  State<_Auditoria> createState() => _AuditoriaState();
}

class _AuditoriaState extends State<_Auditoria> {
  int _pagina = 0;
  late Future<(List<RegistroAuditoria>, int)> _registros;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  void _cargar() {
    setState(() {
      _registros = context.read<Api>().get('/api/auditoria', {'pagina': '$_pagina'}).then((j) {
        final json = j as Json;
        return (lista(json['registros'], RegistroAuditoria.deJson), json['totalPaginas'] as int);
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return Cargador(
      futuro: _registros,
      reintentar: _cargar,
      constructor: (context, datos) {
        final (registros, totalPaginas) = datos;
        return Column(
          children: [
            Expanded(
              child: ListView.separated(
                padding: const EdgeInsets.all(16),
                itemCount: registros.length,
                separatorBuilder: (_, _) => const Divider(height: 1),
                itemBuilder: (_, i) {
                  final r = registros[i];
                  return ListTile(
                    title: Text(r.detalle),
                    subtitle: Text('${fechaHora(r.fecha)} · ${r.usuario} · ${r.accion}'),
                  );
                },
              ),
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                IconButton(
                  tooltip: 'Más recientes',
                  onPressed: _pagina > 0
                      ? () => setState(() {
                          _pagina--;
                          _cargar();
                        })
                      : null,
                  icon: const Icon(Icons.chevron_left),
                ),
                Text('Página ${_pagina + 1} de ${totalPaginas == 0 ? 1 : totalPaginas}'),
                IconButton(
                  tooltip: 'Más antiguos',
                  onPressed: _pagina + 1 < totalPaginas
                      ? () => setState(() {
                          _pagina++;
                          _cargar();
                        })
                      : null,
                  icon: const Icon(Icons.chevron_right),
                ),
              ],
            ),
          ],
        );
      },
    );
  }
}
