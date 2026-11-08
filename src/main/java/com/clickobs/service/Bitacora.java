package com.clickobs.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Bitacora de actividad en un ARCHIVO de texto (TP4: uso de archivos para guardar y recuperar informacion).
 * Cada linea tiene el formato  fecha;usuario;evento  y se agrega al final del archivo (modo APPEND), de modo que
 * el historial sobrevive aunque se cierre la aplicacion. Registrar un evento nunca debe romper la aplicacion: si
 * el archivo no se puede escribir, se informa por la salida de error y el programa sigue funcionando.
 */
public class Bitacora {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path archivo;

    /** Usa el archivo clickobs-bitacora.csv de la carpeta de ejecucion (o la ruta de -Dclickobs.bitacora). */
    public Bitacora() {
        this(Path.of(System.getProperty("clickobs.bitacora", "clickobs-bitacora.csv")));
    }

    public Bitacora(Path archivo) {
        this.archivo = archivo;
    }

    /** Agrega una linea al final del archivo. Devuelve false si no se pudo escribir (no lanza excepcion). */
    public boolean registrar(String usuario, String evento) {
        String linea = LocalDateTime.now().format(FORMATO) + ";" + limpiar(usuario) + ";" + limpiar(evento)
                     + System.lineSeparator();
        try {
            Files.writeString(archivo, linea, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return true;
        } catch (IOException e) {
            System.err.println("Bitacora: no se pudo escribir en " + archivo + " (" + e.getMessage() + ")");
            return false;
        }
    }

    /**
     * Recupera los ultimos n eventos, del mas reciente al mas antiguo. Cada evento es un ARREGLO de tres
     * posiciones {fecha, usuario, evento}; la lista de eventos es un ArrayList porque su tamano varia.
     */
    public List<String[]> ultimos(int n) throws IOException {
        List<String[]> eventos = new ArrayList<>();
        if (!Files.exists(archivo)) {
            return eventos;
        }
        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        for (int i = lineas.size() - 1; i >= 0 && eventos.size() < n; i--) {
            String[] partes = lineas.get(i).split(";", 3);
            if (partes.length == 3) {
                eventos.add(partes);
            }
        }
        return eventos;
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.replace(';', ',').replace('\r', ' ').replace('\n', ' ').trim();
    }
}
