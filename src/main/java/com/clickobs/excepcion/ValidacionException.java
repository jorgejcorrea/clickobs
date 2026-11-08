package com.clickobs.excepcion;

/**
 * Error de validacion de datos ingresados por el usuario (periodo, montos, etc.).
 * Es una excepcion CHEQUEADA a proposito: obliga a quien llama a decidir que hacer
 * (mostrar un mensaje en pantalla) en lugar de dejar que la aplicacion se caiga.
 */
public class ValidacionException extends Exception {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }

    public ValidacionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
