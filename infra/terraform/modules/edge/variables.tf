variable "name" {
  description = "Prefixo dos nomes dos recursos da borda."
  type        = string
}

variable "issuer_url" {
  description = "Emissor JWT (Cognito) validado pelo autorizador."
  type        = string
}

variable "audiences" {
  description = "App clients aceitos pelo autorizador JWT."
  type        = list(string)
}

variable "alb_listener_arn" {
  description = "ARN do listener do ALB interno (etapa 8)."
  type        = string
}

variable "subnet_ids" {
  description = "Subnets privadas do VPC Link (etapa 8)."
  type        = list(string)
}

variable "security_group_ids" {
  description = "Security groups do VPC Link (etapa 8)."
  type        = list(string)
}

variable "route_prefixes" {
  description = "Prefixos de caminho roteados ao ALB; o statement e o limite diario (GET /accounts/{id}/statement e /daily-limit) ficam sob /accounts."
  type        = list(string)
  default     = ["/accounts", "/transfers", "/notifications", "/customers", "/admin"]
}

variable "public_route_prefixes" {
  description = "Prefixos sem autorizador JWT: o BFF (/bff) autentica pelo cookie de sessao, nao pelo cabecalho Authorization."
  type        = list(string)
  default     = ["/bff"]
}

variable "tags" {
  description = "Tags aplicadas aos recursos."
  type        = map(string)
  default     = {}
}

variable "throttling_rate_limit" {
  description = "Requisicoes por segundo permitidas no stage (padrao baixo: ambiente de portfolio)."
  type        = number
  default     = 10
}

variable "throttling_burst_limit" {
  description = "Rajada maxima de requisicoes no stage."
  type        = number
  default     = 20
}

variable "app_throttling_rate_limit" {
  description = "Requisicoes por segundo nas rotas do app ($default e /bff). Conta: 10 visitantes simultaneos x ~12 requisicoes de carga (index, JS, CSS, chunks, /bff), mais o polling de 1 req/s por transferencia e o do sininho (30 s), dao ~15 req/s sustentados; 50 deixa folga."
  type        = number
  default     = 50
}

variable "app_throttling_burst_limit" {
  description = "Rajada maxima nas rotas do app. Conta: 10 visitantes abrindo o app juntos fazem ~120 requisicoes de carga na mesma rajada; 150 cobre com folga."
  type        = number
  default     = 150
}

variable "access_log_retention_days" {
  description = "Retencao do log de acesso no CloudWatch."
  type        = number
  default     = 7
}
