variable "name" {
  description = "Prefixo dos nomes dos recursos de identidade."
  type        = string
}

variable "web_callback_urls" {
  description = "URLs de retorno do app client web (authorization code + PKCE)."
  type        = list(string)
}

variable "web_logout_urls" {
  description = "URLs de logout do app client web."
  type        = list(string)
}

variable "tags" {
  description = "Tags aplicadas aos recursos."
  type        = map(string)
  default     = {}
}
