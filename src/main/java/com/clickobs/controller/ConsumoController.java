package com.clickobs.controller;

import com.clickobs.dao.ConsumoDao;
import com.clickobs.dao.HerramientaDao;
import com.clickobs.excepcion.PersistenciaException;
import com.clickobs.excepcion.ValidacionException;
import com.clickobs.model.ComparacionFila;
import com.clickobs.model.Herramienta;
import com.clickobs.util.Validador;
import java.math.BigDecimal;
import java.util.List;

/** Controlador de la pantalla Consumo: valida el periodo y arma el detalle de una herramienta. */
public class ConsumoController {

    /** Datos para llenar la pantalla al abrirla. */
    public record Inicio(List<Herramienta> herramientas, String ultimoPeriodo) { }

    /** Detalle de consumo de una herramienta y su costo calculado de forma polimorfica. */
    public record Detalle(List<ComparacionFila> filas, BigDecimal costoUsd) { }

    private final HerramientaDao herramientas = new HerramientaDao();
    private final ConsumoDao consumo = new ConsumoDao();

    public Inicio cargarInicio() throws PersistenciaException {
        return new Inicio(herramientas.listar(), consumo.ultimoPeriodo());
    }

    public Detalle detalle(Herramienta herramienta, String periodoTexto) throws ValidacionException, PersistenciaException {
        String periodo = Validador.periodo(periodoTexto);
        return new Detalle(consumo.consumoDeHerramienta(herramienta.getIdHerramienta(), periodo),
                           herramienta.calcularCostoPeriodo(periodo));
    }
}
