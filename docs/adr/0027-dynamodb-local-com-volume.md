# 0027 — DynamoDB Local com volume

- **Status:** aceito
- **Data:** 2026-11-22

## Contexto
O extrato ([ADR 0008](0008-statement-cqrs-dynamodb.md)) vive numa tabela do DynamoDB que, localmente, ficava no LocalStack. O plano gratuito do LocalStack não tem persistência: em 2026-11-20 um `compose down` apagou o extrato. A tabela é uma projeção reconstruível só pelo ledger, e essa reconstrução ainda não existe (#117).

## Decisão
- **Serviço `dynamodb` no docker-compose**, com o DynamoDB Local da AWS (versão fixa), gravando em disco num **volume nomeado** (`dynamodb-data`).
- **A tabela `statement` é criada nesse serviço**, de forma idempotente, e o `statement` só sobe depois dela. O LocalStack fica só com SNS e SQS.
- **O `statement` tem um endpoint do DynamoDB separado do de SQS.** Endpoint vazio segue sendo a AWS real: nada muda no deploy ([ADR 0021](0021-deploy-e-ambiente-efemero.md)).
- **Resíduo aceito:** mensagens em trânsito e na DLQ continuam no LocalStack, sem persistência; recriar o LocalStack as perde. `compose down -v` apaga o volume de propósito, e é o jeito de zerar o ambiente local.

## Consequências
- Positivas: o extrato sobrevive a `down`, a recriação de contêiner e a reinício da máquina; grátis, sem conta nem internet; o deploy na AWS não muda.
- Negativas: um contêiner, uma porta e um endpoint a mais; o DynamoDB Local não é idêntico ao serviço real; fila e DLQ locais seguem voláteis, então um `down` com mensagens pendentes as perde; quem já tem a stack precisa copiar os itens do LocalStack para o banco novo (procedimento no guia local).

## Alternativas consideradas
- **Persistência do LocalStack:** exige plano pago ou de estudante e grava por snapshot periódico, sem garantia no instante da queda.
- **DynamoDB real no desenvolvimento:** exige conta, credencial e internet de quem clonar o projeto.
