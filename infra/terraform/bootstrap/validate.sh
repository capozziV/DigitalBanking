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
# Deny do papel: tem de existir e cobrir budgets, o proprio papel/OIDC e o bucket do estado.
for needle in '"budgets:\*"' '"iam:\*"' 's3:DeleteBucket\*' 's3:PutBucketPolicy' 'aws_iam_role_policy_attachment" "github_guardrails"' 'local.guardrails_policy_arn,'; do
  if ! grep -q "$needle" main.tf; then
    echo "deny do papel do GitHub incompleto: falta $needle" >&2
    exit 1
  fi
done
# Repositorio sem curinga (a validation {} cobre o plan; aqui pega o default no HCL).
if grep -A3 'variable "github_repository"' variables.tf | grep -E 'default.*[*?]'; then
  echo "github_repository com curinga" >&2
  exit 1
fi
echo "trust policy ok: so ref:refs/heads/main"
