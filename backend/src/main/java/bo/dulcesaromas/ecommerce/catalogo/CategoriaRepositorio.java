package bo.dulcesaromas.ecommerce.catalogo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface CategoriaRepositorio extends JpaRepository<Categoria, Long> {

	List<Categoria> findAllByOrderByNombreAsc();

	List<Categoria> findByActivaTrueOrderByNombreAsc();

	boolean existsByNombreClave(String nombreClave);

	boolean existsByNombreClaveAndIdNot(String nombreClave, Long id);

}
