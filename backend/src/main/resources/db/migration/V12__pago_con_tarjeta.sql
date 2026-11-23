-- Pago con tarjeta de débito en el tótem, verificado automáticamente por un procesador de pagos (feria EXPOSOFT 26).
-- El procesador es una simulación: nunca se leen ni se guardan datos de tarjetas.

-- Un método automático no lleva referencia ni comprobante del cliente: lo aprueba el procesador. Como los demás,
-- nace desactivado: el administrador lo activa en Configuración.
alter table metodo_pago add column automatico boolean not null default false;

insert into metodo_pago (nombre, nombre_clave, instrucciones, activo, automatico) values
	('Tarjeta de débito', 'tarjeta de debito',
	 'Acerca o inserta tu tarjeta de débito en el lector. (Procesador de demostración: no se cobra dinero real.)', false, true);

-- La verificación de un pago automático la hace el sistema, no una persona.
alter table verificacion alter column verificador_id drop not null;
