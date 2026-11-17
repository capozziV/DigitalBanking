#!/usr/bin/env bash
# Validacao sem credenciais do ambiente efemero: fmt, init sem backend, validate, tflint (raiz e modulos
# locais) e conferencia do roteamento do ALB contra o nginx local.
set -euo pipefail
cd "$(dirname "$0")"
config="$PWD/.tflint.hcl"

terraform fmt -check -recursive ../..
terraform init -backend=false -input=false -lockfile=readonly >/dev/null
terraform validate

tflint --init >/dev/null
tflint
for module in network eks edge-alb edge identity; do
  tflint --chdir="../../modules/$module" --config="$config"
done

./check-alb-routes.py
