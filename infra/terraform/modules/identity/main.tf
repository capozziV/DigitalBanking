resource "aws_cognito_user_pool" "this" {
  name                     = "${var.name}-users"
  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]
  deletion_protection      = "INACTIVE" # ambiente efemero, destruido por terraform destroy

  # Senha de 6 digitos de proposito: banco simulado, sem dinheiro nem dados reais (spec da etapa 9). O Cognito
  # exige minimo 6; a regra de "so digitos" e do BFF e da tela. O bloqueio por tentativas erradas do Cognito
  # segue ligado (nao ha como desligar).
  password_policy {
    minimum_length    = 6
    require_lowercase = false
    require_uppercase = false
    require_numbers   = false
    require_symbols   = false
  }

  # Autocadastro pela API (SignUp), com codigo por e-mail. E-mail padrao do Cognito (sem SES): limite diario baixo,
  # suficiente para a demonstracao.
  admin_create_user_config {
    allow_admin_create_user_only = false
  }

  email_configuration {
    email_sending_account = "COGNITO_DEFAULT"
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

# App client do BFF: login pela API (USER_PASSWORD_AUTH) e refresh, sem segredo e sem OAuth. O client web nao muda.
resource "aws_cognito_user_pool_client" "bff" {
  name         = "${var.name}-bff"
  user_pool_id = aws_cognito_user_pool.this.id

  generate_secret               = false
  explicit_auth_flows           = ["ALLOW_USER_PASSWORD_AUTH", "ALLOW_REFRESH_TOKEN_AUTH"]
  prevent_user_existence_errors = "ENABLED"
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
