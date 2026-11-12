# Etapa 6 — limite diário: design

- **Data:** 2026-11-12
- **Status:** aguardando revisão do usuário
- **Roadmap:** etapa 6, "Redis + Lua, reserva idempotente, reconstrução a partir do PostgreSQL".
- **ADR base:** [0010 — Limite diário com Redis + Lua](../../adr/0010-limite-diario-redis-lua.md).

## Objetivo
Uma conta não transfere mais que o limite diário num mesmo dia. A conferência é atômica sob concorrência: duas transferências simultâneas não passam juntas do limite. Retentativas da saga não consomem o limite duas vezes. Se o Redis perder os dados ou cair, o limite continua valendo pelo PostgreSQL.

## Decisões desta etapa (usuário, 2026-11-12)
1. **Passo novo da saga, `ReserveLimit`, antes do débito.** Se o limite estourar, a transferência termina `FAILED` com o motivo `DAILY_LIMIT_EXCEEDED`, como a recusa por saldo.
2. **Dia do calendário em `America/Sao_Paulo`.** O dia da reserva é o dia em que a transferência foi pedida (`created_at`), fixo para ela.
3. **A reserva conta enquanto a transferência está `PENDING`**, é consumida em `COMPLETED` e liberada em `FAILED` e `COMPENSATED`.
4. **Reconstrução sob demanda:** o contador ausente no Redis é recalculado do PostgreSQL.
5. **Redis fora:** a conferência é feita no PostgreSQL, com trava por conta e dia.
6. **Endpoint `GET /accounts/{accountId}/daily-limit`** no `transfers`, já nesta etapa.

Valor do limite: R$ 5.000,00 = `500000` centavos para todas as contas (ADR 0010, #19), configurável por `transfers.daily-limit.amount`.

## Critério de pronto
- Build completo verde (`./gradlew build`, com ktlint, unidade, integração, ArchUnit e e2e).
- Smoke e Bruno das quatro coleções verdes, com a coleção do `transfers` cobrindo a recusa por limite e o endpoint novo.
- Testes de integração provam a concorrência, a idempotência, a liberação, a reconstrução e o fallback.
- Revisão de cada task e revisão final da branch pela Beatriz, com mutantes vermelhos.

## Restrições (de `CLAUDE.md` e ADRs)
- Valores em centavos `Long` (`Money`).
- Clean Architecture: a porta do limite fica no `application`; Redis e SQL no `adapters`; o `domain` não conhece Redis.
- Código, testes e logs em inglês, com os termos do `CONTEXT.md`: `DailyLimit`, `LimitReservation`.
- O PostgreSQL do `transfers` é a fonte da verdade (ADR 0010).

## 1. Fonte da verdade no PostgreSQL
- A tabela `transfer` ganha `limit_day date` (dia em `America/Sao_Paulo`, preenchido no pedido) e `limit_reserved boolean not null default false`.
- **Reservas de uma conta num dia** = transferências com `source_account_id` da conta, `limit_day` igual ao dia e `limit_reserved = true`, com `status` em `PENDING` ou `COMPLETED`. `FAILED` e `COMPENSATED` não contam, e isso é a liberação na fonte da verdade.
- Índice `(source_account_id, limit_day)` para essa soma.
- Não há tabela nova: o estado da transferência já diz se a reserva vale.

## 2. Contador no Redis (script Lua)
- **Redis com Lua:** o Redis é um banco em memória. O script Lua roda dentro dele de forma indivisível, então "conferir o limite e reservar" acontece num passo só, sem que outra transferência se meta no meio.
- Chave `daily-limit:v1:{accountId}:{yyyy-MM-dd}`, um hash com `total` (centavos reservados) e um campo por `transferId` com o valor.
- **`reserve(accountId, day, transferId, amount, limit)`:**
  - campo do `transferId` já existe → `RESERVED`, idempotente;
  - chave ausente → `MISSING`;
  - `total + amount > limit` → `EXCEEDED`;
  - senão grava o campo, soma no `total` → `RESERVED`.
- **`release(accountId, day, transferId)`:** apaga o campo e subtrai o valor, se o campo existir; senão não faz nada.
- **`seed(accountId, day, reservations, ttl)`:** cria o hash a partir do PostgreSQL só se a chave não existir (`MISSING` → recalcula → `seed` → `reserve` de novo).
- **TTL da chave: 10 minutos**, renovado a cada `reserve`. Depois disso o contador é reconstruído do PostgreSQL, o que corrige qualquer divergência: liberação que falhou no Redis ou reserva feita pelo fallback.
- Timeout de 100 ms nos comandos, como no `statement`.

## 3. Passo `ReserveLimit` da saga
- Novo passo inicial: `ReserveLimit` → `Debit`. Ele não chama o `accounts`; o `AccountsCall` ganha um irmão, ou o passo ganha uma chamada própria `LimitCall.Reserve`, a definir no plano, mantendo o padrão State da saga.
- **Resultados:**
  - reservou → `Debit`, e a transferência grava `limit_reserved = true` na mesma transação do passo;
  - excedeu → `Failed`, motivo `DAILY_LIMIT_EXCEEDED`;
  - Redis fora → fallback no PostgreSQL (seção 4); se o PostgreSQL também falhar, falha temporária, com retentativa até o prazo da saga e, depois do prazo, `Failed` com `LIMIT_UNAVAILABLE`.
- **Liberação:** ao chegar em `Failed` ou `Compensated` com `limit_reserved = true`, a saga chama `release` no Redis depois de salvar o estado terminal. É só melhor esforço: uma falha vira WARN, e o TTL de 10 min com a reconstrução corrige o contador.
- `Completed` não libera nada: a reserva vira consumo.
- Transferências antigas, em `PENDING` no passo `Debit` sem reserva, seguem como estão: o passo inicial só vale para as novas.

## 4. Fallback no PostgreSQL (Redis fora)
- Numa transação: `pg_advisory_xact_lock` da conta e do dia, soma das reservas (seção 1) e, se couber, `limit_reserved = true` na transferência.
- As duas vias podem rodar ao mesmo tempo em instâncias diferentes, uma com o Redis e outra sem. Nesse caso uma reserva do Redis ainda não gravada no PostgreSQL não entra na soma do fallback, e o limite pode ser passado por pouco. **É aceito e registrado no ADR 0010:** é uma janela de milissegundos durante uma queda parcial do Redis, num banco simulado, e o TTL de 10 min realinha o contador.

## 5. Endpoint de consulta
- `GET /accounts/{accountId}/daily-limit` → `200` com `DailyLimitDTO { day, limitCents, usedCents, availableCents }`.
- `usedCents` vem do contador no Redis, com reconstrução se faltar; com o Redis fora, vem da soma no PostgreSQL.
- Conta desconhecida → `200` com `usedCents = 0`. O `transfers` não guarda contas, e consultar o `accounts` só para isso não vale a chamada. Fica documentado no OpenAPI.

## 6. Observabilidade
- Contador `transfers.daily_limit.reservations`, com a tag `result` = `reserved`, `exceeded`, `fallback` ou `error`.
- Contador `transfers.daily_limit.rebuilds`.
- WARN com o correlation ID ao cair no fallback e ao falhar a liberação.

## 7. Testes
- **Unidade:**
  - transições do `ReserveLimit`;
  - cálculo do dia em São Paulo, inclusive perto da meia-noite UTC;
  - liberação só com `limit_reserved`.
- **Integração (Redis e PostgreSQL em Testcontainers):**
  - script Lua: reserva, idempotência por `transferId`, excesso, liberação, chave ausente;
  - **concorrência:** N transferências simultâneas de uma conta somando mais que o limite e só as que cabem reservam;
  - reconstrução depois de apagar a chave;
  - fallback com o Redis pausado, incluindo a trava por conta;
  - liberação ao falhar por saldo e ao compensar;
  - endpoint.
- **e2e:** duas transferências que juntas passam de R$ 5.000; a segunda termina `FAILED` com `DAILY_LIMIT_EXCEEDED`, e a notificação de transferência recusada chega.
- **Mutantes a rodar na revisão:**
  - sem a checagem de idempotência no Lua, o teste da retentativa fica vermelho;
  - sem a liberação em `Failed`/`Compensated`, o teste da liberação fica vermelho;
  - sem a trava no fallback, o teste de concorrência do fallback fica vermelho.

## 8. Ambiente local, CI e documentação
- Variáveis `TRANSFERS_REDIS_HOST`, `TRANSFERS_REDIS_PORT` e `TRANSFERS_DAILY_LIMIT` no `.env.example` e no guia local.
- `libs/testing-redis`: o apoio de Redis do `statement` vira lib compartilhada, agora com o segundo usuário (ruling da etapa 5).
- ADR 0010 atualizado: dia em São Paulo, TTL do contador, fallback no PostgreSQL e a corrida aceita.
- O motivo `DAILY_LIMIT_EXCEEDED` aparece na notificação de transferência recusada, se o `notification` mostrar o motivo.

## Fora de escopo
- Limite por tipo de conta: depois do MVP.
- Aumento de limite pelo cliente: depois do MVP.
- Limite por transação ou por horário (noturno): não previsto.

## Riscos
- **Corrida entre Redis e fallback:** a seção 4 traz a mitigação, com o limite passado por pouco e só durante uma queda parcial.
- **Migração em tabela com dados:** as colunas novas têm padrão; as transferências antigas ficam com `limit_reserved = false` e não contam.
- **Lua no LocalStack/ElastiCache:** o ElastiCache aceita `EVAL`/`EVALSHA`; o teste roda contra o `redis:7-alpine`.
