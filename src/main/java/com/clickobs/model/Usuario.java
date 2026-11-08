package com.clickobs.model;

/** Usuario autenticado (tabla usuario + nombre de su rol). Atributos privados e inmutables: solo se leen por getters. */
public class Usuario {

    private final int idUsuario;
    private final String nombre;
    private final int idRol;
    private final String rolNombre;

    public Usuario(int idUsuario, String nombre, int idRol, String rolNombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El usuario necesita un nombre.");
        }
        this.idUsuario = idUsuario;
        this.nombre = nombre.trim();
        this.idRol = idRol;
        this.rolNombre = rolNombre == null ? "" : rolNombre;
    }

    public int getIdUsuario() { return idUsuario; }
    public String getNombre() { return nombre; }
    public int getIdRol() { return idRol; }
    public String getRolNombre() { return rolNombre; }
}
