#!/usr/bin/env bash
# Teste, com entrada falsa, da conferencia do nome da contraparte nas notificacoes do demo (issue 232):
# 1. notification_names_ok e notification_found_kinds (seed.sh) olham so as notificacoes das transferencias do proprio
#    seed (pelo data.transferId): aceitam o JSON com as tres trazendo o nome, ignoram transferencias de terceiros
#    (recebida de outra pessoa, envio a outra conta) e rejeitam "outro cliente", nome ausente e projecao vazia;
# 2. seed-check.sh falha alto quando a saida do seed nao traz DEMO_COUNTERPART (um seed falso, copiado ao lado dele).
# Nao toca na stack local. Uso: infra/local/seed-notifications-test.sh
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
# shellcheck disable=SC2034 # lida pelas funcoes do seed.sh, carregadas pelo eval
partner_name="Parceiro capzzPay"
eval "$(sed -n '/^notification_names_ok() {/,/^}/p;/^notification_found_kinds() {/,/^}/p;/^fetch_notifications() {/,/^}/p' "$here/seed.sh")"

fail=0
ids='["t-sent","t-received","t-failed"]'
expect() { # expect <ok|fail> <descricao> <json>
  local got=fail
  if notification_names_ok "$ids" <<< "$3"; then got=ok; fi
  if [ "$got" != "$1" ]; then
    echo "FALHA: $2 (esperado $1, veio $got)" >&2
    fail=1
  fi
}
expect_kinds() { # expect_kinds <tipos esperados> <descricao> <json>
  local got
  got=$(notification_found_kinds "$ids" <<< "$3")
  if [ "$got" != "$1" ]; then
    echo "FALHA: $2 (esperado '$1', veio '$got')" >&2
    fail=1
  fi
}

item() { printf '{"kind":"%s","body":"%s","data":{"transferId":"%s"}}' "$1" "$2" "$3"; }
sent=$(item TRANSFER_SENT "Você enviou R\$ 120,00 para Parceiro capzzPay." t-sent)
received=$(item TRANSFER_RECEIVED "Você recebeu R\$ 300,00 de Parceiro capzzPay." t-received)
failed=$(item TRANSFER_FAILED "Sua transferência de R\$ 4.950,00 para Parceiro capzzPay foi recusada: limite diário." t-failed)
welcome='{"kind":"WELCOME","body":"Bem-vindo.","data":{}}'
unknown_sent=$(item TRANSFER_SENT "Você enviou R\$ 120,00 para outro cliente." t-sent)
no_name_sent=$(item TRANSFER_SENT "Você enviou R\$ 120,00." t-sent)
third_received=$(item TRANSFER_RECEIVED "Você recebeu R\$ 10,00 de Maria Souza." t-other-1)
third_sent=$(item TRANSFER_SENT "Você enviou R\$ 5,00 para outro cliente." t-other-2)
base="$welcome,$sent,$received,$failed"

expect ok "as tres com o nome" "{\"items\":[$base]}"
expect ok "terceiro recebido, de outra pessoa" "{\"items\":[$third_received,$base]}"
expect ok "envio a outra conta (sem nome)" "{\"items\":[$third_sent,$base]}"
many=$(for i in $(seq 1 25); do printf '%s,' "$(item TRANSFER_RECEIVED "Você recebeu R\$ 1,00 de Maria Souza." "t-more-$i")"; done)
expect ok "mais de 20 avisos de terceiros, com o seed ao fim" "{\"items\":[${many}$base]}"
expect fail "enviada do seed com outro cliente" "{\"items\":[$welcome,$unknown_sent,$received,$failed]}"
expect fail "enviada do seed sem o nome" "{\"items\":[$welcome,$no_name_sent,$received,$failed]}"
expect fail "falta a recusada" "{\"items\":[$welcome,$sent,$received]}"
expect fail "so terceiros" "{\"items\":[$welcome,$third_received,$third_sent]}"
expect fail "nenhuma notificacao" '{"items":[]}'
expect_kinds "TRANSFER_FAILED TRANSFER_RECEIVED TRANSFER_SENT WELCOME" "tipos achados com terceiros" "{\"items\":[$third_received,$base]}"
expect_kinds "TRANSFER_SENT" "tipos sem WELCOME nem as outras" "{\"items\":[$third_received,$sent]}"

# fetch_notifications segue o nextCursor (aqui, 3 paginas; o cursor opaco com '+' e '=' sai codificado) e junta os itens.
# shellcheck disable=SC2329 # chamada por fetch_notifications, carregada pelo eval
call() { # call <status> GET <caminho>: o servidor falso
  echo "GET $3" >> "$dir_calls"
  case "$3" in
    /notifications) echo '{"items":[{"kind":"A"}],"nextCursor":"c+1="}' ;;
    '/notifications?cursor=c%2B1%3D') echo '{"items":[{"kind":"B"}],"nextCursor":"c2"}' ;;
    '/notifications?cursor=c2') echo '{"items":[{"kind":"C"}],"nextCursor":null}' ;;
    *) echo "caminho inesperado $3" >&2; return 1 ;;
  esac
}
dir_calls=$(mktemp)
pages=$(fetch_notifications) || { echo "FALHA: fetch_notifications com cursor" >&2; fail=1; }
if [ "$(jq -r '[.items[].kind] | join("")' <<< "$pages")" != ABC ] || [ "$(wc -l < "$dir_calls" | tr -d ' ')" != 3 ]; then
  echo "FALHA: fetch_notifications nao juntou as 3 paginas (veio $pages)" >&2
  fail=1
fi
rm -f "$dir_calls"

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

# Demo ja usado (um visitante transferiu para ele: saldo diferente de 228000): o saldo absoluto so vale em ambiente novo
# (com o 2o argumento); na reexecucao (aws-up repetido) so a igualdade entre as duas execucoes.
fake_seed 'echo "DEMO_COUNTERPART=Parceiro capzzPay"'
sed -i.bak 's/^echo DEMO_BALANCE=.*/echo DEMO_BALANCE=231000/' "$dir/seed.sh"
bash "$dir/seed-check.sh" > /dev/null 2>&1 || { echo "FALHA: seed-check recusou o demo usado numa reexecucao (sem 2o argumento)" >&2; fail=1; }
if bash "$dir/seed-check.sh" http://127.0.0.1 1 > /dev/null 2>&1; then echo "FALHA: seed-check aceitou o demo com saldo diferente em ambiente novo" >&2; fail=1; fi

[ "$fail" = 0 ] && echo "OK: conferencia do nome da contraparte"
exit "$fail"
