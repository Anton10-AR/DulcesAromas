package bo.dulcesaromas;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Comprueba que cada módulo use solo los módulos permitidos y solo su API pública.
 */
class ModularidadTests {

	private final ApplicationModules modulos = ApplicationModules.of(DulcesAromasApplication.class);

	@Test
	void losModulosRespetanSusDependencias() {
		modulos.verify();
	}

}
