# Etapa 6 — limite diário: design

- **Data:** 2026-11-12
- **Status:** aprovada pelo usuário em 2026-11-12
- **Roadmap:** etapa 6, "Redis + Lua, reserva idempotente, reconstrução a partir do PostgreSQL".
- **ADR base:** [0010 — Limite diário com Redis + Lua](../../adr/0010-limite-diario-redis-lua.md).

## Objetivo
Uma conta não transfere mais que o limite diário num mesmo dia. A conferência é atômica sob concorrência: duas transferências simultâneas não passam juntas do limite. Retentativas da saga não consomem o limite duas vezes. Se o Redis perder os dados ou cair, o limite continua valendo pelo PostgreSQL.

## Decisões desta etapa (usuário, 2026-11-12)
1. **Passo novo da saga, `ReserveLimit`, antes do débito.** Se o limite estourar, a transferência termina `FAILED` com o motivo `daily-limit-exceeded`, como a recusa por saldo.
2. **Dia do calendário em `America/Sao_Paulo`.** O dia da reserva é o dia em que a transferência foi pedida (`created_at`), fixo para ela.
3. **A reserva conta enquanto a transferência está `PENDING`**, é consumida em `COMPLETED` e liberada em `FAILED` e `COMPENSATED`.
4. **Reconstrução sob demanda:** o contador ausente no Redis é recalculado do PostgreSQL.
5. **Redis fora:** a conferência é feita no PostgreSQL, com trava por conta e dia. Para não abrir brecha entre as duas vias, **toda reserva passa pela mesma trava no PostgreSQL** e a via do PostgreSQL marca o contador do Redis como desatualizado (seção 4).
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
- **`seed(accountId, day, epoch, reservations, ttl)`:** cria o hash (com o campo `epoch`) a partir do PostgreSQL só se a chave não existir (`MISSING` → recalcula → `seed` → `reserve` de novo).
- **TTL da chave: 10 minutos**, renovado a cada `reserve`. Ele só limpa a chave de uma conta sem reservas nesse prazo; quem corrige divergência é o epoch (seção 4): o `release` que falha no Redis incrementa o epoch, e a próxima reserva reconstrói o contador do PostgreSQL.
- Timeout de 100 ms nos comandos, como no `statement`.

## 3. Passo `ReserveLimit` da saga
- Novo passo inicial: `ReserveLimit` → `Debit`. Ele não chama o `accounts`; o `AccountsCall` ganha um irmão, ou o passo ganha uma chamada própria `LimitCall.Reserve`, a definir no plano, mantendo o padrão State da saga.
- **Resultados:**
  - reservou → `Debit`, e a transferência grava `limit_reserved = true` na mesma transação do passo;
  - excedeu → `Failed`, motivo `daily-limit-exceeded`;
  - Redis fora → fallback no PostgreSQL (seção 4); se o PostgreSQL também falhar, falha temporária, com retentativa até o prazo da saga e, depois do prazo, `Failed` com `daily-limit-unavailable`.
- **Liberação:** ao chegar em `Failed` ou `Compensated` com `limit_reserved = true`, a saga chama `release` no Redis depois de salvar o estado terminal. É só melhor esforço: uma falha vira WARN e incrementa o epoch, e a próxima reserva reconstrói o contador.
- `Completed` não libera nada: a reserva vira consumo.
- Transferências antigas, em `PENDING` no passo `Debit` sem reserva, seguem como estão: o passo inicial só vale para as novas.

## 4. Trava única e fallback no PostgreSQL
- **Trava por conta e dia:** toda reserva, pela via do Redis ou do PostgreSQL, roda numa transação que começa com `pg_advisory_xact_lock(hash(accountId, day))`. É uma trava do próprio PostgreSQL, sem tabela e sem fila externa: quem chega enquanto outra reserva da mesma conta e dia está aberta espera o `commit` (milissegundos). Contas e dias diferentes não se esperam.
- **Via do Redis**, dentro da transação travada:
  1. lê `daily_limit_epoch` da conta e do dia (seção 1);
  2. se o epoch for diferente do gravado no hash do Redis (campo `epoch`), apaga a chave e reconstrói do PostgreSQL;
  3. roda o `reserve` do Lua;
  4. se reservou, grava `limit_reserved = true` na transferência;
  5. faz o `commit`, que solta a trava.

  Como a reserva entra no PostgreSQL antes de a trava soltar, a outra via sempre a enxerga.
- **Via do PostgreSQL (Redis fora)**, na mesma transação travada:
  1. soma as reservas (seção 1);
  2. se couber, grava `limit_reserved = true`;
  3. incrementa `daily_limit_epoch` da conta e do dia, que é o aviso de que o contador do Redis ficou velho.

  Quando o Redis voltar, a primeira reserva vê o epoch diferente e reconstrói o contador antes de conferir.
- **Resultado:** sem brecha entre as vias. Reserva desfeita depois do Lua ou liberação que falhou deixam o contador acima do real, o lado seguro, e o epoch faz a próxima reserva reconstruí-lo. Risco aceito: perda parcial de escritas no Redis (failover assíncrono, snapshot antigo) pode deixar o contador abaixo do real com o mesmo epoch; mitigação futura no cartão #160.
- Tabela `daily_limit_epoch (account_id uuid, limit_day date, epoch bigint, primary key (account_id, limit_day))`. Sem linha, o epoch é 0.

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
  - concorrência mista: reservas pelo Redis e pelo PostgreSQL ao mesmo tempo nunca passam do limite;
  - Redis volta depois de reservas pelo PostgreSQL: o epoch força a reconstrução e o limite não é passado;
  - liberação ao falhar por saldo e ao compensar;
  - endpoint.
- **e2e:** duas transferências que juntas passam de R$ 5.000; a segunda termina `FAILED` com `daily-limit-exceeded`, e a notificação de transferência recusada chega.
- **Mutantes a rodar na revisão:**
  - sem a checagem de idempotência no Lua, o teste da retentativa fica vermelho;
  - sem a liberação em `Failed`/`Compensated`, o teste da liberação fica vermelho;
  - sem a trava no fallback, o teste de concorrência do fallback fica vermelho;
  - sem a comparação do epoch, o teste "Redis volta depois de reservas pelo PostgreSQL" fica vermelho.

## 8. Ambiente local, CI e documentação
- Variáveis `TRANSFERS_REDIS_HOST`, `TRANSFERS_REDIS_PORT` e `TRANSFERS_DAILY_LIMIT` no `.env.example` e no guia local.
- `libs/testing-redis`: o apoio de Redis do `statement` vira lib compartilhada, agora com o segundo usuário (ruling da etapa 5).
- ADR 0010 atualizado: dia em São Paulo, TTL do contador, trava única no PostgreSQL, epoch e fallback.
- O motivo `daily-limit-exceeded` aparece na notificação de transferência recusada, se o `notification` mostrar o motivo.

## Fora de escopo
- Limite por tipo de conta: depois do MVP.
- Aumento de limite pelo cliente: depois do MVP.
- Limite por transação ou por horário (noturno): não previsto.

## Riscos
- **Trava no PostgreSQL em toda reserva:** custa uma transação curta por transferência, que o passo já abriria para gravar o estado. Contenção só entre transferências da mesma conta no mesmo dia.
- **Migração em tabela com dados:** as colunas novas têm padrão; as transferências antigas ficam com `limit_reserved = false` e não contam.
- **Lua no LocalStack/ElastiCache:** o ElastiCache aceita `EVAL`/`EVALSHA`; o teste roda contra o `redis:7-alpine`.
