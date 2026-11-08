package com.clickobs.dao;

import com.clickobs.excepcion.MontoInvalidoException;
import com.clickobs.excepcion.PersistenciaException;
import com.clickobs.model.UmbralAlerta;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Objects;

/** Acceso a la tabla umbral_alerta (CU-04). */
public class UmbralDao extends DaoBase implements Dao<UmbralAlerta> {

    @Override
    public List<UmbralAlerta> listar() throws PersistenciaException {
        String sql = "SELECT u.id_umbral, u.id_herramienta, h.nombre, u.monto_maximo, u.periodicidad "
                   + "FROM umbral_alerta u JOIN herramienta h ON u.id_herramienta = h.id_herramienta ORDER BY h.id_herramienta";
        List<UmbralAlerta> lista = consultar("consultar los umbrales", sql, rs -> {
            try {
                return new UmbralAlerta(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getBigDecimal(4), rs.getString(5));
            } catch (MontoInvalidoException e) {
                return null;   // fila con un monto invalido cargada por fuera de la app: se omite en lugar de romper la pantalla
            }
        });
        lista.removeIf(Objects::isNull);
        return lista;
    }

    /** Crea el umbral de la herramienta si no existe o actualiza el monto si ya existe (una fila por herramienta). */
    public void guardar(int idHerramienta, BigDecimal monto) throws PersistenciaException {
        enConexion("guardar el umbral", c -> {
            int actualizadas;
            try (PreparedStatement up = c.prepareStatement("UPDATE umbral_alerta SET monto_maximo = ? WHERE id_herramienta = ?")) {
                up.setBigDecimal(1, monto);
                up.setInt(2, idHerramienta);
                actualizadas = up.executeUpdate();
            }
            if (actualizadas == 0) {
                int id;
                try (PreparedStatement mx = c.prepareStatement("SELECT COALESCE(MAX(id_umbral), 0) + 1 FROM umbral_alerta");
                     ResultSet rs = mx.executeQuery()) {
                    rs.next();
                    id = rs.getInt(1);
                }
                try (PreparedStatement in = c.prepareStatement(
                        "INSERT INTO umbral_alerta (id_umbral, id_herramienta, monto_maximo, periodicidad) VALUES (?, ?, ?, 'Mensual')")) {
                    in.setInt(1, id);
                    in.setInt(2, idHerramienta);
                    in.setBigDecimal(3, monto);
                    in.executeUpdate();
                }
            }
            return null;
        });
    }
}
