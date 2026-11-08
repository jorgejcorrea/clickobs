package com.clickobs.model;

import com.clickobs.excepcion.ValidacionException;
import java.math.BigDecimal;
import com.clickobs.excepcion.PersistenciaException;

/**
 * CLASE ABSTRACTA del diagrama de clases de diseno (pilar: abstraccion).
 * Define QUE debe poder hacer toda herramienta de observabilidad, pero no COMO calcula su costo:
 * eso lo decide cada subclase (pilar: polimorfismo).
 *
 * Encapsulamiento: los atributos son private; se accede por getters. El unico atributo protected es
 * modeloFacturacion, porque las subclases lo necesitan para describirse (pilar: herencia).
 */
public abstract class Herramienta {

    private final int idHerramienta;
    private final String nombre;
    private final String proveedor;
    protected final String modeloFacturacion;

    /** Constructor con validacion: no se puede crear una herramienta sin id positivo ni nombre. */
    protected Herramienta(int idHerramienta, String nombre, String proveedor, String modeloFacturacion) {
        if (idHerramienta <= 0) {
            throw new IllegalArgumentException("El id de la herramienta debe ser positivo.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("La herramienta necesita un nombre.");
        }
        this.idHerramienta = idHerramienta;
        this.nombre = nombre.trim();
        this.proveedor = proveedor == null ? "" : proveedor.trim();
        this.modeloFacturacion = modeloFacturacion == null ? "" : modeloFacturacion.trim();
    }

    /** METODO ABSTRACTO: costo total (USD) de la herramienta en el periodo AAAA-MM. Cada subclase lo implementa distinto. */
    public abstract BigDecimal calcularCostoPeriodo(String periodo) throws PersistenciaException, ValidacionException;

    /** Tipo legible de herramienta ("SaaS" o "Self-hosted"). */
    public abstract String getTipo();

    /** Aclaracion que se muestra junto al costo: de donde sale el numero. */
    public abstract String describirCosto();

    /**
     * Sincroniza el consumo desde la fuente. En el prototipo devuelve false: la sincronizacion real
     * la realiza la ingesta con Apache Airflow (carpeta airflow/ del repositorio).
     */
    public boolean sincronizarConsumo() {
        return false;
    }

    public int getIdHerramienta() { return idHerramienta; }
    public String getNombre() { return nombre; }
    public String getProveedor() { return proveedor; }
    public String getModeloFacturacion() { return modeloFacturacion; }

    /** Los ComboBox de la interfaz muestran este texto. */
    @Override
    public String toString() {
        return nombre;
    }
}
