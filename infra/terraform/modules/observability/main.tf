# Destinos da telemetria do ambiente (etapa 10): grupos de logs, Transaction Search do X-Ray e o painel
# "Transferencias". O coletor ADOT (chart deploy/helm/otel-collector) escreve aqui; o papel dele fica no
# modules/access. Tudo e do Terraform e some no destroy, com duas excecoes que a conta guarda (ver README.md do
# modulo): a configuracao do Transaction Search e o grupo aws/spans, que o X-Ray cria sozinho.
data "aws_caller_identity" "current" {}

locals {
  # Retencao de 1 dia: o ambiente vive no maximo 8 h (aws-reaper), nao ha o que guardar depois.
  retention_in_days = 1
  account_id        = data.aws_caller_identity.current.account_id
}

# Um grupo por servico: o awscloudwatchlogs do coletor escreve em <prefixo>/<service.name>.
resource "aws_cloudwatch_log_group" "service" {
  for_each = toset(var.services)

  name              = "${var.log_group_prefix}/${each.key}"
  retention_in_days = local.retention_in_days
}

# Registros EMF das metricas: o CloudWatch converte cada registro em metrica do namespace.
resource "aws_cloudwatch_log_group" "metrics" {
  name              = "${var.log_group_prefix}/metrics"
  retention_in_days = local.retention_in_days
}

# Transaction Search (conta inteira): o endpoint OTLP do X-Ray so aceita spans com ele ligado. Os spans vao para o
# grupo aws/spans (IDs W3C guardados como chegam, todo atributo pesquisavel), com 1% indexado de graca.
# Politica de recurso do X-Ray para gravar no grupo: a mesma da documentacao do Transaction Search.
data "aws_iam_policy_document" "xray_spans" {
  statement {
    sid     = "TransactionSearchXRayAccess"
    effect  = "Allow"
    actions = ["logs:PutLogEvents"]
    resources = [
      "arn:aws:logs:${var.region}:${local.account_id}:log-group:aws/spans:*",
      "arn:aws:logs:${var.region}:${local.account_id}:log-group:/aws/application-signals/data:*",
    ]

    principals {
      type        = "Service"
      identifiers = ["xray.amazonaws.com"]
    }

    condition {
      test     = "ArnLike"
      variable = "aws:SourceArn"
      values   = ["arn:aws:xray:${var.region}:${local.account_id}:*"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:SourceAccount"
      values   = [local.account_id]
    }
  }
}

resource "aws_cloudwatch_log_resource_policy" "xray_spans" {
  policy_name     = "${var.metrics_namespace}-xray-spans"
  policy_document = data.aws_iam_policy_document.xray_spans.json
}

resource "aws_xray_trace_segment_destination" "this" {
  destination = "CloudWatchLogs"

  depends_on = [aws_cloudwatch_log_resource_policy.xray_spans]
}

# Painel espelho do Grafana local. O awsemf manda a duracao como conjunto de estatisticas (Count, Sum, Min, Max):
# o CloudWatch mostra media e maximo, nao p50/p95 (o Grafana local calcula os quantis pelos buckets).
locals {
  ns = var.metrics_namespace

  widgets = [
    {
      type = "metric", x = 0, y = 0, width = 12, height = 6
      properties = {
        title  = "Transferencias por minuto, por status"
        region = var.region, view = "timeSeries", stat = "Sum", period = 60
        metrics = [
          [local.ns, "transfers.finished", "status", "COMPLETED"],
          [local.ns, "transfers.finished", "status", "FAILED"],
          [local.ns, "transfers.finished", "status", "COMPENSATED"],
        ]
      }
    },
    {
      type = "metric", x = 12, y = 0, width = 12, height = 6
      properties = {
        title  = "Duracao da saga (media e maximo, segundos)"
        region = var.region, view = "timeSeries", period = 60
        metrics = [
          [local.ns, "transfers.saga.duration", "status", "COMPLETED", { stat = "Average", label = "COMPLETED media" }],
          [local.ns, "transfers.saga.duration", "status", "COMPLETED", { stat = "Maximum", label = "COMPLETED maximo" }],
          [local.ns, "transfers.saga.duration", "status", "COMPENSATED", { stat = "Average", label = "COMPENSATED media" }],
          [local.ns, "transfers.saga.duration", "status", "COMPENSATED", { stat = "Maximum", label = "COMPENSATED maximo" }],
        ]
      }
    },
    {
      type = "metric", x = 0, y = 6, width = 12, height = 6
      properties = {
        title  = "Recusas e devolucoes, por motivo"
        region = var.region, view = "timeSeries", stat = "Sum", period = 60
        metrics = [
          [{ expression = "SEARCH('{${local.ns},reason,status} MetricName=\"transfers.finished\"', 'Sum', 60)", id = "reasons" }],
        ]
      }
    },
    {
      type = "metric", x = 12, y = 6, width = 12, height = 6
      properties = {
        title  = "Mensagens nas DLQs"
        region = var.region, view = "timeSeries", stat = "Maximum", period = 60
        metrics = [
          [{ expression = "SEARCH('{${local.ns},queue} MetricName=\"notification.dlq.messages\"', 'Maximum', 60)", id = "notification" }],
          [{ expression = "SEARCH('{${local.ns},queue} MetricName=\"statement.dlq.messages\"', 'Maximum', 60)", id = "statement" }],
        ]
      }
    },
    {
      type = "metric", x = 0, y = 12, width = 12, height = 6
      properties = {
        title  = "Outbox: pendentes e mais antigo (s)"
        region = var.region, view = "timeSeries", stat = "Maximum", period = 60
        metrics = [
          [{ expression = "SEARCH('{${local.ns},\"service.name\"} MetricName=\"outbox.pending\"', 'Maximum', 60)", id = "pending" }],
          [{ expression = "SEARCH('{${local.ns},\"service.name\"} MetricName=\"outbox.oldest.pending.age\"', 'Maximum', 60)", id = "age", yAxis = "right" }],
        ]
      }
    },
    {
      type = "metric", x = 12, y = 12, width = 12, height = 6
      properties = {
        title  = "Outbox: falhas de publicacao e agregados travados"
        region = var.region, view = "timeSeries", period = 60
        metrics = [
          [{ expression = "SEARCH('{${local.ns},\"service.name\"} MetricName=\"outbox.publish.failures\"', 'Sum', 60)", id = "failures" }],
          [{ expression = "SEARCH('{${local.ns},\"service.name\"} MetricName=\"outbox.stuck.aggregates\"', 'Maximum', 60)", id = "stuck" }],
        ]
      }
    },
    {
      type = "metric", x = 0, y = 18, width = 12, height = 6
      properties = {
        title  = "Cache do extrato, por resultado"
        region = var.region, view = "timeSeries", stat = "Sum", period = 60
        metrics = [
          [{ expression = "SEARCH('{${local.ns},result} MetricName=\"statement.cache.requests\"', 'Sum', 60)", id = "cache" }],
        ]
      }
    },
    {
      type = "metric", x = 12, y = 18, width = 12, height = 6
      properties = {
        title  = "Circuito do limite diario (0 fechado, 1 aberto)"
        region = var.region, view = "timeSeries", stat = "Maximum", period = 60
        metrics = [
          [local.ns, "transfers.daily_limit.circuit.state"],
        ]
      }
    },
  ]
}

resource "aws_cloudwatch_dashboard" "transfers" {
  dashboard_name = var.dashboard_name
  dashboard_body = jsonencode({ widgets = local.widgets })
}
