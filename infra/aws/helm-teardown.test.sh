#!/usr/bin/env bash
# Testes do helm-teardown.sh sem AWS nem cluster: aws, helm, kubectl e sleep falsos no PATH.
# Roda no CI (job aws-scripts) e localmente: infra/aws/helm-teardown.test.sh
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
failures=0

cat > "$tmp/aws" <<'FAKE'
#!/usr/bin/env bash
[ "$2" != describe-cluster ] || [ -z "${FAKE_NO_CLUSTER:-}" ] || exit 254
exit 0
FAKE
printf '#!/usr/bin/env bash\nexit 0\n' > "$tmp/sleep"
cat > "$tmp/helm" <<'FAKE'
#!/usr/bin/env bash
echo "helm $*" >> "$FAKE_LOG"
case "$1" in
  status)
    [ "$2" != "${FAKE_STATUS_ERROR:-}" ] || { echo "Error: Kubernetes cluster unreachable" >&2; exit 1; }
    [ "$2" != "${FAKE_ABSENT:-}" ] || { echo "Error: release: not found" >&2; exit 1; } ;;
  uninstall) [ "$2" != "${FAKE_UNINSTALL_FAIL:-}" ] || { echo "timed out waiting" >&2; exit 1; } ;;
esac
FAKE
cat > "$tmp/kubectl" <<'FAKE'
#!/usr/bin/env bash
echo "kubectl $*" >> "$FAKE_LOG"
case "$1" in
  auth) [ -z "${FAKE_NO_ACCESS:-}" ] || exit 1 ;;
  get) [ -z "${FAKE_TGB_ERROR:-}" ] || { echo "connection refused" >&2; exit 1; }
       [ -z "${FAKE_TGB_LEFT:-}" ] || echo "digitalbanking tgb-accounts" ;;
esac
FAKE
chmod +x "$tmp"/aws "$tmp"/sleep "$tmp"/helm "$tmp"/kubectl

# Uso: expect <exit-esperado> <descricao> [VAR=valor...]; confere tambem os uninstalls feitos em FAKE_LOG.
expect() {
  local expected_status="$1" description="$2" status=0
  shift 2
  : > "$tmp/log"
  env PATH="$tmp:$PATH" FAKE_LOG="$tmp/log" AWS_REGION=sa-east-1 CLUSTER_NAME=c NAMESPACE=digitalbanking \
    TGB_WAIT_ATTEMPTS=2 "$@" "$here/helm-teardown.sh" > "$tmp/out" 2>&1 || status=$?
  if [ "$status" -ne "$expected_status" ]; then
    echo "FALHOU: $description (exit $status, esperado $expected_status)"
    sed 's/^/    /' "$tmp/out"
    failures=$((failures + 1))
  fi
}
uninstalls() { grep -c '^helm uninstall' "$tmp/log" || true; }
check_count() {
  local expected="$1" description="$2" actual
  actual=$(uninstalls)
  [ "$actual" = "$expected" ] || { echo "FALHOU: $description ($actual uninstalls, esperado $expected)"; failures=$((failures + 1)); }
}

expect 0 "tudo certo"
check_count 8 "tudo certo desinstala os oito"
expect 0 "cluster inexistente" FAKE_NO_CLUSTER=1
check_count 0 "cluster inexistente nao desinstala"
expect 0 "release ausente e ignorado" FAKE_ABSENT=customer
check_count 7 "release ausente pula so ele"
expect 1 "uninstall que falha da exit 1" FAKE_UNINSTALL_FAIL=transfers
check_count 8 "uninstall que falha nao impede os seguintes"
grep -q "delete job db-init" "$tmp/log" || { echo "FALHOU: delete job db-init nao rodou depois da falha"; failures=$((failures + 1)); }
expect 1 "erro de acesso no helm status nao vira release ausente" FAKE_STATUS_ERROR=accounts
check_count 7 "erro no status pula so o release"
expect 1 "sem acesso ao cluster" FAKE_NO_ACCESS=1
expect 1 "TargetGroupBinding que nao sai" FAKE_TGB_LEFT=1
expect 1 "erro ao listar TargetGroupBinding" FAKE_TGB_ERROR=1

if [ "$failures" -ne 0 ]; then
  echo "$failures falha(s)"
  exit 1
fi
echo "helm-teardown.test: ok"
