# Etapa 4 — `statement`: plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** serviço `statement` que projeta os lançamentos do `accounts` no DynamoDB (`PutItem` condicional) e expõe o extrato paginado em `GET /accounts/{accountId}/statement`.

**Architecture:** quatro módulos Clean Architecture (`domain`, `application`, `adapters`, `bootstrap`), como o `notification`. O consumo usa `@SqsListener` (Spring Cloud AWS) na fila `statement-accounts.fifo`; a persistência usa o `DynamoDbClient` de baixo nível num adapter só. A idempotência vem da chave do item (`accountId` + `sequence`), sem tabela de controle.

**Tech Stack:** Kotlin, Spring Boot 4.1.1, Spring Cloud AWS 4.2.1 (SQS), AWS SDK v2 2.57.1 (`dynamodb`), LocalStack 2026.10.2 via Testcontainers, JUnit 5, ArchUnit, ktlint.

**Spec:** [`docs/superpowers/specs/2026-11-10-etapa-4-statement-design.md`](../specs/2026-11-10-etapa-4-statement-design.md) e [ADR 0018](../../adr/0018-chave-do-extrato-pela-sequencia.md).

## Global Constraints
- Pacote raiz `io.github.capozziv.digitalbanking.statement`; porta `8084`; sem PostgreSQL.
- Tabela `statement`: partição `accountId` (`S`), ordenação `sequence` (`N`); item da conta com `sequence = 0`.
- Fila `statement-accounts.fifo`, DLQ `statement-accounts-dlq.fifo`, assinada em `accounts-events.fifo`, raw message delivery, 5 tentativas, visibilidade 30 s (configuráveis por `STATEMENT_MAX_RECEIVE_COUNT` e `STATEMENT_VISIBILITY_TIMEOUT_SECONDS`).
- Endpoint AWS: `STATEMENT_AWS_ENDPOINT` vazio = AWS real; o ambiente local e os testes definem `http://localhost:4566` (ou o do contêiner) explicitamente.
- Paginação: `limit` padrão 20, de 1 a 100; ordem decrescente de `sequence`; cursor opaco em base64url.
- Código, testes, logs e comentários em inglês; commits em português, autor `capozziV <vitorcapozzi4@gmail.com>`, sem trailers.
- `domain` sem Spring e sem AWS SDK (ArchUnit via `libs/testing-architecture`); DTOs em `adapters/web/dto/` com sufixo `DTO`; centavos em `Long`; erros em Problem Details.
- Nenhuma classe importada de outro serviço; o envelope do outbox é lido como JSON.
- Agentes em worktree não regeneram nem commitam `graphify-out/`.
- Teste de regra nova roda o mutante (regra desligada) e fica vermelho.

## Review Focus
1. Lançamento que chega antes do `AccountOpened` (ou sem ele): o extrato lista os lançamentos e não devolve `404`. Teste na Task 3 e na Task 4.
2. `balanceAfter` negativo (conta no cheque especial) volta negativo, sem perder o sinal. Teste na Task 4.
3. `description` nula: o atributo não é gravado e volta `null`, sem string vazia. Teste na Task 4.
4. Cursor da última página (`sequence` 1): página vazia com `nextCursor` nulo, sem `404` e sem consulta. Cursor maior que a última `sequence` devolve os lançamentos com `sequence` menor que ele (página vazia só em conta sem lançamentos). Teste na Task 3.
5. `limit=0`, `limit=101`, `limit=abc` e cursor adulterado → `400 validation-error`, nunca `500`. Teste na Task 6.

---

### Task 1: Esqueleto Gradle e cliente DynamoDB no apoio de testes (Rafael, `infra`)

**Files:**
- Modify: `settings.gradle.kts` (incluir `:services:statement:domain`, `:application`, `:adapters`, `:bootstrap`)
- Modify: `gradle/libs.versions.toml` (`aws-sdk-dynamodb = { module = "software.amazon.awssdk:dynamodb" }`)
- Create: `services/statement/{domain,application,adapters,bootstrap}/build.gradle.kts`
- Modify: `libs/testing-localstack/build.gradle.kts` e `LocalStackTestSupport.kt`
- Test: `libs/testing-localstack/src/integrationTest/.../LocalStackTestSupportIntegrationTest.kt` (criar se não existir)

**Interfaces:**
- Produces: os quatro módulos compiláveis vazios; `LocalStackTestSupport.dynamoDbClient(container: LocalStackContainer): DynamoDbClient`.

Dependências de cada módulo, no padrão do `notification`:
- `domain`: `digitalbanking.kotlin-library`, sem dependências.
- `application`: `digitalbanking.kotlin-library`, `implementation(project(":services:statement:domain"))`.
- `adapters`: `digitalbanking.spring-module`; `domain`, `application`, `:libs:outbox` (só a chave de MDC); `spring-boot-starter-webmvc`, `-validation`, `springdoc`, `spring-boot-starter-jackson`, `jackson-module-kotlin`; BOMs do Spring Cloud AWS e do AWS SDK; `spring-cloud-aws-starter-sqs`; `aws-sdk-dynamodb`.
- `bootstrap`: `digitalbanking.spring-boot-app`; os três módulos, `actuator`, BOMs, `spring-cloud-aws-starter-sqs`, `aws-sdk-dynamodb`; test `:libs:testing-architecture`, `spring-boot-starter-test`; integrationTest `:libs:testing-localstack`, `spring-boot-starter-webmvc`, `testcontainers-junit-jupiter`.

- [ ] **Step 1:** teste de integração que sobe `LocalStackTestSupport.container(listOf("dynamodb"))`, cria uma tabela com o `dynamoDbClient` e confirma que `listTables()` a devolve. Rodar e ver falhar por compilação.
- [ ] **Step 2:** implementar `dynamoDbClient` igual ao `sqsClient` (endpoint, região, credenciais do contêiner).
- [ ] **Step 3:** `./gradlew :libs:testing-localstack:build :services:statement:domain:build :services:statement:application:build :services:statement:adapters:build :services:statement:bootstrap:build` → BUILD SUCCESSFUL.
- [ ] **Step 4:** commit `build: módulos do statement e cliente DynamoDB no apoio de testes`.

### Task 2: LocalStack, compose, nginx e CI (Rafael, `infra`)

**Files:**
- Create: `infra/local/localstack/init/ready.d/04-statement.sh`
- Modify: `infra/local/docker-compose.yml` (`SERVICES: sns,sqs,dynamodb`), `infra/local/.env.example` (variáveis `STATEMENT_*`)
- Modify: `infra/local/nginx/nginx.conf`
- Modify: `.github/workflows/ci.yml`

**Interfaces:**
- Produces: tabela, fila, DLQ e assinatura no LocalStack local; rota nginx; job `statement` no CI.

- [ ] **Step 1:** `04-statement.sh` no padrão do `03-notification-queues.sh`: cria a fila e a DLQ (`STATEMENT_MAX_RECEIVE_COUNT` padrão 5, `STATEMENT_VISIBILITY_TIMEOUT_SECONDS` padrão 30), assina `accounts-events.fifo` com `RawMessageDelivery=true` e cria a tabela `statement` (`accountId` `S` HASH, `sequence` `N` RANGE, `PAY_PER_REQUEST`) se ela não existir. Idempotente.
- [ ] **Step 2:** nginx: `location ~ ^/accounts/[^/]+/statement$` → `http://host.docker.internal:8084`; `location /statement/actuator/` → `http://host.docker.internal:8084/actuator/`. Os headers são os mesmos das outras rotas.
- [ ] **Step 3:** CI:
  - filtro `statement: services/statement/**`;
  - job `statement` igual ao `notification` (um `build` por módulo);
  - `statement` em `needs` e no `ci-ok`;
  - `bootJar` e `java -jar` do `statement` nos jobs de smoke e e2e, com o health em `127.0.0.1`.
- [ ] **Step 4:** `docker compose -f infra/local/docker-compose.yml up --wait` e depois `aws --endpoint-url http://127.0.0.1:4566 dynamodb describe-table --table-name statement` mostram a tabela. Rodar o script duas vezes não dá erro. `nginx -t` dentro do contêiner passa.
- [ ] **Step 5:** commit `infra: LocalStack, nginx e CI do statement`.

### Task 3: Domínio e casos de uso (Marina, `dev-kotlin`)

**Files:**
- Create em `services/statement/domain/src/main/kotlin/.../statement/domain/`: `EntryKind.kt`, `StatementEntry.kt`, `StatementCursor.kt`, `StatementPage.kt`
- Create em `services/statement/application/src/main/kotlin/.../statement/application/`: `port/StatementRepository.kt`, `usecase/RecordStatementEntryUseCase.kt`, `usecase/RegisterAccountUseCase.kt`, `usecase/GetStatementUseCase.kt`, `AccountNotFoundException.kt`, `ConflictingStatementEntryException.kt`
- Test: `domain/src/test/.../StatementEntryTest.kt`, `application/src/test/.../usecase/*UseCaseTest.kt`, `application/src/test/.../fake/InMemoryStatementRepository.kt`

**Interfaces:**
- Consumes: módulos da Task 1.
- Produces:
```kotlin
enum class EntryKind { CREDIT, DEBIT }
data class StatementEntry(
    val entryId: UUID, val accountId: UUID, val sequence: Long, val kind: EntryKind,
    val amountCents: Long, val balanceAfterCents: Long, val description: String?, val createdAt: Instant,
) // init: sequence >= 1, amountCents > 0, description null ou 1..140 caracteres
data class StatementCursor(val beforeSequence: Long) // init: beforeSequence >= 1
data class StatementPage(val entries: List<StatementEntry>, val nextCursor: StatementCursor?)

interface StatementRepository {
    fun insertEntry(entry: StatementEntry): Boolean            // false = a chave já existe
    fun findEntry(accountId: UUID, sequence: Long): StatementEntry?
    fun insertAccount(accountId: UUID, customerId: UUID): Boolean
    fun accountExists(accountId: UUID): Boolean
    fun findPage(accountId: UUID, cursor: StatementCursor?, limit: Int): StatementPage
}
enum class RecordOutcome { RECORDED, DUPLICATE }
class RecordStatementEntryUseCase(repository) { fun execute(entry: StatementEntry): RecordOutcome }
class RegisterAccountUseCase(repository) { fun execute(accountId: UUID, customerId: UUID) }
class GetStatementUseCase(repository) { fun execute(accountId: UUID, cursor: StatementCursor?, limit: Int): StatementPage }
class AccountNotFoundException(val accountId: UUID) : RuntimeException
class ConflictingStatementEntryException(val accountId: UUID, val sequence: Long, val storedEntryId: UUID, val receivedEntryId: UUID) : RuntimeException
```
- Contrato do `findPage`: entradas com `sequence < cursor.beforeSequence` (ou todas, sem cursor), em ordem decrescente, no máximo `limit`. O `nextCursor` vale `StatementCursor(última.sequence)` quando a página veio cheia e a última `sequence` é > 1; senão é nulo.

- [ ] **Step 1:** `StatementEntryTest`: `sequence` 0, `amountCents` 0 e `description` com 141 caracteres lançam `IllegalArgumentException`. Um `balanceAfterCents` negativo é aceito. Rodar: FAIL.
- [ ] **Step 2:** implementar o domínio. Rodar `:services:statement:domain:test`: PASS.
- [ ] **Step 3:** `RecordStatementEntryUseCaseTest`:
  - `records a new entry` → `RECORDED`;
  - `same entry twice is a duplicate` → `DUPLICATE`, um item só no repositório;
  - `same sequence with another entryId is a conflict` → `ConflictingStatementEntryException` com os dois ids.
- [ ] **Step 4:** `GetStatementUseCaseTest`:
  - `unknown account without entries is not found` → `AccountNotFoundException`;
  - `known account without entries is an empty page`;
  - `entries without AccountOpened are listed` (Review Focus 1);
  - `cursor past the last page is an empty page` (Review Focus 4): cursor 1 dá `entries` vazio e `nextCursor` nulo, sem exceção; cursor 999 numa conta com lançamentos devolve os de `sequence < 999`;
  - `limit outside 1..100 is rejected` (`IllegalArgumentException`).

  O `404` só sai sem cursor e com página vazia.
- [ ] **Step 5:** `RegisterAccountUseCaseTest`: registrar duas vezes não lança erro e `accountExists` é verdadeiro.
- [ ] **Step 6:** implementar os casos de uso e o `InMemoryStatementRepository` (que segue o contrato do `findPage`). Rodar o mutante do conflito (comparação de `entryId` desligada) e ver vermelho. Depois: `./gradlew :services:statement:domain:build :services:statement:application:build` → verde.
- [ ] **Step 7:** commit `feat(statement): domínio e casos de uso do extrato`.

### Task 4: Repositório DynamoDB e bootstrap (Marina, `dev-kotlin`)

**Files:**
- Create: `services/statement/adapters/src/main/kotlin/.../adapters/persistence/DynamoDbStatementRepository.kt`
- Create: `services/statement/bootstrap/src/main/kotlin/.../bootstrap/StatementApplication.kt`, `UseCaseConfiguration.kt`, `DynamoDbConfiguration.kt`
- Create: `services/statement/bootstrap/src/main/resources/application.yml`
- Test: `bootstrap/src/test/.../ArchitectureTest.kt`, `bootstrap/src/integrationTest/.../StatementIntegrationTest.kt` (base abstrata com LocalStack `dynamodb,sns,sqs` e a criação da tabela e das filas), `bootstrap/src/integrationTest/.../persistence/DynamoDbStatementRepositoryIntegrationTest.kt`

**Interfaces:**
- Consumes: `StatementRepository` (Task 3), `LocalStackTestSupport.dynamoDbClient` (Task 1).
- Produces: bean `DynamoDbClient`; `StatementIntegrationTest` (base usada pelas Tasks 5 e 6); `application.yml` com **todas** as chaves da etapa, para que as Tasks 5 e 6 não editem o mesmo arquivo:
```yaml
server.port: 8084
spring.cloud.aws: region.static ${STATEMENT_AWS_REGION:us-east-1}; credentials ${STATEMENT_AWS_ACCESS_KEY:test}/${STATEMENT_AWS_SECRET_KEY:test}; sqs.endpoint ${STATEMENT_AWS_ENDPOINT:}
statement.dynamodb.endpoint: ${STATEMENT_AWS_ENDPOINT:}
statement.dynamodb.table: ${STATEMENT_TABLE:statement}
statement.queues.accounts: ${STATEMENT_ACCOUNTS_QUEUE:statement-accounts.fifo}
statement.queues.accounts-dlq: ${STATEMENT_ACCOUNTS_DLQ:statement-accounts-dlq.fifo}
statement.dlq-metrics.refresh-interval: ${STATEMENT_DLQ_METRICS_REFRESH:30s}
management.endpoints.web.exposure.include: health,metrics
logging.pattern.correlation: "[%X{correlationId:-}] "
```

Atributos do item: `accountId` (`S`), `sequence` (`N`), `itemType` (`ENTRY`|`ACCOUNT`), `entryId` (`S`), `kind` (`S`), `amount` (`N`), `balanceAfter` (`N`), `description` (`S`, ausente quando nulo), `createdAt` (`S`, ISO-8601 UTC), `customerId` (`S`, só no item da conta).
- `insertEntry` e `insertAccount`: `PutItem` com `conditionExpression = "attribute_not_exists(accountId)"`. A `ConditionalCheckFailedException` vira `false`.
- `findPage`: `Query` com `accountId = :id AND sequence > :zero` (com cursor, `sequence BETWEEN :one AND :before-1`; com cursor 1, devolve a página vazia sem consultar, porque o DynamoDB recusa `BETWEEN` com limite inferior maior que o superior), `scanIndexForward(false)`, `limit(limit)`.
- `accountExists`: `GetItem` de `sequence = 0`.

- [ ] **Step 1:** `ArchitectureTest` com as regras de `libs/testing-architecture`, no padrão do `notification`.
- [ ] **Step 2:** `DynamoDbStatementRepositoryIntegrationTest`:
  - `inserting the same key twice returns false and keeps one item`;
  - `findEntry returns what was inserted`, incluindo `balanceAfterCents = -5_000` (Review Focus 2) e `description = null`, este com o atributo ausente no `GetItem` cru (Review Focus 3);
  - `pages walk newest to oldest`: 45 lançamentos, `limit` 20 → páginas de 20, 20 e 5, com `nextCursor` nulo só na última;
  - `account item is not listed`;
  - `entries inserted out of order come back ordered`.

  Rodar: FAIL.
- [ ] **Step 3:** implementar o repositório, as configurações e o `application.yml`. Rodar o mutante (sem `conditionExpression`) e ver vermelho o primeiro teste.
- [ ] **Step 4:** `./gradlew :services:statement:adapters:build :services:statement:bootstrap:build` → verde.
- [ ] **Step 5:** commit `feat(statement): repositório DynamoDB com PutItem condicional`.

### Task 5: Consumo SQS e métrica da DLQ (Marina, `dev-kotlin`; paralela à Task 6)

**Files:**
- Create: `adapters/src/main/kotlin/.../adapters/messaging/EventEnvelopeReader.kt`, `StatementEventListener.kt`, `QueueProperties.kt`
- Create: `bootstrap/src/main/kotlin/.../bootstrap/DlqMetricsConfiguration.kt`
- Test: `adapters/src/test/.../messaging/EventEnvelopeReaderTest.kt`, `bootstrap/src/integrationTest/.../messaging/StatementConsumptionIntegrationTest.kt`

**Interfaces:**
- Consumes: `RecordStatementEntryUseCase`, `RegisterAccountUseCase` (Task 3); `StatementIntegrationTest`, `application.yml` (Task 4).
- Produces: listener em `statement.queues.accounts` com `acknowledgementMode = ON_SUCCESS`; gauge `statement.dlq.messages`.

O `EventEnvelopeReader.read(body: String): ReceivedEvent` tem o mesmo formato de envelope do `notification` (`eventId`, `eventType`, `eventVersion`, `payload`). É uma classe própria do serviço, sem import do outro.
- `ReceivedEvent` = `LedgerEntryRecorded(entry: StatementEntry)` | `AccountOpened(accountId, customerId)` | `Ignored(eventType, eventVersion)`.
- A versão 1 é a única aceita para os dois eventos; outra versão vira `Ignored`.
- O correlation ID vai do atributo SQS para o MDC, como no `notification`.
- O WARN de falha sai só com a mensagem, sem stack trace.
- `ConflictingStatementEntryException` sai em log ERROR e é relançada.
- A métrica da DLQ lê o valor guardado, que é atualizado por `@Scheduled` em `statement.dlq-metrics.refresh-interval`.

- [ ] **Step 1:** `EventEnvelopeReaderTest`:
  - `LedgerEntryRecorded` com payload real do `accounts` (`entryId`, `accountId`, `sequence`, `kind`, `amount`, `balanceAfter`, `description`, `idempotencyKey`, `createdAt`) vira o `StatementEntry` esperado;
  - `AccountTypeChanged` vira `Ignored`;
  - versão 2 vira `Ignored`;
  - JSON sem `payload.sequence` lança exceção.
- [ ] **Step 2:** `StatementConsumptionIntegrationTest` (publica no tópico `accounts-events.fifo` do LocalStack):
  - `the same message delivered 20 times records one entry`;
  - `entries delivered out of order are listed in sequence order`;
  - `a malformed message ends in the DLQ and other accounts keep flowing` (com `maxReceiveCount` baixo no teste);
  - `AccountTypeChanged is acknowledged without effect`;
  - `a conflicting entry ends in the DLQ`.

  Espera por condição, sem `sleep`.
- [ ] **Step 3:** implementar. Rodar o mutante da deduplicação (o use case grava sem condição) e ver vermelho o teste das 20 entregas.
- [ ] **Step 4:** `./gradlew :services:statement:adapters:build :services:statement:bootstrap:build` → verde.
- [ ] **Step 5:** commit `feat(statement): consumo dos eventos do accounts com DLQ`.

### Task 6: API do extrato (Marina, `dev-kotlin`; paralela à Task 5)

**Files:**
- Create em `adapters/src/main/kotlin/.../adapters/web/`: `StatementController.kt`, `StatementCursorCodec.kt`, `ProblemDetailsExceptionHandler.kt`, `CorrelationIdFilter.kt`, `OpenApiConfiguration.kt`, `dto/StatementDTO.kt`, `dto/StatementEntryDTO.kt`
- Test: `adapters/src/test/.../web/StatementCursorCodecTest.kt`, `bootstrap/src/integrationTest/.../web/StatementApiIntegrationTest.kt`, `bootstrap/src/integrationTest/.../web/OpenApiIntegrationTest.kt`

**Interfaces:**
- Consumes: `GetStatementUseCase`, `AccountNotFoundException` (Task 3); `StatementIntegrationTest` (Task 4).
- Produces:
  - `GET /accounts/{accountId}/statement?cursor&limit`;
  - `StatementDTO(accountId: UUID, entries: List<StatementEntryDTO>, nextCursor: String?)`;
  - `StatementEntryDTO(entryId, sequence, kind, amount: Long, balanceAfter: Long, description: String?, createdAt: Instant)`;
  - `StatementCursorCodec.encode(StatementCursor): String` e `decode(String): StatementCursor`. O `decode` lança `InvalidCursorException`, que vira `400`.

Erros no formato dos outros serviços:
- `/problems/validation-error` (`400`) para UUID inválido, `limit` fora de 1..100 ou não numérico e cursor inválido;
- `/problems/account-not-found` (`404`).

O `ProblemDetailsExceptionHandler` e o `CorrelationIdFilter` são cópias adaptadas das do `notification`.

- [ ] **Step 1:** `StatementCursorCodecTest`: ida e volta de `StatementCursor(42)`; `"abc"`, base64 de `"v1:0"` e base64 de `"v1:x"` lançam `InvalidCursorException`.
- [ ] **Step 2:** `StatementApiIntegrationTest`, com os itens gravados direto pelo repositório:
  - `200` com 3 lançamentos em ordem decrescente e os campos da spec;
  - paginação por `nextCursor` até `null`;
  - conta conhecida sem lançamentos → `entries` vazio;
  - conta desconhecida → `404 account-not-found`;
  - `limit=0`, `limit=101`, `limit=abc`, cursor `%%%` e `accountId=nope` → `400 validation-error` (Review Focus 5);
  - o header `X-Correlation-Id` é devolvido.
- [ ] **Step 3:** implementar. `OpenApiIntegrationTest`: `/v3/api-docs` documenta `200`, `400` e `404` com `ProblemDetail`.
- [ ] **Step 4:** `./gradlew :services:statement:adapters:build :services:statement:bootstrap:build` → verde.
- [ ] **Step 5:** commit `feat(statement): API do extrato com paginação por cursor`.

### Task 7: e2e, smoke e Bruno (Marina, `dev-kotlin`)

**Files:**
- Modify: `tests/e2e/src/integrationTest/kotlin/.../e2e/EndToEndEnvironment.kt`, `ServiceApiClients.kt` (subir o `statement` e o cliente dele)
- Create: `tests/e2e/src/integrationTest/kotlin/.../e2e/StatementEndToEndIntegrationTest.kt`
- Modify: `infra/local/smoke.sh`
- Create: `api/bruno/statement/` (coleção no padrão de `api/bruno/notification/`)

**Interfaces:**
- Consumes: o serviço completo (Tasks 4–6) e a infra (Task 2).

- [ ] **Step 1:** `StatementEndToEndIntegrationTest`:
  - abrir duas contas, creditar 10.000 na origem, debitar 1.500 e transferir 2.000 para o destino;
  - o extrato da origem tem as `sequence` 3, 2 e 1 (o débito da transferência, o débito e o crédito), com `balanceAfter` de 6.500, 8.500 e 10.000;
  - o extrato do destino tem a `sequence` 1 com 2.000.

  Espera por condição.
- [ ] **Step 2:** o `smoke.sh` confere, depois do crédito que ele já faz, que `GET /accounts/{id}/statement` pelo nginx devolve o lançamento.
- [ ] **Step 3:** a coleção Bruno tem extrato com sucesso, paginação, `404` e `400`. Rodar `bru run` nas quatro coleções → verde.
- [ ] **Step 4:** `./gradlew :tests:e2e:integrationTest` → verde; `infra/local/smoke.sh` → OK.
- [ ] **Step 5:** commit `test(statement): e2e, smoke e coleção Bruno do extrato`.

### Task 8: Documentação (Clara, `documentador`)

**Files:**
- Modify: `CONTEXT.md` (em "Consulta e avisos", o termo **Lançamento do extrato**: "Lançamento como aparece no extrato, cópia de leitura do lançamento do ledger.", com `_Código_: StatementEntry`)
- Modify: `docs/arquitetura.md` (a linha do `statement` na tabela de serviços: DynamoDB nesta etapa, Redis na etapa 5)
- Modify: `docs/rodando-localmente.md` (`bootRun` do `statement`, porta 8084, variáveis `STATEMENT_*`, DynamoDB no LocalStack, comando para devolver mensagens da DLQ à fila)

- [ ] **Step 1:** editar os três arquivos, conferindo nomes e valores com as Tasks 2 e 4.
- [ ] **Step 2:** commit `docs: statement no glossário, na arquitetura e no guia local`.

## Ordem e paralelismo
| Onda | Tasks |
|---|---|
| 1 | 1 |
| 2 | 2 ‖ 3 |
| 3 | 4 |
| 4 | 5 ‖ 6 ‖ 8 |
| 5 | 7 |
| 6 | Revisão da branch inteira pela Beatriz |

Arquivos compartilhados: só a Task 1 edita `settings.gradle.kts`, `gradle/libs.versions.toml` e os `build.gradle.kts` do `statement`; só a Task 4 edita o `application.yml`. Se a Task 5 ou a Task 6 precisarem de uma chave nova, elas pedem à coordenadora em vez de editar.
