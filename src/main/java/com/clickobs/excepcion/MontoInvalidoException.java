package com.clickobs.excepcion;

/** El monto ingresado no es un numero valido o esta fuera del rango permitido. */
public class MontoInvalidoException extends ValidacionException {

    public MontoInvalidoException(String mensaje) {
        super(mensaje);
    }

    public MontoInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
