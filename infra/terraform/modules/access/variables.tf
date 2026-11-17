variable "name" {
  description = "Prefixo dos papeis IAM."
  type        = string
}

variable "cluster_name" {
  description = "Cluster EKS das associacoes do Pod Identity."
  type        = string
}

variable "namespace" {
  description = "Namespace do release do Helm; a associacao vale so para as ServiceAccounts deste namespace."
  type        = string
}

variable "topic_arns" {
  description = "Mapa nome do topico -> ARN."
  type        = map(string)
}

variable "queue_arns" {
  description = "Mapa nome da fila (incluindo DLQs) -> ARN."
  type        = map(string)
}

variable "table_arns" {
  description = "Mapa nome da tabela DynamoDB -> ARN."
  type        = map(string)
}

variable "db_init_secret_arns" {
  description = "ARNs que o Job db-init le: o segredo da senha mestre do RDS e os <name>/<servico>-db."
  type        = list(string)
}

variable "db_init_service_account" {
  description = "ServiceAccount do Job db-init (chart deploy/helm/db-init), no namespace do release."
  type        = string
  default     = "db-init"
}
