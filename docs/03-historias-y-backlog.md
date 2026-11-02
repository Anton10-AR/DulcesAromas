# Dulces Aromas — Historias de Usuario y Product Backlog v1

Versión 1.0 · 29/10/2026

## 1. Introducción

### 1.1 Propósito

Este documento reúne las 21 historias de usuario de Dulces Aromas (HU01 a HU21) y las ordena en un Product Backlog priorizado. Se deriva de los Casos de Uso v1 y de la Visión v1. No incluye Sprint Backlog: el equipo no tiene fechas oficiales de entrega por sprint, así que el backlog propone un orden de construcción por dependencias que puede seguirse con o sin sprints.

### 1.2 Formato de cada historia

Cada historia dice quién la necesita, qué quiere y para qué: «Como \[rol\], quiero \[acción\], para \[beneficio\]». Se acompaña de criterios de aceptación verificables, el caso de uso y los requisitos que cubre, la prioridad y los puntos.

### 1.3 Prioridad y puntos

- **Prioridad:** Alta si el sistema no funciona sin ella o si otras historias dependen de ella; Media si completa la operación; Baja si es una mejora de comodidad.
- **Puntos:** una sola escala (1, 2, 3, 5, 8, 13) que mide el esfuerzo relativo de construir la historia completa, con pantalla, reglas y pruebas. Son una estimación inicial del analista; el equipo debe corregirla con su propia experiencia.

### 1.4 Definición de terminado

Una historia está terminada cuando cumple todos sus criterios de aceptación, respeta las reglas de negocio y los requisitos no funcionales que le aplican, y ha sido probada por una persona distinta de quien la construyó.

## 2. Historias de usuario

Las historias se agrupan por módulo. HU01 a HU14 conservan la numeración de la versión 0 y HU15 a HU21 son nuevas, una por cada caso de uso nuevo.

### 2.1 Seguridad

| ID | Historia | Criterios de aceptación | CU / RF | Prior. | Pts. |
| --- | --- | --- | --- | --- | --- |
| HU01 | Como usuario, quiero iniciar sesión con mi correo y contraseña, para acceder a las funciones de mi rol | 1) Con credenciales válidas entro a la Tienda (cliente) o al Panel (personal). 2) Con credenciales incorrectas veo un mensaje general sin saber qué dato falló. 3) Una cuenta desactivada no entra. 4) Tras 5 intentos fallidos la cuenta se bloquea 15 minutos | CU01 / RF02, RF27 | Alta | 5 |
| HU02 | Como usuario, quiero cerrar sesión, para proteger mi cuenta | 1) Al cerrar sesión vuelvo al inicio. 2) Con el botón Atrás no veo páginas privadas. 3) En el Panel, la sesión se cierra sola tras 30 minutos sin uso | CU01 / RF03 | Alta | 2 |
| HU03 | Como administrador, quiero crear, editar, activar y desactivar cuentas de personal, para controlar quién usa el Panel | 1) Creo una cuenta con nombre, correo, rol y contraseña inicial. 2) No se repite un correo. 3) Una cuenta desactivada conserva su historial. 4) No puedo desactivar mi propia cuenta ni al último administrador. 5) Cada cambio queda en la auditoría | CU09 / RF22, RF28 | Alta | 5 |
| HU04 | Como administrador, quiero ver los roles base (Administrador, Vendedor, Repartidor) y asignarlos a las cuentas de personal, para que cada persona tenga el rol que le corresponde | 1) Veo los tres roles con su descripción y sus permisos actuales. 2) Asigno o cambio el rol de una cuenta. 3) No existe la opción de crear roles nuevos. 4) Un cliente no puede recibir un rol de personal | CU09 / RF22, RF26 | Alta | 3 |
| HU05 | Como administrador, quiero editar los permisos de cada rol base, para ajustar lo que puede hacer cada uno | 1) Marco o desmarco permisos por módulo y acción. 2) El cambio rige en el siguiente inicio de sesión o antes. 3) El administrador siempre conserva el acceso a Seguridad. 4) El cambio queda en la auditoría | CU09 / RF25, RF28 | Alta | 5 |
| HU06 | Como usuario del sistema, quiero ver y ejecutar solo lo que mi rol permite, para evitar accesos indebidos | 1) El menú muestra solo lo permitido. 2) Una URL directa a una pantalla no permitida devuelve «acceso denegado» y se registra. 3) El Repartidor solo ve sus entregas. 4) El servidor valida cada acción | CU01, CU09 / RF26 | Alta | 5 |
| HU15 | Como visitante, quiero registrarme como cliente, para poder hacer pedidos | 1) Me registro con nombre, correo y contraseña de 8 caracteres o más con letra y número. 2) Si el correo ya existe, me ofrece iniciar sesión o recuperar la contraseña. 3) Al terminar entro con sesión iniciada | CU11 / RF01 | Alta | 3 |
| HU16 | Como usuario, quiero recuperar mi contraseña, para volver a entrar si la olvidé | 1) El cliente recibe un enlace por correo de un solo uso que vence en 60 minutos. 2) El mensaje es igual exista o no el correo. 3) El personal pide al administrador que la restablezca | CU12 / RF24 | Media | 5 |
| HU17 | Como usuario, quiero ver y actualizar mis datos y cambiar mi contraseña, para mantener mi información al día | 1) Edito nombre, teléfono y dirección. 2) Para cambiar la contraseña debo ingresar la actual. 3) La nueva cumple los requisitos | CU13 / RF04 | Baja | 2 |

### 2.2 E-commerce

| ID | Historia | Criterios de aceptación | CU / RF | Prior. | Pts. |
| --- | --- | --- | --- | --- | --- |
| HU07 | Como cliente, quiero consultar el catálogo, buscar y filtrar por categoría, para encontrar lo que quiero comprar | 1) Veo sin iniciar sesión los productos activos con nombre, descripción, precio, imagen y disponibilidad. 2) Busco por nombre y filtro por categoría. 3) Un producto no disponible no se puede agregar al carrito. 4) Respuesta en 2 s o menos | CU04 / RF05–RF08 | Alta | 3 |
| HU08 | Como administrador, quiero gestionar productos, para mantener el catálogo actualizado | 1) Registro y edito productos con imagen de 800 px o más y hasta 10 MB; el sistema la deja cuadrada y de 1200 px como máximo. 2) Defino opciones de personalización con su recargo fijo. 3) Desactivo un producto sin borrarlo si tiene pedidos. 4) El Vendedor solo cambia la disponibilidad | CU02 / RF16–RF18 | Alta | 5 |
| HU18 | Como administrador, quiero crear, editar y desactivar categorías, para organizar el catálogo | 1) Creo una categoría con su nombre. 2) No se repite el nombre. 3) Edito el nombre. 4) Al desactivar una categoría con productos, el sistema dice cuántos afecta y pide confirmación | CU14 / RF19 | Alta | 3 |
| HU09 | Como administrador, quiero registrar entradas, salidas y ajustes de inventario, y como vendedor consultar alertas, para no quedarme sin insumos | 1) Cada movimiento tiene cantidad y motivo. 2) Una salida no supera el stock. 3) Al llegar al mínimo aparece una alerta. 4) El administrador ve el historial de movimientos. 5) El Vendedor solo consulta | CU03 / RF29–RF31 | Media | 5 |
| HU10 | Como cliente, quiero armar un carrito y confirmar mi pedido con recojo o delivery, para recibir mis productos | 1) Agrego productos con sus opciones, cambio cantidades y quito productos. 2) El total suma precio por cantidad, recargos y envío si es delivery. 3) El delivery pide dirección, teléfono y horario de entrega; el mensaje de la torta admite hasta 24 caracteres sin recargo. 4) No se confirma con productos no disponibles. 5) Debo tener sesión y no pierdo el carrito al iniciarla. 6) El pedido nace Pendiente | CU05 / RF09–RF13, RF32, RF35 | Alta | 13 |
| HU14 | Como administrador, quiero consultar pedidos y reportes de ventas, para conocer cómo va el negocio | 1) El personal ve la lista y el detalle de pedidos con su historial de estados. 2) El administrador filtra por fecha, estado y método de pago y ve totales. 3) El Vendedor no ve montos de ventas. 4) Reporte de 12 meses en 5 s o menos | CU10 / RF20, RF23 | Media | 5 |
| HU19 | Como cliente, quiero ver mis pedidos y su estado, para saber en qué punto está mi compra | 1) Veo mis pedidos del más reciente al más antiguo. 2) En cada uno veo el estado del pedido y el del pago. 3) Si el pago fue rechazado veo la observación y puedo volver a pagar. 4) Solo veo los míos | CU15 / RF14, RF15 | Media | 3 |
| HU20 | Como cliente o vendedor, quiero cancelar un pedido antes de que se prepare, para corregir un error o un cambio de planes | 1) Solo cancelo pedidos Pendientes o Confirmados. 2) Debo indicar el motivo. 3) El pedido queda Cancelado y registrado. 4) Si ya estaba pagado, el sistema avisa que el reembolso se gestiona fuera del sistema | CU16 / RF33 | Media | 3 |
| HU21 | Como vendedor, quiero registrar un pedido a nombre de un cliente, para atender ventas por teléfono o mostrador | 1) Busco al cliente o lo registro. 2) Se aplican las mismas reglas de total, disponibilidad y entrega que en HU10. 3) El pedido queda marcado como asistido y con mi nombre. 4) No puedo verificar el pago de mi propio pedido | CU17 / RF34 | Media | 8 |

### 2.3 Cobros

| ID | Historia | Criterios de aceptación | CU / RF | Prior. | Pts. |
| --- | --- | --- | --- | --- | --- |
| HU11 | Como cliente, quiero registrar mi pago con su comprobante, para que el negocio confirme mi pedido | 1) Elijo QR, transferencia o depósito. 2) Ingreso la referencia y adjunto un comprobante JPG, PNG o PDF de hasta 5 MB. 3) El pago pasa a En verificación. 4) Si fue rechazado, puedo registrar otro comprobante | CU06 / RF36 | Alta | 5 |
| HU12 | Como administrador o vendedor con permiso, quiero aprobar o rechazar un pago, para confirmar solo pedidos realmente pagados | 1) Veo los pagos En verificación con su comprobante. 2) Al aprobar, el pago queda Pagado y el pedido Confirmado. 3) Rechazar exige una observación. 4) Dos personas no pueden decidir el mismo pago. 5) Queda el historial de quien verificó y cuándo | CU07 / RF37–RF39 | Alta | 5 |

### 2.4 Distribución

| ID | Historia | Criterios de aceptación | CU / RF | Prior. | Pts. |
| --- | --- | --- | --- | --- | --- |
| HU13 | Como personal y repartidor, quiero preparar, asignar y entregar los pedidos, para que lleguen al cliente | 1) Solo se prepara un pedido con pago aprobado. 2) El personal marca En preparación y Listo según su permiso. 3) Se asigna un repartidor a cada delivery. 4) El Repartidor ve solo sus entregas con dirección y teléfono y marca En camino y Entregado. 5) Una entrega fallida exige observación y devuelve el pedido a Listo. 6) El recojo lo cierra el Vendedor. 7) Cada cambio queda en el historial | CU08 / RF21, RF40–RF43 | Alta | 8 |

## 3. Product Backlog

### 3.1 Criterio de orden

El backlog sigue las dependencias del sistema: sin cuentas no hay pedidos, sin productos no hay pedidos, y sin pago aprobado no hay preparación ni entrega. Las historias se agrupan en cinco etapas. Cada etapa deja algo usable, y el equipo puede recorrerlas sin fechas de sprint.

### 3.2 Backlog priorizado

| Orden | Etapa | ID | Historia | Depende de | Prior. | Pts. |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | A. Cuentas y acceso | HU01 | Iniciar sesión | — | Alta | 5 |
| 2 | A | HU02 | Cerrar sesión | HU01 | Alta | 2 |
| 3 | A | HU03 | Gestionar cuentas de personal | HU01 | Alta | 5 |
| 4 | A | HU04 | Ver y asignar roles base | HU03 | Alta | 3 |
| 5 | A | HU05 | Editar permisos de los roles base | HU04 | Alta | 5 |
| 6 | A | HU06 | Controlar el acceso por rol | HU05 | Alta | 5 |
| 7 | A | HU15 | Registrarse como cliente | HU01 | Alta | 3 |
| 8 | B. Catálogo | HU08 | Gestionar productos | HU06 | Alta | 5 |
| 8b | B | HU18 | Gestionar categorías | HU06 | Alta | 3 |
| 9 | B | HU07 | Consultar catálogo | HU08 | Alta | 3 |
| 10 | C. Pedido y cobro | HU10 | Hacer un pedido | HU07, HU15 | Alta | 13 |
| 11 | C | HU11 | Registrar pago | HU10 | Alta | 5 |
| 12 | C | HU12 | Verificar pago | HU11, HU06 | Alta | 5 |
| 13 | D. Entrega y seguimiento | HU13 | Preparar, asignar y entregar | HU12 | Alta | 8 |
| 14 | D | HU19 | Consultar mis pedidos | HU10 | Media | 3 |
| 15 | E. Complementos | HU09 | Gestionar inventario | HU06 | Media | 5 |
| 16 | E | HU20 | Cancelar pedido | HU10 | Media | 3 |
| 17 | E | HU21 | Registrar pedido asistido | HU10, HU06 | Media | 8 |
| 18 | E | HU14 | Consultar pedidos y reportes | HU10 | Media | 5 |
| 19 | E | HU16 | Recuperar contraseña | HU01 | Media | 5 |
| 20 | E | HU17 | Gestionar perfil | HU01 | Baja | 2 |

### 3.3 Resumen por etapa

| Etapa | Qué queda funcionando | Historias | Puntos |
| --- | --- | --- | --- |
| A. Cuentas y acceso | Personal y clientes entran al sistema, cada uno con lo que su rol permite | HU01–HU06, HU15 | 28 |
| B. Catálogo | El administrador carga productos y el cliente los consulta | HU08, HU18, HU07 | 11 |
| C. Pedido y cobro | El cliente pide, paga y el personal verifica el pago | HU10–HU12 | 23 |
| D. Entrega y seguimiento | El pedido se prepara, se entrega y el cliente ve su estado | HU13, HU19 | 11 |
| E. Complementos | Inventario, cancelación, pedido asistido, reportes, recuperación de contraseña y perfil | HU09, HU14, HU16, HU17, HU20, HU21 | 28 |
| Total |  | 21 historias | 101 |

Si el tiempo no alcanza, las etapas A a D forman el núcleo: permiten comprar, pagar y entregar. La etapa E puede recortarse sin romper el flujo principal, aunque HU21 (pedido asistido) y HU16 (recuperar contraseña) son las que más se echarían de menos.

## 4. Cambios respecto a la versión 0 y trazabilidad

### 4.1 Cambios

| Tema | Versión 0 | Versión 1 |
| --- | --- | --- |
| Historias | HU01 a HU14, sin historias de cliente salvo catálogo, pedido y pago | HU01 a HU21; se agregan registro, recuperación, perfil, mis pedidos, cancelación y pedido asistido |
| HU01 | Correo o nombre de usuario | Solo correo y contraseña |
| HU04 y HU05 | Crear y administrar roles | HU04 ver y asignar roles base; HU05 editar sus permisos. No se crean roles nuevos |
| HU07 y HU08 | Prioridad Media | Prioridad Alta, porque no hay pedido sin productos |
| HU08, HU09 y HU13 | Actor «empleado autorizado» o «responsable de distribución» | Actores concretos: Administrador, Vendedor y Repartidor |
| Criterios de aceptación | Solo para las historias del Sprint 1 | Para las 21 historias, con valores medibles |
| Puntos | Dos columnas con la misma etiqueta pero distinta escala (27 contra 52) | Una sola escala de puntos por historia; las tareas no se estiman |
| Sprints | Sprint 1 con 14 tareas | Sin sprints; etapas A a E por dependencia |

### 4.2 Trazabilidad

| Historia | Caso de uso | Requisitos |
| --- | --- | --- |
| HU01, HU02 | CU01 | RF02, RF03, RF27 |
| HU03 | CU09 | RF22, RF28 |
| HU04, HU05 | CU09 | RF22, RF25, RF28 |
| HU06 | CU01, CU09 | RF26 |
| HU07 | CU04 | RF05–RF08 |
| HU08 | CU02 | RF16–RF18 |
| HU18 | CU14 | RF19 |
| HU09 | CU03 | RF29–RF31 |
| HU10 | CU05 | RF09–RF13, RF32, RF35 |
| HU11 | CU06 | RF36 |
| HU12 | CU07 | RF37–RF39 |
| HU13 | CU08 | RF21, RF40–RF43 |
| HU14 | CU10 | RF20, RF23 |
| HU15 | CU11 | RF01 |
| HU16 | CU12 | RF24 |
| HU17 | CU13 | RF04 |
| HU19 | CU15 | RF14, RF15 |
| HU20 | CU16 | RF33 |
| HU21 | CU17 | RF34 |

Todos los casos de uso y los 43 requisitos funcionales tienen al menos una historia.

### 4.3 Puntos por confirmar

La numeración y los títulos de HU01 a HU14 coinciden con el PDF de historias v0. Los puntos son estimaciones iniciales y el equipo debe confirmarlos; en la versión 0, por ejemplo, HU10 valía 8 puntos y HU13 valía 5, y aquí suben a 13 y 8 porque la historia incluye ahora carrito, personalización, envío y disponibilidad.
