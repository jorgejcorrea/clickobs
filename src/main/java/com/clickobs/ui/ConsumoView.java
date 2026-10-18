package com.clickobs.ui;

import com.clickobs.dao.ConsumoDao;
import com.clickobs.dao.HerramientaDao;
import com.clickobs.model.ComparacionFila;
import com.clickobs.model.Herramienta;
import com.clickobs.util.Validador;
import java.math.BigDecimal;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Pantalla Consumo: detalle de consumo de UNA herramienta. El costo total lo calcula el objeto Herramienta
 * elegido (polimorfismo: SaaS y self-hosted responden distinto al mismo mensaje).
 */
public class ConsumoView extends VBox {

    private final TableView<ComparacionFila> tabla = new TableView<>();
    private final ComboBox<Herramienta> herramientas = new ComboBox<>();
    private final TextField periodo = new TextField();
    private final Label resultado = new Label();
    private final Label aclaracion = new Label();

    public ConsumoView() {
        setSpacing(10);
        Label titulo = new Label("Consumo por herramienta");
        titulo.getStyleClass().add("titulo");

        herramientas.setPromptText("Herramienta");
        herramientas.setPrefWidth(220);
        periodo.setPromptText("AAAA-MM");
        periodo.setPrefWidth(110);
        Button buscar = new Button("Consultar");
        buscar.getStyleClass().add("button-primary");
        buscar.setDefaultButton(true);
        HBox filtro = new HBox(8, new Label("Herramienta:"), herramientas, new Label("Período:"), periodo, buscar);

        columna("Servicio", ComparacionFila::servicio);
        columna("Tribu", ComparacionFila::tribu);
        columna("Período", ComparacionFila::periodo);
        columna("Cantidad usada", f -> Formato.usd(f.cantidadUsada()));
        columna("Unidad", ComparacionFila::unidad);
        columna("Costo (USD)", f -> Formato.usd(f.costoUsd()));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("Elija una herramienta y un período."));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        aclaracion.getStyleClass().add("hint");
        resultado.getStyleClass().add("ahorro");
        VBox pie = new VBox(4, resultado, aclaracion);
        pie.setPadding(new Insets(4, 0, 0, 0));

        // Carga inicial: herramientas (objetos polimorficos) y el ultimo periodo con datos
        Async.ejecutar(() -> new Object[] {new HerramientaDao().listar(), new ConsumoDao().ultimoPeriodo()},
            datos -> {
                @SuppressWarnings("unchecked")
                java.util.List<Herramienta> lista = (java.util.List<Herramienta>) datos[0];
                herramientas.getItems().setAll(lista);
                if (!lista.isEmpty()) {
                    herramientas.getSelectionModel().selectFirst();
                }
                if (datos[1] != null) {
                    periodo.setText((String) datos[1]);
                }
            },
            fallo -> resultado.setText("No se pudo cargar: " + Async.mensaje(fallo)));

        buscar.setOnAction(e -> {
            Herramienta h = herramientas.getValue();
            if (h == null) {
                resultado.setText("Elija una herramienta.");
                return;
            }
            Async.ejecutar(() -> {
                    String p = Validador.periodo(periodo.getText());
                    return new Object[] {new ConsumoDao().consumoDeHerramienta(h.getIdHerramienta(), p), h.calcularCostoPeriodo(p)};
                },
                datos -> {
                    @SuppressWarnings("unchecked")
                    java.util.List<ComparacionFila> filas = (java.util.List<ComparacionFila>) datos[0];
                    tabla.getItems().setAll(filas);
                    resultado.setText(h.getNombre() + " (" + h.getTipo() + "): USD " + Formato.usd((BigDecimal) datos[1]));
                    aclaracion.setText(h.describirCosto());
                },
                fallo -> {
                    tabla.getItems().clear();
                    resultado.setText(Async.mensaje(fallo));
                    aclaracion.setText("");
                });
        });

        getChildren().addAll(titulo, filtro, tabla, pie);
    }

    private void columna(String titulo, Function<ComparacionFila, String> extractor) {
        TableColumn<ComparacionFila, String> col = new TableColumn<>(titulo);
        col.setCellValueFactory(c -> new SimpleStringProperty(extractor.apply(c.getValue())));
        tabla.getColumns().add(col);
    }
}
