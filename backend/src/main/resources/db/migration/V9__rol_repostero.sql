-- Rol fijo Repostero: prepara los pedidos (decisión D3, revisada el 13/11/2026). Ve en Cocina lo que hay que hacer,
-- marca En preparación y Listo, y consulta los insumos. No cobra, no cancela ni ve datos personales del cliente.
insert into rol (codigo) values ('REPOSTERO');

insert into rol_permiso (rol, permiso) values
	('REPOSTERO', 'INICIO_PENDIENTES'),
	('REPOSTERO', 'PEDIDOS_PREPARAR'),
	('REPOSTERO', 'INVENTARIO_CONSULTAR');
