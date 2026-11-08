package com.clickobs;

import com.clickobs.model.Usuario;
import com.clickobs.ui.LoginView;
import com.clickobs.ui.PrincipalView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Punto de entrada de la aplicacion de escritorio CLICKOBS (prototipo JavaFX). */
public class App extends Application {

    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        mostrarLogin();
        stage.show();
    }

    private void mostrarLogin() {
        Scene scene = new Scene(new LoginView(this::mostrarPrincipal), 480, 440);
        aplicarEstilos(scene);
        stage.setTitle("CLICKOBS - Iniciar sesión");
        stage.setScene(scene);
    }

    private void mostrarPrincipal(Usuario usuario) {
        Scene scene = new Scene(new PrincipalView(usuario, this::mostrarLogin, stage), 1040, 660);
        aplicarEstilos(scene);
        stage.setTitle("CLICKOBS - Panel principal");
        stage.setScene(scene);
    }

    private void aplicarEstilos(Scene scene) {
        var css = App.class.getResource("/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
