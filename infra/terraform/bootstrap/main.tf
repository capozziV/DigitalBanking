data "aws_caller_identity" "current" {}

# --- Estado remoto dos demais stacks (lock nativo do S3: use_lockfile) ---

resource "aws_s3_bucket" "state" {
  bucket = "digital-banking-tfstate-${data.aws_caller_identity.current.account_id}-${var.region}"
}

resource "aws_s3_bucket_versioning" "state" {
  bucket = aws_s3_bucket.state.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "state" {
  bucket = aws_s3_bucket.state.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "state" {
  bucket                  = aws_s3_bucket.state.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# --- OIDC do GitHub: so a main do repositorio assume o papel ---

resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
}

data "aws_iam_policy_document" "github_trust" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_repository}:ref:refs/heads/main"]
    }
  }
}

resource "aws_iam_role" "github" {
  name                 = "digital-banking-github"
  assume_role_policy   = data.aws_iam_policy_document.github_trust.json
  max_session_duration = 3600
}

# O papel cria e destroi o env inteiro (rede, EKS, RDS, IAM...). Escopo amplo e aceito num
# ambiente de portfolio efemero; o limite real e a trust policy (so a main) e o Budget.
resource "aws_iam_role_policy_attachment" "github_admin" {
  role       = aws_iam_role.github.name
  policy_arn = "arn:aws:iam::aws:policy/AdministratorAccess"
}

# Deny anexado ao papel: mesmo admin, ele nao reescreve a propria trust policy, nao apaga o
# OIDC, nao mexe no Budget nem desfaz a protecao do bucket do estado. O env nao usa nenhuma
# dessas acoes (so le e escreve objetos do estado, o que continua permitido).
data "aws_iam_policy_document" "github_guardrails" {
  statement {
    sid       = "ProtectBudgets"
    effect    = "Deny"
    actions   = ["budgets:*"]
    resources = ["*"]
  }

  statement {
    sid     = "ProtectOwnRoleAndOidcProvider"
    effect  = "Deny"
    actions = ["iam:*"]
    resources = [
      aws_iam_role.github.arn,
      aws_iam_openid_connect_provider.github.arn,
    ]
  }

  statement {
    sid    = "ProtectStateBucket"
    effect = "Deny"
    actions = [
      "s3:DeleteBucket*",
      "s3:PutBucketPolicy",
      "s3:PutBucketVersioning",
      "s3:PutLifecycleConfiguration",
    ]
    resources = [aws_s3_bucket.state.arn]
  }
}

resource "aws_iam_policy" "github_guardrails" {
  name   = "digital-banking-github-guardrails"
  policy = data.aws_iam_policy_document.github_guardrails.json
}

resource "aws_iam_role_policy_attachment" "github_guardrails" {
  role       = aws_iam_role.github.name
  policy_arn = aws_iam_policy.github_guardrails.arn
}

# --- ECR: um repositorio por servico ---

resource "aws_ecr_repository" "service" {
  for_each = toset(var.services)

  name                 = "digital-banking/${each.key}"
  image_tag_mutability = "IMMUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }
}

resource "aws_ecr_lifecycle_policy" "service" {
  for_each   = aws_ecr_repository.service
  repository = each.value.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Expira imagens sem etiqueta em 1 dia"
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = 1
        }
        action = { type = "expire" }
      },
      {
        rulePriority = 2
        description  = "Mantem as 10 ultimas imagens etiquetadas"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 10
        }
        action = { type = "expire" }
      }
    ]
  })
}

# --- Orcamento: US$ 20/mes, alertas em 50%, 80% e 100% ---

resource "aws_budgets_budget" "monthly" {
  name         = "digital-banking-monthly"
  budget_type  = "COST"
  limit_amount = tostring(var.monthly_budget_usd)
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  dynamic "notification" {
    for_each = [50, 80, 100]

    content {
      comparison_operator        = "GREATER_THAN"
      threshold                  = notification.value
      threshold_type             = "PERCENTAGE"
      notification_type          = "ACTUAL"
      subscriber_email_addresses = [var.budget_alert_email]
    }
  }
}
