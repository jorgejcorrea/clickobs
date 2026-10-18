package com.clickobs.ui;

import com.clickobs.excepcion.ValidacionException;
import com.clickobs.model.ComparacionFila;
import com.clickobs.util.Algoritmos;
import com.clickobs.service.ExportadorCsv;
import com.clickobs.service.GestorRecomendaciones;
import com.clickobs.service.IASimulada;
import java.io.File;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** Pantalla 2: comparar costos entre herramientas (CU-02). */
public class ComparaCostosView extends VBox {

    private static final NumberFormat NUM = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR"));

    private final GestorRecomendaciones gestor = new GestorRecomendaciones(new IASimulada());
    private final TableView<ComparacionFila> tabla = new TableView<>();
    private final Label estado = new Label();
    private final Button exportar = new Button("Exportar reporte");
    private final ComboBox<String> orden = new ComboBox<>();
    private final TextField herramienta = new TextField();
    private List<ComparacionFila> ultimo = List.of();

    private static final String SIN_ORDEN = "Sin ordenar";
    private static final String POR_HERRAMIENTA = "Herramienta (A-Z)";
    private static final String COSTO_ASC = "Costo (menor a mayor)";
    private static final String COSTO_DESC = "Costo (mayor a menor)";

    public ComparaCostosView() {
        setSpacing(10);
        NUM.setMinimumFractionDigits(2);
        NUM.setMaximumFractionDigits(2);

        Label titulo = new Label("Comparar costos entre herramientas");
        titulo.getStyleClass().add("titulo");

        TextField periodo = new TextField();
        periodo.setPromptText("Período (AAAA-MM), vacío = todos");
        periodo.setPrefWidth(240);
        Button buscar = new Button("Buscar");
        buscar.getStyleClass().add("button-primary");
        buscar.setDefaultButton(true);
        HBox filtro = new HBox(8, new Label("Período:"), periodo, buscar);

        // Ordenar (seleccion a mano) y ubicar una herramienta (busqueda binaria a mano) sobre la tabla ya cargada
        orden.getItems().addAll(SIN_ORDEN, POR_HERRAMIENTA, COSTO_ASC, COSTO_DESC);
        orden.setValue(SIN_ORDEN);
        orden.setOnAction(e -> mostrar());
        herramienta.setPromptText("Nombre de herramienta");
        herramienta.setPrefWidth(190);
        Button ubicar = new Button("Ubicar");
        ubicar.setOnAction(e -> ubicarHerramienta());
        HBox herramientasFila = new HBox(8, new Label("Ordenar por:"), orden, new Label("Ubicar:"), herramienta, ubicar);

        agregarColumna("Herramienta", ComparacionFila::herramienta, 150);
        agregarColumna("Servicio", ComparacionFila::servicio, 180);
        agregarColumna("Tribu", ComparacionFila::tribu, 130);
        agregarColumna("Período", ComparacionFila::periodo, 90);
        agregarColumna("Cantidad", f -> f.cantidadUsada() == null ? "" : NUM.format(f.cantidadUsada()), 110);
        agregarColumna("Unidad", ComparacionFila::unidad, 110);
        agregarColumna("Costo (USD)", f -> f.costoUsd() == null ? "" : NUM.format(f.costoUsd()), 110);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("Ingrese un período y presione Buscar."));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        exportar.setDisable(true);
        exportar.setOnAction(e -> exportarCsv(periodo.getText()));
        HBox pie = new HBox(12, exportar, estado);
        pie.setPadding(new Insets(4, 0, 0, 0));

        buscar.setOnAction(e -> {
            estado.setText("Consultando...");
            String p = periodo.getText();
            Async.ejecutar(() -> gestor.compararCostos(p),
                filas -> {
                    ultimo = filas;
                    mostrar();
                    exportar.setDisable(filas.isEmpty());
                    BigDecimal total = filas.stream().map(ComparacionFila::costoUsd).filter(v -> v != null)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    long sinDatos = filas.stream().filter(ComparacionFila::sinDatos).count();
                    estado.setText(filas.size() + " fila(s). Total: USD " + NUM.format(total)
                        + (sinDatos > 0 ? ". Herramientas sin datos: " + sinDatos : ""));
                },
                fallo -> {
                    estado.setText("");
                    Alert a = new Alert(fallo instanceof ValidacionException ? Alert.AlertType.WARNING : Alert.AlertType.ERROR);
                    Label detalle = new Label(Async.mensaje(fallo));
                    detalle.setWrapText(true);
                    detalle.setPrefWidth(380);
                    detalle.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
                    a.getDialogPane().setContent(detalle);
                    a.setHeaderText(fallo instanceof ValidacionException ? "Dato inválido" : "No se pudo consultar");
                    a.showAndWait();
                });
        });

        getChildren().addAll(titulo, filtro, herramientasFila, tabla, pie);
    }


    /** Muestra las filas en la tabla aplicando el orden elegido (algoritmo de seleccion escrito a mano). */
    private void mostrar() {
        List<ComparacionFila> filas = ultimo;
        String criterio = orden.getValue();
        if (POR_HERRAMIENTA.equals(criterio)) {
            filas = Algoritmos.ordenarPorSeleccion(ultimo, Comparator.comparing(ComparacionFila::herramienta, String.CASE_INSENSITIVE_ORDER));
        } else if (COSTO_ASC.equals(criterio)) {
            filas = Algoritmos.ordenarPorSeleccion(ultimo, Comparator.comparing(ComparacionFila::costoUsd, ComparacionFila::compararCosto));
        } else if (COSTO_DESC.equals(criterio)) {
            filas = Algoritmos.ordenarPorSeleccion(ultimo, (a, b) -> ComparacionFila.compararCosto(b.costoUsd(), a.costoUsd()));
        }
        tabla.getItems().setAll(filas);
    }

    /**
     * Ubica la primera fila de la herramienta escrita. La busqueda binaria exige una lista ordenada por nombre,
     * por eso primero se ordena una copia por herramienta; despues se busca el indice y se selecciona la fila.
     */
    private void ubicarHerramienta() {
        String buscada = herramienta.getText() == null ? "" : herramienta.getText().trim();
        if (buscada.isEmpty() || ultimo.isEmpty()) {
            estado.setText("Cargue la tabla y escriba el nombre de una herramienta.");
            return;
        }
        List<ComparacionFila> porNombre = Algoritmos.ordenarPorSeleccion(ultimo,
            Comparator.comparing(ComparacionFila::herramienta, String.CASE_INSENSITIVE_ORDER));
        int pos = Algoritmos.buscarBinaria(porNombre, buscada, ComparacionFila::herramienta, String.CASE_INSENSITIVE_ORDER);
        if (pos < 0) {
            estado.setText("No se encontró la herramienta \"" + buscada + "\" (use el nombre completo).");
            return;
        }
        int coincidencias = 0;
        for (int i = pos; i < porNombre.size() && porNombre.get(i).herramienta().equalsIgnoreCase(buscada); i++) {
            coincidencias++;
        }
        ComparacionFila primera = porNombre.get(pos);
        int fila = Algoritmos.buscarLineal(tabla.getItems(), f -> f == primera);
        tabla.getSelectionModel().select(fila);
        tabla.scrollTo(fila);
        estado.setText(coincidencias + " fila(s) de " + primera.herramienta() + " (búsqueda binaria).");
    }

    private void agregarColumna(String titulo, Function<ComparacionFila, String> extractor, double ancho) {
        TableColumn<ComparacionFila, String> col = new TableColumn<>(titulo);
        col.setCellValueFactory(c -> new SimpleStringProperty(extractor.apply(c.getValue())));
        col.setPrefWidth(ancho);
        tabla.getColumns().add(col);
    }

    private void exportarCsv(String periodo) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar reporte");
        fc.setInitialFileName("comparacion_costos_" + (periodo == null || periodo.isBlank() ? "todos" : periodo.trim()) + ".csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (Excel)", "*.csv"));
        File destino = fc.showSaveDialog(getScene().getWindow());
        if (destino == null) {
            return;
        }
        try {
            new ExportadorCsv().exportar(ultimo, destino.toPath());
            estado.setText("Reporte exportado: " + destino.getName());
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, Async.mensaje(ex)).showAndWait();
        }
    }
}
