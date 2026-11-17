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
  description = "Requisicoes por segundo permitidas no stage."
  type        = number
  default     = 10
}

variable "throttling_burst_limit" {
  description = "Rajada maxima de requisicoes no stage."
  type        = number
  default     = 20
}
