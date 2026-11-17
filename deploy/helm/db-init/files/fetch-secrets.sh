#!/usr/bin/env bash
# Le do Secrets Manager, pelo Pod Identity da ServiceAccount db-init, a senha mestre do RDS e os segredos
# <SECRET_PREFIX>/<servico>-db, e os grava em SECRETS_DIR (emptyDir em memoria) para o create-service-databases.sh.
set -euo pipefail
umask 077

fetch() {
  aws secretsmanager get-secret-value --secret-id "$1" --query SecretString --output text > "$SECRETS_DIR/$2"
  echo "db-init: segredo $1 lido"
}

fetch "$MASTER_SECRET_ID" master.json
for service in $SERVICES; do
  fetch "$SECRET_PREFIX/$service-db" "$service-db.json"
done
