package com.clickobs.ui;

import com.clickobs.model.Pantalla;
import com.clickobs.model.Permisos;
import com.clickobs.model.Usuario;
import com.clickobs.service.GestorAlertas;
import com.clickobs.util.PilaEnlazada;
import java.util.EnumMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Ventana principal: menu, barra lateral y area de contenido (segun los mockups de la seccion 16.3).
 * Aplica el control de acceso por rol (RF-04) y lleva el historial de navegacion en una PILA propia:
 * cada vez que el usuario cambia de pantalla se apila la anterior; "Volver" desapila.
 */
public class PrincipalView extends BorderPane {

    private final Usuario usuario;
    private final StackPane contenido = new StackPane();
    private final PilaEnlazada<Pantalla> historial = new PilaEnlazada<>();
    private final GestorAlertas gestorAlertas = new GestorAlertas();   // compartido: la cola de alertas sobrevive a la navegacion
    private final Map<Pantalla, Button> botones = new EnumMap<>(Pantalla.class);
    private final Button volver = new Button("← Volver");
    private final Label pie = new Label();
    private Pantalla actual;

    public PrincipalView(Usuario usuario, Runnable alCerrarSesion, Stage stage) {
        this.usuario = usuario;

        // Menu superior (Archivo / Consultas / Recomendaciones / Administracion / Ayuda)
        MenuItem itemVolver = new MenuItem("Volver");
        itemVolver.setOnAction(e -> volver());
        MenuItem cerrar = new MenuItem("Cerrar sesión");
        cerrar.setOnAction(e -> alCerrarSesion.run());
        MenuItem salir = new MenuItem("Salir");
        salir.setOnAction(e -> stage.close());
        MenuItem acerca = new MenuItem("Acerca de");
        acerca.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                "CLICKOBS - prototipo JavaFX\nSeminario de Práctica de Informática (Universidad Siglo 21)");
            a.setHeaderText("Acerca de CLICKOBS");
            a.showAndWait();
        });
        setTop(new MenuBar(
            new Menu("Archivo", null, itemVolver, cerrar, salir),
            new Menu("Consultas", null, itemMenu(Pantalla.DASHBOARD), itemMenu(Pantalla.CONSUMO), itemMenu(Pantalla.COMPARAR)),
            new Menu("Recomendaciones", null, itemMenu(Pantalla.RECOMENDACIONES)),
            new Menu("Administración", null, itemMenu(Pantalla.ALERTAS), itemMenu(Pantalla.ADMINISTRACION)),
            new Menu("Ayuda", null, acerca)));

        // Barra lateral
        VBox lateral = new VBox(8);
        lateral.setPadding(new Insets(12));
        lateral.setPrefWidth(200);
        lateral.getStyleClass().add("sidebar");
        for (Pantalla p : Pantalla.values()) {
            Button b = new Button(p.getTitulo());
            b.getStyleClass().add("sidebar-button");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> ir(p));
            botones.put(p, b);
            lateral.getChildren().add(b);
        }
        setLeft(lateral);

        // Cabecera del contenido con el boton Volver
        volver.setOnAction(e -> volver());
        volver.setDisable(true);
        HBox cabecera = new HBox(volver);
        cabecera.setPadding(new Insets(8, 16, 0, 16));
        contenido.setPadding(new Insets(8, 16, 16, 16));
        setCenter(new VBox(cabecera, contenido));
        javafx.scene.layout.VBox.setVgrow(contenido, javafx.scene.layout.Priority.ALWAYS);

        pie.setPadding(new Insets(6, 12, 6, 12));
        setBottom(pie);

        // Pantalla inicial: la primera a la que el rol tiene acceso
        for (Pantalla p : Pantalla.values()) {
            if (Permisos.puedeAcceder(usuario, p)) {
                ir(p);
                break;
            }
        }
        if (actual == null) {
            contenido.getChildren().add(new Label("Seleccione una opción del menú lateral."));
            actualizarPie();
        }
    }

    private MenuItem itemMenu(Pantalla p) {
        MenuItem item = new MenuItem(p.getTitulo());
        item.setOnAction(e -> ir(p));
        return item;
    }

    /** Navega a una pantalla. Si es distinta de la actual, apila la actual para poder volver. */
    private void ir(Pantalla destino) {
        if (!Permisos.puedeAcceder(usuario, destino)) {
            contenido.getChildren().setAll(accesoDenegado());
            return;
        }
        if (actual != null && actual != destino) {
            historial.apilar(actual);
        }
        mostrar(destino);
    }

    /** Vuelve a la pantalla anterior: desapila (sin volver a apilar la actual). */
    private void volver() {
        if (!historial.estaVacia()) {
            mostrar(historial.desapilar());
        }
    }

    private void mostrar(Pantalla p) {
        actual = p;
        contenido.getChildren().setAll(crearVista(p));
        botones.forEach((pantalla, boton) -> {
            boton.getStyleClass().remove("sidebar-button-activo");
            if (pantalla == p) {
                boton.getStyleClass().add("sidebar-button-activo");
            }
        });
        actualizarPie();
    }

    private Node crearVista(Pantalla p) {
        switch (p) {
            case DASHBOARD:       return new DashboardView();
            case CONSUMO:         return new ConsumoView();
            case COMPARAR:        return new ComparaCostosView();
            case RECOMENDACIONES: return new RecomendacionesView();
            case ALERTAS:         return new AlertasView(gestorAlertas);
            default:              return new AdministracionView();
        }
    }

    private void actualizarPie() {
        volver.setDisable(historial.estaVacia());
        pie.setText("Usuario: " + usuario.getNombre() + "   |   Rol: " + usuario.getRolNombre()
                  + "   |   Pantallas en el historial: " + historial.tamanio());
    }

    private Node accesoDenegado() {
        Label l = new Label("Acceso denegado: su rol no tiene permiso para esta pantalla.");
        l.getStyleClass().add("error");
        return l;
    }
}
