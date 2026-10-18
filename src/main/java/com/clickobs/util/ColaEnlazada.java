package com.clickobs.util;

import com.clickobs.excepcion.EstructuraVaciaException;
import java.util.ArrayList;
import java.util.List;

/**
 * Cola (FIFO) implementada a mano con lista enlazada simple y punteros al frente y al final.
 * En CLICKOBS guarda las alertas de umbral pendientes: se atienden en el orden en que se generaron.
 * Encolar y desencolar son O(1) porque se mantiene el puntero al ultimo nodo.
 */
public class ColaEnlazada<T> {

    private static class Nodo<T> {
        private final T dato;
        private Nodo<T> siguiente;

        Nodo(T dato) {
            this.dato = dato;
        }
    }

    private Nodo<T> frente;
    private Nodo<T> fin;
    private int tamanio;

    public void encolar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (fin == null) {
            frente = nuevo;
        } else {
            fin.siguiente = nuevo;
        }
        fin = nuevo;
        tamanio++;
    }

    public T desencolar() {
        if (frente == null) {
            throw new EstructuraVaciaException("cola");
        }
        T dato = frente.dato;
        frente = frente.siguiente;
        if (frente == null) {
            fin = null;
        }
        tamanio--;
        return dato;
    }

    public T verFrente() {
        if (frente == null) {
            throw new EstructuraVaciaException("cola");
        }
        return frente.dato;
    }

    public boolean estaVacia() {
        return frente == null;
    }

    public int tamanio() {
        return tamanio;
    }

    public void vaciar() {
        frente = null;
        fin = null;
        tamanio = 0;
    }

    /** Copia del contenido del frente hacia el final (solo para mostrar). */
    public List<T> comoLista() {
        List<T> lista = new ArrayList<>();
        for (Nodo<T> n = frente; n != null; n = n.siguiente) {
            lista.add(n.dato);
        }
        return lista;
    }
}
