#!/usr/bin/env bash
# Valida o chart sem credenciais nem cluster: helm lint, helm template de cada values, kubeconform -strict
# (schemas do Kubernetes e o do CRD TargetGroupBinding em schemas/) e as regras do projeto sobre o manifesto.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
k8s_version="${KUBERNETES_VERSION:-1.33.0}"
services=(accounts transfers statement notification customer)
tg_arn="arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/validate/0123456789abcdef"
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
