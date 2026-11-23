-- Pago por QR verificado automáticamente (feria EXPOSOFT 26). Cada cobro tiene un código secreto de un solo uso que
-- vence a los 15 minutos; el QR lleva a la pasarela de pagos simulada, que al confirmar avisa al sistema y el pago se
-- aprueba solo. No se mueve dinero real.

-- Por qué canal se cobra un método automático: con tarjeta en el lector o con un QR por cobro.
alter table metodo_pago add column canal varchar(10);
update metodo_pago set canal = 'TARJETA' where automatico;
update metodo_pago set canal = 'QR', automatico = true where nombre_clave = 'qr';

create table cobro_qr (
	codigo  varchar(64) primary key,
	pago_id bigint      not null references pago (id),
	creado  timestamp with time zone not null,
	vence   timestamp with time zone not null,
	-- Cuándo se confirmó en la pasarela; nulo mientras espera.
	usado   timestamp with time zone
);

create index cobro_qr_pago on cobro_qr (pago_id);
