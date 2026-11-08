package com.clickobs.model;

import com.clickobs.dao.ConsumoDao;
import com.clickobs.excepcion.ValidacionException;
import com.clickobs.util.Validador;
import java.math.BigDecimal;
import com.clickobs.excepcion.PersistenciaException;

/**
 * Herramienta self-hosted (Grafana + Prometheus): no hay factura de un proveedor; el costo es el de la
 * infraestructura propia. Aunque el uso del mes sea bajo, el cluster cuesta lo mismo, por eso el costo
 * del periodo nunca baja del costo fijo del cluster (si hubo consumo ese mes).
 * Aca el polimorfismo es real: el MISMO mensaje calcularCostoPeriodo() hace un calculo distinto al de HerramientaSaaS.
 */
public class HerramientaSelfHosted extends Herramienta {

    private final ConsumoDao consumoDao = new ConsumoDao();
    private final BigDecimal costoFijoClusterMensual;

    /** Constructor completo. */
    public HerramientaSelfHosted(int id, String nombre, String proveedor, String modeloFacturacion,
                                 BigDecimal costoFijoClusterMensual) {
        super(id, nombre, proveedor, modeloFacturacion);
        if (costoFijoClusterMensual == null || costoFijoClusterMensual.signum() < 0) {
            throw new IllegalArgumentException("El costo fijo del cluster no puede ser negativo.");
        }
        this.costoFijoClusterMensual = costoFijoClusterMensual;
    }

    /** Constructor sobrecargado: sin costo fijo declarado, se toma el consumo registrado tal cual. */
    public HerramientaSelfHosted(int id, String nombre, String proveedor, String modeloFacturacion) {
        this(id, nombre, proveedor, modeloFacturacion, BigDecimal.ZERO);
    }

    @Override
    public BigDecimal calcularCostoPeriodo(String periodo) throws PersistenciaException, ValidacionException {
        BigDecimal registrado = consumoDao.costoTotal(getIdHerramienta(), Validador.periodo(periodo));
        if (registrado.signum() > 0 && registrado.compareTo(costoFijoClusterMensual) < 0) {
            return costoFijoClusterMensual;
        }
        return registrado;
    }

    @Override
    public String getTipo() {
        return "Self-hosted";
    }

    @Override
    public String describirCosto() {
        return "Costo estimado de infraestructura propia (minimo mensual del cluster: USD " + costoFijoClusterMensual + ").";
    }

    public BigDecimal getCostoFijoClusterMensual() {
        return costoFijoClusterMensual;
    }
}
