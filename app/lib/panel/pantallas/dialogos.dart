import 'package:flutter/material.dart';

/// Pide un texto obligatorio, como el motivo de una cancelación o la observación de un rechazo.
/// Devuelve nulo si se cancela.
Future<String?> pedirTexto(BuildContext context,
    {required String titulo, required String etiqueta, required String confirmar, int maximo = 500}) {
  final controlador = TextEditingController();
  return showDialog<String>(
    context: context,
    builder: (contexto) => StatefulBuilder(
      builder: (contexto, actualizar) => AlertDialog(
        title: Text(titulo),
        content: TextField(
          controller: controlador,
          autofocus: true,
          maxLength: maximo,
          maxLines: 3,
          onChanged: (_) => actualizar(() {}),
          decoration: InputDecoration(labelText: etiqueta),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(contexto), child: const Text('Volver')),
          FilledButton(
            onPressed: controlador.text.trim().isEmpty ? null : () => Navigator.pop(contexto, controlador.text.trim()),
            child: Text(confirmar),
          ),
        ],
      ),
    ),
  );
}

Future<bool> confirmar(BuildContext context, {required String titulo, required String mensaje, required String accion}) async {
  final respuesta = await showDialog<bool>(
    context: context,
    builder: (contexto) => AlertDialog(
      title: Text(titulo),
      content: Text(mensaje),
      actions: [
        TextButton(onPressed: () => Navigator.pop(contexto, false), child: const Text('Volver')),
        FilledButton(onPressed: () => Navigator.pop(contexto, true), child: Text(accion)),
      ],
    ),
  );
  return respuesta ?? false;
}
