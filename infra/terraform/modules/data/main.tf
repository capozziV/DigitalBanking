# RDS PostgreSQL e ElastiCache Redis nos menores tamanhos, mais as senhas no Secrets Manager.
# Nada de senha em arquivo versionado: o RDS gera e guarda a senha mestre (manage_master_user_password) e as
# demais sao geradas aqui. O ambiente e efemero: sem snapshot final, sem protecao de exclusao e segredos
# apagados na hora (recovery_window_in_days = 0) para o proximo `apply` reaproveitar o nome.

# --- RDS ---

resource "aws_db_subnet_group" "this" {
  name       = "${var.name}-db"
  subnet_ids = var.subnet_ids
}

resource "aws_security_group" "db" {
  name        = "${var.name}-db"
  description = "RDS PostgreSQL: acesso so a partir dos nos e pods do EKS"
  vpc_id      = var.vpc_id
}

resource "aws_vpc_security_group_ingress_rule" "db" {
  security_group_id            = aws_security_group.db.id
  referenced_security_group_id = var.client_security_group_id
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
  description                  = "PostgreSQL a partir dos pods"
}

resource "aws_db_instance" "this" {
  identifier = "${var.name}-pg"

  engine         = "postgres"
  engine_version = "17"
  instance_class = "db.t4g.micro"

  allocated_storage = 20
  storage_type      = "gp3"
  storage_encrypted = true

  db_name  = "postgres"
  username = "dbadmin"
  # O RDS gera a senha mestre e a guarda no Secrets Manager (nunca passa pelo estado do Terraform).
  manage_master_user_password = true

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [aws_security_group.db.id]
  publicly_accessible    = false
  multi_az               = false

  backup_retention_period    = 0
  skip_final_snapshot        = true
  deletion_protection        = false
  auto_minor_version_upgrade = true
  apply_immediately          = true
}

# Senhas do dono (Flyway) e do usuario de aplicacao de cada servico. As chaves do JSON sao os nomes das
# variaveis de ambiente do chart (secretEnv), para virarem o Secret do Kubernetes sem renomear.
resource "random_password" "db_owner" {
  for_each = toset(var.database_services)

  length  = 32
  special = false
}

resource "random_password" "db_app" {
  for_each = toset(var.database_services)

  length  = 32
  special = false
}

resource "aws_secretsmanager_secret" "db" {
  for_each = toset(var.database_services)

  name                    = "${var.name}/${each.key}-db"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "db" {
  for_each = toset(var.database_services)

  secret_id = aws_secretsmanager_secret.db[each.key].id
  secret_string = jsonencode({
    "${upper(each.key)}_DB_PASSWORD"       = random_password.db_app[each.key].result
    "${upper(each.key)}_DB_OWNER_PASSWORD" = random_password.db_owner[each.key].result
  })
}

# Segredo do app client de maquina do Cognito, um por servico que chama outro servico.
resource "aws_secretsmanager_secret" "oauth" {
  for_each = toset(var.oauth_services)

  name                    = "${var.name}/${each.key}-oauth"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "oauth" {
  for_each = toset(var.oauth_services)

  secret_id = aws_secretsmanager_secret.oauth[each.key].id
  secret_string = jsonencode({
    "${upper(each.key)}_OAUTH_CLIENT_SECRET" = var.machine_client_secret
  })
}

# --- ElastiCache ---

resource "aws_elasticache_subnet_group" "this" {
  name       = "${var.name}-redis"
  subnet_ids = var.subnet_ids
}

resource "aws_security_group" "redis" {
  name        = "${var.name}-redis"
  description = "ElastiCache Redis: acesso so a partir dos nos e pods do EKS"
  vpc_id      = var.vpc_id
}

resource "aws_vpc_security_group_ingress_rule" "redis" {
  security_group_id            = aws_security_group.redis.id
  referenced_security_group_id = var.client_security_group_id
  ip_protocol                  = "tcp"
  from_port                    = 6379
  to_port                      = 6379
  description                  = "Redis a partir dos pods"
}

resource "aws_elasticache_parameter_group" "this" {
  name   = "${var.name}-redis7"
  family = "redis7"
}

resource "aws_elasticache_cluster" "this" {
  cluster_id = "${var.name}-redis"

  engine          = "redis"
  engine_version  = "7.1"
  node_type       = "cache.t4g.micro"
  num_cache_nodes = 1
  port            = 6379

  parameter_group_name = aws_elasticache_parameter_group.this.name
  subnet_group_name    = aws_elasticache_subnet_group.this.name
  security_group_ids   = [aws_security_group.redis.id]
  apply_immediately    = true
}
