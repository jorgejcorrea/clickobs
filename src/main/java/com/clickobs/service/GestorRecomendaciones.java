package com.clickobs.service;

import com.clickobs.dao.ConsumoDao;
import com.clickobs.dao.HerramientaDao;
import com.clickobs.dao.RecomendacionDao;
import com.clickobs.excepcion.ValidacionException;
import com.clickobs.model.ComparacionFila;
import com.clickobs.util.Validador;
import com.clickobs.model.DatosDashboard;
import com.clickobs.model.Herramienta;
import com.clickobs.model.ResumenAnual;
import com.clickobs.util.Algoritmos;
import com.clickobs.model.RecomendacionIA;
import com.clickobs.model.Solapamiento;
import java.math.BigDecimal;
import com.clickobs.excepcion.PersistenciaException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Clase de control del diagrama de secuencia del CU-03. Calcula de forma deterministica (SQL + Java) las
 * comparaciones, los solapamientos y el ahorro estimado; la IA solo redacta la recomendacion.
 */
public class GestorRecomendaciones {

    private final ConsumoDao consumoDao = new ConsumoDao();
    private final HerramientaDao herramientaDao = new HerramientaDao();
    private final RecomendacionDao recomendacionDao = new RecomendacionDao();
    private final ClienteIA ia;

    public GestorRecomendaciones(ClienteIA ia) {
        this.ia = ia;
    }

    public List<ComparacionFila> compararCostos(String periodo) throws PersistenciaException, ValidacionException {
        if (periodo != null && !periodo.isBlank()) {
            periodo = Validador.periodo(periodo);
        }
        return consumoDao.comparar(periodo);
    }

    /** Flujo del CU-03: consolidar, detectar solapamientos, calcular ahorro, pedir la narrativa a la IA y registrar. */
    public RecomendacionIA generarRecomendacion(String periodo) throws PersistenciaException, ValidacionException {
        periodo = Validador.periodo(periodo);
        List<Herramienta> herramientas = herramientaDao.listar();
        Map<String, BigDecimal> costos = consumoDao.costosPorHerramienta(herramientas, periodo);
        if (costos.values().stream().allMatch(v -> v.signum() == 0)) {
            throw new IllegalStateException("No hay datos consolidados para el periodo " + periodo + ".");
        }
        List<Solapamiento> solapamientos = consumoDao.solapamientos(periodo);
        BigDecimal ahorro = solapamientos.stream().map(s -> s.menorCosto().costoUsd())
                                         .reduce(BigDecimal.ZERO, BigDecimal::add);
        PayloadIA payload = new PayloadIA(periodo, costos, solapamientos, ahorro);
        String narrativa = ia.redactarRecomendacion(payload);
        Integer herramientaAsociada = solapamientos.stream()
                .map(Solapamiento::menorCosto)
                .max(Comparator.comparing(Solapamiento.Cobertura::costoUsd))
                .map(Solapamiento.Cobertura::idHerramienta)
                .orElse(null);
        return recomendacionDao.insertar(herramientaAsociada, ia.nombreModelo(), narrativa, ahorro);
    }

    /** Ultimo periodo cargado, para abrir el dashboard sin que el usuario tenga que escribirlo. */
    public String ultimoPeriodo() throws PersistenciaException {
        return consumoDao.ultimoPeriodo();
    }

    /** Dashboard ejecutivo: KPIs y datos de los graficos, todo calculado de forma deterministica. */
    public DatosDashboard dashboard(String periodo) throws PersistenciaException, ValidacionException {
        periodo = Validador.periodo(periodo);
        Map<String, BigDecimal> porHerramienta = consumoDao.costoPorHerramienta(periodo);
        BigDecimal total = porHerramienta.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Solapamiento> solapamientos = consumoDao.solapamientos(periodo);
        BigDecimal ahorro = solapamientos.stream().map(s -> s.menorCosto().costoUsd()).reduce(BigDecimal.ZERO, BigDecimal::add);
        ResumenAnual anual = new ResumenAnual(periodo.substring(0, 4), consumoDao.costosPorMes(periodo.substring(0, 4)));
        return new DatosDashboard(periodo, total, consumoDao.serviciosMonitoreados(periodo), solapamientos.size(), ahorro,
                consumoDao.alertasUmbral(periodo), porHerramienta, consumoDao.costoPorTribu(periodo),
                topServicios(periodo, 3), anual);
    }

    /**
     * Ranking de los n servicios con mayor costo del periodo, en un ARREGLO de tamano fijo. Los datos llegan de la
     * base en un ArrayList; se ordenan de mayor a menor con el ordenamiento escrito a mano y se copian al arreglo.
     * Si hay menos de n filas con datos, las posiciones sobrantes no se crean (el arreglo es mas corto).
     */
    public ComparacionFila[] topServicios(String periodo, int n) throws PersistenciaException, ValidacionException {
        List<ComparacionFila> filas = new java.util.ArrayList<>();
        for (ComparacionFila f : compararCostos(periodo)) {
            if (!f.sinDatos() && f.costoUsd() != null) {
                filas.add(f);
            }
        }
        List<ComparacionFila> ordenadas = Algoritmos.ordenarPorSeleccion(filas,
                (a, b) -> ComparacionFila.compararCosto(b.costoUsd(), a.costoUsd()));
        ComparacionFila[] top = new ComparacionFila[Math.min(n, ordenadas.size())];
        for (int i = 0; i < top.length; i++) {
            top[i] = ordenadas.get(i);
        }
        return top;
    }

    public List<RecomendacionIA> historial() throws PersistenciaException {
        return recomendacionDao.listar();
    }
}
