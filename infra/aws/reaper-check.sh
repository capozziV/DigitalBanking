#!/usr/bin/env bash
# Passo do workflow aws-reaper: existe ambiente efemero e ele passou do prazo? Imprime "none", "keep" ou "destroy".
# Uso: infra/aws/reaper-check.sh [agora]   (agora em RFC 3339, padrao o relogio do sistema; existe para os testes)
# Le pela Resource Groups Tagging API os recursos com project=digitalbanking fora do bootstrap (stack=bootstrap).
# Nenhum -> none. Senao, cada valor distinto da etiqueta created-at (ausente conta como vazio) passa pelo
# reaper-decision.sh; basta um "destroy" para derrubar (um recurso de uma subida anterior que sobrou tambem conta).
set -euo pipefail

: "${AWS_REGION:?defina AWS_REGION}"
here="$(cd "$(dirname "$0")" && pwd)"
now="${1:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}"

resources=$(aws resourcegroupstaggingapi get-resources --region "$AWS_REGION" \
  --tag-filters Key=project,Values=digitalbanking --output json |
  jq -c '[.ResourceTagMappingList[]
    | select(([.Tags[]? | select(.Key == "stack" and .Value == "bootstrap")] | length) == 0)
    | {arn: .ResourceARN, createdAt: ([.Tags[]? | select(.Key == "created-at") | .Value] | first // "")}]')

count=$(jq 'length' <<< "$resources")
if [ "$count" -eq 0 ]; then
  echo "nenhum recurso do env na conta" >&2
  echo none
  exit 0
fi

decision=keep
while IFS= read -r created_at; do
  verdict=$("$here/reaper-decision.sh" "$created_at" "$now")
  echo "created-at '$created_at': $verdict ($(jq --arg c "$created_at" '[.[] | select(.createdAt == $c)] | length' <<< "$resources") recursos)" >&2
  [ "$verdict" != destroy ] || decision=destroy
done < <(jq -r '[.[].createdAt] | unique[]' <<< "$resources")
echo "$decision"
