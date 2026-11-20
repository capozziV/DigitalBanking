#!/usr/bin/env bash
# Teste do vetor fixo de local_sub_for (seed.sh): e-mail -> UUID literal. Espelha o spec do BFF
# (frontend/bff/src/identity/local-identity-provider.spec.ts); os dois lados devem mudar juntos.
# Uso: infra/local/seed-sub-test.sh
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
# Carrega so a funcao (o seed.sh executa a semeadura inteira ao ser lido).
eval "$(sed -n '/^local_sub_for() {/,/^}/p' "$here/seed.sh")"

fail=0
check() {
  local got
  got=$(local_sub_for "$1")
  if [ "$got" != "$2" ]; then
    echo "FALHA: local_sub_for $1 = $got (esperado $2)" >&2
    fail=1
  fi
}
check demo@example.test da612c56-258a-7bb2-8660-0d2735a3e580
check partner@example.test 64d6b971-ac6e-76e9-a84e-2cbf71a7a591
[ "$fail" = 0 ] && echo "OK: vetores de local_sub_for"
exit "$fail"
