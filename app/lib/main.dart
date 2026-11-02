import 'package:flutter/material.dart';
import 'package:flutter_web_plugins/url_strategy.dart';

import 'comun/api.dart';
import 'tienda/app_tienda.dart';

/// La Tienda. El Panel tiene su propio punto de entrada: `lib/main_panel.dart`.
void main() {
  // En la web, las direcciones van sin «#» (…/recuperar?token=…), igual que el enlace del correo y los App Links.
  usePathUrlStrategy();
  runApp(AppTienda(api: Api()));
}
