import 'package:flutter/material.dart';

import 'comun/api.dart';
import 'panel/app_panel.dart';

/// El Panel del personal (web). La Tienda está en `lib/main.dart`.
void main() {
  runApp(AppPanel(api: Api()));
}
