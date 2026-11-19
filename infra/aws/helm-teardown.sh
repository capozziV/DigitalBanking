#!/usr/bin/env bash
# Passo "helm uninstall" do workflow aws-down: tira o web, o bff, os cinco servicos e o db-init do cluster e espera os
# TargetGroupBinding sairem (o controller ainda de pe tira os alvos; o destroy leva os target groups).
# Entrada (ambiente): AWS_REGION, CLUSTER_NAME, NAMESPACE.
# Nunca para no primeiro erro: percorre tudo, lista o que falhou e sai com 1 no fim. Quem chama (aws-down) roda o
# terraform destroy e o leftovers.sh de qualquer jeito: uma falha aqui nao pode deixar o ambiente cobrando.
# "Release ausente" (helm: "release: not found") e diferente de erro de acesso ao cluster: so o primeiro e ignorado.
set -uo pipefail

: "${AWS_REGION:?defina AWS_REGION}"
: "${CLUSTER_NAME:?defina CLUSTER_NAME}"
: "${NAMESPACE:?defina NAMESPACE}"

failed=()

if ! aws eks describe-cluster --region "$AWS_REGION" --name "$CLUSTER_NAME" > /dev/null 2>&1; then
  echo "cluster $CLUSTER_NAME nao existe: nada a desinstalar"
  exit 0
fi
if ! aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"; then
  echo "sem kubeconfig para $CLUSTER_NAME" >&2
  exit 1
fi
# Falha alto se o cluster nao responde: sem isso, "nao achei nada" e "nao consegui olhar" pareceriam iguais.
if ! kubectl auth can-i list targetgroupbindings.elbv2.k8s.aws -A > /dev/null; then
  echo "sem acesso ao cluster (kubectl auth can-i falhou)" >&2
  failed+=("acesso ao cluster")
fi

for release in web bff statement notification transfers accounts customer db-init; do
  if status=$(helm status "$release" -n "$NAMESPACE" 2>&1); then
    helm uninstall "$release" -n "$NAMESPACE" --wait --timeout 10m || failed+=("helm uninstall $release")
  elif [[ "$status" == *"release: not found"* ]]; then
    echo "release $release nao instalado"
  else
    echo "helm status $release falhou: $status" >&2
    failed+=("helm status $release")
  fi
done

# O Job do db-init e hook do Helm e continua no cluster depois do uninstall: sai aqui, explicitamente.
kubectl -n "$NAMESPACE" delete job db-init --ignore-not-found --wait=true || failed+=("delete job db-init")

left=""
for _ in $(seq 1 "${TGB_WAIT_ATTEMPTS:-30}"); do
  if ! left=$(kubectl get targetgroupbindings.elbv2.k8s.aws -A --no-headers 2>&1); then
    echo "kubectl get targetgroupbindings falhou: $left" >&2
    failed+=("listar TargetGroupBinding")
    left=""
    break
  fi
  [ -n "$left" ] || break
  echo "aguardando TargetGroupBinding sair: $left"
  sleep "${TGB_WAIT_SLEEP:-10}"
done
if [ -n "$left" ]; then
  echo "TargetGroupBinding ainda presentes: $left" >&2
  failed+=("TargetGroupBinding presentes")
fi

if [ "${#failed[@]}" -gt 0 ]; then
  echo "helm-teardown com falhas: ${failed[*]}" >&2
  exit 1
fi
