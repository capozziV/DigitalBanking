# Graph Report - DigitalBanking  (2026-11-11)

## Corpus Check
- 440 files · ~122,591 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 29 file(s) not represented in the graph (top: .bru 19, (none) 3, .properties 2)

## Summary
- 3819 nodes · 9168 edges · 252 communities (117 shown, 135 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 713 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `8c4f0634`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- .outbox
- SqsListenerFifoIntegrationTest.kt
- AccountApiIntegrationTest
- ChangeAccountTypeUseCaseTest
- TransferApiIntegrationTest
- JdbcNotificationRepositoryIntegrationTest
- ServiceArchitectureRulesTest
- PendingEvent
- Account
- OpenAccountUseCase
- ListNotificationsUseCaseTest
- ProblemDetailsExceptionHandler.kt
- OutboxRelayIntegrationTest.kt
- TransfersIntegrationTest
- LedgerEntry
- Account
- OutboxRelayTest
- duration
- Decisões tomadas
- README.md
- SpringTransactionRunnerIntegrationTest.kt
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
- KnownAccountRepository
- Order
- dev-frontend.md
- dev-kotlin.md
- infra.md
- 0003 — Transactional Outbox e mensageria SNS/SQS
- NotificationEventListener.kt
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
- JdbcOutboxWriter
- JdbcTransferRepositoryIntegrationTest
- SagaTimeoutsValidatorTest.kt
- NotificationConsumptionIntegrationTest
- notification/bootstrap/UseCaseConfiguration.kt
- smoke.sh
- NotificationQueues
- Etapa 0 — Fundação: design
- HttpAccountsGateway
- AccountsApplicationIntegrationTest.kt
- 01-accounts-topic.sh
- StatementEntry
- OutboxRelayScheduler
- OutboxMetrics
- HttpAccountsGatewayIntegrationTest
- Foco de revisão
- JdbcOutboxStoreIntegrationTest
- RunTransferSagaUseCase
- OrderResource
- RunTransferSagaUseCaseTest
- ConsumeEventUseCaseTest
- LocalStackTestSupport
- MdcCorrelationContextTest.kt
- RunTransferSagaUseCase
- AccountTypeAdminController.kt
- InMemoryTransferRepository
- retryWhileAccountBusy
- TransferEventsIntegrationTest.kt
- Etapa 3 — Mensageria e `notification`: design
- MarkNotificationsReadUseCaseTest
- NotificationApiIntegrationTest
- TransfersDatabase
- OutboxRelayConfigurationSupport.kt
- Etapa 4 — `statement` (extrato com CQRS no DynamoDB): design
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
- NotificationEventListenerTest
- AccountsCall
- ResumeDueTransfersUseCaseTest
- ConsumeEventUseCase
- NotificationIntegrationTest
- OutboxRelayConfigurationSupportTest.kt
- ServiceApiClients.kt
- ConcurrencyIntegrationTest
- UseCaseConfiguration
- Notification
- ResumeDueTransfersUseCase
- TransferRepository
- AccountsResponseClassifierTest
- OutboxEventAppenderIntegrationTest.kt
- EndToEndEnvironment
- LocalStackTestSupportIntegrationTest.kt
- TransferMetricsIntegrationTest.kt
- SnsEventPublisherTest.kt
- OutboxRelayUnreachableBrokerIntegrationTest.kt
- JdbcTransferRepository
- AccountTypeAdminApiIntegrationTest
- TransferSagaScheduler
- TransferStatus
- OutboxRelaySettingsTest.kt
- TransferMetricsConfiguration.kt
- JdbcProcessedEventRepositoryIntegrationTest
- ApiClient
- ProcessLogsOnFailureExtension
- ServiceProcess
- LocalStackFifoIntegrationTest.kt
- IdGenerator
- SchedulerLeaseOwners
- RunTransferSagaUseCaseTest.kt
- EndToEndInputs
- InMemoryStatementRepository
- .entry
- Test
- TransactionRunner
- Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)
- JdbcTransferStepLog.kt
- .brl
- uuid
- TransferStep
- SagaPropertiesTest.kt
- LocalStackInitScripts
- .resumeDueOver
- TransferController.kt
- .handle
- 02-transfers-topic.sh
- OpenApiIntegrationTest
- DlqMetricsConfiguration.kt
- transfer
- .container
- GetStatementUseCaseTest
- .repeat
- 03-notification-queues.sh
- V3__outbox.sql
- accounts/bootstrap/web/UnexpectedErrorApiIntegrationTest.kt
- notification/adapters/web/OpenApiConfiguration.kt
- OrderController.kt
- NotificationCursor
- OutboxRelay
- 0014 — Tipo de conta e cheque especial
- ConsumeEventUseCase.kt
- Order
- 0007 — Conta e ledger no mesmo serviço (`accounts`)
- ConflictingStatementEntryException
- transfers/adapters/web/CorrelationIdFilter.kt
- BlockingRepository
- Regras inegociáveis
- ConsumeEventUseCase.kt
- accounts/adapters/web/CorrelationIdFilter.kt
- AccountsClientConfiguration
- HttpAccountsGateway.kt
- Frente A — `notification`
- V1__notification_schema.sql
- Bloco 1 — `accounts` gravando no outbox
- AccountsIntegrationTest
- StatementEntryTest
- 0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS
- GetTransferUseCase
- AccountsApplication.kt
- status.md
- Frente B — débitos técnicos
- 04-statement.sh
- TransfersApplicationIntegrationTest.kt
- MutableClock
- Review Focus
- .transfer
- NotificationCursor
- roadmap.md
- Handler
- Status do projeto
- RequestTransferDTO
- 0002-clean-architecture-por-servico.md
- SpringTransactionRunner
- .inTransaction
- transfers/adapters/persistence/OutboxWriterConfiguration.kt
- 0016 — Saga de transferência orquestrada por HTTP síncrono
- .record
- MutableClock
- TestListeners
- accounts/bootstrap/OutboxRelayConfiguration.kt
- NotificationTest
- EntryKind

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
- `Task 14: Agendador, relay e eventos no `transfers` (Marina, `dev-kotlin`)` --references--> `OutboxRelayScheduler`  [INFERRED]
  docs/superpowers/plans/2026-10-27-etapa-2-transfers.md → libs/outbox-relay/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/relay/spring/OutboxRelayScheduler.kt
- `Task A7: `AccountTypeChanged` versão 2 no `accounts` (Marina, `dev-kotlin`) — paralela desde o início` --references--> `LedgerEntryRecorded`  [INFERRED]
  docs/superpowers/plans/2026-10-28-etapa-3-mensageria.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/AccountEvent.kt
- `8. Ambiente local e CI` --references--> `LedgerEntryRecorded`  [INFERRED]
  docs/superpowers/specs/2026-10-22-etapa-1-accounts-design.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/AccountEvent.kt
- `Decisões desta etapa (usuário, 2026-11-10)` --references--> `LedgerEntryRecorded`  [INFERRED]
  docs/superpowers/specs/2026-11-10-etapa-4-statement-design.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/AccountEvent.kt
- `Decisão` --references--> `LedgerEntry`  [INFERRED]
  docs/adr/0012-saldo-no-lancamento-e-trava-pessimista.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/LedgerEntry.kt

## Import Cycles
- None detected.

## Communities (252 total, 135 thin omitted)

### Community 0 - ".outbox"
Cohesion: 0.33
Nodes (4): Etapa 1 — `accounts` Implementation Plan, Global Constraints, Review Focus, EventOutbox

### Community 3 - "ChangeAccountTypeUseCaseTest"
Cohesion: 0.09
Nodes (9): EventOutbox, TransactionRunner, ChangeAccountTypeUseCase, CallLog, DirectTransactionRunner, RecordingEventOutbox, ChangeAccountTypeUseCaseTest, AccountEvent (+1 more)

### Community 5 - "JdbcNotificationRepositoryIntegrationTest"
Cohesion: 0.24
Nodes (3): JdbcNotificationRepositoryIntegrationTest, aNotification(), microsNow()

### Community 6 - "ServiceArchitectureRulesTest"
Cohesion: 0.06
Nodes (5): ServiceArchitectureRules, ServiceArchitectureRulesTest, ArchitectureTest, ArchitectureTest, ArchitectureTest

### Community 7 - "PendingEvent"
Cohesion: 0.11
Nodes (8): Bloco 2 — relay e SNS FIFO, Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), EventPublisher, JdbcOutboxStore, NoOpPublisher, PendingEvent, OutboxEventEnvelopeTest, RecordingEventPublisher

### Community 8 - "Account"
Cohesion: 0.07
Nodes (14): JdbcAccountRepository, AccountBusyException, AccountRepository, InsertAccountResult, DUPLICATE_CUSTOMER, DUPLICATE_OPENING_KEY, INSERTED, InMemoryAccountRepository (+6 more)

### Community 9 - "OpenAccountUseCase"
Cohesion: 0.17
Nodes (8): Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`), CustomerAlreadyHasAccountException, IdempotencyKeyReusedException, OpenAccountCommand, OpenAccountResult, OpenAccountUseCase, OpenAccountUseCaseTest, AccountOpened

### Community 11 - "ProblemDetailsExceptionHandler.kt"
Cohesion: 0.09
Nodes (8): IdempotencyKeyHeader, ProblemDetailsExceptionHandler, AccountTypeNotFoundException, EntryNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException, BalanceOverflowException

### Community 12 - "OutboxRelayIntegrationTest.kt"
Cohesion: 0.09
Nodes (3): OutboxRelayIntegrationTest, ReceivedEvent, RecordingPublisherConfiguration

### Community 13 - "TransfersIntegrationTest"
Cohesion: 0.08
Nodes (5): ClockWiringIntegrationTest, TransferSchemaIntegrationTest, TransfersIntegrationTest, OpenApiIntegrationTest, UnexpectedErrorApiIntegrationTest

### Community 14 - "LedgerEntry"
Cohesion: 0.05
Nodes (17): Task 5: Casos de uso de crédito e débito (Marina, `dev-kotlin`), JdbcLedgerEntryRepository, AccountNotFoundException, IdGenerator, LedgerEntryRepository, FindEntryByIdempotencyKeyUseCase, IdempotentEntryLookup, DebitOutcome (+9 more)

### Community 17 - "duration"
Cohesion: 0.05
Nodes (28): Clientes e contas, Ledger, Global Constraints, 1. Módulos e camadas, 2. Modelo de domínio, 4. Fluxos, JdbcAccountTypeRepository, AccountTypeRepository (+20 more)

### Community 18 - "Decisões tomadas"
Cohesion: 0.13
Nodes (15): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Em aberto, Fluxo de entrada (+7 more)

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
Cohesion: 0.12
Nodes (15): 1. Configurar o ambiente, 2. Subir a infraestrutura (PostgreSQL, Redis, nginx, LocalStack), 3. Build e testes, 4. Subir os serviços `accounts`, `transfers` e `notification`, 5. Conferir a API pelo nginx, 6. Ver os eventos na fila de inspeção, 7. Coleção Bruno, 8. Derrubar tudo (+7 more)

### Community 30 - "AccountController.kt"
Cohesion: 0.15
Nodes (8): Regras inegociáveis, AccountController, ChangeAccountTypeDTO, ChangeOverdraftLimitDTO, OpenAccountDTO, RecordEntryDTO, AccountDTO, LedgerEntryDTO

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

### Community 36 - "KnownAccountRepository"
Cohesion: 0.17
Nodes (3): JdbcKnownAccountRepository, KnownAccountRepository, InMemoryKnownAccountRepository

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

### Community 42 - "NotificationEventListener.kt"
Cohesion: 0.22
Nodes (5): NotificationEventListener, ConsumeOutcome, DUPLICATE, IGNORED, PROCESSED

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
Cohesion: 0.16
Nodes (4): Balance, Money, AccountTest, BalanceTest

### Community 65 - "outbox-table.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 69 - "LedgerEntryRecorded"
Cohesion: 0.10
Nodes (12): 0018 — Extrato ordenado pela sequência do ledger, Alternativas consideradas, Consequências, Contexto, Decisão, RecordCreditUseCase, RecordEntryCommand, RecordEntryResult (+4 more)

### Community 70 - "JdbcOutboxWriter"
Cohesion: 0.09
Nodes (20): Task 7: `libs/outbox` — envelope e writer (Marie, `dev-kotlin`; em paralelo às Tasks 2–6), 10. Documentação que acompanha, 3. Esquema do banco, 5. Outbox e relay (`libs/outbox`), 6. API, 7. Testes, 8. Ambiente local e CI, 9. Entrega em dois blocos (+12 more)

### Community 71 - "JdbcTransferRepositoryIntegrationTest"
Cohesion: 0.14
Nodes (4): StepRecord, JdbcTransferRepositoryIntegrationTest, aTransfer(), microsNow()

### Community 72 - "SagaTimeoutsValidatorTest.kt"
Cohesion: 0.17
Nodes (4): AccountsClientProperties, SagaTimeoutsValidator, SagaTimeoutsValidatorTest, ValidatorConfiguration

### Community 74 - "notification/bootstrap/UseCaseConfiguration.kt"
Cohesion: 0.24
Nodes (3): 2. Módulos e camadas, ProcessedEventRepository, UseCaseConfiguration

### Community 75 - "smoke.sh"
Cohesion: 0.62
Nodes (6): aws_local(), call(), check_events(), check_notification(), smoke.sh script, uuid()

### Community 76 - "NotificationQueues"
Cohesion: 0.12
Nodes (5): DeadLetter, NotificationQueues, PublishedMessage, ReceivedEvent, TransferEventsQueue

### Community 77 - "Etapa 0 — Fundação: design"
Cohesion: 0.22
Nodes (9): 2. Build e convenções, 4. Ambiente local, 5. CI (GitHub Actions), Critério de pronto, Etapa 0 — Fundação: design, Fora de escopo (e onde foi registrado), Objetivo, Restrições (de `CLAUDE.md` e ADRs) (+1 more)

### Community 78 - "HttpAccountsGateway"
Cohesion: 0.20
Nodes (4): 4. Fluxos, AccountsResponseClassifier, HttpAccountsGateway, RawResponse

### Community 79 - "AccountsApplicationIntegrationTest.kt"
Cohesion: 0.16
Nodes (5): Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`), AccountsApplicationIntegrationTest, ParameterizedTypeReference, RequestThreadProbe, RequestThreadProbeConfiguration

### Community 80 - "01-accounts-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 01-accounts-topic.sh script

### Community 81 - "StatementEntry"
Cohesion: 0.15
Nodes (10): 10. Documentação que acompanha, 2. Módulos e camadas, 3. Modelo de domínio, 4. Tabela no DynamoDB, AccountNotFoundException, StatementRepository, GetStatementUseCase, StatementCursor (+2 more)

### Community 82 - "OutboxRelayScheduler"
Cohesion: 0.10
Nodes (8): 0015 — Relay do outbox com líder único e SNS FIFO, Alternativas consideradas, Consequências, Contexto, Decisão, OutboxRelayScheduler, BlockingStore, OutboxRelaySchedulerTest

### Community 83 - "OutboxMetrics"
Cohesion: 0.12
Nodes (4): OutboxBacklog, OutboxStore, OutboxMetricsTest, FakeOutboxStore

### Community 85 - "Foco de revisão"
Cohesion: 0.18
Nodes (10): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 4: Ambiente local (docker-compose), Task 5: CI no GitHub Actions (+2 more)

### Community 87 - "JdbcOutboxStoreIntegrationTest"
Cohesion: 0.06
Nodes (5): JdbcOutboxStoreIntegrationTest, JdbcOutboxWriterIntegrationTest, OutboxMessage, OutboxEventAppender, OutboxEventAppender

### Community 88 - "RunTransferSagaUseCase"
Cohesion: 0.14
Nodes (3): SagaSettings, SagaProperties, UseCaseConfiguration

### Community 89 - "OrderResource"
Cohesion: 0.27
Nodes (5): PersistenceUsesWeb, OrderResource, WebUsesPersistence, ApplicationUsesAdapters, DomainUsesAdapters

### Community 93 - "MdcCorrelationContextTest.kt"
Cohesion: 0.16
Nodes (3): MdcCorrelationContext, MdcCorrelationContextTest, CorrelationContext

### Community 94 - "RunTransferSagaUseCase"
Cohesion: 0.13
Nodes (5): CallResult, Run, RunTransferSagaUseCase, FakeAccountsGateway, FakeAccountsGatewayTest

### Community 96 - "InMemoryTransferRepository"
Cohesion: 0.15
Nodes (3): InMemoryTransferRepository, Lease, InMemoryTransferRepositoryTest

### Community 97 - "retryWhileAccountBusy"
Cohesion: 0.23
Nodes (3): isAccountBusy(), retryWhileAccountBusy(), RetryWhileAccountBusyTest

### Community 98 - "TransferEventsIntegrationTest.kt"
Cohesion: 0.12
Nodes (3): ReceivedEvent, SentTransfer, TransferEventsIntegrationTest

### Community 99 - "Etapa 3 — Mensageria e `notification`: design"
Cohesion: 0.08
Nodes (25): Banco digital, Consulta e avisos, 10. Ambiente local e CI, 11. Documentação que acompanha, 1. Frentes de trabalho, 3. Modelo de domínio, 4. Fluxo de consumo, 5. Esquema do banco (`notification`) (+17 more)

### Community 101 - "NotificationApiIntegrationTest"
Cohesion: 0.08
Nodes (7): Task A5: Consumo das filas (Marina, `dev-kotlin`) — paralela à A6, EventEnvelopeReader, MalformedEventException, ApiClient, ApiResponse, assertProblem(), NotificationApiIntegrationTest

### Community 103 - "OutboxRelayConfigurationSupport.kt"
Cohesion: 0.14
Nodes (6): Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), OutboxRelaySettings, AwsProperties, OutboxRelayConfigurationSupport, OutboxRelayProperties, OutboxRelayConfiguration

### Community 105 - "Etapa 4 — `statement` (extrato com CQRS no DynamoDB): design"
Cohesion: 0.18
Nodes (11): 1. Frentes de trabalho, 6. API, nginx e Bruno, 7. Operação, 9. Ambiente local e CI, Critério de pronto, Decisões desta etapa (usuário, 2026-11-10), Etapa 4 — `statement` (extrato com CQRS no DynamoDB): design, Fora de escopo (e onde fica registrado) (+3 more)

### Community 128 - "RequestTransferUseCaseTest"
Cohesion: 0.18
Nodes (4): RequestTransferUseCase, RequestTransferUseCaseTest, TransferRepository, TransferRepository

### Community 129 - "AccountsProxy.kt"
Cohesion: 0.17
Nodes (3): AccountsProxy, HeldRequest, Hold

### Community 132 - "AccountsCall"
Cohesion: 0.10
Nodes (16): Task 12: Cliente HTTP do `accounts` (Marina, `dev-kotlin`), AccountsEntryRequestDTO, AccountLookup, EXISTS, NOT_FOUND, UNAVAILABLE, AccountsGateway, AccountsGateway (+8 more)

### Community 134 - "ConsumeEventUseCase"
Cohesion: 0.26
Nodes (5): Etapa 3 — Mensageria e `notification` Implementation Plan, Frentes e ordem, Review Focus, ConsumeEventUseCase, Draft

### Community 135 - "NotificationIntegrationTest"
Cohesion: 0.10
Nodes (4): NotificationIntegrationTest, JdbcKnownAccountRepositoryIntegrationTest, NotificationWebIntegrationTest, OpenApiIntegrationTest

### Community 136 - "OutboxRelayConfigurationSupportTest.kt"
Cohesion: 0.06
Nodes (12): DemoRelayConfiguration, InfrastructureConfiguration, OutboxRelayConfigurationSupportTest, OutboxSchemaAssertionsIntegrationTest, assertOutboxSchemaMatchesModel(), createModelSchema(), currentSchema(), OutboxSchemaSnapshot (+4 more)

### Community 137 - "ServiceApiClients.kt"
Cohesion: 0.19
Nodes (5): AccountsApiClient, HttpResult, NotificationApiClient, send(), TransfersApiClient

### Community 140 - "Notification"
Cohesion: 0.13
Nodes (4): JdbcNotificationRepository, NotificationRepository, InMemoryNotificationRepository, Notification

### Community 141 - "ResumeDueTransfersUseCase"
Cohesion: 0.16
Nodes (7): Task 3: Regras ArchUnit compartilhadas (Marina, `dev-kotlin`) — cartão #32, Outcome, INTERRUPTED, SKIPPED, TAKEN, ResumeDueTransfersUseCase, TransferSagaSchedulerConfiguration

### Community 144 - "OutboxEventAppenderIntegrationTest.kt"
Cohesion: 0.18
Nodes (4): OutboxEventAppenderIntegrationTest, OutboxRow, Money, MoneyTest

### Community 153 - "TransferStatus"
Cohesion: 0.10
Nodes (21): Transferências, Etapa 2 — `transfers` + saga Implementation Plan, Global Constraints, Review Focus, 10. Documentação que acompanha, 5. Esquema do banco (`transfers`), 6. Eventos, 7. API, nginx e Bruno (+13 more)

### Community 155 - "TransferMetricsConfiguration.kt"
Cohesion: 0.15
Nodes (4): JdbcPendingTransferStats, PendingTransfers, TransferMetrics, TransferMetricsConfiguration

### Community 161 - "IdGenerator"
Cohesion: 0.09
Nodes (6): UuidV7IdGenerator, UuidV7IdGenerator, IdGenerator, SequentialIdGenerator, UuidV7IdGenerator, IdGenerator

### Community 163 - "RunTransferSagaUseCaseTest.kt"
Cohesion: 0.09
Nodes (6): Task 9: Portas e execução da saga (Marina, `dev-kotlin`), RetryBackoff, FakeEventOutbox, InMemoryTransferStepLog, MutableClock, RetryBackoffTest

### Community 169 - "InMemoryStatementRepository"
Cohesion: 0.16
Nodes (5): Task 3: Domínio e casos de uso (Marina, `dev-kotlin`), 8. Testes, InMemoryStatementRepository, RecordStatementEntryUseCaseTest, RegisterAccountUseCaseTest

### Community 173 - "Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)"
Cohesion: 0.29
Nodes (7): Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2), Task 10: Pedir, consultar e retomar transferências (Marina, `dev-kotlin`), Task 11: Persistência do `transfers` (Marina, `dev-kotlin`), Task 13: API HTTP do `transfers` (Marina, `dev-kotlin`), Task 14: Agendador, relay e eventos no `transfers` (Marina, `dev-kotlin`), Task 15: Infra local, Bruno, smoke e CI do `transfers` (Rafael, `infra`), Task 7: Esqueleto do `transfers` (Marina, `dev-kotlin`)

### Community 177 - "uuid"
Cohesion: 0.06
Nodes (6): ClockWiringIntegrationTest, JdbcProcessedEventRepository, InMemoryProcessedEventRepository, TransferStepLog, FixedClockConfiguration, OutboxWriterClockIntegrationTest

### Community 178 - "TransferStep"
Cohesion: 0.10
Nodes (14): Task 8: Domínio da transferência e passos da saga (Marina, `dev-kotlin`), 3. Modelo de domínio, Riscos, Transfer, Compensate, Compensated, Completed, Credit (+6 more)

### Community 181 - ".resumeDueOver"
Cohesion: 0.10
Nodes (9): EventOutbox, CorrelationContext, EventOutbox, TransactionRunner, TransferStepLog, TransferCompensated, TransferCompleted, TransferEvent (+1 more)

### Community 183 - ".handle"
Cohesion: 0.22
Nodes (8): IdempotencyKeyHeader, AccountsUnavailableException, InvalidField, InvalidRequestException, SameAccountTransferException, TransferAccountNotFoundException, TransferFailedException, TransferNotFoundException

### Community 184 - "02-transfers-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 02-transfers-topic.sh script

### Community 186 - "DlqMetricsConfiguration.kt"
Cohesion: 0.21
Nodes (3): QueueProperties, DlqDepthProbe, DlqMetricsConfiguration

### Community 187 - "transfer"
Cohesion: 0.47
Nodes (4): transfer, transfer_due_idx, transfer_step, transfer_step_transfer_idx

### Community 190 - ".repeat"
Cohesion: 0.19
Nodes (8): IdempotencyKeyReusedException, RequestTransferCommand, AccountNotFound, AccountsUnavailable, Created, Existing, RequestTransferOutcome, SameAccount

### Community 191 - "03-notification-queues.sh"
Cohesion: 0.39
Nodes (7): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, create_consumer_queue(), queue_arn(), 03-notification-queues.sh script

### Community 192 - "V3__outbox.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 198 - "notification/adapters/web/OpenApiConfiguration.kt"
Cohesion: 0.11
Nodes (6): ApiDocs, OpenApiConfiguration, ApiDocs, OpenApiConfiguration, ApiDocs, OpenApiConfiguration

### Community 199 - "OrderController.kt"
Cohesion: 0.25
Nodes (4): OrderController, PlaceOrderCommand, PlaceOrderResult, PlaceOrderUseCase

### Community 200 - "NotificationCursor"
Cohesion: 0.16
Nodes (5): MarkedNotificationsDTO, MarkNotificationsReadDTO, NotificationController, InvalidField, InvalidRequestException

### Community 201 - "OutboxRelay"
Cohesion: 0.25
Nodes (3): Task 2: Relay em módulo próprio e teste de esquema (Marina, `dev-kotlin`) — cartões #25 e #30, OutboxMetrics, OutboxRelay

### Community 202 - "0014 — Tipo de conta e cheque especial"
Cohesion: 0.40
Nodes (5): 0014 — Tipo de conta e cheque especial, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 203 - "ConsumeEventUseCase.kt"
Cohesion: 0.19
Nodes (3): NotificationContent, NotificationText, NotificationTextTest

### Community 205 - "0007 — Conta e ledger no mesmo serviço (`accounts`)"
Cohesion: 0.50
Nodes (4): 0007 — Conta e ledger no mesmo serviço (`accounts`), Consequências, Contexto, Decisão

### Community 206 - "ConflictingStatementEntryException"
Cohesion: 0.20
Nodes (8): Task 5: Consumo SQS e métrica da DLQ (Marina, `dev-kotlin`; paralela à Task 6), 5. Fluxo de consumo, ConflictingStatementEntryException, RecordOutcome, DUPLICATE, RECORDED, RecordStatementEntryUseCase, RegisterAccountUseCase

### Community 209 - "Regras inegociáveis"
Cohesion: 0.29
Nodes (7): Agentes do projeto, Ao terminar uma tarefa, Banco digital em microsserviços, Contexto, Fluxo de trabalho (skills), Git, Grafo do código (graphify)

### Community 210 - "ConsumeEventUseCase.kt"
Cohesion: 0.11
Nodes (7): Task A3: Casos de uso e portas (Marina, `dev-kotlin`), Task A6: API das notificações (Marina, `dev-kotlin`) — paralela à A5, SpringTransactionRunner, NotificationNotFoundException, TransactionRunner, UnknownAccountException, MarkNotificationsReadUseCase

### Community 213 - "HttpAccountsGateway.kt"
Cohesion: 0.27
Nodes (3): AccountsAccountResponseDTO, AccountsEntryResponseDTO, AccountsProblemDTO

### Community 214 - "Frente A — `notification`"
Cohesion: 0.25
Nodes (8): Frente A — `notification`, Task A10: Documentação (Clara, `documentador`) — paralela desde o início, Task A1: Esqueleto do `notification` e prova do `@SqsListener` (Marina, `dev-kotlin`), Task A2: Domínio das notificações (Marina, `dev-kotlin`), Task A4: Persistência (Marina, `dev-kotlin`), Task A7: `AccountTypeChanged` versão 2 no `accounts` (Marina, `dev-kotlin`) — paralela desde o início, Task A8: Infra local, Bruno, smoke e CI do `notification` (Rafael, `infra`), Task A9: Testes ponta a ponta (Marina, `dev-kotlin`) — depois de A5, A6, A7, A8 e B5

### Community 215 - "V1__notification_schema.sql"
Cohesion: 0.50
Nodes (4): known_accounts, notifications, notifications_customer_page_idx, processed_events

### Community 216 - "Bloco 1 — `accounts` gravando no outbox"
Cohesion: 0.20
Nodes (10): Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 13: Documentação do bloco 1 e das decisões da etapa (Clara, `documentador`; em paralelo às Tasks 8–12), Task 2: Valores monetários e tipos (Marina, `dev-kotlin`), Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`), Task 6: Casos de uso de tipo e limites (Marina, `dev-kotlin`) (+2 more)

### Community 220 - "0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS"
Cohesion: 0.40
Nodes (5): 0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 223 - "GetTransferUseCase"
Cohesion: 0.25
Nodes (3): 2. Módulos e camadas, GetTransferUseCase, GetTransferUseCaseTest

### Community 224 - "AccountsApplication.kt"
Cohesion: 0.20
Nodes (5): Task 3: Aplicação `accounts` com health do banco (Testcontainers), 3. Esqueleto do `accounts`, Testes (no `bootstrap`, que enxerga todos os módulos), AccountsApplication, TransfersApplication

### Community 225 - "status.md"
Cohesion: 0.13
Nodes (10): 0001 — Stack: Kotlin + Spring Boot 4 na AWS, Alternativas consideradas, Consequências, Contexto, Decisão, Bloco 1 — preparação para o 2º serviço (branch `etapa-2-transfers`, PR 1), Task 1: Gradle para vários serviços (Rafael, `infra`) — cartão #37, Task 4: Consulta de lançamento pela chave no `accounts` (Marina, `dev-kotlin`) (+2 more)

### Community 226 - "Frente B — débitos técnicos"
Cohesion: 0.29
Nodes (6): Frente B — débitos técnicos, Task B1: Configuração do relay em `libs/outbox-relay` (Marina, `dev-kotlin`) — cartão #92, Task B2: Backoff do relay e testes (Marina, `dev-kotlin`) — cartões #24 e #26, depois da B1, Task B3: Domínio e persistência do `transfers` (Marina, `dev-kotlin`) — cartões #95 e #93, Task B4: Aplicação e adapters do `transfers` (Marina, `dev-kotlin`) — cartão #94, depois da B3, Task B5: Endurecer os testes ponta a ponta (Marina, `dev-kotlin`) — cartão #96, antes da A9

### Community 227 - "04-statement.sh"
Cohesion: 0.43
Nodes (6): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, queue_arn(), 04-statement.sh script

### Community 230 - "Review Focus"
Cohesion: 0.29
Nodes (7): Review Focus, Task 1: Esqueleto Gradle e cliente DynamoDB no apoio de testes (Rafael, `infra`), Task 2: LocalStack, compose, nginx e CI (Rafael, `infra`), Task 4: Repositório DynamoDB e bootstrap (Marina, `dev-kotlin`), Task 6: API do extrato (Marina, `dev-kotlin`; paralela à Task 5), Task 7: e2e, smoke e Bruno (Marina, `dev-kotlin`), Task 8: Documentação (Clara, `documentador`)

### Community 231 - ".transfer"
Cohesion: 0.05
Nodes (9): CompensateStepTest, CreditStepTest, DebitStepTest, ResolveCreditStepTest, ResolveDebitStepTest, TerminalStepsTest, TransferFixtures, TransferSagaPathsTest (+1 more)

### Community 232 - "NotificationCursor"
Cohesion: 0.18
Nodes (6): NotificationDTO, NotificationPageDTO, NotificationCursorCodec, NotificationCursor, ListNotificationsUseCase, NotificationPage

### Community 233 - "roadmap.md"
Cohesion: 0.25
Nodes (4): Roadmap, Etapa 4 — `statement`: plano de implementação, Global Constraints, Ordem e paralelismo

### Community 235 - "Status do projeto"
Cohesion: 0.33
Nodes (6): Ambiente do Claude, Como rodar localmente, Como usar o board, Concluído, Convenções que já custaram caro, Status do projeto

### Community 237 - "0002-clean-architecture-por-servico.md"
Cohesion: 0.33
Nodes (5): 0002 — Clean Architecture por serviço, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 241 - "0016 — Saga de transferência orquestrada por HTTP síncrono"
Cohesion: 0.40
Nodes (5): 0016 — Saga de transferência orquestrada por HTTP síncrono, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 251 - "EntryKind"
Cohesion: 0.50
Nodes (3): EntryKind, CREDIT, DEBIT

## Knowledge Gaps
- **52 isolated node(s):** `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` (+47 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 673 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **135 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Transfer` connect `TransferStep` to `RequestTransferUseCaseTest`, `ResumeDueTransfersUseCaseTest`, `TransferRepository`, `duration`, `TransferMetricsIntegrationTest.kt`, `SpringTransactionRunnerIntegrationTest.kt`, `JdbcTransferRepository`, `TransferStatus`, `RunTransferSagaUseCaseTest.kt`, `Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)`, `uuid`, `TransferController.kt`, `.handle`, `.repeat`, `JdbcTransferRepositoryIntegrationTest`, `BlockingRepository`, `RunTransferSagaUseCaseTest`, `RunTransferSagaUseCase`, `GetTransferUseCase`, `InMemoryTransferRepository`, `.transfer`?**
  _High betweenness centrality (0.061) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Transfer` (e.g. with `Transferências` and `Global Constraints`) actually correct?**
  _`Transfer` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION` to the rest of the system?**
  _52 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AccountApiIntegrationTest` be split into smaller, more focused modules?**
  _Cohesion score 0.1357142857142857 - nodes in this community are weakly interconnected._
- **Why does `AccountsIntegrationTest` connect `AccountsIntegrationTest` to `accounts/bootstrap/web/UnexpectedErrorApiIntegrationTest.kt`, `AccountApiIntegrationTest`, `OutboxRelayConfigurationSupport.kt`, `OutboxRelayConfigurationSupportTest.kt`, `ConcurrencyIntegrationTest`, `OutboxRelayIntegrationTest.kt`, `Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)`, `EntryLookupApiIntegrationTest`, `AccountsApplicationIntegrationTest.kt`, `Account`, `uuid`, `OutboxRelayUnreachableBrokerIntegrationTest.kt`, `AccountTypeAdminApiIntegrationTest`, `Bloco 1 — `accounts` gravando no outbox`, `OpenApiIntegrationTest`, `AccountController.kt`?**
  _High betweenness centrality (0.041) - this node is a cross-community bridge._
- **Are the 8 inferred relationships involving `Account` (e.g. with `Regras inegociáveis` and `Clientes e contas`) actually correct?**
  _`Account` has 8 INFERRED edges - model-reasoned connections that need verification._
- **Should `ChangeAccountTypeUseCaseTest` be split into smaller, more focused modules?**
  _Cohesion score 0.0945945945945946 - nodes in this community are weakly interconnected._