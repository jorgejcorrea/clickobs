INSERT INTO tribu (id_tribu, nombre, lead_responsable)
VALUES
    (1, 'Infraestructura', 'Head de Infraestructura'),
    (2, 'Pagos', 'Tech Lead Pagos'),
    (3, 'Core Bancario', 'Tech Lead Core Bancario'),
    (4, 'Mobile', 'Tech Lead Mobile');


INSERT INTO rol_permiso (id_rol, nombre, descripcion)
VALUES
    (1, 'CTO', 'Acceso a recomendaciones IA y reportes gerenciales'),
    (2, 'SRE Lead', 'Consulta y comparacion de costos, administracion de conectores'),
    (3, 'DevOps', 'Consulta de consumo por herramienta'),
    (4, 'Finanzas', 'Configuracion de alertas y exportacion de reportes');


INSERT INTO usuario (id_usuario, nombre, id_rol, id_tribu)
VALUES
    (1, 'Diego Farias', 1, 1),
    (2, 'Referente SRE Infraestructura', 2, 1),
    (3, 'Referente DevOps Pagos', 3, 2),
    (4, 'Referente Finanzas', 4, 1);


INSERT INTO servicio (id_servicio, nombre, id_tribu, criticidad)
VALUES
    (1, 'API de pagos', 2, 'Alta'),
    (2, 'Core de cuentas', 3, 'Alta'),
    (3, 'App mobile', 4, 'Media'),
    (4, 'Gateway de infraestructura', 1, 'Alta');


INSERT INTO herramienta (id_herramienta, nombre, proveedor, modelo_facturacion)
VALUES
    (1, 'Dynatrace', 'Dynatrace LLC', 'DDU'),
    (2, 'Datadog', 'Datadog Inc', 'Host/Metrica custom'),
    (3, 'Grafana+Prometheus', 'Self-hosted', 'Infraestructura propia'),
    (4, 'Elastic', 'Elastic NV', 'Almacenamiento');


INSERT INTO conector (id_conector, id_herramienta, endpoint_api, estado)
VALUES
    (1, 1, 'https://api.dynatrace.com/v2/metrics', 'Activo'),
    (2, 2, 'https://api.datadoghq.com/api/v2/usage', 'Activo'),
    (3, 3, 'http://prometheus.clickobs.internal:9090/api/v1', 'Activo'),
    (4, 4, 'https://api.elastic-cloud.com/api/v1/billing', 'Activo');


INSERT INTO consumo (id_consumo, id_herramienta, id_servicio, periodo, cantidad_usada, unidad_medida, monto_usd)
VALUES
    (1, 1, 4, '2026-08', 1250.00, 'DDU', 3200.00),
    (2, 2, 1, '2026-08', 340.00, 'hosts', 2100.00),
    (3, 3, 2, '2026-08', 980.00, 'GB almacenados', 850.00),
    (4, 4, 3, '2026-08', 210.00, 'GB indexados', 640.00),
    (5, 1, 1, '2026-08', 300.00, 'DDU', 780.00);


INSERT INTO umbral_alerta (id_umbral, id_herramienta, monto_maximo, periodicidad)
VALUES
    (1, 1, 3500.00, 'Mensual'),
    (2, 2, 2500.00, 'Mensual'),
    (3, 4, 700.00, 'Mensual');


INSERT INTO recomendacion_ia (id_recomendacion, id_herramienta, fecha, modelo_usado, descripcion, ahorro_estimado)
VALUES
    (1, 1, '2026-09-01 10:00:00', 'modelo-economico', 'El servicio Gateway de infraestructura tiene cobertura parcial redundante con Datadog', 900.00),
    (2, 2, '2026-09-01 10:05:00', 'modelo-robusto', 'La tribu de Pagos podria reducir hosts monitoreados en horarios de bajo trafico', 450.00);
