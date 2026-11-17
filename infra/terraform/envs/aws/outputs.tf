output "edge_url" {
  description = "URL publica da borda (API Gateway HTTP API)."
  value       = module.edge.api_endpoint
}

output "created_at" {
  description = "Etiqueta created-at aplicada a todos os recursos do ambiente."
  value       = var.created_at
}

output "vpc_id" {
  value = module.network.vpc_id
}

output "private_subnet_ids" {
  value = module.network.private_subnet_ids
}

output "cluster_name" {
  value = module.eks.cluster_name
}

output "cluster_security_group_id" {
  description = "Security group dos nos e pods do EKS (a Task 5 libera RDS e Redis a partir dele)."
  value       = module.eks.cluster_security_group_id
}

output "target_group_arns" {
  description = "Mapa servico -> ARN do target group, para o targetGroupArn do chart Helm."
  value       = module.edge_alb.target_group_arns
}

output "alb_listener_arn" {
  value = module.edge_alb.listener_arn
}

output "issuer_url" {
  value = module.identity.issuer_url
}

output "web_client_id" {
  value = module.identity.web_client_id
}

output "machine_client_id" {
  value = module.identity.machine_client_id
}

output "machine_client_secret" {
  value     = module.identity.machine_client_secret
  sensitive = true
}

output "cognito_domain" {
  value = module.identity.domain
}

output "token_endpoint_url" {
  value = module.identity.token_endpoint_url
}

output "service_role_arns" {
  description = "Mapa servico -> ARN do papel do Pod Identity."
  value       = module.access.service_role_arns
}

output "secret_names" {
  description = "Mapa nome-logico -> nome do segredo no Secrets Manager (db e oauth por servico)."
  value       = module.data.secret_names
}

output "queue_urls" {
  description = "Mapa nome da fila (incluindo DLQs) -> URL."
  value       = module.messaging.queue_urls
}

output "topic_arns" {
  description = "Mapa nome do topico -> ARN."
  value       = module.messaging.topic_arns
}

output "db_endpoint" {
  value = module.data.db_endpoint
}

output "db_address" {
  value = module.data.db_address
}

output "db_master_secret_arn" {
  value = module.data.db_master_secret_arn
}

output "redis_endpoint" {
  value = module.data.redis_endpoint
}
