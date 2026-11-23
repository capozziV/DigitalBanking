#!/usr/bin/env bash
# Entrada do DynamoDB Local (compose e e2e): sobe o banco e cria a tabela do extrato (statement-table.json) se ela
# nao existir. Nunca altera nem apaga uma tabela existente.
#   run.sh <opcoes do DynamoDBLocal.jar>   sobe o banco, com a tabela
#   run.sh --check                         healthcheck: sai com 0 so se a tabela existe
# A imagem nao tem CLI da AWS: vai de curl. A assinatura nao e conferida pelo DynamoDB Local.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"

call() {
  curl -fsS --noproxy '*' --max-time 5 http://127.0.0.1:8000/ \
    -H 'Content-Type: application/x-amz-json-1.0' \
    -H "X-Amz-Target: DynamoDB_20120810.$1" \
    -H 'Authorization: AWS4-HMAC-SHA256 Credential=test/20260101/us-east-1/dynamodb/aws4_request, SignedHeaders=host, Signature=0' \
    --data-binary "$2" > /dev/null 2>&1
}
table_exists() { call DescribeTable '{"TableName":"statement"}'; }

if [ "${1:-}" = --check ]; then
  table_exists
  exit
fi

cd /home/dynamodblocal
java -jar DynamoDBLocal.jar -disableTelemetry "$@" &
server=$!
# O bash e o PID 1 e nao repassa sinais: sem isto o `down` mataria o banco com SIGKILL, no meio de uma escrita.
stop() {
  kill -TERM "$server" 2> /dev/null || true
  wait "$server" || true
}
trap 'stop; exit 0' TERM INT

for ((attempt = 0; attempt < 120; attempt++)); do
  if table_exists; then
    echo "dynamodb: table statement ready"
    wait "$server"
    exit
  fi
  if ! kill -0 "$server" 2> /dev/null; then
    echo "dynamodb: o DynamoDB Local encerrou na partida" >&2
    exit 1
  fi
  # Falha enquanto o banco sobe; a volta seguinte confere.
  call CreateTable "@$here/statement-table.json" || true
  sleep 1
done
echo "dynamodb: a tabela statement nao ficou pronta" >&2
stop
exit 1
