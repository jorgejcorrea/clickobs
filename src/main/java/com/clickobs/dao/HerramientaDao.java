package com.clickobs.dao;

import com.clickobs.db.Db;
import com.clickobs.model.Herramienta;
import com.clickobs.model.HerramientaSaaS;
import com.clickobs.model.HerramientaSelfHosted;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class HerramientaDao {

    /** Costo fijo mensual del cluster Grafana+Prometheus (USD). Parametro del prototipo: en Construccion pasa a una columna de la BD. */
    private static final BigDecimal COSTO_FIJO_CLUSTER = new BigDecimal("800.00");

    /** Lista las herramientas y crea la subclase segun el modelo de facturacion (Infraestructura propia = self-hosted). */
    public List<Herramienta> listar() throws SQLException {
        String sql = "SELECT id_herramienta, nombre, proveedor, modelo_facturacion FROM herramienta ORDER BY id_herramienta";
        List<Herramienta> lista = new ArrayList<>();
        try (Connection c = Db.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id_herramienta");
                String nombre = rs.getString("nombre");
                String proveedor = rs.getString("proveedor");
                String modelo = rs.getString("modelo_facturacion");
                if ("Infraestructura propia".equalsIgnoreCase(modelo)) {
                    lista.add(new HerramientaSelfHosted(id, nombre, proveedor, modelo, COSTO_FIJO_CLUSTER));
                } else {
                    lista.add(new HerramientaSaaS(id, nombre, proveedor, modelo));
                }
            }
        }
        return lista;
    }
}
