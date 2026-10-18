package com.clickobs.model;

import com.clickobs.dao.ConsumoDao;
import com.clickobs.excepcion.ValidacionException;
import com.clickobs.util.Validador;
import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Herramienta SaaS (Dynatrace, Datadog, Elastic): el proveedor factura por uso.
 * El costo del periodo es EXACTAMENTE lo que reporta el proveedor (ya normalizado en consumo.monto_usd).
 */
public class HerramientaSaaS extends Herramienta {

    private final ConsumoDao consumoDao = new ConsumoDao();

    public HerramientaSaaS(int id, String nombre, String proveedor, String modeloFacturacion) {
        super(id, nombre, proveedor, modeloFacturacion);
    }

    @Override
    public BigDecimal calcularCostoPeriodo(String periodo) throws SQLException, ValidacionException {
        return consumoDao.costoTotal(getIdHerramienta(), Validador.periodo(periodo));
    }

    @Override
    public String getTipo() {
        return "SaaS";
    }

    @Override
    public String describirCosto() {
        return "Costo reportado por el proveedor (" + modeloFacturacion + ").";
    }
}
