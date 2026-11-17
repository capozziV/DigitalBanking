variable "region" {
  description = "Regiao da AWS."
  type        = string
  default     = "sa-east-1"
}

variable "github_repository" {
  description = "Repositorio do GitHub (dono/nome) autorizado a assumir o papel."
  type        = string
  default     = "capozziV/DigitalBanking"

  validation {
    condition     = can(regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$", var.github_repository))
    error_message = "Use dono/nome exato, sem curinga (* ou ?)."
  }
}

variable "services" {
  description = "Servicos que ganham um repositorio ECR."
  type        = list(string)
  default     = ["accounts", "transfers", "notification", "statement", "customer"]
}

variable "monthly_budget_usd" {
  description = "Orcamento mensal em dolares."
  type        = number
  default     = 20
}

variable "budget_alert_email" {
  description = "E-mail que recebe os alertas do orcamento. Sem padrao: informe fora do git (TF_VAR_budget_alert_email)."
  type        = string
  sensitive   = true
}
