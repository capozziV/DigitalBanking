# 0015 — Relay do outbox com líder único e SNS FIFO

- **Status:** aceito
- **Data:** 2026-10-22

## Contexto
O [ADR 0003](0003-transactional-outbox-e-mensageria.md) definiu o Transactional Outbox, mas deixou em aberto onde roda o relay. A etapa 1 antecipa o outbox, o relay e o SNS da etapa 3. Os consumidores precisam receber os eventos de uma conta na ordem dos lançamentos (`sequence`), e o relay precisa continuar correto com mais de uma instância do serviço.

## Decisão
- **Relay no próprio serviço:** `OutboxRelay` fica em `libs/outbox-relay`, junto da sua configuração Spring, e é agendado por `@Scheduled` (`OutboxRelayScheduler`, intervalo padrão 500 ms) em cada serviço. A `libs/outbox` guarda só o envelope e a escrita no outbox.
- **Um relay ativo por vez**, por trava consultiva do PostgreSQL (`pg_try_advisory_xact_lock`) no início de cada ciclo. Sem a trava, a instância encerra o ciclo.
- **SNS FIFO** com `MessageGroupId` = `aggregateId` (a conta) e `MessageDeduplicationId` = `eventId`. A ordem é garantida por conta, não global.
- **Falha bloqueia só a conta:** a linha ganha `attempts + 1` e `last_error`, e os eventos seguintes da mesma conta esperam; as outras contas seguem. A espera cresce a cada falha: 1 s × 2^(n−1) antes da próxima tentativa, com teto de 30 s (configuráveis em `<prefixo>.outbox.retry-backoff` e `max-retry-backoff`). Ao chegar a 10 tentativas (configurável), sai um log `ERROR` uma vez por conta; as falhas seguintes saem como `WARN`. A contagem da espera fica em memória: um líder novo ou um reinício tenta de novo na hora. Conta em espera não ocupa o lote das outras.
- **Publicado e não confirmado** (queda antes do commit) sai de novo no ciclo seguinte; a deduplicação FIFO (5 min) e os consumidores idempotentes absorvem.
- **Outbox guardado para sempre:** o relay marca `published_at` e não apaga linhas.
- **Envelope com `eventVersion`**, JSON próprio: `eventId`, `eventType`, `eventVersion`, `occurredAt`, `aggregateId`, `correlationId`, `payload`.
- **LocalStack** como emulador AWS local e nos testes, com conta gratuita e `LOCALSTACK_AUTH_TOKEN`.
- Vários relays divididos por conta ficam para se a vazão exigir.
- **Desbloqueio manual por descarte** (rotas na `libs/outbox-relay`, presentes em `accounts`, `transfers` e `customer`; só `ADMIN`):
  - `GET /admin/outbox/stuck-events` lista os eventos presos.
  - `POST /admin/outbox/events/{eventId}/discard`, com motivo obrigatório, descarta o evento. Repetir o descarte não muda nada (204).
  - O evento descartado fica marcado com o sentinela `attempts = -1` e nunca é publicado: é perda deliberada, sem migration. A leitura do lote e o backlog ignoram `attempts < 0`, e os eventos seguintes da conta voltam a sair.
  - O descarte espera a trava consultiva do ciclo do relay. O relay usa `try_lock`, então não há deadlock: enquanto o descarte segura a trava, o ciclo só pula. Com o relay ocupado além da espera, a rota responde 503 `/problems/outbox-relay-busy`.
  - Não existe "tentar de novo", de propósito: o relay já tenta a cabeça presa a cada ciclo (no máximo 30 s) e após reinício ou troca de líder. Para evento venenoso, repetir não muda nada.
  - Como chamar: o nginx não roteia `/admin/outbox` e o mesmo caminho existe em três serviços. Chame direto a porta do serviço, com token de admin.

## Consequências
- Positivas: ordem por conta preservada mesmo com várias instâncias do serviço; falha de uma conta não para as demais; sem processo extra para operar; histórico completo dos eventos no outbox; conta presa por evento venenoso se destrava sem mexer no banco.
- Negativas: vazão limitada a um relay ativo por serviço; entrega pelo menos uma vez; o outbox cresce sem limite (linhas descartadas ficam no índice parcial `outbox_unpublished_idx`; volume irrelevante); dependência do suporte do LocalStack a SNS FIFO (validado no bloco 2 com a versão `2026.10.2`) e do token no CI.
- Negativas do descarte, por consumidor:
  - `statement`: buraco silencioso e permanente no extrato. O `PutItem` condicional não confere a `sequence` anterior, e os lançamentos seguintes entram normalmente. Isso quebra a premissa do [ADR 0018](0018-chave-do-extrato-pela-sequencia.md) (sequência sem buracos), sem alarme; o salto só aparece no `balanceAfter` da linha seguinte.
  - `notification` e `statement`, se o descartado for `AccountOpened`: todo evento posterior da conta vai para a DLQ no `notification`, e o extrato responde 404 ao dono.
  - Ainda é melhor que sem descarte, quando a cabeça presa trava todos os eventos seguintes do agregado.
  - Reconstruir o que foi descartado (corrigir e republicar) está fora do escopo.

## Alternativas consideradas
- **`FOR UPDATE SKIP LOCKED` com vários relays:** daria mais vazão, mas quebra a ordem por conta.
- **Relay como processo separado:** mais um componente por serviço, sem ganho para o volume do projeto.
- **CDC (captura de mudanças do banco):** infraestrutura pesada demais para o escopo.
- **Tópico SNS Standard:** não garante ordem por conta.
- **CloudEvents como envelope:** descartado em favor de um envelope próprio e mínimo.
