import com.clickobs.dao.ConectorDao;
import com.clickobs.dao.HerramientaDao;
import com.clickobs.db.Db;
import com.clickobs.excepcion.EstructuraVaciaException;
import com.clickobs.excepcion.MontoInvalidoException;
import com.clickobs.excepcion.PeriodoInvalidoException;
import com.clickobs.excepcion.ValidacionException;
import com.clickobs.model.AlertaUmbral;
import com.clickobs.model.Conector;
import com.clickobs.model.Herramienta;
import com.clickobs.model.HerramientaSaaS;
import com.clickobs.model.HerramientaSelfHosted;
import com.clickobs.model.Pantalla;
import com.clickobs.model.Permisos;
import com.clickobs.model.UmbralAlerta;
import com.clickobs.model.Usuario;
import com.clickobs.service.GestorAlertas;
import com.clickobs.service.GestorRecomendaciones;
import com.clickobs.service.IASimulada;
import com.clickobs.util.Algoritmos;
import com.clickobs.util.ColaEnlazada;
import com.clickobs.util.PilaEnlazada;
import com.clickobs.util.Validador;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;

/**
 * Pruebas ejecutables sin JUnit (la consigna del TP3 no lo exige). Cada caso imprime OK o FALLO.
 * Ejecutar: ver pruebas/README.md. Los casos con base de datos dejan los datos como los encontraron.
 */
public class PruebasClickobs {

    static int ok = 0;
    static int fallos = 0;

    static void caso(String nombre, boolean condicion) {
        if (condicion) { ok++; System.out.println("OK     " + nombre); }
        else { fallos++; System.out.println("FALLO  " + nombre); }
    }

    interface Accion { void run() throws Exception; }

    static boolean lanza(Class<? extends Throwable> tipo, Accion a) {
        try { a.run(); return false; } catch (Throwable t) { return tipo.isInstance(t); }
    }

    public static void main(String[] args) throws Exception {
        pruebasValidador();
        pruebasEstructuras();
        pruebasAlgoritmos();
        pruebasModelo();
        pruebasConBaseDeDatos();
        System.out.println("\nResultado: " + ok + " OK, " + fallos + " FALLO");
        System.exit(fallos == 0 ? 0 : 1);
    }

    static void pruebasValidador() throws Exception {
        System.out.println("-- Validador (caja negra: particiones de equivalencia)");
        caso("periodo 2026-08 valido", Validador.periodo(" 2026-08 ").equals("2026-08"));
        caso("periodo 2026-13 invalido (mes 13)", lanza(PeriodoInvalidoException.class, () -> Validador.periodo("2026-13")));
        caso("periodo 2026-00 invalido (mes 0)", lanza(PeriodoInvalidoException.class, () -> Validador.periodo("2026-00")));
        caso("periodo '2026/08' invalido", lanza(PeriodoInvalidoException.class, () -> Validador.periodo("2026/08")));
        caso("periodo vacio invalido", lanza(PeriodoInvalidoException.class, () -> Validador.periodo("")));
        caso("periodo null invalido", lanza(PeriodoInvalidoException.class, () -> Validador.periodo(null)));
        caso("monto 3500 valido", Validador.monto("3500").compareTo(new BigDecimal("3500")) == 0);
        caso("monto 3500.50 valido", Validador.monto("3500.50").compareTo(new BigDecimal("3500.50")) == 0);
        caso("monto 3.500,50 (formato AR) valido", Validador.monto("3.500,50").compareTo(new BigDecimal("3500.50")) == 0);
        caso("monto 'abc' -> MontoInvalidoException", lanza(MontoInvalidoException.class, () -> Validador.monto("abc")));
        caso("monto '12abc' -> MontoInvalidoException", lanza(MontoInvalidoException.class, () -> Validador.monto("12abc")));
        caso("monto vacio -> MontoInvalidoException", lanza(MontoInvalidoException.class, () -> Validador.monto("  ")));
        caso("monto 0 (limite) -> invalido", lanza(MontoInvalidoException.class, () -> Validador.monto("0")));
        caso("monto -5 -> invalido", lanza(MontoInvalidoException.class, () -> Validador.monto("-5")));
        caso("monto 10000000 (limite superior) valido", Validador.monto("10000000").signum() > 0);
        caso("monto 10000001 -> invalido", lanza(MontoInvalidoException.class, () -> Validador.monto("10000001")));
    }

    static void pruebasEstructuras() {
        System.out.println("-- Pila y cola (caja blanca: recorre todas las ramas)");
        PilaEnlazada<String> pila = new PilaEnlazada<>();
        caso("pila nueva esta vacia", pila.estaVacia() && pila.tamanio() == 0);
        pila.apilar("A"); pila.apilar("B"); pila.apilar("C");
        caso("pila: el tope es el ultimo apilado", pila.verTope().equals("C") && pila.tamanio() == 3);
        caso("pila LIFO: sale C, B, A", pila.desapilar().equals("C") && pila.desapilar().equals("B") && pila.desapilar().equals("A"));
        caso("pila vacia: desapilar lanza EstructuraVaciaException", lanza(EstructuraVaciaException.class, pila::desapilar));
        caso("pila vacia: verTope lanza EstructuraVaciaException", lanza(EstructuraVaciaException.class, pila::verTope));

        ColaEnlazada<Integer> cola = new ColaEnlazada<>();
        caso("cola nueva esta vacia", cola.estaVacia());
        cola.encolar(1); cola.encolar(2); cola.encolar(3);
        caso("cola: el frente es el primero", cola.verFrente() == 1 && cola.tamanio() == 3);
        caso("cola FIFO: sale 1, 2, 3", cola.desencolar() == 1 && cola.desencolar() == 2 && cola.desencolar() == 3);
        caso("cola vaciada: se puede volver a usar", cola.estaVacia() && lanza(EstructuraVaciaException.class, cola::desencolar));
        cola.encolar(9);
        caso("cola reutilizada tras vaciarse", cola.verFrente() == 9 && cola.tamanio() == 1);
        cola.vaciar();
        caso("cola.vaciar() la deja vacia", cola.estaVacia() && cola.comoLista().isEmpty());
    }

    static void pruebasAlgoritmos() {
        System.out.println("-- Ordenamiento y busqueda");
        List<Integer> datos = List.of(5, 3, 9, 1, 3);
        caso("seleccion ascendente", Algoritmos.ordenarPorSeleccion(datos, Comparator.naturalOrder()).equals(List.of(1, 3, 3, 5, 9)));
        caso("seleccion descendente", Algoritmos.ordenarPorSeleccion(datos, Comparator.reverseOrder()).equals(List.of(9, 5, 3, 3, 1)));
        caso("seleccion no modifica la lista original", datos.equals(List.of(5, 3, 9, 1, 3)));
        caso("seleccion con lista vacia", Algoritmos.ordenarPorSeleccion(List.<Integer>of(), Comparator.naturalOrder()).isEmpty());
        caso("seleccion con un elemento", Algoritmos.ordenarPorSeleccion(List.of(7), Comparator.naturalOrder()).equals(List.of(7)));
        List<String> nombres = List.of("Datadog", "Dynatrace", "Dynatrace", "Elastic", "Grafana+Prometheus");
        Comparator<String> ci = String.CASE_INSENSITIVE_ORDER;
        caso("binaria: encuentra la PRIMERA repetida", Algoritmos.buscarBinaria(nombres, "dynatrace", s -> s, ci) == 1);
        caso("binaria: primer elemento", Algoritmos.buscarBinaria(nombres, "Datadog", s -> s, ci) == 0);
        caso("binaria: ultimo elemento", Algoritmos.buscarBinaria(nombres, "Grafana+Prometheus", s -> s, ci) == 4);
        caso("binaria: no existe -> -1", Algoritmos.buscarBinaria(nombres, "Splunk", s -> s, ci) == -1);
        caso("binaria: lista vacia -> -1", Algoritmos.buscarBinaria(List.<String>of(), "x", s -> s, ci) == -1);
        caso("lineal: encuentra", Algoritmos.buscarLineal(datos, n -> n == 9) == 2);
        caso("lineal: no existe -> -1", Algoritmos.buscarLineal(datos, n -> n == 42) == -1);
    }

    static void pruebasModelo() throws Exception {
        System.out.println("-- Modelo: encapsulamiento, constructores, polimorfismo, permisos");
        caso("Herramienta con nombre vacio -> IllegalArgumentException",
            lanza(IllegalArgumentException.class, () -> new HerramientaSaaS(1, " ", "x", "y")));
        caso("Herramienta con id 0 -> IllegalArgumentException",
            lanza(IllegalArgumentException.class, () -> new HerramientaSaaS(0, "Datadog", "x", "y")));
        caso("Self-hosted con costo fijo negativo -> IllegalArgumentException",
            lanza(IllegalArgumentException.class, () -> new HerramientaSelfHosted(3, "G", "x", "y", new BigDecimal("-1"))));
        Herramienta saas = new HerramientaSaaS(2, "Datadog", "Datadog Inc", "Host");
        Herramienta self = new HerramientaSelfHosted(3, "Grafana", "Self", "Infra", new BigDecimal("800"));
        caso("polimorfismo: getTipo distinto", saas.getTipo().equals("SaaS") && self.getTipo().equals("Self-hosted"));
        caso("polimorfismo: describirCosto distinto", !saas.describirCosto().equals(self.describirCosto()));
        caso("herencia: ambas son Herramienta", saas instanceof Herramienta && self instanceof Herramienta);
        UmbralAlerta u = new UmbralAlerta(1, 1, "Dynatrace", new BigDecimal("3500"), "Mensual");
        caso("umbral: supera estrictamente", u.esSuperadoPor(new BigDecimal("3500.01")) && !u.esSuperadoPor(new BigDecimal("3500")));
        caso("umbral: setter rechaza 0", lanza(MontoInvalidoException.class, () -> u.setMontoMaximo(BigDecimal.ZERO)));
        caso("umbral: monto no cambia tras setter invalido", u.getMontoMaximo().compareTo(new BigDecimal("3500")) == 0);
        caso("constructor de UmbralAlerta valida el monto", lanza(MontoInvalidoException.class, () -> new UmbralAlerta(1, 1, "X", null, "Mensual")));
        Conector c = new Conector(1, "Dynatrace", "https://x", "Activo");
        caso("conector: estado 'Error' no se puede fijar a mano", lanza(ValidacionException.class, () -> c.setEstado("Error")));
        c.setEstado("Inactivo");
        caso("conector: cambia a Inactivo", c.getEstado().equals("Inactivo"));
        Usuario cto = new Usuario(1, "Diego", Permisos.CTO, "CTO");
        Usuario sre = new Usuario(2, "SRE", Permisos.SRE_LEAD, "SRE Lead");
        Usuario dev = new Usuario(3, "Dev", Permisos.DEVOPS, "DevOps");
        Usuario fin = new Usuario(4, "Fin", Permisos.FINANZAS, "Finanzas");
        caso("permisos CTO: Recomendaciones, Alertas, Dashboard, Consumo; no Comparar ni Admin",
            Permisos.puedeAcceder(cto, Pantalla.RECOMENDACIONES) && Permisos.puedeAcceder(cto, Pantalla.ALERTAS)
            && Permisos.puedeAcceder(cto, Pantalla.DASHBOARD) && Permisos.puedeAcceder(cto, Pantalla.CONSUMO)
            && !Permisos.puedeAcceder(cto, Pantalla.COMPARAR) && !Permisos.puedeAcceder(cto, Pantalla.ADMINISTRACION));
        caso("permisos SRE: Comparar, Admin, Consumo, Dashboard; no Recomendaciones ni Alertas",
            Permisos.puedeAcceder(sre, Pantalla.COMPARAR) && Permisos.puedeAcceder(sre, Pantalla.ADMINISTRACION)
            && !Permisos.puedeAcceder(sre, Pantalla.RECOMENDACIONES) && !Permisos.puedeAcceder(sre, Pantalla.ALERTAS));
        caso("permisos DevOps: solo Consumo",
            Permisos.puedeAcceder(dev, Pantalla.CONSUMO) && !Permisos.puedeAcceder(dev, Pantalla.DASHBOARD)
            && !Permisos.puedeAcceder(dev, Pantalla.ALERTAS) && !Permisos.puedeAcceder(dev, Pantalla.ADMINISTRACION));
        caso("permisos Finanzas: Dashboard y Alertas, no Comparar",
            Permisos.puedeAcceder(fin, Pantalla.DASHBOARD) && Permisos.puedeAcceder(fin, Pantalla.ALERTAS)
            && !Permisos.puedeAcceder(fin, Pantalla.COMPARAR));
    }

    static void ejecutarSql(String sql) throws Exception {
        try (Connection c = Db.getConnection(); Statement st = c.createStatement()) { st.executeUpdate(sql); }
    }

    static void pruebasConBaseDeDatos() throws Exception {
        System.out.println("-- Con base de datos (script Insert_CLICKOBS.sql cargado)");
        List<Herramienta> hs = new HerramientaDao().listar();
        caso("HerramientaDao crea 4 herramientas", hs.size() == 4);
        caso("Grafana+Prometheus se crea como Self-hosted, el resto SaaS",
            hs.stream().filter(h -> h instanceof HerramientaSelfHosted).count() == 1
            && hs.stream().filter(h -> h instanceof HerramientaSaaS).count() == 3);
        Herramienta dyna = hs.get(0);
        Herramienta graf = hs.get(2);
        caso("costo SaaS Dynatrace 2026-08 = 3980 (3200 + 780)", dyna.calcularCostoPeriodo("2026-08").compareTo(new BigDecimal("3980")) == 0);
        caso("costo self-hosted Grafana 2026-08 = 850 (supera el minimo de 800)", graf.calcularCostoPeriodo("2026-08").compareTo(new BigDecimal("850")) == 0);
        caso("periodo sin datos -> costo 0 (no se imputa el costo fijo)", graf.calcularCostoPeriodo("2030-01").signum() == 0);
        caso("periodo invalido en calcularCostoPeriodo -> PeriodoInvalidoException",
            lanza(PeriodoInvalidoException.class, () -> dyna.calcularCostoPeriodo("agosto")));
        Herramienta graf2 = new HerramientaSelfHosted(3, "Grafana+Prometheus", "Self", "Infra", new BigDecimal("1000"));
        caso("self-hosted: consumo menor al minimo -> se imputa el costo fijo (1000)", graf2.calcularCostoPeriodo("2026-08").compareTo(new BigDecimal("1000")) == 0);

        GestorRecomendaciones gr = new GestorRecomendaciones(new IASimulada());
        caso("comparar costos 2026-08 devuelve 5 filas + 0 sin datos", gr.compararCostos("2026-08").size() == 5);
        caso("comparar costos de periodo vacio marca 4 herramientas sin datos", gr.compararCostos("2030-01").size() == 4);
        caso("comparar con periodo mal escrito -> PeriodoInvalidoException", lanza(PeriodoInvalidoException.class, () -> gr.compararCostos("2026-8")));

        GestorAlertas ga = new GestorAlertas();
        caso("evaluar 2026-08 con umbrales de prueba -> 1 alerta (Dynatrace)", ga.evaluar("2026-08") == 1
            && ga.pendientes().get(0).herramienta().equals("Dynatrace"));
        ga.guardarUmbral(2, "2.000,00");   // Datadog: 2100 > 2000
        caso("umbral Datadog guardado con formato argentino", ga.listarUmbrales().stream()
            .anyMatch(x -> x.getHerramienta().equals("Datadog") && x.getMontoMaximo().compareTo(new BigDecimal("2000")) == 0));
        caso("evaluar -> 2 alertas, la cola ordena primero la de mayor exceso (Dynatrace 13,7% vs Datadog 5,0%)",
            ga.evaluar("2026-08") == 2 && ga.pendientes().get(0).herramienta().equals("Dynatrace"));
        AlertaUmbral primera = ga.atenderSiguiente();
        caso("atenderSiguiente saca Dynatrace (FIFO) y queda 1", primera.herramienta().equals("Dynatrace") && ga.pendientes().size() == 1);
        ga.atenderSiguiente();
        caso("cola vacia -> atenderSiguiente devuelve null", ga.atenderSiguiente() == null);
        caso("umbral con texto 'abc' lanza MontoInvalidoException", lanza(MontoInvalidoException.class, () -> ga.guardarUmbral(2, "abc")));
        caso("el umbral no cambio tras el error", ga.listarUmbrales().stream()
            .anyMatch(x -> x.getHerramienta().equals("Datadog") && x.getMontoMaximo().compareTo(new BigDecimal("2000")) == 0));
        ga.guardarUmbral(2, "2500");
        ejecutarSql("DELETE FROM umbral_alerta WHERE id_herramienta = 3");   // por si quedo uno de prueba
        ga.guardarUmbral(3, "900");
        caso("guardarUmbral crea el umbral si no existia (Grafana)", ga.listarUmbrales().stream().anyMatch(x -> x.getHerramienta().startsWith("Grafana")));
        ejecutarSql("DELETE FROM umbral_alerta WHERE id_herramienta = 3");

        ConectorDao cd = new ConectorDao();
        List<Conector> conectores = cd.listar();
        caso("hay 4 conectores", conectores.size() == 4);
        cd.actualizarEstado(1, Conector.INACTIVO);
        caso("conector 1 queda Inactivo", cd.listar().get(0).getEstado().equals("Inactivo"));
        cd.actualizarEstado(1, Conector.ACTIVO);

        var rec = gr.generarRecomendacion("2026-08");
        caso("generarRecomendacion detecta ahorro (Gateway: Dynatrace 3200 vs Datadog? / API de pagos: 780 vs 2100)",
            rec.ahorroEstimado().signum() > 0 && rec.descripcion().contains("API de pagos"));
        ejecutarSql("DELETE FROM recomendacion_ia WHERE id_recomendacion = " + rec.id());
        caso("generarRecomendacion con periodo sin datos -> IllegalStateException",
            lanza(IllegalStateException.class, () -> gr.generarRecomendacion("2030-01")));
    }
}
