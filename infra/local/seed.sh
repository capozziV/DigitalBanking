#!/usr/bin/env bash
# Seed local do usuario de teste do dono do projeto (cliente e admin ao mesmo tempo), pelo nginx e com tokens do
# mock-oauth2-server (auth/token.sh):
# 1. cadastra o cliente no customer (sub 00000000-0000-7000-8000-000000000001, Owner Test, owner@example.test) e o
#    deixa ACTIVE pelo papel de admin (desfaz um bloqueio de testes anteriores);
# 2. abre a conta dele, com o proprio token (o accounts aceita uma conta por cliente, regra da etapa 1);
# 3. credita o saldo inicial pelo admin: R$ 10.000,00.
# Idempotente: as chaves de idempotencia sao fixas, entao rodar de novo devolve a mesma conta e nao credita
# outra vez. Para comecar do zero, recrie o ambiente com `docker compose down -v`.
# Saida: as duas ultimas linhas sao OWNER_ACCOUNT=<id> e OWNER_BALANCE=<centavos>.
# Modo --remote (ambiente na AWS, workflow aws-up): o dono e um usuario do Cognito no grupo admin, criado ou
# atualizado por infra/aws/cognito-user.sh (AdminInitiateAuth); o id do cliente e o sub que o Cognito der.
# E-mail em OWNER_EMAIL (padrao owner@example.test) e senha em OWNER_PASSWORD (segredo do repositorio, obrigatoria).
# Uso: infra/local/seed.sh [--remote] [url-base]   (padrao http://127.0.0.1; exige o compose e os cinco servicos de pe)
#      no --remote: tambem COGNITO_USER_POOL_ID, COGNITO_CLIENT_ID, AWS_REGION e credenciais da AWS.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
remote=false
if [ "${1:-}" = "--remote" ]; then
  remote=true
  shift
fi
base="${1:-http://127.0.0.1}"
token_sh="$here/auth/token.sh"
owner=00000000-0000-7000-8000-000000000001
owner_email=owner@example.test
corr="seed-$(date +%s)"
out=$(mktemp)
trap 'rm -f "$out"' EXIT

# Faz a requisicao com o token, confere o status e imprime o corpo. O status esperado pode ser uma lista
# separada por barra vertical (ex.: 200|201). A Idempotency-Key, quando houver, vem no 5o argumento.
call() {
  local expected="$1" method="$2" path="$3" body="${4:-}" key="${5:-}"
  local args=(-sS -o "$out" -w '%{http_code}' -X "$method" -H "X-Correlation-Id: $corr" -H "Authorization: Bearer $token")
  [ -z "$key" ] || args+=(-H "Idempotency-Key: $key")
  [ -z "$body" ] || args+=(-H 'Content-Type: application/json' -d "$body")
  local status
  : > "$out"
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

# O dono e admin: o mesmo token opera as proprias contas como cliente e o resto como admin.
if [ "$remote" = true ]; then
  if [ -z "${OWNER_PASSWORD:-}" ]; then
    echo "FALHA: OWNER_PASSWORD ausente (senha do dono no Cognito, segredo do repositorio)" >&2
    exit 1
  fi
  owner_email="${OWNER_EMAIL:-owner@example.test}"
  pair=$(COGNITO_USER_PASSWORD="$OWNER_PASSWORD" "$here/../aws/cognito-user.sh" token "$owner_email" admin)
  read -r owner token <<< "$pair"
else
  token=$("$token_sh" user "$owner" admin)
fi

# Telefone fixo do dono (fictício, único); troque por OWNER_PHONE se ele já estiver em uso.
call "201|200" POST /customers "{\"name\":\"Owner Test\",\"email\":\"$owner_email\",\"phone\":\"${OWNER_PHONE:-+5511999990000}\"}" > /dev/null
call 200 PUT "/admin/customers/$owner/status" '{"status":"ACTIVE"}' > /dev/null
echo "OK: cliente $owner (Owner Test) cadastrado e ACTIVE"

account=$(call "201|200" POST /accounts "" seed-owner-account-1)
id=$(echo "$account" | jq -er .id)
call "201|200" POST "/accounts/$id/credits" \
  '{"amount":1000000,"description":"seed saldo inicial"}' seed-owner-initial-credit-1 > /dev/null

balance=$(call 200 GET "/accounts/$id" | jq -r .balance)
echo "OK: conta $id do dono com saldo $balance"
# Duas ultimas linhas legiveis por maquina (o CI compara duas execucoes seguidas para provar a idempotencia).
echo "OWNER_ACCOUNT=$id"
echo "OWNER_BALANCE=$balance"
