import 'dart:async';
import 'dart:js_interop';

import 'package:web/web.dart' as web;

const disponible = true;

const _id = 'impresion';

/// Al imprimir se oculta la app, que se dibuja en un lienzo, y solo queda este contenido. No se usa un `iframe` porque
/// Chrome en Android puede imprimir la página principal en su lugar.
const _estilo =
    '''
#$_id { display: none; }
@media print {
  html, body {
    position: static !important; display: block !important; overflow: visible !important;
    width: auto !important; height: auto !important; min-height: 0 !important;
    margin: 0 !important; padding: 0 !important; background: #fff !important;
  }
  body > *:not(#$_id) { display: none !important; }
  #$_id { display: block !important; }
}
''';

Timer? _limpieza;

void imprimir(String html) {
  final documento = web.document;
  documento.getElementById(_id)?.remove();
  final bloque = documento.createElement('div')
    ..id = _id
    ..innerHTML = '<style>$_estilo</style>$html'.toJS;
  documento.body!.append(bloque);
  web.window.print();
  // En Android `print` vuelve antes de que se elija la impresora, así que el contenido se quita después.
  _limpieza?.cancel();
  _limpieza = Timer(const Duration(minutes: 2), () => bloque.remove());
}
