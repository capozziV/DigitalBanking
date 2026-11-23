variable "name" {
  description = "Prefixo dos nomes do ALB, dos target groups e dos security groups."
  type        = string
}

variable "vpc_id" {
  description = "VPC do ambiente."
  type        = string
}

variable "subnet_ids" {
  description = "Sub-redes privadas do ALB interno (duas zonas)."
  type        = list(string)
}
