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
| [docs/04-modelos-uml.md](docs/04-modelos-uml.md) | Diagramas de casos de uso, clases, componentes, despliegue y estados |

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

La APK se firma con la clave de `app/android/key.properties` y `app/android/tienda.jks`, que **no se versionan**: guárdenlas en un lugar seguro, porque sin esa clave los enlaces de recuperación dejan de abrir la app y no se puede actualizar la app ya instalada. Sin `key.properties`, se firma con la clave de depuración.

### 5. Datos de demostración (opcional)

Con la API corriendo sobre una base recién creada, desde `backend/`:

```
pip install pillow
python demo/cargar_demo.py
```

Carga 14 productos en 4 categorías, inventario con alertas, dos vendedores, dos repartidores, una repostera, cuatro clientes y 12 pedidos que recorren todos los estados (entregado, en camino, entrega fallida, pago rechazado, cancelado, pedido asistido…). Todas las cuentas usan la contraseña de `DEMO_CONTRASENA`; los correos de clientes usan el dominio reservado `example.com`. Si la base ya tiene catálogo, el script no carga nada.

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
| Repostero | Panel | Prepara los pedidos desde Cocina: marca En preparación y Listo. No ve datos personales ni montos del cliente |

Los roles son fijos; el administrador edita los permisos del Vendedor, del Repartidor y del Repostero. La matriz completa está en la sección 8 de [docs/02-casos-de-uso.md](docs/02-casos-de-uso.md).

## Despliegue en la nube (RNF21)

El sistema está publicado:

| Qué | Dirección |
| --- | --- |
| Tienda web | https://dulces-aromas.onrender.com |
| Panel | https://dulces-aromas.onrender.com/panel/ |
| API | https://dulces-aromas-api.onrender.com |

En el plan gratuito, la API se apaga tras 15 minutos sin uso y tarda cerca de un minuto en despertar. Antes de una presentación, abran la Tienda unos minutos antes.

### Tecnologías de la nube

| Tecnología | Para qué | Plan gratuito |
| --- | --- | --- |
| **Render** (plataforma de hospedaje) | Ejecuta la API y publica la Tienda web y el Panel, con dominio y HTTPS | 512 MB de RAM; la API se apaga sin uso |
| **Neon** (PostgreSQL administrado) | Base de datos, en AWS us-east-2 (Ohio), la misma región que la API | 0.5 GB |
| **Brevo** (correo transaccional) | Envía el correo de recuperación por su API HTTPS; Render bloquea SMTP | 300 correos al día |
| **GitHub Actions** (CI/CD) | En cada cambio en `app/`, ejecuta las pruebas de Flutter, compila el sitio y lo deja en la rama `web` | Incluido |
| **Docker** (`backend/Dockerfile`) | Empaqueta la API con Java 25: una etapa compila con Maven y otra solo ejecuta el `.jar`, con un usuario sin privilegios y la memoria de la JVM limitada | — |
| **Render Blueprint** (`render.yaml`) | Describe como código los dos servicios, la región, la verificación de salud y las variables | — |
| **Android App Links** (`assetlinks.json`) | El enlace de recuperación abre la Tienda instalada, o la Tienda web si no lo está | — |

```
 Celular (APK) ──┐
                 ├──HTTPS──▶  API en Render (Docker, Ohio) ──▶ PostgreSQL en Neon (Ohio)
 Navegador ──────┘                      │
   ▲                                    └──HTTPS──▶ Brevo ──▶ correo de recuperación
   │
 Tienda (/) y Panel (/panel/) en Render  ◀── rama web ◀── GitHub Actions (pruebas y compilación)
```

El diagrama de despliegue UML está en la sección 4.1 de [docs/04-modelos-uml.md](docs/04-modelos-uml.md).

### Cómo desplegar desde cero

El sistema se despliega desde este repositorio sin cambiar código. El orden importa, porque cada paso usa datos del anterior:

1. **Neon:** creen un proyecto en la región AWS US East 2 (Ohio). En **Connect**, desactiven *Connection pooling* (Flyway necesita la conexión directa) y separen la cadena en tres datos: `DB_URL` = `jdbc:postgresql://<host>/<base>?sslmode=require` (sin usuario, sin contraseña y sin `&channel_binding=require`, que el controlador de Java no reconoce), `DB_USER` y `DB_PASSWORD`. Si eligen otra región, cambien `region` en `render.yaml`.
2. **Brevo:** verifiquen el correo remitente (*Senders*) y creen una API key (`xkeysib-…`, no la SMTP key). Van en `BREVO_CLAVE` y `MAIL_REMITENTE`. Si Brevo bloquea las IP desconocidas, desactiven ese bloqueo: Render cambia de IP.
3. **GitHub:** creen la variable del repositorio `API_URL` (Settings → Secrets and variables → Actions → Variables) con la dirección de la API, `https://dulces-aromas-api.onrender.com`, y ejecuten el workflow **Sitio web** (Actions → Run workflow). Crea la rama `web`, que Render necesita. Desde ahí, cada cambio en `app/` vuelve a publicar el sitio.
4. **Render:** New → Blueprint → este repositorio. Render lee `render.yaml`, crea la API y el sitio estático y pide las variables secretas:

   | Variable | Valor |
   | --- | --- |
   | `DB_URL`, `DB_USER`, `DB_PASSWORD` | Los de Neon |
   | `BREVO_CLAVE`, `MAIL_REMITENTE` | Los de Brevo |
   | `ADMIN_CORREO`, `ADMIN_CONTRASENA` | El primer administrador; se crea en el primer arranque |
   | `CORS_ORIGENES` | `https://dulces-aromas.onrender.com` |
   | `RECUPERACION_URL` | `https://dulces-aromas.onrender.com/recuperar` |

   La API tarda entre 5 y 10 minutos en construirse. En el primer arranque, Flyway crea las tablas en Neon.

Las credenciales solo van en los formularios de Render; nunca en el repositorio. Las imágenes y comprobantes se guardan en la base, así que no hace falta disco propio. Todo el tráfico va por HTTPS (RNF06), que Render incluye.

**Datos de demostración en la nube (opcional):** con la base recién creada, desde `backend/`, en una terminal propia:

```powershell
$env:ADMIN_CORREO = Read-Host "Correo del administrador de Render"
$env:ADMIN_CONTRASENA = Read-Host "Contraseña del administrador de Render"
$env:DEMO_CONTRASENA = Read-Host "Contraseña para las cuentas de demostración"
python demo/cargar_demo.py --api https://dulces-aromas-api.onrender.com
```

Las variables del entorno tienen prioridad sobre `backend/.env` y desaparecen al cerrar la terminal. Usen para la nube una `DEMO_CONTRASENA` distinta de la local, porque el sitio es público.

Los productos que tienen foto en `app/assets/postres/` la usan; los demás llevan una ilustración con su inicial. Si la base ya tiene el catálogo, `--fotos` solo les pone las fotos y no toca nada más:

```powershell
python demo/cargar_demo.py --fotos --api https://dulces-aromas-api.onrender.com
```

Para sumar a una base que ya tiene catálogo las categorías y los productos del script que todavía no existen (los compara por nombre), sin tocar los demás productos, los pedidos ni las cuentas, usen `--ampliar`. Los productos nuevos se crean con su foto:

```powershell
python demo/cargar_demo.py --ampliar --api https://dulces-aromas-api.onrender.com
```

### Respaldo diario (RNF14)

El workflow **Respaldo diario** (`.github/workflows/respaldo.yml`) corre cada noche a las 03:00 de La Paz:

1. Copia la base de Neon con `pg_dump`.
2. Restaura la copia en un PostgreSQL vacío y comprueba que cada tabla tenga las mismas filas que en Neon. Así cada copia queda con una restauración probada.
3. Cifra la copia con AES-256 y la guarda 90 días como artefacto de la ejecución (Actions → Respaldo diario → una ejecución → Artifacts). El repositorio es público: sin la clave, la copia no se puede leer.

Si algo falla, la ejecución sale en rojo y GitHub avisa por correo al dueño del repositorio. Necesita dos secrets (Settings → Secrets and variables → Actions → **Secrets**):

| Secret | Valor |
| --- | --- |
| `RESPALDO_DB_URL` | La cadena de conexión directa de Neon, tal cual la muestra **Connect** (`postgresql://…`, sin `-pooler`) |
| `RESPALDO_CLAVE` | Una frase larga para cifrar. Guárdenla también fuera de GitHub: sin ella no se puede restaurar ninguna copia |

**Restaurar una copia** (con Git Bash, que trae `gpg`, y las herramientas de PostgreSQL 18):

```bash
unzip respaldo-AAAA-MM-DD.zip                     # el artefacto descargado de GitHub
gpg --output respaldo.dump --decrypt dulces-aromas-AAAA-MM-DD.dump.gpg   # pide RESPALDO_CLAVE
pg_restore --clean --if-exists --no-owner --no-acl -d "postgresql://USUARIO@HOST/BASE" respaldo.dump
```

`--clean` borra y vuelve a crear las tablas de la base de destino. Detengan antes la API que usa esa base. Para revisar una copia sin tocar nada, restáurenla en una base local nueva (`createdb prueba_respaldo`).

### API despierta en horario de atención (RNF02, RNF13)

En el plan gratuito, Render duerme la API tras 15 minutos sin uso, y el primer cliente espera cerca de un minuto. El workflow **Despertar la API** (`.github/workflows/despertar.yml`) consulta el catálogo cada 5 minutos en el horario de atención: lunes a sábado de 9:00 a 20:00 y domingo de 10:00 a 14:00, hora de La Paz. Empieza 10 minutos antes de abrir y usa la variable `API_URL`. Fuera de ese horario, la API se duerme y se despierta sola con la siguiente visita.

GitHub puede retrasar unos minutos las tareas programadas, y las desactiva en un repositorio público tras 60 días sin commits. Si eso pasa, se reactivan desde Actions.

### Enlace de recuperación y App Links

El correo de recuperación lleva un enlace `https://dulces-aromas.onrender.com/recuperar?token=…`:

- Si la Tienda está instalada en Android, el enlace la abre directo en «Contraseña nueva» (App Links).
- Si no, abre la misma pantalla en la Tienda web.

Android confía en el dominio porque el sitio publica `/.well-known/assetlinks.json` (en `app/web/.well-known/`) con la huella SHA-256 de la clave que firma la APK. Si Render les asigna otro dominio, cámbienlo en `dominioWeb` de `app/android/gradle.properties`, en `CORS_ORIGENES` y en `RECUPERACION_URL`. Si cambian la clave, actualicen la huella en `assetlinks.json`.

Para probar la imagen de la API en local, desde `backend/`:

```
docker build -t dulces-aromas-api .
docker run -p 8080:8080 --env-file .env -e DB_URL=jdbc:postgresql://host.docker.internal:5432/dulces_aromas dulces-aromas-api
```

Fuera de alcance de esta versión: pasarelas de pago, notificaciones automáticas, varias sucursales, recetas, roles personalizados, publicación en tiendas de aplicaciones y rastreo en mapa.
