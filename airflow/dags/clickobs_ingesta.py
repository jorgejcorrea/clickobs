"""DAG de ingesta de CLICKOBS: 4 fuentes -> normalizacion -> MySQL (tabla consumo).

Un TaskGroup por herramienta (extraer -> normalizar -> cargar): los carriles son independientes, si una fuente
falla las otras igual se cargan, y el estado del conector se actualiza (Activo / Error). La tarea final
`resumen` corre siempre y FALLA el run si alguna fuente fallo (si no, Airflow lo mostraria como exitoso porque
juzga el estado por la ultima tarea). Se evita el dynamic task mapping encadenado: un fallo en una posicion
deja toda la tarea siguiente en upstream_failed. Compatible con Airflow 2.x y 3.x.
"""
from __future__ import annotations

import logging
import os
import sys
from datetime import timedelta

import pendulum

try:  # Airflow 3
    from airflow.sdk import Param, TaskGroup, dag, get_current_context, task
except ImportError:  # Airflow 2.x
    from airflow.decorators import dag, task
    from airflow.models.param import Param
    from airflow.operators.python import get_current_context
    from airflow.utils.task_group import TaskGroup
from airflow.exceptions import AirflowFailException

# El paquete de ingesta vive en include/ (montado junto a dags/ en el contenedor)
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "include"))
from clickobs_ingesta import adapters, loader, normalize  # noqa: E402

log = logging.getLogger(__name__)
CONN_ID = os.environ.get("CLICKOBS_MYSQL_CONN_ID", "clickobs_mysql")


def _conexion():
    return loader.conexion_desde_airflow(CONN_ID)


REINTENTOS = {"retries": int(os.environ.get("CLICKOBS_RETRIES", "3")), "retry_delay": timedelta(minutes=2), "retry_exponential_backoff": True,
              "max_retry_delay": timedelta(minutes=20)}


@dag(
    dag_id="clickobs_ingesta",
    description="Ingesta de consumo y costo de Dynatrace, Datadog, Grafana/Prometheus y Elastic hacia MySQL",
    schedule="0 * * * *",            # cada hora; ver airflow/README.md: 10 minutos no se justifica para datos de facturacion
    start_date=pendulum.datetime(2026, 9, 1, tz="America/Argentina/Buenos_Aires"),  # solo define desde cuando puede programarse
    catchup=False,
    max_active_runs=1,
    tags=["clickobs", "finops"],
    params={
        "periodo": Param("", type="string", description="AAAA-MM. Vacio = mes del intervalo de datos."),
        "modo": Param("muestra", enum=["muestra", "api"], description="muestra = JSON de ejemplo; api = APIs reales"),
    },
)
def clickobs_ingesta():
    @task
    def resolver_periodo() -> str:
        ctx = get_current_context()
        p = (ctx["params"].get("periodo") or "").strip()
        return p or ctx["data_interval_end"].strftime("%Y-%m")

    @task(**REINTENTOS)
    def extraer(fuente: str, periodo: str) -> dict:
        modo = get_current_context()["params"].get("modo", "muestra")
        try:
            crudo = adapters.FUENTES[fuente].extraer(periodo, modo)
        except Exception:
            loader.marcar_estado_conector(_conexion, fuente, "Error")
            raise
        log.info("[%s] extraccion OK (modo=%s)", fuente, modo)
        return {"fuente": fuente, "crudo": crudo}   # se devuelve la fuente junto con el dato para no desalinearlos

    @task
    def normalizar(extraido: dict, periodo: str) -> dict:
        fuente = extraido["fuente"]
        try:
            resultado = normalize.normalizar(fuente, periodo, extraido["crudo"])
        except Exception:
            loader.marcar_estado_conector(_conexion, fuente, "Error")
            raise
        for r in resultado["rechazados"]:
            log.warning("[%s] registro rechazado: %s | %s", fuente, r["motivo"], r["registro"])
        if not resultado["filas"]:
            loader.marcar_estado_conector(_conexion, fuente, "Error")
            raise ValueError(f"[{fuente}] no quedaron filas validas para cargar")
        return resultado

    @task(**REINTENTOS)
    def cargar(resultado: dict) -> dict:
        fuente, periodo = resultado["fuente"], resultado["periodo"]
        try:
            n = loader.reemplazar_consumo(_conexion, fuente, periodo, resultado["filas"])
        except Exception:
            loader.marcar_estado_conector(_conexion, fuente, "Error")
            raise
        loader.marcar_estado_conector(_conexion, fuente, "Activo")
        log.info("[%s] %s: %d fila(s) cargada(s), %d rechazada(s)", fuente, periodo, n, len(resultado["rechazados"]))
        return {"fuente": fuente, "periodo": periodo, "cargadas": n, "rechazadas": len(resultado["rechazados"])}

    @task(trigger_rule="all_done")
    def resumen(resultados: list, fuentes: list) -> None:
        fallidas = []
        for fuente, r in zip(fuentes, resultados or []):
            if r:
                log.info("RESUMEN %s", r)
            else:
                fallidas.append(fuente)
        # Punto de extension: notificar al backend Spring Boot (POST /internal/ingesta/completada) para refrescar clientes.
        if fallidas:
            raise AirflowFailException(f"Fuentes con error: {', '.join(fallidas)} (ver logs de cada tarea)")

    periodo = resolver_periodo()
    fuentes = list(adapters.FUENTES)
    cargados = []
    for fuente in fuentes:
        with TaskGroup(group_id=fuente):
            extraido = extraer(fuente, periodo)
            normalizado = normalizar(extraido, periodo)
            cargados.append(cargar(normalizado))
    resumen(cargados, fuentes)


clickobs_ingesta()
