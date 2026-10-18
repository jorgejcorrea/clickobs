package com.clickobs.dao;

import com.clickobs.db.Db;
import com.clickobs.model.AlertaUmbral;
import com.clickobs.model.ComparacionFila;
import com.clickobs.model.Herramienta;
import com.clickobs.model.Solapamiento;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConsumoDao {

    /**
     * CU-02: comparacion de costos. Es la misma consulta del script Consulta_CLICKOBS.sql, con filtro
     * opcional por periodo. Las herramientas sin consumo en el periodo se agregan marcadas como "sin datos"
     * (flujo alternativo del CU-02) en lugar de fallar.
     */
    public List<ComparacionFila> comparar(String periodo) throws SQLException {
        boolean filtrar = periodo != null && !periodo.isBlank();
        String sql = "SELECT H.nombre AS herramienta, S.nombre AS servicio, T.nombre AS tribu, C.periodo AS periodo, "
                   + "C.cantidad_usada AS cantidad, C.unidad_medida AS unidad, C.monto_usd AS costo "
                   + "FROM consumo C, herramienta H, servicio S, tribu T "
                   + "WHERE C.id_herramienta = H.id_herramienta AND C.id_servicio = S.id_servicio "
                   + "AND S.id_tribu = T.id_tribu " + (filtrar ? "AND C.periodo = ? " : "")
                   + "ORDER BY C.periodo, H.nombre";
        List<ComparacionFila> filas = new ArrayList<>();
        Set<String> conDatos = new HashSet<>();
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (filtrar) {
                ps.setString(1, periodo.trim());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    conDatos.add(rs.getString("herramienta"));
                    filas.add(new ComparacionFila(rs.getString("herramienta"), rs.getString("servicio"),
                            rs.getString("tribu"), rs.getString("periodo"), rs.getBigDecimal("cantidad"),
                            rs.getString("unidad"), rs.getBigDecimal("costo"), false));
                }
            }
            try (PreparedStatement ph = c.prepareStatement("SELECT nombre FROM herramienta ORDER BY id_herramienta");
                 ResultSet rh = ph.executeQuery()) {
                while (rh.next()) {
                    String nombre = rh.getString("nombre");
                    if (!conDatos.contains(nombre)) {
                        filas.add(new ComparacionFila(nombre, "sin datos", "-", filtrar ? periodo.trim() : "-",
                                null, "", null, true));
                    }
                }
            }
        }
        return filas;
    }

    /** Detalle de consumo de UNA herramienta en un periodo (pantalla Consumo). */
    public List<ComparacionFila> consumoDeHerramienta(int idHerramienta, String periodo) throws SQLException {
        String sql = "SELECT H.nombre, S.nombre AS servicio, T.nombre AS tribu, C.periodo, C.cantidad_usada, "
                   + "C.unidad_medida, C.monto_usd FROM consumo C JOIN herramienta H ON C.id_herramienta = H.id_herramienta "
                   + "JOIN servicio S ON C.id_servicio = S.id_servicio JOIN tribu T ON S.id_tribu = T.id_tribu "
                   + "WHERE C.id_herramienta = ? AND C.periodo = ? ORDER BY S.nombre";
        List<ComparacionFila> filas = new ArrayList<>();
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idHerramienta);
            ps.setString(2, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new ComparacionFila(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getBigDecimal(5), rs.getString(6), rs.getBigDecimal(7), false));
                }
            }
        }
        return filas;
    }

    /** Costo total en USD de una herramienta en un periodo (0 si no hay datos). */
    public BigDecimal costoTotal(int idHerramienta, String periodo) throws SQLException {
        String sql = "SELECT COALESCE(SUM(monto_usd), 0) FROM consumo WHERE id_herramienta = ? AND periodo = ?";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idHerramienta);
            ps.setString(2, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    /** RF-12: servicios cubiertos por mas de una herramienta en el periodo, calculado con SQL (no con IA). */
    public List<Solapamiento> solapamientos(String periodo) throws SQLException {
        String sql = "SELECT s.id_servicio, s.nombre AS servicio, h.id_herramienta, h.nombre AS herramienta, "
                   + "SUM(c.monto_usd) AS costo "
                   + "FROM consumo c JOIN herramienta h ON c.id_herramienta = h.id_herramienta "
                   + "JOIN servicio s ON c.id_servicio = s.id_servicio "
                   + "WHERE c.periodo = ? "
                   + "GROUP BY s.id_servicio, s.nombre, h.id_herramienta, h.nombre "
                   + "ORDER BY s.id_servicio, costo";
        Map<String, List<Solapamiento.Cobertura>> porServicio = new LinkedHashMap<>();
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    porServicio.computeIfAbsent(rs.getString("servicio"), k -> new ArrayList<>())
                        .add(new Solapamiento.Cobertura(rs.getInt("id_herramienta"), rs.getString("herramienta"),
                                                        rs.getBigDecimal("costo")));
                }
            }
        }
        List<Solapamiento> resultado = new ArrayList<>();
        porServicio.forEach((servicio, coberturas) -> {
            if (coberturas.size() > 1) {
                resultado.add(new Solapamiento(servicio, coberturas));
            }
        });
        return resultado;
    }

    /** Uso interno del gestor: costo por herramienta usando el polimorfismo de Herramienta. */
    public Map<String, BigDecimal> costosPorHerramienta(List<Herramienta> herramientas, String periodo)
            throws SQLException, com.clickobs.excepcion.ValidacionException {
        Map<String, BigDecimal> costos = new LinkedHashMap<>();
        for (Herramienta h : herramientas) {
            costos.put(h.getNombre(), h.calcularCostoPeriodo(periodo));
        }
        return costos;
    }

    /** Ultimo periodo con datos (o null si la tabla esta vacia). */
    public String ultimoPeriodo() throws SQLException {
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT MAX(periodo) FROM consumo");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getString(1);
        }
    }

    /** Costo por herramienta en el periodo; las herramientas sin datos aparecen con 0. */
    public Map<String, BigDecimal> costoPorHerramienta(String periodo) throws SQLException {
        String sql = "SELECT h.nombre, COALESCE(SUM(c.monto_usd), 0) AS costo FROM herramienta h "
                   + "LEFT JOIN consumo c ON c.id_herramienta = h.id_herramienta AND c.periodo = ? "
                   + "GROUP BY h.id_herramienta, h.nombre ORDER BY h.id_herramienta";
        return mapaNombreCosto(sql, periodo);
    }

    /** Costo por tribu (a traves de los servicios que cada tribu tiene a cargo). */
    public Map<String, BigDecimal> costoPorTribu(String periodo) throws SQLException {
        String sql = "SELECT t.nombre, SUM(c.monto_usd) AS costo FROM consumo c "
                   + "JOIN servicio s ON c.id_servicio = s.id_servicio JOIN tribu t ON s.id_tribu = t.id_tribu "
                   + "WHERE c.periodo = ? GROUP BY t.id_tribu, t.nombre ORDER BY t.id_tribu";
        return mapaNombreCosto(sql, periodo);
    }

    public int serviciosMonitoreados(String periodo) throws SQLException {
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(DISTINCT id_servicio) FROM consumo WHERE periodo = ?")) {
            ps.setString(1, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Herramientas cuyo costo del periodo supera el umbral (se asume periodicidad mensual, como en los datos de prueba). */
    public List<AlertaUmbral> alertasUmbral(String periodo) throws SQLException {
        String sql = "SELECT h.nombre, u.monto_maximo, SUM(c.monto_usd) AS costo FROM umbral_alerta u "
                   + "JOIN herramienta h ON u.id_herramienta = h.id_herramienta "
                   + "JOIN consumo c ON c.id_herramienta = u.id_herramienta AND c.periodo = ? "
                   + "GROUP BY u.id_umbral, h.nombre, u.monto_maximo HAVING SUM(c.monto_usd) > u.monto_maximo";
        List<AlertaUmbral> alertas = new ArrayList<>();
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    alertas.add(new AlertaUmbral(rs.getString(1), rs.getBigDecimal(2), rs.getBigDecimal(3)));
                }
            }
        }
        return alertas;
    }

    private Map<String, BigDecimal> mapaNombreCosto(String sql, String periodo) throws SQLException {
        Map<String, BigDecimal> mapa = new LinkedHashMap<>();
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mapa.put(rs.getString(1), rs.getBigDecimal(2));
                }
            }
        }
        return mapa;
    }
}
