output "user_pool_id" {
  value = aws_cognito_user_pool.this.id
}

output "issuer_url" {
  description = "Emissor dos tokens (claim iss), usado pelo autorizador JWT e pelos servicos."
  value       = "https://cognito-idp.${data.aws_region.current.region}.amazonaws.com/${aws_cognito_user_pool.this.id}"
}

output "web_client_id" {
  value = aws_cognito_user_pool_client.web.id
}

output "machine_client_id" {
  value = aws_cognito_user_pool_client.machine.id
}

output "machine_client_secret" {
  value     = aws_cognito_user_pool_client.machine.client_secret
  sensitive = true
}
