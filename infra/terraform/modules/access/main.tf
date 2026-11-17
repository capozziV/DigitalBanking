# Um papel por servico, ligado pelo Pod Identity a ServiceAccount de mesmo nome do servico (o chart cria
# a ServiceAccount com o nome do servico, no namespace do release). Cada papel recebe so as acoes de que o
# servico precisa, nos ARNs especificos: nenhum curinga de recurso.
locals {
  # sns_publish: o relay do outbox publica no topico do servico.
  # sqs_consume: o @SqsListener recebe, apaga e ajusta a visibilidade na fila do servico.
  # sqs_inspect: so le atributos (a metrica de mensagens na DLQ le o tamanho da DLQ).
  # dynamodb: o extrato grava (PutItem condicional) e le (GetItem, Query) a propria tabela.
  services = {
    accounts = {
      sns_publish = [var.topic_arns["accounts-events.fifo"]]
    }
    transfers = {
      sns_publish = [var.topic_arns["transfers-events.fifo"]]
    }
    customer = {
      sns_publish = [var.topic_arns["customer-events.fifo"]]
    }
    notification = {
      sqs_consume = [var.queue_arns["notification-accounts.fifo"], var.queue_arns["notification-transfers.fifo"]]
      sqs_inspect = [var.queue_arns["notification-accounts-dlq.fifo"], var.queue_arns["notification-transfers-dlq.fifo"]]
    }
    statement = {
      sqs_consume = [var.queue_arns["statement-accounts.fifo"]]
      sqs_inspect = [var.queue_arns["statement-accounts-dlq.fifo"]]
      dynamodb    = [var.table_arns["statement"]]
    }
  }
}

data "aws_iam_policy_document" "assume" {
  statement {
    actions = ["sts:AssumeRole", "sts:TagSession"]

    principals {
      type        = "Service"
      identifiers = ["pods.eks.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "service" {
  for_each = local.services

  dynamic "statement" {
    for_each = lookup(each.value, "sns_publish", [])

    content {
      sid       = "Publish${index(each.value.sns_publish, statement.value)}"
      actions   = ["sns:Publish"]
      resources = [statement.value]
    }
  }

  dynamic "statement" {
    for_each = length(lookup(each.value, "sqs_consume", [])) > 0 ? [1] : []

    content {
      sid = "ConsumeQueues"
      actions = [
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:ChangeMessageVisibility",
        "sqs:GetQueueAttributes",
        "sqs:GetQueueUrl",
      ]
      resources = each.value.sqs_consume
    }
  }

  dynamic "statement" {
    for_each = length(lookup(each.value, "sqs_inspect", [])) > 0 ? [1] : []

    content {
      sid       = "InspectDeadLetterQueues"
      actions   = ["sqs:GetQueueAttributes", "sqs:GetQueueUrl"]
      resources = each.value.sqs_inspect
    }
  }

  dynamic "statement" {
    for_each = length(lookup(each.value, "dynamodb", [])) > 0 ? [1] : []

    content {
      sid       = "StatementTable"
      actions   = ["dynamodb:PutItem", "dynamodb:GetItem", "dynamodb:Query"]
      resources = each.value.dynamodb
    }
  }
}

resource "aws_iam_role" "service" {
  for_each = local.services

  name               = "${var.name}-${each.key}"
  assume_role_policy = data.aws_iam_policy_document.assume.json
}

resource "aws_iam_role_policy" "service" {
  for_each = local.services

  name   = "${var.name}-${each.key}"
  role   = aws_iam_role.service[each.key].id
  policy = data.aws_iam_policy_document.service[each.key].json
}

resource "aws_eks_pod_identity_association" "service" {
  for_each = local.services

  cluster_name    = var.cluster_name
  namespace       = var.namespace
  service_account = each.key
  role_arn        = aws_iam_role.service[each.key].arn
}

# Job db-init (chart deploy/helm/db-init): cria bancos e usuarios dos servicos no RDS, que e privado. So le a
# senha mestre (segredo gerido pelo RDS) e os segredos <name>/<servico>-db. Os segredos usam a chave gerida
# aws/secretsmanager, cuja politica ja libera o Decrypt via Secrets Manager para a conta: sem kms:Decrypt aqui.
data "aws_iam_policy_document" "db_init" {
  statement {
    sid       = "ReadDatabaseSecrets"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = var.db_init_secret_arns
  }
}

resource "aws_iam_role" "db_init" {
  name               = "${var.name}-db-init"
  assume_role_policy = data.aws_iam_policy_document.assume.json
}

resource "aws_iam_role_policy" "db_init" {
  name   = "${var.name}-db-init"
  role   = aws_iam_role.db_init.id
  policy = data.aws_iam_policy_document.db_init.json
}

resource "aws_eks_pod_identity_association" "db_init" {
  cluster_name    = var.cluster_name
  namespace       = var.namespace
  service_account = var.db_init_service_account
  role_arn        = aws_iam_role.db_init.arn
}
