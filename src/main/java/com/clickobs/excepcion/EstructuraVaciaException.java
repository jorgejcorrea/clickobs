package com.clickobs.excepcion;

/** Se intento sacar un elemento de una pila o cola vacia. Es un error de programacion, por eso es no chequeada. */
public class EstructuraVaciaException extends RuntimeException {

    public EstructuraVaciaException(String estructura) {
        super("La " + estructura + " esta vacia.");
    }
}
