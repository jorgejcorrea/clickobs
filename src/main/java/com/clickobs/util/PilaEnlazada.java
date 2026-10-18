package com.clickobs.util;

import com.clickobs.excepcion.EstructuraVaciaException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pila (LIFO) implementada a mano con una lista enlazada simple. No usa java.util.Stack ni Deque.
 * En CLICKOBS guarda el historial de pantallas visitadas para el boton "Volver".
 * Todas las operaciones son O(1): apilar y desapilar solo tocan el tope.
 */
public class PilaEnlazada<T> {

    private static class Nodo<T> {
        private final T dato;
        private final Nodo<T> siguiente;

        Nodo(T dato, Nodo<T> siguiente) {
            this.dato = dato;
            this.siguiente = siguiente;
        }
    }

    private Nodo<T> tope;
    private int tamanio;

    public void apilar(T dato) {
        tope = new Nodo<>(dato, tope);
        tamanio++;
    }

    public T desapilar() {
        if (tope == null) {
            throw new EstructuraVaciaException("pila");
        }
        T dato = tope.dato;
        tope = tope.siguiente;
        tamanio--;
        return dato;
    }

    public T verTope() {
        if (tope == null) {
            throw new EstructuraVaciaException("pila");
        }
        return tope.dato;
    }

    public boolean estaVacia() {
        return tope == null;
    }

    public int tamanio() {
        return tamanio;
    }

    /** Copia del contenido del tope hacia la base (solo para mostrar; no modifica la pila). */
    public List<T> comoLista() {
        List<T> lista = new ArrayList<>();
        for (Nodo<T> n = tope; n != null; n = n.siguiente) {
            lista.add(n.dato);
        }
        return lista;
    }
}
