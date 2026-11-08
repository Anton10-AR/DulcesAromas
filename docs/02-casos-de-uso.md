# Dulces Aromas — Detalle de Casos de Uso v1

Versión 1.3 · 08/11/2026

## 1. Introducción y actores

### 1.1 Propósito

Este documento detalla los 17 casos de uso del sistema Dulces Aromas, versión 1.0. Deriva de la Visión del Proyecto v1 y reemplaza al documento de Casos de Uso v0 (CU01 a CU10), que se ajusta y amplía con siete casos nuevos (CU11 a CU17). Aplica las 11 decisiones tomadas por el equipo (D1 a D11).

### 1.2 Convenciones

Cada caso de uso usa la misma plantilla que el documento v0: ID, versión, nombre, actor principal, fuente, prioridad, objetivo, descripción, precondición, postcondición, secuencia normal, secuencia opcional y atributos (rendimiento, frecuencia, importancia, urgencia y observaciones). La fuente indica los requisitos funcionales (RF) de la Visión v1 que el caso cubre. Las secuencias opcionales se numeran como alternativas y citan las reglas de negocio (RN) de la sección 2. Los valores de rendimiento remiten a los RNF de la Visión.

### 1.3 Actores

| Actor | Descripción | Interfaz |
| --- | --- | --- |
| Cliente | Compra desde la Tienda: consulta, pide, paga y sigue sus pedidos. Se registra él mismo | Tienda |
| Administrador | Acceso total. Gestiona usuarios, permisos, catálogo, inventario, pagos y reportes. Lo crea otro administrador | Panel |
| Vendedor | Atiende pedidos y pagos, registra pedidos asistidos y asigna repartidores. Marca la preparación y verifica pagos solo si tiene el permiso. Lo crea el administrador | Panel |
| Repartidor | Ve y actualiza solo sus entregas. Lo crea el administrador | Panel |
| Sistema | Ejecuta pasos automáticos, como cambiar un estado o registrar la auditoría | — |

Las funciones que el documento v0 llamaba «Encargado de inventario», «Encargado de cobranza» y «Encargado de distribución» son ahora permisos de los roles fijos (decisión D6). El rol «Empleado» de otros documentos se llama «Vendedor» (D11).

## 2. Modelo de estados y reglas de negocio

### 2.1 Estados

El pedido y el pago tienen cada uno su propio estado. El pedido pasa a Confirmado solo cuando su pago pasa a Pagado (D1). En el recojo en tienda, el pedido pasa de Listo a Entregado sin pasar por En camino.

| Elemento | Estados |
| --- | --- |
| Pedido | Pendiente, Confirmado, En preparación, Listo, En camino, Entregado, Cancelado |
| Pago | Pendiente de pago, En verificación, Pagado, Rechazado |

### 2.2 Transiciones permitidas

| Estado actual | Evento | Pasa a | Quién | CU |
| --- | --- | --- | --- | --- |
| Pedido: Pendiente | El pago pasa a Pagado | Confirmado | Sistema, tras CU07 | CU07 |
| Pedido: Pendiente o Confirmado | Se cancela | Cancelado | Cliente o personal | CU16 |
| Pedido: Confirmado | Comienza la preparación | En preparación | Administrador, o Vendedor con permiso | CU08 |
| Pedido: En preparación | Termina la preparación | Listo | Administrador, o Vendedor con permiso | CU08 |
| Pedido: Listo | Sale con repartidor (delivery) | En camino | Vendedor o Repartidor | CU08 |
| Pedido: Listo | El cliente retira (recojo) | Entregado | Vendedor | CU08 |
| Pedido: En camino | El repartidor confirma la entrega | Entregado | Repartidor | CU08 |
| Pedido: En camino | La entrega falla, con observación | Listo | Repartidor | CU08 |
| Pago: Pendiente de pago | El cliente registra método y comprobante | En verificación | Cliente | CU06 |
| Pago: En verificación | Se aprueba | Pagado | Con permiso de verificar | CU07 |
| Pago: En verificación | Se rechaza, con observación | Rechazado | Con permiso de verificar | CU07 |
| Pago: Rechazado | El sistema reabre el cobro | Pendiente de pago | Sistema | CU06 |

### 2.3 Reglas de negocio

| ID | Regla | CU |
| --- | --- | --- |
| RN01 | Un correo identifica una sola cuenta y no puede repetirse | CU09, CU11 |
| RN02 | Una cuenta desactivada no inicia sesión y conserva su historial | CU01, CU09 |
| RN03 | Siempre queda al menos un administrador activo; nadie desactiva su propia cuenta | CU09 |
| RN04 | El administrador siempre conserva acceso a Seguridad | CU09 |
| RN05 | Un pedido pasa a Confirmado solo cuando su pago está Pagado | CU06, CU07 |
| RN06 | Quien registró un pedido asistido no verifica el pago de ese pedido | CU07, CU17 |
| RN07 | Rechazar un pago exige observación; el pago vuelve a Pendiente de pago | CU06, CU07 |
| RN08 | No se confirma un pedido con productos no disponibles | CU05 |
| RN09 | Una salida de inventario no puede superar el stock; al llegar al mínimo se alerta | CU03 |
| RN10 | Un producto con pedidos asociados solo se desactiva, no se elimina | CU02 |
| RN11 | Total del pedido = suma de ((precio + recargos de las opciones elegidas) × cantidad) + costo de envío si es delivery. El recargo se cobra por unidad | CU05 |
| RN12 | El delivery exige dirección, teléfono y horario de entrega; el recojo exige teléfono y horario, pero no dirección | CU05, CU17 |
| RN13 | Una entrega fallida devuelve el pedido a Listo con observación | CU08 |
| RN14 | Un pedido solo se cancela antes de En preparación; el reembolso se hace fuera del sistema | CU16 |
| RN15 | El catálogo se consulta sin iniciar sesión; hacer un pedido exige cuenta | CU04, CU05 |
| RN16 | Un cliente no puede obtener un rol de personal; esas cuentas las crea un administrador | CU09, CU11 |
| RN17 | El mensaje escrito sobre una torta tiene hasta 24 caracteres y no tiene recargo | CU05, CU17 |

## 3. Resumen de casos de uso

Son 17 casos de uso. Los diez primeros vienen del documento v0 y se ajustaron; CU11 a CU17 son nuevos. La prioridad sigue la dependencia: Seguridad primero, luego lo que permite completar un pedido.

| Módulo | ID | Nombre | Actor principal | Prioridad | Origen |
| --- | --- | --- | --- | --- | --- |
| Seguridad | CU01 | Iniciar y cerrar sesión | Cliente, personal | Alta | v0, ajustado |
| Seguridad | CU09 | Gestionar usuarios y permisos | Administrador | Alta | v0, ajustado |
| Seguridad | CU11 | Registrarse como cliente | Cliente | Alta | Nuevo |
| Seguridad | CU12 | Recuperar contraseña | Cliente, personal | Media | Nuevo |
| Seguridad | CU13 | Gestionar perfil | Usuario autenticado | Baja | Nuevo |
| E-commerce | CU02 | Gestionar productos | Administrador | Alta | v0, ajustado |
| E-commerce | CU14 | Gestionar categorías | Administrador | Media | Nuevo |
| E-commerce | CU03 | Gestionar inventario | Administrador | Media | v0, ajustado |
| E-commerce | CU04 | Consultar catálogo | Cliente | Alta | v0, ajustado |
| E-commerce | CU05 | Hacer un pedido | Cliente | Alta | v0, ajustado |
| E-commerce | CU17 | Registrar pedido asistido | Vendedor | Media | Nuevo |
| E-commerce | CU15 | Consultar mis pedidos | Cliente | Media | Nuevo |
| E-commerce | CU16 | Cancelar pedido | Cliente, personal | Media | Nuevo |
| E-commerce | CU10 | Consultar pedidos y ventas | Administrador | Media | v0, ajustado |
| Cobros | CU06 | Registrar pago | Cliente | Alta | v0, ajustado |
| Cobros | CU07 | Verificar pago | Administrador, Vendedor con permiso | Alta | v0, ajustado |
| Distribución | CU08 | Gestionar entrega | Administrador, Vendedor, Repartidor | Alta | v0, ajustado |

## 4. Módulo Seguridad

### CU01 — Iniciar y cerrar sesión

| Campo | Contenido |
| --- | --- |
| ID / versión | CU01 / 1.0 |
| Actor principal | Cliente, Administrador, Vendedor, Repartidor |
| Fuente | RF02, RF03, RF26, RF27 |
| Prioridad | Alta |
| Objetivo | Acceder al sistema con la cuenta propia y salir de forma segura |
| Descripción | El usuario se identifica con correo y contraseña. El sistema lo lleva a la Tienda si es cliente o al Panel si es personal, con las opciones que permite su rol |
| Precondición | El usuario tiene una cuenta activa |
| Postcondición | Hay una sesión abierta con el rol del usuario, o la sesión queda cerrada |
| Secuencia normal | 1. El usuario abre la pantalla de inicio de sesión. 2. Ingresa correo y contraseña. 3. El sistema valida las credenciales y que la cuenta esté activa. 4. El sistema abre la sesión y muestra la Tienda (cliente) o el Panel (personal). 5. Para salir, el usuario elige Cerrar sesión y el sistema la cierra y vuelve al inicio |
| Secuencia opcional | Alt. 1: credenciales incorrectas. El sistema muestra un mensaje general sin decir cuál dato falló y cuenta el intento. Al 5.º intento seguido bloquea la cuenta 15 minutos (RNF05) y lo registra. Alt. 2: cuenta desactivada. El sistema niega el acceso (RN02). Alt. 3: olvidó la contraseña. Continúa en CU12. Alt. 4: inactividad. Pasados 30 minutos sin uso en el Panel, el sistema cierra la sesión (RNF06) |
| Rendimiento | 2 s o menos para el 95 % de los inicios de sesión (RNF02) |
| Frecuencia | Varias veces al día por usuario |
| Importancia | Alta |
| Urgencia | Alta; todo lo demás depende de este caso |
| Observaciones | La contraseña se guarda con hash y sal (RNF04). Cada acción se valida en el servidor según el rol (RNF07). El prototipo guarda contraseñas en texto plano y debe corregirse |

### CU09 — Gestionar usuarios y permisos

| Campo | Contenido |
| --- | --- |
| ID / versión | CU09 / 1.0 |
| Actor principal | Administrador |
| Fuente | RF22, RF25, RF26, RF28 |
| Prioridad | Alta |
| Objetivo | Administrar las cuentas del personal y los permisos de cada rol |
| Descripción | Tiene dos partes. Usuarios: crear, editar, activar y desactivar cuentas de personal. Permisos: editar lo que puede hacer cada rol fijo (Administrador, Vendedor, Repartidor). No se crean roles nuevos (D6) |
| Precondición | El administrador tiene sesión iniciada |
| Postcondición | Los cambios quedan guardados y registrados en la auditoría |
| Secuencia normal | 1. El administrador abre Seguridad. 2. Elige Usuarios o Permisos. 3. En Usuarios, crea una cuenta con nombre, correo, rol y contraseña inicial, o edita, activa o desactiva una existente. 4. En Permisos, marca o desmarca los permisos de un rol. 5. El sistema valida, guarda y registra quién hizo el cambio y cuándo (RNF08) |
| Secuencia opcional | Alt. 1: el correo ya existe. El sistema rechaza la cuenta (RN01). Alt. 2: se desactiva una cuenta. Pierde el acceso y conserva su historial (RN02). Alt. 3: se intenta desactivar el último administrador activo o la propia cuenta. El sistema lo impide (RN03). Alt. 4: se intenta quitar al administrador el acceso a Seguridad o dárselo a otro rol. El sistema lo impide (RN04). Alt. 5: se intenta asignar rol de personal a un cliente. Solo se crean cuentas de personal desde aquí (RN16) |
| Rendimiento | 2 s o menos por operación |
| Frecuencia | Ocasional |
| Importancia | Alta |
| Urgencia | Alta; forma parte del Sprint 1 |
| Observaciones | El administrador también restablece la contraseña del personal (CU12). El registro de auditoría se conserva 12 meses |

### CU11 — Registrarse como cliente

| Campo | Contenido |
| --- | --- |
| ID / versión | CU11 / 1.0 |
| Actor principal | Cliente |
| Fuente | RF01 |
| Prioridad | Alta |
| Objetivo | Crear una cuenta de cliente para poder pedir |
| Descripción | Quien visita la Tienda se registra con nombre, correo y contraseña. La cuenta nace con el rol Cliente |
| Precondición | La persona no tiene una cuenta con ese correo |
| Postcondición | Existe una cuenta de cliente activa y la persona inicia sesión |
| Secuencia normal | 1. La persona elige Registrarse. 2. Ingresa nombre, correo y contraseña. 3. El sistema valida los datos y la contraseña (RNF03). 4. El sistema crea la cuenta con rol Cliente. 5. El sistema inicia la sesión y vuelve a la Tienda |
| Secuencia opcional | Alt. 1: el correo ya está registrado. El sistema lo avisa y ofrece iniciar sesión o recuperar la contraseña (RN01). Alt. 2: datos inválidos. El sistema marca el campo y explica cómo corregirlo (RNF01) |
| Rendimiento | 2 s o menos |
| Frecuencia | Una vez por cliente |
| Importancia | Alta; sin cuenta no hay pedido (RN15) |
| Urgencia | Alta |
| Observaciones | El cliente nunca obtiene un rol de personal (RN16). Se ve el catálogo sin registrarse |

### CU12 — Recuperar contraseña

| Campo | Contenido |
| --- | --- |
| ID / versión | CU12 / 1.0 |
| Actor principal | Cliente, personal |
| Fuente | RF24 |
| Prioridad | Media |
| Objetivo | Recuperar el acceso cuando se olvidó la contraseña |
| Descripción | El cliente recibe un enlace por correo para definir una contraseña nueva. El personal pide al administrador que la restablezca (D5) |
| Precondición | La cuenta existe y está activa |
| Postcondición | La cuenta tiene una contraseña nueva y las sesiones anteriores se cierran |
| Secuencia normal | Cliente: 1. Elige «¿Olvidó su contraseña?» e ingresa su correo. 2. El sistema envía un enlace de un solo uso que vence en 60 minutos. 3. El cliente abre el enlace y define la contraseña nueva. 4. El sistema la guarda con hash y sal. Personal: 1. El usuario avisa al administrador. 2. El administrador restablece la contraseña en Seguridad (CU09). 3. El usuario la cambia al ingresar |
| Secuencia opcional | Alt. 1: el correo no existe. El sistema muestra el mismo mensaje de éxito para no revelar qué correos están registrados. Alt. 2: el enlace venció o ya se usó. El sistema pide solicitar otro |
| Rendimiento | 2 s o menos; el correo llega en menos de 2 minutos |
| Frecuencia | Ocasional |
| Importancia | Media |
| Urgencia | Media |
| Observaciones | Para el personal no se usa correo porque son pocas cuentas y las gestiona el administrador. El plazo de 60 minutos del enlace es una propuesta que el equipo puede ajustar |

### CU13 — Gestionar perfil

| Campo | Contenido |
| --- | --- |
| ID / versión | CU13 / 1.0 |
| Actor principal | Usuario autenticado |
| Fuente | RF04 |
| Prioridad | Baja |
| Objetivo | Ver y actualizar los datos propios y cambiar la contraseña |
| Descripción | El usuario edita su nombre, teléfono y dirección habitual y cambia su contraseña. El correo no se cambia desde aquí |
| Precondición | El usuario tiene sesión iniciada |
| Postcondición | Los datos quedan actualizados |
| Secuencia normal | 1. El usuario abre Mi perfil. 2. Edita sus datos o elige Cambiar contraseña. 3. Para cambiarla, ingresa la actual y la nueva (RNF03). 4. El sistema valida y guarda |
| Secuencia opcional | Alt. 1: la contraseña actual es incorrecta. El sistema no la cambia y avisa. Alt. 2: la nueva no cumple los requisitos. El sistema explica cuáles no cumple |
| Rendimiento | 2 s o menos |
| Frecuencia | Rara |
| Importancia | Baja |
| Urgencia | Baja |
| Observaciones | Teléfono y dirección solo los ven los roles que los necesitan (RNF16) |

## 5. Módulo E-commerce

### CU02 — Gestionar productos

| Campo | Contenido |
| --- | --- |
| ID / versión | CU02 / 1.0 |
| Actor principal | Administrador |
| Fuente | RF16, RF17, RF18 |
| Prioridad | Alta |
| Objetivo | Mantener el catálogo de productos actualizado |
| Descripción | El administrador registra, edita y desactiva productos. Cada producto tiene nombre, descripción, precio, imagen, categoría (CU14), indicador Activo, disponibilidad, si es personalizable y sus opciones con su recargo fijo (D9) |
| Precondición | El administrador tiene sesión iniciada y existe al menos una categoría |
| Postcondición | El catálogo refleja el cambio |
| Secuencia normal | 1. El administrador abre Catálogo. 2. Elige Nuevo producto o selecciona uno existente. 3. Completa o modifica los datos, la imagen, la disponibilidad y las opciones de personalización con su recargo. 4. El sistema valida y guarda. 5. El producto aparece en la Tienda si está activo |
| Secuencia opcional | Alt. 1: faltan datos obligatorios o el precio no es válido. El sistema marca el campo. Alt. 2: la imagen no cumple el formato o tamaño (RNF10). El sistema la rechaza. Si es JPG o PNG y no es cuadrada o mide más de 1200 px, la recorta al centro y la reduce. Alt. 3: se desactiva un producto. Deja de verse en la Tienda y conserva sus pedidos. Se puede borrar solo si nunca se pidió; si tiene pedidos asociados, el sistema lo impide y solo se desactiva (RN10). Alt. 4: el Vendedor solo cambia la disponibilidad, sin editar el resto |
| Rendimiento | 2 s o menos por operación |
| Frecuencia | Semanal |
| Importancia | Alta |
| Urgencia | Alta; sin productos no hay tienda |
| Observaciones | La disponibilidad es un indicador manual que cambia el personal (D2) |

### CU14 — Gestionar categorías

| Campo | Contenido |
| --- | --- |
| ID / versión | CU14 / 1.0 |
| Actor principal | Administrador |
| Fuente | RF19 |
| Prioridad | Media |
| Objetivo | Organizar el catálogo en categorías |
| Descripción | El administrador crea, edita y desactiva categorías como tortas, cupcakes, galletas o postres |
| Precondición | El administrador tiene sesión iniciada |
| Postcondición | Las categorías disponibles para productos y filtros están actualizadas |
| Secuencia normal | 1. El administrador abre Categorías. 2. Crea una o selecciona una existente. 3. Ingresa o modifica el nombre. 4. El sistema valida y guarda |
| Secuencia opcional | Alt. 1: el nombre ya existe. El sistema lo rechaza. Alt. 2: se desactiva una categoría con productos. El sistema avisa cuántos productos afecta y deja que el administrador confirme |
| Rendimiento | 2 s o menos |
| Frecuencia | Rara |
| Importancia | Media |
| Urgencia | Media |
| Observaciones | Agregar una categoría no exige cambiar código (RNF18) |

### CU03 — Gestionar inventario

| Campo | Contenido |
| --- | --- |
| ID / versión | CU03 / 1.0 |
| Actor principal | Administrador (el Vendedor solo consulta) |
| Fuente | RF29, RF30, RF31 |
| Prioridad | Media |
| Objetivo | Controlar las existencias de insumos y materiales |
| Descripción | El administrador registra entradas, salidas y ajustes de cada ítem con cantidad y motivo. El inventario controla insumos; la disponibilidad de los productos terminados es un indicador aparte (D2). Cada ítem tiene un mínimo obligatorio |
| Precondición | El usuario tiene sesión y permiso de inventario |
| Postcondición | El stock queda actualizado y el movimiento guardado en el historial |
| Secuencia normal | 1. El administrador abre Inventario. 2. Selecciona el ítem o crea uno nuevo con su mínimo. 3. Elige Entrada, Salida o Ajuste e ingresa cantidad y motivo. 4. El sistema valida y actualiza el stock. 5. Si el stock llega al mínimo, el sistema muestra una alerta |
| Secuencia opcional | Alt. 1: la salida supera el stock. El sistema la rechaza y el stock nunca queda negativo (RN09, RNF14). Alt. 2: el stock llega o baja del mínimo. El sistema marca el ítem y lo muestra en las alertas del inicio (RN09). Alt. 3: el Vendedor abre Inventario. Solo ve existencias y alertas |
| Rendimiento | 2 s o menos |
| Frecuencia | Diaria |
| Importancia | Media |
| Urgencia | Media |
| Observaciones | El historial de movimientos lo consulta el administrador (RF31) |

### CU04 — Consultar catálogo

| Campo | Contenido |
| --- | --- |
| ID / versión | CU04 / 1.0 |
| Actor principal | Cliente (también visitante sin sesión) |
| Fuente | RF05, RF06, RF07, RF08 |
| Prioridad | Alta |
| Objetivo | Encontrar productos y conocer su precio y disponibilidad |
| Descripción | La Tienda muestra los productos activos con nombre, descripción, precio, imagen y disponibilidad. Se puede buscar por nombre y filtrar por categoría |
| Precondición | Ninguna; no requiere sesión (RN15) |
| Postcondición | El usuario ve los productos que buscó |
| Secuencia normal | 1. El usuario abre la Tienda. 2. El sistema muestra los productos activos. 3. El usuario escribe un nombre o elige una categoría. 4. El sistema filtra la lista. 5. El usuario abre un producto para ver su detalle |
| Secuencia opcional | Alt. 1: no hay resultados. El sistema lo indica y sugiere quitar filtros. Alt. 2: el producto no está disponible. Se muestra como no disponible y no se puede agregar al carrito |
| Rendimiento | 2 s o menos para el 95 % de las consultas (RNF02) |
| Frecuencia | Muy alta |
| Importancia | Alta |
| Urgencia | Alta |
| Observaciones | Los productos desactivados no aparecen |

### CU05 — Hacer un pedido

| Campo | Contenido |
| --- | --- |
| ID / versión | CU05 / 1.0 |
| Actor principal | Cliente |
| Fuente | RF09 a RF13, RF32, RF35 |
| Prioridad | Alta |
| Objetivo | Armar un carrito y confirmar un pedido |
| Descripción | El cliente agrega productos, elige sus opciones de personalización, revisa el total y confirma el pedido con recojo o delivery. El pedido nace en estado Pendiente y continúa en CU06 |
| Precondición | El cliente tiene sesión iniciada |
| Postcondición | Existe un pedido Pendiente con su pago Pendiente de pago |
| Secuencia normal | 1. El cliente agrega un producto al carrito, eligiendo las opciones si las tiene. 2. Cambia cantidades o quita productos. 3. El sistema calcula el total (RN11). 4. El cliente elige recojo o delivery e ingresa teléfono y horario; si es delivery, también la dirección. 5. El sistema valida la disponibilidad (RN08). 6. El cliente confirma. 7. El sistema crea el pedido y pasa a CU06 |
| Secuencia opcional | Alt. 1: un producto dejó de estar disponible. El sistema avisa y no permite confirmar hasta quitarlo (RN08). Alt. 2: el cliente sin sesión intenta confirmar. El sistema le pide iniciar sesión o registrarse y conserva el carrito (RN15). Alt. 3: faltan datos de entrega. El sistema pide completarlos: dirección, teléfono y horario para el delivery; teléfono y horario para el recojo (RN12). Alt. 3a: el horario está fuera de la atención (lunes a sábado de 9:00 a 20:00 y domingo de 10:00 a 14:00) o tiene menos de 24 horas de anticipación. El sistema pide otro. Alt. 3b: el mensaje para la torta supera los 24 caracteres. El sistema no lo acepta (RN17). Alt. 4: el cliente abandona. El carrito se conserva en su sesión |
| Rendimiento | Carrito y total en 1 s o menos (RNF02) |
| Frecuencia | Alta |
| Importancia | Alta |
| Urgencia | Alta |
| Observaciones | Total = (precio + recargos) × cantidad, sumado por línea, + tarifa fija de envío si es delivery (D4, D9). De cada tipo de opción (tamaño, sabor) se elige como máximo una. Si el producto ofrece tamaños, elegir uno es obligatorio; las demás opciones son optativas. Confirmar el pedido y actualizar la disponibilidad ocurre en una sola transacción (RNF14) |

### CU17 — Registrar pedido asistido

| Campo | Contenido |
| --- | --- |
| ID / versión | CU17 / 1.0 |
| Actor principal | Vendedor |
| Fuente | RF34 |
| Prioridad | Media |
| Objetivo | Cargar un pedido que el cliente hizo por teléfono o en mostrador |
| Descripción | El vendedor registra el pedido a nombre de un cliente, con las mismas reglas que CU05 (D7) |
| Precondición | El vendedor tiene sesión y permiso de pedidos |
| Postcondición | Existe un pedido Pendiente a nombre del cliente, marcado como asistido y con el nombre de quien lo registró |
| Secuencia normal | 1. El vendedor abre Pedidos y elige Nuevo pedido asistido. 2. Busca al cliente por correo o teléfono, o lo registra. 3. Agrega productos, opciones, cantidades y tipo de entrega. 4. El sistema calcula el total y valida la disponibilidad. 5. El vendedor confirma y el sistema crea el pedido |
| Secuencia opcional | Alt. 1: el cliente no existe. El vendedor crea la cuenta con sus datos y el cliente define su contraseña por CU12. Alt. 2: el vendedor registra el pago recibido. El pago queda En verificación y otra persona con permiso lo verifica (RN06) |
| Rendimiento | 2 s o menos por paso |
| Frecuencia | Media |
| Importancia | Media |
| Urgencia | Media |
| Observaciones | Quien registró el pedido no verifica su pago (RN06) |

### CU15 — Consultar mis pedidos

| Campo | Contenido |
| --- | --- |
| ID / versión | CU15 / 1.0 |
| Actor principal | Cliente |
| Fuente | RF14, RF15 |
| Prioridad | Media |
| Objetivo | Ver el historial de pedidos y su estado |
| Descripción | El cliente ve sus pedidos, el estado de cada pedido y el estado de su pago |
| Precondición | El cliente tiene sesión iniciada |
| Postcondición | El cliente ve el estado actual |
| Secuencia normal | 1. El cliente abre Mis pedidos. 2. El sistema lista sus pedidos, del más reciente al más antiguo. 3. El cliente abre uno para ver productos, total, estado del pedido y estado del pago |
| Secuencia opcional | Alt. 1: no tiene pedidos. El sistema lo indica y enlaza al catálogo. Alt. 2: el pago fue rechazado. El detalle muestra la observación y permite volver a registrar el pago (CU06) |
| Rendimiento | 2 s o menos |
| Frecuencia | Alta |
| Importancia | Media |
| Urgencia | Media |
| Observaciones | El cliente solo ve sus propios pedidos (RNF16) |

### CU16 — Cancelar pedido

| Campo | Contenido |
| --- | --- |
| ID / versión | CU16 / 1.0 |
| Actor principal | Cliente, personal con permiso de pedidos |
| Fuente | RF33 |
| Prioridad | Media |
| Objetivo | Anular un pedido antes de que se prepare |
| Descripción | El cliente o el personal cancelan un pedido que está Pendiente o Confirmado (D8). El reembolso, si hubo pago, se hace fuera del sistema |
| Precondición | El pedido está en estado Pendiente o Confirmado |
| Postcondición | El pedido queda Cancelado con la fecha, quién lo canceló y el motivo |
| Secuencia normal | 1. El usuario abre el pedido y elige Cancelar. 2. Ingresa el motivo. 3. El sistema pide confirmación. 4. El sistema cambia el estado a Cancelado y lo registra en el historial |
| Secuencia opcional | Alt. 1: el pedido ya está En preparación o más avanzado. El sistema no permite cancelar (RN14). Alt. 2: el pedido ya estaba pagado. El sistema avisa al personal que debe gestionar el reembolso fuera del sistema |
| Rendimiento | 2 s o menos |
| Frecuencia | Baja |
| Importancia | Media |
| Urgencia | Media |
| Observaciones | El pago de un pedido cancelado no se vuelve a verificar |

### CU10 — Consultar pedidos y ventas

| Campo | Contenido |
| --- | --- |
| ID / versión | CU10 / 1.0 |
| Actor principal | Administrador (el Vendedor consulta pedidos, sin montos de ventas) |
| Fuente | RF20, RF21, RF23 |
| Prioridad | Media |
| Objetivo | Revisar pedidos y obtener reportes de ventas |
| Descripción | El personal consulta la lista de pedidos y su detalle. El administrador además ve reportes de pedidos y ventas filtrados por fecha, estado y método de pago |
| Precondición | El usuario tiene sesión y permiso de pedidos o de reportes |
| Postcondición | El usuario ve la información filtrada |
| Secuencia normal | 1. El usuario abre Pedidos o Reportes. 2. Aplica filtros. 3. El sistema muestra la lista y, en reportes, los totales. 4. El usuario abre un pedido para ver su detalle e historial de estados |
| Secuencia opcional | Alt. 1: no hay datos en el rango. El sistema lo indica. Alt. 2: un usuario sin permiso intenta abrir Reportes. El sistema deniega el acceso y lo registra (RNF07) |
| Rendimiento | Listas en 2 s; reportes sobre 12 meses de pedidos en 5 s (RNF02) |
| Frecuencia | Diaria |
| Importancia | Media |
| Urgencia | Media |
| Observaciones | Avanzar el estado de un pedido se detalla en CU08 |

## 6. Módulo Cobros

### CU06 — Registrar pago

| Campo | Contenido |
| --- | --- |
| ID / versión | CU06 / 1.0 |
| Actor principal | Cliente (el Vendedor lo registra en un pedido asistido) |
| Fuente | RF36 |
| Prioridad | Alta |
| Objetivo | Informar el pago de un pedido con su comprobante |
| Descripción | Tras confirmar el pedido, el cliente elige el método (QR estático, transferencia o depósito), paga fuera del sistema y registra la referencia con el comprobante. El pago pasa a En verificación |
| Precondición | Existe un pedido Pendiente con pago en Pendiente de pago |
| Postcondición | El pago queda En verificación con método, referencia y comprobante |
| Secuencia normal | 1. El cliente abre el pedido y elige Pagar. 2. El sistema muestra el monto y los datos de pago (QR o cuenta). 3. El cliente paga por su banco. 4. Elige el método, ingresa la referencia y adjunta el comprobante. 5. El sistema valida el archivo (RNF09) y pasa el pago a En verificación |
| Secuencia opcional | Alt. 1: el archivo no es JPG, PNG o PDF, o supera 5 MB. El sistema lo rechaza y explica el motivo. Alt. 2: falta la referencia o el comprobante. El sistema no registra el pago. Alt. 3: el pago fue rechazado en CU07. El sistema lo reabre como Pendiente de pago (RN07) y el cliente registra otro comprobante |
| Rendimiento | 2 s o menos, sin contar la subida del archivo |
| Frecuencia | Una o más veces por pedido |
| Importancia | Alta |
| Urgencia | Alta; el pedido no avanza sin pago (RN05) |
| Observaciones | El sistema no se conecta a bancos; la validación es manual en CU07 |

### CU07 — Verificar pago

| Campo | Contenido |
| --- | --- |
| ID / versión | CU07 / 1.0 |
| Actor principal | Administrador, o Vendedor con el permiso «Verificar pagos» |
| Fuente | RF37, RF38, RF39 |
| Prioridad | Alta |
| Objetivo | Aprobar o rechazar un pago comparando el comprobante con el pedido |
| Descripción | El verificador revisa el comprobante y decide. Aprobar lleva el pago a Pagado y el pedido a Confirmado. Rechazar exige una observación |
| Precondición | El pago está En verificación y el usuario tiene el permiso |
| Postcondición | El pago queda Pagado o Rechazado, el historial lo registra y, si se aprobó, el pedido queda Confirmado |
| Secuencia normal | 1. El verificador abre Cobranza y ve los pagos En verificación. 2. Abre uno y revisa monto, referencia y comprobante. 3. Elige Aprobar. 4. El sistema pasa el pago a Pagado, el pedido a Confirmado y guarda quién verificó y cuándo |
| Secuencia opcional | Alt. 1: no coincide el monto o el comprobante. El verificador elige Rechazar. Alt. 2: el rechazo sin observación. El sistema la exige (RN07) y luego reabre el cobro como Pendiente de pago. Alt. 3: el pedido fue asistido y lo registró el mismo usuario. El sistema no le permite verificar ese pago (RN06). Alt. 4: otra persona ya decidió ese pago. El sistema muestra el resultado y no aplica una segunda decisión (RNF15) |
| Rendimiento | 2 s o menos por decisión |
| Frecuencia | Diaria |
| Importancia | Alta |
| Urgencia | Alta |
| Observaciones | Cada verificación se guarda en la auditoría por 12 meses (RNF08) |

## 7. Módulo Distribución

### CU08 — Gestionar entrega

| Campo | Contenido |
| --- | --- |
| ID / versión | CU08 / 1.0 |
| Actor principal | Administrador o Vendedor (preparan y asignan), Repartidor (sus entregas) |
| Fuente | RF21, RF40, RF41, RF42, RF43 |
| Prioridad | Alta |
| Objetivo | Llevar el pedido confirmado hasta su entrega |
| Descripción | El personal marca la preparación, asigna un repartidor a los pedidos con delivery y el Repartidor marca En camino y Entregado. El recojo en tienda lo cierra el Vendedor |
| Precondición | El pedido está Confirmado (pago Pagado) |
| Postcondición | El pedido queda Entregado, o vuelve a Listo si la entrega falló. Cada cambio queda en el historial |
| Secuencia normal | 1. El administrador, o el Vendedor con permiso, marca En preparación y luego Listo. 2. Si es delivery, el Vendedor o el Administrador asigna un repartidor. 3. El Repartidor ve el pedido con dirección y teléfono y marca En camino. 4. Entrega y marca Entregado. 5. El sistema guarda cada cambio con usuario y fecha. Si es recojo: el Vendedor marca Entregado cuando el cliente retira |
| Secuencia opcional | Alt. 1: pedido sin pago aprobado. El sistema no permite prepararlo (RN05). Alt. 2: la entrega falla. El Repartidor registra una observación y el pedido vuelve a Listo (RN13), para reasignar o reintentar. Alt. 3: el Repartidor intenta abrir un pedido que no es suyo. El sistema lo niega (RNF16). Alt. 4: se cambia el repartidor antes de En camino. El sistema registra el cambio |
| Rendimiento | 2 s o menos por cambio de estado |
| Frecuencia | Diaria |
| Importancia | Alta |
| Urgencia | Alta |
| Observaciones | Quien marca la preparación depende del permiso del rol (D3). La tarifa de envío es fija (D4) |

## 8. Matriz de permisos por rol

«Sí» significa permitido. El administrador puede editar los permisos del Vendedor y del Repartidor (CU09), salvo el acceso a Seguridad: es solo del administrador, no se da a otro rol y no se le quita (RN04).

| Permiso | Administrador | Vendedor | Repartidor | CU |
| --- | --- | --- | --- | --- |
| Inicio con montos de ventas | Sí | No | No | CU10 |
| Inicio con pedidos y pagos pendientes | Sí | Sí | No | CU10 |
| Catálogo: crear, editar, desactivar | Sí | No | No | CU02, CU14 |
| Catálogo: consultar y cambiar disponibilidad | Sí | Sí | No | CU02 |
| Inventario: consultar y ver alertas | Sí | Sí | No | CU03 |
| Inventario: registrar movimientos | Sí | No | No | CU03 |
| Pedidos: consultar y registrar pedido asistido | Sí | Sí | No | CU10, CU17 |
| Pedidos: cancelar | Sí | Sí | No | CU16 |
| Pedidos: marcar preparación y Listo | Sí | Solo con permiso | No | CU08 |
| Cobranza: registrar pago de un pedido asistido | Sí | Sí | No | CU06 |
| Cobranza: aprobar o rechazar pago | Sí | Solo con el permiso «Verificar pagos» | No | CU07 |
| Distribución: asignar repartidor, marcar En camino y cerrar un recojo | Sí | Sí | No | CU08 |
| Distribución: actualizar entregas | Sí | No | Sí, solo las suyas | CU08 |
| Reportes de pedidos y ventas | Sí | No | No | CU10 |
| Seguridad: usuarios y permisos | Sí | No | No | CU09 |
| Configuración: tarifa de envío y métodos de pago | Sí | No | No | CU05, CU06 |

El Vendedor que registró un pedido asistido no puede verificar el pago de ese mismo pedido (RN06). El Cliente no usa el Panel; solo accede a la Tienda (CU04, CU05, CU06, CU13, CU15, CU16).

## 9. Trazabilidad y cambios respecto a la versión 0

### 9.1 Requisitos funcionales y casos de uso

| Caso de uso | Requisitos que cubre |
| --- | --- |
| CU01 | RF02, RF03, RF26, RF27 |
| CU02 | RF16, RF17, RF18 |
| CU03 | RF29, RF30, RF31 |
| CU04 | RF05, RF06, RF07, RF08 |
| CU05 | RF09, RF10, RF11, RF12, RF13, RF32, RF35 |
| CU06 | RF36 |
| CU07 | RF37, RF38, RF39 |
| CU08 | RF21, RF40, RF41, RF42, RF43 |
| CU09 | RF22, RF25, RF26, RF28 |
| CU10 | RF20, RF23 |
| CU11 | RF01 |
| CU12 | RF24 |
| CU13 | RF04 |
| CU14 | RF19 |
| CU15 | RF14, RF15 |
| CU16 | RF33 |
| CU17 | RF34 |

Los 43 requisitos de la Visión v1 (RF01 a RF43) quedan cubiertos por al menos un caso de uso.

### 9.2 Cambios respecto a los Casos de Uso v0

| Caso de uso | Cambio |
| --- | --- |
| Todos | Se usan los cuatro roles fijos (Cliente, Administrador, Vendedor, Repartidor) en lugar de los «Encargados». Se agregan rendimiento medible y referencias a RNF y RN |
| CU01 | Incluye cierre de sesión, bloqueo por intentos fallidos y cierre por inactividad. La recuperación de contraseña pasa a CU12 |
| CU02 | Agrega categoría, disponibilidad, personalización y recargo. «Eliminar» se reemplaza por desactivar |
| CU03 | Actor: Administrador; el Vendedor solo consulta. Aclara que controla insumos. Mínimo obligatorio |
| CU04 | No requiere sesión. Agrega búsqueda, filtro y disponibilidad |
| CU05 | Exige sesión. Valida disponibilidad, calcula envío y recargos. Estado inicial Pendiente |
| CU06 | Define métodos (QR, transferencia, depósito) y comprobante obligatorio. El rechazo reabre el cobro |
| CU07 | Agrega el permiso «Verificar pagos», la separación de funciones (RN06) y la auditoría |
| CU08 | Parte de un pago aprobado. Agrega recojo, asignación y entrega fallida |
| CU09 | Edita permisos de roles fijos; no crea roles. Agrega RN03 y RN04 |
| CU10 | Reportes solo para el Administrador, con filtros y rendimiento definido |
| CU11 a CU17 | Casos nuevos: registro de cliente, recuperación de contraseña, perfil, categorías, mis pedidos, cancelación y pedido asistido |

### 9.3 Cambios en las versiones 1.1 y 1.2

| Elemento | Cambio |
| --- | --- |
| Matriz de permisos | Se agrega el permiso «Configuración: tarifa de envío y métodos de pago». Ningún permiso cubría editar la tarifa fija de envío (D4) ni los métodos de pago (RNF18) |
| RN11, RN12 | El recargo se cobra por unidad; el recojo exige teléfono y horario. Lo decidió el equipo al implementar la etapa C |
| Matriz de permisos | El permiso de asignar repartidor también permite marcar En camino (tabla 2.2) y cerrar un recojo, que no tenían permiso propio |
| CU02, CU05, CU09 | El equipo confirmó el horario de atención con 24 horas de anticipación, el borrado de productos sin pedidos y que Seguridad es solo del administrador (sección 10.1) |
| Sección 10 | El equipo revisó las decisiones propuestas al implementar y las confirmó todas |
| RNF10 | Las imágenes de producto se aceptan hasta 10 MB y se reducen a 1200 px, para subir fotos de celular sin editarlas antes |
| CU05, CU17 (v1.3) | Si el producto ofrece tamaños, elegir uno es obligatorio; sin él, el pedido de una torta queda incompleto |

## 10. Decisiones de implementación

Decisiones tomadas al construir el sistema que los documentos no fijaban. El equipo las revisó y confirmó todas el 02/11/2026. Cambiar cualquiera de ellas es un ajuste pequeño en el código.

### 10.1 Decisiones confirmadas

| Tema | Decisión |
| --- | --- |
| Recargo (RN11) | Se cobra por unidad: (precio + recargos) × cantidad |
| Opciones de personalización | Como máximo una de cada tipo. Si el producto ofrece tamaños, hay que elegir uno (el equipo lo decidió el 08/11/2026); las demás son optativas |
| Recojo (RN12) | Exige teléfono y horario, no dirección |
| Imagen de producto (RNF10) | Se aceptan fotos de hasta 10 MB. Una JPG o PNG se recorta al centro, respetando la orientación de las fotos de celular, y se reduce a 1200 px por lado. Una WebP debe venir cuadrada y pesar hasta 2 MB, porque no se puede reducir |
| Métodos de pago (RNF18) | Tabla editable desde el Panel; nacen desactivados hasta que el administrador escribe los datos para pagar |
| Horario de entrega (CU05, CU17) | Dentro de la atención (lunes a sábado de 9:00 a 20:00 y domingo de 10:00 a 14:00) y con al menos 24 horas de anticipación |
| Borrar productos (RN10) | Un producto sin pedidos se puede borrar; uno con pedidos solo se desactiva |
| Sesión de la Tienda | Se cierra tras 30 días sin uso (el Panel, tras 30 minutos, según RNF06) |
| Permiso de Seguridad (RN04) | Es solo del administrador: no se da a otro rol ni se le quita |
| Cambio del propio rol | Un administrador no puede cambiar su propio rol |
| Cuenta desactivada | Al ingresar recibe el mismo mensaje que con datos incorrectos |
| Imágenes y comprobantes | Se guardan en la base de datos, para desplegar en la nube sin disco propio (RNF21) |
| Producto sin imagen | Se puede crear y luego subir la imagen |
| Categoría desactivada | Sus productos dejan de verse en la Tienda |
| Mensaje sobre la torta | Solo lo admiten los productos personalizables |
| Tarifa inicial de envío | Bs 10.00, por la landing («desde Bs 10»); el administrador la cambia |
| Permiso Configuración | Nuevo, solo para el administrador: tarifa de envío y métodos de pago |
| Confirmar un pedido (RNF14) | La disponibilidad es manual (D2), así que no cambia; se bloquean los productos mientras se confirma |
| Auditoría de pagos (RNF08) | La tabla de verificaciones registra quién aprobó o rechazó cada pago y cuándo |
| Asignar repartidor | Se puede desde que el pedido está Confirmado, y cambiar hasta que sale En camino; cada asignación queda registrada |
| Ajuste de inventario | Registra el stock contado, no una diferencia |
| Cliente de un pedido asistido | Su cuenta nace sin contraseña conocida; la define con «¿Olvidó su contraseña?» (CU17) |
| Cancelación de un pedido pagado | Su pago sale de la cola de verificación y ya no se puede decidir (CU16) |
| Reporte de ventas | «Vendido» suma los pedidos pagados y no cancelados; el rango es de hasta 12 meses (RNF02) |
| Estado del cobro en Mis pedidos | La Tienda combina dos consultas (pedidos y pagos), para que E-commerce no dependa de Cobros |
