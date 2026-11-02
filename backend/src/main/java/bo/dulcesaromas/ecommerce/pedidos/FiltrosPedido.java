package bo.dulcesaromas.ecommerce.pedidos;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import bo.dulcesaromas.ecommerce.EstadoPedido;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

final class FiltrosPedido {

	private FiltrosPedido() {
	}

	/**
	 * Pedidos en alguno de los estados y creados en [desde, hasta). Un filtro nulo no se aplica.
	 */
	static Specification<Pedido> de(Collection<EstadoPedido> estados, Instant desde, Instant hasta) {
		return (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			if (estados != null) {
				condiciones.add(raiz.get("estado").in(estados));
			}
			if (desde != null) {
				condiciones.add(cb.greaterThanOrEqualTo(raiz.get("fechaCreacion"), desde));
			}
			if (hasta != null) {
				condiciones.add(cb.lessThan(raiz.get("fechaCreacion"), hasta));
			}
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
	}

}
