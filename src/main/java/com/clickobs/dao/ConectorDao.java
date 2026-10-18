package com.clickobs.dao;

import com.clickobs.db.Db;
import com.clickobs.model.Conector;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla conector (pantalla Administracion). */
public class ConectorDao {

    public List<Conector> listar() throws SQLException {
        String sql = "SELECT c.id_conector, h.nombre, c.endpoint_api, c.estado FROM conector c "
                   + "JOIN herramienta h ON c.id_herramienta = h.id_herramienta ORDER BY c.id_conector";
        List<Conector> lista = new ArrayList<>();
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Conector(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)));
            }
        }
        return lista;
    }

    public void actualizarEstado(int idConector, String estado) throws SQLException {
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE conector SET estado = ? WHERE id_conector = ?")) {
            ps.setString(1, estado);
            ps.setInt(2, idConector);
            ps.executeUpdate();
        }
    }
}
