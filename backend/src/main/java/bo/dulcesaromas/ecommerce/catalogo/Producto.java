package bo.dulcesaromas.ecommerce.catalogo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import bo.dulcesaromas.compartido.Textos;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * Lo que se vende. {@code activo} decide si existe en la Tienda; {@code disponible} es el indicador manual de si
 * hoy se puede pedir (D2). Un producto no se borra: se desactiva y conserva sus pedidos (RN10).
 */
@Entity
@Table(name = "producto")
class Producto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "categoria_id")
	private Categoria categoria;

	private String nombre;

	private String nombreBusqueda;

	private String descripcion;

	private BigDecimal precio;

	private boolean activo;

	private boolean disponible;

	private boolean personalizable;

	private Instant imagenActualizada;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "producto_id", nullable = false)
	@OrderColumn(name = "orden")
	private List<OpcionPersonalizacion> opciones = new ArrayList<>();

	protected Producto() {
	}

	Producto(Categoria categoria, String nombre, String descripcion, BigDecimal precio, boolean disponible,
			boolean personalizable, List<OpcionPersonalizacion> opciones) {
		this.activo = true;
		editar(categoria, nombre, descripcion, precio, disponible, personalizable, opciones);
	}

	void editar(Categoria categoria, String nombre, String descripcion, BigDecimal precio, boolean disponible,
			boolean personalizable, List<OpcionPersonalizacion> opciones) {
		this.categoria = categoria;
		this.nombre = Textos.limpio(nombre);
		this.nombreBusqueda = Textos.clave(nombre);
		this.descripcion = descripcion.strip();
		this.precio = precio;
		this.disponible = disponible;
		this.personalizable = personalizable;
		this.opciones.clear();
		this.opciones.addAll(opciones);
	}

	void cambiarDisponibilidad(boolean disponible) {
		this.disponible = disponible;
	}

	void activar() {
		activo = true;
	}

	void desactivar() {
		activo = false;
	}

	void imagenCambiada(Instant cuando) {
		imagenActualizada = cuando;
	}

	/**
	 * Se ve en la Tienda: está activo y su categoría también.
	 */
	public boolean esVisible() {
		return activo && categoria.isActiva();
	}

	public Long getId() {
		return id;
	}

	public Categoria getCategoria() {
		return categoria;
	}

	public String getNombre() {
		return nombre;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public BigDecimal getPrecio() {
		return precio;
	}

	public boolean isActivo() {
		return activo;
	}

	public boolean isDisponible() {
		return disponible;
	}

	public boolean isPersonalizable() {
		return personalizable;
	}

	public Instant getImagenActualizada() {
		return imagenActualizada;
	}

	public List<OpcionPersonalizacion> getOpciones() {
		return opciones;
	}

}
