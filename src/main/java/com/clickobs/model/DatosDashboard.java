package com.clickobs.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Datos del dashboard ejecutivo para un periodo. Todo se calcula con SQL y Java (sin IA). */
public record DatosDashboard(String periodo, BigDecimal costoTotal, int serviciosMonitoreados,
                             int serviciosDuplicados, BigDecimal ahorroPotencial, List<AlertaUmbral> alertas,
                             Map<String, BigDecimal> costoPorHerramienta, Map<String, BigDecimal> costoPorTribu) { }
