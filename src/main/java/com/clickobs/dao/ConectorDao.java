package com.clickobs.dao;

import com.clickobs.excepcion.PersistenciaException;
import com.clickobs.model.Conector;
import java.util.List;

/** Acceso a la tabla conector (pantalla Administracion). */
public class ConectorDao extends DaoBase implements Dao<Conector> {

    @Override
    public List<Conector> listar() throws PersistenciaException {
        String sql = "SELECT c.id_conector, h.nombre, c.endpoint_api, c.estado FROM conector c "
                   + "JOIN herramienta h ON c.id_herramienta = h.id_herramienta ORDER BY c.id_conector";
        return consultar("consultar los conectores", sql,
                rs -> new Conector(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)));
    }

    public void actualizarEstado(int idConector, String estado) throws PersistenciaException {
        actualizar("actualizar el estado del conector", "UPDATE conector SET estado = ? WHERE id_conector = ?",
                estado, idConector);
    }
}
