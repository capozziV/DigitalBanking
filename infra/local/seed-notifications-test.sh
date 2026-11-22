#!/usr/bin/env bash
# Teste, com entrada falsa, da conferencia do nome da contraparte nas notificacoes do demo (issue 232):
# 1. notification_names_ok (seed.sh) aceita so o JSON com as tres notificacoes de transferencia trazendo o nome e
#    nenhuma dizendo "outro cliente";
# 2. seed-check.sh falha alto quando a saida do seed nao traz DEMO_COUNTERPART (um seed falso, copiado ao lado dele).
# Nao toca na stack local. Uso: infra/local/seed-notifications-test.sh
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
partner_name="Parceiro capzzPay"
eval "$(sed -n '/^notification_names_ok() {/,/^}/p' "$here/seed.sh")"

fail=0
expect() { # expect <ok|fail> <descricao> <json>
  local got=fail
  if notification_names_ok <<< "$3"; then got=ok; fi
  if [ "$got" != "$1" ]; then
    echo "FALHA: $2 (esperado $1, veio $got)" >&2
    fail=1
  fi
}

item() { printf '{"kind":"%s","body":"%s"}' "$1" "$2"; }
sent=$(item TRANSFER_SENT "Você enviou R\$ 120,00 para Parceiro capzzPay.")
received=$(item TRANSFER_RECEIVED "Você recebeu R\$ 300,00 de Parceiro capzzPay.")
failed=$(item TRANSFER_FAILED "Sua transferência de R\$ 4.950,00 para Parceiro capzzPay foi recusada: limite diário.")
welcome=$(item WELCOME "Bem-vindo.")
unknown_sent=$(item TRANSFER_SENT "Você enviou R\$ 120,00 para outro cliente.")

expect ok "as tres com o nome" "{\"items\":[$welcome,$sent,$received,$failed]}"
expect fail "enviada com outro cliente" "{\"items\":[$welcome,$unknown_sent,$received,$failed]}"
expect fail "as tres com o nome e um aviso dizendo outro cliente" "{\"items\":[$welcome,$sent,$received,$failed,$(item WELCOME "Fale com outro cliente.")]}"
expect fail "falta a recusada" "{\"items\":[$welcome,$sent,$received]}"
expect fail "nenhuma notificacao" '{"items":[]}'

# seed-check com um seed falso ao lado: sem DEMO_COUNTERPART ele tem de falhar.
dir=$(mktemp -d)
trap 'rm -rf "$dir"' EXIT
cp "$here/seed-check.sh" "$dir/seed-check.sh"
fake_seed() { # fake_seed <linha DEMO_COUNTERPART ou vazio>
  cat > "$dir/seed.sh" <<SEED
#!/usr/bin/env bash
echo OWNER_ACCOUNT=a
echo OWNER_BALANCE=1
echo DEMO_ACCOUNT=b
echo DEMO_BALANCE=228000
echo DEMO_COMPLETED=2
echo DEMO_REFUSED=1
echo 'DEMO_NOTIFICATIONS=TRANSFER_FAILED TRANSFER_RECEIVED TRANSFER_SENT WELCOME'
$1
SEED
}
fake_seed 'echo "DEMO_COUNTERPART=Parceiro capzzPay"'
bash "$dir/seed-check.sh" > /dev/null 2>&1 || { echo "FALHA: seed-check recusou um seed correto" >&2; fail=1; }
fake_seed ''
if bash "$dir/seed-check.sh" > /dev/null 2>&1; then echo "FALHA: seed-check aceitou um seed sem DEMO_COUNTERPART" >&2; fail=1; fi
fake_seed 'echo "DEMO_COUNTERPART=outro cliente"'
if bash "$dir/seed-check.sh" > /dev/null 2>&1; then echo "FALHA: seed-check aceitou a contraparte 'outro cliente'" >&2; fail=1; fi

[ "$fail" = 0 ] && echo "OK: conferencia do nome da contraparte"
exit "$fail"
