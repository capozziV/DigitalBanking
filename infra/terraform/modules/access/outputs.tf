output "service_role_arns" {
  description = "Mapa servico -> ARN do papel (Pod Identity)."
  value       = { for k, v in aws_iam_role.service : k => v.arn }
}

output "db_init_role_arn" {
  description = "ARN do papel do Job db-init (Pod Identity)."
  value       = aws_iam_role.db_init.arn
}

output "collector_role_arn" {
  description = "ARN do papel do coletor ADOT (Pod Identity)."
  value       = aws_iam_role.collector.arn
}
