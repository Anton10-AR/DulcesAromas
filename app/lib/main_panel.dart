import 'package:flutter/material.dart';
import 'package:flutter_web_plugins/url_strategy.dart';

import 'comun/api.dart';
import 'panel/app_panel.dart';

/// El Panel del personal (web). La Tienda está en `lib/main.dart`.
void main() {
  // En la web, las direcciones van sin «#» (…/recuperar?token=…), igual que el enlace del correo y los App Links.
  usePathUrlStrategy();
  runApp(AppPanel(api: Api()));
}
