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
  audiences              = [module.identity.web_client_id, module.identity.machine_client_id]
  alb_listener_arn       = var.alb_listener_arn
  subnet_ids             = var.subnet_ids
  security_group_ids     = var.security_group_ids
  throttling_rate_limit  = var.throttling_rate_limit
  throttling_burst_limit = var.throttling_burst_limit
}
