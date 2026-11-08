package com.clickobs.ui;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.concurrent.Task;

/** Ejecuta trabajo de base de datos fuera del hilo de la interfaz para que la ventana no se congele. */
final class Async {

    private Async() { }

    static <T> void ejecutar(Callable<T> trabajo, Consumer<T> alTerminar, Consumer<Throwable> alFallar) {
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabajo.call();
            }
        };
        tarea.setOnSucceeded(e -> alTerminar.accept(tarea.getValue()));
        tarea.setOnFailed(e -> alFallar.accept(tarea.getException()));
        Thread hilo = new Thread(tarea, "clickobs-db");
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Mensaje legible para el usuario a partir de una excepcion. */
    static String mensaje(Throwable t) {
        return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    }
}
