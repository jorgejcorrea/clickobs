package com.clickobs.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Herramienta cuyo costo del periodo supera el umbral de gasto configurado (RF-15, RF-16). Registro inmutable de solo lectura. */
public record AlertaUmbral(String herramienta, BigDecimal umbralUsd, BigDecimal costoUsd) {

    /** Cuantos USD se paso del umbral. */
    public BigDecimal exceso() {
        return costoUsd.subtract(umbralUsd);
    }

    /** Exceso como porcentaje del umbral (para ordenar las alertas por gravedad). */
    public BigDecimal excesoPorcentaje() {
        return exceso().multiply(new BigDecimal("100")).divide(umbralUsd, 1, RoundingMode.HALF_UP);
    }
}
