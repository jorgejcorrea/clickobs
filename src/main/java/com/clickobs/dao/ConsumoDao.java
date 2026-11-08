package com.clickobs.dao;

import com.clickobs.excepcion.PersistenciaException;
import com.clickobs.excepcion.ValidacionException;
import com.clickobs.model.AlertaUmbral;
import com.clickobs.model.ComparacionFila;
import com.clickobs.model.Herramienta;
import com.clickobs.model.Solapamiento;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Acceso a la tabla consumo (que unifica consumo y costo) y consultas agregadas del dashboard. */
public class ConsumoDao extends DaoBase {

    /**
     * CU-02: comparacion de costos. Es la misma consulta del script Consulta_CLICKOBS.sql, con filtro
     * opcional por periodo. Las herramientas sin consumo en el periodo se agregan marcadas como "sin datos"
     * (flujo alternativo del CU-02) en lugar de fallar.
     */
    public List<ComparacionFila> comparar(String periodo) throws PersistenciaException {
        boolean filtrar = periodo != null && !periodo.isBlank();
        String sql = "SELECT H.nombre AS herramienta, S.nombre AS servicio, T.nombre AS tribu, C.periodo AS periodo, "
                   + "C.cantidad_usada AS cantidad, C.unidad_medida AS unidad, C.monto_usd AS costo "
                   + "FROM consumo C, herramienta H, servicio S, tribu T "
                   + "WHERE C.id_herramienta = H.id_herramienta AND C.id_servicio = S.id_servicio "
                   + "AND S.id_tribu = T.id_tribu " + (filtrar ? "AND C.periodo = ? " : "")
                   + "ORDER BY C.periodo, H.nombre";
        Object[] parametros = filtrar ? new Object[] {periodo.trim()} : new Object[0];
        List<ComparacionFila> filas = consultar("consultar la comparacion de costos", sql,
                rs -> new ComparacionFila(rs.getString("herramienta"), rs.getString("servicio"), rs.getString("tribu"),
                        rs.getString("periodo"), rs.getBigDecimal("cantidad"), rs.getString("unidad"),
                        rs.getBigDecimal("costo"), false), parametros);
        Set<String> conDatos = new HashSet<>();
        for (ComparacionFila f : filas) {
            conDatos.add(f.herramienta());
        }
        List<String> nombres = consultar("consultar las herramientas", "SELECT nombre FROM herramienta ORDER BY id_herramienta",
                rs -> rs.getString("nombre"));
        for (String nombre : nombres) {
            if (!conDatos.contains(nombre)) {
                filas.add(new ComparacionFila(nombre, "sin datos", "-", filtrar ? periodo.trim() : "-", null, "", null, true));
            }
        }
        return filas;
    }

    /** Detalle de consumo de UNA herramienta en un periodo (pantalla Consumo). */
    public List<ComparacionFila> consumoDeHerramienta(int idHerramienta, String periodo) throws PersistenciaException {
        String sql = "SELECT H.nombre, S.nombre AS servicio, T.nombre AS tribu, C.periodo, C.cantidad_usada, "
                   + "C.unidad_medida, C.monto_usd FROM consumo C JOIN herramienta H ON C.id_herramienta = H.id_herramienta "
                   + "JOIN servicio S ON C.id_servicio = S.id_servicio JOIN tribu T ON S.id_tribu = T.id_tribu "
                   + "WHERE C.id_herramienta = ? AND C.periodo = ? ORDER BY S.nombre";
        return consultar("consultar el consumo de la herramienta", sql,
                rs -> new ComparacionFila(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getBigDecimal(5), rs.getString(6), rs.getBigDecimal(7), false), idHerramienta, periodo);
    }

    /** Costo total en USD de una herramienta en un periodo (0 si no hay datos). */
    public BigDecimal costoTotal(int idHerramienta, String periodo) throws PersistenciaException {
        String sql = "SELECT COALESCE(SUM(monto_usd), 0) FROM consumo WHERE id_herramienta = ? AND periodo = ?";
        return consultar("calcular el costo de la herramienta", sql, rs -> rs.getBigDecimal(1), idHerramienta, periodo).get(0);
    }

    /** RF-12: servicios cubiertos por mas de una herramienta en el periodo, calculado con SQL (no con IA). */
    public List<Solapamiento> solapamientos(String periodo) throws PersistenciaException {
        String sql = "SELECT s.id_servicio, s.nombre AS servicio, h.id_herramienta, h.nombre AS herramienta, "
                   + "SUM(c.monto_usd) AS costo "
                   + "FROM consumo c JOIN herramienta h ON c.id_herramienta = h.id_herramienta "
                   + "JOIN servicio s ON c.id_servicio = s.id_servicio "
                   + "WHERE c.periodo = ? "
                   + "GROUP BY s.id_servicio, s.nombre, h.id_herramienta, h.nombre "
                   + "ORDER BY s.id_servicio, costo";
        Map<String, List<Solapamiento.Cobertura>> porServicio = new LinkedHashMap<>();
        List<Object[]> filas = consultar("detectar solapamientos", sql,
                rs -> new Object[] {rs.getString("servicio"), rs.getInt("id_herramienta"), rs.getString("herramienta"),
                                    rs.getBigDecimal("costo")}, periodo);
        for (Object[] f : filas) {
            porServicio.computeIfAbsent((String) f[0], k -> new ArrayList<>())
                .add(new Solapamiento.Cobertura((Integer) f[1], (String) f[2], (BigDecimal) f[3]));
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
            throws PersistenciaException, ValidacionException {
        Map<String, BigDecimal> costos = new LinkedHashMap<>();
        for (Herramienta h : herramientas) {
            costos.put(h.getNombre(), h.calcularCostoPeriodo(periodo));
        }
        return costos;
    }

    /** Ultimo periodo con datos (o null si la tabla esta vacia). */
    public String ultimoPeriodo() throws PersistenciaException {
        return consultar("consultar el ultimo periodo", "SELECT MAX(periodo) FROM consumo", rs -> rs.getString(1)).get(0);
    }

    /** Costo por herramienta en el periodo; las herramientas sin datos aparecen con 0. */
    public Map<String, BigDecimal> costoPorHerramienta(String periodo) throws PersistenciaException {
        String sql = "SELECT h.nombre, COALESCE(SUM(c.monto_usd), 0) AS costo FROM herramienta h "
                   + "LEFT JOIN consumo c ON c.id_herramienta = h.id_herramienta AND c.periodo = ? "
                   + "GROUP BY h.id_herramienta, h.nombre ORDER BY h.id_herramienta";
        return mapaNombreCosto(sql, periodo);
    }

    /** Costo por tribu (a traves de los servicios que cada tribu tiene a cargo). */
    public Map<String, BigDecimal> costoPorTribu(String periodo) throws PersistenciaException {
        String sql = "SELECT t.nombre, SUM(c.monto_usd) AS costo FROM consumo c "
                   + "JOIN servicio s ON c.id_servicio = s.id_servicio JOIN tribu t ON s.id_tribu = t.id_tribu "
                   + "WHERE c.periodo = ? GROUP BY t.id_tribu, t.nombre ORDER BY t.id_tribu";
        return mapaNombreCosto(sql, periodo);
    }

    public int serviciosMonitoreados(String periodo) throws PersistenciaException {
        return consultar("contar los servicios monitoreados",
                "SELECT COUNT(DISTINCT id_servicio) FROM consumo WHERE periodo = ?", rs -> rs.getInt(1), periodo).get(0);
    }

    /** Herramientas cuyo costo del periodo supera el umbral (se asume periodicidad mensual, como en los datos de prueba). */
    public List<AlertaUmbral> alertasUmbral(String periodo) throws PersistenciaException {
        String sql = "SELECT h.nombre, u.monto_maximo, SUM(c.monto_usd) AS costo FROM umbral_alerta u "
                   + "JOIN herramienta h ON u.id_herramienta = h.id_herramienta "
                   + "JOIN consumo c ON c.id_herramienta = u.id_herramienta AND c.periodo = ? "
                   + "GROUP BY u.id_umbral, h.nombre, u.monto_maximo HAVING SUM(c.monto_usd) > u.monto_maximo";
        return consultar("consultar las alertas de umbral", sql,
                rs -> new AlertaUmbral(rs.getString(1), rs.getBigDecimal(2), rs.getBigDecimal(3)), periodo);
    }

    /**
     * Costo registrado en cada mes de un anio, en un ARREGLO de 12 posiciones (indice 0 = enero). El arreglo es la
     * estructura adecuada porque el tamano es fijo y conocido; los registros que devuelve la base llegan en un
     * ArrayList (tamano variable) y se vuelcan en el arreglo. Los meses sin datos quedan en cero.
     */
    public BigDecimal[] costosPorMes(String anio) throws PersistenciaException {
        String sql = "SELECT periodo, SUM(monto_usd) FROM consumo WHERE periodo LIKE ? GROUP BY periodo ORDER BY periodo";
        List<Object[]> filas = consultar("consultar el costo por mes", sql,
                rs -> new Object[] {rs.getString(1), rs.getBigDecimal(2)}, anio + "-%");
        BigDecimal[] porMes = new BigDecimal[12];
        Arrays.fill(porMes, BigDecimal.ZERO);
        for (Object[] f : filas) {
            int mes = Integer.parseInt(((String) f[0]).substring(5, 7));
            if (mes >= 1 && mes <= 12) {
                porMes[mes - 1] = (BigDecimal) f[1];
            }
        }
        return porMes;
    }

    private Map<String, BigDecimal> mapaNombreCosto(String sql, String periodo) throws PersistenciaException {
        Map<String, BigDecimal> mapa = new LinkedHashMap<>();
        List<Object[]> filas = consultar("consultar costos agrupados", sql,
                rs -> new Object[] {rs.getString(1), rs.getBigDecimal(2)}, periodo);
        for (Object[] f : filas) {
            mapa.put((String) f[0], (BigDecimal) f[1]);
        }
        return mapa;
    }
}
