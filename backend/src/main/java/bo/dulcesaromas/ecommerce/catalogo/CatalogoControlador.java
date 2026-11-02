package bo.dulcesaromas.ecommerce.catalogo;

import java.time.Duration;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogo de la Tienda (CU04, HU07). Se consulta sin iniciar sesión (RN15) y solo muestra productos activos de
 * categorías activas, disponibles o no.
 */
@RestController
@RequestMapping("/api/catalogo")
class CatalogoControlador {

	private final ProductoServicio productos;

	private final CategoriaServicio categorias;

	CatalogoControlador(ProductoServicio productos, CategoriaServicio categorias) {
		this.productos = productos;
		this.categorias = categorias;
	}

	@GetMapping("/categorias")
	List<ProductoVista.CategoriaResumen> categorias() {
		return categorias.listarActivas().stream()
			.map(c -> new ProductoVista.CategoriaResumen(c.getId(), c.getNombre()))
			.toList();
	}

	@GetMapping("/productos")
	List<ProductoVista> productos(@RequestParam(required = false) String buscar,
			@RequestParam(required = false) Long categoria) {
		return productos.buscar(buscar, categoria, true).stream().map(ProductoVista::de).toList();
	}

	@GetMapping("/productos/{id}")
	ProductoVista producto(@PathVariable Long id) {
		return ProductoVista.de(productos.obtenerVisible(id));
	}

	/**
	 * La ruta lleva la versión de la imagen ({@code ?v=}), así que puede guardarse en caché por mucho tiempo.
	 */
	@GetMapping("/productos/{id}/imagen")
	ResponseEntity<byte[]> imagen(@PathVariable Long id) {
		return productos.imagen(id)
			.map(imagen -> ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(imagen.getTipoContenido()))
				.cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
				.body(imagen.getDatos()))
			.orElse(ResponseEntity.notFound().build());
	}

}
