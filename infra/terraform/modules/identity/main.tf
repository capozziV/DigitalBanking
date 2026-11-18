resource "aws_cognito_user_pool" "this" {
  name                     = "${var.name}-users"
  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]
  deletion_protection      = "INACTIVE" # ambiente efemero, destruido por terraform destroy

  password_policy {
    minimum_length    = 12
    require_lowercase = true
    require_uppercase = true
    require_numbers   = true
    require_symbols   = true
  }

  tags = var.tags
}

resource "aws_cognito_user_group" "admin" {
  name         = "admin"
  user_pool_id = aws_cognito_user_pool.this.id
  description  = "Operadores do banco; o grupo vai na claim cognito:groups do token."
}

# Escopo `digitalbanking/internal`: so o app client de maquina o recebe.
resource "aws_cognito_resource_server" "digitalbanking" {
  identifier   = "digitalbanking"
  name         = "digitalbanking"
  user_pool_id = aws_cognito_user_pool.this.id

  scope {
    scope_name        = "internal"
    scope_description = "Chamadas internas entre servicos"
  }
}

# App client do frontend: authorization code com PKCE, sem segredo (cliente publico).
resource "aws_cognito_user_pool_client" "web" {
  name         = "${var.name}-web"
  user_pool_id = aws_cognito_user_pool.this.id

  generate_secret                      = false
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  allowed_oauth_scopes                 = ["openid", "email", "profile"]
  supported_identity_providers         = ["COGNITO"]
  callback_urls                        = var.web_callback_urls
  logout_urls                          = var.web_logout_urls
  explicit_auth_flows                  = ["ALLOW_REFRESH_TOKEN_AUTH"]
  prevent_user_existence_errors        = "ENABLED"
}

# App client de maquina: client credentials com o escopo internal. O segredo fica no estado do Terraform
# e na saida sensivel; nada vai para o repositorio.
resource "aws_cognito_user_pool_client" "machine" {
  name         = "${var.name}-machine"
  user_pool_id = aws_cognito_user_pool.this.id

  generate_secret                      = true
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["client_credentials"]
  allowed_oauth_scopes                 = ["${aws_cognito_resource_server.digitalbanking.identifier}/internal"]
  supported_identity_providers         = ["COGNITO"]
}

# App client de operacao: so o fluxo ADMIN_USER_PASSWORD_AUTH, que o seed e o smoke do aws-up usam
# (AdminInitiateAuth exige credenciais IAM do papel do GitHub; o fluxo nao fica aberto ao publico).
# Sem segredo e sem OAuth; o app client web continua sem esse fluxo.
resource "aws_cognito_user_pool_client" "ops" {
  name         = "${var.name}-ops"
  user_pool_id = aws_cognito_user_pool.this.id

  generate_secret               = false
  explicit_auth_flows           = ["ALLOW_ADMIN_USER_PASSWORD_AUTH", "ALLOW_REFRESH_TOKEN_AUTH"]
  prevent_user_existence_errors = "ENABLED"
  access_token_validity         = 1
  token_validity_units {
    access_token = "hours"
  }
}

# Sem dominio nao existe /oauth2/token e o client_credentials nao funciona.
# O prefixo precisa ser unico na regiao.
resource "aws_cognito_user_pool_domain" "this" {
  domain       = var.domain_prefix
  user_pool_id = aws_cognito_user_pool.this.id
}

data "aws_region" "current" {}
