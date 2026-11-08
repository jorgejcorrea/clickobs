package com.clickobs.controller;

import com.clickobs.dao.ConsumoDao;
import com.clickobs.dao.HerramientaDao;
import com.clickobs.excepcion.PersistenciaException;
import com.clickobs.excepcion.ValidacionException;
import com.clickobs.model.AlertaUmbral;
import com.clickobs.model.Herramienta;
import com.clickobs.model.UmbralAlerta;
import com.clickobs.service.GestorAlertas;
import java.util.List;

/**
 * Controlador de Alertas (CU-04). Es unico para toda la sesion: la cola de alertas pendientes vive en el
 * GestorAlertas y sobrevive aunque el usuario cambie de pantalla.
 */
public class AlertasController {

    /** Datos para llenar la pantalla al abrirla. */
    public record Inicio(List<Herramienta> herramientas, List<UmbralAlerta> umbrales, String ultimoPeriodo) { }

    private final GestorAlertas gestor = new GestorAlertas();
    private final HerramientaDao herramientas = new HerramientaDao();
    private final ConsumoDao consumo = new ConsumoDao();

    public Inicio cargarInicio() throws PersistenciaException {
        return new Inicio(herramientas.listar(), gestor.listarUmbrales(), consumo.ultimoPeriodo());
    }

    /** Valida y guarda el umbral (UPDATE o INSERT en MySQL) y devuelve la lista actualizada. */
    public List<UmbralAlerta> guardarUmbral(int idHerramienta, String montoTexto) throws ValidacionException, PersistenciaException {
        gestor.guardarUmbral(idHerramienta, montoTexto);
        return gestor.listarUmbrales();
    }

    public int evaluar(String periodo) throws ValidacionException, PersistenciaException {
        return gestor.evaluar(periodo);
    }

    public AlertaUmbral atenderSiguiente() {
        return gestor.atenderSiguiente();
    }

    public List<AlertaUmbral> pendientes() {
        return gestor.pendientes();
    }
}
