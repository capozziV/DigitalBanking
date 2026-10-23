#!/usr/bin/env bash
# Smoke da API pelo nginx: abre conta, credita 1000, debita 300 e confere saldo 700.
# Uso: infra/local/smoke.sh [url-base]   (padrao http://127.0.0.1)
set -euo pipefail

base="${1:-http://127.0.0.1}"
corr="smoke-$(date +%s)"
out=$(mktemp)
trap 'rm -f "$out"' EXIT

# Faz a requisicao, confere o status e imprime o corpo.
call() {
  local expected="$1" method="$2" path="$3" body="${4:-}"
  local args=(-sS -o "$out" -w '%{http_code}' -X "$method" -H "X-Correlation-Id: $corr")
  if [ -n "$body" ]; then
    args+=(-H 'Content-Type: application/json' -H "Idempotency-Key: $(cat /proc/sys/kernel/random/uuid 2>/dev/null || uuidgen)" -d "$body")
  fi
  local status
  status=$(curl "${args[@]}" "$base$path")
  if [ "$status" != "$expected" ]; then
    echo "FALHA: $method $path -> $status (esperado $expected)" >&2
    cat "$out" >&2
    echo >&2
    exit 1
  fi
  echo "$method $path -> $status" >&2
  cat "$out"
}

customer=$(cat /proc/sys/kernel/random/uuid 2>/dev/null || uuidgen)
account=$(call 201 POST /accounts "{\"customerId\":\"$customer\"}")
id=$(echo "$account" | jq -r .id)

call 201 POST "/accounts/$id/credits" '{"amount":1000,"description":"smoke credito"}' > /dev/null
call 201 POST "/accounts/$id/debits" '{"amount":300,"description":"smoke debito"}' > /dev/null

balance=$(call 200 GET "/accounts/$id" | jq -r .balance)
if [ "$balance" != "700" ]; then
  echo "FALHA: saldo $balance (esperado 700)" >&2
  exit 1
fi
echo "OK: conta $id com saldo $balance"
