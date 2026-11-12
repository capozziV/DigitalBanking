# Graph Report - etapa-6  (2026-11-12)

## Corpus Check
- 529 files · ~154,984 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 39 file(s) not represented in the graph (top: .bru 28, (none) 4, .properties 2)

## Summary
- 4760 nodes · 11499 edges · 318 communities (124 shown, 194 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 827 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `7408ecc7`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- StatementApiIntegrationTest
- SqsListenerFifoIntegrationTest.kt
- AccountApiIntegrationTest
- SpringTransactionRunner
- TransferApiIntegrationTest
- RedisDailyLimitGateway
- ServiceArchitectureRulesTest
- .transfer
- Account
- AccountsApplicationIntegrationTest.kt
- OutboxRelayConfigurationSupportTest.kt
- accounts/adapters/web/ProblemDetailsExceptionHandler.kt
- OutboxRelayIntegrationTest.kt
- OutboxRelay
- LedgerEntry
- JdbcRepositoriesIntegrationTest
- OutboxRelayTest
- OverdraftLimit
- Decisões tomadas
- README.md
- JdbcProcessedEventRepositoryIntegrationTest.kt
- SnsEventPublisherIntegrationTest.kt
- TransferLeaseContentionIntegrationTest.kt
- coordenador.md
- 0004 — API Gateway HTTP API na borda
- 0005 — Frontend Angular e BFF em NestJS
- 0006 — Observabilidade só com correlation ID
- Order
- StatementConsumptionIntegrationTest
- Rodando o projeto localmente (macOS)
- AccountController.kt
- 0008 — Extrato (`statement`) com CQRS no DynamoDB
- accounts/bootstrap/UseCaseConfiguration.kt
- HttpAccountsGatewayIntegrationTest.kt
- 0013 — Persistência com Spring JDBC (`JdbcClient`)
- 0012 — Saldo no lançamento e trava pessimista por conta
- NotificationQueues
- InMemoryDailyLimitGateway
- dev-frontend.md
- dev-kotlin.md
- infra.md
- 0003 — Transactional Outbox e mensageria SNS/SQS
- Notification
- 0011 — Contratos de API
- Versões novas da etapa 1
- TransferSagaEndToEndIntegrationTest
- documentador.md
- revisor.md
- 01-create-service-databases.sh
- LocalStackInitScripts
- outbox_stuck_idx
- Money
- notification/adapters/web/ProblemDetailsExceptionHandler.kt
- account
- AvailableBalance
- outbox-table.sql
- V4__outbox.sql
- RecordCreditUseCaseTest
- NotificationIntegrationTest
- JdbcTransferRepositoryIntegrationTest
- SagaPropertiesTest.kt
- NotificationConsumptionIntegrationTest
- ProcessedEventRepository
- smoke.sh
- statement/adapters/web/ProblemDetailsExceptionHandler.kt
- notification/bootstrap/AwsCredentialsConfiguration.kt
- HttpAccountsGateway
- SpringTransactionRunnerIntegrationTest
- 01-accounts-topic.sh
- StatementPage
- OutboxRelayScheduler
- PendingEvent
- HttpAccountsGatewayIntegrationTest
- Foco de revisão
- JdbcOutboxStoreIntegrationTest
- RetryBackoff
- OrderResource
- RunTransferSagaUseCaseTest
- LocalStackFifoIntegrationTest.kt
- LocalStackTestSupport
- AwsClientConfigurationTest.kt
- StatementRepository
- AccountTypeAdminController.kt
- InMemoryTransferRepository
- retryWhileAccountBusy
- TransferEventsIntegrationTest.kt
- NotificationKind
- MarkNotificationsReadUseCaseTest
- EventEnvelopeReaderTest
- StepCall
- OutboxRelayConfigurationSupport.kt
- StatementEntry
- TransferController.kt
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
- RunTransferSagaUseCase
- AccountsProxy.kt
- NotificationApiIntegrationTest
- StepRecord
- DynamoDbStatementRepositoryIntegrationTest
- ResumeDueTransfersUseCaseTest
- Money
- JdbcNotificationRepositoryIntegrationTest
- assertOutboxSchemaMatchesModel
- ServiceApiClients.kt
- ConcurrencyIntegrationTest
- ChangeAccountTypeUseCaseTest
- .resumeDueOver
- ResumeDueTransfersUseCase
- RedisDailyLimitGatewayIntegrationTest
- AccountsResponseClassifierTest
- JdbcOutboxWriter
- EndToEndEnvironment
- StatementIntegrationTest
- TransferMetricsIntegrationTest.kt
- SnsEventPublisherTest.kt
- OutboxRelayUnreachableBrokerIntegrationTest.kt
- JdbcTransferRepository
- AccountTypeAdminApiIntegrationTest
- TransferSagaScheduler
- Review Focus
- .get
- TransferMetricsConfiguration.kt
- .container
- ChangeAccountTypeUseCaseTest
- clock
- ServiceProcess
- Transfer.kt
- StepOutcome
- SchedulerLeaseOwners
- uuid
- EndToEndInputs
- .run
- Balance
- StatementCacheIntegrationTest.kt
- ConsumeEventUseCaseTest
- TransferStep
- SagaTimeoutsValidatorTest.kt
- OutboxWriterClockIntegrationTest.kt
- AccountsClientConfiguration
- MdcCorrelationContextTest.kt
- TransferRepository
- AccountRequestDTOs.kt
- LocalStackInitScripts
- JdbcRepositoriesIntegrationTest.kt
- TransferEvent
- ListNotificationsUseCaseTest
- 02-transfers-topic.sh
- OpenApiIntegrationTest
- accounts/adapters/web/OpenApiConfiguration.kt
- transfer
- 0001 — Stack: Kotlin + Spring Boot 4 na AWS
- GetStatementUseCaseTest
- CacheConfigurationTest.kt
- 03-notification-queues.sh
- V3__outbox.sql
- accounts/bootstrap/web/UnexpectedErrorApiIntegrationTest.kt
- statement/bootstrap/DlqMetricsConfiguration.kt
- TransferSchemaIntegrationTest
- NotificationController.kt
- NotificationEventListenerTest
- 0014 — Tipo de conta e cheque especial
- NotificationText
- notification/adapters/web/CorrelationIdFilter.kt
- 0007 — Conta e ledger no mesmo serviço (`accounts`)
- TestListeners
- transfers/adapters/web/CorrelationIdFilter.kt
- OpenApiIntegrationTest
- status.md
- SpringTransactionRunner
- accounts/adapters/web/CorrelationIdFilter.kt
- CreditStepTest
- DailyLimitSagaIntegrationTest.kt
- .decode
- TransferStatus
- TransactionRunner
- AccountsIntegrationTest
- StatementEntryTest
- 0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS
- RecordDebitUseCaseTest
- RecordDebitUseCaseTest.kt
- TransfersIntegrationTest
- OpenAccountUseCase
- .brl
- 04-statement.sh
- BlockingRepository
- RedisStatementFirstPageCacheIntegrationTest
- Etapa 0 — Fundação: design
- Etapa 6 — limite diário: design
- statement/adapters/web/CorrelationIdFilter.kt
- ResolveDebitStepTest
- ConsumeEventUseCase
- Frente B — débitos técnicos
- notification/bootstrap/DlqMetricsConfiguration.kt
- DataCentsMigrationIntegrationTest.kt
- AccountsClientProperties
- DynamoDbConfiguration.kt
- DebitStepTest
- Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)
- RetryBackoffTest
- .get
- DlqDepthMonitorTest
- OutboxSchemaAssertions.kt
- ReserveLimitStepTest
- loggerfactory
- StatementEntry
- Status do projeto
- DailyLimitController.kt
- InMemoryTransferRepositoryTest
- CompensateStepTest
- 0016 — Saga de transferência orquestrada por HTTP síncrono
- V1__notification_schema.sql
- StatementEndToEndIntegrationTest
- Handler
- runapplication
- statement/bootstrap/web/UnexpectedErrorApiIntegrationTest.kt
- .record
- 0018 — Extrato ordenado pela sequência do ledger
- Bloco 1 — `accounts` gravando no outbox
- AccountTypeTest.kt
- SchedulingConfiguration.kt
- EntryKindTest.kt
- notification/bootstrap/SchedulingConfiguration.kt
- OutboxRelayConfigurationSupportTest
- Etapa 5 — cache do extrato: design
- EventEnvelopeReader
- GetDailyLimitUseCase
- DailyLimitEndToEndIntegrationTest.kt
- Etapa 2 — `transfers` + saga: design
- assertProblem
- OutboxMetrics
- RedisStatementFirstPageCache
- DynamoDbStatementRepositoryTest.kt
- SagaIntegrationSupport.kt
- 0002 — Clean Architecture por serviço
- OpenApiIntegrationTest
- StatementCacheProperties
- bootstrap/OutboxSchemaIntegrationTest.kt
- CacheConfiguration.kt
- assertProblem
- ProcessLogsOnFailureExtension
- persistence/OutboxSchemaIntegrationTest.kt
- OrderController.kt
- EntryKind
- V6BackfillIntegrationTest.kt
- JdbcTransferStepLogIntegrationTest.kt
- Frente A — `notification`
- TransfersApplicationIntegrationTest.kt
- RedisStatementFirstPageCacheTest.kt
- MockitoAgentArgumentProvider
- JdbcKnownAccountRepositoryIntegrationTest.kt
- OpenApiIntegrationTest
- AccountsApplication.kt
- .outbox
- .accounts
- 0019 — Runtime na AWS com EKS
- 0009 — Cache do extrato
- TransactionRunner
- InMemoryAccountRepositoryTest
- FakeAccountsGatewayTest
- GetTransferUseCaseTest
- QueueProperties
- QueueProperties

## God Nodes (most connected - your core abstractions)
1. `Transfer` - 115 edges
2. `Account` - 70 edges
3. `Money` - 66 edges
4. `OverdraftLimit` - 64 edges
5. `AccountApiIntegrationTest` - 63 edges
6. `LedgerEntry` - 63 edges
7. `TransferStep` - 58 edges
8. `Balance` - 53 edges
9. `JdbcRepositoriesIntegrationTest` - 50 edges
10. `RunTransferSagaUseCaseTest` - 50 edges

## Surprising Connections (you probably didn't know these)
- `Mensageria` --references--> `OutboxRelay`  [INFERRED]
  docs/arquitetura.md → libs/outbox-relay/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/relay/OutboxRelay.kt
- `Task 14: Agendador, relay e eventos no `transfers` (Marina, `dev-kotlin`)` --references--> `OutboxRelayScheduler`  [INFERRED]
  docs/superpowers/plans/2026-10-27-etapa-2-transfers.md → libs/outbox-relay/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/relay/spring/OutboxRelayScheduler.kt
- `Task 7: `libs/outbox` — envelope e writer (Marie, `dev-kotlin`; em paralelo às Tasks 2–6)` --references--> `OutboxEventEnvelope`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/OutboxEventEnvelope.kt
- `6. Eventos consumidos e mudança no `accounts`` --references--> `OutboxEventEnvelope`  [INFERRED]
  docs/superpowers/specs/2026-10-28-etapa-3-mensageria-design.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/OutboxEventEnvelope.kt
- `Task 3: Regras ArchUnit compartilhadas (Marina, `dev-kotlin`) — cartão #32` --references--> `ServiceArchitectureRulesTest`  [INFERRED]
  docs/superpowers/plans/2026-10-27-etapa-2-transfers.md → libs/testing-architecture/src/test/kotlin/io/github/capozziv/digitalbanking/testing/architecture/ServiceArchitectureRulesTest.kt

## Import Cycles
- None detected.

## Communities (318 total, 194 thin omitted)

### Community 5 - "RedisDailyLimitGateway"
Cohesion: 0.06
Nodes (10): DailyLimitConfiguration, DailyLimitProperties, DailyLimitScripts, JdbcDailyLimitStore, Outcome, EXCEEDED, FALLBACK_RESERVED, RESERVED (+2 more)

### Community 6 - "ServiceArchitectureRulesTest"
Cohesion: 0.06
Nodes (6): ServiceArchitectureRules, ServiceArchitectureRulesTest, ArchitectureTest, ArchitectureTest, ArchitectureTest, ArchitectureTest

### Community 8 - "Account"
Cohesion: 0.09
Nodes (8): Task 8: Esquema, usuários de banco e repositórios JDBC (Marina, `dev-kotlin`; o script de init do compose segue o padrão do Rafael), JdbcAccountRepository, FindEntryByIdempotencyKeyUseCaseTest, Account, DebitResult, EntryRecorded, Recorded, Refused

### Community 9 - "AccountsApplicationIntegrationTest.kt"
Cohesion: 0.16
Nodes (5): Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`), AccountsApplicationIntegrationTest, ParameterizedTypeReference, RequestThreadProbe, RequestThreadProbeConfiguration

### Community 11 - "accounts/adapters/web/ProblemDetailsExceptionHandler.kt"
Cohesion: 0.10
Nodes (8): IdempotencyKeyHeader, ProblemDetailsExceptionHandler, AccountTypeNotFoundException, EntryNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException, BalanceOverflowException

### Community 12 - "OutboxRelayIntegrationTest.kt"
Cohesion: 0.09
Nodes (3): OutboxRelayIntegrationTest, ReceivedEvent, RecordingPublisherConfiguration

### Community 13 - "OutboxRelay"
Cohesion: 0.22
Nodes (6): 0015 — Relay do outbox com líder único e SNS FIFO, Alternativas consideradas, Consequências, Contexto, Decisão, OutboxRelay

### Community 14 - "LedgerEntry"
Cohesion: 0.15
Nodes (4): JdbcLedgerEntryRepository, LedgerEntryRepository, InMemoryLedgerEntryRepository, LedgerEntry

### Community 16 - "OutboxRelayTest"
Cohesion: 0.16
Nodes (3): FakeEventPublisher, MutableClock, OutboxRelayTest

### Community 17 - "OverdraftLimit"
Cohesion: 0.07
Nodes (18): Global Constraints, 2. Modelo de domínio, JdbcAccountTypeRepository, AccountTypeRepository, AccountTypeRepository, ChangeOverdraftLimitUseCaseTest, ListAccountTypesUseCaseTest, AccountTypeRepository (+10 more)

### Community 18 - "Decisões tomadas"
Cohesion: 0.13
Nodes (15): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Em aberto, Fluxo de entrada (+7 more)

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
Cohesion: 0.11
Nodes (18): 1. Configurar o ambiente, 2. Subir a infraestrutura (PostgreSQL, Redis, nginx, LocalStack), 3. Build e testes, 4. Subir os serviços `accounts`, `transfers`, `notification` e `statement`, 5. Conferir a API pelo nginx, 6. Ver os eventos na fila de inspeção, 7. Coleção Bruno, 8. Derrubar tudo (+10 more)

### Community 30 - "AccountController.kt"
Cohesion: 0.18
Nodes (4): AccountController, AccountDTO, LedgerEntryDTO, FindEntryByIdempotencyKeyUseCase

### Community 31 - "0008 — Extrato (`statement`) com CQRS no DynamoDB"
Cohesion: 0.50
Nodes (4): 0008 — Extrato (`statement`) com CQRS no DynamoDB, Consequências, Contexto, Decisão

### Community 32 - "accounts/bootstrap/UseCaseConfiguration.kt"
Cohesion: 0.09
Nodes (6): 1. Módulos e camadas, 4. Fluxos, ChangeAccountTypeUseCase, GetAccountUseCase, ListAccountTypesUseCase, RecordDebitUseCase

### Community 34 - "0013 — Persistência com Spring JDBC (`JdbcClient`)"
Cohesion: 0.40
Nodes (5): 0013 — Persistência com Spring JDBC (`JdbcClient`), Alternativas consideradas, Consequências, Contexto, Decisão

### Community 35 - "0012 — Saldo no lançamento e trava pessimista por conta"
Cohesion: 0.40
Nodes (5): 0012 — Saldo no lançamento e trava pessimista por conta, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 36 - "NotificationQueues"
Cohesion: 0.12
Nodes (5): DeadLetter, NotificationQueues, PublishedMessage, ReceivedEvent, TransferEventsQueue

### Community 37 - "InMemoryDailyLimitGateway"
Cohesion: 0.13
Nodes (8): DailyLimitGateway, DailyLimitUsage, ReservationResult, EXCEEDED, RESERVED, UNAVAILABLE, InMemoryDailyLimitGateway, Reservation

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

### Community 42 - "Notification"
Cohesion: 0.05
Nodes (29): Banco digital, Clientes e contas, Consulta e avisos, Ledger, Task A3: Casos de uso e portas (Marina, `dev-kotlin`), Task A6: API das notificações (Marina, `dev-kotlin`) — paralela à A5, 2. Módulos e camadas, 3. Modelo de domínio (+21 more)

### Community 43 - "0011 — Contratos de API"
Cohesion: 0.50
Nodes (4): 0011 — Contratos de API, Consequências, Contexto, Decisão

### Community 44 - "Versões novas da etapa 1"
Cohesion: 0.50
Nodes (3): Observações, springdoc e Spring Boot 4.1.x, Versões novas da etapa 1

### Community 61 - "Money"
Cohesion: 0.18
Nodes (4): OutboxEventAppenderIntegrationTest, OutboxRow, Money, MoneyTest

### Community 63 - "account"
Cohesion: 0.33
Nodes (3): account_type, account, ledger_entry

### Community 65 - "outbox-table.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 69 - "RecordCreditUseCaseTest"
Cohesion: 0.15
Nodes (7): RecordCreditUseCase, DebitOutcome, Recorded, Refused, RecordEntryCommand, RecordEntryResult, RecordCreditUseCaseTest

### Community 71 - "JdbcTransferRepositoryIntegrationTest"
Cohesion: 0.17
Nodes (3): JdbcTransferRepositoryIntegrationTest, aTransfer(), microsNow()

### Community 73 - "NotificationConsumptionIntegrationTest"
Cohesion: 0.10
Nodes (3): NotificationConsumptionIntegrationTest, NotificationTestPropertiesSupport, NotificationApplication

### Community 74 - "ProcessedEventRepository"
Cohesion: 0.17
Nodes (3): JdbcKnownAccountRepository, KnownAccountRepository, InMemoryKnownAccountRepository

### Community 75 - "smoke.sh"
Cohesion: 0.62
Nodes (6): aws_local(), call(), check_events(), check_notification(), smoke.sh script, uuid()

### Community 78 - "HttpAccountsGateway"
Cohesion: 0.15
Nodes (7): Task 12: Cliente HTTP do `accounts` (Marina, `dev-kotlin`), AccountsAccountResponseDTO, AccountsEntryRequestDTO, AccountsEntryResponseDTO, AccountsProblemDTO, HttpAccountsGateway, RawResponse

### Community 80 - "01-accounts-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 01-accounts-topic.sh script

### Community 81 - "StatementPage"
Cohesion: 0.09
Nodes (16): Etapa 5 — cache do extrato: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Revisão final da branch (Beatriz), Rulings deste plano, Task 1: porta do cache e casos de uso, Task 2: adapter Redis (+8 more)

### Community 82 - "OutboxRelayScheduler"
Cohesion: 0.11
Nodes (3): OutboxRelayScheduler, BlockingStore, OutboxRelaySchedulerTest

### Community 83 - "PendingEvent"
Cohesion: 0.08
Nodes (15): Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), Bloco 1 — preparação para o 2º serviço (branch `etapa-2-transfers`, PR 1), Task 1: Gradle para vários serviços (Rafael, `infra`) — cartão #37, Task 2: Relay em módulo próprio e teste de esquema (Marina, `dev-kotlin`) — cartões #25 e #30, Task 3: Regras ArchUnit compartilhadas (Marina, `dev-kotlin`) — cartão #32, Task 4: Consulta de lançamento pela chave no `accounts` (Marina, `dev-kotlin`), Task 5: PostgreSQL com um banco por serviço (Rafael, `infra`) — cartão #38, Task 6: Build seletivo no CI (Rafael, `infra`) (+7 more)

### Community 85 - "Foco de revisão"
Cohesion: 0.22
Nodes (9): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 4: Ambiente local (docker-compose), Task 5: CI no GitHub Actions (+1 more)

### Community 87 - "JdbcOutboxStoreIntegrationTest"
Cohesion: 0.06
Nodes (12): JdbcOutboxStoreIntegrationTest, UuidV7IdGenerator, UuidV7IdGenerator, UuidV7IdGenerator, IdempotencyKeyReusedException, IdGenerator, AccountNotFound, AccountsUnavailable (+4 more)

### Community 89 - "OrderResource"
Cohesion: 0.27
Nodes (5): PersistenceUsesWeb, OrderResource, WebUsesPersistence, ApplicationUsesAdapters, DomainUsesAdapters

### Community 92 - "LocalStackTestSupport"
Cohesion: 0.20
Nodes (3): Bloco 2 — relay e SNS FIFO, Task 14: Verificação do SNS FIFO no LocalStack (Rafael, `infra`), LocalStackTestSupport

### Community 94 - "StatementRepository"
Cohesion: 0.11
Nodes (13): Task 5: Consumo SQS e métrica da DLQ (Marina, `dev-kotlin`; paralela à Task 6), 2. Módulos e camadas, 5. Fluxo de consumo, 1. Porta e casos de uso, StatementEventListener, ConflictingStatementEntryException, StatementRepository, RecordOutcome (+5 more)

### Community 97 - "retryWhileAccountBusy"
Cohesion: 0.21
Nodes (3): isAccountBusy(), retryWhileAccountBusy(), RetryWhileAccountBusyTest

### Community 98 - "TransferEventsIntegrationTest.kt"
Cohesion: 0.12
Nodes (3): ReceivedEvent, SentTransfer, TransferEventsIntegrationTest

### Community 99 - "NotificationKind"
Cohesion: 0.12
Nodes (16): 10. Ambiente local e CI, 11. Documentação que acompanha, 1. Frentes de trabalho, 4. Fluxo de consumo, 5. Esquema do banco (`notification`), 6. Eventos consumidos e mudança no `accounts`, 7. API, nginx e Bruno, 8. Operação (+8 more)

### Community 101 - "EventEnvelopeReaderTest"
Cohesion: 0.14
Nodes (8): EventEnvelopeReaderTest, AccountOpened, AccountTypeChanged, ReceivedEvent, TransferCompensated, TransferCompleted, TransferFailed, Unsupported

### Community 102 - "StepCall"
Cohesion: 0.09
Nodes (15): AccountLookup, EXISTS, NOT_FOUND, UNAVAILABLE, AccountsGateway, CallResult, Run, FakeAccountsGateway (+7 more)

### Community 103 - "OutboxRelayConfigurationSupport.kt"
Cohesion: 0.10
Nodes (8): Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), OutboxRelaySettings, AwsProperties, OutboxRelayConfigurationSupport, OutboxRelayProperties, OutboxRelaySettingsTest, OutboxRelayConfiguration, OutboxRelayConfiguration

### Community 105 - "StatementEntry"
Cohesion: 0.08
Nodes (19): Task 3: Domínio e casos de uso (Marina, `dev-kotlin`), 10. Documentação que acompanha, 1. Frentes de trabalho, 3. Modelo de domínio, 4. Tabela no DynamoDB, 6. API, nginx e Bruno, 7. Operação, 8. Testes (+11 more)

### Community 106 - "TransferController.kt"
Cohesion: 0.05
Nodes (15): RequestTransferDTO, TransferDTO, IdempotencyKeyHeader, ProblemDetailsExceptionHandler, TransferController, AccountsUnavailableException, InvalidField, InvalidRequestException (+7 more)

### Community 128 - "RunTransferSagaUseCase"
Cohesion: 0.10
Nodes (7): 2. Módulos e camadas, RequestTransferUseCase, RunTransferSagaUseCase, SagaSettings, RequestTransferUseCaseTest, TransferRepository, TransferRepository

### Community 129 - "AccountsProxy.kt"
Cohesion: 0.09
Nodes (3): AccountsProxy, HeldRequest, Hold

### Community 131 - "StepRecord"
Cohesion: 0.21
Nodes (5): Task 9: Portas e execução da saga (Marina, `dev-kotlin`), JdbcTransferStepLog, StepRecord, TransferStepLog, InMemoryTransferStepLog

### Community 134 - "Money"
Cohesion: 0.20
Nodes (3): Money, AccountTest, MoneyTest

### Community 135 - "JdbcNotificationRepositoryIntegrationTest"
Cohesion: 0.25
Nodes (3): JdbcNotificationRepositoryIntegrationTest, aNotification(), microsNow()

### Community 137 - "ServiceApiClients.kt"
Cohesion: 0.15
Nodes (6): AccountsApiClient, HttpResult, NotificationApiClient, send(), StatementApiClient, TransfersApiClient

### Community 140 - ".resumeDueOver"
Cohesion: 0.10
Nodes (5): CorrelationContext, DailyLimitGateway, EventOutbox, TransactionRunner, TransferStepLog

### Community 141 - "ResumeDueTransfersUseCase"
Cohesion: 0.17
Nodes (6): Outcome, INTERRUPTED, SKIPPED, TAKEN, ResumeDueTransfersUseCase, TransferSagaSchedulerConfiguration

### Community 142 - "RedisDailyLimitGatewayIntegrationTest"
Cohesion: 0.06
Nodes (9): Ordem e paralelismo, Task 1: `libs/testing-redis` e ambiente, Task 3: adapter Redis + Lua, trava, epoch e fallback, Task 4: endpoint, e2e, smoke e Bruno, RedisTestSupportIntegrationTest, RedisTestSupport, RedisDailyLimitGatewayIntegrationTest, Lease (+1 more)

### Community 144 - "JdbcOutboxWriter"
Cohesion: 0.09
Nodes (17): 10. Documentação que acompanha, 3. Esquema do banco, 5. Outbox e relay (`libs/outbox`), 6. API, 7. Testes, 8. Ambiente local e CI, 9. Entrega em dois blocos, Critério de pronto (+9 more)

### Community 146 - "StatementIntegrationTest"
Cohesion: 0.11
Nodes (3): LocalStackTestSupportIntegrationTest, StatementIntegrationTest, StatementWebIntegrationTest

### Community 153 - "Review Focus"
Cohesion: 0.22
Nodes (9): Etapa 4 — `statement`: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Task 1: Esqueleto Gradle e cliente DynamoDB no apoio de testes (Rafael, `infra`), Task 2: LocalStack, compose, nginx e CI (Rafael, `infra`), Task 4: Repositório DynamoDB e bootstrap (Marina, `dev-kotlin`), Task 7: e2e, smoke e Bruno (Marina, `dev-kotlin`) (+1 more)

### Community 154 - ".get"
Cohesion: 0.15
Nodes (7): AccountOpened, EventEnvelopeReader, Ignored, LedgerEntryRecorded, MalformedEventException, ReceivedEvent, EventEnvelopeReaderTest

### Community 155 - "TransferMetricsConfiguration.kt"
Cohesion: 0.26
Nodes (4): JdbcPendingTransferStats, PendingTransfers, TransferMetrics, TransferMetricsConfiguration

### Community 158 - "clock"
Cohesion: 0.09
Nodes (3): OutboxEventEnvelope, ClockWiringIntegrationTest, JdbcProcessedEventRepository

### Community 161 - "StepOutcome"
Cohesion: 0.13
Nodes (12): Etapa 6 — limite diário: plano de implementação, Global Constraints, Review Focus, Revisão final da branch (Beatriz), Rulings deste plano, Task 2: domínio, saga e migração, 4. Fluxos, AccountsResponseClassifier (+4 more)

### Community 163 - "uuid"
Cohesion: 0.05
Nodes (9): InMemoryProcessedEventRepository, SequentialIdGenerator, NotificationTest, EntryKind, CREDIT, DEBIT, RetryBackoff, MutableClock (+1 more)

### Community 170 - "Balance"
Cohesion: 0.16
Nodes (3): Balance, BalanceTest, LedgerEntryTest

### Community 173 - "TransferStep"
Cohesion: 0.10
Nodes (15): Task 8: Domínio da transferência e passos da saga (Marina, `dev-kotlin`), 3. Modelo de domínio, Riscos, Transfer, Compensate, Compensated, Completed, Credit (+7 more)

### Community 177 - "MdcCorrelationContextTest.kt"
Cohesion: 0.12
Nodes (4): MdcCorrelationContext, MdcCorrelationContextTest, CorrelationContext, RecordingCorrelationContext

### Community 179 - "AccountRequestDTOs.kt"
Cohesion: 0.33
Nodes (4): ChangeAccountTypeDTO, ChangeOverdraftLimitDTO, OpenAccountDTO, RecordEntryDTO

### Community 181 - "JdbcRepositoriesIntegrationTest.kt"
Cohesion: 0.08
Nodes (11): AccountRepository, InsertAccountResult, DUPLICATE_CUSTOMER, DUPLICATE_OPENING_KEY, INSERTED, EventOutbox, IdGenerator, TransactionRunner (+3 more)

### Community 182 - "TransferEvent"
Cohesion: 0.06
Nodes (10): JdbcOutboxWriterIntegrationTest, OutboxMessage, OutboxEventAppender, OutboxEventAppender, EventOutbox, FakeEventOutbox, TransferCompensated, TransferCompleted (+2 more)

### Community 184 - "02-transfers-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 02-transfers-topic.sh script

### Community 186 - "accounts/adapters/web/OpenApiConfiguration.kt"
Cohesion: 0.09
Nodes (8): ApiDocs, OpenApiConfiguration, ApiDocs, OpenApiConfiguration, ApiDocs, OpenApiConfiguration, ApiDocs, OpenApiConfiguration

### Community 187 - "transfer"
Cohesion: 0.28
Nodes (6): transfer, transfer_due_idx, transfer_step, transfer_step_transfer_idx, daily_limit_epoch, transfer_source_limit_day_idx

### Community 188 - "0001 — Stack: Kotlin + Spring Boot 4 na AWS"
Cohesion: 0.40
Nodes (5): 0001 — Stack: Kotlin + Spring Boot 4 na AWS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 189 - "GetStatementUseCaseTest"
Cohesion: 0.14
Nodes (4): statementEntry(), GetStatementUseCaseTest, RecordStatementEntryUseCaseTest, StatementCursor

### Community 191 - "03-notification-queues.sh"
Cohesion: 0.39
Nodes (7): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, create_consumer_queue(), queue_arn(), 03-notification-queues.sh script

### Community 192 - "V3__outbox.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 200 - "NotificationController.kt"
Cohesion: 0.12
Nodes (8): MarkedNotificationsDTO, MarkNotificationsReadDTO, NotificationDTO, NotificationPageDTO, NotificationController, NotificationCursorCodec, InvalidField, InvalidRequestException

### Community 202 - "0014 — Tipo de conta e cheque especial"
Cohesion: 0.40
Nodes (5): 0014 — Tipo de conta e cheque especial, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 203 - "NotificationText"
Cohesion: 0.19
Nodes (3): NotificationContent, NotificationText, NotificationTextTest

### Community 205 - "0007 — Conta e ledger no mesmo serviço (`accounts`)"
Cohesion: 0.50
Nodes (4): 0007 — Conta e ledger no mesmo serviço (`accounts`), Consequências, Contexto, Decisão

### Community 209 - "status.md"
Cohesion: 0.12
Nodes (6): 0010 — Limite diário com Redis + Lua, Alternativas consideradas, Consequências, Contexto, Decisão, Roadmap

### Community 214 - ".decode"
Cohesion: 0.27
Nodes (4): Task 6: API do extrato (Marina, `dev-kotlin`; paralela à Task 5), InvalidCursorException, StatementCursorCodec, StatementCursorCodecTest

### Community 215 - "TransferStatus"
Cohesion: 0.20
Nodes (10): Transferências, Etapa 2 — `transfers` + saga Implementation Plan, Global Constraints, Review Focus, 10. Documentação que acompanha, TransferStatus, COMPENSATED, COMPLETED (+2 more)

### Community 217 - "AccountsIntegrationTest"
Cohesion: 0.15
Nodes (3): AccountsIntegrationTest, UpdateGrantsIntegrationTest, UseCaseConfigurationIntegrationTest

### Community 220 - "0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS"
Cohesion: 0.40
Nodes (5): 0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 223 - "RecordDebitUseCaseTest.kt"
Cohesion: 0.08
Nodes (20): Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`), AccountBusyException, AccountNotFoundException, CustomerAlreadyHasAccountException, IdempotencyKeyReusedException, AccountView, OpenAccountCommand, OpenAccountResult (+12 more)

### Community 225 - "OpenAccountUseCase"
Cohesion: 0.12
Nodes (11): Agentes do projeto, Ao terminar uma tarefa, Autonomia, Banco digital em microsserviços, Contexto, Fluxo de trabalho (skills), Git, Grafo do código (graphify) (+3 more)

### Community 227 - "04-statement.sh"
Cohesion: 0.43
Nodes (6): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, queue_arn(), 04-statement.sh script

### Community 230 - "Etapa 0 — Fundação: design"
Cohesion: 0.20
Nodes (10): 1. Estrutura do monorepo, 2. Build e convenções, 4. Ambiente local, 5. CI (GitHub Actions), Critério de pronto, Etapa 0 — Fundação: design, Fora de escopo (e onde foi registrado), Objetivo (+2 more)

### Community 231 - "Etapa 6 — limite diário: design"
Cohesion: 0.13
Nodes (15): 1. Fonte da verdade no PostgreSQL, 2. Contador no Redis (script Lua), 3. Passo `ReserveLimit` da saga, 4. Trava única e fallback no PostgreSQL, 5. Endpoint de consulta, 6. Observabilidade, 7. Testes, 8. Ambiente local, CI e documentação (+7 more)

### Community 234 - "ConsumeEventUseCase"
Cohesion: 0.26
Nodes (5): Etapa 3 — Mensageria e `notification` Implementation Plan, Frentes e ordem, Review Focus, ConsumeEventUseCase, Draft

### Community 235 - "Frente B — débitos técnicos"
Cohesion: 0.29
Nodes (6): Frente B — débitos técnicos, Task B1: Configuração do relay em `libs/outbox-relay` (Marina, `dev-kotlin`) — cartão #92, Task B2: Backoff do relay e testes (Marina, `dev-kotlin`) — cartões #24 e #26, depois da B1, Task B3: Domínio e persistência do `transfers` (Marina, `dev-kotlin`) — cartões #95 e #93, Task B4: Aplicação e adapters do `transfers` (Marina, `dev-kotlin`) — cartão #94, depois da B3, Task B5: Endurecer os testes ponta a ponta (Marina, `dev-kotlin`) — cartão #96, antes da A9

### Community 236 - "notification/bootstrap/DlqMetricsConfiguration.kt"
Cohesion: 0.23
Nodes (3): DlqDepthMonitor, DlqDepthProbe, DlqMetricsConfiguration

### Community 241 - "Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)"
Cohesion: 0.29
Nodes (7): Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2), Task 10: Pedir, consultar e retomar transferências (Marina, `dev-kotlin`), Task 11: Persistência do `transfers` (Marina, `dev-kotlin`), Task 13: API HTTP do `transfers` (Marina, `dev-kotlin`), Task 14: Agendador, relay e eventos no `transfers` (Marina, `dev-kotlin`), Task 15: Infra local, Bruno, smoke e CI do `transfers` (Rafael, `infra`), Task 7: Esqueleto do `transfers` (Marina, `dev-kotlin`)

### Community 244 - ".get"
Cohesion: 0.15
Nodes (5): StatementDTO, StatementEntryDTO, StatementController, InvalidField, InvalidRequestException

### Community 248 - "OutboxSchemaAssertions.kt"
Cohesion: 0.46
Nodes (3): createModelSchema(), currentSchema(), OutboxSchemaSnapshot

### Community 250 - "loggerfactory"
Cohesion: 0.20
Nodes (5): NotificationEventListener, ConsumeOutcome, DUPLICATE, IGNORED, PROCESSED

### Community 252 - "Status do projeto"
Cohesion: 0.33
Nodes (6): Ambiente do Claude, Como rodar localmente, Como usar o board, Concluído, Convenções que já custaram caro, Status do projeto

### Community 256 - "0016 — Saga de transferência orquestrada por HTTP síncrono"
Cohesion: 0.40
Nodes (5): 0016 — Saga de transferência orquestrada por HTTP síncrono, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 257 - "V1__notification_schema.sql"
Cohesion: 0.50
Nodes (4): known_accounts, notifications, notifications_customer_page_idx, processed_events

### Community 263 - "0018 — Extrato ordenado pela sequência do ledger"
Cohesion: 0.40
Nodes (5): 0018 — Extrato ordenado pela sequência do ledger, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 264 - "Bloco 1 — `accounts` gravando no outbox"
Cohesion: 0.13
Nodes (12): Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 13: Documentação do bloco 1 e das decisões da etapa (Clara, `documentador`; em paralelo às Tasks 8–12), Task 2: Valores monetários e tipos (Marina, `dev-kotlin`), Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`), Task 5: Casos de uso de crédito e débito (Marina, `dev-kotlin`) (+4 more)

### Community 271 - "Etapa 5 — cache do extrato: design"
Cohesion: 0.14
Nodes (14): 2. Adapter Redis, 3. Consistência, 4. Observabilidade, 5. Configuração, 6. Testes, 7. Ambiente local e CI, 8. Documentação que acompanha, Critério de pronto (+6 more)

### Community 273 - "GetDailyLimitUseCase"
Cohesion: 0.23
Nodes (3): GetDailyLimitUseCase, GetDailyLimitUseCaseTest, DailyLimitDayTest

### Community 275 - "Etapa 2 — `transfers` + saga: design"
Cohesion: 0.18
Nodes (11): 5. Esquema do banco (`transfers`), 6. Eventos, 7. API, nginx e Bruno, 8. Testes, 9. Ambiente local e CI, Critério de pronto, Decisões desta etapa (usuário, 2026-10-27), Etapa 2 — `transfers` + saga: design (+3 more)

### Community 277 - "OutboxMetrics"
Cohesion: 0.10
Nodes (4): OutboxMetrics, OutboxBacklog, OutboxMetricsTest, FakeOutboxStore

### Community 281 - "0002 — Clean Architecture por serviço"
Cohesion: 0.40
Nodes (5): 0002 — Clean Architecture por serviço, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 290 - "OrderController.kt"
Cohesion: 0.25
Nodes (4): OrderController, PlaceOrderCommand, PlaceOrderResult, PlaceOrderUseCase

### Community 296 - "Frente A — `notification`"
Cohesion: 0.22
Nodes (9): Frente A — `notification`, Task A10: Documentação (Clara, `documentador`) — paralela desde o início, Task A1: Esqueleto do `notification` e prova do `@SqsListener` (Marina, `dev-kotlin`), Task A2: Domínio das notificações (Marina, `dev-kotlin`), Task A4: Persistência (Marina, `dev-kotlin`), Task A5: Consumo das filas (Marina, `dev-kotlin`) — paralela à A6, Task A7: `AccountTypeChanged` versão 2 no `accounts` (Marina, `dev-kotlin`) — paralela desde o início, Task A8: Infra local, Bruno, smoke e CI do `notification` (Rafael, `infra`) (+1 more)

### Community 302 - "AccountsApplication.kt"
Cohesion: 0.33
Nodes (4): Task 3: Aplicação `accounts` com health do banco (Testcontainers), 3. Esqueleto do `accounts`, Testes (no `bootstrap`, que enxerga todos os módulos), AccountsApplication

### Community 303 - ".outbox"
Cohesion: 0.33
Nodes (4): Etapa 1 — `accounts` Implementation Plan, Global Constraints, Review Focus, EventOutbox

### Community 305 - "0019 — Runtime na AWS com EKS"
Cohesion: 0.40
Nodes (5): 0019 — Runtime na AWS com EKS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 306 - "0009 — Cache do extrato"
Cohesion: 0.40
Nodes (5): 0009 — Cache do extrato, Alternativas consideradas, Consequências, Contexto, Decisão

## Knowledge Gaps
- **56 isolated node(s):** `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` (+51 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 803 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **194 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Transfer` connect `TransferStep` to `RunTransferSagaUseCase`, `ResumeDueTransfersUseCaseTest`, `.transfer`, `OverdraftLimit`, `Etapa 2 — `transfers` + saga: design`, `TransferMetricsIntegrationTest.kt`, `JdbcTransferRepository`, `clock`, `Transfer.kt`, `StepOutcome`, `uuid`, `.run`, `TransferRepository`, `JdbcTransferRepositoryIntegrationTest`, `SpringTransactionRunnerIntegrationTest`, `TransferStatus`, `JdbcOutboxStoreIntegrationTest`, `RunTransferSagaUseCaseTest`, `InMemoryTransferRepository`, `BlockingRepository`, `StepCall`, `TransferController.kt`, `Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)`?**
  _High betweenness centrality (0.068) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Transfer` (e.g. with `Transferências` and `Global Constraints`) actually correct?**
  _`Transfer` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION` to the rest of the system?**
  _56 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AccountApiIntegrationTest` be split into smaller, more focused modules?**
  _Cohesion score 0.11201923076923077 - nodes in this community are weakly interconnected._
- **Why does `Money` connect `Money` to `RunTransferSagaUseCase`, `RedisDailyLimitGateway`, `ResumeDueTransfersUseCaseTest`, `.transfer`, `RedisDailyLimitGatewayIntegrationTest`, `GetDailyLimitUseCase`, `TransferMetricsIntegrationTest.kt`, `JdbcTransferRepository`, `clock`, `HttpAccountsGatewayIntegrationTest.kt`, `uuid`, `InMemoryDailyLimitGateway`, `TransferEvent`, `FakeAccountsGatewayTest`, `GetTransferUseCaseTest`, `JdbcTransferRepositoryIntegrationTest`, `HttpAccountsGatewayIntegrationTest`, `CreditStepTest`, `RunTransferSagaUseCaseTest`, `TransferController.kt`, `DebitStepTest`, `ReserveLimitStepTest`, `InMemoryTransferRepositoryTest`, `CompensateStepTest`?**
  _High betweenness centrality (0.037) - this node is a cross-community bridge._
- **Should `RedisDailyLimitGateway` be split into smaller, more focused modules?**
  _Cohesion score 0.0632996632996633 - nodes in this community are weakly interconnected._
- **Why does `Account` connect `Account` to `OpenAccountUseCase`, `uuid`, `RecordCreditUseCaseTest`, `Money`, `Notification`, `JdbcRepositoriesIntegrationTest`, `.accounts`, `OverdraftLimit`, `InMemoryAccountRepositoryTest`, `JdbcRepositoriesIntegrationTest.kt`, `ChangeAccountTypeUseCaseTest`, `RecordDebitUseCaseTest`, `RecordDebitUseCaseTest.kt`?**
  _High betweenness centrality (0.033) - this node is a cross-community bridge._