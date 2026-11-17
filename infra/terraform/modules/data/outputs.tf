output "db_endpoint" {
  description = "Endereco do RDS (host:porta)."
  value       = aws_db_instance.this.endpoint
}

output "db_address" {
  description = "Host do RDS, sem a porta."
  value       = aws_db_instance.this.address
}

output "db_master_secret_arn" {
  description = "ARN do segredo da senha mestre, gerido pelo RDS; usado so para criar bancos e usuarios dos servicos."
  value       = aws_db_instance.this.master_user_secret[0].secret_arn
}

output "redis_endpoint" {
  description = "Host do Redis (porta 6379)."
  value       = aws_elasticache_cluster.this.cache_nodes[0].address
}

output "secret_names" {
  description = "Mapa nome-logico -> nome do segredo no Secrets Manager (chaves iguais as do secretEnv do chart)."
  value = merge(
    { for k, v in aws_secretsmanager_secret.db : "${k}-db" => v.name },
    { for k, v in aws_secretsmanager_secret.oauth : "${k}-oauth" => v.name },
  )
}

output "secret_arns" {
  description = "Mapa nome-logico -> ARN do segredo (inclui o da senha mestre como rds-master)."
  value = merge(
    { for k, v in aws_secretsmanager_secret.db : "${k}-db" => v.arn },
    { for k, v in aws_secretsmanager_secret.oauth : "${k}-oauth" => v.arn },
    { "rds-master" = aws_db_instance.this.master_user_secret[0].secret_arn },
  )
}
