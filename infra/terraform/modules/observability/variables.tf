variable "region" {
  description = "Regiao do ambiente; o Transaction Search, os grupos de logs e as metricas EMF ficam nela."
  type        = string
}

variable "services" {
  description = "Servicos que recebem um grupo de logs /<prefixo>/<servico>: os cinco Spring e o bff (o nome e o service.name que o coletor le)."
  type        = list(string)
  default     = ["accounts", "transfers", "notification", "statement", "customer", "bff"]
}

variable "log_group_prefix" {
  description = "Prefixo dos grupos de logs. O mesmo valor do logGroupPrefix do chart deploy/helm/otel-collector."
  type        = string
  default     = "/capzzpay"
}

variable "metrics_namespace" {
  description = "Namespace das metricas EMF no CloudWatch. O mesmo valor do metricsNamespace do chart deploy/helm/otel-collector."
  type        = string
  default     = "capzzpay"
}

variable "dashboard_name" {
  description = "Nome do painel (so letras, numeros, hifen e sublinhado; o CloudWatch recusa acento)."
  type        = string
  default     = "capzzpay-transferencias"
}
