package com.clickobs.dao;

import com.clickobs.excepcion.PersistenciaException;
import java.util.List;

/**
 * INTERFAZ del patron DAO (Data Access Object). Es el contrato comun de los accesos a una tabla: la capa de
 * servicio y la interfaz dependen de esta abstraccion y no de JDBC. Cada DAO concreto agrega sus operaciones
 * propias (guardar, actualizar, buscar por nombre, etc.).
 *
 * @param <T> tipo de objeto del dominio que el DAO recupera (por ejemplo UmbralAlerta)
 */
public interface Dao<T> {

    /** Devuelve todos los registros de la tabla como objetos del dominio, en un ArrayList. */
    List<T> listar() throws PersistenciaException;
}
