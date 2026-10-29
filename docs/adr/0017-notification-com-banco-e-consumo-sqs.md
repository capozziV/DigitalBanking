# 0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS

- **Status:** aceito
- **Data:** 2026-10-28

## Contexto
O `notification` é o primeiro consumidor de eventos. Ele lê o `accounts` e o `transfers` em filas SQS FIFO e grava notificações dentro do aplicativo, que o cliente consulta pela API. A arquitetura previa o serviço "sem banco", mas ele precisa guardar as notificações, saber de quem é cada conta e não aplicar o mesmo evento duas vezes (a SQS entrega pelo menos uma vez). Detalhes na [spec da etapa 3](../superpowers/specs/2026-10-28-etapa-3-mensageria-design.md).

## Decisão
- **Banco PostgreSQL próprio** (`notification`, no mesmo contêiner dos outros), com `notifications`, `known_accounts` (conta → cliente, alimentada pelo `AccountOpened`) e `processed_events`. Persistência com `JdbcClient` ([ADR 0013](0013-persistencia-com-spring-jdbc.md)).
- **Consumo com `@SqsListener` do Spring Cloud AWS**, com confirmação no sucesso (`ON_SUCCESS`), em vez de um poller próprio com o AWS SDK como o do relay ([ADR 0015](0015-outbox-relay-lider-e-sns-fifo.md)).
- **Idempotência por `processed_events`:** o `eventId` é gravado na mesma transação que as notificações; se já existe, o evento não tem efeito. A mensagem só é confirmada depois do commit. `UNIQUE (source_event_id, customer_id, kind)` fica como rede de segurança.
- **Uma fila FIFO por tópico, cada uma com DLQ FIFO** (`notification-accounts.fifo` e `notification-transfers.fifo`; DLQs `notification-accounts-dlq.fifo` e `notification-transfers-dlq.fifo`). A política de redirecionamento manda a mensagem para a DLQ na 5ª tentativa; visibilidade de 30 s. Os dois valores são configuráveis.

## Consequências
- Positivas: a idempotência é atômica com o efeito (queda entre commit e confirmação não duplica nada); o consumo ganha polling, concorrência e retentativa prontos; o `notification` fica independente do `accounts` e do `transfers` em tempo de execução; uma mensagem presa segura só o seu grupo FIFO.
- Negativas: mais um banco para operar e migrar; `processed_events` cresce sem limite (limpeza fica para depois); dependência nova, o Spring Cloud AWS; conta ainda desconhecida faz o evento voltar à fila até o `AccountOpened` chegar (filas diferentes não têm ordem entre si).
- **Compatibilidade:** o Spring Cloud AWS 4.2.1 é compilado contra o Spring Boot 4.0.7 e o AWS SDK 2.57.0; o projeto usa o Boot 4.1.1 e o SDK 2.57.1. Um teste de integração com LocalStack confirmou o `@SqsListener` do Spring Cloud AWS 4.2.1 com Spring Boot 4.1.1 numa fila FIFO: ordem por grupo e retorno da mensagem que falhou depois da visibilidade. A tabela oficial de compatibilidade do Spring Cloud AWS 4.x cita só o Boot 4.0.x; esse é um risco aceito.

## Alternativas consideradas
- **Sem banco, estado em memória ou no Redis:** perde notificações e a deduplicação numa queda, e o Redis não é fonte da verdade no projeto.
- **Poller próprio com o AWS SDK:** controle total e nenhuma dependência nova, mas o projeto reescreveria polling, concorrência, backoff e confirmação sem ganho de portfólio.
- **Deduplicar só pela FIFO (`MessageDeduplicationId`):** a janela é de 5 minutos e não cobre entrega repetida depois da leitura.
- **Consultar o `accounts` por HTTP para achar o cliente da conta:** acopla o consumidor ao produtor e derruba o consumo quando o `accounts` cai.
