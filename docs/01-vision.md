# Dulces Aromas — Visión del Proyecto v1

Versión 1.0 · 29/10/2026

Versión 1.0 del documento que reemplaza a la Visión v0. Universidad Mayor Real y Pontíficia San Francisco Xavier de Chuquisaca, Facultad de Ciencias y Tecnología, Sucre, Bolivia, 2026. Estudiantes: Avendaño Retamozo Juan Antonio, Dávalos Nuñez Gabriela y Morejon Morales Luz Yumara. Docente: Ing. Ramiro Duran.

## 1. Introducción

### 1.1 Nombre del proyecto

Dulces Aromas — «Momentos para compartir». Sistema de comercio electrónico para una pastelería de Sucre, Bolivia.

La plataforma permite a los clientes consultar el catálogo, personalizar productos, pedir y pagar desde una aplicación móvil. Al personal de la pastelería le permite administrar productos, inventario, pedidos, pagos y entregas desde un panel propio. Los productos son tortas, pasteles, cupcakes, galletas, postres y productos especiales.

### 1.2 Propósito del documento

Describe los requisitos de alto nivel, el alcance y las características del sistema. Es la base de los Casos de Uso, las Historias de Usuario y el Product Backlog, y sustituye a la Visión v0, que no incluía inventario, cobros ni distribución.

### 1.3 Alcance

La primera versión es una plataforma para una sola pastelería, con dos superficies que usan una misma API REST: la **Tienda**, una aplicación móvil hecha con Flutter para los clientes, y el **Panel**, para el personal. Se organiza en cuatro módulos: Seguridad, E-commerce, Cobros y Distribución (sección 4).

El cliente podrá:

- Registrarse, iniciar y cerrar sesión, recuperar su contraseña y gestionar su perfil.
- Consultar el catálogo, buscar por nombre y filtrar por categoría, sin necesidad de iniciar sesión.
- Personalizar los productos que lo permiten, agregarlos al carrito y confirmar un pedido con recojo en tienda o delivery.
- Pagar con QR, transferencia o depósito adjuntando el comprobante.
- Consultar el historial y el estado de sus pedidos y cancelarlos antes de que se preparen.

El personal podrá, según su rol:

- Administrar usuarios del personal, permisos, productos, categorías y opciones de personalización.
- Registrar entradas y salidas de insumos y ver alertas de bajo stock.
- Registrar pedidos a nombre de un cliente (por teléfono o en mostrador) y avanzar su estado.
- Verificar los pagos, asignar repartidores y registrar las entregas.
- Consultar pedidos y ventas con filtros.

### 1.3.1 Fuera de alcance

- Publicación de la aplicación en Google Play o App Store.
- Gestión de múltiples sucursales: hay un solo punto de recojo, «Sucursal centro».
- Automatización de la producción y descuento automático de insumos por receta.
- Pasarela de pago o QR dinámico con confirmación automática; el pago se verifica a mano.
- Roles personalizados creados por el administrador.
- Notificaciones automáticas de estado, reportes exportables, gráficos, rutas y mapas.
- Funcionalidades de inteligencia artificial y logística de gran escala.

Estas funciones podrán considerarse en versiones futuras.

### 1.4 Definiciones y acrónimos

| Término | Significado |
| --- | --- |
| Tienda | Parte pública del sistema, usada por los clientes |
| Panel | Parte administrativa, usada por el personal |
| Cliente | Persona que compra productos desde la Tienda |
| Administrador | Responsable de la gestión completa del sistema |
| Vendedor | Personal que atiende pedidos y pagos |
| Repartidor | Personal que entrega los pedidos con delivery |
| Producto | Artículo a la venta; puede ser personalizable |
| Insumo | Materia prima o material que se controla en inventario |
| Pedido | Solicitud de compra confirmada por un cliente o registrada por un vendedor |
| Pedido asistido | Pedido que registra un vendedor a nombre de un cliente |
| Comprobante | Imagen o PDF que respalda un pago |
| Rol | Conjunto de permisos del personal; hay tres fijos |
| RF, RNF | Requisito funcional, requisito no funcional |
| CU, HU | Caso de uso, historia de usuario |

### 1.5 Referencias

- Visión del Proyecto v0 y Detalle de Casos de Uso v0 (CU01 a CU10).
- Detalle de Casos de Uso v1 (CU01 a CU17) e Historias de Usuario y Product Backlog v1 (HU01 a HU21).
- Prototipo del Panel con el módulo de Seguridad del Sprint 1.
- Diagramas de componentes (capas y dependencias entre módulos).
- Landing page del Laboratorio 08 (https://sis324-lab-08.onrender.com/), usada como referencia de interfaz, paleta, accesibilidad y portabilidad.

## 2. Posicionamiento del negocio

### 2.1 Oportunidad de negocio

Muchas pastelerías pequeñas gestionan sus pedidos por llamadas, mensajes o redes sociales. Así es difícil organizar los pedidos, saber qué hay disponible y dar seguimiento a cada solicitud. Los clientes, a su vez, no tienen un lugar único donde ver todos los productos, sus precios y sus opciones de personalización.

Dulces Aromas centraliza la información y facilita la compra, el cobro y la entrega.

### 2.2 Declaración del problema

La gestión manual y dispersa de los pedidos provoca desorden, retrasos y dificultad para seguir cada solicitud. Los pagos se confirman a mano sobre comprobantes que llegan por distintos medios, y la entrega depende de coordinaciones informales. El cliente no puede ver el estado de su pedido.

### 2.3 Declaración de la posición del producto

Para quienes quieren comprar productos de pastelería de forma rápida, organizada y accesible, Dulces Aromas es una plataforma de comercio electrónico especializada en tortas, pasteles, cupcakes, galletas y postres. A diferencia de los pedidos por mensaje o llamada, permite ver un catálogo ordenado, personalizar productos, pagar con comprobante y seguir el pedido hasta la entrega, todo desde un mismo sistema.

## 3. Partes interesadas y usuarios

### 3.1 Partes interesadas

| Parte interesada | Interés en el sistema |
| --- | --- |
| Propietario de la pastelería | Ordenar los pedidos, vender más y relacionarse mejor con los clientes |
| Clientes | Pedir productos, pagar y seguir sus compras sin ir al local |
| Personal de la pastelería | Tener claros los pedidos, los pagos pendientes y las entregas |
| Equipo de desarrollo | Analizar, diseñar, construir y mantener el sistema |

### 3.2 Usuarios

Hay un usuario externo y tres roles de personal. Los roles de personal son fijos y el administrador edita sus permisos. Las funciones que antes se llamaban «encargado de inventario», «encargado de cobranza» o «encargado de distribución» son permisos que se asignan a un rol, no roles nuevos.

| Rol | Descripción | Cuenta |
| --- | --- | --- |
| Cliente | Compra desde la Tienda: consulta, pide, paga y sigue sus pedidos | Se registra él mismo |
| Administrador | Gestiona usuarios, permisos, catálogo, inventario, pagos y reportes | Lo crea otro administrador |
| Vendedor | Atiende pedidos y pagos, registra pedidos asistidos, asigna repartidores y marca la preparación si tiene el permiso | Lo crea el administrador |
| Repartidor | Ve y actualiza solo las entregas que se le asignaron | Lo crea el administrador |

### 3.3 Entorno del usuario

Los clientes usan la Tienda, una aplicación móvil hecha con Flutter y Dart. El personal usa el Panel, normalmente desde una computadora en el local o desde el celular en el caso de los repartidores. Ambas interfaces deben adaptarse a distintos tamaños de pantalla.

### 3.4 Perfil del cliente

- **Objetivos:** encontrar productos con facilidad, conocer precios y características, pedir sin ir al local, personalizar tortas y saber en qué estado está su pedido.
- **Problemas actuales:** no ve todos los productos disponibles, la información está repartida en varios medios y es difícil seguir un pedido.

### 3.5 Perfil del personal

- **Administrador:** quiere un catálogo actualizado, control sobre los pedidos, los pagos y los accesos, y datos de ventas. Hoy lo gestiona de forma manual y dispersa.
- **Vendedor:** quiere ver rápido qué pedidos y pagos están pendientes y evitar errores al tomar un pedido por teléfono.
- **Repartidor:** quiere saber qué pedidos debe entregar, con dirección y teléfono, sin ver información del resto del negocio.

## 4. Características del producto

### 4.1 Módulos

El sistema se organiza en cuatro módulos. Catálogo e inventario forman parte de E-commerce, porque ambos describen lo que la tienda vende.

| Módulo | Qué resuelve | Quién lo usa |
| --- | --- | --- |
| Seguridad | Cuentas, inicio de sesión, roles, permisos y auditoría | Todos |
| E-commerce | Catálogo, categorías, personalización, carrito, pedidos, inventario, disponibilidad, reportes y pedidos asistidos | Cliente, Administrador, Vendedor |
| Cobros | Registro de pagos con comprobante, verificación y su historial | Cliente, Administrador, Vendedor con permiso |
| Distribución | Entrega: asignación de repartidor, seguimiento y entregas fallidas | Vendedor, Repartidor, Administrador |

### 4.2 Dependencias entre módulos

| Módulo | Depende de | Para qué |
| --- | --- | --- |
| E-commerce | Seguridad | Identificar al cliente y validar los permisos del personal |
| Cobros | Seguridad, E-commerce | Saber quién paga y qué pedido se paga; al aprobar un pago, el pedido pasa a Confirmado |
| Distribución | Seguridad, E-commerce | Entregar pedidos ya confirmados y actualizar su estado |

Seguridad no depende de ningún otro módulo.

### 4.3 Estados del pedido y del pago

El pedido y el pago tienen cada uno su propio estado. El pedido pasa a Confirmado solo cuando su pago pasa a Pagado (decisión D1).

| Elemento | Estados |
| --- | --- |
| Pedido | Pendiente, Confirmado, En preparación, Listo, En camino, Entregado, Cancelado |
| Pago | Pendiente de pago, En verificación, Pagado, Rechazado |

En el recojo en tienda, el pedido pasa de Listo a Entregado sin pasar por En camino. Un pedido se cancela solo antes de En preparación. El reembolso, si corresponde, se hace fuera del sistema. La tabla completa de transiciones está en el documento de Casos de Uso.

### 4.4 Características por módulo

- **Seguridad:** registro de clientes, inicio y cierre de sesión, recuperación de contraseña (correo para clientes; el administrador la restablece al personal), bloqueo tras intentos fallidos, roles fijos con permisos editables y registro de cambios.
- **E-commerce:** catálogo con búsqueda y filtro, personalización con recargo fijo por opción, carrito, pedido con recojo o delivery (tarifa fija), disponibilidad manual por producto, inventario de insumos con alertas, cancelación y pedido asistido por el Vendedor.
- **Cobros:** pago por QR, transferencia o depósito con comprobante, verificación manual por personal con permiso, rechazo con observación e historial.
- **Distribución:** asignación de repartidor, vista propia del Repartidor, marcas En camino y Entregado, entrega fallida con observación e historial de estados.

## 5. Requisitos funcionales por módulo

Son 43 requisitos. Cada uno se detalla en un caso de uso del documento de Casos de Uso. Los IDs se conservan de la Visión v0 (RF01 a RF23) y se agregan RF24 a RF43.

### 5.1 Seguridad

| ID | Requisito | CU |
| --- | --- | --- |
| RF01 | El cliente puede registrarse con nombre, correo y contraseña | CU11 |
| RF02 | Los usuarios inician sesión con correo y contraseña; las cuentas desactivadas no ingresan | CU01 |
| RF03 | Los usuarios cierran sesión | CU01 |
| RF04 | El usuario consulta y actualiza sus datos y cambia su contraseña | CU13 |
| RF22 | El administrador crea, edita, activa y desactiva cuentas de personal | CU09 |
| RF24 | El usuario recupera su contraseña: por correo si es cliente; el administrador la restablece si es personal | CU12 |
| RF25 | El administrador edita los permisos de cada rol fijo | CU09 |
| RF26 | Cada usuario ve y ejecuta solo lo que permite su rol | CU01, CU09 |
| RF27 | El sistema bloquea temporalmente la cuenta tras intentos fallidos | CU01 |
| RF28 | El sistema registra quién cambió usuarios, roles y permisos, y cuándo | CU09 |

### 5.2 E-commerce

| ID | Requisito | CU |
| --- | --- | --- |
| RF05 | La tienda muestra los productos activos con su disponibilidad | CU04 |
| RF06 | El cliente busca productos por nombre | CU04 |
| RF07 | El cliente filtra productos por categoría | CU04 |
| RF08 | El cliente ve nombre, descripción, precio e imagen de cada producto | CU04 |
| RF09 | El cliente elige las opciones de personalización de los productos que las permiten | CU05 |
| RF10 | El cliente agrega productos al carrito | CU05 |
| RF11 | El cliente cambia cantidades o quita productos del carrito | CU05 |
| RF12 | El sistema calcula y muestra el total: (precio + recargos de personalización) por cantidad, más envío si es delivery | CU05 |
| RF13 | El cliente confirma el pedido eligiendo recojo o delivery, con los datos de entrega | CU05 |
| RF14 | El cliente consulta sus pedidos anteriores | CU15 |
| RF15 | El cliente ve el estado actual de cada pedido y de su pago | CU15 |
| RF16 | El administrador registra productos | CU02 |
| RF17 | El administrador edita productos, sus opciones de personalización y su disponibilidad | CU02 |
| RF18 | El administrador desactiva productos; no se eliminan si tienen pedidos | CU02 |
| RF19 | El administrador crea, edita y desactiva categorías | CU14 |
| RF20 | El personal autorizado consulta todos los pedidos | CU10 |
| RF21 | El personal autorizado avanza el estado del pedido según las transiciones permitidas | CU08 |
| RF23 | El administrador consulta pedidos y ventas filtrando por fecha, estado y método de pago | CU10 |
| RF29 | El personal autorizado registra entradas, salidas y ajustes de inventario con cantidad y motivo | CU03 |
| RF30 | El sistema alerta cuando un ítem llega a su mínimo | CU03 |
| RF31 | El administrador consulta el historial de movimientos de inventario | CU03 |
| RF32 | El sistema valida la disponibilidad indicada por producto antes de confirmar el pedido | CU05 |
| RF33 | El cliente o el personal cancelan un pedido hasta antes de su preparación | CU16 |
| RF34 | El vendedor registra un pedido a nombre de un cliente (venta por teléfono o mostrador) | CU17 |
| RF35 | El sistema suma la tarifa fija de envío a los pedidos con delivery | CU05 |

### 5.3 Cobros

| ID | Requisito | CU |
| --- | --- | --- |
| RF36 | El cliente registra el pago con método (QR, transferencia o depósito), referencia y comprobante | CU06 |
| RF37 | El personal con permiso aprueba o rechaza un pago; el rechazo exige observación | CU07 |
| RF38 | El sistema actualiza el estado del pago y del pedido según la decisión | CU07 |
| RF39 | El sistema guarda el historial de verificaciones | CU07 |

### 5.4 Distribución

| ID | Requisito | CU |
| --- | --- | --- |
| RF40 | El personal asigna un repartidor a cada pedido con delivery | CU08 |
| RF41 | El repartidor ve solo sus entregas y marca En camino y Entregado | CU08 |
| RF42 | El repartidor registra una entrega fallida con observación y el pedido vuelve a Listo | CU08 |
| RF43 | El sistema guarda el historial de estados de cada pedido | CU08 |

## 6. Requisitos no funcionales medibles

Cada requisito tiene un valor que se puede comprobar y una forma de verificarlo. Los valores son propuestas iniciales que el equipo y el docente pueden ajustar.

| ID | Categoría | Requisito medible | Verificación |
| --- | --- | --- | --- |
| RNF01 | Usabilidad | Un cliente nuevo completa un pedido de un producto sin personalizar, con recojo, en 5 pantallas o menos. Los errores de formulario nombran el campo y cómo corregirlo | Prueba con 3 personas sin ayuda |
| RNF02 | Rendimiento | Catálogo, búsqueda e inicio de sesión responden en 2 s o menos para el 95 % de las solicitudes. Carrito y total, 1 s. Cambio de estado o verificación de pago, 2 s. Reportes sobre 12 meses de pedidos, 5 s | Prueba de carga con 20 usuarios simultáneos y 500 pedidos de muestra |
| RNF03 | Contraseñas | Mínimo 8 caracteres con al menos una letra y un número | Prueba de validación |
| RNF04 | Credenciales | Contraseñas guardadas con hash y sal; nunca en texto plano ni en el código | Revisión de la base de datos |
| RNF05 | Bloqueo de cuenta | 5 intentos fallidos seguidos bloquean la cuenta 15 minutos y se registran | Prueba de aceptación |
| RNF06 | Sesión y transporte | Cierre automático tras 30 minutos de inactividad en el Panel. Todo el tráfico por HTTPS | Revisión de configuración |
| RNF07 | Autorización | Cada acción se valida en el servidor según el rol, no solo ocultando el menú. Un acceso no permitido devuelve «acceso denegado» y queda registrado | Prueba con URL directas por cada rol |
| RNF08 | Auditoría | Se registra usuario, acción y fecha en cambios de usuarios, roles, permisos y verificaciones de pago; se conserva 12 meses | Consulta del registro tras una prueba |
| RNF09 | Comprobantes de pago | Formatos JPG, PNG o PDF, hasta 5 MB | Prueba de carga de archivos |
| RNF10 | Imágenes de producto | JPG, PNG o WebP, cuadradas, desde 800 px, hasta 2 MB | Prueba de carga de archivos |
| RNF11 | Compatibilidad | Panel: últimas 2 versiones de Chrome, Edge, Firefox y Safari. Tienda: Android 8.0 (API 26) o superior | Prueba manual en cada navegador |
| RNF12 | Adaptabilidad | Tienda y Panel se usan sin scroll horizontal entre 360 y 1920 px de ancho | Prueba en 3 tamaños de pantalla |
| RNF13 | Disponibilidad | 99 % mensual dentro del horario de atención del negocio (lunes a sábado de 9:00 a 20:00 y domingo de 10:00 a 14:00, según la landing page) | Registro de caídas del hosting |
| RNF14 | Integridad de datos | Confirmar un pedido y actualizar la disponibilidad ocurre en una sola transacción; el stock nunca queda negativo. Copia de respaldo diaria con una restauración probada | Prueba de fallo a mitad de operación; ejercicio de restauración |
| RNF15 | Concurrencia | Dos usuarios no pueden aprobar el mismo pago ni vender la última unidad a la vez | Prueba con dos sesiones simultáneas |
| RNF16 | Privacidad | Nombre, teléfono y dirección del cliente solo los ven los roles que los necesitan; el Repartidor ve solo los de sus entregas | Prueba por rol |
| RNF17 | Mantenibilidad | Módulos separados con interfaces definidas (autenticación, productos, stock, pedidos). Cada regla de negocio tiene al menos una prueba automatizada | Revisión de código y de pruebas |
| RNF18 | Extensibilidad | Agregar una categoría o un método de pago no exige cambiar código | Prueba de alta desde el Panel |
| RNF19 | Formatos | Moneda en bolivianos con 2 decimales (Bs 120.00), fechas dd/mm/aaaa, zona horaria America/La\_Paz | Revisión en pantallas y reportes |
| RNF20 | Accesibilidad | Texto con contraste de 4.5:1 o más contra su fondo (3:1 en texto grande), en tema claro y oscuro. Todo control se usa con teclado y muestra foco visible. Las imágenes con significado tienen texto alternativo y los formularios tienen etiquetas | Medición de contraste en ambos temas; recorrido solo con teclado |
| RNF21 | Portabilidad | El Panel se usa desde el navegador sin instalar nada; la Tienda se instala como aplicación Flutter. El sistema se despliega desde el repositorio en un servicio en la nube y funciona con la configuración por variables de entorno, sin cambiar código entre equipos | Despliegue desde cero en una cuenta limpia siguiendo el README |

### 6.1 Lineamientos de interfaz, accesibilidad y portabilidad

La landing page del Laboratorio 08 (publicada en Render desde el repositorio del equipo) es la referencia visual de la Tienda y del Panel. El prototipo del Panel usa las mismas tipografías. La tabla dice qué se adopta y qué se corrige.

| Aspecto | Lineamiento | Estado en la landing |
| --- | --- | --- |
| Identidad | Nombre «Dulces Aromas» y lema «Momentos para compartir». Títulos en Fraunces y texto en Work Sans | Se adopta |
| Paleta clara | Fondo #FBF4EC, panel #FFFFFF, texto #3A2A23, café #4A342C, rosa #EFC3CE, #E19DB0 y #B96C86, éxito #5E8A63 | Se adopta, con las correcciones de contraste de abajo |
| Paleta oscura | Fondo #1E1714, panel #2D231E, texto #EDE0D6, rosa #E4A6BA, éxito #9CC3A0 | Se adopta |
| Tema | Sigue la preferencia del dispositivo y permite cambiarlo con un botón. El sistema funciona aunque el navegador no guarde la elección | Se adopta |
| Adaptación | Cambios de diseño en 920 px y 560 px; sin scroll horizontal entre 360 y 1920 px (RNF12) | Se adopta |
| Contraste medido | Texto principal 12.5:1 en tema claro y 13.7:1 en oscuro; texto secundario 5.0:1 y 6.9:1. Cumple 4.5:1 | Cumple |
| Contraste a corregir | En tema claro, las etiquetas rosa (#B96C86, 13 px) dan 3.5:1, los enlaces café (#8A6C5C) 4.4:1 y el verde de éxito 3.6:1. Hay que oscurecerlos hasta 4.5:1 | No cumple RNF20 |
| Teclado y foco | Todos los controles usan teclado y muestran foco visible. La landing solo define el foco visible en las opciones del personalizador | Falta ampliarlo |
| Movimiento | Respetar la preferencia de menos movimiento del dispositivo | Falta |
| Navegación | Enlace para saltar al contenido principal | Falta |
| Portabilidad | Una sola página estática sin instalación, publicada desde Git. La Tienda y el Panel siguen el mismo esquema (RNF21) | Se adopta |

La landing es una pieza de presentación y no forma parte del alcance de los documentos. Dice «desde Bs 10» para el delivery y que el costo depende de la zona, y promete avisar cuando el pedido esté listo; la tarifa es fija (D4) y los avisos automáticos están fuera del alcance. Esos textos deben ajustarse a la Visión antes de presentarla.

## 7. Restricciones del proyecto

- **Académicas:** el proyecto se desarrolla en la asignatura a cargo del Ing. Ramiro Duran, con un equipo de tres estudiantes y en los plazos del curso.
- **Metodología:** el desarrollo sigue Scrum; el Sprint 1 corresponde al módulo de Seguridad.
- **Plataforma:** la Tienda es una aplicación móvil en Flutter y Dart, como exige la materia, y el Panel es web. Ambas consumen una API REST.
- **Pagos:** el sistema registra y verifica comprobantes de forma manual. No se conecta a bancos ni a pasarelas de pago, y los reembolsos se hacen fuera del sistema.
- **Entrega:** el costo de envío es una tarifa fija; no hay cálculo por distancia ni rastreo en mapa.
- **Roles:** los cuatro roles son fijos; el administrador edita sus permisos pero no crea roles nuevos.
- **Alcance del negocio:** una sola pastelería, con una sola moneda (bolivianos) y una sola zona horaria.

## 8. Objetivo general y objetivos específicos

### 8.1 Objetivo general

Desarrollar una plataforma de comercio electrónico, con aplicación móvil y panel web, para la pastelería Dulces Aromas que permita al cliente comprar, pagar y seguir su pedido, y al personal gestionar catálogo, inventario, cobros y entregas desde un mismo sistema.

### 8.2 Objetivos específicos

| Objetivo | Resultado esperado | Requisitos |
| --- | --- | --- |
| Controlar el acceso de usuarios | Cuentas, roles fijos con permisos editables y registro de cambios | RF01–RF04, RF22, RF24–RF28 |
| Ofrecer una tienda en línea | Catálogo, personalización, carrito y pedido con recojo o delivery | RF05–RF15, RF32, RF35 |
| Administrar productos e inventario | Productos, categorías, disponibilidad e insumos con alertas | RF16–RF19, RF29–RF31 |
| Gestionar y atender pedidos | Consulta, avance de estado, cancelación, pedido asistido y reportes | RF20, RF21, RF23, RF33, RF34 |
| Verificar los cobros | Pago con comprobante, aprobación o rechazo e historial | RF36–RF39 |
| Organizar la entrega | Asignación de repartidor, seguimiento, entrega fallida e historial | RF40–RF43 |

## 9. Conclusión y control de cambios respecto a la Visión v0

Dulces Aromas reemplaza la gestión dispersa de pedidos por un solo sistema con una Tienda para clientes y un Panel para el personal. Esta versión unifica los documentos y conversaciones del equipo en un modelo único de roles, estados y reglas. Los Casos de Uso y las Historias de Usuario se alinean con él.

| Tema | Visión v0 | Visión v1 |
| --- | --- | --- |
| Módulos | Catálogo e inventario por separado | 4 módulos: Seguridad, E-commerce, Cobros, Distribución |
| Roles | Varios catálogos distintos entre documentos | Cliente, Administrador, Vendedor, Repartidor |
| Estados | Pedido y pago mezclados | Dos ciclos separados, con el pago aprobado antes de preparar |
| Requisitos funcionales | RF01–RF23 | RF01–RF43, agrupados por módulo |
| Requisitos no funcionales | Enunciados generales | RNF01–RNF21 con valor y forma de verificación |
| Pago | Sin definir | QR, transferencia o depósito con comprobante y verificación manual |
| Cliente en el sistema | Sin interfaz propia | Tienda con registro, pedido y seguimiento |
| Roles personalizados | Creación de roles | Roles fijos con permisos editables |
| Cancelación y entrega fallida | No contempladas | Cancelación antes de preparar y entrega fallida con observación |
