# Ingesta con Apache Airflow

Trae el consumo y el costo de las 4 herramientas (Dynatrace, Datadog, Grafana/Prometheus y Elastic), los normaliza a un
modelo común y los carga en la tabla `consumo` de MySQL. **Airflow solo escribe en la base; la aplicación JavaFX solo lee.**
El contrato entre ambos es la base de datos.

## Flujo

```
resolver_periodo ─┬─ [dynatrace]  extraer → normalizar → cargar ─┐
                  ├─ [datadog]    extraer → normalizar → cargar ─┤
                  ├─ [prometheus] extraer → normalizar → cargar ─┼─→ resumen
                  └─ [elastic]    extraer → normalizar → cargar ─┘
```

- **extraer**: un adaptador por herramienta (patrón Adapter, RNF-08). Reintentos con espera exponencial (RF-08).
- **normalizar**: valida, mapea el identificador de origen a un servicio de CLICKOBS, deduplica (suma filas repetidas) y
  convierte a `servicio, cantidad_usada, unidad_medida, monto_usd`. Lo inválido **no se descarta en silencio**: queda en
  `rechazados` con el motivo (log de la tarea).
- **cargar**: por (herramienta, período) hace DELETE + INSERT dentro de **una transacción** → idempotente y sin períodos a medias.
  Actualiza `conector.estado` (`Activo` / `Error`), que es lo que muestra el estado de sincronización (RF-07).
- **resumen**: corre siempre y **falla el run si alguna fuente falló** (Airflow evalúa el estado de un run por su última tarea;
  sin esto, un fallo intermedio figuraría como éxito).

Cada herramienta es un carril independiente: si Elastic falla, las otras tres igual se cargan.

## Estructura

```
airflow/
├── dags/clickobs_ingesta.py            DAG (Airflow 2.x y 3.x)
├── include/clickobs_ingesta/           adapters.py · normalize.py · loader.py
├── include/config/service_aliases.json identificador de origen → servicio de CLICKOBS
├── include/sample_data/                JSON de ejemplo (modo "muestra")
└── tests/test_pipeline.py              pruebas de la lógica contra MySQL/MariaDB
```

## Cómo correrlo (Docker)

1. Bajar el `docker-compose.yaml` oficial de Airflow (https://airflow.apache.org/docs/apache-airflow/stable/howto/docker-compose/).
2. Montar las carpetas y definir la conexión en el servicio `x-airflow-common`:
   ```yaml
   volumes:
     - ./airflow/dags:/opt/airflow/dags
     - ./airflow/include:/opt/airflow/include
   environment:
     AIRFLOW_CONN_CLICKOBS_MYSQL: mysql://USUARIO:CLAVE@192.168.139.222:3306/clickobs
   ```
3. `docker compose up airflow-init` y luego `docker compose up -d`. UI en http://localhost:8080.
4. Activar el DAG `clickobs_ingesta` y lanzarlo con configuración `{"periodo": "2026-08", "modo": "muestra"}`.

La contraseña de la base va en la Connection / variable de entorno, **nunca en el código ni en el repositorio**.

## Modos y frecuencia

- `modo = muestra` (por defecto): lee los JSON de `include/sample_data` (los mismos datos de `db/Insert_CLICKOBS.sql`).
- `modo = api`: cada adaptador tiene que completarse con el endpoint real de uso/costo del proveedor. Hoy lanza
  `NotImplementedError` (y el DAG lo trata como fallo, marcando el conector en `Error`).
- **Frecuencia: cada hora** (`0 * * * *`). Los datos de facturación por período (AAAA-MM) no cambian cada 10 minutos y las
  APIs de uso/costo suelen tener latencia y límites de tasa (verificar en la documentación de cada proveedor).
  Cerrar el mes con una corrida manual del período anterior.

## Qué se probó (y qué no)

Probado con Airflow 2.10.5 (`dag.test()`, proveedor MySQL) contra MariaDB, con los scripts de `db/`:

| Escenario | Resultado |
|---|---|
| Camino feliz con la tabla vaciada | 14 tareas OK, 5 filas, total USD 7.570 |
| JSON de Elastic roto | Elastic queda `Error`; se cargan las otras 3 (4 filas, USD 6.930); el run figura `failed` |
| `modo = api` con datos previos | Fallan las 4 extracciones; la tabla no se modifica; conectores en `Error`; run `failed` |
| Lógica (`tests/test_pipeline.py`) | 12/12: normalización, rechazos, deduplicación, idempotencia (2 corridas = mismo resultado), rollback |

**No probado:** el `docker-compose` oficial, Airflow 3.x, las APIs reales de los proveedores y un MySQL 8 real (se usó MariaDB).

## Límites conocidos y mejoras para Construcción

- `consumo.id_consumo` no es AUTO_INCREMENT ni hay clave única (herramienta, servicio, período): el loader calcula `MAX + 1` dentro de la transacción.
  Mejora: `AUTO_INCREMENT` + `UNIQUE` y `INSERT ... ON DUPLICATE KEY UPDATE`.
- Los resultados viajan entre tareas por XCom (en MySQL el valor está limitado a ~64 KB): alcanza para decenas de filas por fuente; con más volumen, usar una tabla de staging.
- Airflow cifra las Connections con Fernet (AES-128). Si el RNF-03 exige AES-256 estricto, usar un Secrets Backend externo (p. ej. Vault) o ajustar el requerimiento.
- El dato del Core Bancario no sale de la red on-premise durante la ingesta; la anonimización se aplica más adelante, al armar el payload hacia la IA.
- Idea para el backend Spring Boot: botón «Sincronizar ahora» que dispare el DAG por la API REST de Airflow (`POST /api/v1/dags/clickobs_ingesta/dagRuns`) y una notificación final desde `resumen` para refrescar los clientes.
