"""Adaptadores de extraccion (patron Adapter, RNF-08): uno por herramienta, todos con la misma interfaz.

Agregar una herramienta nueva = agregar un adaptador y registrarlo en FUENTES; el resto del pipeline no cambia.
Modo "muestra" (por defecto): lee JSON de include/sample_data, con formatos distintos por herramienta para
demostrar la normalizacion. Modo "api": cada adaptador debe completarse con el endpoint real del proveedor
(credenciales SIEMPRE en Connections/Variables de Airflow, nunca en el codigo).
"""
from __future__ import annotations

import json
import os
from pathlib import Path

DATA_DIR = Path(os.environ.get("CLICKOBS_SAMPLE_DIR", Path(__file__).resolve().parents[1] / "sample_data"))


class AdaptadorBase:
    #: clave de la fuente y nombre de la herramienta en la tabla `herramienta`
    fuente: str = ""
    herramienta: str = ""

    def extraer(self, periodo: str, modo: str = "muestra") -> dict:
        if modo == "muestra":
            return json.loads((DATA_DIR / f"{self.fuente}.json").read_text(encoding="utf-8"))
        if modo == "api":
            return self.extraer_api(periodo)
        raise ValueError(f"Modo de ingesta desconocido: {modo!r} (use 'muestra' o 'api')")

    def extraer_api(self, periodo: str) -> dict:  # pragma: no cover - a completar con la API real
        raise NotImplementedError(
            f"Completar la extraccion real de {self.herramienta}: endpoint de uso/costo, autenticacion via "
            f"Connection de Airflow y respetar limites de tasa del proveedor (periodo {periodo})."
        )


class Dynatrace(AdaptadorBase):
    fuente, herramienta = "dynatrace", "Dynatrace"


class Datadog(AdaptadorBase):
    fuente, herramienta = "datadog", "Datadog"


class Prometheus(AdaptadorBase):
    fuente, herramienta = "prometheus", "Grafana+Prometheus"


class Elastic(AdaptadorBase):
    fuente, herramienta = "elastic", "Elastic"


FUENTES: dict[str, AdaptadorBase] = {a.fuente: a for a in (Dynatrace(), Datadog(), Prometheus(), Elastic())}
