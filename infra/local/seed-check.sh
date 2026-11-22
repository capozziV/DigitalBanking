#!/usr/bin/env bash
# Prova a idempotencia do seed.sh: roda o seed duas vezes seguidas e compara as linhas OWNER_ACCOUNT e OWNER_BALANCE
# das duas execucoes. Um credito em dobro (saldo diferente) ou uma conta nova falham aqui.
# Com o 2o argumento, confere tambem o saldo da 1a execucao (num ambiente novo, o saldo inicial do seed: 1000000).
# Confere tambem o usuario de demonstracao (linhas DEMO_* do seed): conta e saldo estaveis entre as duas execucoes,
# saldo de 228000 (bonus 10000 + credito 200000 + 30000 recebidos - 12000 enviados), 2 transferencias concluidas,
# 1 recusada pelo limite diario, as notificacoes WELCOME, TRANSFER_SENT, TRANSFER_RECEIVED e TRANSFER_FAILED e o nome
# "Parceiro capzzPay" da contraparte nas tres de transferencia (nunca "outro cliente"; linha DEMO_COUNTERPART, que o seed
# so imprime depois de conferir os textos e de esperar a consistencia eventual; sem o nome, o seed sai com erro).
# Com --remote, repassa o modo ao seed.sh (ambiente na AWS, workflow aws-up).
# Uso: infra/local/seed-check.sh [--remote] [url-base] [saldo-esperado]   (padrao http://127.0.0.1; usado pelo CI)
set -euo pipefail

mode=()
if [ "${1:-}" = "--remote" ]; then
  mode=(--remote)
  shift
fi
base="${1:-http://127.0.0.1}"
expected_balance="${2:-}"
seed="$(cd "$(dirname "$0")" && pwd)/seed.sh"

# Linhas OWNER_* da saida do seed, na ordem em que saem.
owner_lines() {
  grep -E '^OWNER_(ACCOUNT|BALANCE)=' <<< "$1" || true
}

echo "--- primeira execucao do seed"
first=$(bash "$seed" ${mode[@]+"${mode[@]}"} "$base")
echo "$first"
echo "--- segunda execucao do seed"
second=$(bash "$seed" ${mode[@]+"${mode[@]}"} "$base")
echo "$second"

first_owner=$(owner_lines "$first")
second_owner=$(owner_lines "$second")
if [ "$(wc -l <<< "$first_owner" | tr -d ' ')" != 2 ] || ! grep -q '^OWNER_ACCOUNT=.' <<< "$first_owner"; then
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

# --- usuario de demonstracao ---
demo_lines() {
  grep -E '^DEMO_(ACCOUNT|BALANCE|COMPLETED|REFUSED|NOTIFICATIONS|COUNTERPART)=' <<< "$1" || true
}
first_demo=$(demo_lines "$first")
second_demo=$(demo_lines "$second")
if [ "$(wc -l <<< "$first_demo" | tr -d ' ')" != 6 ] || ! grep -q '^DEMO_ACCOUNT=.' <<< "$first_demo"; then
  echo "FALHA: o seed nao imprimiu as linhas DEMO_* do usuario de demonstracao" >&2
  exit 1
fi
if [ "$first_demo" != "$second_demo" ]; then
  echo "FALHA: o seed do usuario de demonstracao nao e idempotente" >&2
  echo "primeira: $(tr '\n' ' ' <<< "$first_demo")" >&2
  echo "segunda:  $(tr '\n' ' ' <<< "$second_demo")" >&2
  exit 1
fi
for expected in DEMO_BALANCE=228000 DEMO_COMPLETED=2 DEMO_REFUSED=1 \
  'DEMO_NOTIFICATIONS=TRANSFER_FAILED TRANSFER_RECEIVED TRANSFER_SENT WELCOME' 'DEMO_COUNTERPART=Parceiro capzzPay'; do
  if ! grep -qxF "$expected" <<< "$first_demo"; then
    echo "FALHA: demo sem $expected (veio: $(tr '\n' ' ' <<< "$first_demo"))" >&2
    exit 1
  fi
done
echo "OK: usuario de demonstracao conferido ($(tr '\n' ' ' <<< "$first_demo"))"
