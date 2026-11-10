# 0018 — Extrato ordenado pela sequência do ledger

- **Status:** aceito
- **Data:** 2026-11-10

## Contexto
O [ADR 0008](0008-statement-cqrs-dynamodb.md) definiu a chave do item do extrato como `accountId` + `timestamp#entryId`. Essa ordenação não basta: dois lançamentos podem ter o mesmo instante, e o relógio não é a ordem do ledger. A ordem verdadeira já existe na `sequence` do lançamento (`LedgerEntry.sequence`), atribuída pelo `accounts` em ordem, a partir de 1, única por conta, sob a trava pessimista ([ADR 0012](0012-saldo-no-lancamento-e-trava-pessimista.md)). O `statement` é o segundo consumidor de eventos, depois do `notification` ([ADR 0017](0017-notification-com-banco-e-consumo-sqs.md)).

## Decisão
- **Chave do item:** `accountId` (partição) + `sequence` (ordenação), com `sequence` como atributo numérico (`N`) no DynamoDB. Substitui só a chave de ordenação do ADR 0008.
- **Gravação idempotente:** `PutItem` com `ConditionExpression` `attribute_not_exists(accountId)`. Na `ConditionalCheckFailedException`, o `statement` lê o item da mesma chave: mesmo `entryId` é evento repetido (sem efeito, mensagem confirmada); `entryId` diferente é inconsistência (erro, a mensagem acaba na DLQ). Não há tabela `processed_events` (diferente do `notification`): o próprio item é a prova.
- **`AccountOpened` também é consumido**, para guardar a conta e devolver `404` para conta desconhecida. Ele usa o mesmo tópico e o mesmo grupo FIFO por conta dos lançamentos, então chega antes deles.
- **Cliente:** `DynamoDbClient` de baixo nível do AWS SDK v2, com todo o código do DynamoDB num só adapter. A condição fica explícita.
- **Sem importação:** lançamentos anteriores ao `statement` não são importados (ambiente local descartável); a reconstrução fica para depois.

## Consequências
- Positivas: a ordem do extrato é a do ledger, sem empate; a chave é determinística, então a idempotência não depende de relógio; sem tabela extra de eventos processados; paginação por `sequence` simples.
- Negativas: depende de o `accounts` manter a `sequence` sem buracos e de incluí-la no evento `LedgerEntryRecorded` (já acontece); não serve para extrato que misture várias contas (a `sequence` é por conta); o instante não está na chave, então um filtro por período (cartão #116) vai precisar de um índice secundário global (`accountId` + `createdAt`), que pode ser criado depois; histórico anterior à etapa 4 fica de fora.

## Alternativas consideradas
- **Manter `timestamp#entryId`:** empata em lançamentos no mesmo instante e confunde relógio com ordem do ledger.
- **`processed_events` como no `notification`:** tabela e transação extras sem ganho, pois a chave do item já é única por lançamento e o serviço não tem outro banco onde gravar o controle na mesma transação.
- **Enhanced Client ou Spring Cloud AWS DynamoDB:** menos código, mas escondem a condição de gravação e espalham o acesso ao DynamoDB.
