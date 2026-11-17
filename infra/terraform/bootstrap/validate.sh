#!/usr/bin/env bash
# Validacao sem credenciais: fmt, init sem backend, validate e conferencia da trust policy.
set -euo pipefail
cd "$(dirname "$0")"

terraform fmt -check -recursive
terraform init -backend=false -input=false >/dev/null
terraform validate

# A trust policy aceita so a main: toda linha de "sub" no HCL tem de ser ref:refs/heads/main.
subs=$(grep -n 'values *= *\["repo:' main.tf || true)
count=$(printf '%s\n' "$subs" | grep -c . || true)
if [ "$count" -ne 1 ] || ! printf '%s' "$subs" | grep -q ':ref:refs/heads/main"\]'; then
  echo "trust policy fora do esperado (so ref:refs/heads/main):" >&2
  printf '%s\n' "$subs" >&2
  exit 1
fi
if grep -nE 'StringLike|repo:[^"]*\*' main.tf; then
  echo "trust policy nao pode usar curinga" >&2
  exit 1
fi
echo "trust policy ok: so ref:refs/heads/main"
