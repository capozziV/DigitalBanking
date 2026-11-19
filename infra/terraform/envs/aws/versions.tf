# Ambiente efemero (ADR 0019). Estado no bucket S3 do stack bootstrap, com configuracao parcial:
# o workflow passa bucket, key e region no init (-backend-config); use_lockfile liga o lock nativo do S3.
# Para validar sem credenciais: terraform init -backend=false.
terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.70"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 3.1"
    }
  }

  backend "s3" {
    use_lockfile = true
    encrypt      = true
  }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      project      = "digitalbanking"
      "created-at" = var.created_at
    }
  }
}

# Credencial de curta duracao pelo aws CLI (o runner do GitHub tem o CLI); o papel que aplica e o
# administrador do cluster (bootstrap_cluster_creator_admin_permissions).
provider "helm" {
  kubernetes = {
    host                   = module.eks.cluster_endpoint
    cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)

    exec = {
      api_version = "client.authentication.k8s.io/v1beta1"
      command     = "aws"
      args        = ["eks", "get-token", "--cluster-name", module.eks.cluster_name, "--region", var.region]
    }
  }
}
