# Etapa 6 — limite diário: plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** limite diário de transferência por conta, reservado num passo `ReserveLimit` da saga, com o contador em Redis + Lua e a fonte da verdade no PostgreSQL do `transfers`, sob uma trava única por conta e dia.

**Architecture:** o passo `ReserveLimit` pede ao `application` uma chamada `StepCall.ReserveLimit`. O runner da saga a despacha para a porta `DailyLimitGateway`, e não para o `accounts`. O adapter `RedisDailyLimitGateway` abre a própria transação JDBC com `pg_advisory_xact_lock`, confere o epoch, roda o Lua e grava `limit_reserved`. Com o Redis fora, soma no PostgreSQL e incrementa o epoch. A liberação é de melhor esforço, depois do estado terminal.

**Tech Stack:** Kotlin, Spring Boot 4.1.1, Spring Data Redis (Lettuce) com `RedisScript` Lua, JDBC/Flyway, Micrometer, Testcontainers (`redis:7-alpine`, PostgreSQL).

**Spec:** [`docs/superpowers/specs/2026-11-12-etapa-6-limite-diario-design.md`](../specs/2026-11-12-etapa-6-limite-diario-design.md)

## Global Constraints
- Valores em centavos (`Money`, `Long`). Limite padrão `500000`, configurável por `transfers.daily-limit.amount` / `TRANSFERS_DAILY_LIMIT`.
- Dia: `LocalDate.ofInstant(createdAt, ZoneId.of("America/Sao_Paulo"))`, fixo na transferência (`limitDay`).
- Chave Redis `daily-limit:v1:{accountId}:{yyyy-MM-dd}`, hash com os campos `total`, `epoch` e um por `transferId` (valor em centavos). TTL de 10 min, renovado a cada `reserve`.
- Trava: `select pg_advisory_xact_lock(hashtextextended(:accountId || ':' || :day, 0))` no começo da transação de toda reserva, pela via do Redis ou do PostgreSQL.
- Motivos públicos: `daily-limit-exceeded` e `daily-limit-unavailable`, no estilo kebab dos motivos existentes em `StepOutcome`.
- Redis: `spring.data.redis.host: ${TRANSFERS_REDIS_HOST:localhost}`, `port: ${TRANSFERS_REDIS_PORT:6379}`, `timeout` e `connect-timeout: ${TRANSFERS_REDIS_TIMEOUT:100ms}`, `management.health.redis.enabled: false`.
- Métricas: `transfers.daily_limit.reservations{result=reserved|exceeded|fallback|error}` e `transfers.daily_limit.rebuilds`.
- Clean Architecture: `domain` sem Spring nem Redis; a porta `DailyLimitGateway` no `application`; Redis e SQL no `adapters` (ArchUnit).
- Tabela nova do `transfers` precisa de `grant` próprio para o usuário da aplicação (`${appUser}`), como no `V4__app_grants.sql`.
- Código, testes e logs em inglês; termos do `CONTEXT.md` (`DailyLimit`, `LimitReservation`). Testes terminam em `Test` ou `IntegrationTest`.
- Agentes em worktree não regeneram nem commitam `graphify-out/`.
- Pronto = `./gradlew build` verde, com a saída real no relato.

## Rulings deste plano
- Os motivos ficam em kebab-case (`daily-limit-exceeded`), como `insufficient-available-balance`. A spec escreve `DAILY_LIMIT_EXCEEDED` só como nome do conceito. Custo se errado: renomear uma constante.
- `AccountsCall` vira `StepCall`, com os casos de hoje mais `ReserveLimit`. É uma renomeação mecânica, mas deixa claro que nem todo passo chama o `accounts`. Custo se errado: diff maior na Task 2.
- A reserva grava `limit_reserved` na transação do adapter, sob a trava. Depois, a saga salva o passo seguinte com `limitReserved = true` em outra transação: a coluna já está verdadeira, e o save só repete o valor.
- `ReserveLimit` com falha temporária até o prazo → `Failed` com `daily-limit-unavailable`. Isso pede um ajuste no `Transfer.advance`, que hoje só preenche o motivo em `Refused`.
- Transferências antigas em `PENDING` (passo `DEBIT`, `limit_reserved = false`) seguem sem reserva.
- `libs/testing-redis` nasce agora, porque o `transfers` é o segundo usuário (ruling da etapa 5).

## Review Focus
1. Transferências simultâneas da mesma conta, que somadas passam do limite: só as que cabem reservam (Task 3).
2. Retentativa do passo depois de uma reserva já feita: não consome o limite de novo (Task 3, Lua e adapter).
3. Redis volta depois de reservas feitas pelo PostgreSQL: o epoch força a reconstrução e o limite não é passado (Task 3).
4. Transferência pedida às 23h59 de São Paulo, que é o dia seguinte em UTC: conta no dia de São Paulo (Task 2).
5. Falha por saldo e compensação depois da reserva: o limite volta (Task 2 na unidade, Task 3 na integração).

## Ordem e paralelismo
- **Onda 1 (paralelas):** Task 1 (Rafael) e Task 2 (Marina). Arquivos disjuntos.
- **Onda 2:** Task 3 (Marina), depois das Tasks 1 e 2 revisadas.
- **Onda 3:** Task 4 (Marina), depois da Task 3 revisada.
- Worktrees nascem de `etapa-6-limite`.

---

### Task 1: `libs/testing-redis` e ambiente

**Files:**
- Create: `libs/testing-redis/` (`build.gradle.kts`, `src/main/kotlin/io/github/capozziv/digitalbanking/testing/redis/RedisTestSupport.kt`), com `fun container(): GenericContainer<*>` (`redis:7-alpine`, porta 6379), `fun pause(container)` e `fun unpause(container)`.
- Modify: `settings.gradle.kts` (inclui `:libs:testing-redis`).
- Modify: `services/statement/bootstrap/build.gradle.kts` e `StatementIntegrationTest.kt`, para usar a lib no lugar do `GenericContainer` local; os testes do `statement` que pausam o Redis passam a usar `pause`/`unpause`.
- Modify: `infra/local/.env.example` (`TRANSFERS_REDIS_HOST=localhost`, `TRANSFERS_REDIS_PORT=6379`, `# TRANSFERS_REDIS_TIMEOUT=100ms`, `# TRANSFERS_DAILY_LIMIT=500000`) e `docs/rodando-localmente.md` (variáveis do `transfers`).
- Modify: `tests/e2e/.../EndToEndEnvironment.kt` (`transfersEnvironment()` recebe `TRANSFERS_REDIS_HOST`/`PORT` do Redis que o e2e já sobe).
- Modify: o job `libs` do `.github/workflows/ci.yml`, se ele lista as libs explicitamente.

- [ ] **Step 1:** teste `RedisTestSupportIntegrationTest`: o contêiner sobe, responde `PING` e, pausado, não responde em 1 s.
- [ ] **Step 2:** lib e migração do `statement`; `./gradlew :libs:testing-redis:build :services:statement:bootstrap:build` verde.
- [ ] **Step 3:** `./gradlew build` completo verde. As variáveis novas ainda não têm efeito no `transfers`.
- [ ] **Step 4:** commit `build: libs/testing-redis e variáveis do limite diário`.

### Task 2: domínio, saga e migração

**Files:**
- Modify (domínio): `Transfer.kt` (campos `limitDay: LocalDate` e `limitReserved: Boolean`; `request` começa em `TransferStep.ReserveLimit` e calcula `limitDay` com `DailyLimitDay.of(now)`), `TransferStep.kt` (novo `ReserveLimit`, com tabela de transições na KDoc), `AccountsCall.kt` → `StepCall.kt` (mais `ReserveLimit(accountId, day, transferId, amount)`), `StepOutcome.kt` (`DAILY_LIMIT_EXCEEDED = "daily-limit-exceeded"`, `DAILY_LIMIT_UNAVAILABLE = "daily-limit-unavailable"`).
- Create (domínio): `DailyLimitDay.kt` (`object DailyLimitDay { val ZONE: ZoneId; fun of(instant: Instant): LocalDate }`).
- Create (application): `port/DailyLimitGateway.kt`.
- Modify (application): `RunTransferSagaUseCase.kt` (despacha `StepCall.ReserveLimit` para o gateway; depois de salvar `Failed`/`Compensated` com `limitReserved`, chama `release`; a falha vira WARN com o correlation ID), `AccountsGateway.perform(call)` passa a receber só os casos do `accounts`.
- Create (adapters): `adapters/dailylimit/UnlimitedDailyLimitGateway.kt`, com `@Component` temporário até a Task 3: sempre `RESERVED` e `usage` zero.
- Create: `services/transfers/adapters/src/main/resources/db/migration/V6__daily_limit.sql`, com:
  - `limit_day date` na tabela `transfer`, com backfill pelo `created_at` em São Paulo e depois `not null`;
  - `limit_reserved boolean not null default false`;
  - índice `(source_account_id, limit_day)`;
  - `RESERVE_LIMIT` no check de `step`, se o V5 listar os passos;
  - tabela `daily_limit_epoch (account_id uuid, limit_day date, epoch bigint not null, primary key (account_id, limit_day))`;
  - `grant select, insert, update on daily_limit_epoch to ${appUser}`.
- Modify: `JdbcTransferRepository.kt` (mapeia as colunas novas), fixtures e fakes de teste.

**Interfaces:**
- Produces:
  ```kotlin
  enum class ReservationResult { RESERVED, EXCEEDED, UNAVAILABLE }
  data class DailyLimitUsage(val day: LocalDate, val limit: Money, val used: Long, val available: Long)
  interface DailyLimitGateway {
      /** Idempotent by transferId; holds the per-account-day lock; marks the transfer's limit_reserved on RESERVED. */
      fun reserve(transferId: UUID, accountId: UUID, day: LocalDate, amount: Money): ReservationResult
      /** Best effort; never throws for a Redis failure. */
      fun release(transferId: UUID, accountId: UUID, day: LocalDate)
      fun usage(accountId: UUID, day: LocalDate): DailyLimitUsage
  }
  sealed interface StepCall { /* Debit, Credit, FindEntry (como hoje) */ data class ReserveLimit(val accountId: UUID, val day: LocalDate, val transferId: UUID, val amount: Money) : StepCall }
  ```
- `ReserveLimit`: `RESERVED` → `Success` → `Debit`; `EXCEEDED` → `Refused(daily-limit-exceeded)` → `Failed`; `UNAVAILABLE` → `TransientFailure` → `ReserveLimit` antes do prazo e `Failed` com `daily-limit-unavailable` depois dele.

- [ ] **Step 1: testes que falham:**
  - domínio: `a new transfer starts reserving the limit`; `the limit day is the Sao Paulo calendar day` (`2026-11-13T02:59:59Z` → `2026-11-12`; `2026-11-13T03:00:00Z` → `2026-11-13`); transições do `ReserveLimit`, incluindo o motivo `daily-limit-unavailable` depois do prazo;
  - `RunTransferSagaUseCaseTest`, com um `InMemoryDailyLimitGateway`:
    - `an exceeded limit fails the transfer without calling accounts`;
    - `a failed debit after a reservation releases the limit`;
    - `a compensated transfer releases the limit`;
    - `a completed transfer keeps the reservation`;
    - `a release failure does not change the terminal transfer`;
  - `JdbcTransferRepositoryIntegrationTest`: as colunas novas fazem ida e volta.
- [ ] **Step 2:** FAIL.
- [ ] **Step 3:** implementar.
- [ ] **Step 4:** `./gradlew :services:transfers:domain:build :services:transfers:application:build :services:transfers:adapters:build :services:transfers:bootstrap:build` verde; `./gradlew :tests:e2e:integrationTest` verde (com o gateway sem limite).
- [ ] **Step 5:** commit `feat(transfers): passo ReserveLimit na saga e colunas do limite diário`.

### Task 3: adapter Redis + Lua, trava, epoch e fallback

**Files:**
- Create em `services/transfers/adapters/src/main/kotlin/.../transfers/adapters/dailylimit/`:
  - `RedisDailyLimitGateway.kt` (implementa `DailyLimitGateway`);
  - `DailyLimitScripts.kt` (carrega `reserve.lua`, `release.lua` e `seed.lua` de `src/main/resources/dailylimit/`);
  - `JdbcDailyLimitStore.kt` (trava, soma das reservas, epoch, `limit_reserved`);
  - `DailyLimitProperties.kt` (`@ConfigurationProperties("transfers.daily-limit")`: `amount: Long = 500000`, `counterTtl: Duration = 10m`).
- Modify: `UnlimitedDailyLimitGateway.kt` (remove, ou tira o `@Component`), `application.yml`, os `build.gradle.kts` do `transfers` (`spring-boot-starter-data-redis`, `micrometer-core`; `integrationTestImplementation(project(":libs:testing-redis"))`), a base de integração do `transfers` (Redis da lib).
- Create: `.../bootstrap/src/integrationTest/.../dailylimit/RedisDailyLimitGatewayIntegrationTest.kt` e `DailyLimitSagaIntegrationTest.kt`.

**Algoritmo do `reserve`** (uma transação JDBC):
1. trava (Global Constraints);
2. `epoch` = linha de `daily_limit_epoch`, ou 0 se não houver;
3. Redis: `HGET key epoch`. Ausente ou diferente → `DEL`, soma no PostgreSQL (`transferId → amount` das reservas) e `seed` com o epoch (`rebuilds` += 1);
4. `reserve.lua` → `RESERVED` (novo ou idempotente) ou `EXCEEDED`;
5. em `RESERVED`, `update transfer set limit_reserved = true where id = ?`;
6. commit.

Qualquer erro do Redis nos passos 3 e 4 → **fallback** na mesma transação travada: soma no PostgreSQL. Se a transferência já estiver em `limit_reserved`, é `RESERVED` (idempotente). Se couber, `limit_reserved = true` e `epoch + 1` (upsert). Senão, `EXCEEDED`. Contadores `fallback`/`exceeded`; WARN só com a mensagem. Erro do PostgreSQL → `UNAVAILABLE`, contador `error`.

**Lua (`reserve.lua`):** `KEYS[1]`; `ARGV` = `transferId`, `amount`, `limit`, `ttlSeconds`.
- campo do `transferId` existe → `PEXPIRE` e `1`;
- `total + amount > limit` → `0`;
- senão `HSET`, `HINCRBY total`, `EXPIRE` e `1`.

`release.lua`: `HGET` do campo; se existir, `HDEL` e `HINCRBY total -amount`. `seed.lua`: só se `EXISTS` = 0, `HSET` do `epoch`, do `total` e dos campos, mais `EXPIRE`.

- [ ] **Step 1: testes que falham** (`RedisDailyLimitGatewayIntegrationTest`, com o Redis da lib e o PostgreSQL da base):
  - `reserves while under the limit and refuses above it` (limite 500000: 300000 reserva, 300000 recusa, 200000 reserva);
  - `the same transfer reserved twice counts once`;
  - `concurrent reservations never exceed the limit` (20 threads × 50000 numa conta com limite 500000 → exatamente 10 `RESERVED`; soma no PostgreSQL = 500000);
  - `release returns the amount and is idempotent`;
  - `a missing key is rebuilt from postgres`;
  - `with redis paused, reservations fall back to postgres and bump the epoch`;
  - `mixed paths never exceed the limit` (metade das threads com o Redis pausado entre lotes);
  - `after reservations through postgres, the redis counter is rebuilt before deciding` (pausa, reserva 400000 pelo PostgreSQL, retoma, tenta 200000 → `EXCEEDED`);
  - `usage reports limit, used and available` (com Redis e sem Redis).
  - `DailyLimitSagaIntegrationTest`: `POST /transfers` de 300000 duas vezes na mesma conta → a 2ª fica `FAILED` com `daily-limit-exceeded`; uma falha por saldo depois da reserva devolve o limite.
- [ ] **Step 2:** FAIL.
- [ ] **Step 3:** implementar.
- [ ] **Step 4:** `./gradlew build` completo verde.
- [ ] **Step 5:** commit `feat(transfers): limite diário com Redis + Lua, trava e fallback no PostgreSQL`.

### Task 4: endpoint, e2e, smoke e Bruno

**Files:**
- Modify: `TransferController.kt`, ou novo `DailyLimitController.kt` em `adapters/web/`, com `GET /accounts/{accountId}/daily-limit` → `DailyLimitDTO(day: String, limitCents: Long, usedCents: Long, availableCents: Long)` em `adapters/web/dto/`.
- Create: `application/usecase/GetDailyLimitUseCase.kt` (dia de hoje em São Paulo pelo `Clock`, depois `gateway.usage`).
- Modify: o OpenAPI do serviço (conta desconhecida → `200` com uso zero, documentado), `infra/local/nginx` (rota `/accounts/{id}/daily-limit` para o `transfers`; hoje `/accounts` vai para o `accounts`, então a rota precisa de `location` mais específica), `infra/local/smoke.sh` (consulta o limite depois da transferência do smoke) e `api/bruno/transfers/` (recusa por limite e consulta).
- Create: `tests/e2e/.../DailyLimitEndToEndIntegrationTest.kt`.
- Modify: `docs/rodando-localmente.md` (endpoint e métricas).

- [ ] **Step 1: testes:**
  - `DailyLimitApiIntegrationTest`: `200` com `limitCents = 500000`, `usedCents` depois de uma transferência e `availableCents`;
  - e2e `two transfers above the daily limit: the second fails and the customer is notified` (300000 + 300000; a 2ª fica `FAILED` com `daily-limit-exceeded`; chega a notificação `TRANSFER_FAILED`; o limite mostra `usedCents = 300000`).
- [ ] **Step 2:** implementar; `./gradlew build` verde; smoke e `bru run` das quatro coleções com o compose de pé.
- [ ] **Step 3:** commit `feat(transfers): consulta do limite diário e e2e da recusa`.

## Revisão final da branch (Beatriz)
Mutantes da spec, cada um tem de deixar teste vermelho:
- sem a checagem do campo do `transferId` no `reserve.lua` → `the same transfer reserved twice counts once`;
- sem a liberação em `Failed`/`Compensated` → os testes de liberação (Task 2 e Task 3);
- sem a trava → `mixed paths never exceed the limit` e o teste que segura a advisory lock por JDBC (o Lua já serializa a via só do Redis, então `concurrent reservations` não pega esse mutante);
- epoch lido antes da trava → teste determinístico da ordem trava → epoch (Task 4);
- sem a comparação do epoch → `after reservations through postgres, the redis counter is rebuilt before deciding`.
