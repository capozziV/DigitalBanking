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
  description = "Prefixos de caminho roteados ao ALB; o statement e o limite diario ficam sob /accounts e /transfers."
  type        = list(string)
  default     = ["/accounts", "/transfers", "/notifications", "/customers", "/admin"]
}

variable "tags" {
  description = "Tags aplicadas aos recursos."
  type        = map(string)
  default     = {}
}
