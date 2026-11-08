package com.clickobs.service;

import com.clickobs.dao.ConsumoDao;
import com.clickobs.dao.UmbralDao;
import com.clickobs.excepcion.ValidacionException;
import com.clickobs.model.AlertaUmbral;
import com.clickobs.model.UmbralAlerta;
import com.clickobs.util.Algoritmos;
import com.clickobs.util.ColaEnlazada;
import com.clickobs.util.Validador;
import java.math.BigDecimal;
import com.clickobs.excepcion.PersistenciaException;
import java.util.Comparator;
import java.util.List;

/**
 * Clase de control del CU-04 (alertas de licenciamiento y umbrales).
 * Las alertas pendientes viven en una COLA propia: se atienden en orden. Antes de encolar se ordenan de mayor
 * a menor exceso (con el ordenamiento escrito a mano), asi la primera en la cola es la mas grave.
 */
public class GestorAlertas {

    private final UmbralDao umbralDao = new UmbralDao();
    private final ConsumoDao consumoDao = new ConsumoDao();
    private final ColaEnlazada<AlertaUmbral> pendientes = new ColaEnlazada<>();

    public List<UmbralAlerta> listarUmbrales() throws PersistenciaException {
        return umbralDao.listar();
    }

    /**
     * Configura el umbral de una herramienta. El monto llega como TEXTO desde la pantalla: se valida
     * (numero, positivo, tope) antes de tocar la base de datos.
     */
    public BigDecimal guardarUmbral(int idHerramienta, String montoTexto) throws ValidacionException, PersistenciaException {
        BigDecimal monto = Validador.monto(montoTexto);
        umbralDao.guardar(idHerramienta, monto);
        return monto;
    }

    /** Evalua el periodo: reemplaza el contenido de la cola por las alertas vigentes y devuelve cuantas hay. */
    public int evaluar(String periodo) throws ValidacionException, PersistenciaException {
        String p = Validador.periodo(periodo);
        List<AlertaUmbral> alertas = consumoDao.alertasUmbral(p);
        Comparator<AlertaUmbral> masGraveDelante = (a, b) -> b.excesoPorcentaje().compareTo(a.excesoPorcentaje());
        pendientes.vaciar();
        for (AlertaUmbral a : Algoritmos.ordenarPorSeleccion(alertas, masGraveDelante)) {
            pendientes.encolar(a);
        }
        return pendientes.tamanio();
    }

    /** Saca de la cola la alerta que sigue (la atendida). Devuelve null si no hay pendientes. */
    public AlertaUmbral atenderSiguiente() {
        return pendientes.estaVacia() ? null : pendientes.desencolar();
    }

    public List<AlertaUmbral> pendientes() {
        return pendientes.comoLista();
    }
}
