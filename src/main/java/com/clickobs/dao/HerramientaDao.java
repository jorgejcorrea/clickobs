package com.clickobs.dao;

import com.clickobs.excepcion.PersistenciaException;
import com.clickobs.model.Herramienta;
import com.clickobs.model.HerramientaSaaS;
import com.clickobs.model.HerramientaSelfHosted;
import java.math.BigDecimal;
import java.util.List;

/** Acceso a la tabla herramienta. Ademas de leer, funciona como FABRICA: crea la subclase segun el modelo de facturacion. */
public class HerramientaDao extends DaoBase implements Dao<Herramienta> {

    /** Costo fijo mensual del cluster Grafana+Prometheus (USD). Parametro del prototipo: en Construccion pasa a una columna de la BD. */
    private static final BigDecimal COSTO_FIJO_CLUSTER = new BigDecimal("800.00");

    /** Lista las herramientas y crea la subclase segun el modelo de facturacion (Infraestructura propia = self-hosted). */
    @Override
    public List<Herramienta> listar() throws PersistenciaException {
        String sql = "SELECT id_herramienta, nombre, proveedor, modelo_facturacion FROM herramienta ORDER BY id_herramienta";
        return consultar("consultar las herramientas", sql, rs -> {
            int id = rs.getInt("id_herramienta");
            String nombre = rs.getString("nombre");
            String proveedor = rs.getString("proveedor");
            String modelo = rs.getString("modelo_facturacion");
            if ("Infraestructura propia".equalsIgnoreCase(modelo)) {
                return new HerramientaSelfHosted(id, nombre, proveedor, modelo, COSTO_FIJO_CLUSTER);
            }
            return new HerramientaSaaS(id, nombre, proveedor, modelo);
        });
    }
}
