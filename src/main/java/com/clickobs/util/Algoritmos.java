package com.clickobs.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Ordenamiento y busqueda escritos a mano (no se usa Collections.sort, List.sort ni Collections.binarySearch).
 * Los volumenes del prototipo son chicos, asi que se eligieron algoritmos simples y faciles de explicar.
 */
public final class Algoritmos {

    private Algoritmos() { }

    /**
     * Ordenamiento por seleccion, O(n^2). En cada pasada busca el minimo del tramo que falta ordenar
     * y lo intercambia con la posicion actual. Devuelve una COPIA ordenada: la lista original no se modifica.
     */
    public static <T> List<T> ordenarPorSeleccion(List<T> origen, Comparator<? super T> criterio) {
        List<T> lista = new ArrayList<>(origen);
        int n = lista.size();
        for (int i = 0; i < n - 1; i++) {
            int menor = i;
            for (int j = i + 1; j < n; j++) {
                if (criterio.compare(lista.get(j), lista.get(menor)) < 0) {
                    menor = j;
                }
            }
            if (menor != i) {
                T aux = lista.get(i);
                lista.set(i, lista.get(menor));
                lista.set(menor, aux);
            }
        }
        return lista;
    }

    /**
     * Busqueda binaria, O(log n). PRECONDICION: la lista ya esta ordenada por la misma clave y el mismo comparador.
     * Devuelve la posicion de la PRIMERA coincidencia (para poder recorrer despues las repetidas) o -1.
     */
    public static <T, K> int buscarBinaria(List<T> ordenada, K clave, Function<T, K> extraerClave,
                                           Comparator<? super K> comparador) {
        int bajo = 0;
        int alto = ordenada.size() - 1;
        int encontrado = -1;
        while (bajo <= alto) {
            int medio = (bajo + alto) >>> 1;
            int cmp = comparador.compare(extraerClave.apply(ordenada.get(medio)), clave);
            if (cmp == 0) {
                encontrado = medio;
                alto = medio - 1;   // sigue hacia la izquierda: puede haber una coincidencia anterior
            } else if (cmp < 0) {
                bajo = medio + 1;
            } else {
                alto = medio - 1;
            }
        }
        return encontrado;
    }

    /** Busqueda lineal, O(n): primer elemento que cumple la condicion, o -1. Sirve cuando la lista no esta ordenada. */
    public static <T> int buscarLineal(List<T> lista, Predicate<T> condicion) {
        for (int i = 0; i < lista.size(); i++) {
            if (condicion.test(lista.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
