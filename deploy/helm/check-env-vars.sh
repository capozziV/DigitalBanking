#!/usr/bin/env bash
# Confere que o values de cada servico define toda variavel ${NOME} SEM padrao dos application*.yml do servico
# (application.yml e application-aws.yml). Sem a variavel, o pod nao sobe na AWS.
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

  rendered=$(helm template "$svc" "$here/service" -f "$here/values/$svc.yaml" \
    --set image.repository=registry.example/$svc --set image.tag=test \
    --set targetGroupArn=arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/$svc/0123456789abcdef)

  echo "$svc: $(echo "$required" | grep -c . || true) variavel(is) sem padrao em $(printf '%s ' "${files[@]##*/}")"
  for var in $required; do
    if echo "$rendered" | grep -qE "^  $var:|^            - name: $var\$|^- name: $var\$|name: $var\$"; then
      echo "  ok   $var"
    else
      echo "  FALTA $var no values de $svc" >&2
      failures=$((failures + 1))
    fi
  done
done

[ "$failures" -eq 0 ] || { echo "ERRO: $failures variavel(is) sem definicao no values" >&2; exit 1; }
echo "check-env-vars: ok"
