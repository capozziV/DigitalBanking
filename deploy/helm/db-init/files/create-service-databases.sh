#!/usr/bin/env bash
# Cria, no RDS, o banco <servico>, o dono <servico>_owner (Flyway, dono do banco e das tabelas) e o usuario
# <servico>_app (so LOGIN; as permissoes nas tabelas vem das migrations), como
# infra/local/postgres/init/01-create-service-databases.sh faz no PostgreSQL local. Idempotente, e a senha dos
# roles e sempre a do segredo (rodar de novo depois de trocar o segredo atualiza o role).
# Entrada: SECRETS_DIR/master.json (segredo do RDS: username, password), SECRETS_DIR/<servico>-db.json
# (<SERVICO>_DB_OWNER_PASSWORD e <SERVICO>_DB_PASSWORD), SERVICES e a conexao em PGHOST, PGPORT e PGSSLMODE.
set -euo pipefail

# json_field <arquivo> <chave>: texto de uma chave de um objeto JSON plano. Aceita os escapes \" \\ e \/;
# recusa os demais (\u, \n...), que nem as senhas geradas pelo Terraform nem as do RDS usam.
json_field() {
  local json re raw out="" i=0 c n
  json="$(cat "$1")"
  re="\"$2\"[[:space:]]*:[[:space:]]*\"(([^\"\\\\]|\\\\.)*)\""
  if ! [[ $json =~ $re ]]; then
    echo "db-init: chave $2 ausente em $(basename "$1")" >&2
    return 1
  fi
  raw="${BASH_REMATCH[1]}"
  while ((i < ${#raw})); do
    c="${raw:i:1}"
    if [[ $c == "\\" ]]; then
      n="${raw:i+1:1}"
      case "$n" in
        '"' | '\' | '/') out+="$n" ;;
        *)
          echo "db-init: escape \\$n nao suportado em $2 ($(basename "$1"))" >&2
          return 1
          ;;
      esac
      i=$((i + 2))
    else
      out+="$c"
      i=$((i + 1))
    fi
  done
  if [[ -z $out ]]; then
    echo "db-init: chave $2 vazia em $(basename "$1")" >&2
    return 1
  fi
  printf '%s' "$out"
}

PGUSER="$(json_field "$SECRETS_DIR/master.json" username)"
PGPASSWORD="$(json_field "$SECRETS_DIR/master.json" password)"
export PGUSER PGPASSWORD PGDATABASE=postgres

for attempt in $(seq 1 30); do
  pg_isready --quiet && break
  echo "db-init: aguardando o PostgreSQL em $PGHOST:${PGPORT:-5432} ($attempt/30)"
  sleep 5
done

# create_service_db <servico>: as senhas chegam ao SQL por `psql -v` e sao lidas como :'nome' (quoting do psql),
# nunca interpoladas pelo shell.
create_service_db() {
  local service="$1" secret="$SECRETS_DIR/$1-db.json" upper owner_password app_password
  upper="$(printf '%s' "$service" | tr '[:lower:]' '[:upper:]')"
  owner_password="$(json_field "$secret" "${upper}_DB_OWNER_PASSWORD")"
  app_password="$(json_field "$secret" "${upper}_DB_PASSWORD")"
  psql -v ON_ERROR_STOP=1 --no-psqlrc --quiet \
    -v service="$service" \
    -v owner_password="$owner_password" \
    -v app_password="$app_password" <<-'EOSQL'
		SELECT format('CREATE ROLE %I WITH LOGIN', :'service' || '_owner')
		WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'service' || '_owner') \gexec
		SELECT format('CREATE ROLE %I WITH LOGIN', :'service' || '_app')
		WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'service' || '_app') \gexec
		SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'service' || '_owner', :'owner_password') \gexec
		SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'service' || '_app', :'app_password') \gexec
		-- No RDS o usuario mestre nao e superusuario: o PostgreSQL 16+ so deixa criar o banco com OWNER
		-- <servico>_owner se o mestre puder assumir esse role (SET ROLE). Quem cria o role ganha ADMIN sobre ele.
		SELECT format('GRANT %I TO CURRENT_USER WITH SET TRUE, INHERIT FALSE', :'service' || '_owner')
		WHERE NOT pg_has_role(current_user, :'service' || '_owner', 'SET') \gexec
		SELECT format('CREATE DATABASE %I OWNER %I', :'service', :'service' || '_owner')
		WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'service') \gexec
	EOSQL
  echo "db-init: banco $service, roles ${service}_owner e ${service}_app prontos"
}

for service in $SERVICES; do
  create_service_db "$service"
done
