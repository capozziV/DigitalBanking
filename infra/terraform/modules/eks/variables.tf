variable "name" {
  description = "Nome do cluster e prefixo dos papeis IAM."
  type        = string
}

variable "region" {
  description = "Regiao do cluster (passada ao AWS Load Balancer Controller, que nao consulta o IMDS)."
  type        = string
}

variable "kubernetes_version" {
  description = "Versao do Kubernetes; precisa estar em suporte padrao (suporte estendido custa a mais por hora)."
  type        = string
  default     = "1.36"
}

variable "vpc_id" {
  description = "VPC do cluster."
  type        = string
}

variable "subnet_ids" {
  description = "Sub-redes privadas do plano de controle e dos nos."
  type        = list(string)
}

variable "node_instance_type" {
  description = "Tipo de instancia do node group gerenciado."
  type        = string
  default     = "t3.medium"
}

variable "node_count" {
  description = "Quantidade fixa de nos (minimo = maximo = desejado)."
  type        = number
  default     = 2
}

variable "target_group_arns" {
  description = "Target groups em que o AWS Load Balancer Controller pode registrar e tirar pods."
  type        = list(string)
}

variable "lb_controller_chart_version" {
  description = "Versao do chart aws-load-balancer-controller (repositorio eks-charts)."
  type        = string
  default     = "3.6.2"
}
