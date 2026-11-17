# Stack permanente e quase sem custo (S3, ECR, Budgets). Estado local: e ele que cria o bucket
# do estado dos demais stacks. Aplicado uma vez pelo usuario, com confirmacao explicita.
terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.70"
    }
  }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      project = "digitalbanking"
      stack   = "bootstrap"
    }
  }
}
