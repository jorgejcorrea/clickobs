package com.clickobs.dao;

import com.clickobs.db.Db;
import com.clickobs.model.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UsuarioDao {

    /** Busca un usuario por nombre (sin distinguir mayusculas) e incluye el nombre de su rol. */
    public Optional<Usuario> buscarPorNombre(String nombre) throws SQLException {
        String sql = "SELECT u.id_usuario, u.nombre, u.id_rol, r.nombre AS rol "
                   + "FROM usuario u JOIN rol_permiso r ON u.id_rol = r.id_rol "
                   + "WHERE LOWER(u.nombre) = LOWER(?)";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Usuario(rs.getInt("id_usuario"), rs.getString("nombre"),
                                                   rs.getInt("id_rol"), rs.getString("rol")));
                }
                return Optional.empty();
            }
        }
    }
}
