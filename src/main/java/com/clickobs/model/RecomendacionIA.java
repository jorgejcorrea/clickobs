package com.clickobs.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Recomendacion registrada en la tabla recomendacion_ia. idHerramienta y herramienta pueden ser null. */
public record RecomendacionIA(int id, Integer idHerramienta, String herramienta, LocalDateTime fecha,
                              String modeloUsado, String descripcion, BigDecimal ahorroEstimado) { }
