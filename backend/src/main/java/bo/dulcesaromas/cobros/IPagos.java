package bo.dulcesaromas.cobros;

import java.math.BigDecimal;
import java.util.List;

import bo.dulcesaromas.seguridad.UsuarioActual;

/**
 * Lo que Cobros ofrece: registrar el pago de un pedido (CU06, HU11) y verificarlo (CU07, HU12). Aprobar un pago
 * confirma el pedido en la misma transacción (RN05).
 */
public interface IPagos {

	/**
	 * Todos los pagos de un pedido, del primero al último; el último es el vigente. El cliente solo ve los de sus
	 * pedidos.
	 */
	List<PagoVista> dePedido(Long pedidoId, UsuarioActual usuario);

	/**
	 * El pago pasa a En verificación. En un pedido asistido lo registra el personal con permiso (CU17).
	 * @param comprobante JPG, PNG o PDF de hasta 5 MB (RNF09)
	 */
	PagoVista registrar(Long pedidoId, Long metodoPagoId, String referencia, byte[] comprobante, UsuarioActual usuario);

	/**
	 * El estado del cobro de cada pedido del cliente, para Mis pedidos (HU19).
	 */
	List<Cobro> deCliente(Long clienteId);

	/**
	 * Los pagos que esperan verificación, del más antiguo al más reciente.
	 */
	List<PagoVista> enVerificacion();

	/**
	 * Quien registró un pedido asistido no verifica su pago (RN06), y dos personas no deciden el mismo pago a la vez
	 * (RNF15).
	 */
	PagoVista aprobar(Long pagoId, UsuarioActual verificador);

	/**
	 * El rechazo exige observación y reabre el cobro con un pago nuevo Pendiente de pago (RN07).
	 */
	PagoVista rechazar(Long pagoId, String observacion, UsuarioActual verificador);

	ArchivoComprobante comprobante(Long pagoId, UsuarioActual usuario);

	/**
	 * @param observacionRechazo por qué rechazaron el pago anterior, si el cobro se reabrió por eso
	 */
	record Cobro(Long pedidoId, Long pagoId, EstadoPago estado, BigDecimal monto, String observacionRechazo) {
	}

	record ArchivoComprobante(String tipoContenido, byte[] datos) {
	}

}
