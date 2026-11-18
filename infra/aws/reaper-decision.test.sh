#!/usr/bin/env bash
# Testes do reaper-decision.sh, em shell puro (sem AWS): cada caso passa a etiqueta created-at e o "agora"
# e confere a decisao impressa. Roda no CI (job aws-scripts) e localmente: infra/aws/reaper-decision.test.sh
set -euo pipefail

decide="$(cd "$(dirname "$0")" && pwd)/reaper-decision.sh"
failures=0
total=0

# Uso: expect <decisao-esperada> <descricao> <created-at> <agora>
expect() {
  local expected="$1" description="$2" created_at="$3" now="$4" actual status
  total=$((total + 1))
  status=0
  actual=$("$decide" "$created_at" "$now" 2>/dev/null) || status=$?
  if [ "$status" -eq 0 ] && [ "$actual" = "$expected" ]; then
    echo "ok    $description -> $actual"
  else
    echo "FALHA $description: '$actual' (exit $status), esperado '$expected'" >&2
    failures=$((failures + 1))
  fi
}

# Uso: expect_error <descricao> <argumentos...>: argumento invalido de quem chama sai com exit 2, sem decisao.
expect_error() {
  local description="$1" status=0 actual
  shift
  total=$((total + 1))
  actual=$("$decide" "$@" 2>/dev/null) || status=$?
  if [ "$status" -eq 2 ] && [ -z "$actual" ]; then
    echo "ok    $description -> exit 2"
  else
    echo "FALHA $description: '$actual' (exit $status), esperado exit 2 sem saida" >&2
    failures=$((failures + 1))
  fi
}

# Idade: o limite e 8 h ligado.
expect keep "7 h 59 min" 2026-11-17T08:00:00Z 2026-11-17T15:59:00Z
expect destroy "8 h 01 min" 2026-11-17T08:00:00Z 2026-11-17T16:01:00Z
expect destroy "8 h exatas" 2026-11-17T08:00:00Z 2026-11-17T16:00:00Z
expect keep "recem-criado" 2026-11-17T08:00:00Z 2026-11-17T08:00:00Z
expect destroy "dias depois" 2026-11-10T08:00:00Z 2026-11-17T08:00:00Z
expect keep "virada de dia, 7 h 59 min" 2026-11-30T20:00:00Z 2026-12-01T03:59:00Z
expect destroy "virada de ano, 8 h 01 min" 2026-12-31T20:00:00Z 2027-01-01T04:01:00Z
expect keep "29 de fevereiro, 7 h 59 min" 2028-02-29T20:00:00Z 2028-03-01T03:59:00Z
expect keep "fracao de segundo" 2026-11-17T08:00:00.123Z 2026-11-17T15:59:00Z

# Etiqueta ausente ou malformada: na duvida, derruba.
expect destroy "etiqueta ausente" "" 2026-11-17T08:00:00Z
expect destroy "etiqueta sem fuso" 2026-11-17T08:00:00 2026-11-17T08:30:00Z
expect destroy "etiqueta so com a data" 2026-11-17 2026-11-17T08:30:00Z
expect destroy "etiqueta em texto" ontem 2026-11-17T08:30:00Z
expect destroy "mes 13" 2026-13-17T08:00:00Z 2026-11-17T08:30:00Z
expect destroy "31 de novembro" 2026-11-31T08:00:00Z 2026-11-17T08:30:00Z
expect destroy "29 de fevereiro fora de bissexto" 2027-02-29T08:00:00Z 2027-03-01T08:30:00Z
expect destroy "hora 24" 2026-11-17T24:00:00Z 2026-11-17T08:30:00Z
expect destroy "fuso +25:00" 2026-11-17T08:00:00+25:00 2026-11-17T08:30:00Z
expect destroy "epoch em segundos" 1795248000 2026-11-17T08:30:00Z
expect destroy "espaco no lugar do T" "2026-11-17 08:00:00Z" 2026-11-17T08:30:00Z
expect destroy "lixo depois do fuso" "2026-11-17T08:00:00Zx" 2026-11-17T08:30:00Z
# Uma etiqueta no futuro manteria o ambiente para sempre; alem de 10 min de folga de relogio, derruba.
expect keep "futuro dentro da folga de relogio" 2026-11-17T08:05:00Z 2026-11-17T08:00:00Z
expect destroy "futuro alem da folga de relogio" 2026-11-18T08:00:00Z 2026-11-17T08:00:00Z

# Fuso diferente de UTC, na etiqueta ou no agora: a conta e feita no mesmo instante.
expect keep "etiqueta em -03:00, 7 h 59 min" 2026-11-17T05:00:00-03:00 2026-11-17T15:59:00Z
expect destroy "etiqueta em -03:00, 8 h 01 min" 2026-11-17T05:00:00-03:00 2026-11-17T16:01:00Z
expect keep "etiqueta em +05:30, 7 h 59 min" 2026-11-17T13:30:00+05:30 2026-11-17T15:59:00Z
expect destroy "etiqueta em +05:30, 8 h 01 min" 2026-11-17T13:30:00+05:30 2026-11-17T16:01:00Z
expect keep "agora em -03:00, 7 h 59 min" 2026-11-17T08:00:00Z 2026-11-17T12:59:00-03:00
expect destroy "agora em -03:00, 8 h 01 min" 2026-11-17T08:00:00Z 2026-11-17T13:01:00-03:00
# Lida como hora local sem o fuso, a etiqueta em -03:00 pareceria 3 h mais nova e o ambiente ficaria ligado.
expect destroy "etiqueta em -03:00 com 10 h 59 min" 2026-11-17T01:00:00-03:00 2026-11-17T14:59:00Z
expect keep "minusculas t e z" 2026-11-17t08:00:00z 2026-11-17T15:59:00Z

# Erro de quem chama: "agora" invalido ou argumentos faltando.
expect_error "agora malformado" 2026-11-17T08:00:00Z ontem
expect_error "sem argumentos"

echo "$((total - failures))/$total casos ok"
[ "$failures" -eq 0 ]
