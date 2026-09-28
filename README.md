# CLICKOBS

**Sistema Inteligente de Gestión de Costos y Licenciamiento de Plataformas de Observabilidad basado en IA**

Proyecto del *Seminario de Práctica de Informática* (Universidad Siglo 21). CLICKOBS es un sistema para una fintech ficticia (CLICKOBS S.A., LATAM, ~280 empleados) que centraliza el consumo y el costo de cuatro herramientas de observabilidad (Dynatrace, Datadog, Grafana/Prometheus y Elastic), detecta solapamientos de cobertura y genera recomendaciones y alertas de licenciamiento con IA (vía OpenRouter).

- **Autor:** Jorge Correa Alvarez (legajo VINF02919)
- **Docentes:** Pablo Virgolini y Juan Ojeda
- **Metodología:** Proceso Unificado de Desarrollo (PUD)

![Arquitectura de la solución](docs/img/Arquitectura_Clickobs_Infografia_Funcional.png)

*La infografía es una vista conceptual. El modelo relacional vigente es el de `db/` y el DER de `docs/img/der_clickobs.png`.*

## Arquitectura

| Capa | Tecnología |
|---|---|
| Cliente de escritorio | JavaFX (Windows, macOS, Linux) |
| Backend (arquitectura objetivo) | Java 21 + Spring Boot (API REST) |
| Base de datos | MySQL 8 |
| Ingesta y normalización | Conectores por herramienta + Apache Airflow |
| IA | OpenRouter (gateway hacia modelos de lenguaje) |

**Alcance del prototipo actual:** cliente JavaFX conectado por JDBC directo a MySQL (subconjunto de la arquitectura objetivo). El backend, la orquestación con Airflow y las integraciones reales con las APIs se incorporan en la fase de Construcción.

## Estructura del repositorio

```
clickobs/
├── src/            Código fuente del prototipo JavaFX (pendiente de carga)
├── db/             Scripts SQL (MySQL)
├── docs/           Documento de la entrega (Word y PDF)
│   ├── img/        Diagramas, infografías, mockups y capturas de evidencia
│   └── guias/      Guías paso a paso (entorno MySQL y carga de tablas)
└── README.md
```

## Base de datos

Modelo de 9 tablas: `tribu`, `rol_permiso`, `usuario`, `servicio`, `herramienta`, `conector`, `consumo`, `umbral_alerta` y `recomendacion_ia`. Consumo y Costo se unificaron en la tabla `consumo` (campo `monto_usd`) para evitar un JOIN redundante en la consulta comparativa.

Ejecutar en este orden, sobre un schema vacío llamado `clickobs`:

| # | Script | Función |
|---|---|---|
| 1 | `db/CreacionTablas_CLICKOBS.sql` | Crea las 9 tablas |
| 2 | `db/Insert_CLICKOBS.sql` | Carga datos de prueba |
| 3 | `db/Consulta_CLICKOBS.sql` | CU-02: comparar costos entre herramientas |
| 4 | `db/Borrado_CLICKOBS.sql` | Vacía las tablas y verifica (ejecutar al final) |

```sql
CREATE DATABASE IF NOT EXISTS clickobs;
USE clickobs;
```

Los datos de prueba no llevan tildes a propósito, para evitar problemas de codificación entre el cliente y el servidor MySQL.

## Documentación

- Entrega completa (TP1 + TP2): [`docs/CORREA-JORGE-AP2.pdf`](docs/CORREA-JORGE-AP2.pdf) (versión editable: `.docx`)
- Guías: [`docs/guias/`](docs/guias/)

## Estado

| Fase del PUD | Estado |
|---|---|
| Inicio | Completa |
| Elaboración | Modelo de datos, scripts SQL, diagramas y mockups completos. Prototipo JavaFX en desarrollo |
| Construcción | Pendiente |
| Transición | Pendiente |
