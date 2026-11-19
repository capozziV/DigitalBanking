#!/usr/bin/env bash
# Testes do leftovers.sh sem AWS: um aws falso no PATH devolve respostas fixas (variaveis FAKE_*).
# Roda no CI (job aws-scripts) e localmente: infra/aws/leftovers.test.sh
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
failures=0

cat > "$tmp/aws" <<'FAKE'
#!/usr/bin/env bash
case "$1 $2" in
  "resourcegroupstaggingapi get-resources") [ -z "${FAKE_FAIL:-}" ] || { echo "AccessDenied" >&2; exit 254; }
    empty='{"ResourceTagMappingList": []}'
    echo "${FAKE_TAGGED:-$empty}" ;;
  "ec2 describe-vpcs") printf '%s\n' "${FAKE_VPCS:-}" ;;
  "ec2 describe-network-interfaces") printf '%s\n' "${FAKE_ENIS:-}" ;;
  *) echo "aws falso: chamada inesperada: $*" >&2; exit 1 ;;
esac
FAKE
chmod +x "$tmp/aws"

bootstrap='{"ResourceARN":"arn:aws:s3:::digitalbanking-tfstate","Tags":[{"Key":"project","Value":"digitalbanking"},{"Key":"stack","Value":"bootstrap"}]}'
env_resource='{"ResourceARN":"arn:aws:rds:sa-east-1:123456789012:db:digitalbanking","Tags":[{"Key":"project","Value":"digitalbanking"},{"Key":"created-at","Value":"2026-11-17T08:00:00Z"}]}'

# Uso: expect <exit-esperado> <texto-esperado-na-saida> <descricao> [VAR=valor...] -- [vpc-id]
expect() {
  local expected_status="$1" expected_text="$2" description="$3" status=0 output
  shift 3
  local vars=()
  while [ "$1" != "--" ]; do vars+=("$1"); shift; done
  shift
  output=$(env PATH="$tmp:$PATH" AWS_REGION=sa-east-1 LEFTOVERS_ATTEMPTS=2 LEFTOVERS_INTERVAL=0 ${vars[@]+"${vars[@]}"} \
    "$here/leftovers.sh" "$@" 2>&1) || status=$?
  if [ "$status" -eq "$expected_status" ] && grep -qF -- "$expected_text" <<< "$output"; then
    echo "ok    $description (exit $status)"
  else
    echo "FALHA $description: exit $status (esperado $expected_status), saida:" >&2
    printf '%s\n' "$output" | sed 's/^/        /' >&2
    failures=$((failures + 1))
  fi
}

expect 0 "OK: nada do env sobrou" "conta vazia" --
expect 0 "OK: nada do env sobrou" "so o bootstrap fica" "FAKE_TAGGED={\"ResourceTagMappingList\":[$bootstrap]}" -- vpc-123
expect 1 "arn:aws:rds:sa-east-1:123456789012:db:digitalbanking" "recurso do env com a etiqueta" \
  "FAKE_TAGGED={\"ResourceTagMappingList\":[$bootstrap,$env_resource]}" --
expect 1 "eni: eni-0abc" "ENI presa na VPC" "FAKE_ENIS=eni-0abc	interface	ELB app/digitalbanking" -- vpc-123
expect 1 "vpc: vpc-123" "VPC ainda existe" "FAKE_VPCS=vpc-123" -- vpc-123
expect 0 "OK: nada do env sobrou" "sem vpc-id, ENIs nao sao consultadas" "FAKE_ENIS=eni-0abc	interface	x" --
if out=$(env PATH="$tmp:$PATH" AWS_REGION=sa-east-1 FAKE_FAIL=1 "$here/leftovers.sh" 2>&1); then
  echo "FALHA erro da AWS passou como sucesso: $out" >&2
  failures=$((failures + 1))
elif grep -q "OK: nada" <<< "$out"; then
  echo "FALHA erro da AWS imprimiu OK" >&2
  failures=$((failures + 1))
else
  echo "ok    erro da AWS falha o script"
fi

[ "$failures" -eq 0 ] && echo "leftovers.test: ok"
[ "$failures" -eq 0 ]
