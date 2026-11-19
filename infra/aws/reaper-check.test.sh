#!/usr/bin/env bash
# Testes do reaper-check.sh sem AWS: um aws falso no PATH devolve a resposta da Tagging API em FAKE_TAGGED.
# Roda no CI (job aws-scripts) e localmente: infra/aws/reaper-check.test.sh
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
failures=0
now=2026-11-17T16:00:00Z

cat > "$tmp/aws" <<'FAKE'
#!/usr/bin/env bash
[ "$1 $2" = "resourcegroupstaggingapi get-resources" ] || { echo "aws falso: chamada inesperada: $*" >&2; exit 1; }
[ -z "${FAKE_FAIL:-}" ] || { echo "AccessDenied" >&2; exit 254; }
echo "{\"ResourceTagMappingList\": [${FAKE_TAGGED:-}]}"
FAKE
chmod +x "$tmp/aws"

# Recurso etiquetado. Uso: resource <arn> [created-at|-] [stack]; "-" omite a etiqueta created-at.
resource() {
  local tags='{"Key":"project","Value":"digitalbanking"}'
  [ "${2:--}" = "-" ] || tags="$tags,{\"Key\":\"created-at\",\"Value\":\"$2\"}"
  [ -z "${3:-}" ] || tags="$tags,{\"Key\":\"stack\",\"Value\":\"$3\"}"
  echo "{\"ResourceARN\":\"$1\",\"Tags\":[$tags]}"
}

# Uso: expect <decisao> <descricao> <recursos-separados-por-virgula>
expect() {
  local expected="$1" description="$2" tagged="$3" actual status=0
  actual=$(env PATH="$tmp:$PATH" AWS_REGION=sa-east-1 FAKE_TAGGED="$tagged" "$here/reaper-check.sh" "$now" 2>/dev/null) ||
    status=$?
  if [ "$status" -eq 0 ] && [ "$actual" = "$expected" ]; then
    echo "ok    $description -> $actual"
  else
    echo "FALHA $description: '$actual' (exit $status), esperado '$expected'" >&2
    failures=$((failures + 1))
  fi
}

bucket=$(resource arn:aws:s3:::digitalbanking-tfstate - bootstrap)
ecr=$(resource arn:aws:ecr:sa-east-1:123456789012:repository/digital-banking/accounts - bootstrap)
fresh=$(resource arn:aws:eks:sa-east-1:123456789012:cluster/digitalbanking 2026-11-17T08:01:00Z)
fresh_db=$(resource arn:aws:rds:sa-east-1:123456789012:db:digitalbanking 2026-11-17T08:01:00Z)
old=$(resource arn:aws:ec2:sa-east-1:123456789012:natgateway/nat-0 2026-11-17T07:59:00Z)
untagged=$(resource arn:aws:ec2:sa-east-1:123456789012:vpc/vpc-0)
malformed=$(resource arn:aws:ec2:sa-east-1:123456789012:vpc/vpc-1 ontem)

expect none "conta vazia" ""
expect none "so o bootstrap" "$bucket,$ecr"
expect keep "env com 7 h 59 min" "$bucket,$fresh,$fresh_db"
expect destroy "env com 8 h 01 min" "$bucket,$old"
expect destroy "sobra antiga ao lado de um env novo" "$fresh,$old"
expect destroy "recurso do env sem created-at" "$fresh,$untagged"
expect destroy "created-at malformada" "$fresh,$malformed"

if env PATH="$tmp:$PATH" AWS_REGION=sa-east-1 FAKE_FAIL=1 "$here/reaper-check.sh" "$now" > /dev/null 2>&1; then
  echo "FALHA erro da AWS passou como sucesso" >&2
  failures=$((failures + 1))
else
  echo "ok    erro da AWS falha o script"
fi

[ "$failures" -eq 0 ] && echo "reaper-check.test: ok"
[ "$failures" -eq 0 ]
