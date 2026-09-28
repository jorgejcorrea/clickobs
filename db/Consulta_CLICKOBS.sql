-- Consulta correspondiente a CU-02 "Comparar costos entre herramientas"
SELECT
    H.nombre AS 'Herramienta',
    S.nombre AS 'Servicio',
    T.nombre AS 'Tribu',
    C.periodo AS 'Periodo',
    C.cantidad_usada AS 'Cantidad Usada',
    C.unidad_medida AS 'Unidad',
    C.monto_usd AS 'Costo (USD)'
FROM
    consumo C,
    herramienta H,
    servicio S,
    tribu T
WHERE
    C.id_herramienta = H.id_herramienta
    AND C.id_servicio = S.id_servicio
    AND S.id_tribu = T.id_tribu
ORDER BY
    C.periodo, H.nombre;
