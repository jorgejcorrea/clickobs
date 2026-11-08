"""Carga idempotente en MySQL. Recibe una funcion que devuelve una conexion DB-API (paramstyle %s).

En Airflow la conexion sale de un Connection (MySqlHook); en las pruebas locales, de PyMySQL.
Estrategia: por (herramienta, periodo) se borran e insertan las filas dentro de UNA transaccion, de modo que
ejecutar la ingesta dos veces produce el mismo resultado (idempotencia) y nunca queda el periodo a medias.
La PK de `consumo` no es AUTO_INCREMENT: los ids se calculan como MAX + 1 dentro de la transaccion.
Mejora prevista (Construccion): AUTO_INCREMENT + UNIQUE(id_herramienta, id_servicio, periodo) y usar
INSERT ... ON DUPLICATE KEY UPDATE.
"""
from __future__ import annotations

from decimal import Decimal
from typing import Callable

HERRAMIENTA_POR_FUENTE = {"dynatrace": "Dynatrace", "datadog": "Datadog",
                          "prometheus": "Grafana+Prometheus", "elastic": "Elastic"}


def conexion_desde_airflow(conn_id: str = "clickobs_mysql"):
    from airflow.providers.mysql.hooks.mysql import MySqlHook  # import diferido: el DAG parsea sin el proveedor
    return MySqlHook(mysql_conn_id=conn_id).get_conn()


def reemplazar_consumo(conexion: Callable, fuente: str, periodo: str, filas: list[dict]) -> int:
    conn = conexion()
    try:
        cur = conn.cursor()
        cur.execute("SELECT id_herramienta FROM herramienta WHERE nombre = %s", (HERRAMIENTA_POR_FUENTE[fuente],))
        fila = cur.fetchone()
        if not fila:
            raise ValueError(f"La herramienta de la fuente {fuente!r} no existe en la tabla herramienta")
        id_herramienta = fila[0]
        cur.execute("SELECT nombre, id_servicio FROM servicio")
        servicios = {n.lower(): i for n, i in cur.fetchall()}
        cur.execute("DELETE FROM consumo WHERE id_herramienta = %s AND periodo = %s", (id_herramienta, periodo))
        cur.execute("SELECT COALESCE(MAX(id_consumo), 0) FROM consumo")
        siguiente = cur.fetchone()[0] + 1
        for f in filas:
            id_servicio = servicios.get(f["servicio"].lower())
            if id_servicio is None:
                raise ValueError(f"El servicio {f['servicio']!r} no existe en la tabla servicio")
            cur.execute(
                "INSERT INTO consumo (id_consumo, id_herramienta, id_servicio, periodo, cantidad_usada, unidad_medida, monto_usd) "
                "VALUES (%s, %s, %s, %s, %s, %s, %s)",
                (siguiente, id_herramienta, id_servicio, periodo, Decimal(f["cantidad_usada"]), f["unidad_medida"], Decimal(f["monto_usd"])))
            siguiente += 1
        conn.commit()
        return len(filas)
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def marcar_estado_conector(conexion: Callable, fuente: str, estado: str) -> None:
    """Actualiza conector.estado ('Activo' / 'Error'): es lo que muestra el estado de sincronizacion (RF-07)."""
    conn = conexion()
    try:
        cur = conn.cursor()
        cur.execute("UPDATE conector SET estado = %s WHERE id_herramienta = "
                    "(SELECT id_herramienta FROM herramienta WHERE nombre = %s)", (estado, HERRAMIENTA_POR_FUENTE[fuente]))
        conn.commit()
    finally:
        conn.close()
