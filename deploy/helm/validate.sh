#!/usr/bin/env bash
# Valida o chart sem credenciais nem cluster: helm lint, helm template de cada values, kubeconform -strict
# (schemas do Kubernetes e o do CRD TargetGroupBinding em schemas/) e as regras do projeto sobre o manifesto.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
# Versao do Kubernetes do schema: igual a do EKS (variavel kubernetes_version em infra/terraform/envs/aws/variables.tf,
# "1.36" no Terraform = 1.36.0 aqui). O CI repassa KUBERNETES_VERSION lida do Terraform.
k8s_version="${KUBERNETES_VERSION:-1.36.0}"
services=(accounts transfers statement notification customer)
tg_arn="arn:aws:elasticloadbalancing:sa-east-1:123456789012:targetgroup/validate/0123456789abcdef"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

# Valores de instalacao: imagem, target group e um valor ficticio para cada chave de requiredEnv do servico.
sets() {
  local out="--set image.repository=registry.example/$1 --set image.tag=0123456789abcdef0123456789abcdef01234567 --set targetGroupArn=$tg_arn" key
  for key in $(required_env "$1"); do out="$out --set-string env.$key=valor-de-teste"; done
  echo "$out"
}
required_env() { awk '/^requiredEnv:/{f=1;next} f&&/^  - /{print $2;next} f{exit}' "$here/values/$1.yaml"; }

for svc in "${services[@]}"; do
  echo "== $svc"
  # shellcheck disable=SC2046
  helm lint "$here/service" -f "$here/values/$svc.yaml" $(sets "$svc") --strict
  # shellcheck disable=SC2046
  helm template "$svc" "$here/service" -f "$here/values/$svc.yaml" $(sets "$svc") > "$tmp/$svc.yaml"

  kubeconform -strict -summary -kubernetes-version "$k8s_version" \
    -schema-location default \
    -schema-location "$here/schemas/{{ .ResourceKind }}_{{ .ResourceAPIVersion }}.json" \
    "$tmp/$svc.yaml"

  m="$tmp/$svc.yaml"
  grep -q 'path: /actuator/health/liveness' "$m" || { echo "ERRO: $svc sem probe de liveness" >&2; exit 1; }
  grep -q 'path: /actuator/health/readiness' "$m" || { echo "ERRO: $svc sem probe de readiness" >&2; exit 1; }
  grep -qE 'SPRING_PROFILES_ACTIVE: "aws"' "$m" || { echo "ERRO: $svc sem perfil aws" >&2; exit 1; }
  grep -q 'runAsNonRoot: true' "$m" || { echo "ERRO: $svc pode rodar como root" >&2; exit 1; }
  # O Pod Identity (Terraform) associa papel e ServiceAccount pelo nome: Deployment, ServiceAccount e servico tem de coincidir.
  grep -qE "^      serviceAccountName: $svc\$" "$m" || { echo "ERRO: $svc com serviceAccountName diferente do nome do servico" >&2; exit 1; }
  awk -v n="$svc" '/^kind: ServiceAccount/{f=1} f&&/^  name:/{print $2; exit}' "$m" | grep -qx "$svc" || { echo "ERRO: $svc com ServiceAccount de outro nome" >&2; exit 1; }
  grep -q 'kind: TargetGroupBinding' "$m" || { echo "ERRO: $svc sem TargetGroupBinding" >&2; exit 1; }
  grep -q 'kind: PodDisruptionBudget' "$m" || { echo "ERRO: $svc sem PodDisruptionBudget" >&2; exit 1; }
  # Pod Identity, nunca IRSA: a anotacao faria o SDK tentar web identity com uma trust que so aceita pods.eks.amazonaws.com.
  if grep -q 'eks.amazonaws.com/role-arn' "$m"; then echo "ERRO: $svc com anotacao IRSA na ServiceAccount" >&2; exit 1; fi
  # Os recursos da AWS ficam em sa-east-1 (infra/terraform/envs/aws, variavel region).
  if grep -qE 'us-east-1|AWS_REGION: "[^s]' "$m"; then echo "ERRO: $svc com regiao diferente de sa-east-1" >&2; exit 1; fi
  grep -qE '_AWS_REGION: "sa-east-1"' "$m" || { echo "ERRO: $svc sem *_AWS_REGION sa-east-1" >&2; exit 1; }
done

echo "== db-init"
db_init_sets=(--set dbHost=validate.cluster-x.sa-east-1.rds.amazonaws.com
  --set masterSecretId=arn:aws:secretsmanager:sa-east-1:123456789012:secret:rds!db-validate
  --set secretPrefix=digitalbanking)
helm lint "$here/db-init" "${db_init_sets[@]}" --strict
helm template db-init "$here/db-init" "${db_init_sets[@]}" > "$tmp/db-init.yaml"
kubeconform -strict -summary -kubernetes-version "$k8s_version" -schema-location default "$tmp/db-init.yaml"
m="$tmp/db-init.yaml"
awk '/^kind: ServiceAccount/{f=1} f&&/^  name:/{print $2; exit}' "$m" | grep -qx db-init || { echo "ERRO: db-init com ServiceAccount de outro nome" >&2; exit 1; }
grep -qE '^      serviceAccountName: db-init$' "$m" || { echo "ERRO: Job db-init sem a ServiceAccount db-init" >&2; exit 1; }
if grep -q 'eks.amazonaws.com/role-arn' "$m"; then echo "ERRO: db-init com anotacao IRSA" >&2; exit 1; fi
grep -q 'helm.sh/hook: post-install,post-upgrade' "$m" || { echo "ERRO: Job db-init fora do hook post-install/post-upgrade" >&2; exit 1; }
grep -q 'runAsNonRoot: true' "$m" || { echo "ERRO: db-init pode rodar como root" >&2; exit 1; }
grep -q 'medium: Memory' "$m" || { echo "ERRO: db-init grava as senhas fora de memoria" >&2; exit 1; }
grep -q 'value: "sa-east-1"' "$m" || { echo "ERRO: db-init fora de sa-east-1" >&2; exit 1; }
bash -n "$here/db-init/files/fetch-secrets.sh" "$here/db-init/files/create-service-databases.sh"
for key in dbHost masterSecretId secretPrefix; do
  if helm template db-init "$here/db-init" "${db_init_sets[@]}" --set "$key=" >/dev/null 2>&1; then
    echo "ERRO: db-init renderizou com $key vazio" >&2; exit 1
  fi
  echo "  recusado  db-init $key vazio"
done

echo "== o chart recusa render sem image.tag"
if helm template x "$here/service" -f "$here/values/accounts.yaml" --set image.repository=r --set targetGroupArn="$tg_arn" >/dev/null 2>&1; then
  echo "ERRO: o chart renderizou sem image.tag" >&2; exit 1
fi

echo "== o chart recusa render sem cada valor obrigatorio (requiredEnv)"
for svc in "${services[@]}"; do
  for key in $(required_env "$svc"); do
    # shellcheck disable=SC2046
    if helm template "$svc" "$here/service" -f "$here/values/$svc.yaml" $(sets "$svc") --set-string "env.$key=" >/dev/null 2>&1; then
      echo "ERRO: $svc renderizou com env.$key vazio" >&2; exit 1
    fi
    echo "  recusado  $svc env.$key vazio"
  done
done

echo "== variaveis sem padrao dos application*.yml"
"$here/check-env-vars.sh"

echo "validate: ok"
