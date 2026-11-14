"""Carga datos de demostración en una base recién creada, a través de la API.

Usa la API (no SQL) para que las contraseñas queden con BCrypt y se cumplan las mismas reglas que en el uso real.

Requisitos:
- La API corriendo, con la base vacía (solo el administrador inicial).
- Python 3 con Pillow (pip install pillow), para preparar las imágenes de los productos.
- Variables (del entorno o de backend/.env): ADMIN_CORREO, ADMIN_CONTRASENA y DEMO_CONTRASENA, la contraseña de
  todas las cuentas de demostración (mínimo 8 caracteres, con letra y número).

Uso, desde backend/:
    python demo/cargar_demo.py [--api http://localhost:8080]

Los productos que tienen foto en app/assets/postres/ la usan; los demás llevan una ilustración con su inicial. Para
poner las fotos en una base que ya tiene el catálogo (por ejemplo, la de la nube), sin tocar nada más:
    python demo/cargar_demo.py --fotos --api https://...

Para sumar a una base que ya tiene catálogo las categorías y los productos de este script que todavía no existen
(por nombre), sin tocar los demás, los pedidos ni las cuentas:
    python demo/cargar_demo.py --ampliar --api https://...
"""
import argparse
import io
import json
import os
import sys
import urllib.error
import urllib.request
import uuid
from datetime import datetime, timedelta, timezone

from PIL import Image, ImageDraw, ImageFont

LA_PAZ = timezone(timedelta(hours=-4))
CARPETA = os.path.dirname(os.path.abspath(__file__))
POSTRES = os.path.join(CARPETA, '..', '..', 'app', 'assets', 'postres')

# Paleta de la marca (CLAUDE.md, «Interfaz»).
CREMA, CAFE, ROSAS = '#FBF4EC', '#4A342C', ['#EFC3CE', '#E19DB0', '#B96C86']


def leer_variables():
    variables = {}
    archivo = os.path.join(CARPETA, '..', '.env')
    if os.path.exists(archivo):
        for linea in open(archivo, encoding='utf-8'):
            linea = linea.strip()
            if linea and not linea.startswith('#') and '=' in linea:
                clave, valor = linea.split('=', 1)
                variables[clave.strip()] = valor.strip()
    variables.update(os.environ)
    faltan = [v for v in ('ADMIN_CORREO', 'ADMIN_CONTRASENA', 'DEMO_CONTRASENA') if not variables.get(v)]
    if faltan:
        sys.exit('Faltan variables: ' + ', '.join(faltan) + '. Defínelas en backend/.env o en el entorno.')
    return variables


class Api:
    def __init__(self, base):
        self.base = base.rstrip('/')

    def llamar(self, metodo, ruta, token=None, cuerpo=None, archivos=None, campos=None):
        cabeceras = {}
        datos = None
        if token:
            cabeceras['Authorization'] = 'Bearer ' + token
        if archivos is not None:
            limite = uuid.uuid4().hex
            partes = io.BytesIO()
            for nombre, valor in (campos or {}).items():
                partes.write(f'--{limite}\r\nContent-Disposition: form-data; name="{nombre}"\r\n\r\n{valor}\r\n'.encode())
            for nombre, (archivo, contenido, tipo) in archivos.items():
                partes.write(f'--{limite}\r\nContent-Disposition: form-data; name="{nombre}"; filename="{archivo}"\r\n'
                             f'Content-Type: {tipo}\r\n\r\n'.encode())
                partes.write(contenido)
                partes.write(b'\r\n')
            partes.write(f'--{limite}--\r\n'.encode())
            datos = partes.getvalue()
            cabeceras['Content-Type'] = 'multipart/form-data; boundary=' + limite
        elif cuerpo is not None:
            datos = json.dumps(cuerpo).encode()
            cabeceras['Content-Type'] = 'application/json'
        solicitud = urllib.request.Request(self.base + ruta, data=datos, headers=cabeceras, method=metodo)
        try:
            with urllib.request.urlopen(solicitud) as respuesta:
                texto = respuesta.read().decode()
                return json.loads(texto) if texto else None
        except urllib.error.HTTPError as e:
            sys.exit(f'{metodo} {ruta} respondió {e.code}: {e.read().decode()}')
        except urllib.error.URLError as e:
            sys.exit(f'No se pudo conectar con la API en {self.base}: {e.reason}')

    def get(self, ruta, token=None):
        return self.llamar('GET', ruta, token)

    def post(self, ruta, token=None, cuerpo=None):
        return self.llamar('POST', ruta, token, cuerpo if cuerpo is not None else {})

    def put(self, ruta, token, cuerpo):
        return self.llamar('PUT', ruta, token, cuerpo)

    def login(self, correo, contrasena):
        return self.post('/api/auth/login', cuerpo={'correo': correo, 'contrasena': contrasena})['token']


def fuente(tamano):
    for nombre in ('georgiab.ttf', 'georgia.ttf', 'DejaVuSerif-Bold.ttf'):
        for carpeta in ('C:/Windows/Fonts', '/usr/share/fonts/truetype/dejavu', '/Library/Fonts'):
            ruta = os.path.join(carpeta, nombre)
            if os.path.exists(ruta):
                return ImageFont.truetype(ruta, tamano)
    return ImageFont.load_default()


def imagen_producto(nombre, indice):
    """Una ilustración sencilla y cuadrada (RNF10): un plato con el nombre del producto."""
    lado = 800
    img = Image.new('RGB', (lado, lado), CREMA)
    d = ImageDraw.Draw(img)
    rosa = ROSAS[indice % len(ROSAS)]
    d.ellipse((110, 90, 690, 670), fill=rosa)
    d.ellipse((190, 170, 610, 590), fill=CREMA, outline=CAFE, width=6)
    letra = fuente(170)
    inicial = nombre[0].upper()  # La inicial del nombre: «Alfajores de maicena» lleva una A.
    d.text((400, 380), inicial, font=letra, fill=CAFE, anchor='mm')
    texto = fuente(46)
    lineas, actual = [], ''
    for palabra in nombre.split():
        prueba = (actual + ' ' + palabra).strip()
        if d.textlength(prueba, font=texto) > 700 and actual:
            lineas.append(actual)
            actual = palabra
        else:
            actual = prueba
    lineas.append(actual)
    y = 715 - 28 * (len(lineas) - 1)
    for linea in lineas:
        d.text((400, y), linea, font=texto, fill=CAFE, anchor='mm')
        y += 56
    salida = io.BytesIO()
    img.save(salida, 'PNG', optimize=True)
    return salida.getvalue()


def foto_producto(nombre):
    """La foto del producto, cuadrada y de 1000 px (RNF10 pide 800 o más), o None si no tiene."""
    archivo = FOTOS.get(nombre)
    if not archivo:
        return None
    img = Image.open(os.path.join(POSTRES, archivo)).convert('RGB')
    lado = min(img.size)
    x, y = (img.width - lado) // 2, (img.height - lado) // 2
    img = img.crop((x, y, x + lado, y + lado)).resize((1000, 1000), Image.LANCZOS)
    salida = io.BytesIO()
    img.save(salida, 'JPEG', quality=88, optimize=True)
    return salida.getvalue()


def subir_imagen(api, admin, producto, indice):
    foto = foto_producto(producto['nombre'])
    if foto:
        archivo = (f'producto-{producto["id"]}.jpg', foto, 'image/jpeg')
    else:
        archivo = (f'producto-{producto["id"]}.png', imagen_producto(producto['nombre'], indice), 'image/png')
    api.llamar('PUT', f"/api/productos/{producto['id']}/imagen", admin, archivos={'archivo': archivo})


def comprobante(referencia, monto):
    img = Image.new('RGB', (600, 800), 'white')
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 600, 110), fill=CAFE)
    d.text((300, 55), 'Comprobante de pago', font=fuente(40), fill='white', anchor='mm')
    texto = fuente(30)
    for i, linea in enumerate(['DATOS DE DEMOSTRACIÓN', f'Referencia: {referencia}', f'Monto: Bs {monto}',
                               'Destino: Dulces Aromas']):
        d.text((50, 180 + i * 70), linea, font=texto, fill=CAFE)
    salida = io.BytesIO()
    img.save(salida, 'PNG')
    return salida.getvalue()


def horario(dias, hora, minuto=0):
    """Un horario de entrega dentro de la atención y con más de 24 horas de anticipación."""
    dia = datetime.now(LA_PAZ).date() + timedelta(days=dias)
    if dia.weekday() == 6:  # domingo: de 10:00 a 14:00
        hora = min(max(hora, 10), 13)
    return datetime(dia.year, dia.month, dia.day, hora, minuto, tzinfo=LA_PAZ).isoformat()


# --- Datos ---------------------------------------------------------------------------------------------------------

OPCIONES_TORTA = [
    ('Tamaño', 'Mediana (12 porciones)', '0.00'),
    ('Tamaño', 'Grande (20 porciones)', '60.00'),
    ('Relleno', 'Dulce de leche', '0.00'),
    ('Relleno', 'Frutilla', '10.00'),
    ('Relleno', 'Ganache de chocolate', '15.00'),
]

CATALOGO = {
    'Tortas': [
        ('Torta de chocolate', 'Bizcocho húmedo de cacao con cobertura de ganache. Ideal para cumpleaños.', '120.00', True),
        ('Torta tres leches', 'Bizcocho bañado en tres leches con merengue suave y canela.', '110.00', True),
        ('Torta selva negra', 'Capas de chocolate, crema chantilly y cerezas.', '140.00', True),
        ('Torta de frutilla y crema', 'Bizcocho de vainilla con frutillas frescas y crema batida.', '130.00', True),
        ('Torta moka', 'Bizcocho de café con crema de moka y virutas de chocolate.', '130.00', True),
        ('Torta de zanahoria', 'Bizcocho de zanahoria y nuez con frosting de queso crema.', '115.00', True),
        ('Torta red velvet', 'Bizcocho rojo aterciopelado con frosting de queso crema.', '140.00', True),
        ('Torta helada de frutilla', 'Bizcocho, crema chantilly y gelatina de frutilla, servida fría.', '120.00', True),
        ('Torta tres chocolates', 'Capas de mousse de chocolate amargo, con leche y blanco.', '150.00', True),
    ],
    'Postres': [
        ('Cheesecake de maracuyá', 'Base de galleta, crema de queso y salsa de maracuyá. Entero, para 10 porciones.',
         '90.00', False),
        ('Tiramisú en vaso', 'Porción individual con café, cacao y crema de mascarpone.', '18.00', False),
        ('Pie de limón', 'Masa quebrada, crema de limón y merengue tostado. Entero, para 8 porciones.', '75.00', False),
        ('Flan casero', 'Porción individual de flan de vainilla con caramelo.', '12.00', False),
        ('Mousse de maracuyá', 'Porción individual de mousse de maracuyá con su salsa.', '15.00', False),
        ('Brownie con nuez', 'Brownie húmedo de chocolate con nuez. La unidad.', '10.00', False),
        ('Gelatina con crema', 'Vaso de gelatina de frutilla con crema batida.', '8.00', False),
        ('Cheesecake de frutos rojos', 'Base de galleta, crema de queso y salsa de frutos rojos. Entero, para 10 porciones.',
         '95.00', False),
        ('Pie de manzana', 'Masa quebrada con manzana y canela. Entero, para 8 porciones.', '70.00', False),
    ],
    'Galletas y masitas': [
        ('Alfajores de maicena', 'Docena de alfajores rellenos de dulce de leche y coco rallado.', '30.00', False),
        ('Galletas de avena y chispas', 'Docena de galletas de avena con chispas de chocolate.', '25.00', False),
        ('Masitas de nata', 'Bolsa de 20 masitas de nata, como las de la abuela.', '20.00', False),
        ('Cocadas', 'Bolsa de 12 cocadas doradas de coco rallado.', '18.00', False),
        ('Galletas de chocolate y nuez', 'Docena de galletas de chocolate con trozos de nuez.', '28.00', False),
        ('Empanaditas de manzana', 'Docena de empanaditas de masa hojaldrada rellenas de manzana.', '30.00', False),
        ('Buñuelos con miel', 'Seis buñuelos recién fritos con miel de caña.', '15.00', False),
    ],
    'Panes dulces': [
        ('Rollos de canela', 'Caja de 4 rollos con glaseado de queso crema.', '28.00', False),
        ('Budín de naranja', 'Budín esponjoso con glaseado de naranja. Para 8 porciones.', '35.00', False),
        ('Panetón navideño', 'Panetón artesanal con frutas confitadas. Solo en temporada.', '65.00', False),
        ('Pan de plátano', 'Budín de plátano maduro con nuez. Para 8 porciones.', '32.00', False),
        ('Medialunas', 'Seis medialunas de manteca con almíbar.', '18.00', False),
        ('Rosca de anís', 'Rosca de pan dulce con anís y azúcar. La unidad.', '25.00', False),
    ],
    'Cupcakes y porciones': [
        ('Cupcakes de vainilla', 'Caja de 6 cupcakes de vainilla con crema de mantequilla.', '42.00', False),
        ('Cupcakes red velvet', 'Caja de 6 cupcakes red velvet con frosting de queso crema.', '48.00', False),
        ('Porción de torta de chocolate', 'Una porción de nuestra torta de chocolate con ganache.', '14.00', False),
        ('Donas glaseadas', 'Caja de 6 donas con glaseado de azúcar y chispas de colores.', '36.00', False),
    ],
    'Bebidas': [
        ('Café americano', 'Café de los Yungas, filtrado. Vaso de 12 onzas.', '10.00', False),
        ('Capuchino', 'Espresso con leche espumada y un toque de canela.', '14.00', False),
        ('Chocolate caliente', 'Chocolate espeso hecho con cacao boliviano.', '12.00', False),
        ('Api morado', 'Api de maíz morado con canela y clavo, bien caliente.', '8.00', False),
    ],
    'Para eventos': [
        ('Bocaditos dulces', 'Caja de 50 bocaditos surtidos: alfajores, trufas, tartaletas y masitas.', '180.00', False),
        ('Mesa de postres', 'Selección de postres para unas 30 personas, con montaje en el lugar.', '650.00', False),
        ('Torta de bodas por pisos', 'Torta por pisos decorada a pedido. Se coordina el diseño por teléfono.', '900.00',
         True),
    ],
}

# Opciones de los productos personalizables que no son una torta común.
OPCIONES_ESPECIALES = {
    'Torta de bodas por pisos': [
        ('Tamaño', 'Dos pisos (40 porciones)', '0.00'),
        ('Tamaño', 'Tres pisos (70 porciones)', '400.00'),
        ('Relleno', 'Dulce de leche', '0.00'),
        ('Relleno', 'Frutos rojos', '80.00'),
    ],
}

# Fotos de app/assets/postres/. Las de las tortas y las masitas son de Wikimedia Commons; sus autores y licencias
# están en app/assets/postres/CREDITOS.md.
FOTOS = {
    'Torta de chocolate': 'torta-chocolate.jpg',
    'Torta tres leches': 'torta-tres-leches.jpg',
    'Torta selva negra': 'torta-selva-negra.jpg',
    'Torta de frutilla y crema': 'torta-frutilla.jpg',
    'Masitas de nata': 'masitas-nata.jpg',
    'Cheesecake de maracuyá': 'tarta-maracuya.jpg',
    'Tiramisú en vaso': 'tiramisu-vaso.jpg',
    'Pie de limón': 'pie-limon.jpg',
    'Flan casero': 'flan.jpg',
    'Alfajores de maicena': 'alfajores.jpg',
    'Galletas de avena y chispas': 'galletas-chispas.jpg',
    'Rollos de canela': 'rollos-canela.jpg',
    'Budín de naranja': 'budin-naranja.jpg',
    'Torta moka': 'torta-moka.jpg',
    'Torta de zanahoria': 'torta-zanahoria.jpg',
    'Torta red velvet': 'torta-red-velvet.jpg',
    'Torta helada de frutilla': 'torta-helada.jpg',
    'Torta tres chocolates': 'torta-tres-chocolates.jpg',
    'Mousse de maracuyá': 'mousse-maracuya.jpg',
    'Brownie con nuez': 'brownie-nuez.jpg',
    'Gelatina con crema': 'gelatina-crema.jpg',
    'Cheesecake de frutos rojos': 'cheesecake-frutos-rojos.jpg',
    'Pie de manzana': 'pie-manzana.jpg',
    'Cocadas': 'cocadas.jpg',
    'Galletas de chocolate y nuez': 'galletas-chocolate-nuez.jpg',
    'Empanaditas de manzana': 'empanaditas-manzana.jpg',
    'Buñuelos con miel': 'bunuelos.jpg',
    'Pan de plátano': 'pan-platano.jpg',
    'Medialunas': 'medialunas.jpg',
    'Rosca de anís': 'rosca-anis.jpg',
    'Cupcakes de vainilla': 'cupcakes-vainilla.jpg',
    'Cupcakes red velvet': 'cupcakes-red-velvet.jpg',
    'Porción de torta de chocolate': 'porcion-torta-chocolate.jpg',
    'Donas glaseadas': 'donas.jpg',
    'Café americano': 'cafe-americano.jpg',
    'Capuchino': 'capuchino.jpg',
    'Chocolate caliente': 'chocolate-caliente.jpg',
    'Api morado': 'api-morado.jpg',
    'Bocaditos dulces': 'bocaditos.jpg',
    'Mesa de postres': 'mesa-postres.jpg',
    'Torta de bodas por pisos': 'torta-bodas.jpg',
}

INSUMOS = [  # nombre, unidad, mínimo, compra inicial, consumo
    ('Harina de trigo', 'kg', '10', '25', '6'),
    ('Azúcar', 'kg', '8', '18', '4'),
    ('Huevos', 'unidades', '60', '180', '45'),
    ('Mantequilla', 'kg', '5', '8', '4'),
    ('Leche', 'litros', '6', '15', '5'),
    ('Chocolate de cobertura', 'kg', '3', '4', '2'),
    ('Crema de leche', 'litros', '4', '10', '3'),
    ('Frutillas', 'kg', '2', '5', '1.5'),
]

PERSONAL = [
    ('Carla Mendoza Rojas', 'carla.mendoza@dulcesaromas.bo', '71123456', 'VENDEDOR'),
    ('Diego Flores Quispe', 'diego.flores@dulcesaromas.bo', '72234567', 'VENDEDOR'),
    ('Marco Vargas Llanos', 'marco.vargas@dulcesaromas.bo', '73345678', 'REPARTIDOR'),
    ('Rosa Cruz Mamani', 'rosa.cruz@dulcesaromas.bo', '74456789', 'REPARTIDOR'),
    ('Ana Quispe Flores', 'ana.quispe@dulcesaromas.bo', '75567890', 'REPOSTERO'),
]

# Los correos de clientes usan example.com, un dominio reservado: nunca llega un correo a nadie real.
CLIENTES = [
    ('Lucía Torrez Arancibia', 'lucia.torrez@example.com', '70112233', 'Calle Bolívar 345, zona Central'),
    ('Andrés Salinas Padilla', 'andres.salinas@example.com', '70223344', 'Av. Las Américas 1220, zona San Matías'),
    ('María Fernanda Gutiérrez', 'mafer.gutierrez@example.com', '70334455', 'Calle Junín 78, a media cuadra del mercado'),
    ('Jorge Paredes Antezana', 'jorge.paredes@example.com', '70445566', 'Av. Hernando Siles 560, edificio Los Pinos'),
]


def opciones_de(nombre, personalizable):
    if not personalizable:
        return []
    return [{'tipo': t, 'nombre': n, 'recargo': r} for t, n, r in OPCIONES_ESPECIALES.get(nombre, OPCIONES_TORTA)]


def crear_producto(api, admin, categoria_id, producto, indice):
    nombre, descripcion, precio, personalizable = producto
    p = api.post('/api/productos', admin, {
        'nombre': nombre, 'descripcion': descripcion, 'precio': precio, 'categoriaId': categoria_id,
        'disponible': True, 'personalizable': personalizable, 'opciones': opciones_de(nombre, personalizable),
    })
    subir_imagen(api, admin, p, indice)
    return p


def ampliar(api, admin):
    """Suma las categorías y los productos de CATALOGO que la base todavía no tiene, comparando por nombre."""
    categorias = {c['nombre']: c['id'] for c in api.get('/api/categorias', admin)}
    existentes = {p['nombre'] for p in api.get('/api/productos', admin)}
    nuevas, nuevos = 0, 0
    for categoria, lista in CATALOGO.items():
        if categoria not in categorias:
            categorias[categoria] = api.post('/api/categorias', admin, {'nombre': categoria})['id']
            nuevas += 1
        for indice, producto in enumerate(lista):
            if producto[0] in existentes:
                continue
            crear_producto(api, admin, categorias[categoria], producto, indice)
            print('  ' + producto[0])
            nuevos += 1
    print(f'Listo: {nuevas} categorías y {nuevos} productos nuevos.')


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('--api', default='http://localhost:8080')
    parser.add_argument('--fotos', action='store_true',
                        help='solo pone las fotos a los productos que ya existen; no carga nada más')
    parser.add_argument('--ampliar', action='store_true',
                        help='suma las categorías y los productos que faltan; no toca los demás datos')
    args = parser.parse_args()
    var = leer_variables()
    clave = var['DEMO_CONTRASENA']
    api = Api(args.api)

    admin = api.login(var['ADMIN_CORREO'], var['ADMIN_CONTRASENA'])
    if args.ampliar:
        ampliar(api, admin)
        return
    if args.fotos:
        con_foto = [p for p in api.get('/api/productos', admin) if p['nombre'] in FOTOS]
        for p in con_foto:
            subir_imagen(api, admin, p, 0)
            print('  ' + p['nombre'])
        print(f'Listo: {len(con_foto)} productos con foto.')
        return
    if api.get('/api/productos', admin) or api.get('/api/categorias', admin):
        sys.exit('La base ya tiene catálogo. Este script es para una base recién creada; no se cargó nada.')

    print('Métodos de pago…')
    instrucciones = {
        'Transferencia': 'Banco de ejemplo · Cuenta 1000-0000-0000 · Titular: Dulces Aromas (dato de demostración). '
                         'Escribe tu número de pedido en la glosa.',
        'Depósito': 'Banco de ejemplo · Cuenta 1000-0000-0000 · Titular: Dulces Aromas (dato de demostración).',
    }
    metodos = {}
    for m in api.get('/api/metodos-pago/gestion', admin):
        if m['nombre'] in instrucciones:
            api.put(f"/api/metodos-pago/{m['id']}", admin, {'nombre': m['nombre'], 'instrucciones': instrucciones[m['nombre']]})
            api.post(f"/api/metodos-pago/{m['id']}/activar", admin)
            metodos[m['nombre']] = m['id']

    print('Catálogo…')
    productos = {}
    indice = 0
    for categoria, lista in CATALOGO.items():
        categoria_id = api.post('/api/categorias', admin, {'nombre': categoria})['id']
        for producto in lista:
            productos[producto[0]] = crear_producto(api, admin, categoria_id, producto, indice)
            indice += 1
    # RN08: un producto agotado hoy; D2: la disponibilidad es manual.
    api.put(f"/api/productos/{productos['Pie de limón']['id']}/disponibilidad", admin, {'disponible': False})
    # RN10: un producto de temporada desactivado; no se ve en la Tienda.
    api.post(f"/api/productos/{productos['Panetón navideño']['id']}/desactivar", admin)

    print('Inventario…')
    for nombre, unidad, minimo, compra, consumo in INSUMOS:
        item = api.post('/api/inventario', admin, {'nombre': nombre, 'unidad': unidad, 'minimo': minimo})
        api.post(f"/api/inventario/{item['id']}/movimientos", admin,
                 {'tipo': 'ENTRADA', 'cantidad': compra, 'motivo': 'Compra inicial al proveedor'})
        api.post(f"/api/inventario/{item['id']}/movimientos", admin,
                 {'tipo': 'SALIDA', 'cantidad': consumo, 'motivo': 'Producción de la semana'})

    print('Personal…')
    for nombre, correo, telefono, rol in PERSONAL:
        api.post('/api/personal', admin,
                 {'nombre': nombre, 'correo': correo, 'telefono': telefono, 'rol': rol, 'contrasenaInicial': clave})
    carla = api.login('carla.mendoza@dulcesaromas.bo', clave)
    marco = api.login('marco.vargas@dulcesaromas.bo', clave)
    repartidores = {r['nombre']: r['id'] for r in api.get('/api/entregas/repartidores', admin)}

    print('Clientes…')
    clientes = []
    for nombre, correo, telefono, direccion in CLIENTES:
        token = api.post('/api/clientes', cuerpo={'nombre': nombre, 'correo': correo, 'contrasena': clave})['token']
        api.put('/api/perfil', token, {'nombre': nombre, 'telefono': telefono, 'direccionHabitual': direccion})
        clientes.append((token, telefono, direccion))
    lucia, andres, mafer, jorge = clientes

    def linea(producto, cantidad=1, opciones=(), mensaje=None):
        p = productos[producto]
        ids = [o['id'] for o in p['opciones'] if o['nombre'] in opciones]
        return {'productoId': p['id'], 'cantidad': cantidad, 'opciones': ids, 'mensaje': mensaje}

    def pedido(cliente, lineas, delivery, cuando):
        token, telefono, direccion = cliente
        cuerpo = {'lineas': lineas, 'tipoEntrega': 'DELIVERY' if delivery else 'RECOJO',
                  'direccion': direccion if delivery else None, 'telefono': telefono, 'horarioEntrega': cuando}
        return api.post('/api/pedidos', token, cuerpo)

    def pagar(token, p, metodo='Transferencia'):
        referencia = f"OP-{p['id']:05d}{uuid.uuid4().hex[:4].upper()}"
        return api.llamar('POST', '/api/pagos', token,
                          campos={'pedidoId': p['id'], 'metodoPagoId': metodos[metodo], 'referencia': referencia},
                          archivos={'comprobante': ('comprobante.png', comprobante(referencia, p['total']), 'image/png')})

    def pagado(cliente, p):
        api.post(f"/api/pagos/{pagar(cliente[0], p)['id']}/aprobar", admin)

    def avanzar(p, *pasos, repartidor=None):
        for paso in pasos:
            if paso == 'repartidor':
                api.put(f"/api/entregas/{p['id']}/repartidor", admin, {'repartidorId': repartidores[repartidor]})
            elif paso in ('en-preparacion', 'listo', 'retirado'):
                api.post(f"/api/pedidos/{p['id']}/{paso}", admin)
            elif paso == 'en-camino':
                api.post(f"/api/entregas/{p['id']}/en-camino", admin)
            elif paso == 'entregado':
                api.post(f"/api/entregas/{p['id']}/entregado", marco)
            elif paso == 'fallida':
                api.post(f"/api/entregas/{p['id']}/fallida", marco,
                         {'observacion': 'No había nadie en el domicilio; el teléfono no contestó.'})

    print('Pedidos…')
    # Entregado por delivery, con personalización y mensaje (RN11, RN17).
    p = pedido(lucia, [linea('Torta de chocolate', 1, ['Grande (20 porciones)', 'Ganache de chocolate'],
                             'Feliz cumple Sofi')], True, horario(2, 11))
    pagado(lucia, p)
    avanzar(p, 'repartidor', 'en-preparacion', 'listo', 'en-camino', 'entregado', repartidor='Marco Vargas Llanos')
    # Recojo entregado: de Listo a Entregado sin pasar por En camino.
    p = pedido(andres, [linea('Alfajores de maicena', 2), linea('Rollos de canela')], False, horario(2, 16))
    pagado(andres, p)
    avanzar(p, 'en-preparacion', 'listo', 'retirado')
    # En camino.
    p = pedido(mafer, [linea('Torta tres leches', 1, ['Mediana (12 porciones)', 'Dulce de leche'])], True, horario(3, 10))
    pagado(mafer, p)
    avanzar(p, 'repartidor', 'en-preparacion', 'listo', 'en-camino', repartidor='Marco Vargas Llanos')
    # Entrega fallida: vuelve a Listo con observación (RN13).
    p = pedido(jorge, [linea('Masitas de nata', 2)], True, horario(3, 15))
    pagado(jorge, p)
    avanzar(p, 'repartidor', 'en-preparacion', 'listo', 'en-camino', 'fallida', repartidor='Marco Vargas Llanos')
    # Listo para recoger.
    p = pedido(jorge, [linea('Cheesecake de maracuyá')], False, horario(3, 18))
    pagado(jorge, p)
    avanzar(p, 'en-preparacion', 'listo')
    # En preparación.
    p = pedido(lucia, [linea('Galletas de avena y chispas', 3)], True, horario(4, 12))
    pagado(lucia, p)
    avanzar(p, 'repartidor', 'en-preparacion', repartidor='Rosa Cruz Mamani')
    # Confirmado, con repartidor asignado.
    p = pedido(andres, [linea('Torta selva negra', 1, ['Grande (20 porciones)', 'Frutilla'], 'Felices 50 años')],
               True, horario(5, 17))
    pagado(andres, p)
    avanzar(p, 'repartidor', repartidor='Rosa Cruz Mamani')
    # Pago en verificación.
    p = pedido(mafer, [linea('Tiramisú en vaso', 4)], False, horario(4, 10))
    pagar(mafer[0], p, 'Depósito')
    # Pago rechazado: vuelve a Pendiente de pago (RN07).
    p = pedido(jorge, [linea('Torta de frutilla y crema', 1, ['Mediana (12 porciones)', 'Frutilla'])], True,
               horario(6, 11))
    pago = pagar(jorge[0], p)
    api.post(f"/api/pagos/{pago['id']}/rechazar", admin,
             {'observacion': 'El monto del comprobante no coincide con el total del pedido.'})
    # Pendiente, sin pago todavía.
    pedido(lucia, [linea('Flan casero', 6)], False, horario(5, 11))
    # Cancelado antes de prepararse (RN14).
    p = pedido(andres, [linea('Budín de naranja')], False, horario(6, 16))
    api.post(f"/api/pedidos/{p['id']}/cancelar", andres[0], {'motivo': 'Cambié de fecha; vuelvo a pedir la próxima semana.'})
    # Pedido asistido por teléfono (D7): lo registra Carla, así que ella no puede verificar su pago (RN06).
    elena = api.post('/api/clientes/asistidos', carla,
                     {'nombre': 'Elena Sánchez Vda. de Rojas', 'correo': 'elena.sanchez@example.com', 'telefono': '64512345'})
    p = api.post('/api/pedidos/asistidos', carla, {'clienteId': elena['id'], 'pedido': {
        'lineas': [linea('Torta tres leches', 1, ['Grande (20 porciones)', 'Dulce de leche'], 'Bodas de oro')],
        'tipoEntrega': 'RECOJO', 'direccion': None, 'telefono': '64512345', 'horarioEntrega': horario(4, 15)}})
    pagar(carla, p, 'Depósito')

    print()
    print('Listo. Todas las cuentas de demostración usan la contraseña de DEMO_CONTRASENA.')
    print('  Vendedores:   carla.mendoza@dulcesaromas.bo, diego.flores@dulcesaromas.bo')
    print('  Repartidores: marco.vargas@dulcesaromas.bo, rosa.cruz@dulcesaromas.bo')
    print('  Repostera:    ana.quispe@dulcesaromas.bo')
    print('  Clientes:     ' + ', '.join(c[1] for c in CLIENTES))


if __name__ == '__main__':
    main()
