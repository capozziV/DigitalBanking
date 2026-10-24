#!/bin/bash
# Recursos do accounts no LocalStack: topico SNS FIFO e fila FIFO de inspecao assinada nele.
# A fila nao tem consumidor; serve para ver os eventos publicados (smoke, testes e inspecao manual).
# Raw message delivery: o corpo e o envelope do evento e eventType/correlationId chegam como atributos SQS.
set -euo pipefail

# CLI da AWS da imagem apontando para o proprio LocalStack (o awslocal esta depreciado).
export AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_DEFAULT_REGION=us-east-1
aws_local() {
  aws --endpoint-url http://127.0.0.1:4566 "$@"
}

topic_arn=$(aws_local sns create-topic \
  --name accounts-events.fifo \
  --attributes FifoTopic=true,ContentBasedDeduplication=false \
  --query TopicArn --output text)

queue_url=$(aws_local sqs create-queue \
  --queue-name accounts-events-inspection.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=false \
  --query QueueUrl --output text)

queue_arn=$(aws_local sqs get-queue-attributes \
  --queue-url "$queue_url" \
  --attribute-names QueueArn \
  --query Attributes.QueueArn --output text)

aws_local sns subscribe \
  --topic-arn "$topic_arn" \
  --protocol sqs \
  --notification-endpoint "$queue_arn" \
  --attributes RawMessageDelivery=true > /dev/null

echo "accounts: topico $topic_arn e fila $queue_url prontos"
