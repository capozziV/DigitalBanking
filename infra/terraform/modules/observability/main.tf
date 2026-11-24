# Destinos da telemetria do ambiente (etapa 10): grupos de logs e o painel "Transferencias". O coletor ADOT
# (chart deploy/helm/otel-collector) escreve aqui; o papel dele fica no modules/access. Tudo some no destroy.
# O Transaction Search do X-Ray e da conta e mora no stack bootstrap (ver README.md do modulo).

locals {
  # Retencao de 1 dia: o ambiente vive no maximo 8 h (aws-reaper), nao ha o que guardar depois.
  retention_in_days = 1
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

# Transaction Search (destino, politica e grupo aws/spans) e da conta: fica no stack bootstrap, porque o destroy do
# destino e um no-op e o grupo precisa de retencao. Sem ele ligado, o endpoint OTLP do X-Ray recusa os spans.

# Painel espelho do Grafana local. A duracao da saga chega como histograma exponencial (os values do transfers na AWS
# ligam histogram-flavor, delta e segundos): o awsemf a publica como Values/Counts e o CloudWatch calcula p50 e p95.
# Com buckets explicitos o awsemf so manda Count/Sum/Min/Max (provado em deploy/helm/otel-collector/check-config.sh).
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
        title  = "Duracao da saga, p50 e p95 (segundos)"
        region = var.region, view = "timeSeries", period = 60
        metrics = [
          [{ expression = "SEARCH('{${local.ns},status} MetricName=\"transfers.saga.duration\"', 'p50', 60)", id = "p50" }],
          [{ expression = "SEARCH('{${local.ns},status} MetricName=\"transfers.saga.duration\"', 'p95', 60)", id = "p95" }],
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
        title  = "Circuito do limite diario (0 fechado, 1 aberto, 2 meio aberto)"
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
