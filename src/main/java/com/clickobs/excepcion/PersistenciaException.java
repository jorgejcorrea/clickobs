package com.clickobs.excepcion;

import java.sql.SQLException;

/**
 * Error de acceso a la base de datos MySQL (TP4). Los DAO traducen cada SQLException a esta excepcion:
 * asi las capas superiores (servicio e interfaz) no dependen de java.sql y el usuario recibe un mensaje
 * entendible en lugar de un texto tecnico del driver. Es una excepcion verificada: quien consulta la base
 * esta obligado a decidir que hacer si falla.
 */
public class PersistenciaException extends Exception {

    private final int codigoSql;

    public PersistenciaException(String mensaje, int codigoSql, Throwable causa) {
        super(mensaje, causa);
        this.codigoSql = codigoSql;
    }

    /** Codigo de error de MySQL (por ejemplo 1045 = acceso denegado); 0 si no corresponde. */
    public int getCodigoSql() {
        return codigoSql;
    }

    /** Traduce una SQLException a un mensaje claro segun el codigo de error de MySQL / el estado SQL. */
    public static PersistenciaException de(String operacion, SQLException e) {
        int codigo = e.getErrorCode();
        String estado = e.getSQLState() == null ? "" : e.getSQLState();
        String detalle;
        switch (codigo) {
            case 1045: detalle = "usuario o clave de la base de datos incorrectos"; break;
            case 1049: detalle = "la base de datos no existe"; break;
            case 1146: detalle = "falta una tabla (ejecute CreacionTablas_CLICKOBS.sql)"; break;
            case 1062: detalle = "ya existe un registro con ese valor"; break;
            case 1451:
            case 1452: detalle = "el registro esta relacionado con otros datos"; break;
            case 3819: detalle = "el valor no cumple una restriccion de la base de datos"; break;
            default:
                if (estado.startsWith("08")) {
                    detalle = "no hay conexion con la base de datos (revise que el servidor MySQL este encendido)";
                } else {
                    detalle = e.getMessage() == null ? "error de base de datos" : e.getMessage();
                }
        }
        return new PersistenciaException("No se pudo " + operacion + ": " + detalle + ".", codigo, e);
    }
}
