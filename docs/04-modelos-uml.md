# Dulces Aromas — Modelos UML v1

Versión 1.0 · 29/10/2026. Los diagramas originales están en el documento de Claude; aquí se reproducen en Mermaid para que GitHub los muestre.

## 1. Introducción

Este documento reúne los cuatro modelos UML de Dulces Aromas: casos de uso, clases, componentes y estados. Cada uno responde una pregunta distinta sobre el mismo sistema, y los cuatro usan los nombres, roles y reglas de la Visión v1, los Casos de Uso v1 y las Historias v1.

| Modelo | Pregunta que responde | Fuente principal |
| --- | --- | --- |
| Casos de uso | ¿Qué hace el sistema y para quién? | CU01 a CU17, actores |
| Clases | ¿Con qué objetos trabaja? | RF, RN, datos de los CU |
| Componentes | ¿De qué piezas se arma y cómo se comunican? | Módulos de la Visión, RNF17 |
| Estados | ¿Cómo reacciona a los eventos un pedido o un pago? | Sección 2 de los CU |

La notación es UML 2.5. Los nombres de clases, atributos y estados están en español, igual que el resto del proyecto.

## 2. Modelo de casos de uso

Son 17 casos de uso en cuatro módulos y cuatro roles. Un diagrama por módulo mantiene legibles las relaciones. Donde los cuatro roles hacen lo mismo se usa el actor abstracto Usuario, del que heredan Cliente, Administrador, Vendedor y Repartidor.

### 2.1 Seguridad

```mermaid
flowchart LR
  Usuario((Usuario))
  Cliente((Cliente)) -.->|hereda| Usuario
  Administrador((Administrador)) -.->|hereda| Usuario
  Vendedor((Vendedor)) -.->|hereda| Usuario
  Repartidor((Repartidor)) -.->|hereda| Usuario
  subgraph Seguridad
    CU01([CU01 Iniciar y cerrar sesión])
    CU12([CU12 Recuperar contraseña])
    CU13([CU13 Gestionar perfil])
    CU11([CU11 Registrarse como cliente])
    CU09([CU09 Gestionar usuarios y permisos])
  end
  Usuario --- CU01
  Usuario --- CU12
  Usuario --- CU13
  Cliente --- CU11
  Administrador --- CU09
  CU12 -.->|«extend»| CU01
```

Usuario inicia sesión (CU01), recupera su contraseña (CU12, que extiende a CU01 cuando la olvidó) y gestiona su perfil (CU13). Solo el Cliente se registra (CU11) y solo el Administrador gestiona usuarios y permisos (CU09).

### 2.2 E-commerce

```mermaid
flowchart LR
  Cliente((Cliente))
  Vendedor((Vendedor))
  Administrador((Administrador))
  subgraph E-commerce
    CU04([CU04 Consultar catálogo])
    CU05([CU05 Hacer un pedido])
    CU15([CU15 Consultar mis pedidos])
    CU16([CU16 Cancelar pedido])
    CU17([CU17 Registrar pedido asistido])
    CU10([CU10 Consultar pedidos y ventas])
    CU03([CU03 Gestionar inventario])
    CU02([CU02 Gestionar productos])
    CU14([CU14 Gestionar categorías])
  end
  Cliente --- CU04
  Cliente --- CU05
  Cliente --- CU15
  Cliente --- CU16
  Vendedor --- CU17
  Vendedor --- CU10
  Vendedor --- CU03
  Vendedor --- CU02
  Vendedor --- CU16
  Administrador --- CU10
  Administrador --- CU03
  Administrador --- CU02
  Administrador --- CU14
  Administrador --- CU16
  CU17 -.->|«include»| CU05
```
El Vendedor solo cambia la disponibilidad en CU02 y solo consulta en CU03.

El Cliente consulta el catálogo, hace pedidos, ve sus pedidos y los cancela. El Vendedor registra pedidos asistidos (CU17), que incluyen las reglas de CU05. El Administrador gestiona productos, categorías e inventario. Cancelar (CU16) lo hacen el Cliente y el personal con permiso de pedidos.

### 2.3 Cobros y Distribución

```mermaid
flowchart LR
  Cliente((Cliente))
  Administrador((Administrador))
  Vendedor((Vendedor))
  Repartidor((Repartidor))
  subgraph Cobros
    CU06([CU06 Registrar pago])
    CU07([CU07 Verificar pago])
  end
  subgraph Distribución
    CU08([CU08 Gestionar entrega])
  end
  Cliente --- CU06
  Vendedor --- CU06
  Administrador --- CU07
  Vendedor ---|con permiso| CU07
  Administrador --- CU08
  Vendedor --- CU08
  Repartidor --- CU08
```

El Cliente registra el pago (CU06); el Administrador o el Vendedor con permiso lo verifica (CU07). Con el pago aprobado, el personal prepara y asigna, y el Repartidor entrega (CU08).

### 2.4 Actores y casos de uso

| Caso de uso | Cliente | Administrador | Vendedor | Repartidor |
| --- | --- | --- | --- | --- |
| CU01 Iniciar y cerrar sesión | Sí | Sí | Sí | Sí |
| CU02 Gestionar productos | No | Sí | Solo disponibilidad | No |
| CU03 Gestionar inventario | No | Sí | Solo consulta | No |
| CU04 Consultar catálogo | Sí | Sí | Sí | No |
| CU05 Hacer un pedido | Sí | No | No | No |
| CU06 Registrar pago | Sí | No | Pedido asistido | No |
| CU07 Verificar pago | No | Sí | Con permiso | No |
| CU08 Gestionar entrega | No | Sí | Sí | Solo las suyas |
| CU09 Gestionar usuarios y permisos | No | Sí | No | No |
| CU10 Consultar pedidos y ventas | No | Sí | Solo pedidos | No |
| CU11 Registrarse como cliente | Sí | No | No | No |
| CU12 Recuperar contraseña | Sí | Sí | Sí | Sí |
| CU13 Gestionar perfil | Sí | Sí | Sí | Sí |
| CU14 Gestionar categorías | No | Sí | No | No |
| CU15 Consultar mis pedidos | Sí | No | No | No |
| CU16 Cancelar pedido | Sí | Sí | Sí | No |
| CU17 Registrar pedido asistido | No | Sí | Sí | No |

El Administrador puede hacer todo lo que hace el Vendedor; los diagramas dibujan solo las asociaciones principales de cada rol para no saturarlos, y esta tabla es la referencia completa.

## 3. Modelo de clases

El modelo se parte en cuatro diagramas, uno por módulo y el de E-commerce en dos, para que cada uno quepa en una pantalla. Las clases que aparecen en más de un diagrama se dibujan completas solo en el primero. Los tipos son Entero, Decimal, Texto, Lógico, Fecha y FechaHora; el signo «-» marca atributos privados y «+» métodos públicos.

### 3.1 Seguridad

```mermaid
classDiagram
  class Usuario {
    <<abstract>>
    -id: Entero
    -nombre: Texto
    -correo: Texto (único)
    -telefono: Texto
    -contrasenaHash: Texto
    -activo: Lógico
    -intentosFallidos: Entero
    -bloqueadoHasta: FechaHora
    +iniciarSesion()
    +cerrarSesion()
    +cambiarContrasena()
  }
  class Cliente {
    -direccionHabitual: Texto
    +registrarse()
  }
  class Personal {
    +tienePermiso(accion)
  }
  class Rol {
    -nombre: Texto
    +editarPermisos()
  }
  class Permiso {
    -modulo: Texto
    -accion: Texto
  }
  class Auditoria {
    -accion: Texto
    -detalle: Texto
    -fecha: FechaHora
  }
  Usuario <|-- Cliente
  Usuario <|-- Personal
  Personal "*" --> "1" Rol
  Rol "*" -- "*" Permiso : otorga
  Usuario "1" -- "*" Auditoria : registra
```

Cliente y Personal heredan de Usuario. El personal tiene un solo rol y cada rol reúne permisos; Auditoría guarda quién cambió usuarios, roles y permisos (RF28, RNF08). Un cliente nunca tiene rol de personal (RN16).

### 3.2 Catálogo e inventario

```mermaid
classDiagram
  class Categoria {
    -nombre: Texto
    -activa: Lógico
  }
  class Producto {
    -nombre: Texto
    -descripcion: Texto
    -precio: Decimal
    -imagen: Texto
    -activo: Lógico
    -disponible: Lógico
    -personalizable: Lógico
    +cambiarDisponibilidad()
    +desactivar()
  }
  class OpcionPersonalizacion {
    -tipo: Texto
    -nombre: Texto
    -recargo: Decimal
  }
  class ItemInventario {
    -nombre: Texto
    -unidad: Texto
    -stock: Decimal
    -minimo: Decimal
    +registrarMovimiento()
    +enMinimo()
  }
  class MovimientoInventario {
    -tipo: Texto
    -cantidad: Decimal
    -motivo: Texto
    -fecha: FechaHora
  }
  class Personal
  Categoria "1" -- "*" Producto : agrupa
  Producto "1" -- "*" OpcionPersonalizacion : ofrece
  ItemInventario "1" -- "*" MovimientoInventario : registra
  MovimientoInventario "*" --> "1" Personal : hecho por
```
La disponibilidad es un indicador manual del producto (D2); no se calcula con el stock de insumos.

Una categoría agrupa productos y un producto ofrece opciones de personalización, cada una con su recargo fijo (D9). El inventario controla insumos y sus movimientos, y cada movimiento lo registra una persona del personal (RN09, CU03).

### 3.3 Pedidos

```mermaid
classDiagram
  class Pedido {
    -fechaCreacion: FechaHora
    -estado: EstadoPedido
    -tipoEntrega: Texto
    -direccion: Texto
    -telefono: Texto
    -horarioEntrega: FechaHora
    -costoEnvio: Decimal
    -total: Decimal
    -esAsistido: Lógico
    -motivoCancelacion: Texto
    +calcularTotal()
    +confirmar()
    +cancelar(motivo)
    +cambiarEstado(estado)
  }
  class LineaPedido {
    -cantidad: Entero
    -precioUnitario: Decimal
    -mensaje: Texto (hasta 24)
    -subtotal: Decimal
  }
  class OpcionElegida {
    -nombre: Texto
    -recargo: Decimal
  }
  class HistorialEstado {
    -estadoAnterior: EstadoPedido
    -estadoNuevo: EstadoPedido
    -fecha: FechaHora
    -observacion: Texto
  }
  class Configuracion {
    -tarifaEnvio: Decimal
    -maxMensaje: Entero
  }
  class EstadoPedido {
    <<enumeration>>
    Pendiente
    Confirmado
    EnPreparacion
    Listo
    EnCamino
    Entregado
    Cancelado
  }
  class Cliente
  class Personal
  class Producto
  class Usuario
  Cliente "1" -- "*" Pedido : realiza
  Personal "0..1" -- "*" Pedido : registra
  Pedido "1" *-- "1..*" LineaPedido
  LineaPedido "*" --> "1" Producto
  LineaPedido "1" *-- "*" OpcionElegida
  Pedido "1" *-- "*" HistorialEstado
  HistorialEstado "*" --> "1" Usuario : hecho por
  Pedido ..> Configuracion : usa
```

Un cliente realiza muchos pedidos; un pedido asistido lo registra una persona del personal (RF34). El total se calcula con las líneas, los recargos de las opciones elegidas y la tarifa de envío de Configuración (RN11). Las opciones elegidas guardan una copia del nombre y del recargo, para que un cambio posterior en el producto no altere pedidos anteriores. Cada cambio de estado deja una fila en HistorialEstado (RF43).

### 3.4 Cobros y Distribución

```mermaid
classDiagram
  class Pago {
    -metodo: MetodoPago
    -referencia: Texto
    -monto: Decimal
    -comprobante: Archivo
    -estado: EstadoPago
    -fechaRegistro: FechaHora
    +registrar()
    +aprobar()
    +rechazar(observacion)
  }
  class Verificacion {
    -decision: Texto
    -observacion: Texto
    -fecha: FechaHora
  }
  class Entrega {
    -fechaAsignacion: FechaHora
    -fechaEntrega: FechaHora
    -observacionFallo: Texto
    +marcarEnCamino()
    +marcarEntregado()
    +registrarFallo(observacion)
  }
  class MetodoPago {
    <<enumeration>>
    QR
    Transferencia
    Deposito
  }
  class EstadoPago {
    <<enumeration>>
    PendienteDePago
    EnVerificacion
    Pagado
    Rechazado
  }
  class Pedido
  class Personal
  Pedido "1" -- "*" Pago : se paga con
  Pago "1" -- "*" Verificacion : tiene
  Verificacion "*" --> "1" Personal : verificada por
  Pedido "1" -- "0..1" Entrega : se entrega con
  Entrega "*" --> "1" Personal : repartidor
```

Un pedido puede tener varios pagos porque un comprobante rechazado obliga a registrar otro (RN07). Cada decisión queda en Verificacion con quién la tomó (RF39). La entrega existe solo para pedidos con delivery y apunta al repartidor asignado (RF40).

### 3.5 Diccionario de clases

| Clase | Módulo | Responsabilidad |
| --- | --- | --- |
| Usuario | Seguridad | Credenciales, estado de la cuenta y bloqueo por intentos |
| Cliente | Seguridad | Persona que compra desde la Tienda |
| Personal | Seguridad | Cuenta del Panel con un rol fijo |
| Rol | Seguridad | Administrador, Vendedor o Repartidor, con sus permisos |
| Permiso | Seguridad | Acción permitida sobre un módulo |
| Auditoría | Seguridad | Registro de cambios en usuarios, permisos y pagos |
| Categoría | E-commerce | Agrupa productos para el filtro del catálogo |
| Producto | E-commerce | Lo que se vende, con precio y disponibilidad |
| OpcionPersonalizacion | E-commerce | Tamaño, sabor, relleno o decoración con su recargo |
| ItemInventario | E-commerce | Insumo con stock y mínimo |
| MovimientoInventario | E-commerce | Entrada, salida o ajuste de un insumo |
| Pedido | E-commerce | Compra de un cliente con su estado y su entrega |
| LineaPedido | E-commerce | Producto, cantidad y mensaje dentro de un pedido |
| OpcionElegida | E-commerce | Copia de la opción y del recargo elegidos |
| HistorialEstado | E-commerce | Cada cambio de estado de un pedido |
| Configuracion | E-commerce | Tarifa fija de envío y largo máximo del mensaje |
| Pago | Cobros | Intento de pago con método, referencia y comprobante |
| Verificacion | Cobros | Aprobación o rechazo de un pago |
| Entrega | Distribución | Asignación, entrega o fallo de un pedido con delivery |

## 4. Modelo de componentes

El sistema tiene dos interfaces de usuario, cuatro componentes de negocio y tres recursos de soporte. Cada módulo ofrece interfaces y los demás las usan; ningún módulo accede a los datos de otro sin pasar por su interfaz (RNF17).

```mermaid
flowchart TB
  Tienda["«component» Tienda (Flutter, cliente)"]
  Panel["«component» Panel (personal)"]
  subgraph Interfaces
    IAut((IAutenticacion))
    IProd((IProductos))
    IStock((IStock))
    IPed((IPedidos))
    IPag((IPagos))
    IEnt((IEntregas))
  end
  Seg["«component» Seguridad"]
  Eco["«component» E-commerce"]
  Cob["«component» Cobros"]
  Dis["«component» Distribución"]
  BD[("«database» Base de datos")]
  Arch["«artifact» Imágenes y comprobantes"]
  Correo["«externo» Correo"]
  Tienda -.-> IAut & IProd & IPed & IPag
  Panel -.-> Interfaces
  Seg --- IAut
  Eco --- IProd & IStock & IPed
  Cob --- IPag
  Dis --- IEnt
  Eco -.->|requiere| IAut
  Cob -.->|requiere| IAut & IPed
  Dis -.->|requiere| IAut & IPed
  Seg --> BD
  Eco --> BD
  Cob --> BD
  Dis --> BD
  Eco --> Arch
  Cob --> Arch
  Seg -.-> Correo
```

La flecha discontinua va del componente que requiere hacia la interfaz que usa. Cobros llama a IPedidos para pasar el pedido a Confirmado cuando aprueba un pago, y Distribución la usa para leer pedidos listos y actualizar su estado. Esto corrige el diagrama anterior, donde las flechas entre Pedidos, Cobranza y Distribución iban en sentido contrario.

| Componente | Provee | Requiere |
| --- | --- | --- |
| Tienda | — | IAutenticacion, IProductos, IPedidos, IPagos |
| Panel | — | Las seis interfaces |
| Seguridad | IAutenticacion | Correo, Base de datos |
| E-commerce | IProductos, IStock, IPedidos | IAutenticacion, Base de datos, Imágenes |
| Cobros | IPagos | IAutenticacion, IPedidos, Base de datos, Comprobantes |
| Distribución | IEntregas | IAutenticacion, IPedidos, Base de datos |

## 5. Modelo de estados

El pedido y el pago tienen máquinas de estado separadas. Se conectan en un solo punto: aprobar el pago pasa el pedido de Pendiente a Confirmado (RN05, D1). Cada transición se escribe como evento \[guarda\] / acción.

### 5.1 Pedido

```mermaid
stateDiagram-v2
  [*] --> Pendiente : crear pedido (CU05 o CU17)
  Pendiente --> Confirmado : pago aprobado (CU07)
  Pendiente --> Cancelado : cancelar(motivo)
  Confirmado --> Cancelado : cancelar(motivo)
  Confirmado --> EnPreparacion : iniciar preparación [con permiso]
  EnPreparacion --> Listo : terminar preparación
  Listo --> EnCamino : sale con repartidor [delivery]
  Listo --> Entregado : cliente retira [recojo]
  EnCamino --> Entregado : confirmar entrega
  EnCamino --> Listo : entrega fallida [con observación]
  Entregado --> [*]
  Cancelado --> [*]
  EnPreparacion : En preparación
  EnCamino : En camino
```

El estado resaltado, Confirmado, es la puerta: sin él no hay preparación. Un pedido se cancela solo desde Pendiente o Confirmado (RN14). El recojo salta de Listo a Entregado sin pasar por En camino, y una entrega fallida devuelve el pedido a Listo con observación (RN13).

### 5.2 Pago

```mermaid
stateDiagram-v2
  [*] --> PendienteDePago : pedido creado
  PendienteDePago --> EnVerificacion : registrar comprobante (CU06)
  EnVerificacion --> Pagado : aprobar (CU07) / pedido pasa a Confirmado
  EnVerificacion --> Rechazado : rechazar [observación obligatoria]
  Rechazado --> PendienteDePago : reabrir cobro
  Pagado --> [*]
  PendienteDePago : Pendiente de pago
  EnVerificacion : En verificación
```

Un rechazo exige observación y reabre el cobro, así que un pedido puede acumular varios pagos hasta que uno se apruebe (RN07). Aprobar un pago es el único evento que cambia el estado de otro objeto, el pedido.

## 6. Consistencia entre modelos

Los cuatro modelos usan los mismos nombres. Esta tabla muestra dónde aparece cada elemento clave en cada modelo y qué regla lo sostiene.

| Elemento | Casos de uso | Clases | Componentes | Estados | Regla o requisito |
| --- | --- | --- | --- | --- | --- |
| Roles fijos | Actores Cliente, Administrador, Vendedor, Repartidor | Usuario, Cliente, Personal, Rol, Permiso | Seguridad, IAutenticacion | Guardas \[con permiso\] | RN16, D6 |
| Pedido | CU05, CU15, CU16, CU17 | Pedido, LineaPedido, HistorialEstado | E-commerce, IPedidos | Máquina del Pedido | RF13, RF43 |
| Pago antes de preparar | CU06, CU07 | Pago, Verificacion | Cobros requiere IPedidos | Pagado lleva el pedido a Confirmado | RN05, D1 |
| Rechazo de pago | CU07 alt. 1 y 2 | Verificacion.observacion | Cobros | Rechazado a Pendiente de pago | RN07 |
| Cancelación | CU16 | Pedido.motivoCancelacion | IPedidos | Desde Pendiente o Confirmado | RN14, D8 |
| Entrega fallida | CU08 alt. 2 | Entrega.observacionFallo | Distribución, IEntregas | En camino a Listo | RN13 |
| Recojo | CU08 | Pedido.tipoEntrega | Distribución | Listo a Entregado | RN12 |
| Total y envío | CU05 | Pedido.calcularTotal(), Configuracion.tarifaEnvio, OpcionElegida | E-commerce | — | RN11, D4, D9 |
| Mensaje de la torta | CU05 alt. 3b | LineaPedido.mensaje, Configuracion.maxMensaje | E-commerce | — | RN17 |
| Disponibilidad manual | CU02, CU04 | Producto.disponible (sin relación con ItemInventario) | IProductos, IStock separadas | — | RN08, D2 |
| Auditoría | CU07, CU09 | Auditoría, Verificacion | Seguridad | — | RF28, RNF08 |

Dos supuestos de modelado que el equipo puede cambiar: un pedido tiene varios pagos (uno por intento) y la entrega es una clase aparte que solo existe para pedidos con delivery.
