# 0008 — Extrato (`statement`) com CQRS no DynamoDB

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto
O extrato é um caso de leitura frequente e não deve sobrecarregar o `accounts`, que é a fonte da verdade do saldo e do ledger.

## Decisão
- O serviço `statement` mantém um modelo de leitura (CQRS) no **DynamoDB**.
- Chave: `accountId` (partição) + `timestamp#entryId` (ordenação).
- O modelo é alimentado por eventos consumidos via SQS (ver ADR 0003).
- Gravação idempotente com `PutItem` condicional.

## Consequências
- Positivas: leitura do extrato isolada da escrita; ordenação cronológica nativa pela chave; eventos duplicados não geram lançamentos duplicados.
- Negativas: consistência eventual (o extrato pode ficar atrás do ledger por alguns instantes); mais um banco para operar.
