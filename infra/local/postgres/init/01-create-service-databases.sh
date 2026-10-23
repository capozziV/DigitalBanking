#!/usr/bin/env bash
# Cria os roles e o banco de cada servico. Novos servicos acrescentam o seu bloco.
set -euo pipefail

# accounts: accounts_owner e dono do banco e das tabelas (Flyway);
# accounts_app so tem LOGIN, as permissoes nas tabelas vem da migration V5__app_grants.sql.
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-EOSQL
	CREATE ROLE accounts_owner WITH LOGIN PASSWORD '${ACCOUNTS_DB_OWNER_PASSWORD}';
	CREATE ROLE accounts_app WITH LOGIN PASSWORD '${ACCOUNTS_DB_PASSWORD}';
	CREATE DATABASE accounts OWNER accounts_owner;
EOSQL
