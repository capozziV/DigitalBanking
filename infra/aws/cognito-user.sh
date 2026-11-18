#!/usr/bin/env bash
# Usuarios do Cognito para o seed e o smoke na AWS (workflow aws-up), com token por AdminInitiateAuth
# (ADMIN_USER_PASSWORD_AUTH no app client de operacao, saida ops_client_id do Terraform). O fluxo exige credenciais
# IAM (o papel do GitHub) e nao fica aberto ao publico.
#   cognito-user.sh token <email> [admin]   cria o usuario se faltar, fixa a senha (permanente), poe no grupo admin
#                                           se pedido e imprime "<sub> <access-token>"
#   cognito-user.sh delete <email>          apaga o usuario (ausente nao e erro)
#   cognito-user.sh purge <prefixo>         apaga todo usuario cujo e-mail comeca pelo prefixo (ex.: smoke-)
# Variaveis: COGNITO_USER_POOL_ID, COGNITO_CLIENT_ID (ops_client_id), AWS_REGION; COGNITO_USER_PASSWORD (opcional,
# senha do dono): sem ela, a senha e aleatoria, gerada aqui e nunca impressa nem guardada.
# Senha e token nunca vao na linha de comando do aws CLI: seguem por --cli-input-json na entrada padrao.
set -euo pipefail

: "${COGNITO_USER_POOL_ID:?defina COGNITO_USER_POOL_ID (saida user_pool_id do Terraform)}"
: "${AWS_REGION:?defina AWS_REGION}"
pool="$COGNITO_USER_POOL_ID"

usage() {
  echo "uso: cognito-user.sh token <email> [admin] | delete <email> | purge <prefixo>" >&2
  exit 2
}

idp() { aws cognito-idp --region "$AWS_REGION" "$@"; }

# Senha aleatoria que cumpre a politica do pool (12+ caracteres, maiuscula, minuscula, numero e simbolo).
random_password() {
  printf '%sAa1!' "$(openssl rand -base64 24 | tr -d '/+=\n')"
}

command="${1:-}"
case "$command" in
  token)
    email="${2:-}"
    [ -n "$email" ] || usage
    group="${3:-}"
    [ -z "$group" ] || [ "$group" = admin ] || usage
    : "${COGNITO_CLIENT_ID:?defina COGNITO_CLIENT_ID (saida ops_client_id do Terraform)}"
    password="${COGNITO_USER_PASSWORD:-$(random_password)}"

    # Sem convite por e-mail (SUPPRESS): o usuario ja nasce com o e-mail verificado e a senha vem a seguir.
    if ! error=$(idp admin-create-user --user-pool-id "$pool" --username "$email" --message-action SUPPRESS \
      --user-attributes Name=email,Value="$email" Name=email_verified,Value=true 2>&1 >/dev/null); then
      case "$error" in
        *UsernameExistsException*) ;;
        *) echo "$error" >&2; exit 1 ;;
      esac
    fi
    jq -n --arg pool "$pool" --arg user "$email" --arg password "$password" \
      '{UserPoolId: $pool, Username: $user, Password: $password, Permanent: true}' |
      idp admin-set-user-password --cli-input-json file:///dev/stdin
    if [ "$group" = admin ]; then
      idp admin-add-user-to-group --user-pool-id "$pool" --username "$email" --group-name admin
    fi

    token=$(jq -n --arg pool "$pool" --arg client "$COGNITO_CLIENT_ID" --arg user "$email" --arg password "$password" \
      '{UserPoolId: $pool, ClientId: $client, AuthFlow: "ADMIN_USER_PASSWORD_AUTH",
        AuthParameters: {USERNAME: $user, PASSWORD: $password}}' |
      idp admin-initiate-auth --cli-input-json file:///dev/stdin --query AuthenticationResult.AccessToken --output text)
    if [ -z "$token" ] || [ "$token" = None ]; then
      echo "AdminInitiateAuth sem AccessToken para $email (desafio pendente?)" >&2
      exit 1
    fi
    sub=$(idp admin-get-user --user-pool-id "$pool" --username "$email" \
      --query "UserAttributes[?Name=='sub'].Value | [0]" --output text)
    # O token so sai na saida padrao, para quem chamou (o seed e o smoke nunca o imprimem); vale 1 h.
    echo "$sub $token"
    ;;
  delete)
    email="${2:-}"
    [ -n "$email" ] || usage
    if ! error=$(idp admin-delete-user --user-pool-id "$pool" --username "$email" 2>&1); then
      case "$error" in
        *UserNotFoundException*) ;;
        *) echo "$error" >&2; exit 1 ;;
      esac
    fi
    ;;
  purge)
    prefix="${2:-}"
    [ -n "$prefix" ] || usage
    # O filtro "^=" do ListUsers e por prefixo; o aws CLI pagina sozinho.
    idp list-users --user-pool-id "$pool" --filter "email ^= \"$prefix\"" \
      --query "Users[].Attributes[?Name=='email'].Value[]" --output text | tr '\t' '\n' |
      while read -r email; do
        [ -n "$email" ] || continue
        "$0" delete "$email"
        echo "apagado: $email"
      done
    ;;
  *)
    usage
    ;;
esac
