# Etapa 2 — `transfers` + saga Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Serviço `transfers` com saga orquestrada (débito na origem, crédito no destino, compensação) que nunca cria nem perde dinheiro sob falha, retentativa ou queda, e o monorepo preparado para mais de um serviço.

**Architecture:** Clean Architecture por serviço. O domínio do `transfers` modela os passos da saga com o padrão State (`TransferStep`); o caso de uso `RunTransferSagaUseCase` chama o `accounts` por HTTP fora de transação, grava cada resultado e o próximo passo numa transação curta, e um arrendamento (`locked_until`) garante uma execução por vez. Dúvidas depois do prazo se resolvem consultando o lançamento pela chave no `accounts`.

**Tech Stack:** Kotlin 2.3.21, Spring Boot 4.1.1 (MVC, virtual threads, `JdbcClient`, `RestClient`, Flyway), PostgreSQL 17, AWS SDK for Java v2 (SNS), LocalStack, Testcontainers, WireMock, ArchUnit 1.5.1, JUnit 6, AssertJ, ktlint, GitHub Actions.

**Rulings da execução** (ledger SDD, 2026-10-27): arrendamento renovado em cada passo e parada antes de chamada sem margem (`callBudget`); `next_attempt_at` só nulo em terminal; compensação recusada espera o máximo; `busyAttempts` = chamadas no total; `failureReason` `debit-not-confirmed`.

**Spec:** [`docs/superpowers/specs/2026-10-27-etapa-2-transfers-design.md`](../specs/2026-10-27-etapa-2-transfers-design.md) — leia antes de qualquer task. ADR: [`docs/adr/0016-…`](../../adr/) (saga).

## Global Constraints
- Código Kotlin, testes, comentários e logs em inglês, com os nomes de código do `CONTEXT.md` (`Transfer`, `TransferSaga`, `TransferStep`, `TransferStatus`, `Compensation`, `Money`). Commits em português, autor `capozziV <vitorcapozzi4@gmail.com>`, sem trailers `Co-Authored-By`/`Claude-Session`.
- Valores em centavos, `Long`. Nunca `Double`/`Float`.
- `domain` e `application` sem Spring; nenhum tipo de domínio do `accounts` no `transfers` (nem o contrário).
- Sufixo do papel em classe técnica (`*UseCase`, `*Repository`, `Jdbc*`, `Http*Gateway`, `*Controller`, `*Classifier`, `*Scheduler`); modelo de domínio sem sufixo. Corpos HTTP com sufixo `DTO` em `adapters/web/dto/`. Classes de teste terminam em `Test` (unidade) ou `IntegrationTest`.
- Pacote raiz `io.github.capozziv.digitalbanking.transfers`; libs em `io.github.capozziv.digitalbanking.outbox`, `…outbox.relay`, `…testing.architecture`; e2e em `…e2e`.
- Nenhuma transação de banco aberta durante chamada HTTP. Nenhuma transação trava duas transferências.
- IDs UUID v7 gerados na aplicação.
- Valores padrão (configuráveis): prazo 2 min; arrendamento 30 s; agendador a cada 1 s, lote 50; espera 1 s × 2^(n−1), teto 15 s; 3 tentativas imediatas para `503 account-busy` na requisição; `RestClient` conexão 1 s e leitura 3 s; porta 8082; `TRANSFERS_ACCOUNTS_URL` padrão `http://localhost:8081`.
- Chaves enviadas ao `accounts`: `transfer-<id>-debit`, `transfer-<id>-credit`, `transfer-<id>-compensation`. Descrições: `Transfer to <destinationAccountId>`, `Transfer from <sourceAccountId>`, `Transfer <id> compensation`.
- Testes que precisam de Docker ficam no source set `integrationTest`; `test` roda sem Docker.
- Versões novas no `gradle/libs.versions.toml`, conferidas em fonte primária na task que as introduz.
- TDD em toda task de código (`superpowers:test-driven-development`). Ao fim de cada task: `./gradlew build` verde (com `LOCALSTACK_AUTH_TOKEN`), saída real no relato; `graphify update .` depois de mudar código.
- Toda tabela nova recebe `grant` próprio para o usuário da aplicação.

## Review Focus
1. **Valor inválido no corpo:** `amount` 0, negativo, decimal (`10.5`) ou acima de `Long.MAX_VALUE` → `400 validation-error`, nada gravado nem chamado no `accounts`. Teste na Task 13.
2. **Mesma conta escrita de dois jeitos:** UUID da origem e do destino iguais com caixa diferente (`ABC…` × `abc…`) → `422 same-account-transfer`. Teste na Task 13.
3. **Repetição com só a descrição diferente:** mesma `Idempotency-Key`, mesma origem, destino e valor, outra `description` → é repetição (devolve a existente), não `422`. Teste na Task 10.
4. **Resposta inesperada do `accounts`:** `502` com corpo HTML do nginx, `200` com corpo que não é JSON, ou conexão cortada no meio → falha passageira, sem exceção não tratada nem passo pulado. Teste na Task 12.
5. **`GET /transfers/{id}` malformado:** `id` que não é UUID → `400 validation-error`; UUID desconhecido → `404 transfer-not-found`. Teste na Task 13.

---

**Task 0 (Clara, `documentador`) — já em andamento antes do plano:** ADR 0016, termos `TransferStatus` e `TransferStep` no `CONTEXT.md`, `docs/arquitetura.md` e `docs/roadmap.md`. Entra no PR do bloco 1.

# Bloco 1 — preparação para o 2º serviço (branch `etapa-2-transfers`, PR 1)

### Task 1: Gradle para vários serviços (Rafael, `infra`) — cartão #37

**Files:**
- Modify: `settings.gradle.kts`, `build-logic/src/main/kotlin/*.gradle.kts` (se o `val libs` repetido for extraído), `build-logic/settings.gradle.kts` (se precisar dos repositórios de plugin)

**Interfaces:**
- Produces: `dependencyResolutionManagement { repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS }`; `pluginManagement` da raiz só com `includeBuild("build-logic")` se o build resolver os plugins assim (senão, mantém os repositórios e registra o motivo no relato).

- [ ] **Step 1:** Aplicar `FAIL_ON_PROJECT_REPOS`; rodar `./gradlew build` → verde (nenhum módulo declara repositório próprio).
- [ ] **Step 2:** Tentar remover `pluginManagement.repositories` da raiz; `./gradlew build --refresh-dependencies` verde ou reverter com o motivo.
- [ ] **Step 3:** Extrair `val libs = …` só se ele aparecer em 3+ plugins de convenção.
- [ ] **Step 4:** Commit `build: repositórios centralizados para vários serviços`.

### Task 2: Relay em módulo próprio e teste de esquema (Marina, `dev-kotlin`) — cartões #25 e #30

**Files:**
- Create: `libs/outbox-relay/build.gradle.kts`; mover de `libs/outbox` para `libs/outbox-relay/src/{main,test,integrationTest}/kotlin/io/github/capozziv/digitalbanking/outbox/relay/`: `OutboxRelay`, `OutboxRelaySettings`, `OutboxStore`, `JdbcOutboxStore`, `EventPublisher`, `SnsEventPublisher`, `OutboxMetrics` e os testes deles (`PendingEvent` fica em `libs/outbox`, porque `OutboxEventEnvelope.toJson` depende dele)
- Create: `libs/outbox/src/testFixtures/…/outbox/OutboxSchemaAssertions.kt` (ou classe em `libs/outbox` de teste exportada para os serviços) e `services/accounts/bootstrap/src/integrationTest/kotlin/…/bootstrap/OutboxSchemaIntegrationTest.kt`
- Modify: `settings.gradle.kts`, `libs/outbox/build.gradle.kts` (sem AWS SDK nem Micrometer), `services/accounts/bootstrap/build.gradle.kts`, imports no `accounts`, regra ArchUnit `outbox library does not depend on the accounts service` (cobre as duas libs)

**Interfaces:**
- Produces: `:libs:outbox` (envelope `OutboxEventEnvelope`, `OutboxMessage`, `JdbcOutboxWriter`, DDL modelo em `src/main/resources/…/outbox.sql`); `:libs:outbox-relay` (pacote `io.github.capozziv.digitalbanking.outbox.relay`) com `api(":libs:outbox")`, AWS SDK SNS e Micrometer como `api`. Mesmas assinaturas da etapa 1 (plano da etapa 1, Task 15), só o pacote muda.
- Produces: `fun assertOutboxSchemaMatchesModel(dataSource: DataSource)` — compara colunas (nome, tipo, nulidade, padrão), índices e constraints da tabela `outbox` do banco migrado com o DDL modelo aplicado num schema temporário.

- [ ] **Step 1:** `OutboxSchemaIntegrationTest` no `accounts`: migrations `V4` + `V6` aplicadas → `assertOutboxSchemaMatchesModel` passa; um teste da própria lib prova que uma coluna a mais ou um índice a menos fazem a asserção falhar.
- [ ] **Step 2:** Rodar → falha (função inexistente); mover os arquivos e criar o módulo; implementar a asserção; passa.
- [ ] **Step 3:** `./gradlew build` verde; `./gradlew :libs:outbox:dependencies --configuration runtimeClasspath` sem `software.amazon.awssdk` nem `micrometer`.
- [ ] **Step 4:** Commit `refactor(outbox): relay em libs/outbox-relay e teste de esquema do outbox`.

### Task 3: Regras ArchUnit compartilhadas (Marina, `dev-kotlin`) — cartão #32

**Files:**
- Create: `libs/testing-architecture/build.gradle.kts`, `libs/testing-architecture/src/main/kotlin/io/github/capozziv/digitalbanking/testing/architecture/ServiceArchitectureRules.kt`
- Modify: `services/accounts/bootstrap/src/test/kotlin/…/bootstrap/ArchitectureTest.kt` (usa as regras da lib), `services/accounts/bootstrap/build.gradle.kts` (`testImplementation`)
- Test: `libs/testing-architecture/src/test/kotlin/…/ServiceArchitectureRulesTest.kt` com classes de exemplo que violam cada regra

**Interfaces:**
- Produces: `object ServiceArchitectureRules { fun all(rootPackage: String): List<ArchRule> }` com as 7 regras atuais do `accounts`, parametrizadas pelo pacote raiz, mais: sufixo `UseCase` para classe pública com método `execute` em qualquer pacote de `application`; `domain` e `application` sem dependência de `org.springframework..`, `java.sql..`, `software.amazon..`; classe com `@Controller` + `@ResponseBody` tratada como `@RestController` na regra de sufixo `Controller`; isenção por sufixo trocada por lista explícita de tipos permitidos (`Command`, `Result`, `Outcome`, `View`).

- [ ] **Step 1:** `ServiceArchitectureRulesTest`: cada regra falha com a classe de exemplo que a viola e passa com a que a respeita (8 regras × 2 casos).
- [ ] **Step 2:** Rodar → falha; implementar; passa; `ArchitectureTest` do `accounts` vira `ServiceArchitectureRules.all(ACCOUNTS_PACKAGE).forEach { it.check(classes) }` e passa sem mudar código do `accounts` (se uma regra nova pegar código existente, corrigir o código e citar no relato).
- [ ] **Step 3:** `./gradlew build` verde.
- [ ] **Step 4:** Commit `test(arch): regras arquiteturais compartilhadas entre serviços`.

### Task 4: Consulta de lançamento pela chave no `accounts` (Marina, `dev-kotlin`)

**Files:**
- Create: `services/accounts/application/src/main/kotlin/…/application/usecase/FindEntryByIdempotencyKeyUseCase.kt`
- Modify: `services/accounts/adapters/src/main/kotlin/…/adapters/web/AccountController.kt`, `ProblemDetailsExceptionHandler.kt`, `api/bruno/accounts/` (+ `08-consultar-lancamento-por-chave.bru`)
- Test: `services/accounts/application/src/test/kotlin/…/usecase/FindEntryByIdempotencyKeyUseCaseTest.kt`, `services/accounts/bootstrap/src/integrationTest/kotlin/…/web/EntryLookupApiIntegrationTest.kt`

**Interfaces:**
- Consumes: `LedgerEntryRepository.findByIdempotencyKey(accountId, key)`, `AccountRepository.findById`.
- Produces: `GET /accounts/{id}/entries?idempotencyKey=<1..255>` → `200 LedgerEntryDTO` (mesmo corpo do crédito/débito); `404 entry-not-found`; `404 account-not-found`; `400 validation-error` sem o parâmetro ou fora de 1..255. `class FindEntryByIdempotencyKeyUseCase(accounts, entries) { fun execute(accountId: UUID, key: String): LedgerEntry? }` — lança `AccountNotFoundException`; sem trava, sem transação de escrita.

- [ ] **Step 1:** Testes: lançamento existente → `200` com `sequence`, `kind`, `amount`, `balanceAfter` iguais aos do `POST`; chave de outra conta → `404 entry-not-found`; conta inexistente → `404 account-not-found`; sem parâmetro → `400`; chave com 256 caracteres → `400`.
- [ ] **Step 2:** Rodar → falha; implementar; passa; OpenAPI com o endpoint; `./gradlew build` verde.
- [ ] **Step 3:** Commit `feat(accounts): consulta de lançamento pela chave de idempotência`.

### Task 5: PostgreSQL com um banco por serviço (Rafael, `infra`) — cartão #38

**Files:**
- Modify: `infra/local/postgres/init/01-create-service-databases.sh`, `infra/local/docker-compose.yml` (volume nomeado `postgres-data`), `infra/local/.env.example` (`TRANSFERS_DB_OWNER_PASSWORD`, `TRANSFERS_DB_PASSWORD`), `docs/rodando-localmente.md` (como adicionar um banco; `down -v` para recriar)

**Interfaces:**
- Produces: bancos `accounts` e `transfers`; roles `transfers_owner` (dono, Flyway) e `transfers_app` (só `LOGIN`). Script idempotente (`CREATE ROLE`/`DATABASE` só se não existir, via `\gexec` ou bloco `DO`), senhas passadas com `psql -v name=value` e lidas como `:'name'`, nunca interpoladas pelo shell no SQL. Uma função `create_service_db <service> <owner_password_var> <app_password_var>` por serviço.

- [ ] **Step 1:** Subir o compose do zero → os dois bancos e os quatro roles existem (`psql -c '\l'`, `\du`); rodar o script uma segunda vez dentro do contêiner → sai 0 sem erro; `./gradlew build` verde.
- [ ] **Step 2:** `docker compose down` (sem `-v`) e `up` → dados preservados no volume nomeado.
- [ ] **Step 3:** Commit `infra(local): init idempotente e banco do transfers`.

### Task 6: Build seletivo no CI (Rafael, `infra`)

**Files:**
- Modify: `.github/workflows/ci.yml`

**Interfaces:**
- Produces: job `changes` (filtro por caminho, ação fixada em versão) com saídas `accounts`, `transfers`, `shared`; jobs `accounts` (`./gradlew :services:accounts:…:build` dos 4 módulos), `transfers` (idem, a partir do bloco 2), `libs` (`./gradlew :libs:…:build`) quando `shared`; `smoke` quando qualquer serviço ou `shared` mudou; `e2e` (a partir do bloco 2); `ci-ok` com `needs` em todos, `if: always()`, falha se algum resultado for `failure` ou `cancelled` (pulado conta como sucesso). `shared` = `libs/**`, `build-logic/**`, `gradle/**`, `settings.gradle.kts`, `build.gradle.kts`, `infra/local/**`, `.github/workflows/**`.

- [ ] **Step 1:** `actionlint` sem erros.
- [ ] **Step 2:** PR de teste (ou o próprio PR do bloco 1): com mudança só em `docs/**`, só `changes` e `ci-ok` rodam e `ci-ok` passa; com mudança em `libs/**`, todos rodam. Registrar os links das execuções no relato.
- [ ] **Step 3:** Commit `ci: build seletivo por serviço e job agregador ci-ok`.

**Fim do bloco 1:** revisão da branch pela Beatriz, verificação, PR, board e `docs/status.md` pelo coordenador. Avisar o usuário que a proteção da `main` (#51) deve exigir o check `ci-ok`.

# Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)

### Task 7: Esqueleto do `transfers` (Marina, `dev-kotlin`)

**Files:**
- Create: `services/transfers/{domain,application,adapters,bootstrap}/build.gradle.kts` (mesmos plugins de convenção do `accounts`), `services/transfers/bootstrap/src/main/kotlin/…/transfers/bootstrap/TransfersApplication.kt`, `application.yml` (porta 8082, datasource `transfers`, Flyway com `transfers_owner`, virtual threads), `services/transfers/bootstrap/src/test/kotlin/…/bootstrap/ArchitectureTest.kt`, `services/transfers/bootstrap/src/integrationTest/kotlin/…/bootstrap/TransfersApplicationIntegrationTest.kt` e a base `TransfersIntegrationTest`
- Modify: `settings.gradle.kts`

**Interfaces:**
- Produces: projetos `:services:transfers:{domain,application,adapters,bootstrap}`; `TransfersIntegrationTest` (Testcontainers PostgreSQL com banco `transfers` e os dois usuários, como `AccountsIntegrationTest`); `ArchitectureTest` com `ServiceArchitectureRules.all(TRANSFERS_PACKAGE)` + regra "transfers não depende de `…accounts..`".

- [ ] **Step 1:** IT: `/actuator/health` `UP` com `db` `UP`; ArchUnit verde.
- [ ] **Step 2:** Rodar → falha; criar; passa; `./gradlew build` verde.
- [ ] **Step 3:** Commit `feat(transfers): esqueleto do serviço`.

### Task 8: Domínio da transferência e passos da saga (Marina, `dev-kotlin`)

**Files:**
- Create em `services/transfers/domain/src/main/kotlin/…/transfers/domain/`: `Money.kt`, `TransferStatus.kt`, `StepOutcome.kt`, `AccountsCall.kt`, `TransferStep.kt`, `Transfer.kt`, `TransferEvent.kt`
- Test: `MoneyTest.kt`, um arquivo por passo (`DebitStepTest.kt`, `ResolveDebitStepTest.kt`, `CreditStepTest.kt`, `ResolveCreditStepTest.kt`, `CompensateStepTest.kt`), `TransferTest.kt`, `TransferSagaPathsTest.kt`

**Interfaces:**
- Produces:
  - `data class Money(val cents: Long)` — `init { require(cents > 0) }`.
  - `enum class TransferStatus { PENDING, COMPLETED, FAILED, COMPENSATED }`.
  - `sealed interface StepOutcome { data object Success; data class Refused(val reason: String); data object TransientFailure }`; constantes `INSUFFICIENT_AVAILABLE_BALANCE = "insufficient-available-balance"`, `ENTRY_NOT_FOUND = "entry-not-found"` em `StepOutcome.Companion`.
  - `sealed interface AccountsCall { data class Debit(val accountId: UUID, val amount: Money, val idempotencyKey: String, val description: String); data class Credit(…mesmos campos); data class FindEntry(val accountId: UUID, val idempotencyKey: String) }`.
  - `sealed interface TransferStep { val name: String; val status: TransferStatus; fun call(transfer: Transfer): AccountsCall?; fun onSuccess(transfer: Transfer): TransferStep; fun onRefused(transfer: Transfer, reason: String): TransferStep; fun onTransientFailure(transfer: Transfer, now: Instant): TransferStep; companion object { fun fromName(name: String): TransferStep } }` com `data object` `Debit`, `ResolveDebit`, `Credit`, `ResolveCredit`, `Compensate`, `Completed`, `Failed`, `Compensated`. Transições exatamente as da tabela da spec, seção 3. Terminais: `call` = `null`; os três `on*` lançam `IllegalStateException`. KDoc da interface reproduz a tabela.
  - `data class Transfer(id, sourceAccountId, destinationAccountId, amount: Money, description: String?, idempotencyKey: String, step: TransferStep, failureReason: String?, deadline: Instant, attempts: Int, correlationId: String?, createdAt: Instant, updatedAt: Instant)` com `val status get() = step.status`, `fun isPastDeadline(now: Instant) = !now.isBefore(deadline)`, `fun advance(outcome: StepOutcome, now: Instant): Transfer` (troca de passo → `attempts = 0`; mesmo passo → `attempts + 1`; recusa que leva a `Failed` grava `failureReason = reason`; `updatedAt = now`), `fun terminalEvent(): TransferEvent?` e `companion fun request(id, source, destination, amount, description, key, correlationId, now, deadlineAfter: Duration): Transfer` (`require(source != destination)`, `step = Debit`).
  - `sealed interface TransferEvent { val transferId: UUID }` com `TransferCompleted`, `TransferFailed(…, reason)`, `TransferCompensated` (campos da spec, seção 6).

- [ ] **Step 1:** Testes por passo cobrindo cada célula da tabela da spec; `Compensate.onRefused` volta `Compensate`; `ResolveDebit.onTransientFailure` depois do prazo continua `ResolveDebit`; `TransferSagaPathsTest` percorre: débito ok + crédito ok → `COMPLETED`; débito recusado por saldo → `FAILED` com o motivo; crédito recusado → `COMPENSATED`; débito com timeout até o prazo + entrada encontrada → `COMPENSATED`; + não encontrada → `FAILED`; crédito com timeout até o prazo + entrada encontrada → `COMPLETED`; + não encontrada → `COMPENSATED`. `TransferTest`: `request` com origem = destino lança; `advance` zera ou soma `attempts`; `terminalEvent` só nos terminais; `fromName` ida e volta para os 8 passos.
- [ ] **Step 2:** Rodar → falha; implementar; passa; `./gradlew build` verde.
- [ ] **Step 3:** Commit `feat(transfers): domínio e passos da saga`.

### Task 9: Portas e execução da saga (Marina, `dev-kotlin`)

**Files:**
- Create em `services/transfers/application/src/main/kotlin/…/transfers/application/`: `port/TransferRepository.kt`, `port/TransferStepLog.kt`, `port/AccountsGateway.kt`, `port/EventOutbox.kt`, `port/TransactionRunner.kt`, `port/IdGenerator.kt`, `RetryBackoff.kt` (no pacote `application`, fora de `usecase`), `usecase/SagaSettings.kt`, `usecase/RunTransferSagaUseCase.kt`
- Test: `application/src/test/kotlin/…/fake/` (`InMemoryTransferRepository`, `InMemoryTransferStepLog`, `FakeAccountsGateway` com roteiro de respostas por chamada, `FakeEventOutbox`, `ImmediateTransactionRunner`), `RetryBackoffTest.kt`, `usecase/RunTransferSagaUseCaseTest.kt`

**Interfaces:**
- Consumes: Task 8.
- Produces:
  - `interface TransferRepository { fun findById(id: UUID): Transfer?; fun findByIdempotencyKey(key: String): Transfer?; fun insert(transfer: Transfer): Boolean /* false = chave já existe */; fun tryAcquireLease(id: UUID, owner: String, until: Instant, now: Instant): Boolean; fun saveIfOwner(transfer: Transfer, owner: String, nextAttemptAt: Instant?, leaseUntil: Instant): Boolean /* renova o arrendamento; nextAttemptAt nulo só em terminal */; fun releaseLease(id: UUID, owner: String); fun findDue(now: Instant, limit: Int): List<UUID> }`.
  - `interface TransferStepLog { fun append(record: StepRecord) }`; `data class StepRecord(val transferId: UUID, val step: String, val outcome: String, val httpStatus: Int?, val detail: String?, val at: Instant)`.
  - `interface AccountsGateway { fun lookupAccount(id: UUID): AccountLookup; fun perform(call: AccountsCall): CallResult }`; `enum class AccountLookup { EXISTS, NOT_FOUND, UNAVAILABLE }`; `data class CallResult(val outcome: StepOutcome, val httpStatus: Int?, val detail: String?)`.
  - `interface EventOutbox { fun append(event: TransferEvent) }`; `TransactionRunner` e `IdGenerator` como no `accounts`.
  - `class RetryBackoff(initial: Duration, max: Duration) { fun delayBeforeRetry(attempt: Int): Duration }` — sem interface.
  - `data class SagaSettings(val deadline: Duration = 2.minutes, val lease: Duration = 30.seconds, val busyAttempts: Int = 3 /* chamadas no total para 503 */, val dueBatchSize: Int = 50, val callBudget: Duration = 4.seconds)`.
  - `class RunTransferSagaUseCase(transfers, stepLog, accounts, outbox, tx, backoff, settings, clock) { fun execute(transferId: UUID, owner: String, busyAttempts: Int = 1): Transfer? }` — `null` se não tomou o arrendamento. Laço: `call` do passo → `accounts.execute` (fora de transação; `TransientFailure` com HTTP 503 repete até completar `busyAttempts` chamadas no total) → numa transação: `stepLog.append`, `transfer.advance`, se terminal `outbox.append(terminalEvent)`, em falha passageira grava `next_attempt_at = now + backoff.delayBeforeRetry(attempts)`, `saveIfOwner` (false → para sem efeito). Para em terminal ou falha passageira; solta o arrendamento no `finally`.

- [ ] **Step 1:** `RetryBackoffTest`: tentativas 1..6 → 1, 2, 4, 8, 15, 15 s; `attempt` 0 lança. `RunTransferSagaUseCaseTest` (relógio controlado): caminho feliz grava 2 `StepRecord` e 1 `TransferCompleted`; 503 duas vezes e depois 201 com `busyAttempts = 3` → `COMPLETED` sem nova tentativa agendada; timeout no crédito → `PENDING`, `next_attempt_at = now + 1 s`, arrendamento solto; arrendamento ocupado → `null` e nenhuma chamada ao gateway; `saveIfOwner` falso (arrendamento perdido) → laço para e nenhum evento gravado; as chaves e descrições enviadas são exatamente as da spec.
- [ ] **Step 2:** Rodar → falha; implementar; passa; `./gradlew build` verde.
- [ ] **Step 3:** Commit `feat(transfers): execução da saga com arrendamento e novas tentativas`.

### Task 10: Pedir, consultar e retomar transferências (Marina, `dev-kotlin`)

**Files:**
- Create em `…/application/usecase/`: `RequestTransferCommand.kt`, `RequestTransferUseCase.kt`, `GetTransferUseCase.kt`, `ResumeDueTransfersUseCase.kt`; `…/application/IdempotencyKeyReusedException.kt`
- Test: `RequestTransferUseCaseTest.kt`, `GetTransferUseCaseTest.kt`, `ResumeDueTransfersUseCaseTest.kt`

**Interfaces:**
- Consumes: Task 9.
- Produces:
  - `data class RequestTransferCommand(val sourceAccountId: UUID, val destinationAccountId: UUID, val amount: Money, val description: String?, val idempotencyKey: String, val correlationId: String?)`.
  - `sealed interface RequestTransferOutcome { data class Created(val transfer: Transfer); data class Existing(val transfer: Transfer); data object SameAccount; data class AccountNotFound(val accountId: UUID); data object AccountsUnavailable }`.
  - `class RequestTransferUseCase(transfers, accounts, runSaga: RunTransferSagaUseCase, ids, settings, clock) { fun execute(command: RequestTransferCommand): RequestTransferOutcome }` — fluxo da spec, seção 4, passos 1–5; repetição compara origem, destino e valor (não a descrição); pedido diferente lança `IdempotencyKeyReusedException`; conflito no `insert` relê e trata como repetição; roda a saga com `owner = "request-<uuid>"` e `busyAttempts = settings.busyAttempts`; devolve o estado depois da saga.
  - `class GetTransferUseCase(transfers) { fun execute(id: UUID): Transfer? }`.
  - `class ResumeDueTransfersUseCase(transfers, runSaga, settings, clock, owner: String) { fun execute(): Int }` — roda a saga para cada id de `findDue(now, dueBatchSize)`; devolve quantas tomou.

- [ ] **Step 1:** Testes: origem = destino → `SameAccount` sem chamada ao gateway; destino inexistente → `AccountNotFound(destino)` sem gravar; `UNAVAILABLE` → `AccountsUnavailable` sem gravar; repetição com outra descrição → `Existing` (Review Focus 3); repetição com outro valor → `IdempotencyKeyReusedException`; `insert` devolve falso (corrida) → `Existing`; `ResumeDueTransfersUseCase` pega só as vencidas e ignora as com arrendamento de outro dono.
- [ ] **Step 2:** Rodar → falha; implementar; passa; `./gradlew build` verde.
- [ ] **Step 3:** Commit `feat(transfers): pedido, consulta e retomada de transferências`.

### Task 11: Persistência do `transfers` (Marina, `dev-kotlin`)

**Files:**
- Create em `services/transfers/adapters/src/main/resources/db/migration/`: `V1__transfer.sql`, `V2__transfer_step.sql`, `V3__outbox.sql` (DDL modelo de `libs/outbox`, com o índice de conta presa), `V4__app_grants.sql`
- Create em `…/adapters/persistence/`: `JdbcTransferRepository.kt`, `JdbcTransferStepLog.kt`, `OutboxEventAppender.kt`, `SpringTransactionRunner.kt`, `UuidV7IdGenerator.kt`, `OutboxWriterConfiguration.kt`
- Test: `services/transfers/bootstrap/src/integrationTest/kotlin/…/persistence/JdbcTransferRepositoryIntegrationTest.kt`, `JdbcTransferStepLogIntegrationTest.kt`, `OutboxSchemaIntegrationTest.kt`

**Interfaces:**
- Consumes: Tasks 2, 8, 9.
- Produces: esquema da spec, seção 5 (colunas, `CHECK`s, índice parcial `transfer_due_idx (next_attempt_at) WHERE status = 'PENDING'`); `transfers_app` com `SELECT, INSERT, UPDATE` em `transfer` e `outbox`, `SELECT, INSERT` em `transfer_step` e `USAGE` na sequência; `tryAcquireLease` com o `UPDATE … WHERE … (locked_until IS NULL OR locked_until < :now)` da spec; `saveIfOwner` com `WHERE id = :id AND lock_owner = :owner AND locked_until >= :now`, gravando `next_attempt_at` e `locked_until = :leaseUntil` (renovação); `findDue` nunca casa `next_attempt_at` nulo; `findDue` com `FOR UPDATE SKIP LOCKED` só na seleção dos ids; `OutboxEventAppender` com `aggregateId = transferId` e os payloads da spec, seção 6.

- [ ] **Step 1:** ITs: dois `tryAcquireLease` concorrentes → só um `true`; arrendamento vencido → outro dono toma; `saveIfOwner` com dono errado → `false` e nada muda; `insert` com chave repetida → `false`; `findDue` ignora não vencidas, terminais e arrendadas; `transfers_app` não consegue `UPDATE`/`DELETE` em `transfer_step` (erro de permissão); `assertOutboxSchemaMatchesModel` passa; ida e volta de `Transfer` (todos os campos, os 8 passos).
- [ ] **Step 2:** Rodar → falha; implementar; passa; `./gradlew build` verde.
- [ ] **Step 3:** Commit `feat(transfers): persistência com arrendamento e trilha de passos`.

### Task 12: Cliente HTTP do `accounts` (Marina, `dev-kotlin`)

**Files:**
- Create em `services/transfers/adapters/src/main/kotlin/…/adapters/accounts/`: `HttpAccountsGateway.kt`, `AccountsResponseClassifier.kt`, `AccountsClientProperties.kt`, DTOs internos do cliente (`AccountsEntryRequestDTO` etc. em `adapters/accounts/dto/`)
- Modify: `gradle/libs.versions.toml` (WireMock, versão conferida em fonte primária)
- Test: `…/adapters/src/test/kotlin/…/accounts/AccountsResponseClassifierTest.kt`, `services/transfers/bootstrap/src/integrationTest/kotlin/…/accounts/HttpAccountsGatewayIntegrationTest.kt` (WireMock)

**Interfaces:**
- Consumes: Task 9 (`AccountsGateway`, `AccountsCall`, `CallResult`, `AccountLookup`).
- Produces: `class HttpAccountsGateway(restClient: RestClient, classifier: AccountsResponseClassifier) : AccountsGateway` — `Debit` → `POST /accounts/{id}/debits`, `Credit` → `POST /accounts/{id}/credits` (header `Idempotency-Key`, corpo `{amount, description}`), `FindEntry` → `GET /accounts/{id}/entries?idempotencyKey=`, `lookupAccount` → `GET /accounts/{id}`; `X-Correlation-Id` do MDC em toda chamada; timeouts de conexão e leitura de `AccountsClientProperties`. `class AccountsResponseClassifier { fun classify(status: Int?, problemType: String?): StepOutcome }` — regras da spec, seção 4; `status = null` (timeout, conexão) → `TransientFailure`; `problemType` é o último segmento do `type` do Problem Details.

- [ ] **Step 1:** `AccountsResponseClassifierTest` com uma linha por regra da spec. IT com WireMock: corpo e headers enviados (chave, descrição, correlation ID); `201`/`200` → `Success`; `422 insufficient-available-balance` → `Refused`; `404 entry-not-found` → `Refused(ENTRY_NOT_FOUND)`; `503` → `TransientFailure`; atraso de 4 s → `TransientFailure` em ~3 s; Review Focus 4: `502` com HTML, `200` com corpo inválido e conexão cortada (`Fault.CONNECTION_RESET_BY_PEER`) → `TransientFailure`, sem exceção.
- [ ] **Step 2:** Rodar → falha; implementar; passa; `./gradlew build` verde.
- [ ] **Step 3:** Commit `feat(transfers): cliente http do accounts com classificação de respostas`.

### Task 13: API HTTP do `transfers` (Marina, `dev-kotlin`)

**Files:**
- Create em `services/transfers/adapters/src/main/kotlin/…/adapters/web/`: `TransferController.kt`, `ProblemDetailsExceptionHandler.kt`, `CorrelationIdFilter.kt` (cópia mínima do `accounts`), `IdempotencyKeyHeader.kt`, `OpenApiConfiguration.kt`, `dto/TransferRequestDTOs.kt`, `dto/TransferResponseDTOs.kt`
- Test: `services/transfers/bootstrap/src/integrationTest/kotlin/…/web/TransferApiIntegrationTest.kt` (WireMock no lugar do `accounts`), `OpenApiIntegrationTest.kt`

**Interfaces:**
- Consumes: Task 10.
- Produces: `POST /transfers` (`Idempotency-Key`) `RequestTransferDTO {sourceAccountId, destinationAccountId, amount, description?}` e `GET /transfers/{id}` → `TransferDTO {id, sourceAccountId, destinationAccountId, amount, description, status, failureReason, createdAt, updatedAt}`; códigos pela tabela da spec, seção 4 (`201`, `200`, `202`, `422 insufficient-available-balance` com `transferId`, `422 transfer-failed` com `reason`, `422 same-account-transfer`, `422 account-not-found` com `accountId`, `422 idempotency-key-reused`, `503 accounts-unavailable` com `Retry-After: 1`, `404 transfer-not-found`); validação de `Idempotency-Key` e `description` como no `accounts`.

- [ ] **Step 1:** IT: cada linha da tabela de respostas da spec, incluindo a repetição com a mesma chave; Review Focus 1 (`0`, `-1`, `10.5`, `9223372036854775808` → `400`, WireMock sem chamadas); Review Focus 2 (mesmo UUID com caixa diferente → `422 same-account-transfer`); Review Focus 5 (`/transfers/abc` → `400`; UUID desconhecido → `404`); `X-Correlation-Id` recebido chega ao WireMock; OpenAPI lista os dois endpoints.
- [ ] **Step 2:** Rodar → falha; implementar; passa; `./gradlew build` verde.
- [ ] **Step 3:** Commit `feat(transfers): api http de transferências`.

### Task 14: Agendador, relay e eventos no `transfers` (Marina, `dev-kotlin`)

**Files:**
- Create em `services/transfers/bootstrap/src/main/kotlin/…/bootstrap/`: `UseCaseConfiguration.kt`, `SagaProperties.kt` (`transfers.saga.*`: `deadline`, `lease`, `busy-attempts`, `call-budget`, `due-batch-size`, `retry-initial`, `retry-max`, `poll-interval`, `scheduler-enabled`), `TransferSagaScheduler.kt` (`@Scheduled(fixedDelayString = "\${transfers.saga.poll-interval:1s}")`, desligamento gracioso como o `OutboxRelayScheduler`), `OutboxRelayConfiguration.kt` e `OutboxRelayScheduler.kt` (como no `accounts`, chave de trava própria, `transfers.outbox.*`, `transfers.aws.*`)
- Modify: `application.yml`
- Test: `services/transfers/bootstrap/src/integrationTest/kotlin/…/bootstrap/TransferSagaResumeIntegrationTest.kt`, `TransferLeaseContentionIntegrationTest.kt`, `TransferEventsIntegrationTest.kt` (LocalStack), `TransferSagaSchedulerTest.kt` (unitário)

**Interfaces:**
- Consumes: Tasks 2, 9–13.
- Produces: aplicação completa; `owner` do agendador único por execução = `scheduler-<hostname>-<pid>-<uuid>` (o `tryAcquireLease` aceita de novo o mesmo dono).
  - Métricas Micrometer `transfers.pending` (gauge) e `transfers.oldest.pending.age` (segundos), para enxergar compensação recusada em laço (spec, Riscos).

- [ ] **Step 1:** ITs: WireMock falha o crédito 2 vezes com `503` e depois aceita → transferência `PENDING` na resposta e `COMPLETED` pelo agendador; crédito falhando até o prazo (prazo de 3 s no teste) e consulta `404 entry-not-found` → `COMPENSATED`, com a chamada de compensação na chave certa; requisição e agendador disputando a mesma transferência → cada chamada ao WireMock acontece no máximo uma vez por tentativa registrada em `transfer_step`; contexto reiniciado com transferência `PENDING` e arrendamento vencido → o novo contexto termina; eventos `TransferCompleted`, `TransferFailed` e `TransferCompensated` na fila `transfers-events-inspection.fifo` com `correlationId`; `transfers.pending` e `transfers.oldest.pending.age` no `/actuator/metrics` refletem uma transferência `PENDING`.
- [ ] **Step 2:** Rodar → falha; implementar; passa; `./gradlew build` verde.
- [ ] **Step 3:** Commit `feat(transfers): agendador da saga e relay de eventos`.

### Task 15: Infra local, Bruno, smoke e CI do `transfers` (Rafael, `infra`)

**Files:**
- Create: `infra/local/localstack/init/ready.d/02-transfers-topic.sh` (tópico `transfers-events.fifo`, fila `transfers-events-inspection.fifo`, `RawMessageDelivery=true`), `api/bruno/transfers/` (`01-criar-transferencia.bru`, `02-repetir-mesma-chave.bru`, `03-consultar-transferencia.bru`)
- Modify: `infra/local/nginx/nginx.conf` (`/transfers`, `/transfers/`, `/transfers/actuator/` → `host.docker.internal:8082`), `infra/local/.env.example` (`TRANSFERS_ACCOUNTS_URL`), `infra/local/smoke.sh` (duas contas, crédito, transferência, saldos 700/300 conforme o roteiro atual, `TransferCompleted` na fila), `.github/workflows/ci.yml` (job `transfers`; `smoke` com os dois jars), `docs/rodando-localmente.md` (2º `bootRun`, sequência manual de transferência)

- [ ] **Step 1:** Compose do zero saudável; os dois serviços com `bootRun`; `infra/local/smoke.sh` passa; `bru run api/bruno/transfers` verde; `actionlint` e `shellcheck` sem erros.
- [ ] **Step 2:** Commit `infra: transfers no nginx, localstack, smoke e ci`.

### Task 16: Testes ponta a ponta (Marina, `dev-kotlin`; Gradle do módulo pode vir do Rafael)

**Files:**
- Create: `tests/e2e/build.gradle.kts` (plugin `digitalbanking.integration-test`; depende de `:services:accounts:bootstrap:bootJar` e `:services:transfers:bootstrap:bootJar`, caminhos dos jars por propriedade de sistema), `tests/e2e/src/integrationTest/kotlin/io/github/capozziv/digitalbanking/e2e/ServiceProcess.kt` (sobe `java -jar` com variáveis de ambiente, espera o health, `stop()`/`restart()`), `EndToEndEnvironment.kt` (PostgreSQL e LocalStack por Testcontainers, os dois bancos), `TransferSagaEndToEndIntegrationTest.kt`
- Modify: `settings.gradle.kts`, `.github/workflows/ci.yml` (job `e2e`)

**Interfaces:**
- Consumes: Tasks 4, 14.

- [ ] **Step 1:** Testes dos critérios de pronto 3 a 7 da spec, com os números exatos: 50 × 200 a partir de 5000 → 25 `COMPLETED`, 25 `FAILED`, origem 0, soma constante; 20 pedidos com a mesma chave → 1 transferência, 1 débito, 1 crédito (conferidos pela consulta por chave e pelo saldo); `accounts` parado depois do débito (prazo de 30 s por configuração; o reinício real do `accounts` leva de 6 a 10 s): volta antes → `COMPLETED`; volta depois → `COMPENSATED`, destino sem crédito; `transfers` morto no meio e reiniciado → termina; eventos na fila de inspeção.
- [ ] **Step 2:** Rodar → falha; implementar; passa; `./gradlew build` verde; tempo do job `e2e` no relato.
- [ ] **Step 3:** Commit `test(e2e): saga de transferência ponta a ponta`.

**Fim do bloco 2:** revisão da branch pela Beatriz, verificação, PR, `docs/status.md` e board pelo coordenador (Stage 2 → Done; cartões sem pendência da Stage 3 → Ready).
