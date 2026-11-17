#!/usr/bin/env bash
# Validacao sem credenciais do ambiente efemero: fmt, init sem backend, validate, tflint (raiz e modulos
# locais), conferencia do roteamento do ALB contra o nginx local, da mensageria contra o LocalStack e dos
# nomes de servico entre o Pod Identity e os charts Helm.
set -euo pipefail
cd "$(dirname "$0")"
config="$PWD/.tflint.hcl"

terraform fmt -check -recursive ../..
terraform init -backend=false -input=false -lockfile=readonly >/dev/null
terraform validate

tflint --init >/dev/null
tflint
for module in network eks edge-alb edge identity messaging data access; do
  tflint --chdir="../../modules/$module" --config="$config"
done

./check-alb-routes.py
./check-messaging.py
./check-service-names.py
