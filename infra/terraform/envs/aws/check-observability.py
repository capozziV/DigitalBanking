#!/usr/bin/env python3
"""Confere que a observabilidade da AWS fala os mesmos nomes em todos os lados.

- chart do coletor (deploy/helm/otel-collector/values.yaml) x Terraform: ServiceAccount do Pod Identity
  (modules/access, collector_service_account), namespace das metricas e prefixo dos grupos de logs
  (modules/observability), grupo EMF = <prefixo>/metrics, regiao = a do envs/aws;
- um grupo de logs por servico (modules/observability, services) para cada values/<servico>.yaml, menos o web (nginx);
- cada servico, e o bff, aponta o OTLP para o Service do coletor e se chama pelo proprio `name:`;
- toda metrica do painel (modules/observability) passa pelo filtro do coletor, tem uma declaracao do awsemf, existe no
  codigo e usa so dimensoes que o awsemf declara para ela.

Uso: check-observability.py [raiz do repositorio]
"""
import re
import sys
from pathlib import Path

ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[4]
TF = ROOT / "infra/terraform"
CHART = ROOT / "deploy/helm/otel-collector"
VALUES = ROOT / "deploy/helm/values"

errors = []


def default_of(path, name):
    text = path.read_text()
    m = re.search(r'variable\s+"%s"\s*\{(.*?)\n\}' % re.escape(name), text, re.S)
    if not m:
        errors.append(f"{path.relative_to(ROOT)}: variavel {name} ausente")
        return None
    d = re.search(r"default\s*=\s*(.+)", m.group(1))
    if not d:
        errors.append(f"{path.relative_to(ROOT)}: variavel {name} sem default")
        return None
    return d.group(1).strip()


def scalar(text, key):
    m = re.search(rf"^{key}:\s*(\S+)\s*$", text, re.M)
    return m.group(1).strip("\"'") if m else None


chart_values = (CHART / "values.yaml").read_text()
collector = scalar(chart_values, "name")
port = scalar(chart_values, "port")
region = scalar(chart_values, "region")
namespace = scalar(chart_values, "metricsNamespace")
prefix = scalar(chart_values, "logGroupPrefix")
emf_group = scalar(chart_values, "emfLogGroup")

sa = default_of(TF / "modules/access/variables.tf", "collector_service_account")
if sa and sa.strip('"') != collector:
    errors.append(f"ServiceAccount do coletor: chart {collector}, modules/access {sa}")
tf_ns = default_of(TF / "modules/observability/variables.tf", "metrics_namespace")
if tf_ns and tf_ns.strip('"') != namespace:
    errors.append(f"namespace das metricas: chart {namespace}, modules/observability {tf_ns}")
tf_prefix = default_of(TF / "modules/observability/variables.tf", "log_group_prefix")
if tf_prefix and tf_prefix.strip('"') != prefix:
    errors.append(f"prefixo dos grupos de logs: chart {prefix}, modules/observability {tf_prefix}")
if emf_group != f"{prefix}/metrics":
    errors.append(f"grupo EMF do chart ({emf_group}) deve ser {prefix}/metrics, o que o Terraform cria")
tf_region = default_of(TF / "envs/aws/variables.tf", "region")
if tf_region and tf_region.strip('"') != region:
    errors.append(f"regiao: chart {region}, envs/aws {tf_region}")

# Servicos com grupo de logs x values dos servicos (o web e nginx: sem OTLP).
services_default = default_of(TF / "modules/observability/variables.tf", "services")
tf_services = set(re.findall(r'"([^"]+)"', services_default or ""))
values_names = {}
for path in sorted(VALUES.glob("*.yaml")):
    name = scalar(path.read_text(), "name")
    if name:
        values_names[name] = path
instrumented = set(values_names) - {"web"}
for k in sorted(instrumented - tf_services):
    errors.append(f"{k}: tem values e falta em modules/observability (services)")
for k in sorted(tf_services - instrumented):
    errors.append(f"{k}: tem grupo de logs e nao tem values")

endpoint = f"http://{collector}:{port}"
for name in sorted(instrumented):
    text = values_names[name].read_text()
    ep = re.search(r"^  OTEL_EXPORTER_OTLP_ENDPOINT:\s*(\S+)", text, re.M)
    sn = re.search(r"^  OTEL_SERVICE_NAME:\s*(\S+)", text, re.M)
    if not ep or ep.group(1).strip("\"'") != endpoint:
        errors.append(f"{name}: OTEL_EXPORTER_OTLP_ENDPOINT deve ser {endpoint}")
    if not sn or sn.group(1).strip("\"'") != name:
        errors.append(f"{name}: OTEL_SERVICE_NAME deve ser {name}")

# Metricas do painel x filtro e declaracoes do coletor.
config = (CHART / "files/config.yaml").read_text()
filt = re.search(r'IsMatch\(name, "\^\((.+?)\)\$"\)', config)
if not filt:
    errors.append("filtro de metricas (IsMatch) nao encontrado em otel-collector/files/config.yaml")
    filt_re = None
else:
    filt_re = re.compile("^(" + filt.group(1).replace("\\\\", "\\") + ")$")
selectors = [re.compile(s.replace("\\\\", "\\")) for s in re.findall(r'metric_name_selectors:\s*\["([^"]+)"\]', config)]
dashboard = (TF / "modules/observability/main.tf").read_text()
metrics = set(re.findall(r'MetricName=\\"([^"\\]+)\\"', dashboard)) | set(re.findall(r'local\.ns,\s*"([a-z][a-z0-9_.]+)"', dashboard))
if not metrics:
    errors.append("nenhuma metrica encontrada no painel")
for m in sorted(metrics):
    if filt_re and not filt_re.match(m):
        errors.append(f"{m}: esta no painel e o filtro do coletor derruba")
    if not any(s.match(m) for s in selectors):
        errors.append(f"{m}: esta no painel e nenhum metric_name_selectors do awsemf a cobre")

# Nomes do painel x codigo: um nome com erro que ainda cabe no filtro (outbox\\..+) deixaria o widget vazio. Cada nome
# tem de existir como literal no codigo de producao; transfers.finished e transfers.saga.duration sao da Task 2 da
# etapa 10 e so entram no codigo depois dela (ate la, tolerados).
ETAPA10_NAMES = {"transfers.finished", "transfers.saga.duration"}
code_literals = set()
for kt in list((ROOT / "libs").rglob("*.kt")) + list((ROOT / "services").rglob("*.kt")):
    if {"test", "integrationTest", "build"} & set(kt.parts):
        continue
    code_literals.update(re.findall(r'"([a-z][a-z0-9_.]*)"', kt.read_text()))
for m in sorted(metrics):
    if m not in code_literals and m not in ETAPA10_NAMES:
        errors.append(f"{m}: esta no painel e nao existe como nome de metrica no codigo (libs/ e services/)")

# Dimensoes de cada widget x dimensoes declaradas no awsemf para a metrica (senao o widget fica vazio).
declared = []
for dims, sel in re.findall(r'dimensions:\s*(\[\[.*?\]\])\s*\n\s*metric_name_selectors:\s*\["([^"]+)"\]', config):
    sets = {frozenset(x.strip().strip('"') for x in inner.split(",") if x.strip()) for inner in re.findall(r"\[([^\[\]]*)\]", dims[1:-1])}
    declared.append((re.compile(sel.replace("\\\\", "\\")), sets))


def check_dims(name, dims):
    allowed = [sets for rx, sets in declared if rx.match(name)]
    if allowed and not any(frozenset(dims) in sets for sets in allowed):
        errors.append(f"{name}: o painel usa as dimensoes {sorted(dims)} e o awsemf declara {sorted(sorted(x) for sets in allowed for x in sets)}")


for dims, name in re.findall(r"SEARCH\('\{\$\{local\.ns\},?([^}]*)\}\s*MetricName=\\\"([^\"\\]+)\\\"", dashboard):
    check_dims(name, {d.strip().strip('\\"') for d in dims.split(",") if d.strip()})
for name, tail in re.findall(r'\[local\.ns,\s*"([a-z][a-z0-9_.]+)"((?:,\s*"[^"]*")*)', dashboard):
    keys = re.findall(r'"([^"]*)"', tail)[0::2]
    check_dims(name, set(keys))

# Transaction Search e da conta: o destino, a politica e o grupo aws/spans (com retencao) ficam no bootstrap.
boot = (TF / "bootstrap/main.tf").read_text()
for res in ("aws_xray_trace_segment_destination", "aws_cloudwatch_log_resource_policy"):
    if f'resource "{res}"' not in boot:
        errors.append(f"bootstrap/main.tf: falta {res} (Transaction Search e da conta)")
    if f'resource "{res}"' in dashboard:
        errors.append(f"modules/observability: {res} nao pode ficar no ambiente (destroy no-op); mora no bootstrap")
if not re.search(r'resource "aws_cloudwatch_log_group" "xray_spans" \{\s*name\s*=\s*"aws/spans"\s*retention_in_days\s*=\s*1\b', boot):
    errors.append("bootstrap/main.tf: grupo aws/spans ausente ou sem retention_in_days = 1")
if "aws_cloudwatch_log_group.xray_spans" not in boot.split('resource "aws_xray_trace_segment_destination"')[-1]:
    errors.append("bootstrap/main.tf: o destino do X-Ray tem de depender do grupo aws/spans (criado antes, com retencao)")

if errors:
    print("check-observability: divergencias na observabilidade da AWS:", file=sys.stderr)
    for e in errors:
        print(f"  - {e}", file=sys.stderr)
    sys.exit(1)
print(f"check-observability: ok ({len(instrumented)} servicos com OTLP, {len(metrics)} metricas do painel no filtro e no awsemf)")
