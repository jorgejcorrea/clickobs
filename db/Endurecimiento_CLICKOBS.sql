-- CLICKOBS - Endurecimiento del modelo (reglas de negocio e integridad)
-- Se ejecuta DESPUES de CreacionTablas_CLICKOBS.sql (e idealmente antes de Insert_CLICKOBS.sql).
-- No cambia las columnas ni los datos de prueba: agrega restricciones que antes solo cumplia la aplicacion.

-- R1. Un consumo por herramienta, servicio y periodo (evita duplicar costos al reingestar).
ALTER TABLE consumo
    ADD CONSTRAINT uq_consumo_herr_serv_periodo UNIQUE (id_herramienta, id_servicio, periodo);

-- R2. El periodo tiene formato AAAA-MM y los montos y cantidades no son negativos.
ALTER TABLE consumo
    ADD CONSTRAINT ck_consumo_periodo  CHECK (periodo REGEXP '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    ADD CONSTRAINT ck_consumo_monto    CHECK (monto_usd >= 0),
    ADD CONSTRAINT ck_consumo_cantidad CHECK (cantidad_usada >= 0);

-- R3. Un umbral de alerta y un conector por herramienta (relacion 1 a 1 del dominio).
ALTER TABLE umbral_alerta
    ADD CONSTRAINT uq_umbral_herramienta UNIQUE (id_herramienta),
    ADD CONSTRAINT ck_umbral_monto CHECK (monto_maximo > 0);
ALTER TABLE conector
    ADD CONSTRAINT uq_conector_herramienta UNIQUE (id_herramienta),
    ADD CONSTRAINT ck_conector_estado CHECK (estado IN ('Activo', 'Inactivo', 'Error'));

-- R4. Las claves foraneas no pueden quedar nulas donde la relacion es obligatoria.
ALTER TABLE consumo       MODIFY id_herramienta INT NOT NULL, MODIFY id_servicio INT NOT NULL,
                          MODIFY periodo VARCHAR(20) NOT NULL, MODIFY monto_usd DECIMAL(12,2) NOT NULL;
ALTER TABLE umbral_alerta MODIFY id_herramienta INT NOT NULL, MODIFY monto_maximo DECIMAL(12,2) NOT NULL;
ALTER TABLE conector      MODIFY id_herramienta INT NOT NULL;
ALTER TABLE servicio      MODIFY id_tribu INT NOT NULL;
ALTER TABLE usuario       MODIFY id_rol INT NOT NULL, MODIFY id_tribu INT NOT NULL;

-- R5. Todas las consultas de la aplicacion filtran por periodo: se agrega un indice para esa busqueda.
CREATE INDEX ix_consumo_periodo ON consumo (periodo);
