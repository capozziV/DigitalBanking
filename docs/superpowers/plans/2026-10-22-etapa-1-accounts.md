# Etapa 1 — `accounts` Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `accounts` com abrir conta, crédito, débito com cheque especial, troca de tipo e limites por tipo, com concorrência e idempotência provadas por teste, e cada fato publicado num tópico SNS FIFO pelo Transactional Outbox.

**Architecture:** Clean Architecture por serviço (`domain` → `application` → `adapters` → `bootstrap`). O domínio decide (lançamento + evento); o caso de uso grava lançamento e evento na mesma transação, sob `FOR UPDATE` na conta. `libs/outbox` guarda o envelope, o writer JDBC e (bloco 2) o relay líder que publica no SNS FIFO.

**Tech Stack:** Kotlin 2.3.21, Spring Boot 4.1.1 (MVC, virtual threads, `JdbcClient`, Flyway), PostgreSQL 17, AWS SDK for Java v2 (SNS), LocalStack, Testcontainers, ArchUnit 1.5.1, JUnit 6, AssertJ, ktlint.

**Spec:** [`docs/superpowers/specs/2026-10-22-etapa-1-accounts-design.md`](../specs/2026-10-22-etapa-1-accounts-design.md) — leia antes de qualquer task.

## Global Constraints
- Código Kotlin, testes, comentários e logs em inglês, com os nomes de código do `CONTEXT.md`. Commits em português, autor `capozziV <vitorcapozzi4@gmail.com>`, sem trailers `Co-Authored-By`/`Claude-Session`.
- Valores monetários em centavos, `Long`. Nunca `Double`/`Float`.
- `domain` e `application` sem Spring (o Gradle já impede no classpath; ArchUnit confere).
- Sufixo do papel em classe técnica (`*UseCase`, `*Repository`, `Jdbc*Repository`, `*Controller`, `*Publisher`, `*Writer`, `*Relay`); modelo de domínio sem sufixo.
- Pacote raiz `io.github.capozziv.digitalbanking.accounts` (subpacotes `domain`, `application`, `adapters.web`, `adapters.persistence`, `bootstrap`); lib em `io.github.capozziv.digitalbanking.outbox`.
- Lançamento imutável; saldo para débito nunca de cache; nenhuma transação trava duas contas.
- IDs UUID v7 gerados na aplicação.
- Testes que precisam de Docker ficam no source set `integrationTest`; `test` roda sem Docker.
- Versões novas no `gradle/libs.versions.toml`, conferidas em fonte primária (Maven Central / documentação oficial) na task que as introduz.
- TDD em toda task de código (`superpowers:test-driven-development`). Ao fim de cada task: `./gradlew build` verde, com a saída real no relato.

## Review Focus
1. **Descrição no limite:** `description` com 140 caracteres passa; com 141 → `400 validation-error`; vazia (`""`) é tratada como ausente (`null`). Teste na Task 9.
2. **Estouro de `Long` no saldo:** crédito que faria `balanceAfter` passar de `Long.MAX_VALUE` → `400 validation-error` (domínio usa `Math.addExact`/`subtractExact`, nunca dá a volta). Teste na Task 3 (domínio) e Task 9 (HTTP).
3. **Débito igual ao saldo disponível:** passa (comparação `≥`) e deixa o saldo exatamente em `-overdraftLimit`. Teste na Task 3.
4. **Entrada malformada no caminho e no corpo:** `id` que não é UUID → `400 validation-error`; tipo desconhecido ou em minúsculas (`"gold"`, `"PLATINUM"`) no corpo de `PUT /accounts/{id}/type` → `400`; no caminho de `PUT /admin/account-types/{type}` → `404 account-type-not-found`. Teste na Task 9.
5. **`Idempotency-Key` em branco ou com mais de 255 caracteres** → `400 validation-error` (ausente → `400 missing-idempotency-key`). Teste na Task 9.

---

# Bloco 1 — `accounts` gravando no outbox

### Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`)

**Files:**
- Create: `build-logic/src/main/kotlin/digitalbanking.integration-test.gradle.kts`
- Modify: `build-logic/src/main/kotlin/digitalbanking.spring-boot-app.gradle.kts`, `gradle/libs.versions.toml`, `services/accounts/bootstrap/build.gradle.kts`
- Move: `services/accounts/bootstrap/src/test/kotlin/.../AccountsApplicationIT.kt` → `services/accounts/bootstrap/src/integrationTest/kotlin/.../AccountsApplicationIT.kt`
- Create: `docs/superpowers/research/2026-10-22-versoes-etapa-1.md`

**Interfaces:**
- Produces: plugin `digitalbanking.integration-test` (aplicado por `spring-boot-app`; também aplicável a libs), suíte `integrationTest` (JVM Test Suite) com acesso às dependências de `testImplementation`, `check` dependendo dela. Catálogo com: `springdoc-openapi-starter-webmvc-ui` (se compatível), `java-uuid-generator` (`com.fasterxml.uuid`), `testcontainers-localstack`, `aws-sdk-bom` + `sns`/`sqs`.

- [ ] **Step 1:** Pesquisar em fonte primária (`mattpocock-skills:research`): versão do springdoc compatível com Spring Boot 4.1.x; versão estável do `java-uuid-generator`; versão do AWS SDK for Java v2 BOM; módulo `org.testcontainers:testcontainers-localstack` na linha do BOM do Boot. Registrar no arquivo de research com links. **Se não houver springdoc compatível, registrar e seguir: a Task 10 usa o plano B (YAML).**
- [ ] **Step 2:** Criar o plugin com `testing.suites.register<JvmTestSuite>("integrationTest") { useJUnitJupiter(); dependencies { implementation(project()) } }`, `configurations["integrationTestImplementation"].extendsFrom(configurations["testImplementation"])` (idem `RuntimeOnly`), e `tasks.check { dependsOn(testing.suites.named("integrationTest")) }`.
- [ ] **Step 3:** Mover o `AccountsApplicationIT`. Rodar `./gradlew :services:accounts:bootstrap:test` → passa **sem** Docker e não executa o IT; `./gradlew :services:accounts:bootstrap:integrationTest` → roda o IT e passa.
- [ ] **Step 4:** `./gradlew build` verde.
- [ ] **Step 5:** Commit `build: source set integrationTest e versões da etapa 1`.

### Task 2: Valores monetários e tipos (Marina, `dev-kotlin`)

**Files:**
- Create em `services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/`: `Money.kt`, `Balance.kt`, `OverdraftLimit.kt`, `AvailableBalance.kt`, `AccountType.kt`, `EntryKind.kt`
- Test: `services/accounts/domain/src/test/kotlin/.../domain/MoneyTest.kt`, `BalanceTest.kt`

**Interfaces:**
- Produces:
  - `@JvmInline value class Money(val cents: Long)` — `require(cents > 0)`.
  - `@JvmInline value class Balance(val cents: Long)` — `operator fun plus(m: Money): Balance` e `minus(m: Money): Balance` com `Math.addExact`/`subtractExact` (estouro → `IllegalArgumentException("balance overflow")`); `companion val ZERO`.
  - `@JvmInline value class OverdraftLimit(val cents: Long)` — `require(cents >= 0)`.
  - `@JvmInline value class AvailableBalance(val cents: Long)` — `companion fun of(balance: Balance, limit: OverdraftLimit): AvailableBalance`; `fun covers(amount: Money): Boolean` (`cents >= amount.cents`).
  - `enum class AccountType { FREE, STANDARD, GOLD, BLACK, DIAMOND }`, `enum class EntryKind { CREDIT, DEBIT }`.

- [ ] **Step 1:** Testes: `Money(0)` e `Money(-1)` lançam; `Balance(100) - Money(150) == Balance(-50)`; `Balance(Long.MAX_VALUE) + Money(1)` lança `IllegalArgumentException`; `OverdraftLimit(-1)` lança; `AvailableBalance.of(Balance(-50), OverdraftLimit(200)).cents == 150`; `covers(Money(150))` é `true`, `covers(Money(151))` é `false`.
- [ ] **Step 2:** Rodar `./gradlew :services:accounts:domain:test` → falha (classes não existem).
- [ ] **Step 3:** Implementar.
- [ ] **Step 4:** Rodar → passa. `./gradlew build` verde.
- [ ] **Step 5:** Commit `feat(accounts): valores monetários e tipos de conta`.

### Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`)

**Files:**
- Create em `.../accounts/domain/`: `Account.kt`, `LedgerEntry.kt`, `DebitResult.kt`, `AccountEvent.kt`, `AccountTypeLimit.kt`
- Test: `.../domain/AccountTest.kt`

**Interfaces:**
- Consumes: Task 2.
- Produces:
  - `data class LedgerEntry(val id: UUID, val accountId: UUID, val sequence: Long, val kind: EntryKind, val amount: Money, val balanceAfter: Balance, val description: String?, val idempotencyKey: String, val createdAt: Instant)` — `init`: `sequence >= 1`, `description` nula ou 1..140 caracteres.
  - `fun LedgerEntry?.currentBalance(): Balance` (`null` → `Balance.ZERO`) e `fun LedgerEntry?.nextSequence(): Long` (`null` → 1).
  - `sealed interface AccountEvent { val accountId: UUID }` com `data class AccountOpened(accountId, customerId: UUID, type: AccountType)`, `data class LedgerEntryRecorded(val entry: LedgerEntry)` (`accountId` = `entry.accountId`), `data class AccountTypeChanged(accountId, previousType: AccountType, newType: AccountType)`.
  - `data class EntryRecorded(val entry: LedgerEntry, val event: LedgerEntryRecorded)`.
  - `sealed interface DebitResult { data class Recorded(val recorded: EntryRecorded) : DebitResult; data class Refused(val availableBalance: AvailableBalance) : DebitResult }`.
  - `data class AccountTypeLimit(val type: AccountType, val overdraftLimit: OverdraftLimit, val updatedAt: Instant)`.
  - `data class Account(val id: UUID, val customerId: UUID, val type: AccountType)` com:
    - `companion fun open(id: UUID, customerId: UUID): Pair<Account, AccountOpened>` — tipo `FREE`;
    - `fun credit(last: LedgerEntry?, entryId: UUID, amount: Money, description: String?, idempotencyKey: String, now: Instant): EntryRecorded`;
    - `fun debit(last: LedgerEntry?, limit: OverdraftLimit, entryId: UUID, amount: Money, description: String?, idempotencyKey: String, now: Instant): DebitResult`;
    - `fun changeType(newType: AccountType): Pair<Account, AccountTypeChanged>?` — `null` se o tipo for o mesmo.

- [ ] **Step 1:** Testes (`AccountTest`):
  - `open` → tipo `FREE` e `AccountOpened` com os mesmos ids;
  - crédito sem lançamento anterior → `sequence 1`, `balanceAfter 1000`; crédito após `balanceAfter 1000` (`sequence 3`) → `sequence 4`, `balanceAfter 1500`;
  - débito de 1500 com saldo 1000 e limite 500 → `Recorded`, `balanceAfter -500` (Review Focus 3);
  - débito de 1501 no mesmo cenário → `Refused(AvailableBalance(1500))`;
  - conta com saldo -800 e limite 500 (após downgrade): débito de 1 → `Refused(AvailableBalance(-300))`; crédito de 100 → `balanceAfter -700`;
  - crédito de 1 com `balanceAfter Long.MAX_VALUE` → `IllegalArgumentException` (Review Focus 2);
  - `changeType(GOLD)` numa `FREE` → evento `FREE→GOLD`; `changeType(FREE)` numa `FREE` → `null`;
  - `LedgerEntry` com descrição de 141 caracteres lança; com 140 passa.
- [ ] **Step 2:** Rodar `./gradlew :services:accounts:domain:test` → falha.
- [ ] **Step 3:** Implementar.
- [ ] **Step 4:** Rodar → passa; `./gradlew build` verde.
- [ ] **Step 5:** Commit `feat(accounts): conta, lançamento e eventos de domínio`.

### Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`)

**Files:**
- Create em `services/accounts/application/src/main/kotlin/.../accounts/application/`: `port/AccountRepository.kt`, `port/LedgerEntryRepository.kt`, `port/AccountTypeRepository.kt`, `port/EventOutbox.kt`, `port/TransactionRunner.kt`, `port/IdGenerator.kt`, `ApplicationExceptions.kt`, `usecase/OpenAccountUseCase.kt`, `usecase/GetAccountUseCase.kt`
- Create (test fixtures): `services/accounts/application/src/test/kotlin/.../application/fake/` — `InMemoryAccountRepository.kt`, `InMemoryLedgerEntryRepository.kt`, `InMemoryAccountTypeRepository.kt`, `RecordingEventOutbox.kt`, `DirectTransactionRunner.kt`, `SequentialIdGenerator.kt`
- Test: `.../application/usecase/OpenAccountUseCaseTest.kt`, `GetAccountUseCaseTest.kt`

**Interfaces:**
- Consumes: Tasks 2–3.
- Produces:
  - `interface AccountRepository { fun insert(account: Account, openingIdempotencyKey: String): InsertAccountResult; fun findById(id: UUID): Account?; fun lockById(id: UUID): Account?; fun findByOpeningIdempotencyKey(key: String): Account?; fun updateType(account: Account) }` — `lockById` faz `SET LOCAL lock_timeout` + `FOR UPDATE` e lança `AccountBusyException` ao estourar.
  - `enum class InsertAccountResult { INSERTED, DUPLICATE_OPENING_KEY, DUPLICATE_CUSTOMER }`.
  - `interface LedgerEntryRepository { fun findLast(accountId: UUID): LedgerEntry?; fun findByIdempotencyKey(accountId: UUID, key: String): LedgerEntry?; fun insert(entry: LedgerEntry) }`.
  - `interface AccountTypeRepository { fun findLimit(type: AccountType): OverdraftLimit; fun findAll(): List<AccountTypeLimit>; fun updateLimit(type: AccountType, limit: OverdraftLimit, now: Instant): AccountTypeLimit }`.
  - `interface EventOutbox { fun append(event: AccountEvent) }`.
  - `interface TransactionRunner { fun <T> inTransaction(block: () -> T): T }`.
  - `interface IdGenerator { fun newId(): UUID }`.
  - Exceções (`RuntimeException`): `AccountNotFoundException(accountId)`, `AccountBusyException(accountId)`, `IdempotencyKeyReusedException(key)`, `CustomerAlreadyHasAccountException(customerId)`.
  - `class OpenAccountUseCase(accounts, outbox: EventOutbox, tx: TransactionRunner, ids: IdGenerator) { fun execute(command: OpenAccountCommand): OpenAccountResult }`; `data class OpenAccountCommand(val customerId: UUID, val idempotencyKey: String)`; `data class OpenAccountResult(val account: Account, val created: Boolean)`.
  - `class GetAccountUseCase(accounts, entries, types) { fun execute(accountId: UUID): AccountView }`; `data class AccountView(val account: Account, val balance: Balance, val overdraftLimit: OverdraftLimit, val availableBalance: AvailableBalance)`.
  - Os fakes são reutilizados pelas Tasks 5 e 6. `InMemoryAccountRepository` simula os três resultados de `insert`.

- [ ] **Step 1:** Testes: abrir conta nova → `created = true`, tipo `FREE`, um `AccountOpened` no outbox; mesma chave de novo → `created = false`, mesma conta, nenhum evento novo; outro cliente já com conta usando outra chave → `CustomerAlreadyHasAccountException`; consultar conta sem lançamentos tipo `STANDARD` (limite 50000) → saldo 0, disponível 50000; conta inexistente → `AccountNotFoundException`.
- [ ] **Step 2:** Rodar `./gradlew :services:accounts:application:test` → falha.
- [ ] **Step 3:** Implementar. `OpenAccountUseCase`: `insert`; `DUPLICATE_OPENING_KEY` → `findByOpeningIdempotencyKey` e `created = false`; `DUPLICATE_CUSTOMER` → exceção; o evento só é gravado no `INSERTED`, tudo dentro de `tx.inTransaction`.
- [ ] **Step 4:** Rodar → passa; `./gradlew build` verde.
- [ ] **Step 5:** Commit `feat(accounts): abrir e consultar conta`.

### Task 5: Casos de uso de crédito e débito (Marina, `dev-kotlin`)

**Files:**
- Create: `.../application/usecase/RecordCreditUseCase.kt`, `RecordDebitUseCase.kt`, `RecordEntryCommand.kt`, `IdempotentEntryLookup.kt` (`internal`)
- Test: `.../application/usecase/RecordCreditUseCaseTest.kt`, `RecordDebitUseCaseTest.kt`

**Interfaces:**
- Consumes: Task 4 (portas e fakes).
- Produces:
  - `data class RecordEntryCommand(val accountId: UUID, val amount: Money, val description: String?, val idempotencyKey: String)`.
  - `data class RecordEntryResult(val entry: LedgerEntry, val created: Boolean)`.
  - `class RecordCreditUseCase(accounts, entries, outbox, tx, ids, clock: java.time.Clock) { fun execute(command: RecordEntryCommand): RecordEntryResult }`.
  - `class RecordDebitUseCase(accounts, entries, types, outbox, tx, ids, clock) { fun execute(command: RecordEntryCommand): DebitOutcome }`; `sealed interface DebitOutcome { data class Recorded(val result: RecordEntryResult); data class Refused(val availableBalance: AvailableBalance) }`.
  - Ordem dentro da transação (spec, seção 4): `lockById` (nulo → `AccountNotFoundException`) → `findByIdempotencyKey` (mesmo `kind` e `amount` → `created = false`; diferente → `IdempotencyKeyReusedException`) → `findLast` → (débito: `findLimit`) → domínio → `insert` + `append`.

- [ ] **Step 1:** Testes: crédito grava lançamento e um `LedgerEntryRecorded`; repetição com a mesma chave → `created = false`, nenhum lançamento nem evento novo; mesma chave com outro valor → `IdempotencyKeyReusedException`; mesma chave usada antes num **crédito** e agora num **débito** de mesmo valor → `IdempotencyKeyReusedException`; débito recusado → `Refused` e nada gravado nem publicado; débito em conta inexistente → `AccountNotFoundException`; o `RecordingEventOutbox` recebe o evento depois do lançamento (mesma chamada de `inTransaction`).
- [ ] **Step 2:** Rodar → falha.
- [ ] **Step 3:** Implementar; `IdempotentEntryLookup` concentra a regra de repetição para os dois casos de uso.
- [ ] **Step 4:** Rodar → passa; `./gradlew build` verde.
- [ ] **Step 5:** Commit `feat(accounts): crédito e débito com idempotência`.

### Task 6: Casos de uso de tipo e limites (Marina, `dev-kotlin`)

**Files:**
- Create: `.../application/usecase/ChangeAccountTypeUseCase.kt`, `ListAccountTypesUseCase.kt`, `ChangeOverdraftLimitUseCase.kt`
- Test: os três `*Test.kt`

**Interfaces:**
- Consumes: Task 4.
- Produces:
  - `class ChangeAccountTypeUseCase(accounts, outbox, tx) { fun execute(accountId: UUID, newType: AccountType): Account }` — `lockById`; mesmo tipo → devolve sem `updateType` nem evento.
  - `class ListAccountTypesUseCase(types) { fun execute(): List<AccountTypeLimit> }` — ordenado pela ordem do enum.
  - `class ChangeOverdraftLimitUseCase(types, tx, clock) { fun execute(type: AccountType, limit: OverdraftLimit): AccountTypeLimit }`.

- [ ] **Step 1:** Testes: upgrade grava `AccountTypeChanged` e atualiza o tipo; mesmo tipo → nenhum evento; conta inexistente → `AccountNotFoundException`; lista devolve os cinco tipos na ordem `FREE…DIAMOND`; alterar limite de `GOLD` para 300000 → `updatedAt` = instante do `Clock` fixo.
- [ ] **Step 2:** Rodar → falha. **Step 3:** Implementar. **Step 4:** Rodar → passa; `./gradlew build` verde.
- [ ] **Step 5:** Commit `feat(accounts): troca de tipo e limites por tipo`.

### Task 7: `libs/outbox` — envelope e writer (Marie, `dev-kotlin`; em paralelo às Tasks 2–6)

**Files:**
- Modify: `settings.gradle.kts` (incluir `:libs:outbox`)
- Create: `libs/outbox/build.gradle.kts` (plugins `digitalbanking.spring-module` e `digitalbanking.integration-test`; `spring-boot-starter-jdbc`, `java-uuid-generator`, `slf4j-api`)
- Create em `libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/`: `OutboxMessage.kt`, `JdbcOutboxWriter.kt`, `OutboxEnvelope.kt`
- Create: `libs/outbox/src/main/resources/outbox/outbox-table.sql` (DDL modelo; os serviços copiam para uma migration)
- Test: `libs/outbox/src/integrationTest/kotlin/.../outbox/JdbcOutboxWriterIT.kt` (Testcontainers PostgreSQL aplicando o DDL modelo)

**Interfaces:**
- Produces:
  - `data class OutboxMessage(val eventType: String, val eventVersion: Int, val aggregateId: String, val payloadJson: String)`.
  - `class JdbcOutboxWriter(private val jdbcClient: JdbcClient, private val clock: Clock) { fun write(message: OutboxMessage) }` — gera `event_id` UUID v7 (JUG `Generators.timeBasedEpochGenerator()`), `occurred_at = clock.instant()`, `correlation_id = MDC.get("correlationId")` (pode ser nulo).
  - `object OutboxEnvelope { fun toJson(row: …): String }` fica para o bloco 2; nesta task só a constante `const val CORRELATION_ID_MDC_KEY = "correlationId"` em `OutboxEnvelope`.
  - DDL: tabela `outbox` exatamente como a spec, seção 3 (`position bigserial` PK, `event_id uuid unique`, `aggregate_id text`, `event_type text`, `event_version int`, `payload jsonb`, `correlation_id text`, `occurred_at timestamptz`, `published_at timestamptz null`, `attempts int not null default 0`, `last_error text`) + `create index outbox_unpublished_idx on outbox (position) where published_at is null`.

- [ ] **Step 1:** IT: `write` grava uma linha com `published_at` nulo, `attempts 0`, `payload` igual ao JSON (comparar como JSON), `correlation_id` igual ao valor posto no MDC; duas escritas seguidas → `position` crescente e `event_id` diferentes e com versão 7 (`UUID.version() == 7`).
- [ ] **Step 2:** `./gradlew :libs:outbox:integrationTest` → falha. **Step 3:** Implementar. **Step 4:** Passa; `./gradlew build` verde.
- [ ] **Step 5:** Commit `feat(outbox): envelope e writer JDBC do outbox`.

### Task 8: Esquema, usuários de banco e repositórios JDBC (Marina, `dev-kotlin`; o script de init do compose segue o padrão do Rafael)

**Files:**
- Create em `services/accounts/adapters/src/main/resources/db/migration/`: `V1__account_type.sql`, `V2__account.sql`, `V3__ledger_entry.sql`, `V4__outbox.sql` (cópia do DDL modelo da Task 7), `V5__app_grants.sql` (com o placeholder `${appUser}`)
- Create em `services/accounts/adapters/src/main/kotlin/.../adapters/persistence/`: `JdbcAccountRepository.kt`, `JdbcLedgerEntryRepository.kt`, `JdbcAccountTypeRepository.kt`, `SpringTransactionRunner.kt`, `UuidV7IdGenerator.kt`, `OutboxEventAppender.kt`
- Modify: `services/accounts/adapters/build.gradle.kts` (+ `:libs:outbox`, `java-uuid-generator`, `spring-boot-starter-jackson`), `services/accounts/bootstrap/src/main/resources/application.yml`, `infra/local/postgres/init/01-create-service-databases.sh`, `infra/local/.env.example`, `infra/local/docker-compose.yml`
- Create: `services/accounts/bootstrap/src/integrationTest/kotlin/.../bootstrap/AccountsIntegrationTest.kt` (base), `services/accounts/bootstrap/src/integrationTest/resources/db/create-app-role.sql`
- Modify: `AccountsApplicationIT.kt` (estende a base)
- Test: `.../bootstrap/persistence/JdbcRepositoriesIT.kt`

**Interfaces:**
- Consumes: Tasks 4 e 7.
- Produces:
  - Implementações das portas da Task 4. `JdbcAccountRepository.lockById` executa `SET LOCAL lock_timeout = '<accounts.lock-timeout>'` (padrão `2s`) e `SELECT … FOR UPDATE`; SQLState `55P03` → `AccountBusyException`. `insert` traduz violação de `account_opening_idempotency_key_key` → `DUPLICATE_OPENING_KEY` e de `account_customer_id_key` → `DUPLICATE_CUSTOMER` (nomeie as constraints assim nas migrations).
  - `SpringTransactionRunner(TransactionTemplate)`.
  - `OutboxEventAppender(writer: JdbcOutboxWriter, objectMapper) : EventOutbox` — mapeia cada `AccountEvent` para `OutboxMessage` com `eventVersion = 1`, `aggregateId = accountId.toString()` e o payload da spec, seção 5 (valores em centavos como número, datas ISO-8601 UTC).
  - `application.yml`: `spring.datasource.username: ${ACCOUNTS_DB_USER:accounts_app}`, `spring.flyway.user: ${ACCOUNTS_DB_OWNER_USER:accounts_owner}`, `spring.flyway.password: ${ACCOUNTS_DB_OWNER_PASSWORD:accounts_owner}`, `spring.flyway.placeholders.appUser: ${ACCOUNTS_DB_USER:accounts_app}`, `accounts.lock-timeout: 2s`.
  - Init do PostgreSQL: cria `accounts_owner` (dono do banco `accounts`) e `accounts_app` (só `LOGIN`), senhas de `ACCOUNTS_DB_OWNER_PASSWORD` e `ACCOUNTS_DB_PASSWORD`. `V5` dá `USAGE` no schema, `SELECT, INSERT` em `ledger_entry`, `SELECT, INSERT, UPDATE` em `account`, `account_type` e `outbox`, `USAGE` na sequência de `outbox.position`.
  - `AccountsIntegrationTest` (abstrata, `@SpringBootTest(RANDOM_PORT)`): container `postgres:17-alpine` com `withInitScript("db/create-app-role.sql")` (cria `accounts_app` com senha `app`) e `@DynamicPropertySource` apontando datasource para `accounts_app` e Flyway para o usuário do container. Usada por todos os ITs do `accounts`.

- [ ] **Step 1:** ITs (`JdbcRepositoriesIT`): `account_type` vem semeada com os cinco limites da spec (0 / 50000 / 200000 / 1000000 / 5000000); inserir conta e reler; segunda conta do mesmo cliente → `DUPLICATE_CUSTOMER`; mesma chave → `DUPLICATE_OPENING_KEY`; `findLast` devolve o de maior `sequence`; **`UPDATE ledger_entry` e `DELETE FROM ledger_entry` executados com `accounts_app` falham** (permissão negada); `lockById` com a conta travada por outra transação → `AccountBusyException` em menos de 3 s; `OutboxEventAppender` grava `LedgerEntryRecorded` com o payload esperado.
- [ ] **Step 2:** `./gradlew :services:accounts:bootstrap:integrationTest` → falha. **Step 3:** Implementar. **Step 4:** Passa; `./gradlew build` verde; `docker compose -f infra/local/docker-compose.yml down -v && … up --wait` saudável e `bootRun` sobe com os dois usuários.
- [ ] **Step 5:** Commit `feat(accounts): esquema, usuários de banco e repositórios JDBC`.

### Task 9: API HTTP, Problem Details e correlation ID (Marina, `dev-kotlin`)

**Files:**
- Create em `services/accounts/adapters/src/main/kotlin/.../adapters/web/`: `AccountController.kt`, `AccountTypeAdminController.kt`, `ProblemDetailsExceptionHandler.kt`, `CorrelationIdFilter.kt`, `dto/*.kt`
- Create: `services/accounts/bootstrap/src/main/kotlin/.../bootstrap/UseCaseConfiguration.kt` (`@Bean` de cada `*UseCase` e do `Clock.systemUTC()`)
- Modify: `services/accounts/adapters/build.gradle.kts` (+ `spring-boot-starter-validation`)
- Test: `.../bootstrap/web/AccountApiIT.kt`, `AccountTypeAdminApiIT.kt`

**Interfaces:**
- Consumes: Tasks 4–8.
- Produces: os endpoints, status e slugs da spec, seção 6, exatamente. `type` do Problem Details = `/problems/<slug>`. `422 insufficient-available-balance` com a propriedade `availableBalance`. `503 account-busy` com `Retry-After: 1`. `400 validation-error` com a propriedade `errors: [{field, message}]`. `CorrelationIdFilter` usa a chave `OutboxEnvelope.CORRELATION_ID_MDC_KEY`, lê/gera `X-Correlation-Id` e o devolve na resposta.

- [ ] **Step 1:** ITs cobrindo cada linha da tabela da spec, seção 6 (status e `type` do problema), mais os itens 1, 2, 4 e 5 do Review Focus; resposta traz `X-Correlation-Id` igual ao enviado, ou um gerado; o `correlation_id` gravado no outbox é o do header.
- [ ] **Step 2:** Rodar → falha. **Step 3:** Implementar. **Step 4:** Passa; `./gradlew build` verde.
- [ ] **Step 5:** Commit `feat(accounts): api http com problem details e correlation id`.

### Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`)

**Files:**
- Modify: `services/accounts/adapters/build.gradle.kts` e `application.yml` (springdoc, se a Task 1 confirmou; senão `services/accounts/adapters/src/main/resources/openapi/accounts.yaml` + teste que valida as rotas contra o YAML)
- Test: `.../bootstrap/ConcurrencyIT.kt`, `.../bootstrap/OpenApiIT.kt`

**Interfaces:**
- Consumes: Task 9.

- [ ] **Step 1:** `ConcurrencyIT`:
  - conta `FREE` com crédito de 3700; 100 débitos de 100 em paralelo (virtual threads, chaves distintas) → exatamente 37 respostas `201` e 63 `422`; saldo final 0; `sequence` 1..38 sem buraco (1 do crédito + 37);
  - 20 débitos paralelos com a **mesma** chave → um `201`, dezenove `200`, um único lançamento;
  - número de linhas `LedgerEntryRecorded` no outbox = número de lançamentos.
- [ ] **Step 2:** `OpenApiIT`: `GET /v3/api-docs` (ou o YAML) contém os sete caminhos/métodos da spec.
- [ ] **Step 3:** Rodar → falha onde ainda faltar; implementar; passa; `./gradlew build` verde.
- [ ] **Step 4:** Commit `test(accounts): concorrência, idempotência e contrato openapi`.

### Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`)

**Files:**
- Modify: `services/accounts/bootstrap/src/test/kotlin/.../bootstrap/ArchitectureTest.kt`, `services/accounts/bootstrap/build.gradle.kts` (+ `:libs:outbox` no classpath de teste), `AccountsApplicationIT.kt`
- Test: o próprio `ArchitectureTest`

- [ ] **Step 1:** Regras (sem `allowEmptyShould`): `domain` sem Spring/application/adapters; `application` sem Spring/adapters; `adapters.web` → `adapters.persistence` (um teste); `adapters.persistence` → `adapters.web` (outro teste); classes em `..application.usecase..` cujo nome não termina em `Command`, `Result`, `Outcome` ou `View` e que não são `internal` terminam em `UseCase`; classes com `@RestController` terminam em `Controller`; `io.github.capozziv.digitalbanking.outbox..` não depende de `io.github.capozziv.digitalbanking.accounts..`.
- [ ] **Step 2:** **Ver cada regra falhar:** para cada uma, criar temporariamente uma classe violadora, rodar `./gradlew :services:accounts:bootstrap:test`, colar a falha no relato e apagar a classe.
- [ ] **Step 3:** Health IT: trocar os casts por `assertThat(health).extractingByKey("components").asInstanceOf(InstanceOfAssertFactories.MAP)…`. Virtual threads: endpoint de teste só no source set `integrationTest` (ou `Thread.currentThread().isVirtual` capturado num filtro de teste) provando que a requisição roda numa virtual thread.
- [ ] **Step 4:** `./gradlew build` verde.
- [ ] **Step 5:** Commit `test(accounts): regras archunit da etapa 1 e melhorias da etapa 0`.

### Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`)

**Files:**
- Modify: `infra/local/nginx/nginx.conf`, `.github/workflows/ci.yml` (job `smoke`), `docs/status.md` → seção "Como rodar localmente" (repassar ao coordenador)
- Create: `api/bruno/accounts/bruno.json`, `environments/local.bru`, uma `.bru` por endpoint da spec, seção 6
- Create: `docs/rodando-localmente.md` — guia para o usuário rodar na própria máquina (macOS): pré-requisitos (JDK 21 instalado, porque `org.gradle.java.installations.auto-download=false`; Docker Desktop em execução; a partir do bloco 2, `LOCALSTACK_AUTH_TOKEN` em `infra/local/.env`), `cp infra/local/.env.example infra/local/.env`, `docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml up --wait`, `./gradlew build`, `./gradlew test` (sem Docker), `bootRun`, a sequência de `curl` do smoke, a coleção Bruno e `down -v`. O `docs/status.md` passa a apontar para ele (coordenador). A Task 16 acrescenta o LocalStack e a leitura da fila de inspeção.

**Interfaces:**
- Consumes: Task 9.
- nginx: `location /accounts/actuator/ { proxy_pass http://host.docker.internal:8081/actuator/; }`, `location /accounts { proxy_pass http://host.docker.internal:8081; }`, `location /admin/account-types { … }` (sem a barra final: caminho preservado), mais `X-Correlation-Id` repassado.
- Smoke: depois do health, via `curl` no nginx: `POST /accounts` (com `Idempotency-Key`) → `201`; `POST …/credits` 1000 → `201`; `POST …/debits` 300 → `201`; `GET /accounts/{id}` → `balance 700`. Falha → `cat accounts.log`. Variáveis de banco do jar: `ACCOUNTS_DB_USER`, `ACCOUNTS_DB_PASSWORD`, `ACCOUNTS_DB_OWNER_USER`, `ACCOUNTS_DB_OWNER_PASSWORD` do `.env.example`.

- [ ] **Step 1:** Rodar localmente (nuvem): compose `down -v` + `up --wait`, `bootRun`, executar a sequência do smoke à mão → valores esperados.
- [ ] **Step 2:** Coleção Bruno com `Idempotency-Key` gerada por `{{$guid}}`; rodar com `bru run` se a CLI estiver disponível (senão, registrar que não foi rodada).
- [ ] **Step 3:** `actionlint` sem erros; seguir o `docs/rodando-localmente.md` do zero (compose `down -v`) e conferir que cada comando funciona como escrito.
- [ ] **Step 4:** Commit `build: nginx com caminhos da api, smoke com operações, coleção bruno e guia para rodar localmente`.

### Task 13: Documentação do bloco 1 e das decisões da etapa (Clara, `documentador`; em paralelo às Tasks 8–12)

**Files:**
- Create: `docs/adr/0015-outbox-relay-lider-e-sns-fifo.md`
- Modify: `docs/adr/0001-*.md`, `docs/adr/0003-*.md`, `docs/adr/0014-*.md`, `docs/adr/README.md`, `docs/arquitetura.md`, `docs/roadmap.md`, `CLAUDE.md` (com `mattpocock-skills:writing-for-agents`)

- [ ] **Step 1:** ADR 0015 e atualizações conforme a spec, seção 10 (alternativas listadas lá).
- [ ] **Step 2:** `CLAUDE.md`: regra de sufixos nas "Regras inegociáveis", uma linha.
- [ ] **Step 3:** `docs/arquitetura.md`: remove de "Em aberto" o emulador AWS e o relay; registra idempotência por chave no lançamento, SNS FIFO e o relay. `docs/roadmap.md`: etapa 1 com outbox, relay, SNS FIFO e LocalStack; etapa 3 com SQS, DLQ, `notification` e consumidores idempotentes.
- [ ] **Step 4:** Relatar ao coordenador os itens de débito técnico da spec ("Fora de escopo") para o `docs/status.md`.
- [ ] **Step 5:** Commit `docs: adr 0015 e decisões da etapa 1`.

**Fim do bloco 1:** revisão da branch pela Beatriz (`revisor`), `superpowers:verification-before-completion`, PR do bloco 1 para a `main`.

---

# Bloco 2 — relay e SNS FIFO

**Pré-requisito (usuário):** `LOCALSTACK_AUTH_TOKEN` disponível no ambiente de execução e como segredo do GitHub. Sem ele: parar e pedir.

### Task 14: Verificação do SNS FIFO no LocalStack (Rafael, `infra`)

**Files:**
- Create: `libs/testing-localstack` (módulo `:libs:testing-localstack`, sem Spring, consumido só por `integrationTestImplementation`): `LocalStackTestSupport` com `IMAGE` (`localstack/localstack:2026.10.2`), `authToken(env)` (sem token: pula fora do CI e falha com `CI=true`), `container(services, env)`, `snsClient` e `sqsClient`; expõe em `api` o SDK `sns`/`sqs` e o `testcontainers-localstack`
- Modify: `libs/outbox/build.gradle.kts` (`integrationTestImplementation(project(":libs:testing-localstack"))`), `settings.gradle.kts`, `build-logic/.../digitalbanking.integration-test.gradle.kts` (presença do token e `CI` como entradas do `integrationTest`, sem guardar o valor)
- Test: `libs/outbox/src/integrationTest/kotlin/.../outbox/LocalStackFifoIT.kt`, `libs/testing-localstack/src/test/.../LocalStackTestSupportTest.kt`

- [x] **Step 1:** IT com `LocalStackContainer` (imagem fixa da Task 1, token por variável de ambiente): cria tópico `t.fifo` e fila `q.fifo` com assinatura `RawMessageDelivery=false`; publica A1, B1, A2, B2, A3 (`MessageGroupId` A/B) e A2 repetido com o mesmo `MessageDeduplicationId`; lê a fila → por grupo, ordem A1, A2, A3 e B1, B2; sem duplicata de A2; atributos `eventType` e `correlationId` presentes.
- [x] **Step 2:** Rodar. **Se falhar por falta de suporte do LocalStack, parar e reportar ao coordenador** (decisão do usuário). Resultado: o LocalStack `2026.10.2` suporta SNS FIFO → SQS FIFO.
- [x] **Step 3:** Commit `test(outbox): sns fifo no localstack`.

### Task 15: Relay e publicador SNS (Marina, `dev-kotlin`)

**Files:**
- Create em `libs/outbox/src/main/kotlin/.../outbox/`: `PendingEvent.kt`, `EventPublisher.kt`, `SnsEventPublisher.kt`, `OutboxStore.kt`, `JdbcOutboxStore.kt`, `OutboxRelay.kt`, `OutboxRelaySettings.kt`, `OutboxMetrics.kt`; completar `OutboxEnvelope.kt` (`toJson`); índice `outbox_stuck_idx` no DDL modelo
- Create: `services/accounts/.../db/migration/V6__outbox_stuck_idx.sql` (o `V4` já está aplicado)
- Test: `libs/outbox/src/test/kotlin/.../outbox/OutboxRelayTest.kt` (fakes de `OutboxStore` e `EventPublisher`), `OutboxRelaySettingsTest.kt`, `OutboxEnvelopeTest.kt`, `OutboxMetricsTest.kt`, `SnsEventPublisherTest.kt`; `libs/outbox/src/integrationTest/kotlin/.../outbox/JdbcOutboxStoreIT.kt`, `SnsEventPublisherIT.kt`

**Interfaces:**
- Consumes: Tasks 7 e 14.
- Produces:
  - `data class PendingEvent(val position: Long, val eventId: UUID, val aggregateId: String, val eventType: String, val eventVersion: Int, val payloadJson: String, val correlationId: String?, val occurredAt: Instant, val attempts: Int)`.
  - `interface EventPublisher { fun publish(event: PendingEvent) }` — lança exceção em falha.
  - `class SnsEventPublisher(snsClient: SnsClient, topicArn: String, publishTimeout: Duration) : EventPublisher` — corpo = `OutboxEnvelope.toJson(event)` (campos da spec, seção 5), `MessageGroupId = aggregateId`, `MessageDeduplicationId = eventId`, atributos `eventType` e `correlationId` (este só se não nulo); `apiCallTimeout = publishTimeout` em cada `PublishRequest`; `publishTimeout` sem padrão.
  - `interface OutboxStore { fun <T : Any> asLeader(lockKey: Long, block: () -> T): T?; fun fetchPending(limit: Int): List<PendingEvent>; fun markPublished(position: Long, at: Instant); fun markFailed(position: Long, error: String); fun backlog(now: Instant): OutboxBacklog }` — `asLeader` abre transação, tenta `pg_try_advisory_xact_lock(lockKey)` e devolve `null` sem executar `block` se não conseguir. `fetchPending` lê os pendentes em ordem de `position` e pula os eventos de conta presa (conta com evento pendente anterior e `attempts > 0`); só a cabeça da conta presa entra no lote. `markFailed` incrementa `attempts` e grava `last_error`. `backlog` conta todos os pendentes e a idade do mais antigo, fora da transação de líder.
  - `data class OutboxBacklog(val pending: Long, val oldestAge: Duration?)`.
  - `class JdbcOutboxStore(jdbcClient: JdbcClient, transactions: TransactionOperations) : OutboxStore` — leitura do lote com `not exists` apoiada nos índices parciais `outbox_unpublished_idx` e `outbox_stuck_idx (aggregate_id, position) where published_at is null and attempts > 0`.
  - `data class OutboxRelaySettings(val batchSize: Int = 100, val alertAfterAttempts: Int = 10, val lockKey: Long, val publishTimeout: Duration = Duration.ofSeconds(2), val maxCycleDuration: Duration = Duration.ofSeconds(10))` — valores positivos; `lockKey` constante e única por serviço, distinta de outras travas consultivas do banco.
  - `class OutboxRelay(store, publisher, settings, clock, metrics: OutboxMetrics) { fun runCycle() }` — algoritmo da spec, seção 5, passos 1–4. O prazo do ciclo (`maxCycleDuration`) é conferido antes de cada evento; estourado, um `WARN` e o lote termina (pior caso: `maxCycleDuration + publishTimeout`). Falha abaixo do limiar: `WARN` sem stack trace; no limiar: `ERROR` com a exceção. Erro de banco no `markPublished` propaga.
  - `class OutboxMetrics(registry: MeterRegistry)` — gauges `outbox.pending`, `outbox.oldest.pending.age` (segundos) e contadores `outbox.publish.failures` e `outbox.relay.cycles` (ciclos concluídos, com ou sem liderança).

- [x] **Step 1:** Testes unitários do relay: publica em ordem de `position`; falha no 2º evento da conta A → 3º evento de A não é publicado no ciclo, eventos de B são; conta presa não impede eventos de outras contas no ciclo seguinte; `markFailed` com a mensagem do erro; log `ERROR` quando `attempts + 1 >= alertAfterAttempts` (capturar com appender de teste); prazo do ciclo; sem liderança → nada lido nem publicado.
- [x] **Step 2:** IT do `JdbcOutboxStore`: dois `asLeader` concorrentes → só um executa o bloco; `fetchPending` respeita os índices parciais, a ordem e pula conta presa; plano sem `Seq Scan` com outbox grande.
- [x] **Step 3:** Rodar → falha; implementar; passa; `./gradlew build` verde.
- [x] **Step 4:** Commit `feat(outbox): relay líder e publicador sns fifo`.

### Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff)

**Files:**
- Create: `services/accounts/bootstrap/src/main/kotlin/.../bootstrap/OutboxRelayConfiguration.kt` (`@EnableScheduling`, `@ConditionalOnBooleanProperty("accounts.outbox.relay-enabled", matchIfMissing = true)`; beans de `OutboxRelaySettings` com `RELAY_LOCK_KEY`, `AwsCredentialsProvider`, `SnsClient` com `apiCallTimeout`/`apiCallAttemptTimeout` = `publishTimeout`, `SnsEventPublisher(snsClient, topicArn, settings.publishTimeout)`, `JdbcOutboxStore(jdbcClient, transactionTemplate)`, `OutboxMetrics`, `OutboxRelay` e `OutboxRelayScheduler`), `OutboxRelayProperties` e `AwsProperties` (`@ConfigurationProperties`)
- Create: `.../bootstrap/OutboxRelayScheduler.kt` — `@Scheduled(fixedDelayString = "\${accounts.outbox.poll-interval:500ms}")` chamando `runCycle`; desligamento gracioso próprio (`DisposableBean`: nenhum ciclo novo e espera o ciclo em andamento por `maxCycleDuration + publishTimeout + 1 s`); bean com `@DependsOn("taskScheduler")`, para ser destruído antes do agendador do Boot e antes do pool de conexões
- Modify: `application.yml` (`accounts.outbox.*` com `relay-enabled`, `batch-size`, `alert-after-attempts`, `publish-timeout`, `max-cycle-duration`, `poll-interval`; `accounts.aws.endpoint`, `accounts.aws.region`, `accounts.aws.topic-arn`; `management.endpoints.web.exposure.include: health,metrics`), `services/accounts/bootstrap/build.gradle.kts` (`libs/outbox` em `implementation`, `libs/testing-localstack` no `integrationTest`, scripts de init do LocalStack como entrada com caminho relativo), `infra/local/docker-compose.yml` (+ `localstack`, imagem fixa, `LOCALSTACK_AUTH_TOKEN`, healthcheck em `python3` pelo `/_localstack/init/ready`), `infra/local/localstack/init/ready.d/01-accounts-topic.sh` (tópico `accounts-events.fifo`, fila `accounts-events-inspection.fifo`, assinatura com `RawMessageDelivery=true`), `infra/local/.env.example`, `infra/local/smoke.sh` (lê a fila de inspeção e confere `AccountOpened` e dois `LedgerEntryRecorded`, em ordem), `.github/workflows/ci.yml` (segredo no `build` e no `smoke`), `docs/rodando-localmente.md`
- Modify: `AccountsIntegrationTest` (`accounts.outbox.relay-enabled=false` nos ITs que não exercitam o relay)
- Test: `.../bootstrap/OutboxRelayIT.kt` (base `AccountsIntegrationTest` + LocalStack), `.../bootstrap/OutboxRelayUnreachableBrokerIT.kt` (endpoint inválido, sem LocalStack), `.../bootstrap/OutboxRelaySchedulerTest.kt` (unitário)

- [x] **Step 1:** `OutboxRelayIT`: operações em duas contas → na fila de inspeção, por conta, os eventos na ordem de `sequence`, com `eventType` e `correlationId`; **dois `OutboxRelay` rodando `runCycle` em paralelo em loop** → nenhum evento duplicado nem fora de ordem; métricas do relay no actuator. `OutboxRelayUnreachableBrokerIT`: com o endpoint do SNS inválido, `attempts` cresce e o `/actuator/health` continua `UP`; o agendador do relay depende do `taskScheduler`.
- [x] **Step 2:** Rodar → falha; implementar; passa; `./gradlew build` verde; compose saudável com LocalStack; `actionlint` sem erros.
- [x] **Step 3:** Commit `feat(accounts): relay do outbox publicando no sns fifo`.

**Fim do bloco 2:** revisão da branch pela Beatriz, verificação, PR, `docs/status.md` atualizado pelo coordenador.
