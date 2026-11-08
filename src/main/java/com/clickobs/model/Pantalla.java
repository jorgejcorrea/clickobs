package com.clickobs.model;

/** Pantallas navegables de la aplicacion (cada una con su titulo para el menu y el historial). */
public enum Pantalla {
    DASHBOARD("Dashboard"),
    CONSUMO("Consumo"),
    COMPARAR("Comparar Costos"),
    RECOMENDACIONES("Recomendaciones IA"),
    ALERTAS("Alertas"),
    ADMINISTRACION("Administración");

    private final String titulo;

    Pantalla(String titulo) {
        this.titulo = titulo;
    }

    public String getTitulo() {
        return titulo;
    }
}
