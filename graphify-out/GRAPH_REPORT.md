# Graph Report - DigitalBanking  (2026-10-29)

## Corpus Check
- 415 files · ~115,350 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 29 file(s) not represented in the graph (top: .bru 19, (none) 3, .properties 2)

## Summary
- 3668 nodes · 8879 edges · 223 communities (103 shown, 120 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 676 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `ad94a6cd`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- ChangeAccountTypeUseCaseTest
- SqsListenerFifoIntegrationTest.kt
- AccountApiIntegrationTest
- SpringTransactionRunner
- TransferApiIntegrationTest
- NotificationQueues
- ServiceArchitectureRulesTest
- .transfer
- Account
- RecordDebitUseCaseTest.kt
- OpenAccountUseCaseTest.kt
- ProblemDetailsExceptionHandler.kt
- OutboxRelayIntegrationTest.kt
- TransferSchemaIntegrationTest
- LedgerEntry
- Account
- OutboxRelayTest
- duration
- Decisões tomadas
- README.md
- AvailableBalance
- SnsEventPublisherIntegrationTest.kt
- TransferLeaseContentionIntegrationTest.kt
- coordenador.md
- 0004 — API Gateway HTTP API na borda
- 0005 — Frontend Angular e BFF em NestJS
- 0006 — Observabilidade só com correlation ID
- TransferSagaEndToEndIntegrationTest
- Rodando o projeto localmente (macOS)
- AccountController.kt
- 0008 — Extrato (`statement`) com CQRS no DynamoDB
- 0009 — Cache do extrato
- 0010 — Limite diário com Redis + Lua
- 0013 — Persistência com Spring JDBC (`JdbcClient`)
- 0012 — Saldo no lançamento e trava pessimista por conta
- OpenApiIntegrationTest
- Order
- dev-frontend.md
- dev-kotlin.md
- infra.md
- 0003 — Transactional Outbox e mensageria SNS/SQS
- TransferController.kt
- 0011 — Contratos de API
- Versões novas da etapa 1
- documentador.md
- revisor.md
- 01-create-service-databases.sh
- LocalStackInitScripts
- outbox_stuck_idx
- EventEnvelopeReaderTest
- notification/adapters/web/ProblemDetailsExceptionHandler.kt
- account
- Balance
- outbox-table.sql
- V4__outbox.sql
- LedgerEntryRecorded
- OutboxRelay
- JdbcTransferRepositoryIntegrationTest
- SagaPropertiesTest.kt
- NotificationConsumptionIntegrationTest
- ProcessedEventRepository
- smoke.sh
- 0016 — Saga de transferência orquestrada por HTTP síncrono
- Etapa 0 — Fundação: design
- HttpAccountsGateway.kt
- AccountsApplicationIntegrationTest.kt
- 01-accounts-topic.sh
- TransferEvent
- OutboxRelayScheduler
- OutboxBacklog
- HttpAccountsGatewayIntegrationTest
- Foco de revisão
- JdbcOutboxStoreIntegrationTest
- RunTransferSagaUseCase
- OrderResource
- Money
- LocalStackFifoIntegrationTest.kt
- LocalStackTestSupport
- NotificationEventListenerTest.kt
- SagaIntegrationSupport.kt
- AccountTypeAdminController.kt
- InMemoryTransferRepository
- retryWhileAccountBusy
- TransferEventsIntegrationTest.kt
- Etapa 3 — Mensageria e `notification`: design
- MarkNotificationsReadUseCaseTest.kt
- NotificationApiIntegrationTest
- RunTransferSagaUseCase
- OutboxRelayConfigurationSupport.kt
- RequestTransferDTO
- transfers/adapters/web/ProblemDetailsExceptionHandler.kt
- LegacyResource.kt
- PlaceOrderHandler
- LegacyController.kt
- EntryLookupApiIntegrationTest
- ApiClient
- ApplicationWithAws.kt
- ApplicationWithSpring.kt
- ApplicationWithSql.kt
- OrderRunner
- PlaceOrderHandlerResult
- DomainWithAws.kt
- DomainWithSpring.kt
- DomainWithSql.kt
- JdbcOrderRepository.kt
- OrderLookup
- OrderBody.kt
- OrderDTO.kt
- TransferSagaResumeIntegrationTest.kt
- RequestTransferUseCaseTest
- AccountsProxy.kt
- .repeat
- AccountsCall
- ResumeDueTransfersUseCaseTest
- TransfersDatabase
- NotificationIntegrationTest
- OutboxRelayConfigurationSupportTest.kt
- ServiceApiClients.kt
- ConcurrencyIntegrationTest
- UseCaseConfiguration
- Notification
- ResumeDueTransfersUseCase
- OutboxRelay
- AccountsResponseClassifierTest
- accounts/adapters/persistence/OutboxWriterConfiguration.kt
- EndToEndEnvironment
- transfers/adapters/persistence/OutboxWriterConfiguration.kt
- TransferMetricsIntegrationTest.kt
- SnsEventPublisherTest.kt
- OutboxRelayUnreachableBrokerIntegrationTest.kt
- JdbcTransferRepository
- AccountTypeAdminApiIntegrationTest
- TransferSagaScheduler
- KnownAccountRepository
- OutboxRelaySettings
- TransferMetricsConfiguration.kt
- Test
- transfers/adapters/persistence/OutboxEventAppender.kt
- ProcessLogsOnFailureExtension
- ServiceProcess
- InMemoryTransferRepositoryTest
- IdGenerator
- SchedulerLeaseOwners
- uuid
- EndToEndInputs
- .run
- .entry
- Test
- TransactionRunner
- Etapa 2 — `transfers` + saga: design
- NotificationTest
- clock
- JdbcOutboxWriter
- TransferStep
- TransfersIntegrationTest
- LocalStackInitScripts
- .resumeDueOver
- RetryBackoffTest
- AccountRequestDTOs.kt
- 02-transfers-topic.sh
- OpenApiIntegrationTest
- notification/adapters/web/OpenApiConfiguration.kt
- transfer
- OutboxMessage
- 03-notification-queues.sh
- V3__outbox.sql
- accounts/bootstrap/web/UnexpectedErrorApiIntegrationTest.kt
- DlqMetricsConfiguration.kt
- OrderController.kt
- NotificationCursor
- 0014 — Tipo de conta e cheque especial
- ConsumeEventUseCase
- Order
- 0007 — Conta e ledger no mesmo serviço (`accounts`)
- TestListeners
- transfers/adapters/web/CorrelationIdFilter.kt
- BlockingRepository
- TransactionRunner
- accounts/adapters/web/CorrelationIdFilter.kt
- CreditStepTest
- OutboxWriterClockIntegrationTest.kt
- V1__notification_schema.sql
- AccountsIntegrationTest
- 0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS
- accounts/adapters/persistence/OutboxEventAppender.kt
- Banco digital em microsserviços
- Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)
- ResolveCreditStepTest
- ResolveDebitStepTest
- Status do projeto
- .record
- 0002 — Clean Architecture por serviço

## God Nodes (most connected - your core abstractions)
1. `Transfer` - 110 edges
2. `Account` - 71 edges
3. `OverdraftLimit` - 62 edges
4. `AccountApiIntegrationTest` - 61 edges
5. `LedgerEntry` - 58 edges
6. `TransferStep` - 53 edges
7. `AccountType` - 49 edges
8. `Balance` - 49 edges
9. `Money` - 49 edges
10. `assertProblem()` - 47 edges

## Surprising Connections (you probably didn't know these)
- `Mensageria` --references--> `OutboxRelay`  [INFERRED]
  docs/arquitetura.md → libs/outbox-relay/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/relay/OutboxRelay.kt
- `Task 14: Agendador, relay e eventos no `transfers` (Marina, `dev-kotlin`)` --references--> `OutboxRelayScheduler`  [INFERRED]
  docs/superpowers/plans/2026-10-27-etapa-2-transfers.md → libs/outbox-relay/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/relay/spring/OutboxRelayScheduler.kt
- `Task 7: `libs/outbox` — envelope e writer (Marie, `dev-kotlin`; em paralelo às Tasks 2–6)` --references--> `OutboxEventEnvelope`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/OutboxEventEnvelope.kt
- `6. Eventos consumidos e mudança no `accounts`` --references--> `OutboxEventEnvelope`  [INFERRED]
  docs/superpowers/specs/2026-10-28-etapa-3-mensageria-design.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/OutboxEventEnvelope.kt
- `Task 4: Consulta de lançamento pela chave no `accounts` (Marina, `dev-kotlin`)` --references--> `AccountNotFoundException`  [INFERRED]
  docs/superpowers/plans/2026-10-27-etapa-2-transfers.md → services/accounts/application/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/application/ApplicationExceptions.kt

## Import Cycles
- None detected.

## Communities (223 total, 120 thin omitted)

### Community 0 - "ChangeAccountTypeUseCaseTest"
Cohesion: 0.11
Nodes (7): ChangeAccountTypeUseCase, CallLog, EventOutbox, RecordingEventOutbox, ChangeAccountTypeUseCaseTest, AccountEvent, AccountTypeChanged

### Community 5 - "NotificationQueues"
Cohesion: 0.12
Nodes (5): DeadLetter, NotificationQueues, PublishedMessage, ReceivedEvent, TransferEventsQueue

### Community 6 - "ServiceArchitectureRulesTest"
Cohesion: 0.06
Nodes (5): ServiceArchitectureRules, ServiceArchitectureRulesTest, ArchitectureTest, ArchitectureTest, ArchitectureTest

### Community 7 - ".transfer"
Cohesion: 0.20
Nodes (3): TerminalStepsTest, TransferFixtures, TransferTest

### Community 8 - "Account"
Cohesion: 0.07
Nodes (17): Regras inegociáveis, Clientes e contas, Restrições (de `CLAUDE.md` e ADRs), JdbcAccountRepository, AccountRepository, InsertAccountResult, DUPLICATE_CUSTOMER, DUPLICATE_OPENING_KEY (+9 more)

### Community 9 - "RecordDebitUseCaseTest.kt"
Cohesion: 0.12
Nodes (18): Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 13: Documentação do bloco 1 e das decisões da etapa (Clara, `documentador`; em paralelo às Tasks 8–12), Task 2: Valores monetários e tipos (Marina, `dev-kotlin`), Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`), Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`) (+10 more)

### Community 10 - "OpenAccountUseCaseTest.kt"
Cohesion: 0.16
Nodes (6): OpenAccountCommand, OpenAccountResult, DirectTransactionRunner, SequentialIdGenerator, OpenAccountUseCaseTest, AccountOpened

### Community 11 - "ProblemDetailsExceptionHandler.kt"
Cohesion: 0.11
Nodes (8): IdempotencyKeyHeader, ProblemDetailsExceptionHandler, AccountTypeNotFoundException, EntryNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException, BalanceOverflowException

### Community 12 - "OutboxRelayIntegrationTest.kt"
Cohesion: 0.09
Nodes (3): OutboxRelayIntegrationTest, ReceivedEvent, RecordingPublisherConfiguration

### Community 14 - "LedgerEntry"
Cohesion: 0.07
Nodes (14): Ledger, JdbcLedgerEntryRepository, FindEntryByIdempotencyKeyUseCase, InMemoryLedgerEntryRepository, FindEntryByIdempotencyKeyUseCaseTest, DebitResult, EntryRecorded, Recorded (+6 more)

### Community 16 - "OutboxRelayTest"
Cohesion: 0.16
Nodes (3): FakeEventPublisher, MutableClock, OutboxRelayTest

### Community 17 - "duration"
Cohesion: 0.07
Nodes (20): Global Constraints, 1. Módulos e camadas, 2. Modelo de domínio, JdbcAccountTypeRepository, AccountTypeRepository, ChangeOverdraftLimitUseCase, ListAccountTypesUseCase, AccountTypeRepository (+12 more)

### Community 18 - "Decisões tomadas"
Cohesion: 0.13
Nodes (15): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Em aberto, Fluxo de entrada (+7 more)

### Community 20 - "AvailableBalance"
Cohesion: 0.16
Nodes (5): 4. Fluxos, AccountView, GetAccountUseCase, GetAccountUseCaseTest, AvailableBalance

### Community 22 - "TransferLeaseContentionIntegrationTest.kt"
Cohesion: 0.13
Nodes (3): TransferLeaseContentionIntegrationTest, ApiClient, ApiResponse

### Community 23 - "coordenador.md"
Cohesion: 0.29
Nodes (6): Ao começar uma sessão, Board, Para cada etapa do roadmap, Para cada tarefa, Paralelismo, Quando faltar algo

### Community 24 - "0004 — API Gateway HTTP API na borda"
Cohesion: 0.40
Nodes (5): 0004 — API Gateway HTTP API na borda, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 25 - "0005 — Frontend Angular e BFF em NestJS"
Cohesion: 0.40
Nodes (5): 0005 — Frontend Angular e BFF em NestJS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 26 - "0006 — Observabilidade só com correlation ID"
Cohesion: 0.40
Nodes (5): 0006 — Observabilidade só com correlation ID, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 29 - "Rodando o projeto localmente (macOS)"
Cohesion: 0.13
Nodes (15): 1. Configurar o ambiente, 2. Subir a infraestrutura (PostgreSQL, Redis, nginx, LocalStack), 3. Build e testes, 4. Subir os serviços `accounts`, `transfers` e `notification`, 5. Conferir a API pelo nginx, 6. Ver os eventos na fila de inspeção, 7. Coleção Bruno, 8. Derrubar tudo (+7 more)

### Community 30 - "AccountController.kt"
Cohesion: 0.21
Nodes (3): AccountController, AccountDTO, LedgerEntryDTO

### Community 31 - "0008 — Extrato (`statement`) com CQRS no DynamoDB"
Cohesion: 0.50
Nodes (4): 0008 — Extrato (`statement`) com CQRS no DynamoDB, Consequências, Contexto, Decisão

### Community 32 - "0009 — Cache do extrato"
Cohesion: 0.50
Nodes (4): 0009 — Cache do extrato, Consequências, Contexto, Decisão

### Community 33 - "0010 — Limite diário com Redis + Lua"
Cohesion: 0.50
Nodes (4): 0010 — Limite diário com Redis + Lua, Consequências, Contexto, Decisão

### Community 34 - "0013 — Persistência com Spring JDBC (`JdbcClient`)"
Cohesion: 0.40
Nodes (5): 0013 — Persistência com Spring JDBC (`JdbcClient`), Alternativas consideradas, Consequências, Contexto, Decisão

### Community 35 - "0012 — Saldo no lançamento e trava pessimista por conta"
Cohesion: 0.40
Nodes (5): 0012 — Saldo no lançamento e trava pessimista por conta, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 38 - "dev-frontend.md"
Cohesion: 0.50
Nodes (3): Antes de devolver, Como trabalhar, Regras

### Community 39 - "dev-kotlin.md"
Cohesion: 0.50
Nodes (3): Antes de devolver, Como trabalhar, Regras

### Community 40 - "infra.md"
Cohesion: 0.50
Nodes (3): Antes de devolver, Escopo, Regras

### Community 41 - "0003 — Transactional Outbox e mensageria SNS/SQS"
Cohesion: 0.50
Nodes (4): 0003 — Transactional Outbox e mensageria SNS/SQS, Consequências, Contexto, Decisão

### Community 42 - "TransferController.kt"
Cohesion: 0.23
Nodes (3): Task A6: API das notificações (Marina, `dev-kotlin`) — paralela à A5, TransferDTO, TransferController

### Community 43 - "0011 — Contratos de API"
Cohesion: 0.50
Nodes (4): 0011 — Contratos de API, Consequências, Contexto, Decisão

### Community 44 - "Versões novas da etapa 1"
Cohesion: 0.50
Nodes (3): Observações, springdoc e Spring Boot 4.1.x, Versões novas da etapa 1

### Community 61 - "EventEnvelopeReaderTest"
Cohesion: 0.14
Nodes (8): EventEnvelopeReaderTest, AccountOpened, AccountTypeChanged, ReceivedEvent, TransferCompensated, TransferCompleted, TransferFailed, Unsupported

### Community 63 - "account"
Cohesion: 0.33
Nodes (3): account_type, account, ledger_entry

### Community 64 - "Balance"
Cohesion: 0.14
Nodes (4): Balance, Money, AccountTest, BalanceTest

### Community 65 - "outbox-table.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 69 - "LedgerEntryRecorded"
Cohesion: 0.10
Nodes (10): RecordCreditUseCase, DebitOutcome, RecordDebitUseCase, Recorded, Refused, RecordEntryCommand, RecordEntryResult, RecordCreditUseCaseTest (+2 more)

### Community 70 - "OutboxRelay"
Cohesion: 0.08
Nodes (17): Bloco 2 — relay e SNS FIFO, Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), Bloco 1 — preparação para o 2º serviço (branch `etapa-2-transfers`, PR 1), Task 1: Gradle para vários serviços (Rafael, `infra`) — cartão #37, Task 2: Relay em módulo próprio e teste de esquema (Marina, `dev-kotlin`) — cartões #25 e #30, Task 4: Consulta de lançamento pela chave no `accounts` (Marina, `dev-kotlin`), Task 5: PostgreSQL com um banco por serviço (Rafael, `infra`) — cartão #38 (+9 more)

### Community 71 - "JdbcTransferRepositoryIntegrationTest"
Cohesion: 0.06
Nodes (7): JdbcTransferStepLog, StepRecord, JdbcTransferRepositoryIntegrationTest, JdbcTransferStepLogIntegrationTest, aTransfer(), microsNow(), SpringTransactionRunnerIntegrationTest

### Community 72 - "SagaPropertiesTest.kt"
Cohesion: 0.07
Nodes (7): AccountsClientConfiguration, AccountsClientProperties, SagaTimeoutsValidator, PropertiesConfiguration, SagaPropertiesTest, SagaTimeoutsValidatorTest, ValidatorConfiguration

### Community 74 - "ProcessedEventRepository"
Cohesion: 0.08
Nodes (10): Task A3: Casos de uso e portas (Marina, `dev-kotlin`), 2. Módulos e camadas, JdbcProcessedEventRepository, NotificationController, NotificationNotFoundException, ProcessedEventRepository, UnknownAccountException, MarkNotificationsReadUseCase (+2 more)

### Community 75 - "smoke.sh"
Cohesion: 0.62
Nodes (6): aws_local(), call(), check_events(), check_notification(), smoke.sh script, uuid()

### Community 76 - "0016 — Saga de transferência orquestrada por HTTP síncrono"
Cohesion: 0.40
Nodes (5): 0016 — Saga de transferência orquestrada por HTTP síncrono, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 77 - "Etapa 0 — Fundação: design"
Cohesion: 0.10
Nodes (14): Task 3: Aplicação `accounts` com health do banco (Testcontainers), 2. Build e convenções, 3. Esqueleto do `accounts`, 4. Ambiente local, 5. CI (GitHub Actions), Critério de pronto, Etapa 0 — Fundação: design, Fora de escopo (e onde foi registrado) (+6 more)

### Community 78 - "HttpAccountsGateway.kt"
Cohesion: 0.09
Nodes (14): Task 12: Cliente HTTP do `accounts` (Marina, `dev-kotlin`), 4. Fluxos, AccountsResponseClassifier, AccountsAccountResponseDTO, AccountsEntryRequestDTO, AccountsEntryResponseDTO, AccountsProblemDTO, HttpAccountsGateway (+6 more)

### Community 79 - "AccountsApplicationIntegrationTest.kt"
Cohesion: 0.10
Nodes (7): Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`), AccountsApplicationIntegrationTest, ParameterizedTypeReference, RequestThreadProbe, RequestThreadProbeConfiguration, TransfersApplicationIntegrationTest, ParameterizedTypeReference

### Community 80 - "01-accounts-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 01-accounts-topic.sh script

### Community 81 - "TransferEvent"
Cohesion: 0.29
Nodes (5): EventOutbox, TransferCompensated, TransferCompleted, TransferEvent, TransferFailed

### Community 82 - "OutboxRelayScheduler"
Cohesion: 0.11
Nodes (3): OutboxRelayScheduler, BlockingStore, OutboxRelaySchedulerTest

### Community 83 - "OutboxBacklog"
Cohesion: 0.11
Nodes (4): OutboxMetrics, OutboxBacklog, OutboxMetricsTest, FakeOutboxStore

### Community 85 - "Foco de revisão"
Cohesion: 0.12
Nodes (13): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 4: Ambiente local (docker-compose), Task 5: CI no GitHub Actions (+5 more)

### Community 88 - "RunTransferSagaUseCase"
Cohesion: 0.12
Nodes (3): SagaSettings, SagaProperties, UseCaseConfiguration

### Community 89 - "OrderResource"
Cohesion: 0.27
Nodes (5): PersistenceUsesWeb, OrderResource, WebUsesPersistence, ApplicationUsesAdapters, DomainUsesAdapters

### Community 90 - "Money"
Cohesion: 0.06
Nodes (8): RunTransferSagaUseCaseTest, Handler, OutboxEventAppenderIntegrationTest, OutboxRow, Money, CompensateStepTest, DebitStepTest, MoneyTest

### Community 93 - "NotificationEventListenerTest.kt"
Cohesion: 0.08
Nodes (8): OutboxEventEnvelope, TransactionRunner, ConsumeOutcome, DUPLICATE, IGNORED, PROCESSED, MdcCorrelationContext, MdcCorrelationContextTest

### Community 97 - "retryWhileAccountBusy"
Cohesion: 0.23
Nodes (3): isAccountBusy(), retryWhileAccountBusy(), RetryWhileAccountBusyTest

### Community 98 - "TransferEventsIntegrationTest.kt"
Cohesion: 0.13
Nodes (3): ReceivedEvent, SentTransfer, TransferEventsIntegrationTest

### Community 99 - "Etapa 3 — Mensageria e `notification`: design"
Cohesion: 0.07
Nodes (26): Banco digital, Consulta e avisos, 10. Ambiente local e CI, 11. Documentação que acompanha, 1. Frentes de trabalho, 3. Modelo de domínio, 4. Fluxo de consumo, 5. Esquema do banco (`notification`) (+18 more)

### Community 100 - "MarkNotificationsReadUseCaseTest.kt"
Cohesion: 0.13
Nodes (3): MutableClock, MarkNotificationsReadUseCaseTest, MutableClock

### Community 101 - "NotificationApiIntegrationTest"
Cohesion: 0.06
Nodes (9): Task A5: Consumo das filas (Marina, `dev-kotlin`) — paralela à A6, EventEnvelopeReader, MalformedEventException, NotificationEventListener, NotificationEventListenerTest, ApiClient, ApiResponse, assertProblem() (+1 more)

### Community 102 - "RunTransferSagaUseCase"
Cohesion: 0.24
Nodes (3): CallResult, Run, RunTransferSagaUseCase

### Community 103 - "OutboxRelayConfigurationSupport.kt"
Cohesion: 0.12
Nodes (5): AwsProperties, OutboxRelayConfigurationSupport, OutboxRelayProperties, OutboxRelayConfiguration, OutboxRelayConfiguration

### Community 106 - "transfers/adapters/web/ProblemDetailsExceptionHandler.kt"
Cohesion: 0.11
Nodes (9): IdempotencyKeyHeader, ProblemDetailsExceptionHandler, AccountsUnavailableException, InvalidField, InvalidRequestException, SameAccountTransferException, TransferAccountNotFoundException, TransferFailedException (+1 more)

### Community 128 - "RequestTransferUseCaseTest"
Cohesion: 0.18
Nodes (4): RequestTransferUseCase, RequestTransferUseCaseTest, TransferRepository, TransferRepository

### Community 129 - "AccountsProxy.kt"
Cohesion: 0.12
Nodes (3): AccountsProxy, HeldRequest, Hold

### Community 131 - ".repeat"
Cohesion: 0.27
Nodes (7): RequestTransferCommand, AccountNotFound, AccountsUnavailable, Created, Existing, RequestTransferOutcome, SameAccount

### Community 132 - "AccountsCall"
Cohesion: 0.11
Nodes (10): AccountLookup, EXISTS, NOT_FOUND, UNAVAILABLE, AccountsGateway, FakeAccountsGateway, FakeAccountsGatewayTest, AccountsCall (+2 more)

### Community 135 - "NotificationIntegrationTest"
Cohesion: 0.06
Nodes (8): NotificationIntegrationTest, JdbcKnownAccountRepositoryIntegrationTest, JdbcNotificationRepositoryIntegrationTest, JdbcProcessedEventRepositoryIntegrationTest, aNotification(), microsNow(), NotificationWebIntegrationTest, OpenApiIntegrationTest

### Community 136 - "OutboxRelayConfigurationSupportTest.kt"
Cohesion: 0.07
Nodes (10): DemoRelayConfiguration, InfrastructureConfiguration, OutboxRelayConfigurationSupportTest, OutboxSchemaAssertionsIntegrationTest, assertOutboxSchemaMatchesModel(), createModelSchema(), currentSchema(), OutboxSchemaSnapshot (+2 more)

### Community 137 - "ServiceApiClients.kt"
Cohesion: 0.19
Nodes (5): AccountsApiClient, HttpResult, NotificationApiClient, send(), TransfersApiClient

### Community 140 - "Notification"
Cohesion: 0.10
Nodes (8): JdbcNotificationRepository, NotificationCursor, NotificationRepository, ListNotificationsUseCase, NotificationPage, InMemoryNotificationRepository, ListNotificationsUseCaseTest, Notification

### Community 141 - "ResumeDueTransfersUseCase"
Cohesion: 0.16
Nodes (7): Task 3: Regras ArchUnit compartilhadas (Marina, `dev-kotlin`) — cartão #32, Outcome, INTERRUPTED, SKIPPED, TAKEN, ResumeDueTransfersUseCase, TransferSagaSchedulerConfiguration

### Community 142 - "OutboxRelay"
Cohesion: 0.22
Nodes (6): 0015 — Relay do outbox com líder único e SNS FIFO, Alternativas consideradas, Consequências, Contexto, Decisão, OutboxRelay

### Community 153 - "KnownAccountRepository"
Cohesion: 0.17
Nodes (3): JdbcKnownAccountRepository, KnownAccountRepository, InMemoryKnownAccountRepository

### Community 155 - "TransferMetricsConfiguration.kt"
Cohesion: 0.23
Nodes (4): JdbcPendingTransferStats, PendingTransfers, TransferMetrics, TransferMetricsConfiguration

### Community 161 - "IdGenerator"
Cohesion: 0.10
Nodes (5): UuidV7IdGenerator, UuidV7IdGenerator, IdGenerator, SequentialIdGenerator, UuidV7IdGenerator

### Community 163 - "uuid"
Cohesion: 0.07
Nodes (4): Task 9: Portas e execução da saga (Marina, `dev-kotlin`), RetryBackoff, FakeEventOutbox, InMemoryTransferStepLog

### Community 173 - "Etapa 2 — `transfers` + saga: design"
Cohesion: 0.11
Nodes (14): 2. Módulos e camadas, 5. Esquema do banco (`transfers`), 6. Eventos, 7. API, nginx e Bruno, 8. Testes, 9. Ambiente local e CI, Critério de pronto, Decisões desta etapa (usuário, 2026-10-27) (+6 more)

### Community 175 - "clock"
Cohesion: 0.06
Nodes (8): ClockWiringIntegrationTest, IdempotencyKeyReusedException, CorrelationContext, IdGenerator, TransactionRunner, TransferRepository, TransferStepLog, RecordingCorrelationContext

### Community 177 - "JdbcOutboxWriter"
Cohesion: 0.13
Nodes (13): 10. Documentação que acompanha, 3. Esquema do banco, 5. Outbox e relay (`libs/outbox`), 6. API, 7. Testes, 8. Ambiente local e CI, 9. Entrega em dois blocos, Critério de pronto (+5 more)

### Community 178 - "TransferStep"
Cohesion: 0.07
Nodes (24): Transferências, Etapa 2 — `transfers` + saga Implementation Plan, Global Constraints, Review Focus, Task 8: Domínio da transferência e passos da saga (Marina, `dev-kotlin`), 10. Documentação que acompanha, 3. Modelo de domínio, Riscos (+16 more)

### Community 179 - "TransfersIntegrationTest"
Cohesion: 0.14
Nodes (3): ClockWiringIntegrationTest, TransfersIntegrationTest, UnexpectedErrorApiIntegrationTest

### Community 181 - ".resumeDueOver"
Cohesion: 0.11
Nodes (5): AccountsGateway, CorrelationContext, EventOutbox, TransactionRunner, TransferStepLog

### Community 183 - "AccountRequestDTOs.kt"
Cohesion: 0.33
Nodes (4): ChangeAccountTypeDTO, ChangeOverdraftLimitDTO, OpenAccountDTO, RecordEntryDTO

### Community 184 - "02-transfers-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 02-transfers-topic.sh script

### Community 186 - "notification/adapters/web/OpenApiConfiguration.kt"
Cohesion: 0.11
Nodes (6): ApiDocs, OpenApiConfiguration, ApiDocs, OpenApiConfiguration, ApiDocs, OpenApiConfiguration

### Community 187 - "transfer"
Cohesion: 0.47
Nodes (4): transfer, transfer_due_idx, transfer_step, transfer_step_transfer_idx

### Community 191 - "03-notification-queues.sh"
Cohesion: 0.39
Nodes (7): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, create_consumer_queue(), queue_arn(), 03-notification-queues.sh script

### Community 192 - "V3__outbox.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 198 - "DlqMetricsConfiguration.kt"
Cohesion: 0.21
Nodes (3): QueueProperties, DlqDepthProbe, DlqMetricsConfiguration

### Community 199 - "OrderController.kt"
Cohesion: 0.25
Nodes (4): OrderController, PlaceOrderCommand, PlaceOrderResult, PlaceOrderUseCase

### Community 200 - "NotificationCursor"
Cohesion: 0.14
Nodes (7): MarkedNotificationsDTO, MarkNotificationsReadDTO, NotificationDTO, NotificationPageDTO, NotificationCursorCodec, InvalidField, InvalidRequestException

### Community 202 - "0014 — Tipo de conta e cheque especial"
Cohesion: 0.40
Nodes (5): 0014 — Tipo de conta e cheque especial, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 203 - "ConsumeEventUseCase"
Cohesion: 0.05
Nodes (25): Etapa 3 — Mensageria e `notification` Implementation Plan, Frente A — `notification`, Frente B — débitos técnicos, Frentes e ordem, Review Focus, Task A10: Documentação (Clara, `documentador`) — paralela desde o início, Task A1: Esqueleto do `notification` e prova do `@SqsListener` (Marina, `dev-kotlin`), Task A2: Domínio das notificações (Marina, `dev-kotlin`) (+17 more)

### Community 205 - "0007 — Conta e ledger no mesmo serviço (`accounts`)"
Cohesion: 0.50
Nodes (4): 0007 — Conta e ledger no mesmo serviço (`accounts`), Consequências, Contexto, Decisão

### Community 215 - "V1__notification_schema.sql"
Cohesion: 0.50
Nodes (4): known_accounts, notifications, notifications_customer_page_idx, processed_events

### Community 220 - "0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS"
Cohesion: 0.40
Nodes (5): 0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 225 - "Banco digital em microsserviços"
Cohesion: 0.12
Nodes (13): Agentes do projeto, Ao terminar uma tarefa, Banco digital em microsserviços, Contexto, Fluxo de trabalho (skills), Git, Grafo do código (graphify), 0001 — Stack: Kotlin + Spring Boot 4 na AWS (+5 more)

### Community 226 - "Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)"
Cohesion: 0.29
Nodes (7): Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2), Task 10: Pedir, consultar e retomar transferências (Marina, `dev-kotlin`), Task 11: Persistência do `transfers` (Marina, `dev-kotlin`), Task 13: API HTTP do `transfers` (Marina, `dev-kotlin`), Task 14: Agendador, relay e eventos no `transfers` (Marina, `dev-kotlin`), Task 15: Infra local, Bruno, smoke e CI do `transfers` (Rafael, `infra`), Task 7: Esqueleto do `transfers` (Marina, `dev-kotlin`)

### Community 235 - "Status do projeto"
Cohesion: 0.33
Nodes (6): Ambiente do Claude, Como rodar localmente, Como usar o board, Concluído, Convenções que já custaram caro, Status do projeto

### Community 243 - ".record"
Cohesion: 0.19
Nodes (3): AccountRepository, LedgerEntryRepository, TransactionRunner

### Community 244 - "0002 — Clean Architecture por serviço"
Cohesion: 0.40
Nodes (5): 0002 — Clean Architecture por serviço, Alternativas consideradas, Consequências, Contexto, Decisão

## Knowledge Gaps
- **45 isolated node(s):** `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` (+40 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 616 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **120 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Transfer` connect `TransferStep` to `RequestTransferUseCaseTest`, `.repeat`, `ResumeDueTransfersUseCaseTest`, `.transfer`, `duration`, `TransferMetricsIntegrationTest.kt`, `JdbcTransferRepository`, `uuid`, `.run`, `TransferController.kt`, `Etapa 2 — `transfers` + saga: design`, `clock`, `JdbcTransferRepositoryIntegrationTest`, `BlockingRepository`, `Money`, `InMemoryTransferRepository`, `Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)`, `RunTransferSagaUseCase`, `transfers/adapters/web/ProblemDetailsExceptionHandler.kt`?**
  _High betweenness centrality (0.050) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Transfer` (e.g. with `Transferências` and `Global Constraints`) actually correct?**
  _`Transfer` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION` to the rest of the system?**
  _45 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `ChangeAccountTypeUseCaseTest` be split into smaller, more focused modules?**
  _Cohesion score 0.11494252873563218 - nodes in this community are weakly interconnected._
- **Why does `AccountsIntegrationTest` connect `AccountsIntegrationTest` to `accounts/bootstrap/web/UnexpectedErrorApiIntegrationTest.kt`, `Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)`, `AccountApiIntegrationTest`, `OutboxRelay`, `Account`, `RecordDebitUseCaseTest.kt`, `ConcurrencyIntegrationTest`, `OutboxRelayConfigurationSupportTest.kt`, `OutboxRelayIntegrationTest.kt`, `EntryLookupApiIntegrationTest`, `AccountsApplicationIntegrationTest.kt`, `clock`, `Account`, `OutboxRelayUnreachableBrokerIntegrationTest.kt`, `AccountTypeAdminApiIntegrationTest`, `OpenApiIntegrationTest`?**
  _High betweenness centrality (0.036) - this node is a cross-community bridge._
- **Are the 8 inferred relationships involving `Account` (e.g. with `Regras inegociáveis` and `Clientes e contas`) actually correct?**
  _`Account` has 8 INFERRED edges - model-reasoned connections that need verification._
- **Should `AccountApiIntegrationTest` be split into smaller, more focused modules?**
  _Cohesion score 0.1357142857142857 - nodes in this community are weakly interconnected._