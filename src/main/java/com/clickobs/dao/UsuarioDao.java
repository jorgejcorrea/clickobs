package com.clickobs.dao;

import com.clickobs.excepcion.PersistenciaException;
import com.clickobs.model.Usuario;
import java.util.List;
import java.util.Optional;

public class UsuarioDao extends DaoBase {

    /** Busca un usuario por nombre (sin distinguir mayusculas) e incluye el nombre de su rol. */
    public Optional<Usuario> buscarPorNombre(String nombre) throws PersistenciaException {
        String sql = "SELECT u.id_usuario, u.nombre, u.id_rol, r.nombre AS rol "
                   + "FROM usuario u JOIN rol_permiso r ON u.id_rol = r.id_rol "
                   + "WHERE LOWER(u.nombre) = LOWER(?)";
        List<Usuario> encontrados = consultar("buscar el usuario", sql,
                rs -> new Usuario(rs.getInt("id_usuario"), rs.getString("nombre"), rs.getInt("id_rol"), rs.getString("rol")),
                nombre.trim());
        return encontrados.isEmpty() ? Optional.empty() : Optional.of(encontrados.get(0));
    }
}
