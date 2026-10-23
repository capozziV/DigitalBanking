#!/usr/bin/env bash
# Smoke da API pelo nginx: abre conta, credita 1000, debita 300 e confere saldo 700.
# Depois le a fila de inspecao do LocalStack e confere os eventos da conta, em ordem:
# AccountOpened, LedgerEntryRecorded (sequence 1) e LedgerEntryRecorded (sequence 2).
# Uso: infra/local/smoke.sh [url-base]   (padrao http://127.0.0.1; exige o compose de pe)
set -euo pipefail

compose_file="$(cd "$(dirname "$0")" && pwd)/docker-compose.yml"
queue_name=accounts-events-inspection.fifo

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
  : > "$out"
  # Falha de transporte (sem conexao) vira status 000, em vez de derrubar o script pelo set -e.
  status=$(curl "${args[@]}" "$base$path") || status="000"
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

# CLI da AWS de dentro do conteiner do LocalStack, sem exigir a CLI na maquina.
aws_local() {
  docker compose -f "$compose_file" exec -T \
    -e AWS_ACCESS_KEY_ID=test -e AWS_SECRET_ACCESS_KEY=test -e AWS_DEFAULT_REGION=us-east-1 \
    localstack aws --endpoint-url http://127.0.0.1:4566 "$@"
}

queue_url=$(aws_local sqs get-queue-url --queue-name "$queue_name" --query QueueUrl --output text)
expected="AccountOpened LedgerEntryRecorded:1 LedgerEntryRecorded:2"
events=""
# Le ate 60 s; apaga so as mensagens desta conta (as de outras contas voltam para a fila).
for _ in $(seq 1 30); do
  batch=$(aws_local sqs receive-message --queue-url "$queue_url" \
    --max-number-of-messages 10 --wait-time-seconds 2 --visibility-timeout 120 \
    --message-attribute-names All)
  [ -n "$batch" ] || continue
  while IFS=$'\t' read -r handle event; do
    events="${events:+$events }$event"
    aws_local sqs delete-message --queue-url "$queue_url" --receipt-handle "$handle" < /dev/null
  done < <(echo "$batch" | jq -r --arg id "$id" '
    .Messages[]?
    | (.Body | fromjson) as $e
    | select($e.aggregateId == $id)
    | [.ReceiptHandle, ((.MessageAttributes.eventType.StringValue // "sem-atributo-eventType") + (if $e.payload.sequence then ":" + ($e.payload.sequence | tostring) else "" end))]
    | @tsv')
  [ "$(echo "$events" | wc -w)" -lt 3 ] || break
done

if [ "$events" != "$expected" ]; then
  echo "FALHA: eventos na fila $queue_name: '$events' (esperado '$expected')" >&2
  exit 1
fi
echo "OK: eventos da conta $id na fila $queue_name: $events"
