package com.clickobs.model;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** Servicio cubierto por mas de una herramienta en un periodo (RF-12). */
public record Solapamiento(String servicio, List<Cobertura> coberturas) {

    /** Costo de una herramienta para ese servicio. */
    public record Cobertura(int idHerramienta, String herramienta, BigDecimal costoUsd) { }

    /** Cobertura de menor costo: es la que se evaluaria retirar (estimacion conservadora del ahorro). */
    public Cobertura menorCosto() {
        return coberturas.stream().min(Comparator.comparing(Cobertura::costoUsd)).orElseThrow();
    }

    /** Cobertura que se mantiene: la de mayor costo. */
    public Cobertura mayorCosto() {
        return coberturas.stream().max(Comparator.comparing(Cobertura::costoUsd)).orElseThrow();
    }
}
