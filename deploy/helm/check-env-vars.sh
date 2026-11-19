#!/usr/bin/env bash
# Confere que o values de cada servico (values/<servico>.yaml, env ou secretEnv) define:
#  1. toda variavel ${NOME} SEM padrao dos application*.yml do servico (application.yml e application-aws.yml);
#  2. toda variavel cujo padrao e local: contem localhost, 000000000000 ou local-secret, ou termina em _app/_owner
#     (usuario e senha de banco). Sem isso o pod sobe na AWS apontando para o padrao local, em silencio.
# Le os arquivos do worktree de forma generica: vale de novo quando o application-aws.yml mudar.
# Uso: check-env-vars.sh   (REPO_ROOT opcional: raiz do repositorio a ler, padrao o deste script)
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
root="${REPO_ROOT:-$(cd "$here/../.." && pwd)}"
services=(accounts transfers statement notification customer)
failures=0

for svc in "${services[@]}"; do
  resources="$root/services/$svc/bootstrap/src/main/resources"
  files=()
  for f in "$resources"/application.yml "$resources"/application-*.yml; do
    [ -f "$f" ] && files+=("$f")
  done
  [ "${#files[@]}" -gt 0 ] || { echo "ERRO: $svc sem application*.yml em $resources" >&2; exit 1; }

  # ${NOME} sem ":" antes do "}" = sem padrao. Comentarios ficam de fora.
  required=$(grep -hv '^[[:space:]]*#' "${files[@]}" | grep -oE '\$\{[A-Za-z_][A-Za-z0-9_]*\}' | tr -d '${}' | sort -u || true)

  # Le o values-fonte (nao o render: o render receberia valores ficticios de requiredEnv e esconderia a ausencia).
  values="$here/values/$svc.yaml"

  # ${NOME:padrao} com padrao local (comentarios e a forma sem padrao ficam de fora desta regra).
  local_defaults=$(grep -hv '^[[:space:]]*#' "${files[@]}" | grep -oE '\$\{[A-Za-z_][A-Za-z0-9_]*:[^}]*\}' \
    | grep -E ':[^}]*(localhost|000000000000|local-secret)|_(app|owner)\}$' | sed -E 's/^\$\{([A-Za-z0-9_]+):.*/\1/' | sort -u || true)
  required=$(printf '%s\n%s\n' "$required" "$local_defaults" | grep . | sort -u || true)

  echo "$svc: $(echo "$required" | grep -c . || true) variavel(is) (sem padrao ou com padrao local) em $(printf '%s ' "${files[@]##*/}")"
  for var in $required; do
    if grep -qE "^  $var:|^    - $var\$" "$values"; then
      echo "  ok   $var"
    else
      echo "  FALTA $var no values de $svc" >&2
      failures=$((failures + 1))
    fi
  done
done

[ "$failures" -eq 0 ] || { echo "ERRO: $failures variavel(is) sem definicao no values" >&2; exit 1; }
echo "check-env-vars: ok"
