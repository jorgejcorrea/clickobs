package com.clickobs.ui;

import com.clickobs.controller.LoginController;
import com.clickobs.model.Usuario;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Pantalla 1: iniciar sesion (CU-01). */
public class LoginView extends VBox {

    private final LoginController controlador = new LoginController();

    public LoginView(Consumer<Usuario> alIngresar) {
        setAlignment(Pos.CENTER);
        setSpacing(10);
        setPadding(new Insets(30));

        Label titulo = new Label("CLICKOBS");
        titulo.getStyleClass().add("titulo-grande");
        Label subtitulo = new Label("Gestión inteligente de costos de observabilidad");
        subtitulo.getStyleClass().add("hint");

        TextField usuario = new TextField();
        usuario.setPromptText("Usuario");
        usuario.setMaxWidth(280);
        PasswordField clave = new PasswordField();
        clave.setPromptText("Contraseña");
        clave.setMaxWidth(280);

        Button entrar = new Button("Iniciar sesión");
        entrar.getStyleClass().add("button-primary");
        entrar.setMaxWidth(280);
        entrar.setDefaultButton(true);

        Label error = new Label();
        error.getStyleClass().add("error");
        error.setWrapText(true);
        error.setMaxWidth(320);

        Label aviso = new Label("Prototipo: por ahora se valida que el usuario exista en la base;\n"
                              + "la contraseña se verificará en la fase de Construcción.");
        aviso.getStyleClass().add("hint");

        entrar.setOnAction(e -> {
            error.setText("");
            String nombre = usuario.getText();
            if (nombre == null || nombre.isBlank()) {
                error.setText("Ingrese el nombre de usuario.");
                return;
            }
            entrar.setDisable(true);
            Async.ejecutar(() -> controlador.autenticar(nombre),
                encontrado -> {
                    entrar.setDisable(false);
                    if (encontrado.isPresent()) {
                        alIngresar.accept(encontrado.get());
                    } else {
                        error.setText("Usuario no encontrado.");
                    }
                },
                fallo -> {
                    entrar.setDisable(false);
                    error.setText("No se pudo conectar con la base de datos: " + Async.mensaje(fallo));
                });
        });

        getChildren().addAll(titulo, subtitulo, usuario, clave, entrar, error, aviso);
    }
}
