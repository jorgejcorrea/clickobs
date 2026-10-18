package com.clickobs.model;

import com.clickobs.excepcion.MontoInvalidoException;
import java.math.BigDecimal;

/**
 * Umbral de gasto mensual de una herramienta (tabla umbral_alerta, CU-04).
 * Ejemplo de encapsulamiento con setter validado: el monto maximo NO se puede poner en un valor invalido
 * desde ningun lugar del programa, porque la unica puerta de entrada es setMontoMaximo().
 */
public class UmbralAlerta {

    private final int idUmbral;
    private final int idHerramienta;
    private final String herramienta;
    private BigDecimal montoMaximo;
    private final String periodicidad;

    public UmbralAlerta(int idUmbral, int idHerramienta, String herramienta, BigDecimal montoMaximo, String periodicidad)
            throws MontoInvalidoException {
        this.idUmbral = idUmbral;
        this.idHerramienta = idHerramienta;
        this.herramienta = herramienta;
        this.periodicidad = periodicidad == null ? "Mensual" : periodicidad;
        setMontoMaximo(montoMaximo);
    }

    /** Setter con regla de negocio: el umbral debe ser un monto positivo. */
    public final void setMontoMaximo(BigDecimal monto) throws MontoInvalidoException {
        if (monto == null || monto.signum() <= 0) {
            throw new MontoInvalidoException("El umbral debe ser mayor que cero.");
        }
        this.montoMaximo = monto;
    }

    /** true si el costo supera estrictamente el umbral. */
    public boolean esSuperadoPor(BigDecimal costo) {
        return costo != null && costo.compareTo(montoMaximo) > 0;
    }

    public int getIdUmbral() { return idUmbral; }
    public int getIdHerramienta() { return idHerramienta; }
    public String getHerramienta() { return herramienta; }
    public BigDecimal getMontoMaximo() { return montoMaximo; }
    public String getPeriodicidad() { return periodicidad; }
}
