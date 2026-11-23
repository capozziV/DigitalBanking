#!/usr/bin/env bash
# Prova que o extrato sobrevive a derrubar e subir a stack: grava por um cliente novo (bonus, credito e debito),
# le o extrato pela API, roda `down` SEM -v, roda `up` e exige o mesmo corpo. Sem o volume do dynamodb, falha.
# Confere tambem o item da conta (dono): a leitura vai com o token do proprio cliente, que sem ele leva 404.
# Le com limit=10, que nunca entra no cache do Redis (so a 1a pagina com limit 20).
#
# ATENCAO: derruba e recria os conteineres do projeto informado (as filas do LocalStack se perdem). Rode so num
# projeto descartavel, como o do CI. O projeto capzzpay, o da stack de uso diario, e recusado.
# Uso: persistence-check.sh --project <projeto> [--env-file <arquivo>] [--file <compose>]... [url-base]
#      (imagens ja construidas e stack de pe com o perfil apps; IMAGE_TAG e MOCK_OAUTH2_HOST vem do ambiente)
set -euo pipefail

here="$(cd "$(dirname "$0")/.." && pwd)"
project=""
env_file="$here/.env.example"
files=()
base=http://127.0.0.1
while [ $# -gt 0 ]; do
  case "$1" in
    --project) project="${2:?--project exige um valor}"; shift 2 ;;
    --env-file) env_file="${2:?--env-file exige um valor}"; shift 2 ;;
    --file) files+=(-f "${2:?--file exige um valor}"); shift 2 ;;
    -*) echo "opcao desconhecida: $1" >&2; exit 2 ;;
    *) base="$1"; shift ;;
  esac
done
if [ -z "$project" ] || [ "$project" = capzzpay ]; then
  echo "informe --project com um projeto descartavel (capzzpay nao): este script derruba a stack" >&2
  exit 2
fi
[ "${#files[@]}" -gt 0 ] || files=(-f "$here/docker-compose.yml")
compose=(docker compose -p "$project" --env-file "$env_file" "${files[@]}" --profile apps)

# Sem conteiner de pe neste projeto, a url-base seria de outra stack: nada e gravado.
if [ -z "$("${compose[@]}" ps -q statement)" ]; then
  echo "FALHA: o projeto $project nao tem statement de pe" >&2
  exit 1
fi
port=$("${compose[@]}" port nginx 80)
case "$base" in
  *":${port##*:}" | *":${port##*:}/") ;;
  http://127.0.0.1 | http://localhost)
    if [ "${port##*:}" != 80 ]; then
      echo "FALHA: $base nao e o nginx do projeto $project" >&2
      exit 1
    fi ;;
  *) echo "FALHA: $base nao e o nginx do projeto $project" >&2; exit 1 ;;
esac

uuid() { { cat /proc/sys/kernel/random/uuid 2>/dev/null || uuidgen; } | tr '[:upper:]' '[:lower:]'; }
token() { "$here/auth/token.sh" user "$@"; }
post() {
  curl -fsS -X POST "$base$2" -H "Authorization: Bearer $1" -H "Idempotency-Key: $(uuid)" \
    -H 'Content-Type: application/json' ${3:+-d "$3"}
}
# O corpo do extrato com as chaves ordenadas; vazio enquanto nao responder 200.
read_statement() {
  local body
  body=$(curl -fsS "$base/accounts/$account/statement?limit=10" -H "Authorization: Bearer $1" 2> /dev/null) || return 0
  jq -S . <<< "$body"
}

customer=$(uuid)
customer_token=$(token "$customer")
admin_token=$(token 00000000-0000-7000-8000-000000000001 admin)
digits=$(printf '%s' "$customer" | tr -dc '0-9')00000000
post "$customer_token" /customers \
  "{\"name\":\"Persistence Check\",\"email\":\"persistence-$customer@example.test\",\"phone\":\"+55119${digits:0:8}\"}" > /dev/null
account=$(post "$customer_token" /accounts | jq -er .id)
post "$admin_token" "/accounts/$account/credits" '{"amount":1000,"description":"persistence credit"}' > /dev/null
post "$admin_token" "/accounts/$account/debits" '{"amount":300,"description":"persistence debit"}' > /dev/null

before=""
for _ in $(seq 1 60); do
  before=$(read_statement "$customer_token")
  if [ -n "$before" ] && [ "$(jq '.entries | length' <<< "$before")" = 3 ]; then
    break
  fi
  before=""
  sleep 1
done
if [ -z "$before" ]; then
  echo "FALHA: o extrato da conta $account nao chegou aos 3 lancamentos" >&2
  exit 1
fi
echo "OK: conta $account com 3 lancamentos; derrubando $project sem apagar volumes"

# O id do conteiner dynamodb prova que a stack foi mesmo recriada (sem down/up o id se repete).
dynamodb_before=$("${compose[@]}" ps -q dynamodb)
"${compose[@]}" down
"${compose[@]}" up --wait --no-build --wait-timeout 600
dynamodb_after=$("${compose[@]}" ps -q dynamodb)
if [ -z "$dynamodb_before" ] || [ -z "$dynamodb_after" ] || [ "$dynamodb_before" = "$dynamodb_after" ]; then
  echo "FALHA: o conteiner dynamodb nao foi recriado (antes=$dynamodb_before depois=$dynamodb_after)" >&2
  exit 1
fi

# O mock troca as chaves ao subir: token novo.
customer_token=$(token "$customer")
after=""
for _ in $(seq 1 30); do
  after=$(read_statement "$customer_token")
  if [ "$after" = "$before" ]; then
    echo "OK: extrato da conta $account identico depois de derrubar e subir"
    exit 0
  fi
  sleep 1
done
echo "FALHA: o extrato da conta $account mudou depois de derrubar e subir" >&2
echo "antes:  $(jq -c '[.entries[].sequence]' <<< "$before")" >&2
echo "depois: $(jq -c '[.entries[].sequence]' <<< "$after") (vazio: sem resposta 200)" >&2
exit 1
