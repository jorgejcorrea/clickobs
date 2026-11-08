-- CLICKOBS - Datos de DEMOSTRACION (opcional): consumo historico de mayo a julio de 2026.
-- Sirve para que el grafico 'Costo por mes' del dashboard muestre mas de un mes. No modifica agosto.
-- Ejecutar DESPUES de Insert_CLICKOBS.sql. Para quitarlos: DELETE FROM consumo WHERE id_consumo > 5;

INSERT INTO consumo (id_consumo, id_herramienta, id_servicio, periodo, cantidad_usada, unidad_medida, monto_usd)
VALUES
    (6, 1, 4, '2026-05', 975.00, 'DDU', 2496.00),
    (7, 2, 1, '2026-05', 265.20, 'hosts', 1638.00),
    (8, 3, 2, '2026-05', 764.40, 'GB almacenados', 663.00),
    (9, 4, 3, '2026-05', 163.80, 'GB indexados', 499.20),
    (10, 1, 1, '2026-05', 234.00, 'DDU', 608.40),
    (11, 1, 4, '2026-06', 1075.00, 'DDU', 2752.00),
    (12, 2, 1, '2026-06', 292.40, 'hosts', 1806.00),
    (13, 3, 2, '2026-06', 842.80, 'GB almacenados', 731.00),
    (14, 4, 3, '2026-06', 180.60, 'GB indexados', 550.40),
    (15, 1, 1, '2026-06', 258.00, 'DDU', 670.80),
    (16, 1, 4, '2026-07', 1162.50, 'DDU', 2976.00),
    (17, 2, 1, '2026-07', 316.20, 'hosts', 1953.00),
    (18, 3, 2, '2026-07', 911.40, 'GB almacenados', 790.50),
    (19, 4, 3, '2026-07', 195.30, 'GB indexados', 595.20),
    (20, 1, 1, '2026-07', 279.00, 'DDU', 725.40);
