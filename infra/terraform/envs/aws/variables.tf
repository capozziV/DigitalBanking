variable "region" {
  type    = string
  default = "us-east-1"
}

variable "name" {
  type    = string
  default = "digitalbanking"
}

variable "web_callback_urls" {
  type    = list(string)
  default = ["http://localhost:4200/callback"]
}

variable "web_logout_urls" {
  type    = list(string)
  default = ["http://localhost:4200/"]
}

# Rede, EKS e ALB vem da etapa 8; aqui entram so como variaveis (sem valor padrao).
variable "alb_listener_arn" {
  description = "ARN do listener do ALB interno."
  type        = string
}

variable "subnet_ids" {
  description = "Subnets privadas do VPC Link."
  type        = list(string)
}

variable "security_group_ids" {
  description = "Security groups do VPC Link."
  type        = list(string)
}
