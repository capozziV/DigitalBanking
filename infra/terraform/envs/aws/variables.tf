variable "region" {
  type    = string
  default = "sa-east-1"
}

variable "name" {
  type    = string
  default = "digitalbanking"
}

variable "created_at" {
  description = "Momento da subida do ambiente (RFC 3339), etiqueta created-at lida pelo reaper. Sem padrao: vem do workflow."
  type        = string

  validation {
    condition     = can(formatdate("YYYY", var.created_at))
    error_message = "created_at precisa estar em RFC 3339, por exemplo 2026-11-16T22:00:00Z."
  }
}

variable "kubernetes_version" {
  description = "Versao do Kubernetes do EKS; precisa estar em suporte padrao."
  type        = string
  default     = "1.36"
}

variable "web_callback_urls" {
  type    = list(string)
  default = ["http://localhost:4200/callback"]
}

variable "web_logout_urls" {
  type    = list(string)
  default = ["http://localhost:4200/"]
}

variable "cognito_domain_prefix" {
  description = "Prefixo do dominio do Cognito; precisa ser unico na regiao (sem padrao)."
  type        = string
}

variable "throttling_rate_limit" {
  description = "Requisicoes por segundo permitidas nas rotas da API (com JWT)."
  type        = number
  default     = 10
}

variable "throttling_burst_limit" {
  description = "Rajada maxima de requisicoes nas rotas da API."
  type        = number
  default     = 20
}

variable "app_throttling_rate_limit" {
  description = "Requisicoes por segundo nas rotas do app ($default e /bff). Conta: 10 visitantes simultaneos x ~12 requisicoes de carga (index, JS, CSS, chunks, /bff), mais o polling de 1 req/s por transferencia e o do sininho (30 s), dao ~15 req/s sustentados; 50 deixa folga. As rotas da API seguem em throttling_rate_limit."
  type        = number
  default     = 50
}

variable "app_throttling_burst_limit" {
  description = "Rajada maxima nas rotas do app. Conta: 10 visitantes abrindo o app juntos fazem ~120 requisicoes de carga na mesma rajada; 150 cobre com folga."
  type        = number
  default     = 150
}

variable "kubernetes_namespace" {
  description = "Namespace do release do Helm; o Pod Identity associa as ServiceAccounts deste namespace aos papeis. Fixo em digitalbanking: infra/aws/deploy-services.sh e o aws-down.yml usam o mesmo literal."
  type        = string
  default     = "digitalbanking"

  validation {
    condition     = var.kubernetes_namespace == "digitalbanking"
    error_message = "O namespace e fixo em digitalbanking (deploy-services.sh e aws-down.yml); mude os tres juntos."
  }
}
