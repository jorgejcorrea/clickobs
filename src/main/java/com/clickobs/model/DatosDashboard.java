package com.clickobs.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Datos del dashboard ejecutivo para un periodo. Todo se calcula con SQL y Java (sin IA).
 * topServicios es un ARREGLO (maximo 3 posiciones) y anual guarda el costo de los 12 meses; el resto son colecciones.
 */
public record DatosDashboard(String periodo, BigDecimal costoTotal, int serviciosMonitoreados,
                             int serviciosDuplicados, BigDecimal ahorroPotencial, List<AlertaUmbral> alertas,
                             Map<String, BigDecimal> costoPorHerramienta, Map<String, BigDecimal> costoPorTribu,
                             ComparacionFila[] topServicios, ResumenAnual anual) { }
