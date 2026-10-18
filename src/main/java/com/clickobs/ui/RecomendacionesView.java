package com.clickobs.ui;

import com.clickobs.model.RecomendacionIA;
import com.clickobs.service.GestorRecomendaciones;
import com.clickobs.service.IASimulada;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Pantalla 3: generar recomendacion IA (CU-03) e historial (RF-14). */
public class RecomendacionesView extends VBox {

    private static final NumberFormat NUM = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR"));
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final GestorRecomendaciones gestor = new GestorRecomendaciones(new IASimulada());
    private final TableView<RecomendacionIA> historial = new TableView<>();
    private final TextArea narrativa = new TextArea();
    private final Label ahorro = new Label();
    private final Label mensaje = new Label();
    private final ProgressIndicator progreso = new ProgressIndicator();

    public RecomendacionesView() {
        setSpacing(10);
        NUM.setMinimumFractionDigits(2);
        NUM.setMaximumFractionDigits(2);

        Label titulo = new Label("Generar recomendación IA");
        titulo.getStyleClass().add("titulo");

        TextField periodo = new TextField();
        periodo.setPromptText("AAAA-MM");
        periodo.setPrefWidth(120);
        Button generar = new Button("Generar recomendación");
        generar.getStyleClass().add("button-primary");
        generar.setDefaultButton(true);
        progreso.setVisible(false);
        progreso.setPrefSize(22, 22);
        HBox filtro = new HBox(8, new Label("Período:"), periodo, generar, progreso);

        narrativa.setEditable(false);
        narrativa.setWrapText(true);
        narrativa.setPrefRowCount(5);
        narrativa.setMinHeight(120);
        narrativa.setPromptText("La recomendación generada se mostrará aquí.");
        ahorro.getStyleClass().add("ahorro");
        mensaje.getStyleClass().add("error");
        VBox tarjeta = new VBox(6, new Label("Recomendación"), narrativa, ahorro);
        tarjeta.getStyleClass().add("card");

        Label tituloHistorial = new Label("Historial de recomendaciones");
        tituloHistorial.getStyleClass().add("titulo");
        columna("Fecha", r -> r.fecha() == null ? "" : r.fecha().format(FECHA), 130);
        columna("Herramienta", r -> r.herramienta() == null ? "-" : r.herramienta(), 130);
        columna("Modelo", RecomendacionIA::modeloUsado, 110);
        columna("Descripción", RecomendacionIA::descripcion, 420);
        columna("Ahorro (USD)", r -> NUM.format(r.ahorroEstimado()), 100);
        historial.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(historial, Priority.ALWAYS);

        generar.setOnAction(e -> {
            mensaje.setText("");
            generar.setDisable(true);
            progreso.setVisible(true);
            String p = periodo.getText() == null ? "" : periodo.getText().trim();
            Async.ejecutar(() -> gestor.generarRecomendacion(p),
                r -> {
                    generar.setDisable(false);
                    progreso.setVisible(false);
                    narrativa.setText(r.descripcion());
                    ahorro.setText("Ahorro estimado: USD " + NUM.format(r.ahorroEstimado()));
                    recargarHistorial();
                },
                fallo -> {
                    generar.setDisable(false);
                    progreso.setVisible(false);
                    mensaje.setText(Async.mensaje(fallo));
                });
        });

        getChildren().addAll(titulo, filtro, mensaje, tarjeta, tituloHistorial, historial);
        recargarHistorial();
    }

    private void columna(String titulo, Function<RecomendacionIA, String> extractor, double ancho) {
        TableColumn<RecomendacionIA, String> col = new TableColumn<>(titulo);
        col.setCellValueFactory(c -> new SimpleStringProperty(extractor.apply(c.getValue())));
        col.setPrefWidth(ancho);
        historial.getColumns().add(col);
    }

    private void recargarHistorial() {
        Async.ejecutar(gestor::historial, lista -> historial.getItems().setAll(lista),
            fallo -> {
                Alert a = new Alert(Alert.AlertType.ERROR, Async.mensaje(fallo));
                a.setHeaderText("No se pudo cargar el historial");
                a.showAndWait();
            });
    }
}
