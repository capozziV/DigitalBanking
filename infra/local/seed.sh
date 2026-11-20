#!/usr/bin/env bash
# Seed local do usuario de teste do dono do projeto (cliente e admin ao mesmo tempo), pelo nginx e com tokens do
# mock-oauth2-server (auth/token.sh):
# 1. cadastra o cliente no customer (sub 00000000-0000-7000-8000-000000000001, Owner Test, owner@example.test) e o
#    deixa ACTIVE pelo papel de admin (desfaz um bloqueio de testes anteriores);
# 2. abre a conta dele, com o proprio token (o accounts aceita uma conta por cliente, regra da etapa 1);
# 3. credita o saldo inicial pelo admin: R$ 10.000,00.
# Idempotente: as chaves de idempotencia sao fixas, entao rodar de novo devolve a mesma conta e nao credita
# outra vez. Para comecar do zero, recrie o ambiente com `docker compose down -v`.
# 4. usuario de demonstracao (etapa 9): cliente "Demo capzzPay" (DEMO_EMAIL, DEMO_PASSWORD de 6 digitos, DEMO_PHONE) com
#    conta (nasce com o bonus de 10000), credito de 200000, uma transferencia enviada (12000) e uma recebida (30000) de
#    "Parceiro capzzPay" (cliente do seed, partner@example.test), as duas concluidas, e uma de 495000 recusada pelo
#    limite diario; o notification gera WELCOME, TRANSFER_SENT, TRANSFER_RECEIVED e TRANSFER_FAILED.
#    Local: o login do demo nasce no BFF (POST /bff/auth/sign-up, IDENTITY_PROVIDER=local; BFF_URL, padrao a url-base);
#    o `sub` do BFF local e um hash do e-mail (subFor em frontend/bff/src/identity/local-identity-provider.ts) e o seed o
#    recalcula aqui, para pedir ao mock um token do mesmo `sub` (auth/token.sh) em vez de depender de cookie de sessao.
#    AWS: o demo e o parceiro sao usuarios do Cognito criados por infra/aws/cognito-user.sh (AdminCreateUser +
#    AdminSetUserPassword permanente); a senha do demo vem de DEMO_PASSWORD (segredo do repositorio, obrigatoria).
# Saida: as linhas OWNER_ACCOUNT=<id> e OWNER_BALANCE=<centavos>, depois DEMO_ACCOUNT, DEMO_BALANCE, DEMO_COMPLETED,
# DEMO_REFUSED e DEMO_NOTIFICATIONS (o seed-check compara duas execucoes seguidas).
# Modo --remote (ambiente na AWS, workflow aws-up): o dono e um usuario do Cognito no grupo admin, criado ou
# atualizado por infra/aws/cognito-user.sh (AdminInitiateAuth); o id do cliente e o sub que o Cognito der.
# E-mail em OWNER_EMAIL (padrao owner@example.test) e senha em OWNER_PASSWORD (segredo do repositorio, obrigatoria).
# Demo: DEMO_EMAIL (padrao demo@example.test), DEMO_PASSWORD (6 digitos; padrao 123456 so no local) e DEMO_PHONE
# (E.164, padrao +5511999990001); ver infra/local/.env.example (o seed le o .env se existir).
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
# Variaveis do demo: o ambiente tem prioridade sobre o .env (so valores de desenvolvimento local).
if [ -f "$here/.env" ]; then
  for name in DEMO_EMAIL DEMO_PASSWORD DEMO_PHONE; do
    if [ -z "${!name:-}" ]; then
      value=$(sed -n "s/^$name=//p" "$here/.env" | tail -1)
      [ -z "$value" ] || export "$name=$value"
    fi
  done
fi
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
owner_token="$token"

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

# --- usuario de demonstracao ---
demo_email=$(printf '%s' "${DEMO_EMAIL:-demo@example.test}" | tr '[:upper:]' '[:lower:]')
demo_phone="${DEMO_PHONE:-+5511999990001}"
partner_email=partner@example.test
partner_phone=+5511999990002
if [ "$remote" = true ]; then
  if [ -z "${DEMO_PASSWORD:-}" ]; then
    echo "FALHA: DEMO_PASSWORD ausente (senha de 6 digitos do usuario de demonstracao, segredo do repositorio)" >&2
    exit 1
  fi
else
  DEMO_PASSWORD="${DEMO_PASSWORD:-123456}"
fi
if ! [[ "$DEMO_PASSWORD" =~ ^[0-9]{6}$ ]]; then
  echo "FALHA: DEMO_PASSWORD deve ter exatamente 6 digitos (ADR 0024)" >&2
  exit 1
fi

# O mesmo calculo do BFF local: sha256("local-user:<email>"), 16 bytes, versao 7 e variante RFC 4122.
local_sub_for() {
  local hex
  hex=$(printf 'local-user:%s' "$1" | openssl dgst -sha256 -binary | xxd -p -c 64 | cut -c1-32)
  printf '%s-%s-%02x%s-%02x%s-%s' "${hex:0:8}" "${hex:8:4}" \
    $(( (0x${hex:12:2} & 0x0f) | 0x70 )) "${hex:14:2}" $(( (0x${hex:16:2} & 0x3f) | 0x80 )) "${hex:18:2}" "${hex:20:12}"
}

# Cadastro e login do demo no BFF (local); o 409 do cadastro repetido e esperado, e o login prova a senha.
bff_post() {
  local path="$1" expected="$2" status
  : > "$out"
  status=$(curl -sS -o "$out" -w '%{http_code}' -X POST -H 'Content-Type: application/json' \
    -d "{\"email\":\"$demo_email\",\"password\":\"$DEMO_PASSWORD\"}" "${BFF_URL:-$base}$path") || status=000
  case "|$expected|" in
    *"|$status|"*) echo "POST $path -> $status" >&2 ;;
    *)
      echo "FALHA: POST $path -> $status (esperado $expected)" >&2
      cat "$out" >&2
      echo >&2
      exit 1
      ;;
  esac
}

if [ "$remote" = true ]; then
  pair=$(COGNITO_USER_PASSWORD="$DEMO_PASSWORD" "$here/../aws/cognito-user.sh" token "$demo_email")
  read -r demo demo_token <<< "$pair"
  pair=$("$here/../aws/cognito-user.sh" token "$partner_email")
  read -r partner partner_token <<< "$pair"
else
  bff_post /bff/auth/sign-up '200|201|409'
  bff_post /bff/auth/sign-in 200
  demo=$(local_sub_for "$demo_email")
  demo_token=$("$token_sh" user "$demo")
  partner=00000000-0000-7000-8000-000000000002
  partner_token=$("$token_sh" user "$partner")
fi

token="$demo_token"
call "201|200" POST /customers "{\"name\":\"Demo capzzPay\",\"email\":\"$demo_email\",\"phone\":\"$demo_phone\"}" > /dev/null
token="$partner_token"
call "201|200" POST /customers "{\"name\":\"Parceiro capzzPay\",\"email\":\"$partner_email\",\"phone\":\"$partner_phone\"}" > /dev/null
echo "OK: clientes $demo (Demo capzzPay) e $partner (Parceiro capzzPay) cadastrados"

token="$demo_token"
demo_account=$(call "201|200" POST /accounts "" seed-demo-account-1 | jq -er .id)
token="$partner_token"
partner_account=$(call "201|200" POST /accounts "" seed-partner-account-1 | jq -er .id)
token="$owner_token"
call "201|200" POST "/accounts/$demo_account/credits" \
  '{"amount":200000,"description":"seed saldo da demonstracao"}' seed-demo-initial-credit-1 > /dev/null
call "201|200" POST "/accounts/$partner_account/credits" \
  '{"amount":100000,"description":"seed saldo do parceiro"}' seed-partner-initial-credit-1 > /dev/null

# Pede a transferencia (201 concluida, 202 em andamento, 422 recusada; a mesma chave devolve a existente) e imprime
# o status final depois de esperar a saga (ate 30 s). Uso: transfer_status <token> <origem> <destino> <centavos> <descricao> <chave>
transfer_status() {
  local from_token="$1" source="$2" destination="$3" amount="$4" description="$5" key="$6" id status="" saved="$token"
  token="$from_token"
  id=$(call "200|201|202|422" POST /transfers \
    "{\"sourceAccountId\":\"$source\",\"destinationAccountId\":\"$destination\",\"amount\":$amount,\"description\":\"$description\"}" \
    "$key" | jq -er '.transferId // .id')
  for _ in $(seq 1 30); do
    status=$(call 200 GET "/transfers/$id" | jq -r .status)
    case "$status" in COMPLETED | FAILED | COMPENSATED) break ;; esac
    sleep 1
  done
  token="$saved"
  echo "$status"
}

sent=$(transfer_status "$demo_token" "$demo_account" "$partner_account" 12000 "seed pizza com o parceiro" seed-demo-transfer-sent-1)
received=$(transfer_status "$partner_token" "$partner_account" "$demo_account" 30000 "seed acerto de contas" seed-demo-transfer-received-1)
# 12000 ja usados do limite diario de 500000: 495000 passa do que resta (488000) e a saga recusa antes do debito.
refused=$(transfer_status "$demo_token" "$demo_account" "$partner_account" 495000 "seed acima do limite diario" seed-demo-transfer-refused-1)
completed=0
[ "$sent" != COMPLETED ] || completed=$((completed + 1))
[ "$received" != COMPLETED ] || completed=$((completed + 1))
refusals=0
[ "$refused" != FAILED ] || refusals=1
if [ "$completed" != 2 ] || [ "$refusals" != 1 ]; then
  echo "FALHA: transferencias do demo: enviada $sent, recebida $received, acima do limite $refused (esperado COMPLETED, COMPLETED, FAILED)" >&2
  exit 1
fi

# O notification e assincrono: espera ate 45 s pelos quatro tipos de aviso do demo.
token="$demo_token"
wanted='TRANSFER_FAILED TRANSFER_RECEIVED TRANSFER_SENT WELCOME'
kinds=""
for _ in $(seq 1 45); do
  kinds=$(call 200 GET /notifications | jq -r '[.items[].kind] | unique | join(" ")')
  [ "$kinds" != "$wanted" ] || break
  sleep 1
done
demo_balance=$(call 200 GET "/accounts/$demo_account" | jq -r .balance)
echo "OK: demo $demo_account com saldo $demo_balance, $completed concluidas, $refusals recusada, avisos: $kinds"
echo "DEMO_ACCOUNT=$demo_account"
echo "DEMO_BALANCE=$demo_balance"
echo "DEMO_COMPLETED=$completed"
echo "DEMO_REFUSED=$refusals"
echo "DEMO_NOTIFICATIONS=$kinds"
