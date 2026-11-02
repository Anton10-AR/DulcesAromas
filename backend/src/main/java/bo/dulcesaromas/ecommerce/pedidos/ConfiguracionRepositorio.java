package bo.dulcesaromas.ecommerce.pedidos;

import org.springframework.data.jpa.repository.JpaRepository;

interface ConfiguracionRepositorio extends JpaRepository<Configuracion, Short> {

	default Configuracion actual() {
		return findById(Configuracion.ID).orElseThrow();
	}

}
