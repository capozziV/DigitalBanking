module "network" {
  source = "../../modules/network"

  name = var.name
}

module "edge_alb" {
  source = "../../modules/edge-alb"

  name       = var.name
  vpc_id     = module.network.vpc_id
  subnet_ids = module.network.private_subnet_ids
}

module "eks" {
  source = "../../modules/eks"

  name               = var.name
  region             = var.region
  kubernetes_version = var.kubernetes_version
  vpc_id             = module.network.vpc_id
  subnet_ids         = module.network.private_subnet_ids
  target_group_arns  = values(module.edge_alb.target_group_arns)
}

# ALB -> pods: o ALB alcanca os IPs dos pods (VPC CNI) pelo security group do cluster, que o EKS liga
# aos nos do node group. As regras sao do Terraform; o TargetGroupBinding nao usa `networking`.
resource "aws_vpc_security_group_egress_rule" "alb_to_pods" {
  for_each = module.edge_alb.service_ports

  security_group_id            = module.edge_alb.alb_security_group_id
  referenced_security_group_id = module.eks.cluster_security_group_id
  ip_protocol                  = "tcp"
  from_port                    = each.value
  to_port                      = each.value
  description                  = "ALB para os pods do ${each.key}"
}

resource "aws_vpc_security_group_ingress_rule" "pods_from_alb" {
  for_each = module.edge_alb.service_ports

  security_group_id            = module.eks.cluster_security_group_id
  referenced_security_group_id = module.edge_alb.alb_security_group_id
  ip_protocol                  = "tcp"
  from_port                    = each.value
  to_port                      = each.value
  description                  = "Pods do ${each.key} a partir do ALB"
}

module "identity" {
  source = "../../modules/identity"

  name              = var.name
  domain_prefix     = var.cognito_domain_prefix
  web_callback_urls = var.web_callback_urls
  web_logout_urls   = var.web_logout_urls
}

module "edge" {
  source = "../../modules/edge"

  name                   = var.name
  issuer_url             = module.identity.issuer_url
  audiences              = [module.identity.web_client_id, module.identity.machine_client_id, module.identity.ops_client_id, module.identity.bff_client_id]
  alb_listener_arn       = module.edge_alb.listener_arn
  subnet_ids             = module.network.private_subnet_ids
  security_group_ids     = [module.edge_alb.vpc_link_security_group_id]
  throttling_rate_limit  = var.throttling_rate_limit
  throttling_burst_limit = var.throttling_burst_limit
}

module "messaging" {
  source = "../../modules/messaging"
}

module "data" {
  source = "../../modules/data"

  name                     = var.name
  vpc_id                   = module.network.vpc_id
  subnet_ids               = module.network.private_subnet_ids
  client_security_group_id = module.eks.cluster_security_group_id
  machine_client_secret    = module.identity.machine_client_secret
}

module "access" {
  source = "../../modules/access"

  name         = var.name
  cluster_name = module.eks.cluster_name
  namespace    = var.kubernetes_namespace
  topic_arns   = module.messaging.topic_arns
  queue_arns   = module.messaging.queue_arns
  table_arns   = module.messaging.table_arns

  db_init_secret_arns = module.data.db_init_secret_arns
}
