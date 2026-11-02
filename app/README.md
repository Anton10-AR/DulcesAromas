# Dulces Aromas — Tienda y Panel

Un solo proyecto Flutter con dos puntos de entrada que comparten `lib/comun/`:

- **Tienda** (clientes, Android y navegador): `lib/main.dart`, con el código en `lib/tienda/`.
- **Panel** (personal, navegador): `lib/main_panel.dart`, con el código en `lib/panel/`.

Cómo instalar, configurar y ejecutar todo el sistema está en el [README principal](../README.md).

```
flutter test                                                   # pruebas
dart format lib test                                           # formato (ancho 120)
flutter run -d chrome --web-port 8081                          # Tienda
flutter run -d chrome --web-port 8081 -t lib/main_panel.dart   # Panel
```
