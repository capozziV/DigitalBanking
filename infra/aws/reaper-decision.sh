#!/usr/bin/env bash
# Decide se o aws-reaper derruba o ambiente efemero: imprime "destroy" ou "keep".
# Uso: infra/aws/reaper-decision.sh <created-at> [agora]
#   created-at: valor da etiqueta created-at do ambiente (RFC 3339, com fuso: Z ou +-HH:MM). Vazio = etiqueta ausente.
#   agora: RFC 3339 (padrao: o relogio do sistema, em UTC). Existe para os testes (reaper-decision.test.sh).
# Regras: ligado ha 8 h ou mais -> destroy. Etiqueta ausente, malformada ou mais de 10 min no futuro (folga de
# relogio) -> destroy: na duvida, derruba. Sem dependencia do `date` para a conta (GNU e BSD divergem): o instante
# e calculado em bash a partir dos campos, com o fuso aplicado.
# Saida 2, sem decisao, se faltar o argumento ou se "agora" for invalido (erro de quem chama, nao do ambiente).
set -euo pipefail

max_age_seconds=$((8 * 3600))
clock_skew_seconds=$((10 * 60))

# Dias desde 1970-01-01 para uma data civil (algoritmo days_from_civil, calendario gregoriano proleptico).
days_from_civil() {
  local y=$1 m=$2 d=$3 era yoe doy doe
  if [ "$m" -le 2 ]; then y=$((y - 1)); fi
  era=$(((y >= 0 ? y : y - 399) / 400))
  yoe=$((y - era * 400))
  doy=$(((153 * (m > 2 ? m - 3 : m + 9) + 2) / 5 + d - 1))
  doe=$((yoe * 365 + yoe / 4 - yoe / 100 + doy))
  echo $((era * 146097 + doe - 719468))
}

days_in_month() {
  local y=$1 m=$2
  case "$m" in
    1 | 3 | 5 | 7 | 8 | 10 | 12) echo 31 ;;
    4 | 6 | 9 | 11) echo 30 ;;
    2) if [ $((y % 4)) -eq 0 ] && { [ $((y % 100)) -ne 0 ] || [ $((y % 400)) -eq 0 ]; }; then echo 29; else echo 28; fi ;;
  esac
}

# Segundos desde a epoch (UTC) de um instante RFC 3339; falha (retorno 1) se o texto nao for RFC 3339 valido.
epoch_of() {
  local re='^([0-9]{4})-([0-9]{2})-([0-9]{2})[Tt]([0-9]{2}):([0-9]{2}):([0-9]{2})(\.[0-9]+)?([Zz]|[+-][0-9]{2}:[0-9]{2})$'
  [[ "$1" =~ $re ]] || return 1
  local y=$((10#${BASH_REMATCH[1]})) mo=$((10#${BASH_REMATCH[2]})) d=$((10#${BASH_REMATCH[3]}))
  local h=$((10#${BASH_REMATCH[4]})) mi=$((10#${BASH_REMATCH[5]})) s=$((10#${BASH_REMATCH[6]})) zone="${BASH_REMATCH[8]}"
  [ "$mo" -ge 1 ] && [ "$mo" -le 12 ] || return 1
  [ "$d" -ge 1 ] && [ "$d" -le "$(days_in_month "$y" "$mo")" ] || return 1
  [ "$h" -le 23 ] && [ "$mi" -le 59 ] && [ "$s" -le 60 ] || return 1
  local offset=0
  if [ "$zone" != Z ] && [ "$zone" != z ]; then
    local oh=$((10#${zone:1:2})) om=$((10#${zone:4:2}))
    [ "$oh" -le 23 ] && [ "$om" -le 59 ] || return 1
    offset=$((oh * 3600 + om * 60))
    if [ "${zone:0:1}" = "-" ]; then offset=$((-offset)); fi
  fi
  # Hora local = UTC + offset, entao UTC = hora local - offset.
  echo $(($(days_from_civil "$y" "$mo" "$d") * 86400 + h * 3600 + mi * 60 + s - offset))
}

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  echo "uso: reaper-decision.sh <created-at> [agora]" >&2
  exit 2
fi
created_at="$1"
now_text="${2:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}"

if ! now=$(epoch_of "$now_text"); then
  echo "agora invalido: '$now_text' (esperado RFC 3339)" >&2
  exit 2
fi

if [ -z "$created_at" ]; then
  echo "etiqueta created-at ausente: derruba" >&2
  echo destroy
  exit 0
fi
if ! created=$(epoch_of "$created_at"); then
  echo "etiqueta created-at malformada: '$created_at': derruba" >&2
  echo destroy
  exit 0
fi

age=$((now - created))
if [ "$age" -lt "$((-clock_skew_seconds))" ]; then
  echo "etiqueta created-at no futuro ($created_at, agora $now_text): derruba" >&2
  echo destroy
elif [ "$age" -ge "$max_age_seconds" ]; then
  echo "ligado ha ${age} s (limite ${max_age_seconds} s): derruba" >&2
  echo destroy
else
  echo "ligado ha ${age} s (limite ${max_age_seconds} s): mantem" >&2
  echo keep
fi
