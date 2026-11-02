# Dulces Aromas — guía para Claude Code

Sistema de comercio electrónico para una pastelería de Sucre, Bolivia. Es el proyecto de la materia de Ingeniería de Software (USFX, 2026). El equipo son tres estudiantes: Avendaño Retamozo Juan Antonio, Dávalos Nuñez Gabriela y Morejon Morales Luz Yumara. Docente: Ing. Ramiro Duran.

## Fuente de verdad

Los requisitos están en `docs/`. Si el código y un documento no coinciden, **pregunta antes de cambiar cualquiera de los dos**.

| Archivo | Contenido |
| --- | --- |
| `docs/01-vision.md` | Alcance, roles, RF01–RF43, RNF01–RNF21, restricciones, paleta |
| `docs/02-casos-de-uso.md` | CU01–CU17, estados y transiciones, reglas RN01–RN17, matriz de permisos |
| `docs/03-historias-y-backlog.md` | HU01–HU21 con criterios de aceptación, backlog por etapas |
| `docs/04-modelos-uml.md` | Casos de uso, clases, componentes y estados (Mermaid) |

Cuando implementes una historia, lee sus criterios de aceptación y el caso de uso que cita. Cita el ID (HU, CU, RN) en el mensaje del commit.

## Arquitectura

- **Tienda (cliente):** aplicación móvil en **Flutter y Dart**. Es obligatorio por la materia.
- **Panel (personal):** web, en Flutter web, para usar un solo lenguaje en las dos interfaces.
- **API REST:** la consumen la Tienda y el Panel. Spring Boot 4.1 con Java 25 y Maven.
- **Base de datos:** PostgreSQL. El esquema se versiona con migraciones de Flyway en `backend/src/main/resources/db/migration` (`V1__descripcion.sql`, `V2__...`). Hibernate solo valida el esquema; nunca lo crea.

La API se organiza en cuatro módulos, cada uno en su propio paquete de `bo.dulcesaromas` (`seguridad`, `ecommerce`, `cobros`, `distribucion`). Un módulo nunca lee las tablas de otro; solo usa su interfaz. Spring Modulith hace cumplir esto: lo que está en el paquete raíz del módulo es su API pública, los subpaquetes son internos, y las dependencias permitidas se declaran en el `package-info.java` de cada módulo. `ModularidadTests` falla si un módulo usa otro que no tiene permitido o una clase interna de otro módulo.

| Módulo | Provee | Requiere |
| --- | --- | --- |
| Seguridad | IAutenticacion | — |
| E-commerce (catálogo, inventario, pedidos) | IProductos, IStock, IPedidos | IAutenticacion |
| Cobros | IPagos | IAutenticacion, IPedidos |
| Distribución | IEntregas | IAutenticacion, IPedidos |

Cobros llama a IPedidos para pasar el pedido a Confirmado al aprobar un pago. No al revés. Cuando E-commerce necesita que otro módulo reaccione, publica un evento (por ejemplo `PedidoRegistrado`, que hace que Cobros abra el pago Pendiente de pago) y el otro módulo lo escucha con `@EventListener`, dentro de la misma transacción.

Estructura de carpetas:

```
backend/   API REST (Spring Boot)
app/       Flutter: Tienda (móvil) y Panel (web)
docs/      Documentos del proyecto
```

## Comandos

Todos los comandos del backend se ejecutan desde `backend/`. Usa el wrapper (`./mvnw` o `mvnw.cmd`); no hace falta instalar Maven.

```
./mvnw test              # pruebas (usan H2 en memoria en modo PostgreSQL; no requieren PostgreSQL)
./mvnw spring-boot:run   # levanta la API en http://localhost:8080
```

**Base de datos local (una sola vez):** crea el usuario y la base en PostgreSQL y copia `backend/.env.example` como `backend/.env` con la contraseña. `.env` no se versiona.

```sql
CREATE USER dulces_aromas WITH PASSWORD '...';
CREATE DATABASE dulces_aromas OWNER dulces_aromas;
```

Las variables son `DB_URL`, `DB_USER` y `DB_PASSWORD`. También se pueden definir como variables de entorno, que tienen prioridad sobre `.env`. Para crear el primer administrador, define también `ADMIN_CORREO` y `ADMIN_CONTRASENA`: se crea al arrancar solo si no hay ningún administrador activo. Para que la recuperación de contraseña envíe correos, define `MAIL_HOST`, `MAIL_PUERTO`, `MAIL_USUARIO`, `MAIL_CONTRASENA` y `MAIL_REMITENTE`; sin `MAIL_HOST`, el correo se escribe en el registro de la API, lo que solo sirve en desarrollo. `RECUPERACION_URL` es la pantalla de la app que recibe el enlace.

**App Flutter** (desde `app/`). Un solo proyecto con dos puntos de entrada que comparten `lib/comun/`: la Tienda (`lib/main.dart`, código en `lib/tienda/`) y el Panel (`lib/main_panel.dart`, código en `lib/panel/`).

```
flutter test                                                  # pruebas
dart format lib test                                           # formato (ancho 120, en analysis_options.yaml)
flutter run -d chrome --web-port 8081                         # Tienda en el navegador
flutter run -d chrome --web-port 8081 -t lib/main_panel.dart  # Panel
flutter build apk --dart-define=API_URL=https://...           # Tienda para Android (requiere Android SDK)
```

La dirección de la API se pasa con `--dart-define=API_URL=...` (por defecto `http://localhost:8080`; desde el emulador de Android es `http://10.0.2.2:8080`). El Panel corre en el puerto 8081 porque la API solo acepta llamadas de navegador desde `CORS_ORIGENES`.

Convenciones de la app: los montos llegan del servidor y se convierten con `decimalDe` (paquete `decimal`); la app nunca calcula totales. Los errores de la API llegan como `ErrorApi` con `campos`, que `Campo` muestra junto a cada campo. Las fechas se muestran con `fecha`/`fechaHora` en hora de La Paz. Los colores salen de `Colores` en `lib/comun/tema.dart`; `test/tema_test.dart` comprueba el contraste de 4.5:1. En `setState`, usa un bloque (`setState(() { _x = ...; })`) cuando asignas un `Future`: la forma con flecha devuelve el `Future` y Flutter lo rechaza.

## Dominio

### Roles

Hay cuatro roles **fijos**. El administrador edita sus permisos, pero **no se crean roles nuevos**.

- **Cliente:** usa la Tienda. Se registra solo y nunca puede tener un rol de personal.
- **Administrador:** acceso total. Siempre conserva el acceso a Seguridad.
- **Vendedor:** atiende pedidos y pagos, registra pedidos asistidos y asigna repartidores. Marca la preparación y verifica pagos solo si tiene ese permiso.
- **Repartidor:** ve y actualiza solo sus propias entregas.

La matriz completa de permisos está en la sección 8 de `docs/02-casos-de-uso.md`.

### Estados

El pedido y el pago tienen **máquinas de estado separadas**.

- **Pedido:** Pendiente → Confirmado → En preparación → Listo → En camino → Entregado. También puede pasar a Cancelado.
- **Pago:** Pendiente de pago → En verificación → Pagado o Rechazado. Un pago Rechazado vuelve a Pendiente de pago.
- El único punto de contacto: **aprobar el pago pasa el pedido de Pendiente a Confirmado**.
- **Recojo:** el pedido va de Listo a Entregado sin pasar por En camino.
- **Entrega fallida:** el pedido vuelve de En camino a Listo, con una observación.
- Implementa solo las transiciones de la tabla 2.2 de `docs/02-casos-de-uso.md`. Cualquier otra transición se rechaza en el servidor.

### Reglas de negocio

Cada una necesita al menos una prueba automatizada (RNF17).

- **RN01:** un correo identifica una sola cuenta.
- **RN02:** una cuenta desactivada no inicia sesión y conserva su historial.
- **RN03:** siempre queda al menos un administrador activo, y nadie desactiva su propia cuenta.
- **RN04:** el administrador siempre conserva el acceso a Seguridad.
- **RN05:** un pedido pasa a Confirmado solo con el pago Pagado.
- **RN06:** quien registró un pedido asistido no verifica el pago de ese pedido.
- **RN07:** rechazar un pago exige observación, y el cobro se reabre.
- **RN08:** no se confirma un pedido con productos no disponibles.
- **RN09:** una salida de inventario no supera el stock, y al llegar al mínimo se genera una alerta.
- **RN10:** un producto con pedidos se desactiva; no se elimina.
- **RN11:** total = Σ((precio + recargos de las opciones) × cantidad) + tarifa de envío si es delivery. El recargo se cobra por unidad.
- **RN12:** el delivery exige dirección, teléfono y horario de entrega; el recojo exige teléfono y horario, pero no dirección.
- **RN13:** una entrega fallida devuelve el pedido a Listo, con observación.
- **RN14:** un pedido solo se cancela antes de En preparación. El reembolso se hace fuera del sistema.
- **RN15:** el catálogo se ve sin sesión; para pedir hace falta cuenta.
- **RN16:** un cliente no obtiene un rol de personal.
- **RN17:** el mensaje sobre la torta tiene hasta 24 caracteres y no tiene recargo.

### Decisiones ya tomadas (no reabrir sin preguntar)

Las decisiones de implementación, todas confirmadas por el equipo, están en la sección 10 de `docs/02-casos-de-uso.md`.

- **D1:** el pago se aprueba antes de preparar el pedido.
- **D2:** la disponibilidad de un producto es un indicador manual; no sale del inventario de insumos.
- **D3:** el Vendedor con permiso marca la preparación. No existe un rol Producción.
- **D4:** el delivery tiene tarifa fija, guardada en Configuracion.
- **D5:** el cliente recupera su contraseña por un enlace de correo (un solo uso, 60 min); al personal se la restablece el administrador.
- **D6:** no hay roles personalizados.
- **D7:** el pedido asistido (por teléfono o mostrador) es un canal oficial.
- **D8:** se puede cancelar hasta antes de preparar.
- **D9:** cada opción de personalización tiene un recargo fijo que define el negocio.
- **D10:** catálogo e inventario están dentro del módulo E-commerce.
- **D11:** el rol se llama «Vendedor», no «Empleado».

## Convenciones

- **Nombres del dominio en español**, como en el modelo de clases: `Pedido`, `LineaPedido`, `OpcionElegida`, `Pago`, `Verificacion`, `Entrega`, `HistorialEstado`, `ItemInventario`.
- **Dinero:** usa tipos decimales, nunca `float` o `double`. Se muestra en bolivianos con 2 decimales («Bs 120.00»).
- **Fechas y hora:** formato dd/mm/aaaa, zona horaria America/La_Paz.
- **Copias en el pedido:** las opciones elegidas guardan una copia del nombre y del recargo, para que un cambio en el producto no altere pedidos anteriores.
- **Transacciones:** confirmar un pedido es una sola transacción. Dos usuarios no pueden decidir el mismo pago a la vez (RNF14, RNF15).

### Convenciones de la API

- **Sesión:** `POST /api/auth/login` devuelve un token opaco; la app lo manda en `Authorization: Bearer <token>`. La sesión vive en la tabla `sesion`, así que cerrarla, desactivar la cuenta o cambiar permisos rige en la siguiente solicitud.
- **Autorización:** cada controlador nuevo declara su permiso con `@PreAuthorize("hasAuthority('PERMISO')")`, usando los nombres del enum `seguridad.Permiso`. Las rutas de la Tienda usan `hasAuthority('CLIENTE')`. Las rutas públicas (como el catálogo, RN15) se abren en `ConfiguracionSeguridad`.
- **Usuario actual:** los otros módulos lo obtienen con `IAutenticacion.usuarioActual()`; nunca leen las tablas de Seguridad.
- **Datos de entrada:** usa `Boolean`, `Integer`, etc. con `@NotNull` en los records de las solicitudes, no tipos primitivos. Jackson 3 rechaza un primitivo ausente con un error genérico y el usuario no sabe qué campo falta.
- **Errores de negocio:** lanza `compartido.ReglaNegocioException` con estado HTTP, un código en MAYÚSCULAS (la interfaz reacciona a él) y un mensaje para el usuario. Los errores de validación devuelven `campos` con el mensaje de cada campo (RNF01).
- **Pruebas:** las de integración heredan de `PruebaIntegracion` (base H2 limpia en cada prueba y un `RelojDePrueba` para adelantar el tiempo). Nombra la prueba con la regla que cubre, por ejemplo `rn03SiempreQuedaAlMenosUnAdministradorActivo`.

## Seguridad (no negociable)

- **Contraseñas:** se guardan con hash y sal (por ejemplo BCrypt). Mínimo 8 caracteres, con al menos una letra y un número. El prototipo HTML las guardaba en texto plano; no copies eso.
- **Bloqueo:** 5 intentos fallidos seguidos bloquean la cuenta 15 minutos.
- **Sesión:** en el Panel se cierra tras 30 minutos de inactividad. Todo el tráfico va por HTTPS.
- **Autorización:** cada endpoint valida el rol en el servidor; ocultar botones en la interfaz no basta. Un acceso no permitido devuelve «acceso denegado» y se registra.
- **Auditoría:** se registran los cambios de usuarios, permisos y verificaciones de pago.
- **Privacidad:** el Repartidor solo ve los datos de sus propias entregas.
- **Credenciales:** nunca van en el código. Usa variables de entorno.

## Interfaz

- **Tipografías:** títulos en Fraunces y texto en Work Sans.
- **Tema claro:** fondo #FBF4EC, texto #3A2A23, café #4A342C, rosa #EFC3CE, #E19DB0 y #B96C86.
- **Tema oscuro:** fondo #1E1714, texto #EDE0D6. Sigue la preferencia del dispositivo.
- **Accesibilidad:** el texto tiene contraste de 4.5:1 o más (RNF20). En el tema claro, oscurece el rosa #B96C86 y el café #8A6C5C antes de usarlos en texto pequeño.

## Orden de trabajo

Sigue el backlog por etapas. **A a D** forman el núcleo (73 de 101 puntos); **E** se hace solo si sobra tiempo.

1. **A. Cuentas y acceso:** HU01–HU06, HU15.
2. **B. Catálogo:** HU08, HU18, HU07.
3. **C. Pedido y cobro:** HU10, HU11, HU12.
4. **D. Entrega y seguimiento:** HU13, HU19.
5. **E. Complementos:** HU09, HU20, HU21, HU14, HU16, HU17.

Una historia está terminada cuando:

- cumple todos sus criterios de aceptación,
- respeta las RN y los RNF que le aplican,
- tiene pruebas que pasan,
- y la revisó una persona distinta de quien la hizo.

## Fuera de alcance

No implementes nada de esto sin que el equipo lo pida:

- Pasarelas de pago o QR dinámico. El pago se verifica a mano.
- Notificaciones automáticas.
- Varias sucursales.
- Recetas y descuento automático de insumos.
- Roles personalizados.
- Publicación de la app en tiendas.
- Rastreo de entregas en mapa.
