package com.clickobs.ui;

import com.clickobs.controller.AdministracionController;
import com.clickobs.excepcion.ValidacionException;
import com.clickobs.model.Conector;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Pantalla Administracion: conectores de ingesta (estado Activo / Inactivo). La ingesta real la hace Airflow. */
public class AdministracionView extends VBox {

    private final TableView<Conector> tabla = new TableView<>();
    private final Label estado = new Label();
    private final AdministracionController controlador = new AdministracionController();
    private final TableView<String[]> tablaBitacora = new TableView<>();

    public AdministracionView() {
        setSpacing(10);
        Label titulo = new Label("Administración de conectores");
        titulo.getStyleClass().add("titulo");

        columna("Herramienta", Conector::getHerramienta);
        columna("Endpoint API", Conector::getEndpointApi);
        columna("Estado", Conector::getEstado);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button activar = new Button("Marcar Activo");
        activar.getStyleClass().add("button-primary");
        Button desactivar = new Button("Marcar Inactivo");
        activar.setOnAction(e -> cambiar(Conector.ACTIVO));
        desactivar.setOnAction(e -> cambiar(Conector.INACTIVO));
        activar.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        desactivar.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        estado.setWrapText(true);
        HBox acciones = new HBox(8, activar, desactivar);
        acciones.setPadding(new Insets(4, 0, 0, 0));

        // Bitacora de actividad: se lee del archivo clickobs-bitacora.csv (los eventos son arreglos {fecha, usuario, evento}).
        Label tituloBitacora = new Label("Bitácora de actividad (últimos 8 eventos, guardados en archivo)");
        tituloBitacora.getStyleClass().add("grafico-titulo");
        String[] encabezados = {"Fecha", "Usuario", "Evento"};
        for (int i = 0; i < encabezados.length; i++) {
            final int posicion = i;
            TableColumn<String[], String> col = new TableColumn<>(encabezados[i]);
            col.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[posicion]));
            tablaBitacora.getColumns().add(col);
        }
        tablaBitacora.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaBitacora.setPrefHeight(190);
        tablaBitacora.setMinHeight(190);
        tabla.setPrefHeight(150);

        cargar();
        getChildren().addAll(titulo, tabla, acciones, estado, tituloBitacora, tablaBitacora);
    }

    private void cargar() {
        Async.ejecutar(controlador::listarConectores, lista -> tabla.getItems().setAll(lista),
            fallo -> estado.setText("No se pudo cargar: " + Async.mensaje(fallo)));
        Async.ejecutar(() -> controlador.ultimosEventos(8), eventos -> tablaBitacora.getItems().setAll(eventos),
            fallo -> estado.setText("No se pudo leer la bitácora: " + Async.mensaje(fallo)));
    }

    private void cambiar(String nuevoEstado) {
        Conector sel = tabla.getSelectionModel().getSelectedItem();
        int indice = tabla.getSelectionModel().getSelectedIndex();
        if (sel == null) {
            estado.setText("Seleccione un conector de la tabla.");
            return;
        }
        Async.ejecutar(() -> controlador.cambiarEstado(sel, nuevoEstado),   // valida en el modelo y guarda con UPDATE
            lista -> {
                tabla.getItems().setAll(lista);
                tabla.getSelectionModel().select(indice);   // se mantiene la fila elegida tras recargar
                estado.setText("Conector de " + sel.getHerramienta() + " ahora " + nuevoEstado + ".");
            },
            fallo -> estado.setText(fallo instanceof ValidacionException ? fallo.getMessage() : "Error: " + Async.mensaje(fallo)));
    }

    private void columna(String titulo, Function<Conector, String> extractor) {
        TableColumn<Conector, String> col = new TableColumn<>(titulo);
        col.setCellValueFactory(c -> new SimpleStringProperty(extractor.apply(c.getValue())));
        tabla.getColumns().add(col);
    }
}
