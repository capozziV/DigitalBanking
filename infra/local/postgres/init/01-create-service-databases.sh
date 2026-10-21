#!/usr/bin/env bash
# Cria um role e um banco por servico. Novos servicos acrescentam o seu bloco.
set -euo pipefail

# accounts
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-EOSQL
	CREATE ROLE accounts WITH LOGIN PASSWORD '${ACCOUNTS_DB_PASSWORD}';
	CREATE DATABASE accounts OWNER accounts;
EOSQL
