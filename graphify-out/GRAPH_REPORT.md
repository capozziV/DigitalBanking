# Graph Report - etapa-7  (2026-11-14)

## Corpus Check
- 660 files · ~193,736 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 59 file(s) not represented in the graph (top: .bru 46, (none) 4, .properties 2)

## Summary
- 5992 nodes · 14739 edges · 369 communities (150 shown, 219 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 1070 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `11b4febf`
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
- SecurityAutoConfigurationIntegrationTest
- TransferAuthorizationIntegrationTest
- SnsEventPublisherIntegrationTest.kt
- TransferLeaseContentionIntegrationTest.kt
- coordenador.md
- 0019-runtime-na-aws-com-eks.md
- 0005 — Frontend Angular e BFF em NestJS
- 0006 — Observabilidade só com correlation ID
- Order
- StatementConsumptionIntegrationTest
- Rodando o projeto localmente (macOS)
- AccountController.kt
- 0008 — Extrato (`statement`) com CQRS no DynamoDB
- RecordDebitUseCase
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
- ServiceTokenClient
- documentador.md
- revisor.md
- 01-create-service-databases.sh
- LocalStackInitScripts
- outbox_stuck_idx
- Money
- notification/adapters/web/ProblemDetailsExceptionHandler.kt
- account
- .adminToken
- outbox-table.sql
- V4__outbox.sql
- RecordDebitUseCaseTest
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
- StatementEntry
- AccountTypeAdminController.kt
- InMemoryTransferRepository
- AccountAuthorizationIntegrationTest
- TransferEventsIntegrationTest.kt
- NotificationKind
- MarkNotificationsReadUseCaseTest
- EventEnvelopeReaderTest
- RunTransferSagaUseCase
- OutboxRelayConfigurationSupport.kt
- InMemoryStatementRepository
- transfers/adapters/web/ProblemDetailsExceptionHandler.kt
- LegacyResource.kt
- PlaceOrderHandler
- LegacyController.kt
- EntryLookupApiIntegrationTest
- ApiClient
- ApplicationWithAws.kt
- CallerResolver
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
- NotificationApiIntegrationTest
- TransferRepository
- DynamoDbStatementRepositoryIntegrationTest
- ResumeDueTransfersUseCaseTest
- Balance
- JdbcNotificationRepositoryIntegrationTest
- assertOutboxSchemaMatchesModel
- ServiceApiClients.kt
- CustomerAdminController.kt
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
- Customer
- .get
- TransferMetricsConfiguration.kt
- .container
- ChangeAccountTypeUseCaseTest.kt
- customer/bootstrap/OutboxRelayIntegrationTest.kt
- ServiceProcess
- Transfer.kt
- StepOutcome
- SchedulerLeaseOwners
- uuid
- EndToEndInputs
- .run
- .entry
- StatementCacheIntegrationTest.kt
- ConsumeEventUseCaseTest
- TransferStep
- SagaTimeoutsValidatorTest.kt
- OutboxMessage
- AccountsClientConfiguration
- MdcCorrelationContextTest.kt
- ApiClient
- CustomerLookup
- LocalStackInitScripts
- CustomerRepository
- RunTransferSagaUseCaseTest.kt
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
- .request
- 0007 — Conta e ledger no mesmo serviço (`accounts`)
- customer/adapters/web/ProblemDetailsExceptionHandler.kt
- ObservabilityAutoConfigurationTest.kt
- OpenApiIntegrationTest
- status.md
- SpringTransactionRunner
- Fakes.kt
- CreditStepTest
- DailyLimitSagaIntegrationTest.kt
- .decode
- TransferStatus
- TransactionRunner
- AccountsIntegrationTest
- StatementEntryTest
- 0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS
- uri
- OpenAccountUseCaseTest.kt
- TransfersIntegrationTest
- Banco digital em microsserviços
- .brl
- 04-statement.sh
- BlockingRepository
- CustomerServiceStub
- Etapa 0 — Fundação: design
- Etapa 6 — limite diário: design
- content
- SecurityProblems.kt
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
- assertProblem
- .record
- 0018 — Extrato ordenado pela sequência do ledger
- CorrelationIdFilter
- AccountTypeTest.kt
- SchedulingConfiguration.kt
- EntryKindTest.kt
- notification/bootstrap/SchedulingConfiguration.kt
- OutboxRelayConfigurationSupportTest
- Etapa 5 — cache do extrato: design
- EventEnvelopeReader
- MutableClock
- DailyLimitEndToEndIntegrationTest.kt
- SecurityTestApplication.kt
- NotificationAccessIntegrationTest
- OutboxMetrics
- RedisStatementFirstPageCache
- DynamoDbStatementRepositoryTest.kt
- SagaIntegrationSupport.kt
- 0002 — Clean Architecture por serviço
- OpenApiIntegrationTest
- StatementCacheProperties
- bootstrap/OutboxSchemaIntegrationTest.kt
- AccountOpeningCustomerIntegrationTest
- SecurityRules
- ProcessLogsOnFailureExtension
- persistence/OutboxSchemaIntegrationTest.kt
- CustomerController.kt
- JwtTestSupport.kt
- V6BackfillIntegrationTest.kt
- DatabaseIntegrationTest.kt
- Frente A — `notification`
- TransfersApplicationIntegrationTest.kt
- AccountOpeningCustomerDownIntegrationTest.kt
- MockitoAgentArgumentProvider
- JdbcKnownAccountRepositoryIntegrationTest.kt
- OpenApiIntegrationTest
- TransferController.kt
- ServiceTokenClientIntegrationTest
- AccountLookup
- 0009 — Cache do extrato
- jsonmapper
- .repeat
- CallerResolverTest
- DigitalBankingSecurityAutoConfiguration.kt
- EventEnvelopeReaderTest
- Ordem e paralelismo
- Etapa 7 — borda e identidade: design
- CorrelationIdRestClientInterceptorTest.kt
- WebExceptionHandling.kt
- HttpCustomerGatewayIntegrationTest
- IdentityEndToEndIntegrationTest.kt
- CallerAuthentication.kt
- accounts/adapters/persistence/UuidV7IdGenerator.kt
- accounts/adapters/customer/CustomerClientConfiguration.kt
- CorrelationIdFilterTest
- TransactionRunner
- CustomerIntegrationTest
- CorrelationIdRestClientInterceptor
- RouteRulesIntegrationTest.kt
- JdbcCustomerRepository
- GetCustomerUseCaseTest
- OpenApiIntegrationTest
- CustomerTest
- .properties
- OutboxRelaySettings
- DigitalBankingSecurityAutoConfigurationTest
- ReceivedEvent
- transfers/adapters/customer/CustomerClientConfiguration.kt
- MutableClock
- InMemoryCustomerRepository
- LocalStackInitScripts
- RequestTransferDTO
- 05-customer-topic.sh
- CallerTest.kt
- customer/adapters/persistence/OutboxWriterConfiguration.kt
- transfers/adapters/persistence/OutboxWriterConfiguration.kt
- 0010 — Limite diário com Redis + Lua
- 0020 — Identidade e autorização
- Test
- transfers/bootstrap/ClockWiringIntegrationTest.kt
- token.sh
- V2__outbox.sql
- seed.sh
- seed-check.sh
- ApiDocs.kt
- V1__customer.sql

## God Nodes (most connected - your core abstractions)
1. `Transfer` - 114 edges
2. `Account` - 74 edges
3. `Caller` - 67 edges
4. `Money` - 65 edges
5. `assertProblem()` - 64 edges
6. `OverdraftLimit` - 64 edges
7. `AccountApiIntegrationTest` - 63 edges
8. `LedgerEntry` - 63 edges
9. `TransfersIntegrationTest` - 59 edges
10. `TransferStep` - 58 edges

## Surprising Connections (you probably didn't know these)
- `Mensageria` --references--> `OutboxRelay`  [INFERRED]
  docs/arquitetura.md → libs/outbox-relay/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/relay/OutboxRelay.kt
- `Task 14: Agendador, relay e eventos no `transfers` (Marina, `dev-kotlin`)` --references--> `OutboxRelayScheduler`  [INFERRED]
  docs/superpowers/plans/2026-10-27-etapa-2-transfers.md → libs/outbox-relay/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/relay/spring/OutboxRelayScheduler.kt
- `Task 7: `libs/outbox` — envelope e writer (Marie, `dev-kotlin`; em paralelo às Tasks 2–6)` --references--> `OutboxEventEnvelope`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/OutboxEventEnvelope.kt
- `6. Eventos consumidos e mudança no `accounts`` --references--> `OutboxEventEnvelope`  [INFERRED]
  docs/superpowers/specs/2026-10-28-etapa-3-mensageria-design.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/OutboxEventEnvelope.kt
- `Consequências` --references--> `Caller`  [INFERRED]
  docs/adr/0020-identidade-e-autorizacao.md → libs/security-caller/src/main/kotlin/io/github/capozziv/digitalbanking/security/Caller.kt

## Import Cycles
- None detected.

## Communities (369 total, 219 thin omitted)

### Community 1 - "SqsListenerFifoIntegrationTest.kt"
Cohesion: 0.14
Nodes (3): ListenersConfiguration, SqsListenerFifoIntegrationTest, TestListeners

### Community 5 - "RedisDailyLimitGateway"
Cohesion: 0.05
Nodes (15): DailyLimitConfiguration, DailyLimitProperties, DailyLimitScripts, JdbcDailyLimitStore, Outcome, EXCEEDED, FALLBACK_RESERVED, RESERVED (+7 more)

### Community 6 - "ServiceArchitectureRulesTest"
Cohesion: 0.05
Nodes (7): ServiceArchitectureRules, ServiceArchitectureRulesTest, ArchitectureTest, ArchitectureTest, ArchitectureTest, ArchitectureTest, ArchitectureTest

### Community 8 - "Account"
Cohesion: 0.05
Nodes (19): Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`), Task 8: Esquema, usuários de banco e repositórios JDBC (Marina, `dev-kotlin`; o script de init do compose segue o padrão do Rafael), JdbcAccountRepository, AccountBusyException, CustomerAlreadyHasAccountException, AccountRepository, InsertAccountResult, DUPLICATE_CUSTOMER (+11 more)

### Community 9 - "AccountsApplicationIntegrationTest.kt"
Cohesion: 0.06
Nodes (18): Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 13: Documentação do bloco 1 e das decisões da etapa (Clara, `documentador`; em paralelo às Tasks 8–12), Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`), Task 2: Valores monetários e tipos (Marina, `dev-kotlin`), Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`) (+10 more)

### Community 11 - "accounts/adapters/web/ProblemDetailsExceptionHandler.kt"
Cohesion: 0.05
Nodes (14): IdempotencyKeyHeader, ProblemDetailsExceptionHandler, AccountTypeNotFoundException, EntryNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException, HttpCustomerGatewayTest (+6 more)

### Community 12 - "OutboxRelayIntegrationTest.kt"
Cohesion: 0.08
Nodes (4): OutboxRelayIntegrationTest, ReceivedEvent, RecordingEventPublisher, RecordingPublisherConfiguration

### Community 13 - "OutboxRelay"
Cohesion: 0.22
Nodes (6): 0015 — Relay do outbox com líder único e SNS FIFO, Alternativas consideradas, Consequências, Contexto, Decisão, OutboxRelay

### Community 14 - "LedgerEntry"
Cohesion: 0.07
Nodes (11): JdbcLedgerEntryRepository, LedgerEntryRepository, InMemoryLedgerEntryRepository, DebitResult, EntryRecorded, Recorded, Refused, EntryKind (+3 more)

### Community 16 - "OutboxRelayTest"
Cohesion: 0.16
Nodes (3): FakeEventPublisher, MutableClock, OutboxRelayTest

### Community 17 - "OverdraftLimit"
Cohesion: 0.06
Nodes (19): 2. Modelo de domínio, JdbcAccountTypeRepository, AccountTypeRepository, ChangeOverdraftLimitUseCase, ListAccountTypesUseCase, AccountTypeRepository, InMemoryAccountTypeRepository, ChangeOverdraftLimitUseCaseTest (+11 more)

### Community 18 - "Decisões tomadas"
Cohesion: 0.13
Nodes (15): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Em aberto, Fluxo de entrada (+7 more)

### Community 19 - "SecurityAutoConfigurationIntegrationTest"
Cohesion: 0.10
Nodes (5): GroupsClaimIntegrationTest, SecurityAutoConfigurationIntegrationTest, assertProblem(), TestHttp, JwtTestSupportTest

### Community 22 - "TransferLeaseContentionIntegrationTest.kt"
Cohesion: 0.13
Nodes (3): TransferLeaseContentionIntegrationTest, ApiClient, ApiResponse

### Community 23 - "coordenador.md"
Cohesion: 0.29
Nodes (6): Ao começar uma sessão, Board, Para cada etapa do roadmap, Para cada tarefa, Paralelismo, Quando faltar algo

### Community 24 - "0019-runtime-na-aws-com-eks.md"
Cohesion: 0.10
Nodes (17): 0004 — API Gateway HTTP API na borda, Alternativas consideradas, Consequências, Contexto, Decisão, 0019 — Runtime na AWS com EKS, Alternativas consideradas, Consequências (+9 more)

### Community 25 - "0005 — Frontend Angular e BFF em NestJS"
Cohesion: 0.40
Nodes (5): 0005 — Frontend Angular e BFF em NestJS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 26 - "0006 — Observabilidade só com correlation ID"
Cohesion: 0.40
Nodes (5): 0006 — Observabilidade só com correlation ID, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 27 - "Order"
Cohesion: 0.07
Nodes (7): OrderController, PlaceOrderCommand, PlaceOrderResult, PlaceOrderUseCase, Order, NotificationEndToEndIntegrationTest, TransferSagaEndToEndIntegrationTest

### Community 29 - "Rodando o projeto localmente (macOS)"
Cohesion: 0.09
Nodes (22): 1. Configurar o ambiente, 2. Subir a infraestrutura (PostgreSQL, Redis, nginx, LocalStack, mock-oauth2-server), 3. Build e testes, 5. Conferir a API pelo nginx, 6. Ver os eventos na fila de inspeção, 7. Coleção Bruno, 8. Derrubar tudo, A mesma sequência à mão (+14 more)

### Community 30 - "AccountController.kt"
Cohesion: 0.15
Nodes (8): Regras inegociáveis, currentCaller(), AccountController, ChangeAccountTypeDTO, ChangeOverdraftLimitDTO, RecordEntryDTO, AccountDTO, LedgerEntryDTO

### Community 31 - "0008 — Extrato (`statement`) com CQRS no DynamoDB"
Cohesion: 0.50
Nodes (4): 0008 — Extrato (`statement`) com CQRS no DynamoDB, Consequências, Contexto, Decisão

### Community 32 - "RecordDebitUseCase"
Cohesion: 0.12
Nodes (4): Task 5: Casos de uso de crédito e débito (Marina, `dev-kotlin`), IdempotentEntryLookup, RecordCreditUseCase, RecordDebitUseCase

### Community 33 - "HttpAccountsGatewayIntegrationTest.kt"
Cohesion: 0.07
Nodes (3): CustomerClientProperties, RequireServiceToken, ServiceTokenServers

### Community 34 - "0013 — Persistência com Spring JDBC (`JdbcClient`)"
Cohesion: 0.40
Nodes (5): 0013 — Persistência com Spring JDBC (`JdbcClient`), Alternativas consideradas, Consequências, Contexto, Decisão

### Community 35 - "0012 — Saldo no lançamento e trava pessimista por conta"
Cohesion: 0.40
Nodes (5): 0012 — Saldo no lançamento e trava pessimista por conta, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 36 - "NotificationQueues"
Cohesion: 0.12
Nodes (5): DeadLetter, NotificationQueues, PublishedMessage, ReceivedEvent, TransferEventsQueue

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
Cohesion: 0.04
Nodes (31): Banco digital, Clientes e contas, Consulta e avisos, Ledger, Global Constraints, Task A3: Casos de uso e portas (Marina, `dev-kotlin`), Task A6: API das notificações (Marina, `dev-kotlin`) — paralela à A5, 2. Módulos e camadas (+23 more)

### Community 43 - "0011 — Contratos de API"
Cohesion: 0.50
Nodes (4): 0011 — Contratos de API, Consequências, Contexto, Decisão

### Community 44 - "Versões novas da etapa 1"
Cohesion: 0.50
Nodes (3): Observações, springdoc e Spring Boot 4.1.x, Versões novas da etapa 1

### Community 46 - "ServiceTokenClient"
Cohesion: 0.11
Nodes (9): Task 1: `libs/security` e `libs/observabilidade`, Configuração, libs/security, Módulos, Uso, CachedToken, RecentFailure, ServiceTokenClient (+1 more)

### Community 61 - "Money"
Cohesion: 0.18
Nodes (4): OutboxEventAppenderIntegrationTest, OutboxRow, Money, MoneyTest

### Community 63 - "account"
Cohesion: 0.33
Nodes (3): account_type, account, ledger_entry

### Community 64 - ".adminToken"
Cohesion: 0.23
Nodes (3): JwtTestSupport, assertProblem(), CustomerApiIntegrationTest

### Community 65 - "outbox-table.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 69 - "RecordDebitUseCaseTest"
Cohesion: 0.10
Nodes (11): IdempotencyKeyReusedException, DebitOutcome, Recorded, Refused, RecordEntryCommand, RecordEntryResult, DirectTransactionRunner, SequentialIdGenerator (+3 more)

### Community 71 - "JdbcTransferRepositoryIntegrationTest"
Cohesion: 0.16
Nodes (3): JdbcTransferRepositoryIntegrationTest, aTransfer(), microsNow()

### Community 74 - "ProcessedEventRepository"
Cohesion: 0.17
Nodes (3): JdbcKnownAccountRepository, KnownAccountRepository, InMemoryKnownAccountRepository

### Community 75 - "smoke.sh"
Cohesion: 0.57
Nodes (7): aws_local(), call(), check_events(), check_notification(), new_customer(), smoke.sh script, uuid()

### Community 78 - "HttpAccountsGateway"
Cohesion: 0.18
Nodes (6): AccountsAccountResponseDTO, AccountsEntryRequestDTO, AccountsEntryResponseDTO, AccountsProblemDTO, HttpAccountsGateway, RawResponse

### Community 80 - "01-accounts-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 01-accounts-topic.sh script

### Community 81 - "StatementPage"
Cohesion: 0.11
Nodes (4): NoOpStatementFirstPageCache, InMemoryStatementFirstPageCache, RedisStatementFirstPageCacheIntegrationTest, StatementPage

### Community 82 - "OutboxRelayScheduler"
Cohesion: 0.11
Nodes (4): OutboxRelayScheduler, BlockingStore, NoOpPublisher, OutboxRelaySchedulerTest

### Community 83 - "PendingEvent"
Cohesion: 0.06
Nodes (16): Bloco 2 — relay e SNS FIFO, Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), Bloco 1 — preparação para o 2º serviço (branch `etapa-2-transfers`, PR 1), Task 1: Gradle para vários serviços (Rafael, `infra`) — cartão #37, Task 2: Relay em módulo próprio e teste de esquema (Marina, `dev-kotlin`) — cartões #25 e #30, Task 3: Regras ArchUnit compartilhadas (Marina, `dev-kotlin`) — cartão #32, Task 4: Consulta de lançamento pela chave no `accounts` (Marina, `dev-kotlin`) (+8 more)

### Community 85 - "Foco de revisão"
Cohesion: 0.12
Nodes (13): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 4: Ambiente local (docker-compose), Task 5: CI no GitHub Actions (+5 more)

### Community 88 - "RetryBackoff"
Cohesion: 0.08
Nodes (7): 2. Módulos e camadas, RetryBackoff, GetTransferUseCase, RequestTransferUseCase, SagaSettings, SagaProperties, UseCaseConfiguration

### Community 89 - "OrderResource"
Cohesion: 0.27
Nodes (5): PersistenceUsesWeb, OrderResource, WebUsesPersistence, ApplicationUsesAdapters, DomainUsesAdapters

### Community 94 - "StatementEntry"
Cohesion: 0.05
Nodes (42): Task 5: Consumo SQS e métrica da DLQ (Marina, `dev-kotlin`; paralela à Task 6), Etapa 5 — cache do extrato: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Revisão final da branch (Beatriz), Rulings deste plano, Task 1: porta do cache e casos de uso (+34 more)

### Community 96 - "InMemoryTransferRepository"
Cohesion: 0.13
Nodes (3): InMemoryTransferRepository, Lease, GetTransferUseCaseTest

### Community 97 - "AccountAuthorizationIntegrationTest"
Cohesion: 0.19
Nodes (3): AccountAuthorizationIntegrationTest, asCustomer(), bearer()

### Community 98 - "TransferEventsIntegrationTest.kt"
Cohesion: 0.13
Nodes (3): ReceivedEvent, SentTransfer, TransferEventsIntegrationTest

### Community 99 - "NotificationKind"
Cohesion: 0.12
Nodes (16): 10. Ambiente local e CI, 11. Documentação que acompanha, 1. Frentes de trabalho, 4. Fluxo de consumo, 5. Esquema do banco (`notification`), 6. Eventos consumidos e mudança no `accounts`, 7. API, nginx e Bruno, 8. Operação (+8 more)

### Community 102 - "RunTransferSagaUseCase"
Cohesion: 0.12
Nodes (5): CallResult, Run, RunTransferSagaUseCase, FakeAccountsGateway, FakeAccountsGatewayTest

### Community 103 - "OutboxRelayConfigurationSupport.kt"
Cohesion: 0.11
Nodes (6): AwsProperties, OutboxRelayConfigurationSupport, OutboxRelayProperties, OutboxRelayConfiguration, OutboxRelayConfiguration, OutboxRelayConfiguration

### Community 105 - "InMemoryStatementRepository"
Cohesion: 0.16
Nodes (4): Task 3: Domínio e casos de uso (Marina, `dev-kotlin`), InMemoryStatementRepository, RecordStatementEntryUseCaseTest, RegisterAccountUseCaseTest

### Community 111 - "ApiClient"
Cohesion: 0.14
Nodes (3): ClockWiringIntegrationTest, ApiClient, ApiResponse

### Community 113 - "CallerResolver"
Cohesion: 0.15
Nodes (6): Service, O que a lib faz sozinha, CallerResolver, InvalidCallerException, CallerRoles, ApplicationWithSpring

### Community 127 - "TransferSagaResumeIntegrationTest.kt"
Cohesion: 0.09
Nodes (3): AccountsStubs, TransferSagaResumeIntegrationTest, DailyLimitApiIntegrationTest

### Community 129 - "AccountsProxy.kt"
Cohesion: 0.13
Nodes (3): AccountsProxy, HeldRequest, Hold

### Community 131 - "TransferRepository"
Cohesion: 0.10
Nodes (5): JdbcTransferStepLog, TransferRepository, StepRecord, TransferStepLog, JdbcTransferStepLogIntegrationTest

### Community 134 - "Balance"
Cohesion: 0.08
Nodes (7): GetAccountUseCaseTest, Balance, Money, AccountTest, AvailableBalanceTest, BalanceTest, MoneyTest

### Community 135 - "JdbcNotificationRepositoryIntegrationTest"
Cohesion: 0.14
Nodes (4): JdbcNotificationRepositoryIntegrationTest, JdbcProcessedEventRepositoryIntegrationTest, aNotification(), microsNow()

### Community 137 - "ServiceApiClients.kt"
Cohesion: 0.14
Nodes (7): AccountsApiClient, CustomerApiClient, HttpResult, NotificationApiClient, send(), StatementApiClient, TransfersApiClient

### Community 138 - "CustomerAdminController.kt"
Cohesion: 0.13
Nodes (5): CustomerAdminController, ChangeCustomerStatusDTO, CustomerDTO, RegisterCustomerDTO, ChangeCustomerStatusUseCase

### Community 140 - ".resumeDueOver"
Cohesion: 0.11
Nodes (5): AccountsGateway, CorrelationContext, EventOutbox, TransactionRunner, TransferStepLog

### Community 141 - "ResumeDueTransfersUseCase"
Cohesion: 0.17
Nodes (6): Outcome, INTERRUPTED, SKIPPED, TAKEN, ResumeDueTransfersUseCase, TransferSagaSchedulerConfiguration

### Community 142 - "RedisDailyLimitGatewayIntegrationTest"
Cohesion: 0.07
Nodes (6): Task 3: adapter Redis + Lua, trava, epoch e fallback, RedisTestSupportIntegrationTest, RedisTestSupport, RedisDailyLimitGatewayIntegrationTest, Lease, TransfersDatabase

### Community 144 - "JdbcOutboxWriter"
Cohesion: 0.05
Nodes (21): 10. Documentação que acompanha, 1. Módulos e camadas, 3. Esquema do banco, 4. Fluxos, 5. Outbox e relay (`libs/outbox`), 6. API, 7. Testes, 8. Ambiente local e CI (+13 more)

### Community 153 - "Customer"
Cohesion: 0.15
Nodes (13): 4. Subir os serviços `customer`, `accounts`, `transfers`, `notification` e `statement`, Task 4: serviço `customer`, 2. Serviço `customer`, OutboxEventAppender, EventOutbox, Customer, CustomerBlocked, CustomerEvent (+5 more)

### Community 154 - ".get"
Cohesion: 0.23
Nodes (6): AccountOpened, EventEnvelopeReader, Ignored, LedgerEntryRecorded, MalformedEventException, ReceivedEvent

### Community 155 - "TransferMetricsConfiguration.kt"
Cohesion: 0.26
Nodes (4): JdbcPendingTransferStats, PendingTransfers, TransferMetrics, TransferMetricsConfiguration

### Community 157 - "ChangeAccountTypeUseCaseTest.kt"
Cohesion: 0.14
Nodes (5): OutboxEventAppender, RecordingEventOutbox, ChangeAccountTypeUseCaseTest, AccountEvent, AccountTypeChanged

### Community 160 - "Transfer.kt"
Cohesion: 0.13
Nodes (3): ResolveCreditStepTest, ResolveDebitStepTest, TerminalStepsTest

### Community 161 - "StepOutcome"
Cohesion: 0.10
Nodes (21): Etapa 6 — limite diário: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Revisão final da branch (Beatriz), Rulings deste plano, Task 1: `libs/testing-redis` e ambiente, Task 2: domínio, saga e migração (+13 more)

### Community 163 - "uuid"
Cohesion: 0.03
Nodes (21): OutboxEventEnvelope, PendingEvent, Caller, Customer, AccountNotFoundException, requireCanRead(), CallerHasNoCustomerException, CustomerNotFoundException (+13 more)

### Community 173 - "TransferStep"
Cohesion: 0.10
Nodes (15): Task 8: Domínio da transferência e passos da saga (Marina, `dev-kotlin`), 3. Modelo de domínio, Riscos, Transfer, Compensate, Compensated, Completed, Credit (+7 more)

### Community 175 - "OutboxMessage"
Cohesion: 0.11
Nodes (4): JdbcOutboxWriterIntegrationTest, OutboxMessage, FixedClockConfiguration, OutboxWriterClockIntegrationTest

### Community 178 - "ApiClient"
Cohesion: 0.16
Nodes (4): CustomerEventsIntegrationTest, OutboxRow, ApiClient, ApiResponse

### Community 179 - "CustomerLookup"
Cohesion: 0.12
Nodes (10): CustomerProblemDTO, CustomerStatusResponseDTO, HttpCustomerGateway, CustomerGateway, CustomerLookup, ACTIVE, BLOCKED, NOT_FOUND (+2 more)

### Community 181 - "CustomerRepository"
Cohesion: 0.17
Nodes (4): CustomerRepository, GetCustomerUseCase, GetOwnCustomerUseCase, UseCaseConfiguration

### Community 182 - "RunTransferSagaUseCaseTest.kt"
Cohesion: 0.10
Nodes (9): Task 9: Portas e execução da saga (Marina, `dev-kotlin`), OutboxEventAppender, EventOutbox, FakeEventOutbox, InMemoryTransferStepLog, TransferCompensated, TransferCompleted, TransferEvent (+1 more)

### Community 184 - "02-transfers-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 02-transfers-topic.sh script

### Community 186 - "accounts/adapters/web/OpenApiConfiguration.kt"
Cohesion: 0.08
Nodes (8): ApiDocs, OpenApiConfiguration, OpenApiConfiguration, NotificationDTO, ApiDocs, OpenApiConfiguration, ApiDocs, OpenApiConfiguration

### Community 187 - "transfer"
Cohesion: 0.28
Nodes (6): transfer, transfer_due_idx, transfer_step, transfer_step_transfer_idx, daily_limit_epoch, transfer_source_limit_day_idx

### Community 188 - "0001 — Stack: Kotlin + Spring Boot 4 na AWS"
Cohesion: 0.33
Nodes (5): 0001 — Stack: Kotlin + Spring Boot 4 na AWS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 189 - "GetStatementUseCaseTest"
Cohesion: 0.14
Nodes (12): Etapa 4 — `statement`: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Task 1: Esqueleto Gradle e cliente DynamoDB no apoio de testes (Rafael, `infra`), Task 2: LocalStack, compose, nginx e CI (Rafael, `infra`), Task 4: Repositório DynamoDB e bootstrap (Marina, `dev-kotlin`), Task 7: e2e, smoke e Bruno (Marina, `dev-kotlin`) (+4 more)

### Community 191 - "03-notification-queues.sh"
Cohesion: 0.39
Nodes (7): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, create_consumer_queue(), queue_arn(), 03-notification-queues.sh script

### Community 192 - "V3__outbox.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 198 - "statement/bootstrap/DlqMetricsConfiguration.kt"
Cohesion: 0.22
Nodes (3): QueueProperties, DlqDepthRefresher, DlqMetricsConfiguration

### Community 200 - "NotificationController.kt"
Cohesion: 0.14
Nodes (7): MarkedNotificationsDTO, MarkNotificationsReadDTO, NotificationPageDTO, NotificationController, NotificationCursorCodec, InvalidField, InvalidRequestException

### Community 202 - "0014 — Tipo de conta e cheque especial"
Cohesion: 0.40
Nodes (5): 0014 — Tipo de conta e cheque especial, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 203 - "NotificationText"
Cohesion: 0.20
Nodes (3): NotificationContent, NotificationText, NotificationTextTest

### Community 204 - ".request"
Cohesion: 0.16
Nodes (11): IdempotencyKeyHeader, AccountsUnavailableException, CustomerBlockedException, CustomerNotFoundException, CustomersUnavailableException, InvalidField, InvalidRequestException, OwnAccountNotFoundException (+3 more)

### Community 205 - "0007 — Conta e ledger no mesmo serviço (`accounts`)"
Cohesion: 0.50
Nodes (4): 0007 — Conta e ledger no mesmo serviço (`accounts`), Consequências, Contexto, Decisão

### Community 211 - "Fakes.kt"
Cohesion: 0.12
Nodes (4): DirectTransactionRunner, MutableClock, RecordingEventOutbox, ChangeCustomerStatusUseCaseTest

### Community 214 - ".decode"
Cohesion: 0.27
Nodes (4): Task 6: API do extrato (Marina, `dev-kotlin`; paralela à Task 5), InvalidCursorException, StatementCursorCodec, StatementCursorCodecTest

### Community 215 - "TransferStatus"
Cohesion: 0.10
Nodes (21): Transferências, Etapa 2 — `transfers` + saga Implementation Plan, Global Constraints, Review Focus, 10. Documentação que acompanha, 5. Esquema do banco (`transfers`), 6. Eventos, 7. API, nginx e Bruno (+13 more)

### Community 217 - "AccountsIntegrationTest"
Cohesion: 0.15
Nodes (3): AccountsIntegrationTest, UpdateGrantsIntegrationTest, UseCaseConfigurationIntegrationTest

### Community 220 - "0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS"
Cohesion: 0.40
Nodes (5): 0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 223 - "OpenAccountUseCaseTest.kt"
Cohesion: 0.11
Nodes (12): CustomerGateway, CustomerStatus, ACTIVE, BLOCKED, AccountView, OpenAccountCommand, OpenAccountResult, CallLog (+4 more)

### Community 225 - "Banco digital em microsserviços"
Cohesion: 0.25
Nodes (8): Agentes do projeto, Ao terminar uma tarefa, Autonomia, Banco digital em microsserviços, Contexto, Fluxo de trabalho (skills), Git, Grafo do código (graphify)

### Community 227 - "04-statement.sh"
Cohesion: 0.43
Nodes (6): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, queue_arn(), 04-statement.sh script

### Community 229 - "CustomerServiceStub"
Cohesion: 0.13
Nodes (9): Active, Behavior, Blocked, CustomerServiceStub, Lookup, NotFound, ServerError, Slow (+1 more)

### Community 230 - "Etapa 0 — Fundação: design"
Cohesion: 0.13
Nodes (13): Task 3: Aplicação `accounts` com health do banco (Testcontainers), 2. Build e convenções, 3. Esqueleto do `accounts`, 4. Ambiente local, 5. CI (GitHub Actions), Critério de pronto, Etapa 0 — Fundação: design, Fora de escopo (e onde foi registrado) (+5 more)

### Community 231 - "Etapa 6 — limite diário: design"
Cohesion: 0.13
Nodes (15): 1. Fonte da verdade no PostgreSQL, 2. Contador no Redis (script Lua), 3. Passo `ReserveLimit` da saga, 4. Trava única e fallback no PostgreSQL, 5. Endpoint de consulta, 6. Observabilidade, 7. Testes, 8. Ambiente local, CI e documentação (+7 more)

### Community 232 - "content"
Cohesion: 0.15
Nodes (4): CustomerLookupDTO, InternalCustomerController, ApiDocs, OpenApiConfiguration

### Community 233 - "SecurityProblems.kt"
Cohesion: 0.19
Nodes (3): ProblemDetailsAccessDeniedHandler, ProblemDetailsAuthenticationEntryPoint, SecurityProblems

### Community 234 - "ConsumeEventUseCase"
Cohesion: 0.26
Nodes (5): Etapa 3 — Mensageria e `notification` Implementation Plan, Frentes e ordem, Review Focus, ConsumeEventUseCase, Draft

### Community 235 - "Frente B — débitos técnicos"
Cohesion: 0.29
Nodes (6): Frente B — débitos técnicos, Task B1: Configuração do relay em `libs/outbox-relay` (Marina, `dev-kotlin`) — cartão #92, Task B2: Backoff do relay e testes (Marina, `dev-kotlin`) — cartões #24 e #26, depois da B1, Task B3: Domínio e persistência do `transfers` (Marina, `dev-kotlin`) — cartões #95 e #93, Task B4: Aplicação e adapters do `transfers` (Marina, `dev-kotlin`) — cartão #94, depois da B3, Task B5: Endurecer os testes ponta a ponta (Marina, `dev-kotlin`) — cartão #96, antes da A9

### Community 236 - "notification/bootstrap/DlqMetricsConfiguration.kt"
Cohesion: 0.17
Nodes (4): QueueProperties, DlqDepthMonitor, DlqDepthProbe, DlqMetricsConfiguration

### Community 238 - "AccountsClientProperties"
Cohesion: 0.31
Nodes (3): 3. Autorização nos serviços, AccountsClientProperties, SagaTimeoutsValidator

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
Cohesion: 0.22
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

### Community 260 - "runapplication"
Cohesion: 0.15
Nodes (4): CustomerApplication, NotificationApplication, StatementApplication, TransfersApplication

### Community 261 - "assertProblem"
Cohesion: 0.11
Nodes (5): ApiClient, ApiResponse, assertProblem(), StatementOwnershipIntegrationTest, UnexpectedErrorApiIntegrationTest

### Community 262 - ".record"
Cohesion: 0.18
Nodes (3): AccountRepository, LedgerEntryRepository, TransactionRunner

### Community 263 - "0018 — Extrato ordenado pela sequência do ledger"
Cohesion: 0.40
Nodes (5): 0018 — Extrato ordenado pela sequência do ledger, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 264 - "CorrelationIdFilter"
Cohesion: 0.31
Nodes (3): Task 9: API HTTP, Problem Details e correlation ID (Marina, `dev-kotlin`), 5. `libs/observabilidade`, CorrelationIdFilter

### Community 271 - "Etapa 5 — cache do extrato: design"
Cohesion: 0.14
Nodes (14): 2. Adapter Redis, 3. Consistência, 4. Observabilidade, 5. Configuração, 6. Testes, 7. Ambiente local e CI, 8. Documentação que acompanha, Critério de pronto (+6 more)

### Community 273 - "MutableClock"
Cohesion: 0.10
Nodes (8): AccountNotFound, AccountsUnavailable, DailyLimitOutcome, Found, GetDailyLimitUseCase, MutableClock, GetDailyLimitUseCaseTest, DailyLimitDayTest

### Community 275 - "SecurityTestApplication.kt"
Cohesion: 0.17
Nodes (3): CatchAllExceptionHandler, SecurityTestApplication, SecurityTestController

### Community 276 - "NotificationAccessIntegrationTest"
Cohesion: 0.13
Nodes (4): ApiClient, ApiResponse, assertProblem(), NotificationAccessIntegrationTest

### Community 277 - "OutboxMetrics"
Cohesion: 0.16
Nodes (3): OutboxBacklog, OutboxMetricsTest, FakeOutboxStore

### Community 278 - "RedisStatementFirstPageCache"
Cohesion: 0.19
Nodes (3): CachedStatementEntry, CachedStatementPage, RedisStatementFirstPageCache

### Community 281 - "0002 — Clean Architecture por serviço"
Cohesion: 0.40
Nodes (5): 0002 — Clean Architecture por serviço, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 283 - "StatementCacheProperties"
Cohesion: 0.09
Nodes (6): ServiceTokenAutoConfiguration, ServiceTokenProperties, StatementCacheProperties, RedisStatementFirstPageCacheTest, StatementCachePropertiesTest, CacheConfiguration

### Community 287 - "SecurityRules"
Cohesion: 0.12
Nodes (6): SecurityRules, AccountsSecurityConfiguration, CustomerSecurityConfiguration, NotificationSecurityConfiguration, StatementSecurityConfiguration, TransfersSecurityConfiguration

### Community 290 - "CustomerController.kt"
Cohesion: 0.17
Nodes (4): CustomerController, RegisterCustomerCommand, RegisterCustomerUseCase, RegisterCustomerUseCaseTest

### Community 296 - "Frente A — `notification`"
Cohesion: 0.22
Nodes (9): Frente A — `notification`, Task A10: Documentação (Clara, `documentador`) — paralela desde o início, Task A1: Esqueleto do `notification` e prova do `@SqsListener` (Marina, `dev-kotlin`), Task A2: Domínio das notificações (Marina, `dev-kotlin`), Task A4: Persistência (Marina, `dev-kotlin`), Task A5: Consumo das filas (Marina, `dev-kotlin`) — paralela à A6, Task A7: `AccountTypeChanged` versão 2 no `accounts` (Marina, `dev-kotlin`) — paralela desde o início, Task A8: Infra local, Bruno, smoke e CI do `notification` (Rafael, `infra`) (+1 more)

### Community 303 - "TransferController.kt"
Cohesion: 0.19
Nodes (4): CorrelationId, TransferDTO, TransferController, TransferFailedException

### Community 305 - "AccountLookup"
Cohesion: 0.13
Nodes (8): Task 12: Cliente HTTP do `accounts` (Marina, `dev-kotlin`), UuidV7IdGenerator, AccountLookup, AccountsGateway, Found, NotFound, Unavailable, IdGenerator

### Community 306 - "0009 — Cache do extrato"
Cohesion: 0.40
Nodes (5): 0009 — Cache do extrato, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 308 - ".repeat"
Cohesion: 0.17
Nodes (12): IdempotencyKeyReusedException, RequestTransferCommand, AccountNotFound, AccountsUnavailable, Created, CustomerBlocked, CustomerNotFound, CustomersUnavailable (+4 more)

### Community 313 - "Ordem e paralelismo"
Cohesion: 0.15
Nodes (13): Etapa 7 — borda e identidade: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Revisão final da branch (Beatriz), Rulings deste plano, Task 2: ambiente local de identidade, Task 3: Terraform do Cognito e do API Gateway (sem aplicar) (+5 more)

### Community 318 - "Etapa 7 — borda e identidade: design"
Cohesion: 0.14
Nodes (14): 10. Documentação, 1. Identidade e o id do cliente, 4. Consulta ao `customer` no pedido, 6. Segurança transversal, 7. Ambiente local, 8. Terraform (sem aplicar), 9. Testes, Critério de pronto (+6 more)

### Community 338 - "ReceivedEvent"
Cohesion: 0.25
Nodes (7): AccountOpened, AccountTypeChanged, ReceivedEvent, TransferCompensated, TransferCompleted, TransferFailed, Unsupported

### Community 344 - "05-customer-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 05-customer-topic.sh script

### Community 348 - "0010 — Limite diário com Redis + Lua"
Cohesion: 0.40
Nodes (5): 0010 — Limite diário com Redis + Lua, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 349 - "0020 — Identidade e autorização"
Cohesion: 0.40
Nodes (5): 0020 — Identidade e autorização, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 352 - "token.sh"
Cohesion: 0.83
Nodes (3): request(), token.sh script, usage()

### Community 353 - "V2__outbox.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

## Knowledge Gaps
- **72 isolated node(s):** `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` (+67 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 974 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **219 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `NotificationApplication` connect `runapplication` to `NotificationConsumptionIntegrationTest`, `SqsListenerFifoIntegrationTest.kt`?**
  _High betweenness centrality (0.030) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Transfer` (e.g. with `Transferências` and `Global Constraints`) actually correct?**
  _`Transfer` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION` to the rest of the system?**
  _72 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SqsListenerFifoIntegrationTest.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.14039408866995073 - nodes in this community are weakly interconnected._
- **Why does `Caller` connect `uuid` to `RequestTransferUseCaseTest`, `Account`, `MutableClock`, `Decisões tomadas`, `Customer`, `AccountController.kt`, `Notification`, `ServiceTokenClient`, `.repeat`, `CustomerRepository`, `Ordem e paralelismo`, `GetStatementUseCaseTest`, `Etapa 7 — borda e identidade: design`, `CallerAuthentication.kt`, `RetryBackoff`, `0020 — Identidade e autorização`, `StatementEntry`, `OpenAccountUseCaseTest.kt`, `TransfersIntegrationTest`, `CallerResolver`?**
  _High betweenness centrality (0.028) - this node is a cross-community bridge._
- **Should `AccountApiIntegrationTest` be split into smaller, more focused modules?**
  _Cohesion score 0.11153846153846154 - nodes in this community are weakly interconnected._
- **Why does `TransfersIntegrationTest` connect `TransfersIntegrationTest` to `TransferRepository`, `TransferApiIntegrationTest`, `RedisDailyLimitGatewayIntegrationTest`, `TransferMetricsIntegrationTest.kt`, `TransferAuthorizationIntegrationTest`, `TransferLeaseContentionIntegrationTest.kt`, `persistence/OutboxSchemaIntegrationTest.kt`, `HttpAccountsGatewayIntegrationTest.kt`, `uuid`, `V6BackfillIntegrationTest.kt`, `JwtTestSupport.kt`, `TransfersApplicationIntegrationTest.kt`, `OutboxMessage`, `Money`, `JdbcTransferRepositoryIntegrationTest`, `TransferSchemaIntegrationTest`, `SpringTransactionRunnerIntegrationTest`, `OpenApiIntegrationTest`, `DailyLimitSagaIntegrationTest.kt`, `transfers/bootstrap/ClockWiringIntegrationTest.kt`, `TransferEventsIntegrationTest.kt`, `CustomerServiceStub`, `Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)`, `TransferSagaResumeIntegrationTest.kt`?**
  _High betweenness centrality (0.026) - this node is a cross-community bridge._