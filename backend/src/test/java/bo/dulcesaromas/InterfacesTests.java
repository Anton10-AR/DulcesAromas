package bo.dulcesaromas;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import bo.dulcesaromas.cobros.IPagos;
import bo.dulcesaromas.distribucion.IEntregas;
import bo.dulcesaromas.ecommerce.IPedidos;
import bo.dulcesaromas.ecommerce.IProductos;
import bo.dulcesaromas.ecommerce.IStock;
import bo.dulcesaromas.seguridad.IAutenticacion;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las seis interfaces del modelo de componentes (sección 4 de los Modelos UML) existen, y cada parte del sistema usa
 * la interfaz de la otra en vez de sus clases internas (RNF17).
 */
class InterfacesTests {

	private final JavaClasses clases = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
		.importPackages("bo.dulcesaromas");

	@Test
	void lasSeisInterfacesDelModeloDeComponentesEstanEnLaRaizDeSuModulo() {
		assertThat(IAutenticacion.class.getPackageName()).isEqualTo("bo.dulcesaromas.seguridad");
		assertThat(IProductos.class.getPackageName()).isEqualTo("bo.dulcesaromas.ecommerce");
		assertThat(IStock.class.getPackageName()).isEqualTo("bo.dulcesaromas.ecommerce");
		assertThat(IPedidos.class.getPackageName()).isEqualTo("bo.dulcesaromas.ecommerce");
		assertThat(IPagos.class.getPackageName()).isEqualTo("bo.dulcesaromas.cobros");
		assertThat(IEntregas.class.getPackageName()).isEqualTo("bo.dulcesaromas.distribucion");
	}

	/**
	 * Catálogo, inventario y pedidos están en el mismo módulo, así que Spring Modulith no los separa: lo hace esta
	 * prueba. Pedidos ve los productos solo por IProductos.
	 */
	@Test
	void pedidosNoUsaLasClasesInternasDelCatalogoNiDelInventario() {
		noClasses().that().resideInAPackage("..ecommerce.pedidos..")
			.should().dependOnClassesThat().resideInAnyPackage("..ecommerce.catalogo..", "..ecommerce.inventario..")
			.check(clases);
	}

	/**
	 * Los controladores REST de inventario, pagos y entregas atienden a la Tienda y al Panel a través de la interfaz de
	 * su módulo, no del servicio que la implementa.
	 */
	@Test
	void losControladoresUsanLaInterfazDeSuModulo() {
		noClasses().that().areAnnotatedWith(RestController.class)
			.should().dependOnClassesThat()
			.haveFullyQualifiedName("bo.dulcesaromas.ecommerce.inventario.InventarioServicio")
			.orShould().dependOnClassesThat().haveFullyQualifiedName("bo.dulcesaromas.cobros.pagos.PagoServicio")
			.orShould().dependOnClassesThat().haveFullyQualifiedName("bo.dulcesaromas.distribucion.entregas.EntregaServicio")
			.check(clases);
	}

}
