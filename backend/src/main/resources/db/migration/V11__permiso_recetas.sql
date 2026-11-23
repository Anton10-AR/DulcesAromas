-- Permiso para consultar las recetas aproximadas sin editar el catálogo (D12). Lo usa el Repostero en Cocina, para
-- saber qué insumos lleva cada pedido. De inicio lo tienen el administrador y el Repostero.
insert into rol_permiso (rol, permiso) values
	('ADMINISTRADOR', 'CATALOGO_RECETAS'),
	('REPOSTERO', 'CATALOGO_RECETAS');
