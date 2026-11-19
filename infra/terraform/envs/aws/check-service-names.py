#!/usr/bin/env python3
"""Confere que o Pod Identity do Terraform e os charts Helm usam os mesmos nomes.

O papel de cada servico vale para a ServiceAccount de mesmo nome (modules/access, chaves de local.services);
o chart cria a ServiceAccount com o `name:` de deploy/helm/values/<servico>.yaml. Um nome diferente deixa o pod
sem papel e sem acesso a AWS. Confere tambem o Job db-init: a lista de servicos com banco (database_services do
modules/data, services do chart db-init e os values com <SERVICO>_DB_URL) e a ServiceAccount do Job.

Uso: check-service-names.py [raiz do repositorio]
"""
import re
import sys
from pathlib import Path

ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[4]
ACCESS = ROOT / "infra/terraform/modules/access"
DATA = ROOT / "infra/terraform/modules/data"
VALUES = ROOT / "deploy/helm/values"
DB_INIT = ROOT / "deploy/helm/db-init/values.yaml"

# App do cliente (etapa 9): o web e o bff nao acessam a AWS por papel (o BFF so chama a API publica do Cognito e o
# Redis pela rede), entao nao tem papel no Pod Identity; ainda assim os values tem de existir.
APP_SERVICES = {"web", "bff"}

errors = []


def block(text, start):
    """Texto entre a chave que abre em `start` e a que fecha no mesmo nivel."""
    i = text.index("{", start)
    depth = 0
    for j in range(i, len(text)):
        depth += {"{": 1, "}": -1}.get(text[j], 0)
        if depth == 0:
            return text[i + 1:j]
    raise SystemExit(f"check-service-names: chave sem fechar em {start}")


def access_services():
    text = (ACCESS / "main.tf").read_text()
    m = re.search(r"^\s*services\s*=\s*\{", text, re.M)
    if not m:
        raise SystemExit("check-service-names: local.services nao encontrado em modules/access/main.tf")
    body = block(text, m.start())
    return set(re.findall(r"^    (\w[\w-]*)\s*=\s*\{", body, re.M))


def variable_default(path, name):
    text = path.read_text()
    m = re.search(rf'^variable "{name}"\s*\{{', text, re.M)
    if not m:
        raise SystemExit(f"check-service-names: variavel {name} nao encontrada em {path}")
    d = re.search(r"default\s*=\s*(\[[^\]]*\]|\"[^\"]*\")", block(text, m.start()))
    if not d:
        raise SystemExit(f"check-service-names: variavel {name} sem default em {path}")
    return re.findall(r'"([^"]*)"', d.group(1))


def chart_values():
    out = {}
    for path in sorted(VALUES.glob("*.yaml")):
        text = path.read_text()
        m = re.search(r"^name:\s*(\S+)\s*$", text, re.M)
        if not m:
            errors.append(f"{path.name}: sem name:")
            continue
        out[m.group(1).strip("\"'")] = text
    return out


def compare(label_a, a, label_b, b):
    for k in sorted(a - b):
        errors.append(f"{k}: esta em {label_a} e falta em {label_b}")
    for k in sorted(b - a):
        errors.append(f"{k}: esta em {label_b} e falta em {label_a}")


access = access_services()
values = chart_values()
for app in sorted(APP_SERVICES - set(values)):
    errors.append(f"{app}: app do cliente sem deploy/helm/values/{app}.yaml")
for app in sorted(APP_SERVICES & access):
    errors.append(f"{app}: app do cliente nao deve ter papel no Pod Identity (modules/access)")
compare("modules/access (local.services)", access, "deploy/helm/values (name:)", set(values) - APP_SERVICES)

db_init_text = DB_INIT.read_text()
services_block = re.search(r"^services:\n((?:  - .*\n)+)", db_init_text, re.M)
db_init_services = set(re.findall(r"^  - (\S+)", services_block.group(1), re.M)) if services_block else set()
database_services = set(variable_default(DATA / "variables.tf", "database_services"))
with_db_url = {name for name, text in values.items() if re.search(rf"^  {name.upper()}_DB_URL:", text, re.M)}
compare("modules/data (database_services)", database_services, "db-init (services)", db_init_services)
compare("modules/data (database_services)", database_services, "values com <SERVICO>_DB_URL", with_db_url)

sa = re.search(r"^serviceAccountName:\s*(\S+)", db_init_text, re.M)
tf_sa = variable_default(ACCESS / "variables.tf", "db_init_service_account")
if not sa or [sa.group(1)] != tf_sa:
    errors.append(f"ServiceAccount do db-init: chart {sa.group(1) if sa else None}, Terraform {tf_sa}")

if errors:
    print("check-service-names: nomes divergentes entre o Terraform e os charts:", file=sys.stderr)
    for e in errors:
        print(f"  - {e}", file=sys.stderr)
    sys.exit(1)
print(f"check-service-names: ok ({len(access)} servicos com papel, {len(database_services)} com banco, ServiceAccount do db-init {tf_sa[0]})")
