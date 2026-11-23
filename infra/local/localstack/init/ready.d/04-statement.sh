#!/bin/bash
# Recursos do statement no LocalStack: fila FIFO (com DLQ FIFO) assinada no topico accounts-events.fifo
# (criado pelo script 01, que roda antes deste). A tabela do extrato fica no DynamoDB Local, com volume
# (infra/local/dynamodb/init/), fora do LocalStack, que nao tem persistencia.
# Idempotente: create-queue e subscribe com os mesmos parametros devolvem o recurso existente.
# Raw message delivery: o corpo e o envelope do evento e eventType/correlationId chegam como atributos SQS.
# Configuravel por variavel de ambiente do conteiner: STATEMENT_MAX_RECEIVE_COUNT (padrao 5) e
# STATEMENT_VISIBILITY_TIMEOUT_SECONDS (padrao 30).
set -euo pipefail

export AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_DEFAULT_REGION=us-east-1
aws_local() {
  aws --endpoint-url http://127.0.0.1:4566 "$@"
}

max_receive_count="${STATEMENT_MAX_RECEIVE_COUNT:-5}"
visibility_timeout="${STATEMENT_VISIBILITY_TIMEOUT_SECONDS:-30}"

queue_arn() {
  aws_local sqs get-queue-attributes --queue-url "$1" \
    --attribute-names QueueArn --query Attributes.QueueArn --output text
}

dlq_url=$(aws_local sqs create-queue \
  --queue-name statement-accounts-dlq.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=false \
  --query QueueUrl --output text)
dlq_arn=$(queue_arn "$dlq_url")

# RedrivePolicy e um JSON dentro do JSON de atributos, por isso as aspas escapadas.
policy="{\\\"deadLetterTargetArn\\\":\\\"$dlq_arn\\\",\\\"maxReceiveCount\\\":\\\"$max_receive_count\\\"}"
attributes="{\"FifoQueue\":\"true\",\"ContentBasedDeduplication\":\"false\",\"VisibilityTimeout\":\"$visibility_timeout\",\"RedrivePolicy\":\"$policy\"}"
queue_url=$(aws_local sqs create-queue \
  --queue-name statement-accounts.fifo \
  --attributes "$attributes" \
  --query QueueUrl --output text)
arn=$(queue_arn "$queue_url")

topic_arn=$(aws_local sns create-topic \
  --name accounts-events.fifo \
  --attributes FifoTopic=true,ContentBasedDeduplication=false \
  --query TopicArn --output text)
aws_local sns subscribe \
  --topic-arn "$topic_arn" \
  --protocol sqs \
  --notification-endpoint "$arn" \
  --attributes RawMessageDelivery=true > /dev/null

echo "statement: fila $queue_url (DLQ $dlq_url) assinada em $topic_arn"
