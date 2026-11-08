package com.clickobs.dao;

import com.clickobs.db.Db;
import com.clickobs.excepcion.PersistenciaException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * CLASE ABSTRACTA base de todos los DAO. Aplica el patron Template Method: define UNA sola vez los pasos
 * repetitivos de JDBC (abrir la conexion, preparar la consulta, enlazar parametros, recorrer el resultado, cerrar
 * todo y traducir SQLException a PersistenciaException). Cada DAO concreto solo aporta lo que cambia: la
 * sentencia SQL y como convertir una fila en un objeto (MapeadorFila).
 *
 * No se puede instanciar: es una pieza de reutilizacion, no una entidad.
 */
public abstract class DaoBase {

    /** Convierte la fila actual de un ResultSet en un objeto del dominio. */
    @FunctionalInterface
    public interface MapeadorFila<T> {
        T mapear(ResultSet rs) throws SQLException;
    }

    /** Bloque de trabajo que usa una conexion ya abierta (para operaciones de varios pasos). */
    @FunctionalInterface
    public interface Operacion<R> {
        R ejecutar(Connection conexion) throws SQLException;
    }

    /** SELECT con parametros: devuelve las filas como un ArrayList de objetos del dominio. */
    protected <T> ArrayList<T> consultar(String operacion, String sql, MapeadorFila<T> mapeador, Object... parametros)
            throws PersistenciaException {
        return enConexion(operacion, c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                enlazar(ps, parametros);
                try (ResultSet rs = ps.executeQuery()) {
                    ArrayList<T> filas = new ArrayList<>();
                    while (rs.next()) {
                        filas.add(mapeador.mapear(rs));
                    }
                    return filas;
                }
            }
        });
    }

    /** INSERT / UPDATE / DELETE con parametros: devuelve la cantidad de filas afectadas. */
    protected int actualizar(String operacion, String sql, Object... parametros) throws PersistenciaException {
        return enConexion(operacion, c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                enlazar(ps, parametros);
                return ps.executeUpdate();
            }
        });
    }

    /** Abre una conexion, ejecuta el bloque, la cierra siempre y traduce cualquier SQLException. */
    protected <R> R enConexion(String operacion, Operacion<R> trabajo) throws PersistenciaException {
        try (Connection c = Db.getConnection()) {
            return trabajo.ejecutar(c);
        } catch (SQLException e) {
            throw PersistenciaException.de(operacion, e);
        }
    }

    private static void enlazar(PreparedStatement ps, Object[] parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            ps.setObject(i + 1, parametros[i]);
        }
    }
}
