#!/usr/bin/env bash
# Prova a idempotencia do seed.sh: roda o seed duas vezes seguidas e compara as linhas OWNER_ACCOUNT e OWNER_BALANCE
# das duas execucoes. Um credito em dobro (saldo diferente) ou uma conta nova falham aqui.
# Com o 2o argumento, confere tambem o saldo da 1a execucao (num ambiente novo, o saldo inicial do seed: 1000000).
# Uso: infra/local/seed-check.sh [url-base] [saldo-esperado]   (padrao http://127.0.0.1; usado pelo CI)
set -euo pipefail

base="${1:-http://127.0.0.1}"
expected_balance="${2:-}"
seed="$(cd "$(dirname "$0")" && pwd)/seed.sh"

# Linhas OWNER_* da saida do seed, na ordem em que saem.
owner_lines() {
  grep -E '^OWNER_(ACCOUNT|BALANCE)=' <<< "$1" || true
}

echo "--- primeira execucao do seed"
first=$(bash "$seed" "$base")
echo "$first"
echo "--- segunda execucao do seed"
second=$(bash "$seed" "$base")
echo "$second"

first_owner=$(owner_lines "$first")
second_owner=$(owner_lines "$second")
if [ "$(wc -l <<< "$first_owner" | tr -d ' ')" != 2 ] || [ -z "$(grep '^OWNER_ACCOUNT=.' <<< "$first_owner")" ]; then
  echo "FALHA: o seed nao imprimiu OWNER_ACCOUNT e OWNER_BALANCE" >&2
  exit 1
fi
if [ "$first_owner" != "$second_owner" ]; then
  echo "FALHA: o seed nao e idempotente" >&2
  echo "primeira: $(tr '\n' ' ' <<< "$first_owner")" >&2
  echo "segunda:  $(tr '\n' ' ' <<< "$second_owner")" >&2
  exit 1
fi
if [ -n "$expected_balance" ] && ! grep -qx "OWNER_BALANCE=$expected_balance" <<< "$first_owner"; then
  echo "FALHA: saldo do dono $(grep '^OWNER_BALANCE=' <<< "$first_owner") (esperado OWNER_BALANCE=$expected_balance)" >&2
  exit 1
fi
echo "OK: seed idempotente ($(tr '\n' ' ' <<< "$first_owner"))"
