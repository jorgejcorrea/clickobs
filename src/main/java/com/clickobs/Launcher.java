package com.clickobs;

/** Lanzador auxiliar (necesario para ejecutar JavaFX desde un jar sin modulos). */
public final class Launcher {
    private Launcher() { }

    public static void main(String[] args) {
        App.main(args);
    }
}
