#!/usr/bin/env bash
# Fonte unica da versao do Kubernetes fora do Terraform: le o default da variavel kubernetes_version de
# variables.tf (ex.: "1.36") e imprime a versao de patch que o kubeconform espera (ex.: 1.36.0).
# Usado por deploy/helm/validate.sh (padrao de KUBERNETES_VERSION) e pelo job helm do CI.
set -euo pipefail

variables="$(cd "$(dirname "$0")" && pwd)/variables.tf"
version=$(awk '
  /^variable "kubernetes_version"/ { inside = 1; next }
  inside && /^}/ { exit }
  inside && $1 == "default" { gsub(/"/, "", $3); print $3; exit }
' "$variables")

if ! [[ "$version" =~ ^[0-9]+\.[0-9]+$ ]]; then
  echo "kubernetes_version sem default MAIOR.MENOR em $variables: '$version'" >&2
  exit 1
fi
echo "$version.0"
