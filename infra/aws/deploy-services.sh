#!/usr/bin/env bash
# Instala no EKS, a partir das saidas do Terraform (envs/aws), o Job db-init e os cinco servicos, no namespace do
# Pod Identity (digitalbanking). Usado pelo workflow aws-up.
#   deploy-services.sh <outputs.json> <registro-ecr> <tag>             instala (exige helm, kubectl e aws CLI)
#   deploy-services.sh --render <dir> <outputs.json> <registro-ecr> <tag>   so renderiza (helm template), sem cluster
#                                                                           nem AWS: o CI confere o mapeamento
# outputs.json: `terraform output -json`. registro-ecr: <conta>.dkr.ecr.<regiao>.amazonaws.com (repositorios
# digital-banking/<servico> do bootstrap). tag: SHA do commit.
# Ordem: db-init (hook post-install: o helm espera o Job e falha junto; depois confere .status.succeeded), Secrets
# do Kubernetes a partir do Secrets Manager e os servicos (--wait, um por vez). Nenhum ARN de papel vai aos charts:
# o Pod Identity liga o papel pela ServiceAccount no namespace.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
charts="$here/../../deploy/helm"
namespace=digitalbanking
services=(customer accounts transfers notification statement)

render_dir=""
if [ "${1:-}" = "--render" ]; then
  render_dir="${2:?uso: deploy-services.sh --render <dir> <outputs.json> <registro-ecr> <tag>}"
  shift 2
fi
outputs="${1:?uso: deploy-services.sh [--render <dir>] <outputs.json> <registro-ecr> <tag>}"
registry="${2:?informe o registro do ECR}"
tag="${3:?informe a tag da imagem (SHA do commit)}"

# Valor de uma saida do Terraform (terraform output -json: {"nome": {"value": ...}}); falha se faltar.
out() { jq -er --arg k "$1" '.[$k].value' "$outputs"; }
# Valor de uma chave de uma saida em mapa.
out_map() { jq -er --arg k "$1" --arg m "$2" '.[$k].value[$m]' "$outputs"; }

db_address=$(out db_address)
issuer=$(out issuer_url)
token_uri=$(out token_endpoint_url)
machine_client=$(out machine_client_id)
redis=$(out redis_endpoint)

# Prefixo dos segredos <prefixo>/<servico>-db (variavel name do Terraform), tirado de secret_names.
secret_prefix=$(jq -er '.secret_names.value["accounts-db"] | sub("/accounts-db$"; "")' "$outputs")

required_env() { awk '/^requiredEnv:/{f=1;next} f&&/^  - /{print $2;next} f{exit}' "$charts/values/$1.yaml"; }

# Origem de cada chave obrigatoria do values: o sufixo depois de <SERVICO>_ diz qual saida do Terraform usar.
# Chave nova sem origem aqui falha, em vez de o pod subir com o padrao local do application.yml.
env_value() {
  local svc="$1" key="$2" upper
  upper=$(tr '[:lower:]' '[:upper:]' <<< "$svc")
  case "${key#"${upper}"_}" in
    DB_URL) echo "jdbc:postgresql://$db_address:5432/$svc?sslmode=require" ;;
    JWT_ISSUER_URI) echo "$issuer" ;;
    OAUTH_TOKEN_URI) echo "$token_uri" ;;
    OAUTH_CLIENT_ID) echo "$machine_client" ;;
    TOPIC_ARN) out_map topic_arns "$svc-events.fifo" || return 1 ;;
    REDIS_HOST) echo "$redis" ;;
    *) echo "sem origem para $key ($svc) em deploy-services.sh" >&2; return 1 ;;
  esac
}

master_secret=$(out db_master_secret_arn)
db_init_args=(-n "$namespace" --set "dbHost=$db_address" --set "masterSecretId=$master_secret"
  --set "secretPrefix=$secret_prefix")

# Argumentos do helm de um servico, no array global args. Cada $(...) tem o proprio "|| exit 1": dentro de funcao
# chamada por substituicao de comando o set -e do bash nao vale, e uma saida faltando passaria em silencio.
load_service_args() {
  local svc="$1" key value tg keys
  tg=$(out_map target_group_arns "$svc") || exit 1
  keys=$(required_env "$svc") || exit 1
  args=(-f "$charts/values/$svc.yaml" --set "image.repository=$registry/digital-banking/$svc" --set "image.tag=$tag"
    --set "targetGroupArn=$tg")
  for key in $keys; do
    value=$(env_value "$svc" "$key") || exit 1
    args+=(--set-string "env.$key=$value")
  done
}

if [ -n "$render_dir" ]; then
  mkdir -p "$render_dir"
  helm template db-init "$charts/db-init" "${db_init_args[@]}" > "$render_dir/db-init.yaml"
  for svc in "${services[@]}"; do
    load_service_args "$svc"
    helm template "$svc" "$charts/service" -n "$namespace" "${args[@]}" > "$render_dir/$svc.yaml"
  done
  echo "renderizado em $render_dir: db-init ${services[*]}"
  exit 0
fi

echo "== namespace $namespace"
kubectl create namespace "$namespace" --dry-run=client -o yaml | kubectl apply -f -

echo "== db-init"
helm upgrade --install db-init "$charts/db-init" "${db_init_args[@]}" --wait --timeout 15m
succeeded=$(kubectl -n "$namespace" get job db-init -o jsonpath='{.status.succeeded}')
if [ "$succeeded" != 1 ]; then
  echo "FALHA: Job db-init sem sucesso (status.succeeded='$succeeded')" >&2
  kubectl -n "$namespace" describe job db-init >&2 || true
  kubectl -n "$namespace" logs job/db-init --all-containers >&2 || true
  exit 1
fi
echo "OK: Job db-init concluido"

# Secrets do Kubernetes do secretEnv dos values (<servico>-db, <servico>-oauth), com o JSON do Secrets Manager
# como stringData. A senha passa so por pipes, nunca por argumento nem por arquivo.
echo "== Secrets dos servicos"
for name in $(jq -r '.secret_names.value | keys[]' "$outputs"); do
  secret_id=$(out_map secret_names "$name") || exit 1
  aws secretsmanager get-secret-value --secret-id "$secret_id" --query SecretString --output text |
    jq --arg name "$name" --arg ns "$namespace" \
      '{apiVersion: "v1", kind: "Secret", type: "Opaque", metadata: {name: $name, namespace: $ns}, stringData: .}' |
    kubectl apply -f - > /dev/null
  echo "OK: Secret $name"
done

for svc in "${services[@]}"; do
  echo "== $svc"
  load_service_args "$svc"
  helm upgrade --install "$svc" "$charts/service" -n "$namespace" "${args[@]}" --wait --timeout 15m
done
echo "OK: db-init e ${services[*]} instalados em $namespace"
