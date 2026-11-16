#!/usr/bin/env bash
# Imprime um access token do mock-oauth2-server (compose local). Uso:
#   token.sh user <sub-uuid> [admin]   token de usuario (cliente, ou admin com o grupo "admin")
#   token.sh service [client-id]       token de maquina (client credentials, escopo digitalbanking/internal)
# Variaveis: MOCK_OAUTH2_URL (padrao http://localhost:8090/default), TOKEN_CLIENT_ID e TOKEN_CLIENT_SECRET
# (clientes de usuario, padrao web/local-secret). O mock aceita qualquer segredo; sao so valores de desenvolvimento.
set -euo pipefail

issuer="${MOCK_OAUTH2_URL:-http://localhost:8090/default}"

usage() {
  echo "uso: token.sh user <sub-uuid> [admin] | token.sh service [client-id]" >&2
  exit 2
}

request() {
  local response
  response="$(curl -sS --fail-with-body "$@" "${issuer}/token")" || {
    echo "falha ao pedir token em ${issuer}/token: ${response:-sem resposta}" >&2
    exit 1
  }
  printf '%s' "${response}" | jq -er .access_token
}

command="${1:-}"
case "${command}" in
  user)
    sub="${2:-}"
    [[ -n "${sub}" ]] || usage
    args=(-u "${TOKEN_CLIENT_ID:-web}:${TOKEN_CLIENT_SECRET:-local-secret}"
      -d grant_type=password -d "username=${sub}" -d password=unused)
    if [[ "${3:-}" == "admin" ]]; then
      args+=(-d user_groups=admin)
    elif [[ -n "${3:-}" ]]; then
      usage
    fi
    request "${args[@]}"
    ;;
  service)
    client="${2:-${TOKEN_CLIENT_ID:-accounts}}"
    request -u "${client}:${TOKEN_CLIENT_SECRET:-local-secret}" \
      -d grant_type=client_credentials -d scope=digitalbanking/internal
    ;;
  *)
    usage
    ;;
esac
