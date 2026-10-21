# 0003 — Transactional Outbox e mensageria SNS/SQS

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto
Serviços precisam publicar eventos (ex.: lançamentos no ledger, etapas da saga) sem perder nem inventar mensagens quando o banco grava e o broker falha, ou vice-versa. Consumidores (statement, notification, etc.) podem receber a mesma mensagem mais de uma vez.

## Decisão
- Eventos são publicados **somente** via Transactional Outbox: o evento é gravado na mesma transação do dado de negócio e publicado depois por um relay.
- **Um tópico SNS por produtor**; **uma fila SQS + DLQ por consumidor**.
- Consumidores são sempre **idempotentes**.
- O correlation ID é gravado em uma coluna do outbox, vai no envelope do evento e nos atributos SNS/SQS (raw message delivery).

## Consequências
- Positivas: sem dual write; falhas de publicação são reprocessadas a partir do outbox; mensagens problemáticas isoladas na DLQ; cada consumidor evolui com sua própria fila.
- Negativas: entrega pelo menos uma vez (exige idempotência em todo consumidor); latência extra entre gravação e publicação; mais recursos SNS/SQS para gerenciar.

## Pontos em aberto
- **Onde roda o relay do outbox:** dentro de cada serviço ou como processo separado — pendente.

## Alternativas consideradas
- Relay dentro do serviço vs processo separado (ainda em aberto, ver acima). Outras alternativas: não registradas no planejamento.
