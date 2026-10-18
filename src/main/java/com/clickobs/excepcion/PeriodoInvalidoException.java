package com.clickobs.excepcion;

/** El periodo no tiene el formato AAAA-MM (por ejemplo 2026-08). */
public class PeriodoInvalidoException extends ValidacionException {

    public PeriodoInvalidoException(String valorIngresado) {
        super("El periodo debe tener el formato AAAA-MM (por ejemplo 2026-08). Ingresado: \""
              + (valorIngresado == null ? "" : valorIngresado) + "\".");
    }
}
