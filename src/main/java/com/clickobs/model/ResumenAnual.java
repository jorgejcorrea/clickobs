package com.clickobs.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Resumen del costo de un anio, mes por mes (TP4). Usa un ARREGLO de 12 posiciones porque los meses de un anio
 * son siempre 12: tamano fijo y acceso directo por indice (indice 0 = enero). Los datos que trae la base llegan
 * en un ArrayList y se vuelcan aca; todos los calculos son bucles for sobre el arreglo.
 */
public final class ResumenAnual {

    /** Arreglo constante con las etiquetas de los 12 meses. */
    public static final String[] MESES = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

    private final String anio;
    private final BigDecimal[] costoPorMes = new BigDecimal[MESES.length];

    public ResumenAnual(String anio, BigDecimal[] costos) {
        if (costos == null || costos.length != MESES.length) {
            throw new IllegalArgumentException("Se necesitan exactamente 12 costos mensuales.");
        }
        this.anio = anio;
        for (int i = 0; i < costoPorMes.length; i++) {
            costoPorMes[i] = costos[i] == null ? BigDecimal.ZERO : costos[i];
        }
    }

    public String getAnio() {
        return anio;
    }

    /** Copia del arreglo: nadie de afuera puede modificar el estado interno (encapsulamiento). */
    public BigDecimal[] getCostoPorMes() {
        return costoPorMes.clone();
    }

    public BigDecimal total() {
        BigDecimal suma = BigDecimal.ZERO;
        for (BigDecimal c : costoPorMes) {
            suma = suma.add(c);
        }
        return suma;
    }

    /** Cantidad de meses con costo mayor que cero. */
    public int mesesConDatos() {
        int cuenta = 0;
        for (BigDecimal c : costoPorMes) {
            if (c.signum() > 0) {
                cuenta++;
            }
        }
        return cuenta;
    }

    /** Promedio de los meses que tienen datos (cero si no hay ninguno). */
    public BigDecimal promedioMensual() {
        int meses = mesesConDatos();
        return meses == 0 ? BigDecimal.ZERO : total().divide(BigDecimal.valueOf(meses), 2, RoundingMode.HALF_UP);
    }

    /** Indice (0 a 11) del mes mas caro, o -1 si ningun mes tiene datos. */
    public int indiceMesMasCaro() {
        int mayor = -1;
        for (int i = 0; i < costoPorMes.length; i++) {
            if (costoPorMes[i].signum() > 0 && (mayor == -1 || costoPorMes[i].compareTo(costoPorMes[mayor]) > 0)) {
                mayor = i;
            }
        }
        return mayor;
    }

    public String nombreMesMasCaro() {
        int i = indiceMesMasCaro();
        return i == -1 ? "-" : MESES[i];
    }
}
