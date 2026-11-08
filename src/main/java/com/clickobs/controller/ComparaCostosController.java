package com.clickobs.controller;

import com.clickobs.excepcion.PersistenciaException;
import com.clickobs.excepcion.ValidacionException;
import com.clickobs.model.ComparacionFila;
import com.clickobs.service.ExportadorCsv;
import com.clickobs.service.GestorRecomendaciones;
import com.clickobs.service.IASimulada;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Controlador de Comparar costos (CU-02): consulta la comparacion y exporta el reporte a un archivo CSV. */
public class ComparaCostosController {

    private final GestorRecomendaciones gestor = new GestorRecomendaciones(new IASimulada());

    public List<ComparacionFila> comparar(String periodo) throws PersistenciaException, ValidacionException {
        return gestor.compararCostos(periodo);
    }

    public void exportar(List<ComparacionFila> filas, Path destino) throws IOException {
        new ExportadorCsv().exportar(filas, destino);
    }
}
