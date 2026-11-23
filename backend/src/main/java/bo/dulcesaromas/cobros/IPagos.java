package bo.dulcesaromas.cobros;

import java.math.BigDecimal;
import java.time.Instant;
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
	 * El cliente paga con tarjeta de débito en el tótem: el procesador aprueba el cobro y el sistema lo verifica solo,
	 * sin comprobante ni revisión manual. El pago queda Pagado y el pedido Confirmado en la misma transacción (RN05).
	 */
	PagoVista pagarConTarjeta(Long pedidoId, UsuarioActual cliente);

	/**
	 * Abre un cobro por QR para el pedido: un código secreto de un solo uso que vence a los 15 minutos. El QR lleva a
	 * la pasarela de pagos con ese código.
	 */
	CobroQrVista crearCobroQr(Long pedidoId, UsuarioActual cliente);

	/**
	 * Lo que muestra la pasarela de un cobro: el comercio, el pedido y el monto, sin datos del cliente.
	 */
	CobroPasarela cobroEnPasarela(String codigo);

	/**
	 * La pasarela confirma el pago: el sistema lo verifica solo y el pedido queda Confirmado (RN05).
	 */
	CobroPasarela confirmarEnPasarela(String codigo);

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

	record CobroQrVista(String codigo, Long pedidoId, BigDecimal monto, Instant vence) {
	}

	/**
	 * @param estado PENDIENTE, PAGADO, VENCIDO o CERRADO (el pedido ya no espera este pago)
	 */
	record CobroPasarela(String comercio, Long pedidoId, BigDecimal monto, Instant vence, String estado,
			String referencia) {
	}

}
