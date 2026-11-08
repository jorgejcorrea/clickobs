package com.clickobs.controller;

import com.clickobs.service.Bitacora;

/** Controlador de la ventana principal: deja constancia de la actividad del usuario en la bitacora (archivo de texto). */
public class SesionController {

    private final Bitacora bitacora = new Bitacora();

    public void registrar(String usuario, String evento) {
        bitacora.registrar(usuario, evento);
    }
}
