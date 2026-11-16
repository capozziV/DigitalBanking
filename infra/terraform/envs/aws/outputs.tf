output "api_endpoint" {
  value = module.edge.api_endpoint
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
