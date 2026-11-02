# Dulces Aromas

Comercio electrónico para Dulces Aromas, una pastelería de Sucre, Bolivia: «Momentos para compartir».

- **Tienda** (clientes): aplicación móvil en Flutter. Se ve el catálogo, se personalizan productos, se pide con recojo o delivery, se paga con comprobante y se sigue el pedido.
- **Panel** (personal): aplicación web en Flutter. Se gestionan catálogo, inventario, pedidos, pagos, entregas, cuentas y permisos.
- **API REST**: Spring Boot con PostgreSQL. La usan la Tienda y el Panel.

Proyecto de la materia Ingeniería de Software (SIS324), Universidad Mayor Real y Pontificia San Francisco Xavier de Chuquisaca, 2026.
Estudiantes: Avendaño Retamozo Juan Antonio, Dávalos Nuñez Gabriela y Morejon Morales Luz Yumara. Docente: Ing. Ramiro Duran.

## Documentos

Los requisitos son la fuente de verdad del proyecto.

| Archivo | Contenido |
| --- | --- |
| [docs/01-vision.md](docs/01-vision.md) | Alcance, roles, requisitos funcionales (RF) y no funcionales (RNF) |
| [docs/02-casos-de-uso.md](docs/02-casos-de-uso.md) | Casos de uso, estados, reglas de negocio (RN), matriz de permisos y decisiones de implementación |
| [docs/03-historias-y-backlog.md](docs/03-historias-y-backlog.md) | Historias de usuario con criterios de aceptación y backlog por etapas |
| [docs/04-modelos-uml.md](docs/04-modelos-uml.md) | Diagramas de casos de uso, clases, componentes y estados |

## Estructura

```
backend/   API REST: Spring Boot 4.1, Java 25, Maven, PostgreSQL y Flyway
app/       Flutter: la Tienda (lib/main.dart) y el Panel (lib/main_panel.dart) comparten lib/comun/
docs/      Documentos del proyecto
```

La API se divide en cuatro módulos, cada uno en su paquete de `bo.dulcesaromas`: **seguridad**, **ecommerce** (catálogo, inventario y pedidos), **cobros** y **distribucion**. Un módulo solo usa la interfaz pública de otro, nunca sus tablas; Spring Modulith lo comprueba en las pruebas.

## Requisitos

| Herramienta | Versión | Para qué |
| --- | --- | --- |
| JDK | 25 | API. Maven no hace falta: viene el wrapper `mvnw` |
| PostgreSQL | 18 | Base de datos |
| Flutter | 3.47 o superior | Tienda y Panel |
| Android SDK | API 26 o superior | Tienda en Android (RNF11) |
| Python 3 con Pillow | — | Solo para cargar los datos de demostración |

## Puesta en marcha

### 1. Base de datos

Crea el usuario y la base, una sola vez:

```sql
CREATE USER dulces_aromas WITH PASSWORD 'elige-una-contraseña';
CREATE DATABASE dulces_aromas OWNER dulces_aromas;
```

Las tablas las crea Flyway al arrancar la API, con las migraciones de `backend/src/main/resources/db/migration`.

### 2. Configuración

Copia `backend/.env.example` como `backend/.env` y completa los valores. `.env` no se versiona: **las credenciales nunca van en el código**. Las variables de entorno tienen prioridad sobre `.env`, así que en la nube se configuran ahí.

| Variable | Obligatoria | Qué es |
| --- | --- | --- |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Sí | Conexión a PostgreSQL |
| `ADMIN_CORREO`, `ADMIN_CONTRASENA` | La primera vez | Primer administrador. Se crea al arrancar solo si no hay ningún administrador activo |
| `CORS_ORIGENES` | Sí, para el Panel | Direcciones desde las que el navegador puede llamar a la API, separadas por comas. En desarrollo, `http://localhost:8081` |
| `MAIL_HOST`, `MAIL_PUERTO`, `MAIL_USUARIO`, `MAIL_CONTRASENA`, `MAIL_REMITENTE` | No | Correo por SMTP para recuperar contraseñas. Sin `MAIL_HOST` ni `BREVO_CLAVE`, el enlace se escribe en el registro de la API (solo sirve en desarrollo) |
| `BREVO_CLAVE` | En la nube | Clave de la API de Brevo. Si está, el correo sale por Brevo (HTTPS) y no por SMTP, que el plan gratuito de Render bloquea. `MAIL_REMITENTE` debe estar verificado en Brevo |
| `PORT` | No | Puerto de la API; por defecto 8080. Los servicios en la nube lo definen solos |
| `RECUPERACION_URL` | No | Pantalla de la app que recibe el enlace de recuperación |
| `DEMO_CONTRASENA` | No | Contraseña de las cuentas de demostración |

Las contraseñas deben tener al menos 8 caracteres, con una letra y un número.

### 3. API

Desde `backend/`:

```
./mvnw spring-boot:run      # en Windows: mvnw.cmd spring-boot:run
```

Queda en `http://localhost:8080`. El catálogo público se puede probar en `http://localhost:8080/api/catalogo/productos`.

### 4. Tienda y Panel

Desde `app/`:

```
flutter pub get
flutter run -d chrome --web-port 8081                          # Tienda en el navegador
flutter run -d chrome --web-port 8081 -t lib/main_panel.dart   # Panel
flutter run -d emulator-5554 --dart-define=API_URL=http://10.0.2.2:8080   # Tienda en el emulador de Android
```

La dirección de la API se pasa con `--dart-define=API_URL=...`; por defecto es `http://localhost:8080`. Desde el emulador de Android, la computadora se ve como `10.0.2.2`. El Panel usa el puerto 8081 porque la API solo acepta llamadas de navegador desde `CORS_ORIGENES`.

Para generar la Tienda instalable:

```
flutter build apk --dart-define=API_URL=https://tu-api.ejemplo
```

### 5. Datos de demostración (opcional)

Con la API corriendo sobre una base recién creada, desde `backend/`:

```
pip install pillow
python demo/cargar_demo.py
```

Carga 14 productos en 4 categorías, inventario con alertas, dos vendedores, dos repartidores, cuatro clientes y 12 pedidos que recorren todos los estados (entregado, en camino, entrega fallida, pago rechazado, cancelado, pedido asistido…). Todas las cuentas usan la contraseña de `DEMO_CONTRASENA`; los correos de clientes usan el dominio reservado `example.com`. Si la base ya tiene catálogo, el script no carga nada.

Para empezar de cero, detén la API, recrea el esquema y vuelve a arrancarla:

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

## Pruebas

```
cd backend && ./mvnw test   # API: usan H2 en memoria, no requieren PostgreSQL
cd app && flutter test      # Tienda y Panel
```

Cada regla de negocio (RN01–RN17) tiene al menos una prueba automatizada, nombrada por la regla que cubre (RNF17). `ModularidadTests` falla si un módulo de la API usa algo que no le corresponde de otro.

Antes de subir cambios en la app, aplica el formato con `dart format lib test` (ancho 120) y revisa con `flutter analyze`.

## Roles

| Rol | Usa | Qué hace |
| --- | --- | --- |
| Cliente | Tienda | Se registra solo, pide, paga y sigue sus pedidos |
| Administrador | Panel | Acceso total. Es el único con acceso a Seguridad |
| Vendedor | Panel | Atiende pedidos y pagos, registra pedidos asistidos y asigna repartidores. Verifica pagos y marca la preparación solo con ese permiso |
| Repartidor | Panel | Ve y actualiza solo sus propias entregas |

Los roles son fijos; el administrador edita los permisos del Vendedor y del Repartidor. La matriz completa está en la sección 8 de [docs/02-casos-de-uso.md](docs/02-casos-de-uso.md).

## Despliegue en la nube (RNF21)

El sistema se despliega desde este repositorio sin cambiar código, y puede hacerse con planes gratuitos:

1. **Base de datos:** PostgreSQL administrado, por ejemplo Neon. `DB_URL` lleva el formato `jdbc:postgresql://<host>/<base>?sslmode=require`.
2. **API:** la imagen de `backend/Dockerfile`. `render.yaml` la describe para Render (New → Blueprint), que pide las variables secretas al crear el servicio. Las imágenes y comprobantes se guardan en la base, así que no hace falta disco propio. En el plan gratuito, la API se apaga tras 15 minutos sin uso y tarda cerca de un minuto en despertar.
3. **Correo:** una cuenta de Brevo con el remitente verificado; su clave va en `BREVO_CLAVE`.
4. **Panel:** sitio estático con `flutter build web -t lib/main_panel.dart --dart-define=API_URL=https://...`, publicando `app/build/web`. Su dirección va en `CORS_ORIGENES`.
5. Todo el tráfico por HTTPS (RNF06); Render lo incluye.

Para probar la imagen de la API en local, desde `backend/`:

```
docker build -t dulces-aromas-api .
docker run -p 8080:8080 --env-file .env -e DB_URL=jdbc:postgresql://host.docker.internal:5432/dulces_aromas dulces-aromas-api
```

Fuera de alcance de esta versión: pasarelas de pago, notificaciones automáticas, varias sucursales, recetas, roles personalizados, publicación en tiendas de aplicaciones y rastreo en mapa.
