#!/usr/bin/env bash
# Smoke da API pelo nginx, com os servicos accounts e transfers de pe:
# 1. accounts: abre conta, credita 1000, debita 300 e confere saldo 700; le a fila de inspecao do
#    LocalStack e confere os eventos da conta, em ordem: AccountOpened, LedgerEntryRecorded (sequence 1)
#    e LedgerEntryRecorded (sequence 2).
# 2. transfers: abre duas contas, credita 1000 na origem, transfere 400, espera COMPLETED, confere
#    saldos 600 (origem) e 400 (destino) e o evento TransferCompleted na fila transfers-events-inspection.fifo.
# Uso: infra/local/smoke.sh [url-base]   (padrao http://127.0.0.1; exige o compose de pe)
set -euo pipefail

compose_file="$(cd "$(dirname "$0")" && pwd)/docker-compose.yml"
queue_name=accounts-events-inspection.fifo
transfers_queue_name=transfers-events-inspection.fifo

base="${1:-http://127.0.0.1}"
corr="smoke-$(date +%s)"
out=$(mktemp)
trap 'rm -f "$out"' EXIT

# UUID aleatorio: /proc no Linux, uuidgen no macOS.
uuid() { cat /proc/sys/kernel/random/uuid 2>/dev/null || uuidgen; }

# Faz a requisicao, confere o status e imprime o corpo. O status esperado pode ser uma lista
# separada por barra vertical (ex.: 201|202).
call() {
  local expected="$1" method="$2" path="$3" body="${4:-}"
  local args=(-sS -o "$out" -w '%{http_code}' -X "$method" -H "X-Correlation-Id: $corr")
  if [ -n "$body" ]; then
    args+=(-H 'Content-Type: application/json' -H "Idempotency-Key: $(uuid)" -d "$body")
  fi
  local status
  : > "$out"
  # Falha de transporte (sem conexao) vira status 000, em vez de derrubar o script pelo set -e.
  status=$(curl "${args[@]}" "$base$path") || status="000"
  case "|$expected|" in
    *"|$status|"*) ;;
    *)
      echo "FALHA: $method $path -> $status (esperado $expected)" >&2
      cat "$out" >&2
      echo >&2
      exit 1
      ;;
  esac
  echo "$method $path -> $status" >&2
  cat "$out"
}

customer=$(uuid)
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

# Le a fila por ate 60 s e confere, em ordem, os eventos de um agregado; apaga so as mensagens dele
# (as de outros voltam para a fila). Uso: check_events <fila> <caminho-jq> <id> "<esperado>" <total>
# O caminho jq aponta o id do agregado no envelope: .aggregateId (accounts) ou .payload.transferId (transfers).
check_events() {
  local queue="$1" key="$2" aggregate="$3" expected="$4" total="$5"
  local queue_url events="" batch handle event
  queue_url=$(aws_local sqs get-queue-url --queue-name "$queue" --query QueueUrl --output text)
  for _ in $(seq 1 30); do
    batch=$(aws_local sqs receive-message --queue-url "$queue_url" \
      --max-number-of-messages 10 --wait-time-seconds 2 --visibility-timeout 120 \
      --message-attribute-names All)
    [ -n "$batch" ] || continue
    while IFS=$'\t' read -r handle event; do
      events="${events:+$events }$event"
      aws_local sqs delete-message --queue-url "$queue_url" --receipt-handle "$handle" < /dev/null
    done < <(echo "$batch" | jq -r --arg id "$aggregate" "
      .Messages[]?
      | (.Body | fromjson) as \$e
      | select((\$e | $key) == \$id)
      | [.ReceiptHandle, ((.MessageAttributes.eventType.StringValue // \"sem-atributo-eventType\") + (if \$e.payload.sequence then \":\" + (\$e.payload.sequence | tostring) else \"\" end))]
      | @tsv")
    # Para no ultimo evento esperado: uma duplicata que chegasse depois nao seria vista. Aceitavel porque a
    # deduplicacao do FIFO (MessageDeduplicationId = eventId) e a exclusividade do relay ja sao testadas
    # nos testes de integracao do relay; o smoke so confere a entrega ponta a ponta e a ordem.
    [ "$(echo "$events" | wc -w)" -lt "$total" ] || break
  done
  if [ "$events" != "$expected" ]; then
    echo "FALHA: eventos de $aggregate na fila $queue: '$events' (esperado '$expected')" >&2
    exit 1
  fi
  echo "OK: eventos de $aggregate na fila $queue: $events"
}

check_events "$queue_name" .aggregateId "$id" \
  "AccountOpened LedgerEntryRecorded:1 LedgerEntryRecorded:2" 3

# --- transfers ---
source_id=$(call 201 POST /accounts "{\"customerId\":\"$(uuid)\"}" | jq -r .id)
destination_id=$(call 201 POST /accounts "{\"customerId\":\"$(uuid)\"}" | jq -r .id)
call 201 POST "/accounts/$source_id/credits" '{"amount":1000,"description":"smoke credito"}' > /dev/null

# 201 concluida na hora, 202 ainda em andamento (a saga termina em segundo plano).
transfer=$(call "201|202" POST /transfers \
  "{\"sourceAccountId\":\"$source_id\",\"destinationAccountId\":\"$destination_id\",\"amount\":400,\"description\":\"smoke transferencia\"}")
transfer_id=$(echo "$transfer" | jq -r .id)

transfer_status=""
for _ in $(seq 1 30); do
  transfer_status=$(call 200 GET "/transfers/$transfer_id" 2> /dev/null | jq -r .status)
  [ "$transfer_status" != "COMPLETED" ] || break
  sleep 1
done
if [ "$transfer_status" != "COMPLETED" ]; then
  echo "FALHA: transferencia $transfer_id em $transfer_status (esperado COMPLETED)" >&2
  exit 1
fi

source_balance=$(call 200 GET "/accounts/$source_id" | jq -r .balance)
destination_balance=$(call 200 GET "/accounts/$destination_id" | jq -r .balance)
if [ "$source_balance" != "600" ] || [ "$destination_balance" != "400" ]; then
  echo "FALHA: saldos origem $source_balance e destino $destination_balance (esperado 600 e 400)" >&2
  exit 1
fi
echo "OK: transferencia $transfer_id concluida (origem $source_balance, destino $destination_balance)"

check_events "$transfers_queue_name" .payload.transferId "$transfer_id" "TransferCompleted" 1
