"""Normalizacion al modelo canonico de la tabla `consumo`: servicio, cantidad_usada, unidad_medida, monto_usd.

Cada herramienta reporta en su propio formato y unidad (DDU, hosts, GB almacenados, GB indexados).
Aqui se validan los datos, se mapean los identificadores de origen a servicios de CLICKOBS y se deduplican
(filas repetidas del mismo servicio se suman). Los registros invalidos NO se descartan en silencio:
quedan en `rechazados` con el motivo, para su seguimiento (RF-08).
"""
from __future__ import annotations

import json
import os
import re
from decimal import Decimal, InvalidOperation
from pathlib import Path

CONFIG_DIR = Path(os.environ.get("CLICKOBS_CONFIG_DIR", Path(__file__).resolve().parents[1] / "config"))
PERIODO = re.compile(r"^\d{4}-(0[1-9]|1[0-2])$")

# fuente -> (lista de registros, campo id de servicio origen, campo cantidad, campo costo, unidad)
_FORMATOS = {
    "dynatrace": (lambda raw: raw["records"], "managementZone", "ddu", "cost_usd", "DDU"),
    "datadog": (lambda raw: [d["attributes"] for d in raw["data"]], "service", "hosts", "cost_usd", "hosts"),
    "prometheus": (lambda raw: raw["series"], "job", "gb_stored", "infra_cost_usd", "GB almacenados"),
    "elastic": (lambda raw: raw["indices"], "index", "gb_indexed", "cost_usd", "GB indexados"),
}


def alias_servicios() -> dict[str, dict[str, str]]:
    return json.loads((CONFIG_DIR / "service_aliases.json").read_text(encoding="utf-8"))


def _decimal(valor, campo: str) -> Decimal:
    try:
        d = Decimal(str(valor))
    except (InvalidOperation, ValueError):
        raise ValueError(f"{campo} no es numerico: {valor!r}")
    if d < 0:
        raise ValueError(f"{campo} negativo: {valor!r}")
    return d.quantize(Decimal("0.01"))


def normalizar(fuente: str, periodo: str, crudo: dict, alias: dict[str, dict[str, str]] | None = None) -> dict:
    if not PERIODO.match(periodo or ""):
        raise ValueError(f"Periodo invalido {periodo!r}: se espera AAAA-MM")
    if fuente not in _FORMATOS:
        raise ValueError(f"Fuente desconocida: {fuente!r}")
    alias = alias if alias is not None else alias_servicios()
    registros_fn, campo_srv, campo_cant, campo_costo, unidad = _FORMATOS[fuente]
    acumulado: dict[str, dict] = {}
    rechazados: list[dict] = []
    for reg in registros_fn(crudo):
        try:
            origen = reg[campo_srv]
            servicio = alias.get(fuente, {}).get(origen)
            if servicio is None:
                raise ValueError(f"servicio de origen sin mapeo: {origen!r}")
            cantidad = _decimal(reg[campo_cant], campo_cant)
            costo = _decimal(reg[campo_costo], campo_costo)
        except (KeyError, ValueError) as e:
            rechazados.append({"registro": reg, "motivo": str(e)})
            continue
        fila = acumulado.setdefault(servicio, {"servicio": servicio, "cantidad_usada": Decimal("0.00"),
                                              "unidad_medida": unidad, "monto_usd": Decimal("0.00")})
        fila["cantidad_usada"] += cantidad      # deduplicacion: mismo servicio -> se suma
        fila["monto_usd"] += costo
    filas = [{**f, "cantidad_usada": str(f["cantidad_usada"]), "monto_usd": str(f["monto_usd"])} for f in acumulado.values()]
    return {"fuente": fuente, "periodo": periodo, "filas": filas, "rechazados": rechazados}
