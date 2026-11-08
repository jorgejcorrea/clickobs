package com.clickobs.ui;

import com.clickobs.model.AlertaUmbral;
import com.clickobs.model.ComparacionFila;
import com.clickobs.model.DatosDashboard;
import com.clickobs.model.ResumenAnual;
import com.clickobs.controller.DashboardController;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/** Dashboard ejecutivo: tarjetas KPI, costo por herramienta (barras), costo por tribu (dona) y alertas de umbral. */
public class DashboardView extends VBox {

    private static final NumberFormat NUM = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR"));
    private static final String[] COLORES = {"#4A90D9", "#7B61D9", "#E8873A", "#5F5E5A", "#0F6E56", "#C0392B"};

    private final DashboardController controlador = new DashboardController();
    private final TextField periodo = new TextField();
    private final Label mensaje = new Label();
    private final HBox tarjetas = new HBox(12);
    private final HBox graficos = new HBox(12);
    private final VBox alertas = new VBox(4);
    private final HBox inferior = new HBox(12);   // evolucion mensual (arreglo de 12 meses) + ranking Top 3 (arreglo)

    public DashboardView() {
        setSpacing(12);
        NUM.setMinimumFractionDigits(2);
        NUM.setMaximumFractionDigits(2);

        Label titulo = new Label("Dashboard ejecutivo");
        titulo.getStyleClass().add("titulo");
        periodo.setPromptText("AAAA-MM");
        periodo.setPrefWidth(110);
        Button actualizar = new Button("Actualizar");
        actualizar.getStyleClass().add("button-primary");
        actualizar.setDefaultButton(true);
        actualizar.setOnAction(e -> cargar(periodo.getText() == null ? "" : periodo.getText().trim()));
        HBox filtro = new HBox(8, new Label("Período:"), periodo, actualizar);
        filtro.setAlignment(Pos.CENTER_LEFT);
        mensaje.getStyleClass().add("error");

        VBox.setVgrow(graficos, Priority.ALWAYS);
        graficos.setMinHeight(250);
        getChildren().addAll(titulo, filtro, mensaje, tarjetas, graficos, inferior, alertas);

        // Al abrir, se muestra el ultimo periodo con datos.
        Async.ejecutar(controlador::ultimoPeriodo, ultimo -> {
            if (ultimo == null) {
                mensaje.setText("Todavía no hay datos de consumo cargados.");
            } else {
                periodo.setText(ultimo);
                cargar(ultimo);
            }
        }, fallo -> mensaje.setText("No se pudo consultar: " + Async.mensaje(fallo)));
    }

    private void cargar(String p) {
        mensaje.setText("");
        Async.ejecutar(() -> controlador.dashboard(p), this::mostrar, fallo -> mensaje.setText(Async.mensaje(fallo)));
    }

    private void mostrar(DatosDashboard d) {
        tarjetas.getChildren().setAll(
            kpi("Costo total (USD)", NUM.format(d.costoTotal()), null),
            kpi("Servicios monitoreados", String.valueOf(d.serviciosMonitoreados()), null),
            kpi("Servicios con cobertura duplicada", String.valueOf(d.serviciosDuplicados()), d.serviciosDuplicados() > 0 ? "kpi-alerta" : null),
            kpi("Ahorro potencial (USD)", NUM.format(d.ahorroPotencial()), "kpi-ahorro"),
            kpi("Herramientas sobre el umbral", String.valueOf(d.alertas().size()), d.alertas().isEmpty() ? null : "kpi-alerta"));
        graficos.getChildren().setAll(barras(d.costoPorHerramienta()), dona(d.costoPorTribu()));

        inferior.getChildren().setAll(mensual(d.anual()), ranking(d.topServicios()));

        alertas.getChildren().clear();
        for (AlertaUmbral a : d.alertas()) {
            Label l = new Label("Alerta: " + a.herramienta() + " supera su umbral mensual (USD " + NUM.format(a.costoUsd())
                                + " > USD " + NUM.format(a.umbralUsd()) + ").");
            l.getStyleClass().add("error");
            alertas.getChildren().add(l);
        }
    }

    /** Barras de los 12 meses del anio: recorre el arreglo de ResumenAnual con un for. */
    private Node mensual(ResumenAnual anual) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        BarChart<String, Number> grafico = new BarChart<>(x, y);
        grafico.setTitle("Costo por mes " + anual.getAnio() + "  (promedio USD " + NUM.format(anual.promedioMensual())
                         + ", mes más caro: " + anual.nombreMesMasCaro() + ")");
        grafico.setLegendVisible(false);
        grafico.setAnimated(false);
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        BigDecimal[] costos = anual.getCostoPorMes();
        for (int i = 0; i < costos.length; i++) {
            serie.getData().add(new XYChart.Data<>(ResumenAnual.MESES[i], costos[i]));
        }
        grafico.getData().add(serie);
        grafico.setPrefHeight(165);
        grafico.setMinHeight(165);
        grafico.setMaxHeight(165);
        HBox.setHgrow(grafico, Priority.ALWAYS);
        return grafico;
    }

    /** Ranking Top 3: recorre el arreglo que arma el gestor (puede tener menos de 3 posiciones). */
    private Node ranking(ComparacionFila[] top) {
        Label titulo = new Label("Top 3 servicios más costosos");
        titulo.getStyleClass().add("grafico-titulo");
        VBox caja = new VBox(8, titulo);
        for (int i = 0; i < top.length; i++) {
            Label l = new Label((i + 1) + ". " + top[i].servicio() + " (" + top[i].herramienta() + ")  USD " + NUM.format(top[i].costoUsd()));
            l.setWrapText(true);
            caja.getChildren().add(l);
        }
        if (top.length == 0) {
            caja.getChildren().add(new Label("Sin datos en el período."));
        }
        caja.setPrefWidth(330);
        caja.setMinWidth(330);
        caja.setPrefHeight(165);
        caja.setMaxHeight(165);
        caja.getStyleClass().add("kpi");
        return caja;
    }

    private Node kpi(String etiqueta, String valor, String estiloExtra) {
        Label v = new Label(valor);
        v.getStyleClass().add("kpi-valor");
        Label e = new Label(etiqueta);
        e.getStyleClass().add("kpi-etiqueta");
        e.setWrapText(true);
        e.setMinHeight(30);
        e.setPrefHeight(30);
        VBox caja = new VBox(4, e, v);
        caja.getStyleClass().add("kpi");
        if (estiloExtra != null) {
            caja.getStyleClass().add(estiloExtra);
        }
        HBox.setHgrow(caja, Priority.ALWAYS);
        caja.setMaxWidth(Double.MAX_VALUE);
        caja.setPrefWidth(160);
        return caja;
    }

    private Node barras(Map<String, BigDecimal> datos) {
        CategoryAxis x = new CategoryAxis();
        x.setTickLabelRotation(0);
        NumberAxis y = new NumberAxis();
        y.setLabel("USD");
        BarChart<String, Number> grafico = new BarChart<>(x, y);
        grafico.setTitle("Costo por herramienta");
        grafico.setLegendVisible(false);
        grafico.setAnimated(false);
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        int i = 0;
        for (Map.Entry<String, BigDecimal> e : datos.entrySet()) {
            XYChart.Data<String, Number> punto = new XYChart.Data<>(e.getKey(), e.getValue());
            String color = COLORES[i++ % COLORES.length];
            punto.nodeProperty().addListener((obs, viejo, nodo) -> {
                if (nodo != null) {
                    nodo.setStyle("-fx-bar-fill: " + color + ";");
                }
            });
            serie.getData().add(punto);
        }
        grafico.getData().add(serie);
        HBox.setHgrow(grafico, Priority.ALWAYS);
        grafico.setPrefWidth(420);
        return grafico;
    }

    /** Dona: PieChart sin leyenda propia, con un circulo central del color de fondo, el total al centro y leyenda a la derecha. */
    private Node dona(Map<String, BigDecimal> datos) {
        PieChart pie = new PieChart();
        pie.setAnimated(false);
        pie.setLabelsVisible(false);
        pie.setLegendVisible(false);
        BigDecimal total = datos.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        VBox leyenda = new VBox(6);
        leyenda.setAlignment(Pos.CENTER_LEFT);
        leyenda.setMinWidth(Region.USE_PREF_SIZE);
        int i = 0;
        for (Map.Entry<String, BigDecimal> e : datos.entrySet()) {
            String pct = total.signum() == 0 ? "0" : e.getValue().multiply(BigDecimal.valueOf(100))
                                                     .divide(total, 0, java.math.RoundingMode.HALF_UP).toPlainString();
            String color = COLORES[i++ % COLORES.length];
            PieChart.Data porcion = new PieChart.Data(e.getKey(), e.getValue().doubleValue());
            porcion.nodeProperty().addListener((obs, viejo, nodo) -> {
                if (nodo != null) {
                    nodo.setStyle("-fx-pie-color: " + color + ";");
                }
            });
            pie.getData().add(porcion);
            Region punto = new Region();
            punto.setMinSize(12, 12);
            punto.setMaxSize(12, 12);
            punto.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");
            Label texto = new Label(e.getKey() + "  " + pct + "%");
            texto.setMinWidth(Region.USE_PREF_SIZE);
            HBox fila = new HBox(6, punto, texto);
            fila.setAlignment(Pos.CENTER_LEFT);
            leyenda.getChildren().add(fila);
        }
        Circle hueco = new Circle();
        hueco.getStyleClass().add("dona-centro");
        StackPane zona = new StackPane(pie, hueco);
        hueco.radiusProperty().bind(Bindings.min(zona.widthProperty(), zona.heightProperty()).multiply(0.21));
        Label centro = new Label("Total\nUSD " + NUM.format(total));
        centro.getStyleClass().add("dona-total");
        centro.setAlignment(Pos.CENTER);
        centro.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        zona.getChildren().add(centro);
        zona.setMinSize(220, 220);
        HBox.setHgrow(zona, Priority.ALWAYS);

        Label titulo = new Label("Costo por tribu");
        titulo.getStyleClass().add("grafico-titulo");
        HBox cuerpo = new HBox(12, zona, leyenda);
        cuerpo.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(cuerpo, Priority.ALWAYS);
        VBox caja = new VBox(6, titulo, cuerpo);
        caja.setAlignment(Pos.TOP_CENTER);
        caja.setPrefWidth(420);
        HBox.setHgrow(caja, Priority.ALWAYS);
        return caja;
    }
}
