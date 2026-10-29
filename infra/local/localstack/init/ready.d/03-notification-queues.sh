#!/bin/bash
# Recursos do notification no LocalStack: uma fila FIFO por produtor (com DLQ FIFO), assinadas nos topicos
# accounts-events.fifo e transfers-events.fifo (criados pelos scripts 01 e 02, que rodam antes deste).
# Idempotente: create-queue e subscribe com os mesmos parametros devolvem o recurso existente.
# Raw message delivery: o corpo e o envelope do evento e eventType/correlationId chegam como atributos SQS.
# Configuravel por variavel de ambiente do conteiner: NOTIFICATION_MAX_RECEIVE_COUNT (padrao 5) e
# NOTIFICATION_VISIBILITY_TIMEOUT_SECONDS (padrao 30).
set -euo pipefail

export AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_DEFAULT_REGION=us-east-1
aws_local() {
  aws --endpoint-url http://127.0.0.1:4566 "$@"
}

max_receive_count="${NOTIFICATION_MAX_RECEIVE_COUNT:-5}"
visibility_timeout="${NOTIFICATION_VISIBILITY_TIMEOUT_SECONDS:-30}"

# queue_arn <queue-url>
queue_arn() {
  aws_local sqs get-queue-attributes --queue-url "$1" \
    --attribute-names QueueArn --query Attributes.QueueArn --output text
}

# create_consumer_queue <queue-name> <dlq-name> <topic-name>
create_consumer_queue() {
  local queue_name="$1" dlq_name="$2" topic_name="$3"
  local dlq_url dlq_arn queue_url arn topic_arn policy attributes

  dlq_url=$(aws_local sqs create-queue \
    --queue-name "$dlq_name" \
    --attributes FifoQueue=true,ContentBasedDeduplication=false \
    --query QueueUrl --output text)
  dlq_arn=$(queue_arn "$dlq_url")

  # RedrivePolicy e um JSON dentro do JSON de atributos, por isso as aspas escapadas.
  policy="{\\\"deadLetterTargetArn\\\":\\\"$dlq_arn\\\",\\\"maxReceiveCount\\\":\\\"$max_receive_count\\\"}"
  attributes="{\"FifoQueue\":\"true\",\"ContentBasedDeduplication\":\"false\",\"VisibilityTimeout\":\"$visibility_timeout\",\"RedrivePolicy\":\"$policy\"}"
  queue_url=$(aws_local sqs create-queue \
    --queue-name "$queue_name" \
    --attributes "$attributes" \
    --query QueueUrl --output text)
  arn=$(queue_arn "$queue_url")

  topic_arn=$(aws_local sns create-topic \
    --name "$topic_name" \
    --attributes FifoTopic=true,ContentBasedDeduplication=false \
    --query TopicArn --output text)
  aws_local sns subscribe \
    --topic-arn "$topic_arn" \
    --protocol sqs \
    --notification-endpoint "$arn" \
    --attributes RawMessageDelivery=true > /dev/null

  echo "notification: fila $queue_url (DLQ $dlq_url) assinada em $topic_arn"
}

create_consumer_queue notification-accounts.fifo notification-accounts-dlq.fifo accounts-events.fifo
create_consumer_queue notification-transfers.fifo notification-transfers-dlq.fifo transfers-events.fifo
