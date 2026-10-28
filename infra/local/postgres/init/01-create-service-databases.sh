#!/usr/bin/env bash
# Cria os roles e o banco de cada servico. Idempotente: pode rodar de novo sem erro.
# Novo servico: acrescente uma linha `create_service_db <servico> <VAR_SENHA_DONO> <VAR_SENHA_APP>`
# no fim deste arquivo e as duas variaveis no docker-compose.yml e no .env.example.
set -euo pipefail

# create_service_db <service> <owner_password_var> <app_password_var>
# Cria <service>_owner (dono do banco e das tabelas, usado pelo Flyway), <service>_app (so LOGIN;
# as permissoes nas tabelas vem das migrations do servico) e o banco <service>, se ainda nao existirem.
# Os argumentos sao os NOMES das variaveis de ambiente com as senhas. As senhas chegam ao SQL
# por `psql -v` e sao lidas como :'nome' (quoting do psql), nunca interpoladas pelo shell.
create_service_db() {
	local service="$1" owner_password_var="$2" app_password_var="$3"
	psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER:-postgres}" --dbname postgres \
		-v service="$service" \
		-v owner_password="${!owner_password_var}" \
		-v app_password="${!app_password_var}" <<-'EOSQL'
		SELECT format('CREATE ROLE %I WITH LOGIN PASSWORD %L', :'service' || '_owner', :'owner_password')
		WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'service' || '_owner') \gexec
		SELECT format('CREATE ROLE %I WITH LOGIN PASSWORD %L', :'service' || '_app', :'app_password')
		WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'service' || '_app') \gexec
		SELECT format('CREATE DATABASE %I OWNER %I', :'service', :'service' || '_owner')
		WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'service') \gexec
	EOSQL
}

create_service_db accounts ACCOUNTS_DB_OWNER_PASSWORD ACCOUNTS_DB_PASSWORD
create_service_db transfers TRANSFERS_DB_OWNER_PASSWORD TRANSFERS_DB_PASSWORD
