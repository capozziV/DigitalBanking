variable "name" {
  description = "Prefixo dos nomes dos recursos de rede."
  type        = string
}

variable "cidr_block" {
  description = "Faixa da VPC."
  type        = string
  default     = "10.20.0.0/16"
}

variable "az_count" {
  description = "Zonas de disponibilidade usadas (o EKS e o ALB exigem pelo menos duas)."
  type        = number
  default     = 2

  validation {
    condition     = var.az_count >= 2
    error_message = "O EKS e o ALB exigem sub-redes em pelo menos duas zonas."
  }
}
