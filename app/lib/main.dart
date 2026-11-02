import 'package:flutter/material.dart';

import 'comun/api.dart';
import 'tienda/app_tienda.dart';

/// La Tienda. El Panel tiene su propio punto de entrada: `lib/main_panel.dart`.
void main() {
  runApp(AppTienda(api: Api()));
}
