# src

Código fuente del prototipo JavaFX (Maven, Java 21). Las vistas se construyen por código; el paso a FXML y a un backend Spring Boot queda para la fase de Construcción.

```
src/main/java/com/clickobs/
├── App.java, Launcher.java       arranque JavaFX
├── db/Db.java                    conexión JDBC (config por db.properties o variables de entorno)
├── model/                        Herramienta (abstracta) → HerramientaSaaS / HerramientaSelfHosted, records, Permisos
├── dao/                          acceso a MySQL (UsuarioDao, ConsumoDao, HerramientaDao, RecomendacionDao)
├── service/                      GestorRecomendaciones (control), ClienteIA / IASimulada, PayloadIA, ExportadorCsv
└── ui/                           LoginView, PrincipalView, DashboardView, ComparaCostosView, RecomendacionesView
```
