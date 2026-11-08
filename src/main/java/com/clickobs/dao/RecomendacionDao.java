package com.clickobs.dao;

import com.clickobs.excepcion.PersistenciaException;
import com.clickobs.model.RecomendacionIA;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

public class RecomendacionDao extends DaoBase implements Dao<RecomendacionIA> {

    /** Historial de recomendaciones (RF-14), de la mas reciente a la mas antigua. */
    @Override
    public List<RecomendacionIA> listar() throws PersistenciaException {
        String sql = "SELECT r.id_recomendacion, r.id_herramienta, h.nombre AS herramienta, r.fecha, "
                   + "r.modelo_usado, r.descripcion, r.ahorro_estimado "
                   + "FROM recomendacion_ia r LEFT JOIN herramienta h ON r.id_herramienta = h.id_herramienta "
                   + "ORDER BY r.fecha DESC, r.id_recomendacion DESC";
        return consultar("consultar el historial de recomendaciones", sql, rs -> {
            int idH = rs.getInt("id_herramienta");
            Integer idHerramienta = rs.wasNull() ? null : idH;
            Timestamp ts = rs.getTimestamp("fecha");
            return new RecomendacionIA(rs.getInt("id_recomendacion"), idHerramienta, rs.getString("herramienta"),
                    ts == null ? null : ts.toLocalDateTime(), rs.getString("modelo_usado"),
                    rs.getString("descripcion"), rs.getBigDecimal("ahorro_estimado"));
        });
    }

    /**
     * Registra la recomendacion. La clave primaria de la tabla no es AUTO_INCREMENT, por lo que el prototipo
     * calcula el siguiente id (MAX + 1). Mejora prevista para Construccion: AUTO_INCREMENT.
     */
    public RecomendacionIA insertar(Integer idHerramienta, String modeloUsado, String descripcion, BigDecimal ahorro)
            throws PersistenciaException {
        String texto = descripcion.length() > 500 ? descripcion.substring(0, 497) + "..." : descripcion;
        LocalDateTime ahora = LocalDateTime.now().withNano(0);
        return enConexion("registrar la recomendacion", c -> {
            int id;
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(id_recomendacion), 0) + 1 FROM recomendacion_ia")) {
                rs.next();
                id = rs.getInt(1);
            }
            String sql = "INSERT INTO recomendacion_ia (id_recomendacion, id_herramienta, fecha, modelo_usado, descripcion, "
                       + "ahorro_estimado) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, id);
                if (idHerramienta == null) {
                    ps.setNull(2, Types.INTEGER);
                } else {
                    ps.setInt(2, idHerramienta);
                }
                ps.setTimestamp(3, Timestamp.valueOf(ahora));
                ps.setString(4, modeloUsado);
                ps.setString(5, texto);
                ps.setBigDecimal(6, ahorro);
                ps.executeUpdate();
            }
            return new RecomendacionIA(id, idHerramienta, null, ahora, modeloUsado, texto, ahorro);
        });
    }
}
