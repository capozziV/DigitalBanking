# Etapa 3 — Mensageria e `notification` Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Serviço `notification` que consome os eventos do `accounts` e do `transfers` em filas SQS FIFO com DLQ, grava notificações dentro do aplicativo de forma idempotente e as expõe por API; e os débitos do relay e da etapa 2 fechados.

**Architecture:** Clean Architecture por serviço. O adapter de mensageria (`@SqsListener`, Spring Cloud AWS) traduz o envelope em `ReceivedEvent` e chama `ConsumeEventUseCase`, que numa transação marca o `eventId` em `processed_events`, resolve conta → cliente pela cópia local (`known_accounts`) e grava as notificações. A mensagem só é confirmada depois do commit; falha repetida leva à DLQ pela política da fila.

**Tech Stack:** Kotlin 2.3.21, Spring Boot 4.1.1 (MVC, virtual threads, `JdbcClient`, Flyway), Spring Cloud AWS 4.2.x (`spring-cloud-aws-starter-sqs`), AWS SDK v2 2.57.1, PostgreSQL 17, LocalStack, Testcontainers, ArchUnit, JUnit 6, AssertJ, ktlint, GitHub Actions.

**Spec:** [`docs/superpowers/specs/2026-10-28-etapa-3-mensageria-design.md`](../specs/2026-10-28-etapa-3-mensageria-design.md) — leia antes de qualquer task.

## Global Constraints
- Código, testes, comentários e logs em inglês com os nomes do `CONTEXT.md` (`Notification`, `NotificationKind`, `Account`, `AccountType`, `Transfer`, `Money`, `OverdraftLimit`). Textos das notificações em português (conteúdo do produto). Commits em português, autor `capozziV <vitorcapozzi4@gmail.com>`, sem trailers.
- Valores em centavos, `Long`; formatação em reais por aritmética inteira, nunca `Double`.
- `domain` e `application` sem Spring; nenhum tipo do `accounts` ou do `transfers` no `notification` (lê só o JSON do envelope).
- Sufixo de papel em classe técnica; DTOs com sufixo `DTO` em `adapters/web/dto/`; testes terminam em `Test` ou `IntegrationTest`.
- Pacote raiz `io.github.capozziv.digitalbanking.notification`; porta 8083; IDs UUID v7 gerados na aplicação.
- Filas: `notification-accounts.fifo` e `notification-transfers.fifo`; DLQs `notification-accounts-dlq.fifo` e `notification-transfers-dlq.fifo`; `maxReceiveCount` 5 e visibilidade 30 s, configuráveis (`NOTIFICATION_MAX_RECEIVE_COUNT`, `NOTIFICATION_VISIBILITY_TIMEOUT_SECONDS` no script; `notification.queues.*` na aplicação).
- Mensagem confirmada só depois do commit. Evento de tipo desconhecido: log `INFO` e confirmação. Envelope malformado e conta desconhecida: exceção (volta para a fila).
- Página da API: 20, mais recentes primeiro (`created_at desc, id desc`).
- Testes que precisam de Docker no source set `integrationTest`; `test` roda sem Docker.
- Versões novas em `gradle/libs.versions.toml`, conferidas em fonte primária (Maven Central) na task que as introduz.
- TDD em toda task de código. Fim de cada task: `./gradlew build` verde com `LOCALSTACK_AUTH_TOKEN` (saída real no relato, até ~10 linhas); agentes em worktree não regeneram `graphify-out/`.
- Toda tabela nova com `grant` próprio para `notification_app`.

## Review Focus
1. **Mesma notificação por dois caminhos:** a mensagem é entregue de novo depois de o `processed_events` ter sido gravado mas antes da confirmação (queda entre commit e ack) → nenhuma notificação nova. Teste na Task A5 (entrega repetida após processamento).
2. **Transferência para a própria titularidade:** origem e destino da mesma pessoa (duas contas do mesmo cliente) → o cliente recebe `TRANSFER_SENT` e `TRANSFER_RECEIVED`, sem violar o `UNIQUE (source_event_id, customer_id, kind)`. Teste na Task A3.
3. **`upTo` de outro cliente ou inexistente:** `POST /notifications/read` com `upTo` que não pertence ao `customerId` → `404 notification-not-found`, nada marcado. Teste na Task A6.
4. **Cursor adulterado ou `customerId` inválido:** cursor que não decodifica ou `customerId` que não é UUID → `400 validation-error`. Teste na Task A6.
5. **`AccountTypeChanged` versão 1 ainda na fila:** evento sem `overdraftLimit` (v1) → notificação sem o limite ("Sua conta agora é Gold."), sem ir para a DLQ. Teste na Task A3.

---

## Frentes e ordem
- **Frente A (`notification`):** A1 → A2 → A3 → A4 → (A5 ∥ A6) → A8 → A9. A7 e A10 em paralelo desde o início.
- **Frente B (débitos):** B1 → B2; B3 → B4; B5 antes da A9 (as duas mexem em `tests/e2e`).
- **Arquivos compartilhados:** `settings.gradle.kts` e `gradle/libs.versions.toml` só na A1; `.github/workflows/ci.yml`, `infra/local/**` e `api/bruno/**` só na A8; `tests/e2e/**` na B5 e depois na A9.
- Worktrees nascem da branch `etapa-3-mensageria`.

# Frente A — `notification`

### Task A1: Esqueleto do `notification` e prova do `@SqsListener` (Marina, `dev-kotlin`)

**Files:**
- Modify: `settings.gradle.kts` (4 módulos `:services:notification:*`), `gradle/libs.versions.toml` (`spring-cloud-aws` e `spring-cloud-aws-starter-sqs`)
- Create: `services/notification/{domain,application,adapters,bootstrap}/build.gradle.kts`, `bootstrap/src/main/kotlin/…/notification/bootstrap/NotificationApplication.kt`, `bootstrap/src/main/resources/application.yml` (porta 8083), teste ArchUnit do serviço com `ServiceArchitectureRules`
- Test: `bootstrap/src/integrationTest/kotlin/…/bootstrap/SqsListenerFifoIntegrationTest.kt`

**Interfaces:**
- Produces: módulos compiláveis no padrão do `transfers`; dependência `spring-cloud-aws-starter-sqs` no `adapters`/`bootstrap` com o BOM `spring-cloud-aws-dependencies`.

- [ ] **Step 1:** Conferir no Maven Central a versão 4.2.x mais recente e a nota de compatibilidade do Spring Cloud AWS com Spring Boot 4.1.
- [ ] **Step 2:** Teste `SqsListenerFifoIntegrationTest`: com LocalStack (`LocalStackTestSupport`), cria uma fila FIFO e um listener de teste com `acknowledgementMode = ON_SUCCESS`; envia 3 mensagens no mesmo `MessageGroupId` → recebidas na ordem; listener que lança na 1ª entrega → a mensagem volta e é recebida de novo depois da visibilidade (teste com visibilidade de 1 s).
- [ ] **Step 3:** Rodar `./gradlew :services:notification:bootstrap:integrationTest` → PASS. **Se não houver combinação que funcione com Boot 4.1.1, parar e relatar à coordenadora** (a decisão volta ao usuário).
- [ ] **Step 4:** `./gradlew build` verde; commit `feat(notification): esqueleto e prova do SqsListener em fila FIFO`.

### Task A2: Domínio das notificações (Marina, `dev-kotlin`)

**Files:**
- Create: `services/notification/domain/src/main/kotlin/…/notification/domain/{Notification.kt,NotificationKind.kt,NotificationText.kt,MoneyFormat.kt}`
- Test: `domain/src/test/kotlin/…/domain/{NotificationTextTest.kt,MoneyFormatTest.kt,NotificationTest.kt}`

**Interfaces:**
- Produces:
  - `enum class NotificationKind { WELCOME, ACCOUNT_TYPE_CHANGED, TRANSFER_SENT, TRANSFER_RECEIVED, TRANSFER_FAILED, TRANSFER_COMPENSATED }`
  - `data class Notification(val id: UUID, val customerId: UUID, val kind: NotificationKind, val title: String, val body: String, val data: Map<String, String>, val sourceEventId: UUID, val createdAt: Instant, val readAt: Instant?)`
  - `object NotificationText` com `welcome(accountType: String, accountId: UUID)`, `accountTypeChanged(newType: String, overdraftLimitCents: Long?)`, `transferSent(amountCents: Long, destinationAccountId: UUID)`, `transferReceived(amountCents: Long, sourceAccountId: UUID)`, `transferFailed(amountCents: Long, destinationAccountId: UUID, reason: String)`, `transferCompensated(amountCents: Long, destinationAccountId: UUID)`, cada uma devolvendo `NotificationContent(title: String, body: String)`.
  - `object MoneyFormat { fun brl(cents: Long): String }`

- [ ] **Step 1:** Testes de `MoneyFormat.brl`: `0` → `"R$ 0,00"`, `5` → `"R$ 0,05"`, `123456` → `"R$ 1.234,56"`, `100000000` → `"R$ 1.000.000,00"`.
- [ ] **Step 2:** Testes de `NotificationText` com o texto exato abaixo (tipo exibido com a 1ª letra maiúscula: `GOLD` → `Gold`):

| Kind | Título | Corpo |
|---|---|---|
| `WELCOME` | `Bem-vindo ao banco!` | `Sua conta {Tipo} está aberta. Você pode receber transferências pelo número {conta} e acompanhar tudo por aqui no sininho.` |
| `ACCOUNT_TYPE_CHANGED` | `Sua conta mudou de tipo` | `Sua conta agora é {Tipo}. Seu limite de cheque especial passou a ser {limite}.` — sem limite (v1): `Sua conta agora é {Tipo}.` |
| `TRANSFER_SENT` | `Transferência enviada` | `Você enviou {valor} para a conta {destino}.` |
| `TRANSFER_RECEIVED` | `Você recebeu uma transferência` | `Você recebeu {valor} da conta {origem}.` |
| `TRANSFER_FAILED` | `Transferência recusada` | `Sua transferência de {valor} para a conta {destino} foi recusada: {motivo}. Nenhum valor saiu da sua conta.` |
| `TRANSFER_COMPENSATED` | `Transferência devolvida` | `Sua transferência de {valor} para a conta {destino} não pôde ser concluída e o valor voltou para a sua conta.` |

  Motivo: `insufficient-available-balance` → `saldo disponível insuficiente`; `debit-not-confirmed` → `não foi possível confirmar o débito`; qualquer outro → `não foi possível processar a transferência`.
- [ ] **Step 3:** Implementar; `Notification.markRead(at: Instant): Notification` idempotente (já lida mantém o `readAt` original) com teste.
- [ ] **Step 4:** `./gradlew :services:notification:domain:build` verde; commit `feat(notification): domínio e textos das notificações`.

### Task A3: Casos de uso e portas (Marina, `dev-kotlin`)

**Files:**
- Create: `application/src/main/kotlin/…/application/{ReceivedEvent.kt,UnknownAccountException.kt}`, `port/{NotificationRepository.kt,KnownAccountRepository.kt,ProcessedEventRepository.kt,TransactionRunner.kt,IdGenerator.kt}`, `usecase/{ConsumeEventUseCase.kt,ListNotificationsUseCase.kt,MarkNotificationsReadUseCase.kt}`
- Test: fakes em memória em `application/src/test/kotlin/…/application/fake/` e testes de cada caso de uso

**Interfaces:**
- Consumes: Task A2.
- Produces:
  - `sealed interface ReceivedEvent { val eventId: UUID }` com `AccountOpened(eventId, accountId, customerId, accountType: String)`, `AccountTypeChanged(eventId, accountId, newType: String, overdraftLimitCents: Long?)`, `TransferCompleted(eventId, transferId, sourceAccountId, destinationAccountId, amountCents)`, `TransferFailed(…, reason: String)`, `TransferCompensated(…)`, `Unsupported(eventId, eventType: String)`.
  - `enum class ConsumeOutcome { PROCESSED, DUPLICATE, IGNORED }`; `ConsumeEventUseCase.execute(event: ReceivedEvent): ConsumeOutcome` — tudo numa transação via `TransactionRunner`; `UnknownAccountException(accountId)` quando a conta não está em `known_accounts`.
  - `ProcessedEventRepository.markProcessed(eventId: UUID, eventType: String, at: Instant): Boolean` (false = já existia).
  - `KnownAccountRepository.save(accountId: UUID, customerId: UUID)` (idempotente), `customerOf(accountId: UUID): UUID?`.
  - `NotificationRepository.saveAll(notifications: List<Notification>)`, `page(customerId: UUID, cursor: NotificationCursor?, size: Int): List<Notification>`, `countUnread(customerId: UUID): Long`, `find(id: UUID): Notification?`, `markReadUpTo(customerId: UUID, upTo: Notification, at: Instant): Int`.
  - `data class NotificationCursor(val createdAt: Instant, val id: UUID)`; `ListNotificationsUseCase.execute(customerId: UUID, cursor: NotificationCursor?): NotificationPage(items, unreadCount, nextCursor)` com tamanho 20; `MarkNotificationsReadUseCase.execute(customerId: UUID, upTo: UUID): Int` lança `NotificationNotFoundException` se `upTo` não existe ou é de outro cliente.

- [ ] **Step 1:** Testes de `ConsumeEventUseCase`: `AccountOpened` → conta conhecida + 1 `WELCOME`; mesmo evento de novo → `DUPLICATE`, nada novo; `TransferCompleted` → `TRANSFER_SENT` para o dono da origem e `TRANSFER_RECEIVED` para o do destino; origem e destino do mesmo cliente → 2 notificações (Review Focus 2); `TransferFailed`/`TransferCompensated` → 1 para o dono da origem; `AccountTypeChanged` sem limite → texto v1 (Review Focus 5); `Unsupported` → `IGNORED` e `eventId` marcado; conta desconhecida → `UnknownAccountException` e nada gravado (nem `processed_events`).
- [ ] **Step 2:** Testes de `ListNotificationsUseCase` (25 notificações → 20 + `nextCursor`; 2ª página com 5 e `nextCursor` nulo; `unreadCount`) e de `MarkNotificationsReadUseCase` (marca até `upTo` inclusive e não as posteriores; `upTo` de outro cliente → exceção).
- [ ] **Step 3:** Implementar; `./gradlew :services:notification:application:build` verde; commit `feat(notification): casos de uso de consumo, lista e leitura`.

### Task A4: Persistência (Marina, `dev-kotlin`)

**Files:**
- Create: `bootstrap/src/main/resources/db/migration/V1__notification_schema.sql`, `V2__app_grants.sql`; `adapters/src/main/kotlin/…/adapters/persistence/{JdbcNotificationRepository.kt,JdbcKnownAccountRepository.kt,JdbcProcessedEventRepository.kt,SpringTransactionRunner.kt,UuidV7IdGenerator.kt}`
- Test: `bootstrap/src/integrationTest/kotlin/…/bootstrap/persistence/*IntegrationTest.kt`

**Interfaces:**
- Consumes: portas da A3.
- Produces: esquema da seção 5 da spec (`notifications` com `UNIQUE (source_event_id, customer_id, kind)` e índice `(customer_id, created_at desc, id desc)`; `known_accounts`; `processed_events`); `data` em `jsonb`.

- [ ] **Step 1:** Testes de integração (Testcontainers PostgreSQL): `markProcessed` 2× → `true`, depois `false`; `known_accounts.save` 2× sem erro; página e cursor na ordem `created_at desc, id desc` com empate de `created_at`; `markReadUpTo` não toca notificações de outro cliente nem posteriores; transação que falha depois de `markProcessed` desfaz tudo.
- [ ] **Step 2:** Implementar com `JdbcClient`; `grant` por tabela para `notification_app`.
- [ ] **Step 3:** `./gradlew :services:notification:bootstrap:integrationTest` verde; commit `feat(notification): persistência com JdbcClient e Flyway`.

### Task A5: Consumo das filas (Marina, `dev-kotlin`) — paralela à A6

**Files:**
- Create: `adapters/src/main/kotlin/…/adapters/messaging/{EventEnvelopeReader.kt,NotificationEventListener.kt,QueueProperties.kt}`, `bootstrap/…/bootstrap/DlqMetricsConfiguration.kt`
- Test: `adapters/src/test/…/messaging/EventEnvelopeReaderTest.kt`; `bootstrap/src/integrationTest/…/messaging/NotificationConsumptionIntegrationTest.kt`

**Interfaces:**
- Consumes: `ConsumeEventUseCase` (A3), persistência (A4).
- Produces: `EventEnvelopeReader.read(body: String): ReceivedEvent` (lança `MalformedEventException`); listener com `@SqsListener("\${notification.queues.accounts}")` e `("\${notification.queues.transfers}")`, correlation ID do atributo `correlationId` no MDC durante a chamada; gauge `notification.dlq.messages` com tag `queue`.

- [ ] **Step 1:** Testes do `EventEnvelopeReader`: os 5 tipos com os payloads atuais (ver `services/*/adapters/…/persistence/OutboxEventAppender.kt`), `AccountTypeChanged` v1 e v2, tipo desconhecido → `Unsupported`, JSON inválido ou campo obrigatório ausente → `MalformedEventException`.
- [ ] **Step 2:** Teste de integração (LocalStack, filas e DLQs criadas no teste com `maxReceiveCount` 5 e visibilidade 1 s): `AccountOpened` + `TransferCompleted` → 3 notificações; a mesma `TransferCompleted` enviada 20× (com `MessageDeduplicationId` diferentes) → continuam 3; mensagem malformada → na DLQ depois de 5 recebimentos, e mensagem de outro grupo enviada depois é processada; `TransferCompleted` de conta desconhecida volta, e é processada quando o `AccountOpened` chega; gauge da DLQ mostra 1.
- [ ] **Step 3:** Implementar; `./gradlew build` verde; commit `feat(notification): consumo idempotente das filas com DLQ`.

### Task A6: API das notificações (Marina, `dev-kotlin`) — paralela à A5

**Files:**
- Create: `adapters/src/main/kotlin/…/adapters/web/{NotificationController.kt,ProblemDetailsExceptionHandler.kt,CorrelationIdFilter.kt,OpenApiConfiguration.kt,NotificationCursorCodec.kt}`, `web/dto/{NotificationDTO.kt,NotificationPageDTO.kt,MarkNotificationsReadDTO.kt}`
- Test: `bootstrap/src/integrationTest/…/web/NotificationApiIntegrationTest.kt`, `OpenApiIntegrationTest.kt`

**Interfaces:**
- Consumes: `ListNotificationsUseCase`, `MarkNotificationsReadUseCase` (A3).
- Produces: `GET /notifications?customerId=&cursor=` → `NotificationPageDTO(items: List<NotificationDTO>, unreadCount: Long, nextCursor: String?)`; `POST /notifications/read` com `MarkNotificationsReadDTO(customerId, upTo)` → `200 {"marked": n}`; cursor opaco em Base64 URL-safe de `createdAt|id`.

- [ ] **Step 1:** Testes: lista vazia → `items: []`, `unreadCount: 0`, `nextCursor: null`; paginação 20 + cursor; marcar como lidas e repetir a mesma chamada → idempotente; `upTo` de outro cliente ou inexistente → `404 notification-not-found` (Review Focus 3); `customerId` inválido ou cursor adulterado → `400 validation-error` (Review Focus 4); `X-Correlation-Id` devolvido; OpenAPI publicado.
- [ ] **Step 2:** Implementar no padrão do `TransferController`; `./gradlew build` verde; commit `feat(notification): API de lista e leitura das notificações`.

### Task A7: `AccountTypeChanged` versão 2 no `accounts` (Marina, `dev-kotlin`) — paralela desde o início

**Files:**
- Modify: `services/accounts/domain/…/domain/{AccountEvent.kt,Account.kt}`, `services/accounts/application/…/usecase/ChangeAccountTypeUseCase.kt`, `services/accounts/adapters/…/persistence/OutboxEventAppender.kt` e os testes deles

**Interfaces:**
- Produces: `AccountTypeChanged(accountId, previousType, newType, overdraftLimit: OverdraftLimit)`; payload ganha `overdraftLimit` (centavos); versão por tipo de evento (`AccountTypeChanged` = 2, os outros continuam 1).

- [ ] **Step 1:** Testes: troca de tipo grava no outbox `eventVersion` 2 com `overdraftLimit` igual ao limite do novo tipo na tabela de tipos; `AccountOpened` e `LedgerEntryRecorded` seguem com versão 1.
- [ ] **Step 2:** Implementar (o limite vem da mesma leitura que o caso de uso já faz, na mesma transação); `./gradlew :services:accounts:build` verde; commit `feat(accounts): AccountTypeChanged v2 com o limite de cheque especial`.

### Task A8: Infra local, Bruno, smoke e CI do `notification` (Rafael, `infra`)

**Files:**
- Create: `infra/local/localstack/init/ready.d/03-notification-queues.sh`, `api/bruno/notification/*.bru`
- Modify: `infra/local/postgres/init/01-create-service-databases.sh` (banco e usuários do `notification`), `infra/local/docker-compose.yml` (senhas do `notification`), `infra/local/.env.example`, `infra/local/nginx/nginx.conf` (`/notifications`, mesmas regras do `transfers`), `infra/local/smoke.sh`, `.github/workflows/ci.yml` (filtro e job `notification`, `ci-ok`, smoke com os 3 serviços)

- [ ] **Step 1:** Script 03: cria as 2 DLQs FIFO, as 2 filas FIFO com `RedrivePolicy` (`maxReceiveCount` de `NOTIFICATION_MAX_RECEIVE_COUNT`, padrão 5) e `VisibilityTimeout` (`NOTIFICATION_VISIBILITY_TIMEOUT_SECONDS`, padrão 30), e assina cada fila no tópico correspondente com `RawMessageDelivery=true`. Idempotente; roda depois dos scripts 01 e 02.
- [ ] **Step 2:** Smoke: depois da transferência concluída, `GET /notifications?customerId=` do remetente e do destinatário pelo nginx mostra `TRANSFER_SENT` e `TRANSFER_RECEIVED` (espera até 45 s).
- [ ] **Step 3:** CI: job `notification` no padrão do job `transfers`, incluído no `ci-ok`; smoke sobe os 3 jars.
- [ ] **Step 4:** Rodar compose do zero, `bootRun` dos 3 serviços e `infra/local/smoke.sh` → verde; Bruno `bru run api/bruno/notification` verde; commit `ci: notification no compose, nginx, smoke, Bruno e CI`.

### Task A9: Testes ponta a ponta (Marina, `dev-kotlin`) — depois de A5, A6, A7, A8 e B5

**Files:**
- Modify: `tests/e2e/build.gradle.kts`, `tests/e2e/src/integrationTest/kotlin/…/e2e/{EndToEndEnvironment.kt,ServiceApiClients.kt}`
- Create: `tests/e2e/src/integrationTest/kotlin/…/e2e/NotificationEndToEndIntegrationTest.kt`

- [ ] **Step 1:** Testes com os 3 serviços como processos: critérios de pronto 1 a 4 da spec; critério 3 reenviando a mesma `TransferCompleted` 20× direto na fila; critério 5 (mensagem malformada na DLQ, outra conta segue); critério 6 (`notification` parado durante 3 transferências, ao voltar 6 notificações, nenhuma duplicada).
- [ ] **Step 2:** `./gradlew :tests:e2e:integrationTest` verde; commit `test(e2e): notificações ponta a ponta`.

### Task A10: Documentação (Clara, `documentador`) — paralela desde o início

**Files:**
- Create: `docs/adr/0017-notification-com-banco-e-consumo-sqs.md`
- Modify: `docs/arquitetura.md` (tabela de serviços: `notification` com PostgreSQL; mensageria: filas e DLQs, Spring Cloud AWS, `processed_events`), `docs/adr/README.md`, `CONTEXT.md` (termo **Tipo de notificação**, `_Código_: NotificationKind`), `docs/rodando-localmente.md` (serviço novo, como ver a DLQ e devolver mensagens com `aws sqs start-message-move-task`), `docs/roadmap.md` (link da spec na etapa 3)

- [ ] **Step 1:** ADR curto com o trade-off (banco próprio × "sem banco"; Spring Cloud AWS × poller próprio; idempotência por `processed_events`). Commit `docs: ADR 0017 e documentação da etapa 3`.

# Frente B — débitos técnicos

### Task B1: Configuração do relay em `libs/outbox-relay` (Marina, `dev-kotlin`) — cartão #92

**Files:**
- Create: `libs/outbox-relay/src/main/kotlin/…/outbox/relay/spring/{OutboxRelayAutoConfiguration.kt ou OutboxRelayWiring.kt,OutboxRelayScheduler.kt}`
- Delete/Modify: `services/{accounts,transfers}/bootstrap/…/OutboxRelayConfiguration.kt` e `OutboxRelayScheduler.kt`

**Interfaces:**
- Produces: configuração parametrizada pelo prefixo das propriedades e pela chave da trava consultiva; os dois serviços passam a só declarar os valores.

- [ ] **Step 1:** Os testes de integração do relay nos dois serviços continuam passando sem mudança de comportamento; `./gradlew build` verde; commit `refactor(outbox-relay): configuração do relay na biblioteca`.

### Task B2: Backoff do relay e testes (Marina, `dev-kotlin`) — cartões #24 e #26, depois da B1

**Files:**
- Modify: `libs/outbox-relay/src/main/kotlin/…/relay/OutboxRelay.kt` e testes; `services/accounts/bootstrap/src/integrationTest/…/OutboxRelayIntegrationTest.kt`, `OutboxRelayUnreachableBrokerIntegrationTest.kt`

- [ ] **Step 1:** Teste: com o broker fora, o log `ERROR` sai uma vez por conta na transição para presa (não a cada ciclo) e as tentativas seguem com espera crescente (1 s × 2^(n−1), teto 30 s, configurável).
- [ ] **Step 2:** `get()` fora do `finally` (não mascarar a exceção original); KDoc do teste do broker inalcançável cita a fiação do agendador.
- [ ] **Step 3:** `./gradlew build` verde; commit `fix(outbox-relay): backoff com o broker fora e testes mais claros`.

### Task B3: Domínio e persistência do `transfers` (Marina, `dev-kotlin`) — cartões #95 e #93

**Files:**
- Modify: `services/transfers/domain/…/Transfer.kt` e testes; `application/…/port/TransferRepository.kt`, `adapters/…/persistence/{JdbcTransferRepository.kt,SpringTransactionRunner.kt}`, fakes e testes; migração nova com os `CHECK` de `step` e de `next_attempt_at`

- [ ] **Step 1:** Testes listados nos cartões #95 e #93 (PENDING em todos os passos não terminais, `terminalEvent() == null` em todos, caminho antes do prazo, `deadlineAfter <= 0` rejeitado; `CHECK`s exercitados; rollback do `SpringTransactionRunner`; teste de gravação terminal que falha pelo `status`).
- [ ] **Step 2:** `saveIfOwner(transfer, owner, now: Instant)`; `./gradlew :services:transfers:build` verde; commit `test(transfers): lacunas do domínio e da persistência`.

### Task B4: Aplicação e adapters do `transfers` (Marina, `dev-kotlin`) — cartão #94, depois da B3

- [ ] **Step 1:** Cada item do cartão #94 com teste ou correção; `./gradlew :services:transfers:build` verde; commit `test(transfers): lacunas da aplicação e dos adapters`.

### Task B5: Endurecer os testes ponta a ponta (Marina, `dev-kotlin`) — cartão #96, antes da A9

- [ ] **Step 1:** Caminho do jar descoberto pela tarefa `bootJar` (sem `build/libs/bootstrap.jar` fixo); sem `println`; correlation ID conferido nas transferências recusada e compensada; arrendamento do e2e com margem documentada. `./gradlew :tests:e2e:integrationTest` verde; commit `test(e2e): testes ponta a ponta mais robustos`.

---

**Fim da etapa:** revisão de cada task e da branch inteira pela Beatriz; `./gradlew build --rerun-tasks` verde com LocalStack; PR; coordenadora atualiza `docs/status.md` e o board (Stage 3 → Done; cartões sem pendência da Stage 4 → Ready). #23 e #56 mudam para a Stage 7.
