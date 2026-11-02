import 'package:flutter/material.dart';

import '../../comun/api.dart';
import '../../comun/widgets.dart';

/// Un campo de un formulario en diálogo: su nombre en la API, su etiqueta y su valor inicial.
class CampoDialogo {
  CampoDialogo(this.nombre, this.etiqueta, {String valor = '', this.oculto = false, this.ayuda, this.teclado})
    : controlador = TextEditingController(text: valor);

  final String nombre;
  final String etiqueta;
  final bool oculto;
  final String? ayuda;
  final TextInputType? teclado;
  final TextEditingController controlador;

  String get texto => controlador.text.trim();
}

/// Diálogo con un formulario que se envía a la API. Si el servidor responde con errores, se quedan junto a cada
/// campo y el diálogo sigue abierto (RNF01). Devuelve la respuesta, o nulo si se cancela.
Future<dynamic> formularioEnDialogo(
  BuildContext context, {
  required String titulo,
  required List<CampoDialogo> campos,
  required Future<dynamic> Function() enviar,
  List<Widget> extra = const [],
  String confirmar = 'Guardar',
}) {
  return showDialog<dynamic>(
    context: context,
    builder: (contexto) {
      ErrorApi? error;
      var enviando = false;
      return StatefulBuilder(
        builder: (contexto, actualizar) => AlertDialog(
          title: Text(titulo),
          content: SizedBox(
            width: 420,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (error != null && error!.campos.isEmpty) AvisoError(error!.mensaje),
                  for (final c in campos)
                    Campo(
                      controlador: c.controlador,
                      etiqueta: c.etiqueta,
                      nombre: c.nombre,
                      errores: error?.campos,
                      oculto: c.oculto,
                      ayuda: c.ayuda,
                      teclado: c.teclado,
                    ),
                  ...extra,
                ],
              ),
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(contexto), child: const Text('Cancelar')),
            FilledButton(
              onPressed: enviando
                  ? null
                  : () async {
                      actualizar(() => enviando = true);
                      try {
                        final respuesta = await enviar();
                        if (contexto.mounted) Navigator.pop(contexto, respuesta ?? true);
                      } on ErrorApi catch (e) {
                        actualizar(() {
                          error = e;
                          enviando = false;
                        });
                      }
                    },
              child: Text(confirmar),
            ),
          ],
        ),
      );
    },
  );
}
