variable "name" {
  description = "Prefixo dos recursos e dos nomes de segredo."
  type        = string
}

variable "vpc_id" {
  type = string
}

variable "subnet_ids" {
  description = "Sub-redes privadas do RDS e do ElastiCache."
  type        = list(string)
}

variable "client_security_group_id" {
  description = "Security group dos nos e pods (cluster do EKS), unico com acesso ao RDS e ao Redis."
  type        = string
}

variable "database_services" {
  description = "Servicos com banco proprio no RDS (um banco, um dono e um usuario de aplicacao por servico)."
  type        = list(string)
  default     = ["accounts", "transfers", "notification", "customer"]
}

variable "oauth_services" {
  description = "Servicos que pedem token de maquina (client credentials) ao Cognito."
  type        = list(string)
  default     = ["accounts", "transfers"]
}

variable "machine_client_secret" {
  description = "Segredo do app client de maquina do Cognito."
  type        = string
  sensitive   = true
}
