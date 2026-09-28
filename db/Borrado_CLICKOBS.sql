-- Se desactiva temporalmente el modo Safe Updates de MySQL Workbench,
-- que bloquea los DELETE sin WHERE sobre columna clave.
SET SQL_SAFE_UPDATES = 0;

-- Orden de borrado respetando las dependencias de FOREIGN KEY (hijas antes que padres)
DELETE FROM recomendacion_ia;
DELETE FROM umbral_alerta;
DELETE FROM consumo;
DELETE FROM conector;
DELETE FROM usuario;
DELETE FROM servicio;
DELETE FROM herramienta;
DELETE FROM tribu;
DELETE FROM rol_permiso;

SET SQL_SAFE_UPDATES = 1;

SELECT * FROM recomendacion_ia;
SELECT * FROM umbral_alerta;
SELECT * FROM consumo;
SELECT * FROM conector;
SELECT * FROM usuario;
SELECT * FROM servicio;
SELECT * FROM herramienta;
SELECT * FROM tribu;
SELECT * FROM rol_permiso;
