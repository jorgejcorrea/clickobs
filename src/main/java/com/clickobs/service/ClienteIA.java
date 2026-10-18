package com.clickobs.service;

/**
 * Puerto hacia el modelo de lenguaje (OpenRouter en la arquitectura objetivo). La IA solo redacta la narrativa:
 * los numeros (costos y ahorro) llegan ya calculados en el payload.
 */
public interface ClienteIA {

    String redactarRecomendacion(PayloadIA payload);

    String nombreModelo();
}
