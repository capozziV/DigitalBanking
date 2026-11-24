output "log_group_arns" {
  description = "ARNs dos grupos de logs do coletor (servicos e metricas EMF), para o papel dele em modules/access."
  value       = concat([for g in aws_cloudwatch_log_group.service : g.arn], [aws_cloudwatch_log_group.metrics.arn])
}

output "dashboard_name" {
  description = "Nome do painel de transferencias no CloudWatch."
  value       = aws_cloudwatch_dashboard.transfers.dashboard_name
}
