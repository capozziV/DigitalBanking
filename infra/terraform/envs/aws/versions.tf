# Sem backend remoto por enquanto: o estado e local e o ambiente e efemero (ADR 0019).
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
      project = "digital-banking"
    }
  }
}
