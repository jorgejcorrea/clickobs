package com.clickobs.model;

import com.clickobs.excepcion.ValidacionException;

/** Conector de ingesta hacia una herramienta (tabla conector). El estado solo admite valores conocidos. */
public class Conector {

    public static final String ACTIVO = "Activo";
    public static final String INACTIVO = "Inactivo";

    private final int idConector;
    private final String herramienta;
    private final String endpointApi;
    private String estado;

    public Conector(int idConector, String herramienta, String endpointApi, String estado) {
        this.idConector = idConector;
        this.herramienta = herramienta;
        this.endpointApi = endpointApi;
        this.estado = estado;
    }

    /** Solo se puede cambiar a Activo o Inactivo; "Error" lo marca la ingesta (Airflow), no el usuario. */
    public void setEstado(String nuevo) throws ValidacionException {
        if (!ACTIVO.equals(nuevo) && !INACTIVO.equals(nuevo)) {
            throw new ValidacionException("Estado no permitido: " + nuevo);
        }
        this.estado = nuevo;
    }

    public int getIdConector() { return idConector; }
    public String getHerramienta() { return herramienta; }
    public String getEndpointApi() { return endpointApi; }
    public String getEstado() { return estado; }
}
