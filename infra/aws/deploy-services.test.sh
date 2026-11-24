#!/usr/bin/env bash
# Teste do deploy-services.sh sem cluster nem AWS: renderiza o db-init, os cinco servicos e o app (bff e web) a partir de saidas
# ficticias do Terraform (testdata/terraform-outputs.json), valida com kubeconform -strict e confere que cada valor
# veio da saida certa, que nenhum ARN de papel chega aos charts e que uma saida faltando derruba a renderizacao.
# Exige helm, kubeconform e jq. Roda no CI (job helm).
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
schemas="$here/../../deploy/helm/schemas"
fixture="$here/testdata/terraform-outputs.json"
k8s_version="${KUBERNETES_VERSION:-$("$here/../terraform/envs/aws/kubernetes-version.sh")}"
registry=123456789012.dkr.ecr.sa-east-1.amazonaws.com
tag=0123456789abcdef0123456789abcdef01234567
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

fail() { echo "FALHA: $*" >&2; exit 1; }
# Confere que o manifesto tem a linha (grep -F, texto literal).
has() { grep -qF -- "$2" "$tmp/out/$1.yaml" || fail "$1 sem '$2'"; }

"$here/deploy-services.sh" --render "$tmp/out" "$fixture" "$registry" "$tag"

for manifest in "$tmp"/out/*.yaml; do
  kubeconform -strict -summary -kubernetes-version "$k8s_version" -schema-location default \
    -schema-location "$schemas/{{ .ResourceKind }}_{{ .ResourceAPIVersion }}.json" "$manifest"
done

has db-init 'digitalbanking-db.abcdefghijkl.sa-east-1.rds.amazonaws.com'
has db-init 'arn:aws:secretsmanager:sa-east-1:123456789012:secret:rds!db-0123-abcdef'
has db-init '"digitalbanking"'

# Coletor ADOT (etapa 10): instalado antes dos servicos, no namespace do Pod Identity, com a ServiceAccount fixa.
has otel-collector 'serviceAccountName: otel-collector'
has otel-collector 'traces_endpoint: https://xray.sa-east-1.amazonaws.com/v1/traces'
for svc in customer accounts transfers notification statement bff; do
  has "$svc" 'OTEL_EXPORTER_OTLP_ENDPOINT: "http://otel-collector:4318"'
  has "$svc" "OTEL_SERVICE_NAME: \"$svc\""
done

for svc in customer accounts transfers notification statement; do
  has "$svc" "image: \"$registry/digital-banking/$svc:$tag\""
  has "$svc" "targetGroupARN: \"$(jq -r --arg s "$svc" '.target_group_arns.value[$s]' "$fixture")\""
  has "$svc" '"https://cognito-idp.sa-east-1.amazonaws.com/sa-east-1_TESTPOOL"'
done
for svc in customer accounts transfers notification; do
  has "$svc" "\"jdbc:postgresql://digitalbanking-db.abcdefghijkl.sa-east-1.rds.amazonaws.com:5432/$svc?sslmode=require\""
done
for svc in customer accounts transfers; do
  has "$svc" "\"arn:aws:sns:sa-east-1:123456789012:$svc-events.fifo\""
done
for svc in accounts transfers; do
  has "$svc" '"https://digitalbanking-test.auth.sa-east-1.amazoncognito.com/oauth2/token"'
  has "$svc" '"machineclientid0123456789"'
done
for svc in transfers statement; do
  has "$svc" '"digitalbanking-redis.abc123.0001.sae1.cache.amazonaws.com"'
done
# App (etapa 9): bff com o client do Cognito e o Redis provisionados, web so com imagem e target group.
for svc in bff web; do
  has "$svc" "image: \"$registry/digital-banking/$svc:$tag\""
  has "$svc" "targetGroupARN: \"$(jq -r --arg s "$svc" '.target_group_arns.value[$s]' "$fixture")\""
done
has bff '"bffclientid0123456789"'
has bff 'COGNITO_REGION: "sa-east-1"'
has bff '"redis://digitalbanking-redis.abc123.0001.sae1.cache.amazonaws.com:6379"'
has bff '"http://accounts:8081"'
echo "ok: valores de cada servico vieram das saidas certas"

# O Pod Identity liga o papel pela ServiceAccount: nenhum ARN de papel nem anotacao IRSA nos manifestos.
if grep -l 'arn:aws:iam::\|eks.amazonaws.com/role-arn' "$tmp"/out/*.yaml; then
  fail "ARN de papel ou anotacao IRSA nos manifestos acima"
fi
echo "ok: nenhum ARN de papel nos charts"

# Saida do Terraform faltando derruba a renderizacao (sem cair no padrao local do application.yml).
for missing in topic_arns redis_endpoint target_group_arns db_master_secret_arn cognito_bff_client_id; do
  jq --arg k "$missing" 'del(.[$k])' "$fixture" > "$tmp/partial.json"
  if "$here/deploy-services.sh" --render "$tmp/partial" "$tmp/partial.json" "$registry" "$tag" > /dev/null 2>&1; then
    fail "renderizou sem a saida $missing"
  fi
  echo "ok: recusado sem a saida $missing"
done
echo "deploy-services.test: ok"
