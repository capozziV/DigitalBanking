# Mensageria e tabela do extrato, a partir de resources.json (espelho dos scripts do LocalStack).
# Nomes iguais aos do codigo e dos scripts: os servicos os recebem por variavel de ambiente.
locals {
  spec = jsondecode(file("${path.module}/resources.json"))

  topics = { for t in local.spec.topics : t.name => t }
  queues = { for q in local.spec.queues : q.name => q }
  dlqs   = { for q in local.spec.dead_letter_queues : q.name => q }
  tables = { for t in local.spec.dynamodb_tables : t.name => t }
}

resource "aws_sns_topic" "this" {
  for_each = local.topics

  name                        = each.key
  fifo_topic                  = each.value.fifo
  content_based_deduplication = false

  lifecycle {
    precondition {
      condition     = endswith(each.key, ".fifo") == each.value.fifo
      error_message = "Topico ${each.key}: o sufixo .fifo precisa casar com o campo fifo."
    }
  }
}

resource "aws_sqs_queue" "dlq" {
  for_each = local.dlqs

  name                        = each.key
  fifo_queue                  = each.value.fifo
  content_based_deduplication = false
  # Retencao maxima para dar tempo de olhar as mensagens mortas.
  message_retention_seconds = 1209600

  lifecycle {
    precondition {
      condition     = endswith(each.key, ".fifo") == each.value.fifo
      error_message = "DLQ ${each.key}: o sufixo .fifo precisa casar com o campo fifo."
    }
  }
}

resource "aws_sqs_queue" "this" {
  for_each = local.queues

  name                        = each.key
  fifo_queue                  = each.value.fifo
  content_based_deduplication = false
  visibility_timeout_seconds  = local.spec.visibility_timeout_seconds

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq[each.value.dlq].arn
    maxReceiveCount     = local.spec.max_receive_count
  })

  lifecycle {
    precondition {
      condition     = endswith(each.key, ".fifo") == each.value.fifo
      error_message = "Fila ${each.key}: o sufixo .fifo precisa casar com o campo fifo."
    }
  }
}

# O topico so entrega na fila com esta permissao, restrita ao ARN do topico assinante.
data "aws_iam_policy_document" "queue_from_topic" {
  for_each = local.queues

  statement {
    sid       = "AllowTopic"
    actions   = ["sqs:SendMessage"]
    resources = [aws_sqs_queue.this[each.key].arn]

    principals {
      type        = "Service"
      identifiers = ["sns.amazonaws.com"]
    }

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [aws_sns_topic.this[each.value.topic].arn]
    }
  }
}

resource "aws_sqs_queue_policy" "this" {
  for_each = local.queues

  queue_url = aws_sqs_queue.this[each.key].url
  policy    = data.aws_iam_policy_document.queue_from_topic[each.key].json
}

resource "aws_sns_topic_subscription" "this" {
  for_each = local.queues

  topic_arn            = aws_sns_topic.this[each.value.topic].arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.this[each.key].arn
  raw_message_delivery = true

  depends_on = [aws_sqs_queue_policy.this]
}

resource "aws_dynamodb_table" "this" {
  for_each = local.tables

  name         = each.key
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = each.value.hash_key.name
  range_key    = each.value.range_key.name

  attribute {
    name = each.value.hash_key.name
    type = each.value.hash_key.type
  }

  attribute {
    name = each.value.range_key.name
    type = each.value.range_key.type
  }
}
