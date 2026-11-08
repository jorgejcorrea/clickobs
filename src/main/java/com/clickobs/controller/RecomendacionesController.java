package com.clickobs.controller;

import com.clickobs.excepcion.PersistenciaException;
import com.clickobs.excepcion.ValidacionException;
import com.clickobs.model.RecomendacionIA;
import com.clickobs.service.GestorRecomendaciones;
import com.clickobs.service.IASimulada;
import java.util.List;

/** Controlador de Recomendaciones IA (CU-03): genera una recomendacion y recupera el historial guardado. */
public class RecomendacionesController {

    private final GestorRecomendaciones gestor = new GestorRecomendaciones(new IASimulada());

    public RecomendacionIA generar(String periodo) throws PersistenciaException, ValidacionException {
        return gestor.generarRecomendacion(periodo);
    }

    public List<RecomendacionIA> historial() throws PersistenciaException {
        return gestor.historial();
    }
}
