#!/usr/bin/env bash
# Smoke da API pelo nginx, com os servicos accounts, transfers, notification, statement e customer de pe, e
# tokens do mock-oauth2-server (auth/token.sh): cada cliente se cadastra no customer e opera com o proprio token;
# creditos e debitos de preparo vao com o token de admin do usuario de teste do dono.
# 0. identidade: sem token -> 401; conta de outro cliente -> 404; GET /customers/me do cliente.
# 1. accounts: abre conta, credita 1000, debita 300 e confere saldo 700; le a fila de inspecao do
#    LocalStack e confere os eventos da conta, em ordem: AccountOpened, LedgerEntryRecorded (sequence 1)
#    e LedgerEntryRecorded (sequence 2).
# 2. transfers: abre duas contas, credita 1000 na origem, transfere 400, espera COMPLETED, confere
#    saldos 600 (origem) e 400 (destino) e o evento TransferCompleted na fila transfers-events-inspection.fifo.
#    Depois confere o limite diario da origem em GET /accounts/{id}/daily-limit (500000:400:499600).
# 3. notification: espera ate 45 s (45 tentativas de 1 s) e confere TRANSFER_SENT para o remetente e TRANSFER_RECEIVED para o
#    destinatario em GET /notifications, cada um com o proprio token.
# 4. statement: espera ate 30 s e confere, em GET /accounts/{id}/statement, os lancamentos da conta do passo 1:
#    sequence 2 (debito de 300, saldo 700) e sequence 1 (credito de 1000, saldo 1000), do mais novo para o mais antigo.
# Uso: infra/local/smoke.sh [url-base]   (padrao http://127.0.0.1; exige o compose de pe)
set -euo pipefail

compose_file="$(cd "$(dirname "$0")" && pwd)/docker-compose.yml"
queue_name=accounts-events-inspection.fifo
transfers_queue_name=transfers-events-inspection.fifo

base="${1:-http://127.0.0.1}"
token_sh="$(cd "$(dirname "$0")" && pwd)/auth/token.sh"
# Usuario de teste do dono (grupo admin); so o papel de admin e usado aqui, sem depender do seed.
admin_sub=00000000-0000-7000-8000-000000000001
corr="smoke-$(date +%s)"
out=$(mktemp)
trap 'rm -f "$out"' EXIT

# UUID aleatorio em minusculas: /proc no Linux, uuidgen no macOS (que imprime em maiusculas).
uuid() { { cat /proc/sys/kernel/random/uuid 2>/dev/null || uuidgen; } | tr 'A-Z' 'a-z'; }

# Faz a requisicao com o token, confere o status e imprime o corpo. O status esperado pode ser uma lista
# separada por barra vertical (ex.: 201|202). Token "-" manda a requisicao sem Authorization. Todo POST leva
# uma Idempotency-Key nova.
call() {
  local expected="$1" token="$2" method="$3" path="$4" body="${5:-}"
  local args=(-sS -o "$out" -w '%{http_code}' -X "$method" -H "X-Correlation-Id: $corr")
  if [ "$token" != "-" ]; then
    args+=(-H "Authorization: Bearer $token")
  fi
  if [ "$method" = "POST" ]; then
    args+=(-H "Idempotency-Key: $(uuid)")
  fi
  if [ -n "$body" ]; then
    args+=(-H 'Content-Type: application/json' -d "$body")
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

admin=$("$token_sh" user "$admin_sub" admin)

# Cliente novo: sub aleatorio, cadastro no customer com o proprio token. Imprime "<sub> <token>"; use com
# pair=$(new_customer), para o set -e valer (numa substituicao de processo a falha passaria em silencio).
new_customer() {
  local sub token
  sub=$(uuid)
  token=$("$token_sh" user "$sub") || exit 1
  call 201 "$token" POST /customers "{\"name\":\"Smoke Customer\",\"email\":\"smoke-$sub@example.test\"}" > /dev/null
  echo "$sub $token"
}

# --- identidade ---
call 401 - GET /customers/me > /dev/null
pair=$(new_customer)
read -r customer customer_token <<< "$pair"
me_status=$(call 200 "$customer_token" GET /customers/me | jq -r .status)
if [ "$me_status" != "ACTIVE" ]; then
  echo "FALHA: cadastro do cliente $customer em $me_status (esperado ACTIVE)" >&2
  exit 1
fi
echo "OK: cliente $customer cadastrado e ACTIVE"

account=$(call 201 "$customer_token" POST /accounts)
id=$(echo "$account" | jq -r .id)

pair=$(new_customer)
read -r _ intruder_token <<< "$pair"
call 401 - GET "/accounts/$id" > /dev/null
call 404 "$intruder_token" GET "/accounts/$id" > /dev/null
echo "OK: conta $id sem token 401 e de outro cliente 404"

call 201 "$admin" POST "/accounts/$id/credits" '{"amount":1000,"description":"smoke credito"}' > /dev/null
call 201 "$admin" POST "/accounts/$id/debits" '{"amount":300,"description":"smoke debito"}' > /dev/null

balance=$(call 200 "$customer_token" GET "/accounts/$id" | jq -r .balance)
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
pair=$(new_customer)
read -r source_customer source_token <<< "$pair"
pair=$(new_customer)
read -r destination_customer destination_token <<< "$pair"
source_id=$(call 201 "$source_token" POST /accounts | jq -r .id)
destination_id=$(call 201 "$destination_token" POST /accounts | jq -r .id)
call 201 "$admin" POST "/accounts/$source_id/credits" '{"amount":1000,"description":"smoke credito"}' > /dev/null

# 201 concluida na hora, 202 ainda em andamento (a saga termina em segundo plano).
transfer=$(call "201|202" "$source_token" POST /transfers \
  "{\"sourceAccountId\":\"$source_id\",\"destinationAccountId\":\"$destination_id\",\"amount\":400,\"description\":\"smoke transferencia\"}")
transfer_id=$(echo "$transfer" | jq -r .id)

transfer_status=""
for _ in $(seq 1 30); do
  transfer_status=$(call 200 "$source_token" GET "/transfers/$transfer_id" | jq -r .status)
  [ "$transfer_status" != "COMPLETED" ] || break
  sleep 1
done
if [ "$transfer_status" != "COMPLETED" ]; then
  echo "FALHA: transferencia $transfer_id em $transfer_status (esperado COMPLETED)" >&2
  exit 1
fi

source_balance=$(call 200 "$source_token" GET "/accounts/$source_id" | jq -r .balance)
destination_balance=$(call 200 "$destination_token" GET "/accounts/$destination_id" | jq -r .balance)
if [ "$source_balance" != "600" ] || [ "$destination_balance" != "400" ]; then
  echo "FALHA: saldos origem $source_balance e destino $destination_balance (esperado 600 e 400)" >&2
  exit 1
fi
echo "OK: transferencia $transfer_id concluida (origem $source_balance, destino $destination_balance)"

check_events "$transfers_queue_name" .payload.transferId "$transfer_id" "TransferCompleted" 1

# O limite diario da origem (padrao 500000) mostra so a transferencia de 400. A rota e do transfers.
daily_limit=$(call 200 "$source_token" GET "/accounts/$source_id/daily-limit" | jq -r '"\(.limitCents):\(.usedCents):\(.availableCents)"')
if [ "$daily_limit" != "500000:400:499600" ]; then
  echo "FALHA: limite diario da conta $source_id: '$daily_limit' (esperado '500000:400:499600')" >&2
  exit 1
fi
echo "OK: limite diario da conta $source_id (limite:usado:disponivel): $daily_limit"

# --- notification ---
# O notification consome as filas notification-accounts.fifo e notification-transfers.fifo; espera ate 45 s (se o evento chegar antes da conta conhecida, a mensagem so volta apos 30 s de visibilidade).
# Uso: check_notification <customerId> <token-do-cliente> <kind-esperado>
check_notification() {
  local customer="$1" token="$2" expected="$3" kinds=""
  for _ in $(seq 1 45); do
    kinds=$(call 200 "$token" GET /notifications | jq -r '[.items[].kind] | join(" ")')
    case " $kinds " in
      *" $expected "*) echo "OK: cliente $customer recebeu $expected"; return 0 ;;
    esac
    sleep 1
  done
  echo "FALHA: notificacoes do cliente $customer: '$kinds' (esperado $expected)" >&2
  exit 1
}

check_notification "$source_customer" "$source_token" TRANSFER_SENT
check_notification "$destination_customer" "$destination_token" TRANSFER_RECEIVED

# --- statement ---
# O statement consome a fila statement-accounts.fifo; espera ate 30 s pelos dois lancamentos da conta do passo 1.
statement=""
for _ in $(seq 1 30); do
  statement=$(call "200|404" "$customer_token" GET "/accounts/$id/statement?limit=10" | jq -r '[.entries[]? | "\(.sequence):\(.kind):\(.amount):\(.balanceAfter)"] | join(" ")')
  [ "$statement" != "2:DEBIT:300:700 1:CREDIT:1000:1000" ] || break
  sleep 1
done
if [ "$statement" != "2:DEBIT:300:700 1:CREDIT:1000:1000" ]; then
  echo "FALHA: extrato da conta $id: '$statement' (esperado '2:DEBIT:300:700 1:CREDIT:1000:1000')" >&2
  exit 1
fi
echo "OK: extrato da conta $id: $statement"
