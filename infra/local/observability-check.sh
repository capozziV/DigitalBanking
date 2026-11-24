#!/usr/bin/env bash
# Confere a observabilidade de uma transferencia: o Tempo tem um trace com spans de bff, transfers, accounts,
# notification e statement, e o Prometheus tem transfers_finished_total.
# Uso: infra/local/observability-check.sh <transfer-id>
# Variaveis: TEMPO_URL (padrao http://127.0.0.1:3200), PROMETHEUS_URL (padrao http://127.0.0.1:9090),
# CHECK_TIMEOUT (segundos, padrao 60; os lotes de exportacao demoram).
set -euo pipefail

TRANSFER_ID="${1:?uso: observability-check.sh <transfer-id>}"
TEMPO_URL="${TEMPO_URL:-http://127.0.0.1:3200}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://127.0.0.1:9090}"
CHECK_TIMEOUT="${CHECK_TIMEOUT:-60}"
SERVICES=(bff transfers accounts notification statement)

# Aceita so caracteres de ID (evita injetar TraceQL).
case "$TRANSFER_ID" in
  *[!A-Za-z0-9_-]*) echo "transfer-id invalido: $TRANSFER_ID" >&2; exit 2 ;;
esac

QUERY="%7Bspan.transfer.id%3D%22${TRANSFER_ID}%22%7D"
deadline=$((SECONDS + CHECK_TIMEOUT))
missing="(nenhum trace achado)"

trace_has_all_services() {
  local body="$1" service
  missing=""
  for service in "${SERVICES[@]}"; do
    if ! grep -Eq "\"key\":\"service\.name\",\"value\":\{\"stringValue\":\"${service}\"\}" <<<"$body"; then
      missing="$missing $service"
    fi
  done
  [ -z "$missing" ]
}

trace_ok=""
while [ "$SECONDS" -lt "$deadline" ]; do
  search="$(curl -fsS "${TEMPO_URL}/api/search?q=${QUERY}&limit=20" 2>/dev/null || true)"
  for trace_id in $(grep -Eo '"traceID":"[0-9a-f]+"' <<<"$search" | cut -d'"' -f4 | sort -u); do
    body="$(curl -fsS "${TEMPO_URL}/api/traces/${trace_id}" 2>/dev/null || true)"
    if trace_has_all_services "$body"; then
      trace_ok="$trace_id"
      break
    fi
  done
  [ -n "$trace_ok" ] && break
  sleep 3
done

if [ -z "$trace_ok" ]; then
  echo "FALHOU: nenhum trace da transferencia ${TRANSFER_ID} com os cinco servicos (faltam:${missing})" >&2
  exit 1
fi
echo "OK: trace ${trace_ok} com spans de ${SERVICES[*]}"

metric_ok=""
deadline=$((SECONDS + CHECK_TIMEOUT))
while [ "$SECONDS" -lt "$deadline" ]; do
  result="$(curl -fsS "${PROMETHEUS_URL}/api/v1/query?query=transfers_finished_total" 2>/dev/null || true)"
  if grep -q '"result":\[{' <<<"$result"; then
    metric_ok=1
    break
  fi
  sleep 3
done

if [ -z "$metric_ok" ]; then
  echo "FALHOU: o Prometheus nao tem transfers_finished_total" >&2
  exit 1
fi
echo "OK: transfers_finished_total no Prometheus"
