package com.clickobs.ui;

import com.clickobs.dao.ConectorDao;
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
    private final ConectorDao dao = new ConectorDao();

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
        HBox acciones = new HBox(8, activar, desactivar, estado);
        acciones.setPadding(new Insets(4, 0, 0, 0));

        cargar();
        getChildren().addAll(titulo, tabla, acciones);
    }

    private void cargar() {
        Async.ejecutar(dao::listar, lista -> tabla.getItems().setAll(lista),
            fallo -> estado.setText("No se pudo cargar: " + Async.mensaje(fallo)));
    }

    private void cambiar(String nuevoEstado) {
        Conector sel = tabla.getSelectionModel().getSelectedItem();
        int indice = tabla.getSelectionModel().getSelectedIndex();
        if (sel == null) {
            estado.setText("Seleccione un conector de la tabla.");
            return;
        }
        Async.ejecutar(() -> {
                sel.setEstado(nuevoEstado);                 // valida el estado en el modelo
                dao.actualizarEstado(sel.getIdConector(), nuevoEstado);
                return dao.listar();
            },
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
