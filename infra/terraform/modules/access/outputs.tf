output "service_role_arns" {
  description = "Mapa servico -> ARN do papel (Pod Identity)."
  value       = { for k, v in aws_iam_role.service : k => v.arn }
}
