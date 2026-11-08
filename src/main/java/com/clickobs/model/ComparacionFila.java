package com.clickobs.model;

import java.math.BigDecimal;

/** Fila de la comparacion de costos (CU-02). Si sinDatos es true, la herramienta no tiene consumo en el periodo. */
public record ComparacionFila(String herramienta, String servicio, String tribu, String periodo,
                              BigDecimal cantidadUsada, String unidad, BigDecimal costoUsd, boolean sinDatos) {

    /** Compara costos tratando "sin datos" (null) como cero, para poder ordenar toda la tabla. */
    public static int compararCosto(BigDecimal a, BigDecimal b) {
        return (a == null ? BigDecimal.ZERO : a).compareTo(b == null ? BigDecimal.ZERO : b);
    }
}
