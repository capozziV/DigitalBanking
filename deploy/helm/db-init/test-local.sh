#!/usr/bin/env bash
# Testa o create-service-databases.sh do Job db-init contra um PostgreSQL 17 em Docker, com o usuario mestre
# sem superusuario (CREATEROLE e CREATEDB, como o mestre do RDS) e senha com aspas, barras e apostrofo.
# Roda o script duas vezes (idempotencia), troca uma senha no segredo (o role acompanha) e confere que o dono
# cria tabela no proprio banco e o usuario de aplicacao entra. Precisa de Docker; nao toca na AWS.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
image="$(awk '/^  postgres:/{print $2}' "$here/values.yaml")"
net="db-init-test-$$"
server="db-init-pg-$$"
work="$(mktemp -d)"
cleanup() {
  docker rm -f "$server" >/dev/null 2>&1 || true
  docker network rm "$net" >/dev/null 2>&1 || true
  rm -rf "$work"
}
trap cleanup EXIT

services="accounts transfers notification customer"
master_password='p\a"b/c'"'"'d'
docker network create "$net" >/dev/null
docker run -d --name "$server" --network "$net" -e POSTGRES_PASSWORD=postgres "$image" >/dev/null
until docker exec "$server" pg_isready -h 127.0.0.1 -U postgres -q; do sleep 1; done
sleep 2
until docker exec "$server" pg_isready -h 127.0.0.1 -U postgres -q; do sleep 1; done
docker exec -i "$server" psql -h 127.0.0.1 -U postgres -v ON_ERROR_STOP=1 -q -v pw="$master_password" <<'SQL'
SELECT format('CREATE ROLE dbadmin WITH LOGIN CREATEROLE CREATEDB PASSWORD %L', :'pw') \gexec
SQL

# Segredos no formato do Secrets Manager: o do RDS (username/password) e os <servico>-db do Terraform.
mkdir -p "$work/secrets"
printf '{"username":"dbadmin","password":"p\\\\a\\"b\\/c'"'"'d","engine":"postgres"}' > "$work/secrets/master.json"
write_secret() {
  local upper
  upper="$(printf '%s' "$1" | tr '[:lower:]' '[:upper:]')"
  printf '{"%s_DB_PASSWORD":"%s","%s_DB_OWNER_PASSWORD":"%s"}' "$upper" "$2" "$upper" "$3" > "$work/secrets/$1-db.json"
}
for s in $services; do write_secret "$s" "${s}App1" "${s}Owner1"; done
chmod -R a+rX "$work"

run_job() {
  docker run --rm --network "$net" --user 10001:10001 --read-only --tmpfs /tmp \
    -v "$work/secrets:/secrets:ro" -v "$here/files:/scripts:ro" \
    -e PGHOST="$server" -e PGPORT=5432 -e PGSSLMODE=disable -e SERVICES="$services" -e SECRETS_DIR=/secrets -e HOME=/tmp \
    "$image" bash /scripts/create-service-databases.sh
}
as() { # as <usuario> <senha> <banco> <sql>
  docker run --rm --network "$net" -e PGPASSWORD="$2" "$image" \
    psql -h "$server" -U "$1" -d "$3" -v ON_ERROR_STOP=1 -tAqc "$4"
}

echo "== 1a execucao"; run_job
echo "== 2a execucao (idempotente)"; run_job

for s in $services; do
  owner="$(as postgres postgres postgres "SELECT pg_get_userbyid(datdba) FROM pg_database WHERE datname = '$s'")"
  [[ $owner == "${s}_owner" ]] || { echo "ERRO: banco $s com dono '$owner'" >&2; exit 1; }
  as "${s}_owner" "${s}Owner1" "$s" "CREATE TABLE probe (id int); DROP TABLE probe;" >/dev/null
  [[ "$(as "${s}_app" "${s}App1" "$s" "SELECT current_user")" == "${s}_app" ]] || { echo "ERRO: ${s}_app nao entra" >&2; exit 1; }
  echo "  ok  $s: dono ${s}_owner cria tabela, ${s}_app entra"
done

echo "== senha trocada no segredo"
write_secret accounts accountsApp2 accountsOwner1
run_job
[[ "$(as accounts_app accountsApp2 accounts "SELECT 1")" == 1 ]] || { echo "ERRO: senha nova do accounts_app nao vale" >&2; exit 1; }
if as accounts_app accountsApp1 accounts "SELECT 1" >/dev/null 2>&1; then echo "ERRO: senha antiga ainda vale" >&2; exit 1; fi
echo "  ok  accounts_app com a senha nova"

echo "== segredo sem a chave do dono falha"
printf '{"CUSTOMER_DB_PASSWORD":"x"}' > "$work/secrets/customer-db.json"
if run_job >/dev/null 2>&1; then echo "ERRO: o Job passou sem CUSTOMER_DB_OWNER_PASSWORD" >&2; exit 1; fi
echo "  ok  recusado"

echo "test-local: ok"
