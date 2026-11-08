package com.clickobs.controller;

import com.clickobs.dao.ConectorDao;
import com.clickobs.excepcion.PersistenciaException;
import com.clickobs.excepcion.ValidacionException;
import com.clickobs.model.Conector;
import com.clickobs.service.Bitacora;
import java.io.IOException;
import java.util.List;

/** Controlador de Administracion: estado de los conectores (MySQL) y lectura de la bitacora (archivo). */
public class AdministracionController {

    private final ConectorDao conectores = new ConectorDao();

    public List<Conector> listarConectores() throws PersistenciaException {
        return conectores.listar();
    }

    /** Valida el estado en el modelo, lo guarda con UPDATE y devuelve la lista recargada. */
    public List<Conector> cambiarEstado(Conector conector, String nuevoEstado) throws ValidacionException, PersistenciaException {
        conector.setEstado(nuevoEstado);
        conectores.actualizarEstado(conector.getIdConector(), nuevoEstado);
        return conectores.listar();
    }

    /** Ultimos eventos de la bitacora: cada uno es un arreglo {fecha, usuario, evento}. */
    public List<String[]> ultimosEventos(int cantidad) throws IOException {
        return new Bitacora().ultimos(cantidad);
    }
}
