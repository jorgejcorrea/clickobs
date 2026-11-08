"""Pruebas de la ingesta contra una base MySQL/MariaDB con los scripts de db/ ya cargados.
Uso: CLICKOBS_TEST_DB="host=localhost user=clickobs password=test db=clickobs" python tests/test_pipeline.py
"""
import os
import sys
from decimal import Decimal
from pathlib import Path

import pymysql

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "include"))
from clickobs_ingesta import adapters, loader, normalize  # noqa: E402

cfg = dict(p.split("=", 1) for p in os.environ.get("CLICKOBS_TEST_DB", "host=localhost user=clickobs password=test db=clickobs").split())
conexion = lambda: pymysql.connect(host=cfg["host"], user=cfg["user"], password=cfg["password"], database=cfg["db"])
ok = fail = 0


def check(nombre, cond):
    global ok, fail
    ok, fail = (ok + 1, fail) if cond else (ok, fail + 1)
    print(("OK   " if cond else "FALLA ") + nombre)


def consulta(sql, args=()):
    c = conexion(); cur = c.cursor(); cur.execute(sql, args); r = cur.fetchall(); c.close(); return r


# 1. normalizacion de las 4 fuentes (formatos distintos -> modelo canonico)
esperado = {"dynatrace": 2, "datadog": 1, "prometheus": 1, "elastic": 1}
for fuente, n in esperado.items():
    r = normalize.normalizar(fuente, "2026-08", adapters.FUENTES[fuente].extraer("2026-08"))
    check(f"normalizar {fuente}: {n} fila(s), 0 rechazadas", len(r["filas"]) == n and not r["rechazados"])

# 2. registros invalidos: se rechazan con motivo (no se pierden en silencio)
malo = {"records": [{"managementZone": "Zona X", "ddu": 1, "cost_usd": 5},
                    {"managementZone": "API de pagos", "ddu": -3, "cost_usd": 10},
                    {"managementZone": "API de pagos", "cost_usd": 10},
                    {"managementZone": "API de pagos", "ddu": 300, "cost_usd": 780}]}
r = normalize.normalizar("dynatrace", "2026-08", malo)
motivos = " | ".join(x["motivo"] for x in r["rechazados"])
check("rechaza servicio sin mapeo, cantidad negativa y campo faltante (3 rechazos)", len(r["rechazados"]) == 3 and "sin mapeo" in motivos and "negativo" in motivos)
check("conserva el registro valido", len(r["filas"]) == 1 and r["filas"][0]["monto_usd"] == "780.00")

# 3. deduplicacion: mismo servicio repetido se suma
dup = {"records": [{"managementZone": "API de pagos", "ddu": 100, "cost_usd": 200}, {"managementZone": "API de pagos", "ddu": 200, "cost_usd": 580}]}
r = normalize.normalizar("dynatrace", "2026-08", dup)
check("deduplica sumando (300 DDU / 780 USD)", len(r["filas"]) == 1 and r["filas"][0]["cantidad_usada"] == "300.00" and r["filas"][0]["monto_usd"] == "780.00")

# 4. periodo invalido
try:
    normalize.normalizar("dynatrace", "agosto", {}); check("periodo invalido debe fallar", False)
except ValueError:
    check("periodo invalido -> error", True)

# 5. carga idempotente: 2 corridas = mismo resultado
def total(): return consulta("SELECT COUNT(*), COALESCE(SUM(monto_usd),0) FROM consumo WHERE periodo='2026-08'")[0]
for _ in range(2):
    for fuente in esperado:
        r = normalize.normalizar(fuente, "2026-08", adapters.FUENTES[fuente].extraer("2026-08"))
        loader.reemplazar_consumo(conexion, fuente, "2026-08", r["filas"])
cnt, suma = total()
check("tras 2 corridas: 5 filas y total 7570 (idempotente, coincide con los datos de prueba)", cnt == 5 and suma == Decimal("7570.00"))

# 6. rollback: si una fila falla, el periodo queda como estaba
antes = total()
try:
    loader.reemplazar_consumo(conexion, "dynatrace", "2026-08",
                              [{"servicio": "API de pagos", "cantidad_usada": "1", "unidad_medida": "DDU", "monto_usd": "1"},
                               {"servicio": "Servicio inexistente", "cantidad_usada": "1", "unidad_medida": "DDU", "monto_usd": "1"}])
    check("servicio inexistente debe fallar", False)
except ValueError:
    check("carga con error -> ROLLBACK: el periodo no cambia", total() == antes)

# 7. estado del conector
loader.marcar_estado_conector(conexion, "elastic", "Error")
check("conector Elastic marcado 'Error'", consulta("SELECT c.estado FROM conector c JOIN herramienta h ON c.id_herramienta=h.id_herramienta WHERE h.nombre='Elastic'")[0][0] == "Error")
loader.marcar_estado_conector(conexion, "elastic", "Activo")
check("conector Elastic vuelve a 'Activo'", consulta("SELECT c.estado FROM conector c JOIN herramienta h ON c.id_herramienta=h.id_herramienta WHERE h.nombre='Elastic'")[0][0] == "Activo")

print(f"\nRESULTADO: {ok} OK, {fail} fallas")
sys.exit(1 if fail else 0)
