package com.clickobs.ui;

import com.clickobs.controller.AlertasController;
import com.clickobs.excepcion.MontoInvalidoException;
import com.clickobs.model.AlertaUmbral;
import com.clickobs.model.Herramienta;
import com.clickobs.model.UmbralAlerta;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Pantalla Alertas (CU-04): configurar umbrales de gasto y atender las alertas pendientes, que se muestran
 * en el orden de la cola (la primera es la que se atiende primero).
 */
public class AlertasView extends VBox {

    private final AlertasController controlador;
    private final TableView<UmbralAlerta> tabla = new TableView<>();
    private final ComboBox<Herramienta> herramientas = new ComboBox<>();
    private final TextField monto = new TextField();
    private final TextField periodo = new TextField();
    private final ListView<String> lista = new ListView<>();
    private final Label mensaje = new Label();
    private final Label contador = new Label("Alertas pendientes: 0");

    public AlertasView(AlertasController controlador) {
        this.controlador = controlador;
        setSpacing(10);
        Label titulo = new Label("Alertas de licenciamiento y umbrales");
        titulo.getStyleClass().add("titulo");

        // --- Configurar umbral ---
        herramientas.setPromptText("Herramienta");
        herramientas.setPrefWidth(200);
        monto.setPromptText("Monto máximo mensual (USD)");
        monto.setPrefWidth(250);
        Button guardar = new Button("Guardar umbral");
        guardar.getStyleClass().add("button-primary");
        HBox config = new HBox(8, new Label("Umbral:"), herramientas, monto, guardar);

        TableColumn<UmbralAlerta, String> c1 = columna("Herramienta", UmbralAlerta::getHerramienta);
        TableColumn<UmbralAlerta, String> c2 = columna("Monto máximo (USD)", u -> Formato.usd(u.getMontoMaximo()));
        TableColumn<UmbralAlerta, String> c3 = columna("Periodicidad", UmbralAlerta::getPeriodicidad);
        tabla.getColumns().add(c1);
        tabla.getColumns().add(c2);
        tabla.getColumns().add(c3);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPrefHeight(130);
        tabla.setMinHeight(130);

        // --- Evaluar periodo y cola de pendientes ---
        periodo.setPromptText("AAAA-MM");
        periodo.setPrefWidth(110);
        Button evaluar = new Button("Evaluar período");
        evaluar.getStyleClass().add("button-primary");
        Button atender = new Button("Atender siguiente");
        HBox evaluacion = new HBox(8, new Label("Período:"), periodo, evaluar, atender, contador);
        VBox.setVgrow(lista, Priority.ALWAYS);
        lista.setPlaceholder(new Label("Sin alertas pendientes. Evalúe un período."));

        mensaje.getStyleClass().add("hint");
        mensaje.setWrapText(true);
        mensaje.setMinHeight(32);

        // --- Acciones ---
        guardar.setOnAction(e -> {
            Herramienta h = herramientas.getValue();
            if (h == null) {
                mensaje.setText("Elija una herramienta.");
                return;
            }
            String texto = monto.getText();
            Async.ejecutar(() -> controlador.guardarUmbral(h.getIdHerramienta(), texto),   // valida el texto con try-catch dentro del Validador
                umbrales -> {
                    tabla.getItems().setAll(umbrales);
                    monto.clear();
                    aviso("Umbral de " + h.getNombre() + " guardado.", false);
                },
                fallo -> {
                    aviso(fallo instanceof MontoInvalidoException ? fallo.getMessage() : "Error: " + Async.mensaje(fallo), true);
                });
        });
        evaluar.setOnAction(e -> {
            String p = periodo.getText();
            Async.ejecutar(() -> controlador.evaluar(p),
                cantidad -> {
                    aviso(cantidad == 0 ? "Ningún umbral fue superado en el período." : cantidad + " alerta(s) encoladas.", false);
                    refrescarCola();
                },
                fallo -> {
                    aviso(Async.mensaje(fallo), true);
                });
        });
        atender.setOnAction(e -> {
            AlertaUmbral a = controlador.atenderSiguiente();
            aviso(a == null ? "No hay alertas pendientes." : "Atendida: " + a.herramienta() + ".", false);
            refrescarCola();
        });

        // Carga inicial
        Async.ejecutar(controlador::cargarInicio,
            inicio -> {
                herramientas.getItems().setAll(inicio.herramientas());
                tabla.getItems().setAll(inicio.umbrales());
                if (inicio.ultimoPeriodo() != null) {
                    periodo.setText(inicio.ultimoPeriodo());
                }
                refrescarCola();
            },
            fallo -> mensaje.setText("No se pudo cargar: " + Async.mensaje(fallo)));

        VBox.setMargin(contador, new Insets(0));
        Label subtitulo = new Label("Alertas pendientes (la primera se atiende primero)");
        subtitulo.getStyleClass().add("titulo");
        getChildren().addAll(titulo, config, tabla, evaluacion, subtitulo, lista, mensaje);
    }

    /** Muestra un mensaje de estado; los errores salen en rojo (se cambia la clase de estilo). */
    private void aviso(String texto, boolean esError) {
        mensaje.getStyleClass().removeAll("error", "hint");
        mensaje.getStyleClass().add(esError ? "error" : "hint");
        mensaje.setText(texto);
    }

    private void refrescarCola() {
        List<AlertaUmbral> pendientes = controlador.pendientes();
        lista.getItems().clear();
        int i = 1;
        for (AlertaUmbral a : pendientes) {
            lista.getItems().add(i++ + ". " + a.herramienta() + ": USD " + Formato.usd(a.costoUsd())
                + " supera el umbral de USD " + Formato.usd(a.umbralUsd()) + " (+" + a.excesoPorcentaje() + "%)");
        }
        contador.setText("Alertas pendientes: " + pendientes.size());
    }

    private TableColumn<UmbralAlerta, String> columna(String titulo, Function<UmbralAlerta, String> extractor) {
        TableColumn<UmbralAlerta, String> col = new TableColumn<>(titulo);
        col.setCellValueFactory(c -> new SimpleStringProperty(extractor.apply(c.getValue())));
        return col;
    }
}
