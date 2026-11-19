#!/usr/bin/env bash
# Confere, depois do terraform destroy do env, se sobrou recurso cobrado na conta. Usado pelo workflow aws-down.
# Uso: infra/aws/leftovers.sh [vpc-id]
#   1. Resource Groups Tagging API: recursos com project=digitalbanking fora do stack bootstrap (stack=bootstrap fica).
#   2. Com o vpc-id (saida vpc_id, lida antes do destroy): a VPC e as ENIs dela (ALB, VPC Link, nos e pods do EKS).
# So lista: nao apaga nada fora do Terraform (o papel do GitHub nao precisa de permissao de delete ampla). Sobrou
# algo -> exit 1, com a lista. A Tagging API e eventualmente consistente e mostra por um tempo recursos ja apagados:
# tenta de novo por ate LEFTOVERS_ATTEMPTS x LEFTOVERS_INTERVAL segundos (padrao 10 x 30 s) antes de falhar.
set -euo pipefail

: "${AWS_REGION:?defina AWS_REGION}"
vpc_id="${1:-}"
attempts="${LEFTOVERS_ATTEMPTS:-10}"
interval="${LEFTOVERS_INTERVAL:-30}"

tagged() {
  aws resourcegroupstaggingapi get-resources --region "$AWS_REGION" \
    --tag-filters Key=project,Values=digitalbanking --output json |
    jq -r '.ResourceTagMappingList[]
      | select(([.Tags[]? | select(.Key == "stack" and .Value == "bootstrap")] | length) == 0)
      | .ResourceARN'
}

vpc_leftovers() {
  [ -n "$vpc_id" ] || return 0
  aws ec2 describe-vpcs --region "$AWS_REGION" --filters "Name=vpc-id,Values=$vpc_id" \
    --query 'Vpcs[].VpcId' --output text | tr '\t' '\n' | sed '/^$/d; s/^/vpc: /' || return 1
  aws ec2 describe-network-interfaces --region "$AWS_REGION" --filters "Name=vpc-id,Values=$vpc_id" \
    --query 'NetworkInterfaces[].[NetworkInterfaceId, InterfaceType, Description]' --output text |
    sed '/^$/d; s/^/eni: /'
}

# Cada consulta numa atribuicao propria: um erro da AWS derruba o script (set -e), em vez de parecer "nada sobrou".
left=""
for attempt in $(seq 1 "$attempts"); do
  by_tag=$(tagged)
  in_vpc=$(vpc_leftovers)
  left=$(printf '%s\n%s\n' "$by_tag" "$in_vpc" | sed '/^$/d')
  if [ -z "$left" ]; then
    echo "OK: nada do env sobrou (tentativa $attempt/$attempts)"
    exit 0
  fi
  echo "ainda listados (tentativa $attempt/$attempts):"
  printf '%s\n' "$left" | sed 's/^/  /'
  [ "$attempt" -eq "$attempts" ] || sleep "$interval"
done

echo "FALHA: recursos do env sobraram depois do destroy (apague-os ou rode o aws-down de novo):" >&2
printf '%s\n' "$left" | sed 's/^/  /' >&2
exit 1
