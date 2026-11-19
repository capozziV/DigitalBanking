output "state_bucket" {
  description = "Bucket S3 do estado do env."
  value       = aws_s3_bucket.state.id
}

output "github_role_arn" {
  description = "Papel que os workflows do GitHub assumem por OIDC."
  value       = aws_iam_role.github.arn
}

output "ecr_repository_urls" {
  description = "URL do repositorio ECR por servico."
  value       = { for name, repo in aws_ecr_repository.service : name => repo.repository_url }
}
