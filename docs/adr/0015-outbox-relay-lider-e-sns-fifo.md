# 0015 — Relay do outbox com líder único e SNS FIFO

- **Status:** aceito
- **Data:** 2026-10-22

## Contexto
O [ADR 0003](0003-transactional-outbox-e-mensageria.md) definiu o Transactional Outbox, mas deixou em aberto onde roda o relay. A etapa 1 antecipa o outbox, o relay e o SNS da etapa 3. Os consumidores precisam receber os eventos de uma conta na ordem dos lançamentos (`sequence`), e o relay precisa continuar correto com mais de uma instância do serviço.

## Decisão
- **Relay no próprio serviço** (`OutboxRelay`, em `libs/outbox`), agendado por `@Scheduled` no bootstrap de cada serviço (`OutboxRelayScheduler`, intervalo padrão 500 ms).
- **Um relay ativo por vez**, por trava consultiva do PostgreSQL (`pg_try_advisory_xact_lock`) no início de cada ciclo. Sem a trava, a instância encerra o ciclo.
- **SNS FIFO** com `MessageGroupId` = `aggregateId` (a conta) e `MessageDeduplicationId` = `eventId`. A ordem é garantida por conta, não global.
- **Falha bloqueia só a conta:** a linha ganha `attempts + 1` e `last_error`, e os eventos seguintes da mesma conta esperam o próximo ciclo; as outras contas seguem. Com `attempts` ≥ 10 (configurável), log `ERROR`.
- **Publicado e não confirmado** (queda antes do commit) sai de novo no ciclo seguinte; a deduplicação FIFO (5 min) e os consumidores idempotentes absorvem.
- **Outbox guardado para sempre:** o relay marca `published_at` e não apaga linhas.
- **Envelope com `eventVersion`**, JSON próprio: `eventId`, `eventType`, `eventVersion`, `occurredAt`, `aggregateId`, `correlationId`, `payload`.
- **LocalStack** como emulador AWS local e nos testes, com conta gratuita e `LOCALSTACK_AUTH_TOKEN`.
- Vários relays divididos por conta ficam para se a vazão exigir.

## Consequências
- Positivas: ordem por conta preservada mesmo com várias instâncias do serviço; falha de uma conta não para as demais; sem processo extra para operar; histórico completo dos eventos no outbox.
- Negativas: vazão limitada a um relay ativo por serviço; conta com evento de falha permanente fica presa (desbloqueio manual é débito técnico); entrega pelo menos uma vez; o outbox cresce sem limite; dependência do suporte do LocalStack a SNS FIFO (validado no bloco 2 com a versão `2026.10.2`) e do token no CI.

## Alternativas consideradas
- **`FOR UPDATE SKIP LOCKED` com vários relays:** daria mais vazão, mas quebra a ordem por conta.
- **Relay como processo separado:** mais um componente por serviço, sem ganho para o volume do projeto.
- **CDC (captura de mudanças do banco):** infraestrutura pesada demais para o escopo.
- **Tópico SNS Standard:** não garante ordem por conta.
- **CloudEvents como envelope:** descartado em favor de um envelope próprio e mínimo.
