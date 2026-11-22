#!/usr/bin/env python3
"""Confere o throttle da borda: o app (web em $default e o BFF) tem limite proprio, maior que o da API.

O stage aplica `default_route_settings` (10 req/s, rajada 20) a todas as rotas. Uma carga do app faz cerca de
12 requisicoes (index, JS, CSS, chunks, /bff), entao poucos visitantes juntos estourariam esse limite e receberiam
429 no JS (tela branca). As rotas publicas ($default, /bff) precisam de `route_settings` proprios, com folga para
10 visitantes simultaneos, e a API segue com o padrao.

Uso: check-edge-throttling.py [raiz do repositorio]
"""
import re
import sys
from pathlib import Path

ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[4]
EDGE = ROOT / "infra/terraform/modules/edge"
ENV = ROOT / "infra/terraform/envs/aws"

# 10 visitantes x ~12 requisicoes de carga na mesma rajada, mais o polling das transferencias e o do sininho.
MIN_APP_RATE = 50
MIN_APP_BURST = 150

errors = []


def default_of(path, name):
    text = path.read_text()
    m = re.search(r'variable\s+"%s"\s*\{(.*?)\n\}' % re.escape(name), text, re.S)
    if not m:
        errors.append(f"{path.name}: variavel {name} ausente")
        return None
    d = re.search(r"default\s*=\s*(\d+)", m.group(1))
    if not d:
        errors.append(f"{path.name}: variavel {name} sem padrao numerico")
        return None
    return int(d.group(1))


main = (EDGE / "main.tf").read_text()
if "default_route_settings" not in main:
    errors.append("stage sem default_route_settings (limite da API)")
if 'dynamic "route_settings"' not in main:
    errors.append("stage sem route_settings: $default e /bff herdam o limite 10/20 da API")
else:
    keys = re.search(r'app_route_keys\s*=\s*(.+)', main)
    if not keys or "public_route_keys" not in keys.group(1) or '"$default"' not in keys.group(1):
        errors.append("app_route_keys deve cobrir as rotas publicas (/bff) e $default")
    if not re.search(r'dynamic "route_settings"\s*\{\s*for_each\s*=\s*toset\(local\.app_route_keys\)', main):
        errors.append("route_settings deve iterar sobre local.app_route_keys")

for var, floor in (("app_throttling_rate_limit", MIN_APP_RATE), ("app_throttling_burst_limit", MIN_APP_BURST)):
    for path in (EDGE / "variables.tf", ENV / "variables.tf"):
        value = default_of(path, var)
        if value is not None and value < floor:
            errors.append(f"{path.name}: {var} padrao {value} abaixo de {floor}")

for var, expected in (("throttling_rate_limit", 10), ("throttling_burst_limit", 20)):
    for path in (EDGE / "variables.tf", ENV / "variables.tf"):
        value = default_of(path, var)
        if value is not None and value != expected:
            errors.append(f"{path.name}: {var} (API) deve seguir {expected}, esta {value}")

if errors:
    print("Throttle da borda inconsistente:")
    for e in errors:
        print(f"  - {e}")
    sys.exit(1)
print("Throttle da borda ok: app com limite proprio, API com o padrao")
