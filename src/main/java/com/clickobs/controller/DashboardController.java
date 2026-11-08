package com.clickobs.controller;

import com.clickobs.excepcion.PersistenciaException;
import com.clickobs.excepcion.ValidacionException;
import com.clickobs.model.DatosDashboard;
import com.clickobs.service.GestorRecomendaciones;
import com.clickobs.service.IASimulada;

/** Controlador del Dashboard ejecutivo: pide al modelo los indicadores del periodo elegido. */
public class DashboardController {

    private final GestorRecomendaciones gestor = new GestorRecomendaciones(new IASimulada());

    public String ultimoPeriodo() throws PersistenciaException {
        return gestor.ultimoPeriodo();
    }

    public DatosDashboard dashboard(String periodo) throws PersistenciaException, ValidacionException {
        return gestor.dashboard(periodo);
    }
}
