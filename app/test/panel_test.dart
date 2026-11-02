import 'dart:convert';

import 'package:dulces_aromas/comun/api.dart';
import 'package:dulces_aromas/panel/app_panel.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';

http.Response json(Object cuerpo, [int estado = 200]) =>
    http.Response(jsonEncode(cuerpo), estado, headers: {'content-type': 'application/json; charset=utf-8'});

/// Una API falsa en la que ingresa una cuenta con el tipo, el rol y los permisos indicados.
Api apiCon({required String tipo, String? rol, List<String> permisos = const [], bool sesionVencida = false}) {
  return Api(
    base: 'http://api',
    cliente: MockClient((solicitud) async {
      final ruta = solicitud.url.path;
      if (ruta == '/api/auth/login') {
        return json({
          'token': 't',
          'usuario': {'id': 1, 'nombre': 'Luz', 'correo': 'luz@dulces.bo', 'tipo': tipo, 'rol': rol, 'permisos': permisos},
          'debeCambiarContrasena': false,
        });
      }
      if (sesionVencida) return json({'codigo': 'SIN_SESION', 'mensaje': 'Inicia sesión para continuar.'}, 401);
      return switch (ruta) {
        '/api/auth/logout' => http.Response('', 204),
        _ => json([]),
      };
    }),
  );
}

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));

  Future<void> ingresar(WidgetTester tester, Api api) async {
    tester.view.physicalSize = const Size(1400, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(AppPanel(api: api));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Correo'), 'luz@dulces.bo');
    await tester.enterText(find.widgetWithText(TextField, 'Contraseña'), 'Secreta123');
    await tester.tap(find.widgetWithText(FilledButton, 'Ingresar'));
    await tester.pumpAndSettle();
  }

  testWidgets('HU06: el Vendedor ve pedidos, entregas y catálogo, pero no la verificación de pagos', (tester) async {
    await ingresar(
        tester,
        apiCon(tipo: 'PERSONAL', rol: 'VENDEDOR', permisos: [
          'INICIO_PENDIENTES', 'CATALOGO_DISPONIBILIDAD', 'PEDIDOS_CONSULTAR', 'PEDIDOS_CANCELAR', 'DISTRIBUCION_ASIGNAR',
        ]));

    expect(find.text('Hola, Luz'), findsOneWidget);
    for (final visible in ['Pedidos', 'Entregas', 'Catálogo', 'Mi perfil']) {
      expect(find.text(visible), findsWidgets, reason: visible);
    }
    expect(find.text('Verificar pagos'), findsNothing);
    expect(find.text('Mis entregas'), findsNothing);
  });

  testWidgets('HU06: el Repartidor solo ve sus entregas', (tester) async {
    await ingresar(tester, apiCon(tipo: 'PERSONAL', rol: 'REPARTIDOR', permisos: ['DISTRIBUCION_ENTREGAS']));

    expect(find.text('Mis entregas'), findsOneWidget);
    for (final oculto in ['Pedidos', 'Verificar pagos', 'Entregas', 'Catálogo']) {
      expect(find.text(oculto), findsNothing, reason: oculto);
    }
  });

  testWidgets('una cuenta de cliente no entra al Panel', (tester) async {
    await ingresar(tester, apiCon(tipo: 'CLIENTE'));

    expect(find.text('Esta cuenta es de cliente. Usa la Tienda para pedir.'), findsOneWidget);
    expect(find.text('Hola, Luz'), findsNothing);
  });

  testWidgets('RNF06: si la sesión venció, vuelve al ingreso y lo explica', (tester) async {
    await ingresar(tester, apiCon(tipo: 'PERSONAL', rol: 'ADMINISTRADOR', permisos: ['PEDIDOS_PREPARAR'], sesionVencida: true));

    expect(find.text('Tu sesión se cerró tras 30 minutos sin uso. Ingresa de nuevo.'), findsOneWidget);
  });
}
