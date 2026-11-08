import com.clickobs.controller.AdministracionController;
import com.clickobs.controller.AlertasController;
import com.clickobs.controller.ComparaCostosController;
import com.clickobs.controller.ConsumoController;
import com.clickobs.controller.DashboardController;
import com.clickobs.controller.LoginController;
import com.clickobs.controller.RecomendacionesController;
import com.clickobs.controller.SesionController;
import com.clickobs.dao.ConectorDao;
import com.clickobs.dao.Dao;
import com.clickobs.dao.DaoBase;
import com.clickobs.dao.HerramientaDao;
import com.clickobs.db.Db;
import com.clickobs.excepcion.EstructuraVaciaException;
import com.clickobs.excepcion.MontoInvalidoException;
import com.clickobs.excepcion.PeriodoInvalidoException;
import com.clickobs.excepcion.PersistenciaException;
import com.clickobs.excepcion.ValidacionException;
import com.clickobs.model.AlertaUmbral;
import com.clickobs.model.Conector;
import com.clickobs.model.Herramienta;
import com.clickobs.model.HerramientaSaaS;
import com.clickobs.model.HerramientaSelfHosted;
import com.clickobs.model.ComparacionFila;
import com.clickobs.model.Pantalla;
import com.clickobs.model.ResumenAnual;
import com.clickobs.model.Permisos;
import com.clickobs.model.UmbralAlerta;
import com.clickobs.model.Usuario;
import com.clickobs.service.Bitacora;
import com.clickobs.service.GestorAlertas;
import com.clickobs.service.GestorRecomendaciones;
import com.clickobs.service.IASimulada;
import com.clickobs.util.Algoritmos;
import com.clickobs.util.ColaEnlazada;
import com.clickobs.util.PilaEnlazada;
import com.clickobs.util.Validador;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
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
        pruebasTP4();
        pruebasMVC();
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

    /** TP4: arreglos + ArrayList, archivos, excepciones de persistencia, patron DAO. */
    /** Verifica el patron MVC: la estructura del codigo (regla de dependencias) y el funcionamiento de cada controlador. */
    static void pruebasMVC() throws Exception {
        System.out.println("-- TP4: patron MVC (controladores entre las vistas y el modelo)");
        // Regla de dependencias, comprobada sobre el codigo fuente (se ejecuta desde la raiz del repositorio)
        Path ui = Path.of("src/main/java/com/clickobs/ui");
        Path ctrl = Path.of("src/main/java/com/clickobs/controller");
        caso("estructura: existen los paquetes ui y controller", Files.isDirectory(ui) && Files.isDirectory(ctrl));
        caso("MVC: ninguna vista importa dao, service ni db (solo habla con su controlador)", sinImports(ui, "import com.clickobs.dao", "import com.clickobs.service", "import com.clickobs.db"));
        caso("MVC: los controladores no importan JavaFX (no dependen de la vista)", sinImports(ctrl, "import javafx"));
        long vistas = Files.list(ui).filter(p -> p.getFileName().toString().endsWith("View.java")).count();
        long controladores = Files.list(ctrl).filter(p -> p.getFileName().toString().endsWith("Controller.java")).count();
        caso("MVC: hay un controlador para cada una de las 7 pantallas mas la sesion (" + controladores + ")", vistas == 8 && controladores == 8);

        LoginController login = new LoginController();
        caso("LoginController: usuario existente -> presente", login.autenticar("dfarias").isPresent());
        caso("LoginController: usuario inexistente -> vacio", login.autenticar("nadie").isEmpty());

        ConsumoController consumo = new ConsumoController();
        ConsumoController.Inicio inicio = consumo.cargarInicio();
        caso("ConsumoController: carga las 4 herramientas y el ultimo periodo", inicio.herramientas().size() == 4 && inicio.ultimoPeriodo() != null);
        Herramienta primera = inicio.herramientas().get(0);
        ConsumoController.Detalle det = consumo.detalle(primera, "2026-08");
        caso("ConsumoController: detalle con filas y costo calculado por polimorfismo", !det.filas().isEmpty() && det.costoUsd().signum() > 0);
        caso("ConsumoController: periodo invalido -> PeriodoInvalidoException", lanza(PeriodoInvalidoException.class, () -> consumo.detalle(primera, "13-99")));

        AlertasController alertas = new AlertasController();
        AlertasController.Inicio ai = alertas.cargarInicio();
        caso("AlertasController: carga herramientas y umbrales", ai.herramientas().size() == 4 && !ai.umbrales().isEmpty());
        caso("AlertasController: monto invalido -> MontoInvalidoException", lanza(MontoInvalidoException.class, () -> alertas.guardarUmbral(1, "abc")));
        UmbralAlerta u = ai.umbrales().get(0);
        List<UmbralAlerta> tras = alertas.guardarUmbral(u.getIdHerramienta(), u.getMontoMaximo().toPlainString());
        caso("AlertasController: guardar el mismo umbral (UPDATE) deja los datos igual", tras.size() == ai.umbrales().size());
        int encoladas = alertas.evaluar("2026-08");
        caso("AlertasController: evaluar 2026-08 encola alertas y la cola las conserva", encoladas >= 1 && alertas.pendientes().size() == encoladas);
        caso("AlertasController: atenderSiguiente saca la primera de la cola", alertas.atenderSiguiente() != null && alertas.pendientes().size() == encoladas - 1);

        DashboardController dash = new DashboardController();
        caso("DashboardController: costo total de 2026-08 = 7570", dash.dashboard("2026-08").costoTotal().compareTo(new BigDecimal("7570")) == 0);

        ComparaCostosController comp = new ComparaCostosController();
        List<ComparacionFila> filas = comp.comparar("2026-08");
        Path csv = Files.createTempFile("comparacion", ".csv");
        comp.exportar(filas, csv);
        caso("ComparaCostosController: compara y exporta un archivo CSV con contenido", !filas.isEmpty() && Files.size(csv) > 0);
        Files.delete(csv);

        caso("RecomendacionesController: el historial se recupera de MySQL", new RecomendacionesController().historial() != null);

        AdministracionController admin = new AdministracionController();
        List<Conector> conectores = admin.listarConectores();
        Conector c0 = conectores.get(0);
        caso("AdministracionController: cambiar al mismo estado (UPDATE) mantiene los 4 conectores",
            admin.cambiarEstado(c0, c0.getEstado()).size() == conectores.size());
        caso("AdministracionController: estado invalido -> ValidacionException", lanza(ValidacionException.class, () -> admin.cambiarEstado(c0, "Roto")));

        Path bit = Files.createTempFile("sesion", ".csv");
        Files.delete(bit);
        System.setProperty("clickobs.bitacora", bit.toString());
        new SesionController().registrar("dfarias", "Abrio: Dashboard");
        System.clearProperty("clickobs.bitacora");
        caso("SesionController: registra el evento en el archivo de la bitacora", Files.exists(bit) && Files.readString(bit).contains("dfarias;Abrio: Dashboard"));
        Files.deleteIfExists(bit);
    }

    /** true si ningun .java de la carpeta contiene alguna de las cadenas indicadas. */
    static boolean sinImports(Path carpeta, String... prohibidos) throws Exception {
        try (var archivos = Files.walk(carpeta)) {
            for (Path p : (Iterable<Path>) archivos.filter(f -> f.toString().endsWith(".java"))::iterator) {
                String texto = Files.readString(p);
                for (String x : prohibidos) {
                    if (texto.contains(x)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    static void pruebasTP4() throws Exception {
        System.out.println("-- TP4: arreglos, archivos, excepciones de persistencia y patron DAO");
        // Arreglos (ResumenAnual: 12 posiciones fijas)
        BigDecimal[] costos = new BigDecimal[12];
        costos[1] = new BigDecimal("100"); costos[2] = new BigDecimal("300"); costos[4] = new BigDecimal("200");
        ResumenAnual ra = new ResumenAnual("2026", costos);
        caso("ResumenAnual: 12 etiquetas de mes", ResumenAnual.MESES.length == 12 && ResumenAnual.MESES[0].equals("Ene"));
        caso("ResumenAnual: los meses null se toman como 0", ra.getCostoPorMes()[0].signum() == 0 && ra.getCostoPorMes().length == 12);
        caso("ResumenAnual: total = 600", ra.total().compareTo(new BigDecimal("600")) == 0);
        caso("ResumenAnual: 3 meses con datos y promedio 200", ra.mesesConDatos() == 3 && ra.promedioMensual().compareTo(new BigDecimal("200")) == 0);
        caso("ResumenAnual: mes mas caro = Mar (indice 2)", ra.indiceMesMasCaro() == 2 && ra.nombreMesMasCaro().equals("Mar"));
        caso("ResumenAnual vacio: promedio 0 y mes mas caro '-'", new ResumenAnual("2026", new BigDecimal[12]).promedioMensual().signum() == 0
            && new ResumenAnual("2026", new BigDecimal[12]).nombreMesMasCaro().equals("-"));
        caso("ResumenAnual con arreglo de 11 posiciones -> IllegalArgumentException",
            lanza(IllegalArgumentException.class, () -> new ResumenAnual("2026", new BigDecimal[11])));
        ra.getCostoPorMes()[2] = BigDecimal.ZERO;   // modificar la copia no debe afectar al objeto
        caso("encapsulamiento: getCostoPorMes devuelve una copia", ra.total().compareTo(new BigDecimal("600")) == 0);

        // Archivos (Bitacora)
        Path tmp = Files.createTempFile("bitacora", ".csv");
        Files.delete(tmp);
        Bitacora b = new Bitacora(tmp);
        caso("bitacora sin archivo -> lista vacia", b.ultimos(5).isEmpty());
        caso("bitacora: registrar crea el archivo", b.registrar("jorge", "Inicio de sesion") && Files.exists(tmp));
        b.registrar("jorge", "Abrio; Dashboard");
        b.registrar("ana", "Cierre de sesion");
        List<String[]> ultimos = b.ultimos(2);
        caso("bitacora: ultimos(2) devuelve 2 eventos, el mas reciente primero", ultimos.size() == 2 && ultimos.get(0)[1].equals("ana"));
        caso("bitacora: cada evento es un arreglo de 3 posiciones", ultimos.get(1).length == 3 && ultimos.get(1)[1].equals("jorge"));
        caso("bitacora: el ';' del texto no rompe el formato", ultimos.get(1)[2].equals("Abrio, Dashboard"));
        caso("bitacora: ultimos(10) con 3 eventos devuelve 3", b.ultimos(10).size() == 3);
        caso("bitacora en una ruta imposible: registrar devuelve false y no lanza", !new Bitacora(Path.of("/ruta/que/no/existe/b.csv")).registrar("x", "y"));
        Files.delete(tmp);

        // Excepciones de persistencia
        caso("traduccion 1045 -> clave incorrecta", PersistenciaException.de("conectar", new SQLException("x", "28000", 1045))
            .getMessage().contains("clave"));
        caso("traduccion 1146 -> tabla faltante", PersistenciaException.de("leer", new SQLException("x", "42S02", 1146))
            .getMessage().contains("falta una tabla"));
        caso("traduccion 08S01 -> sin conexion", PersistenciaException.de("leer", new SQLException("x", "08S01", 0))
            .getMessage().contains("conexion"));
        PersistenciaException pe = PersistenciaException.de("leer", new SQLException("x", "42S02", 1146));
        caso("PersistenciaException guarda el codigo y la causa original", pe.getCodigoSql() == 1146 && pe.getCause() instanceof SQLException);

        // Patron DAO: DaoBase (clase abstracta) + interfaz Dao
        var dao = new DaoBase() {
            int contarHerramientas() throws PersistenciaException {
                return consultar("contar", "SELECT COUNT(*) FROM herramienta", rs -> rs.getInt(1)).get(0);
            }
            void tablaInexistente() throws PersistenciaException {
                consultar("leer", "SELECT * FROM tabla_que_no_existe", rs -> 1);
            }
            int actualizarNada() throws PersistenciaException {
                return actualizar("actualizar", "UPDATE herramienta SET nombre = nombre WHERE id_herramienta = ?", -1);
            }
        };
        caso("DaoBase.consultar: cuenta las 4 herramientas", dao.contarHerramientas() == 4);
        caso("DaoBase.consultar: tabla inexistente -> PersistenciaException (no SQLException)", lanza(PersistenciaException.class, dao::tablaInexistente));
        caso("DaoBase.actualizar: devuelve 0 filas afectadas con un id inexistente", dao.actualizarNada() == 0);
        Dao<?> conectores = new ConectorDao();
        caso("ConectorDao cumple la interfaz Dao y lista conectores", !conectores.listar().isEmpty());

        // Con la base: ranking (arreglo) y evolucion mensual (arreglo de 12)
        GestorRecomendaciones gr = new GestorRecomendaciones(new IASimulada());
        ComparacionFila[] top = gr.topServicios("2026-08", 3);
        caso("topServicios devuelve un arreglo de 3 posiciones", top.length == 3);
        caso("topServicios esta ordenado de mayor a menor costo (3200, 2100, 850)", top[0].costoUsd().compareTo(new BigDecimal("3200")) == 0
            && top[1].costoUsd().compareTo(new BigDecimal("2100")) == 0 && top[2].costoUsd().compareTo(new BigDecimal("850")) == 0);
        caso("topServicios con periodo sin datos -> arreglo vacio", gr.topServicios("2030-01", 3).length == 0);
        caso("topServicios con n mayor que los datos -> arreglo del tamano de los datos (5)", gr.topServicios("2026-08", 10).length == 5);
        BigDecimal[] porMes = new com.clickobs.dao.ConsumoDao().costosPorMes("2026");
        caso("costosPorMes: arreglo de 12; agosto (indice 7) = 7570; enero = 0", porMes.length == 12
            && porMes[7].compareTo(new BigDecimal("7570")) == 0 && porMes[0].signum() == 0);
        ResumenAnual anual = gr.dashboard("2026-08").anual();
        caso("dashboard incluye el resumen anual: mes mas caro = Ago", anual.nombreMesMasCaro().equals("Ago"));
        caso("dashboard incluye el Top 3 (arreglo)", gr.dashboard("2026-08").topServicios().length == 3);
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
