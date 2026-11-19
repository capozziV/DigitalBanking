output "topic_arns" {
  description = "Mapa nome do topico -> ARN."
  value       = { for k, v in aws_sns_topic.this : k => v.arn }
}

output "queue_urls" {
  description = "Mapa nome da fila (incluindo DLQs) -> URL."
  value = merge(
    { for k, v in aws_sqs_queue.this : k => v.url },
    { for k, v in aws_sqs_queue.dlq : k => v.url },
  )
}

output "queue_arns" {
  description = "Mapa nome da fila (incluindo DLQs) -> ARN."
  value = merge(
    { for k, v in aws_sqs_queue.this : k => v.arn },
    { for k, v in aws_sqs_queue.dlq : k => v.arn },
  )
}

output "table_arns" {
  description = "Mapa nome da tabela -> ARN."
  value       = { for k, v in aws_dynamodb_table.this : k => v.arn }
}
