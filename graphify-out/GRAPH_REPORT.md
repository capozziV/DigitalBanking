# Graph Report - feature-conta-status  (2026-11-18)

## Corpus Check
- 737 files · ~225,100 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 63 file(s) not represented in the graph (top: .bru 50, (none) 4, .properties 2)

## Summary
- 6918 nodes · 17506 edges · 387 communities (168 shown, 219 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 1243 edges (avg confidence: 0.88)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `2791ec1b`
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
- CompensateTransferManuallyUseCase
- .handle
- OutboxRelayIntegrationTest.kt
- CloseAccountUseCaseTest
- LedgerEntry
- JdbcRepositoriesIntegrationTest
- OutboxRelayTest
- OverdraftLimit
- Decisões tomadas
- SecurityAutoConfigurationIntegrationTest
- TransferAuthorizationIntegrationTest
- PendingEvent
- TransferLeaseContentionIntegrationTest.kt
- coordenador.md
- Terraform
- 0005 — Frontend Angular e BFF em NestJS
- 0006 — Observabilidade só com correlation ID
- TransferSagaEndToEndIntegrationTest
- StatementConsumptionIntegrationTest
- Rodando o projeto localmente (macOS)
- AccountController.kt
- 0008 — Extrato (`statement`) com CQRS no DynamoDB
- TransferAdminApiIntegrationTest
- HttpAccountsGatewayIntegrationTest.kt
- 0013 — Persistência com Spring JDBC (`JdbcClient`)
- 0012 — Saldo no lançamento e trava pessimista por conta
- NotificationQueues
- CorrelationIdSqsMessageInterceptorTest
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
- AccountClosingConcurrencyIntegrationTest
- JdbcTransferRepositoryIntegrationTest
- SagaPropertiesTest.kt
- NotificationConsumptionIntegrationTest
- ProcessedEventRepository
- smoke.sh
- statement/adapters/web/ProblemDetailsExceptionHandler.kt
- notification/bootstrap/AwsCredentialsConfiguration.kt
- StepCall
- SpringTransactionRunnerIntegrationTest
- 01-accounts-topic.sh
- StatementPage
- OutboxRelayScheduler
- OutboxStore
- HttpAccountsGatewayIntegrationTest
- Foco de revisão
- .repeat
- RetryBackoff
- OrderResource
- RunTransferSagaUseCaseTest
- LocalStackFifoIntegrationTest.kt
- LocalStackTestSupport
- AwsClientConfigurationTest.kt
- StatementRepository
- AccountTypeAdminController.kt
- InMemoryTransferRepository
- AccountAuthorizationIntegrationTest
- TransferEventsIntegrationTest.kt
- NotificationKind
- MarkNotificationsReadUseCaseTest
- EventEnvelopeReaderTest
- AwsProfileConfigAssertions
- OutboxRelayConfigurationSupport.kt
- StatementEntry
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
- HttpCustomerGatewayTest
- RedisDailyLimitGatewayIntegrationTest.kt
- StepRecord
- DynamoDbStatementRepositoryIntegrationTest
- ResumeDueTransfersUseCaseTest
- Balance
- NotificationApiIntegrationTest
- assertOutboxSchemaMatchesModel
- ServiceApiClients.kt
- CustomerAdminController.kt
- accounts/bootstrap/UseCaseConfiguration.kt
- Order
- ProcessedEventRepository
- RedisDailyLimitGatewayIntegrationTest
- OutboxAdminControllerIntegrationTest.kt
- AccountNotFoundException
- EndToEndEnvironment
- StatementIntegrationTest
- TransferMetricsIntegrationTest.kt
- SnsEventPublisherTest.kt
- jsonmapper
- JdbcTransferRepository
- AccountTypeAdminApiIntegrationTest
- TransferSagaScheduler
- Customer
- .get
- TransferMetricsConfiguration.kt
- .container
- JdbcRepositoriesIntegrationTest.kt
- customer/bootstrap/OutboxRelayIntegrationTest.kt
- ServiceProcess
- ResolveCreditStepTest
- StepOutcome
- SchedulerLeaseOwners
- uuid
- EndToEndInputs
- TransferAdminController.kt
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
- StuckTransferAdminUseCasesTest.kt
- ListNotificationsUseCaseTest
- 02-transfers-topic.sh
- OpenApiIntegrationTest
- accounts/adapters/web/OpenApiConfiguration.kt
- transfer
- TestHttp.kt
- GetStatementUseCaseTest
- CacheConfigurationTest.kt
- 03-notification-queues.sh
- V3__outbox.sql
- accounts/bootstrap/web/UnexpectedErrorApiIntegrationTest.kt
- statement/bootstrap/DlqMetricsConfiguration.kt
- TransferSchemaIntegrationTest
- NotificationController.kt
- loggerfactory
- 0014 — Tipo de conta e cheque especial
- NotificationText
- .request
- 0007 — Conta e ledger no mesmo serviço (`accounts`)
- customer/adapters/web/ProblemDetailsExceptionHandler.kt
- ProblemResponsesOpenApiCustomizerTest.kt
- OpenApiIntegrationTest
- status.md
- SpringTransactionRunner
- ChangeCustomerStatusUseCaseTest
- CreditStepTest
- DailyLimitSagaIntegrationTest.kt
- .decode
- OutboxAdminController.kt
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
- AccountStatusAdminController.kt
- Etapa 0 — Fundação: design
- Etapa 6 — limite diário: design
- content
- SecurityProblems.kt
- ConsumeEventUseCase
- Frente B — débitos técnicos
- notification/bootstrap/DlqMetricsConfiguration.kt
- AccountsApplicationIntegrationTest.kt
- AccountsClientProperties
- DynamoDbConfiguration.kt
- DebitStepTest
- DailyLimitProperties
- BlockedOrClosedAccountSagaIntegrationTest.kt
- .get
- header
- HttpTransfersGatewayTest
- ReserveLimitStepTest
- loggerfactory
- StatementEntry
- Status do projeto
- DailyLimitController.kt
- .dailyLimitGateway
- RedisDailyLimitGatewayResilienceIntegrationTest.kt
- 0016 — Saga de transferência orquestrada por HTTP síncrono
- V1__notification_schema.sql
- StatementEndToEndIntegrationTest
- send
- runapplication
- assertProblem
- .record
- 0018 — Extrato ordenado pela sequência do ledger
- Bloco 1 — `accounts` gravando no outbox
- AccountTypeTest.kt
- SchedulingConfiguration.kt
- EntryKindTest.kt
- notification/bootstrap/SchedulingConfiguration.kt
- ProblemResponsesOpenApiCustomizer.kt
- Etapa 5 — cache do extrato: design
- EventEnvelopeReader
- MutableClock
- DailyLimitEndToEndIntegrationTest.kt
- SecurityTestApplication.kt
- NotificationAccessIntegrationTest
- OutboxMetrics
- RedisTestSupportIntegrationTest
- DynamoDbStatementRepositoryTest.kt
- SagaIntegrationSupport.kt
- 0002 — Clean Architecture por serviço
- OpenApiIntegrationTest
- StatementCacheProperties
- TransferAdminExceptionHandler.kt
- AccountOpeningCustomerIntegrationTest
- SecurityRules
- ProcessLogsOnFailureExtension
- RedisStatementFirstPageCacheIntegrationTest.kt
- CustomerController.kt
- JwtTestSupport.kt
- V6BackfillIntegrationTest.kt
- AccountStatusApiIntegrationTest
- httpclient
- TransfersApplicationIntegrationTest.kt
- AccountOpeningCustomerDownIntegrationTest.kt
- MockitoAgentArgumentProvider
- accounts/adapters/customer/HttpCustomerGateway.kt
- OpenApiIntegrationTest
- Bloqueio e encerramento de conta: design
- TransferController.kt
- ServiceTokenClientIntegrationTest
- Service
- status.md
- jsonmapper
- IdempotencyKeyReusedException
- CallerResolverTest
- DigitalBankingSecurityAutoConfiguration.kt
- ProblemResponsesOpenApiCustomizerTest
- Ordem e paralelismo
- Etapa 7 — borda e identidade: design
- CorrelationIdRestClientInterceptorTest.kt
- WebExceptionHandling.kt
- RedisCircuitBreaker
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
- TransfersClientConfiguration.kt
- ChangeAccountTypeUseCaseTest
- statement/bootstrap/web/UnexpectedErrorApiIntegrationTest.kt
- ReceivedEvent
- ObservabilityAutoConfiguration.kt
- OrderController.kt
- TransfersDatabase
- LocalStackInitScripts
- RequestTransferDTO
- 05-customer-topic.sh
- CallerTest.kt
- customer/adapters/persistence/OutboxWriterConfiguration.kt
- transfers/adapters/persistence/OutboxWriterConfiguration.kt
- 0010 — Limite diário com Redis + Lua
- 0020 — Identidade e autorização
- TransfersSecurityConfiguration.kt
- transfers/bootstrap/ClockWiringIntegrationTest.kt
- token.sh
- V2__outbox.sql
- seed.sh
- seed-check.sh
- accounts/bootstrap/AwsProfileIntegrationTest.kt
- ApiDocs.kt
- V1__customer.sql
- accounts/bootstrap/ClockWiringIntegrationTest.kt
- customer/bootstrap/AwsProfileIntegrationTest.kt
- notification/bootstrap/web/AwsProfileIntegrationTest.kt
- statement/bootstrap/web/AwsProfileIntegrationTest.kt
- JdbcTransferStepLogIntegrationTest.kt
- transfers/bootstrap/web/AwsProfileIntegrationTest.kt
- .outbox
- AccountsApplication.kt
- accounts/adapters/persistence/OutboxWriterConfiguration.kt
- LedgerEntryRepository
- 0004 — API Gateway HTTP API na borda
- 0019 — Runtime na AWS com EKS
- 0023 — Status da conta e encerramento
- TransactionRunner
- TestListeners
- NotificationTest
- StatementApplication.kt

## God Nodes (most connected - your core abstractions)
1. `Transfer` - 135 edges
2. `Account` - 91 edges
3. `assertProblem()` - 83 edges
4. `Money` - 81 edges
5. `Caller` - 73 edges
6. `OverdraftLimit` - 69 edges
7. `TransfersIntegrationTest` - 68 edges
8. `LedgerEntry` - 66 edges
9. `TransferStep` - 66 edges
10. `AccountApiIntegrationTest` - 63 edges

## Surprising Connections (you probably didn't know these)
- `5. `libs/observabilidade`` --references--> `CorrelationIdFilter`  [INFERRED]
  docs/superpowers/specs/2026-11-13-etapa-7-borda-e-identidade-design.md → libs/observabilidade/src/main/kotlin/io/github/capozziv/digitalbanking/observability/CorrelationIdFilter.kt
- `Mensageria` --references--> `OutboxRelay`  [INFERRED]
  docs/arquitetura.md → libs/outbox-relay/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/relay/OutboxRelay.kt
- `Task 14: Agendador, relay e eventos no `transfers` (Marina, `dev-kotlin`)` --references--> `OutboxRelayScheduler`  [INFERRED]
  docs/superpowers/plans/2026-10-27-etapa-2-transfers.md → libs/outbox-relay/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/relay/spring/OutboxRelayScheduler.kt
- `Consequências` --references--> `Caller`  [INFERRED]
  docs/adr/0020-identidade-e-autorizacao.md → libs/security-caller/src/main/kotlin/io/github/capozziv/digitalbanking/security/Caller.kt
- `Borda e segurança` --references--> `Caller`  [INFERRED]
  docs/arquitetura.md → libs/security-caller/src/main/kotlin/io/github/capozziv/digitalbanking/security/Caller.kt

## Import Cycles
- None detected.

## Communities (387 total, 219 thin omitted)

### Community 6 - "ServiceArchitectureRulesTest"
Cohesion: 0.05
Nodes (7): ServiceArchitectureRules, ServiceArchitectureRulesTest, ArchitectureTest, ArchitectureTest, ArchitectureTest, ArchitectureTest, ArchitectureTest

### Community 7 - ".transfer"
Cohesion: 0.07
Nodes (7): CompensateStepTest, DailyLimitDayTest, ManualCompensationTest, ResolveDebitStepTest, TerminalStepsTest, TransferFixtures, TransferTest

### Community 8 - "Account"
Cohesion: 0.07
Nodes (20): Regras inegociáveis, Clientes e contas, Task 8: Esquema, usuários de banco e repositórios JDBC (Marina, `dev-kotlin`; o script de init do compose segue o padrão do Rafael), Task 1: `accounts`: status, bloqueio, encerramento e conta nova, Restrições (de `CLAUDE.md` e ADRs), JdbcAccountRepository, AccountBusyException, AccountRepository (+12 more)

### Community 9 - "AccountsApplicationIntegrationTest.kt"
Cohesion: 0.14
Nodes (4): ConcurrencyIntegrationTest, isAccountBusy(), retryWhileAccountBusy(), RetryWhileAccountBusyTest

### Community 10 - "CompensateTransferManuallyUseCase"
Cohesion: 0.08
Nodes (23): JdbcManualResolutionLog, ManualResolution, ManualResolutionLog, Accepted, AccountsUnavailable, Busy, Compensated, CompensateManuallyOutcome (+15 more)

### Community 12 - "OutboxRelayIntegrationTest.kt"
Cohesion: 0.09
Nodes (3): OutboxRelayIntegrationTest, ReceivedEvent, RecordingPublisherConfiguration

### Community 13 - "CloseAccountUseCaseTest"
Cohesion: 0.13
Nodes (4): TransfersGateway, CloseAccountUseCase, StubTransfersGateway, CloseAccountUseCaseTest

### Community 14 - "LedgerEntry"
Cohesion: 0.06
Nodes (14): Task 5: Casos de uso de crédito e débito (Marina, `dev-kotlin`), JdbcLedgerEntryRepository, LedgerEntryRepository, FindEntryByIdempotencyKeyUseCase, IdempotentEntryLookup, InMemoryLedgerEntryRepository, AccountBlockedException, AccountClosedException (+6 more)

### Community 16 - "OutboxRelayTest"
Cohesion: 0.20
Nodes (3): FakeEventPublisher, MutableClock, OutboxRelayTest

### Community 17 - "OverdraftLimit"
Cohesion: 0.06
Nodes (25): Global Constraints, 2. Modelo de domínio, JdbcAccountTypeRepository, AccountTypeRepository, CallLog, AccountTypeRepository, DirectTransactionRunner, InMemoryAccountTypeRepository (+17 more)

### Community 18 - "Decisões tomadas"
Cohesion: 0.13
Nodes (15): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Em aberto, Fluxo de entrada (+7 more)

### Community 19 - "SecurityAutoConfigurationIntegrationTest"
Cohesion: 0.15
Nodes (3): SecurityAutoConfigurationIntegrationTest, assertProblem(), JwtTestSupportTest

### Community 21 - "PendingEvent"
Cohesion: 0.07
Nodes (17): Task 7: `libs/outbox` — envelope e writer (Marie, `dev-kotlin`; em paralelo às Tasks 2–6), Bloco 1 — preparação para o 2º serviço (branch `etapa-2-transfers`, PR 1), Task 1: Gradle para vários serviços (Rafael, `infra`) — cartão #37, Task 2: Relay em módulo próprio e teste de esquema (Marina, `dev-kotlin`) — cartões #25 e #30, Task 3: Regras ArchUnit compartilhadas (Marina, `dev-kotlin`) — cartão #32, Task 4: Consulta de lançamento pela chave no `accounts` (Marina, `dev-kotlin`), Task 5: PostgreSQL com um banco por serviço (Rafael, `infra`) — cartão #38, Task 6: Build seletivo no CI (Rafael, `infra`) (+9 more)

### Community 22 - "TransferLeaseContentionIntegrationTest.kt"
Cohesion: 0.09
Nodes (4): TransferLeaseContentionIntegrationTest, ApiClient, ApiResponse, PendingTransfersApiIntegrationTest

### Community 23 - "coordenador.md"
Cohesion: 0.29
Nodes (6): Ao começar uma sessão, Board, Para cada etapa do roadmap, Para cada tarefa, Paralelismo, Quando faltar algo

### Community 24 - "Terraform"
Cohesion: 0.25
Nodes (7): Borda: limites e log de acesso, Cognito: domínio e PKCE, Entradas da etapa 8, Estrutura, Observações, Terraform, Validar

### Community 25 - "0005 — Frontend Angular e BFF em NestJS"
Cohesion: 0.40
Nodes (5): 0005 — Frontend Angular e BFF em NestJS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 26 - "0006 — Observabilidade só com correlation ID"
Cohesion: 0.40
Nodes (5): 0006 — Observabilidade só com correlation ID, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 29 - "Rodando o projeto localmente (macOS)"
Cohesion: 0.09
Nodes (22): 1. Configurar o ambiente, 2. Subir a infraestrutura (PostgreSQL, Redis, nginx, LocalStack, mock-oauth2-server), 3. Build e testes, 5. Conferir a API pelo nginx, 6. Ver os eventos na fila de inspeção, 7. Coleção Bruno, 8. Derrubar tudo, A mesma sequência à mão (+14 more)

### Community 30 - "AccountController.kt"
Cohesion: 0.12
Nodes (5): currentCaller(), AccountClosingController, AccountController, AccountDTO, LedgerEntryDTO

### Community 31 - "0008 — Extrato (`statement`) com CQRS no DynamoDB"
Cohesion: 0.40
Nodes (4): 0008 — Extrato (`statement`) com CQRS no DynamoDB, Consequências, Contexto, Decisão

### Community 33 - "HttpAccountsGatewayIntegrationTest.kt"
Cohesion: 0.05
Nodes (9): Behavior, Pending, ServerError, Slow, TransfersServiceStub, CustomerClientProperties, HttpCustomerGatewayIntegrationTest, RequireServiceToken (+1 more)

### Community 34 - "0013 — Persistência com Spring JDBC (`JdbcClient`)"
Cohesion: 0.40
Nodes (5): 0013 — Persistência com Spring JDBC (`JdbcClient`), Alternativas consideradas, Consequências, Contexto, Decisão

### Community 35 - "0012 — Saldo no lançamento e trava pessimista por conta"
Cohesion: 0.40
Nodes (5): 0012 — Saldo no lançamento e trava pessimista por conta, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 36 - "NotificationQueues"
Cohesion: 0.12
Nodes (5): DeadLetter, NotificationQueues, PublishedMessage, ReceivedEvent, TransferEventsQueue

### Community 37 - "CorrelationIdSqsMessageInterceptorTest"
Cohesion: 0.14
Nodes (3): CorrelationIdSqsMessageInterceptor, SqsCorrelationIdAutoConfiguration, CorrelationIdSqsMessageInterceptorTest

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
Cohesion: 0.13
Nodes (4): JdbcNotificationRepository, NotificationRepository, InMemoryNotificationRepository, Notification

### Community 43 - "0011 — Contratos de API"
Cohesion: 0.50
Nodes (4): 0011 — Contratos de API, Consequências, Contexto, Decisão

### Community 44 - "Versões novas da etapa 1"
Cohesion: 0.50
Nodes (3): Observações, springdoc e Spring Boot 4.1.x, Versões novas da etapa 1

### Community 46 - "ServiceTokenClient"
Cohesion: 0.12
Nodes (9): Task 1: `libs/security` e `libs/observabilidade`, Configuração, libs/security, Módulos, Uso, CachedToken, RecentFailure, ServiceTokenClient (+1 more)

### Community 61 - "Money"
Cohesion: 0.08
Nodes (8): FakeAccountsGatewayTest, StuckTransferAdminUseCasesTest, CorrelationContext, EventOutbox, OutboxEventAppenderIntegrationTest, OutboxRow, Money, MoneyTest

### Community 63 - "account"
Cohesion: 0.25
Nodes (4): account_type, account, ledger_entry, account_customer_id_not_closed_key

### Community 65 - "outbox-table.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 69 - "RecordDebitUseCaseTest"
Cohesion: 0.12
Nodes (9): RecordCreditUseCase, DebitOutcome, Recorded, Refused, RecordEntryCommand, RecordEntryResult, RecordCreditUseCaseTest, RecordDebitUseCaseTest (+1 more)

### Community 71 - "JdbcTransferRepositoryIntegrationTest"
Cohesion: 0.14
Nodes (3): JdbcTransferRepositoryIntegrationTest, aTransfer(), microsNow()

### Community 73 - "NotificationConsumptionIntegrationTest"
Cohesion: 0.10
Nodes (3): NotificationConsumptionIntegrationTest, NotificationTestPropertiesSupport, NotificationApplication

### Community 74 - "ProcessedEventRepository"
Cohesion: 0.16
Nodes (4): 2. Módulos e camadas, JdbcKnownAccountRepository, KnownAccountRepository, InMemoryKnownAccountRepository

### Community 75 - "smoke.sh"
Cohesion: 0.57
Nodes (7): aws_local(), call(), check_events(), check_notification(), new_customer(), smoke.sh script, uuid()

### Community 78 - "StepCall"
Cohesion: 0.06
Nodes (27): AccountsAccountResponseDTO, AccountsEntryRequestDTO, AccountsEntryResponseDTO, AccountsLedgerEntryResponseDTO, AccountsProblemDTO, HttpAccountsGateway, RawResponse, AccountLookup (+19 more)

### Community 80 - "01-accounts-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 01-accounts-topic.sh script

### Community 81 - "StatementPage"
Cohesion: 0.09
Nodes (14): Etapa 5 — cache do extrato: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Revisão final da branch (Beatriz), Rulings deste plano, Task 1: porta do cache e casos de uso, Task 2: adapter Redis (+6 more)

### Community 82 - "OutboxRelayScheduler"
Cohesion: 0.09
Nodes (9): 0015 — Relay do outbox com líder único e SNS FIFO, Alternativas consideradas, Consequências, Contexto, Decisão, OutboxRelay, OutboxRelayScheduler, BlockingStore (+1 more)

### Community 83 - "OutboxStore"
Cohesion: 0.07
Nodes (14): Bloco 2 — relay e SNS FIFO, Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), JdbcOutboxStore, DiscardOutcome, ALREADY_DISCARDED, ALREADY_PUBLISHED, DISCARDED, NOT_FAILED (+6 more)

### Community 85 - "Foco de revisão"
Cohesion: 0.17
Nodes (11): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 3: Aplicação `accounts` com health do banco (Testcontainers), Task 4: Ambiente local (docker-compose) (+3 more)

### Community 87 - ".repeat"
Cohesion: 0.08
Nodes (12): JdbcOutboxStoreIntegrationTest, RedisCircuitBreakerTest, AccountNotFound, AccountsUnavailable, Created, CustomerBlocked, CustomerNotFound, CustomersUnavailable (+4 more)

### Community 88 - "RetryBackoff"
Cohesion: 0.05
Nodes (17): 2. Módulos e camadas, 4. Fluxos, RetryBackoff, GetTransferUseCase, RequestTransferUseCase, Outcome, INTERRUPTED, SKIPPED (+9 more)

### Community 89 - "OrderResource"
Cohesion: 0.27
Nodes (5): PersistenceUsesWeb, OrderResource, WebUsesPersistence, ApplicationUsesAdapters, DomainUsesAdapters

### Community 90 - "RunTransferSagaUseCaseTest"
Cohesion: 0.10
Nodes (3): RunTransferSagaUseCaseTest, Handler, Handler

### Community 94 - "StatementRepository"
Cohesion: 0.10
Nodes (12): Task 5: Consumo SQS e métrica da DLQ (Marina, `dev-kotlin`; paralela à Task 6), 2. Módulos e camadas, 5. Fluxo de consumo, StatementEventListener, ConflictingStatementEntryException, StatementRepository, RecordOutcome, DUPLICATE (+4 more)

### Community 95 - "AccountTypeAdminController.kt"
Cohesion: 0.14
Nodes (6): AccountTypeAdminController, ChangeAccountStatusDTO, ChangeAccountTypeDTO, ChangeOverdraftLimitDTO, RecordEntryDTO, AccountTypeLimitDTO

### Community 96 - "InMemoryTransferRepository"
Cohesion: 0.09
Nodes (5): InMemoryTransferRepository, Lease, InMemoryTransferRepositoryTest, CountPendingTransfersUseCaseTest, GetTransferUseCaseTest

### Community 98 - "TransferEventsIntegrationTest.kt"
Cohesion: 0.12
Nodes (3): ReceivedEvent, SentTransfer, TransferEventsIntegrationTest

### Community 99 - "NotificationKind"
Cohesion: 0.06
Nodes (29): Banco digital, Consulta e avisos, 10. Ambiente local e CI, 11. Documentação que acompanha, 1. Frentes de trabalho, 3. Modelo de domínio, 4. Fluxo de consumo, 5. Esquema do banco (`notification`) (+21 more)

### Community 100 - "MarkNotificationsReadUseCaseTest"
Cohesion: 0.20
Nodes (3): MarkNotificationsReadUseCase, MutableClock, MarkNotificationsReadUseCaseTest

### Community 102 - "AwsProfileConfigAssertions"
Cohesion: 0.12
Nodes (6): AwsProfileConfigAssertions, AwsProfileConfigIntegrationTest, AwsProfileConfigIntegrationTest, AwsProfileConfigIntegrationTest, AwsProfileConfigIntegrationTest, AwsProfileConfigIntegrationTest

### Community 103 - "OutboxRelayConfigurationSupport.kt"
Cohesion: 0.09
Nodes (9): Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), OutboxRelaySettings, AwsProperties, OutboxRelayConfigurationSupport, OutboxRelayProperties, OutboxRelaySettingsTest, OutboxRelayConfiguration, OutboxRelayConfiguration (+1 more)

### Community 105 - "StatementEntry"
Cohesion: 0.07
Nodes (24): Task 3: Domínio e casos de uso (Marina, `dev-kotlin`), 10. Documentação que acompanha, 1. Frentes de trabalho, 3. Modelo de domínio, 4. Tabela no DynamoDB, 6. API, nginx e Bruno, 7. Operação, 8. Testes (+16 more)

### Community 111 - "ApiClient"
Cohesion: 0.13
Nodes (3): OutboxRelayUnreachableBrokerIntegrationTest, ApiClient, ApiResponse

### Community 127 - "TransferSagaResumeIntegrationTest.kt"
Cohesion: 0.10
Nodes (3): AccountsStubs, TransferSagaResumeIntegrationTest, DailyLimitApiIntegrationTest

### Community 129 - "HttpCustomerGatewayTest"
Cohesion: 0.05
Nodes (13): HttpCustomerGatewayTest, Active, Behavior, Blocked, CustomerServiceStub, Lookup, NotFound, ServerError (+5 more)

### Community 130 - "RedisDailyLimitGatewayIntegrationTest.kt"
Cohesion: 0.08
Nodes (9): Outcome, EXCEEDED, FALLBACK_RESERVED, RESERVED, ReservationResult, EXCEEDED, RESERVED, UNAVAILABLE (+1 more)

### Community 131 - "StepRecord"
Cohesion: 0.21
Nodes (4): JdbcTransferStepLog, StepRecord, TransferStepLog, TransferStepLog

### Community 133 - "ResumeDueTransfersUseCaseTest"
Cohesion: 0.16
Nodes (3): InMemoryDailyLimitGateway, Reservation, ResumeDueTransfersUseCaseTest

### Community 134 - "Balance"
Cohesion: 0.07
Nodes (9): Ledger, AvailableBalance, Balance, Money, AccountStatusRulesTest, AccountTest, AvailableBalanceTest, BalanceTest (+1 more)

### Community 135 - "NotificationApiIntegrationTest"
Cohesion: 0.05
Nodes (8): NotificationIntegrationTest, JdbcKnownAccountRepositoryIntegrationTest, JdbcNotificationRepositoryIntegrationTest, JdbcProcessedEventRepositoryIntegrationTest, aNotification(), microsNow(), NotificationApiIntegrationTest, NotificationWebIntegrationTest

### Community 136 - "assertOutboxSchemaMatchesModel"
Cohesion: 0.05
Nodes (11): DemoRelayConfiguration, InfrastructureConfiguration, OutboxRelayConfigurationSupportTest, OutboxSchemaAssertionsIntegrationTest, assertOutboxSchemaMatchesModel(), createModelSchema(), currentSchema(), OutboxSchemaSnapshot (+3 more)

### Community 137 - "ServiceApiClients.kt"
Cohesion: 0.21
Nodes (3): NotificationApiClient, StatementApiClient, TransfersApiClient

### Community 138 - "CustomerAdminController.kt"
Cohesion: 0.12
Nodes (6): CustomerAdminController, ChangeCustomerStatusDTO, CustomerDTO, CustomerLookupDTO, RegisterCustomerDTO, InternalCustomerController

### Community 139 - "accounts/bootstrap/UseCaseConfiguration.kt"
Cohesion: 0.09
Nodes (8): 1. Módulos e camadas, 4. Fluxos, ChangeAccountStatusUseCase, ChangeAccountTypeUseCase, ChangeOverdraftLimitUseCase, ListAccountTypesUseCase, RecordDebitUseCase, UseCaseConfiguration

### Community 141 - "ProcessedEventRepository"
Cohesion: 0.13
Nodes (6): Task A6: API das notificações (Marina, `dev-kotlin`) — paralela à A5, IdGenerator, ProcessedEventRepository, ListNotificationsUseCase, InMemoryProcessedEventRepository, UseCaseConfiguration

### Community 143 - "OutboxAdminControllerIntegrationTest.kt"
Cohesion: 0.05
Nodes (6): DemoRelayConfiguration, OutboxAdminControllerIntegrationTest, TestApplication, DlqDepthMonitorTest, RedisStatementFirstPageCacheTest, AccountsResponseClassifierTest

### Community 144 - "AccountNotFoundException"
Cohesion: 0.13
Nodes (5): AccountNotFoundException, requireCanRead(), AccountView, GetAccountUseCase, GetAccountUseCaseTest

### Community 145 - "EndToEndEnvironment"
Cohesion: 0.18
Nodes (3): Task 4: ambiente local, e2e e guia, Task 1: `libs/testing-redis` e ambiente, EndToEndEnvironment

### Community 146 - "StatementIntegrationTest"
Cohesion: 0.10
Nodes (3): LocalStackTestSupportIntegrationTest, StatementIntegrationTest, StatementWebIntegrationTest

### Community 152 - "TransferSagaScheduler"
Cohesion: 0.14
Nodes (3): TransferSagaScheduler, TransferSagaSchedulerConfiguration, TransferSagaSchedulerTest

### Community 153 - "Customer"
Cohesion: 0.19
Nodes (7): 4. Subir os serviços `customer`, `accounts`, `transfers`, `notification` e `statement`, 2. Serviço `customer`, OutboxEventAppender, CustomerBlocked, CustomerEvent, CustomerRegistered, CustomerUnblocked

### Community 154 - ".get"
Cohesion: 0.15
Nodes (7): AccountOpened, EventEnvelopeReader, Ignored, LedgerEntryRecorded, MalformedEventException, ReceivedEvent, EventEnvelopeReaderTest

### Community 155 - "TransferMetricsConfiguration.kt"
Cohesion: 0.19
Nodes (5): JdbcPendingTransferStats, PendingTransfers, TransferMetrics, TransferMetricsConfiguration, TransferMetricsProperties

### Community 157 - "JdbcRepositoriesIntegrationTest.kt"
Cohesion: 0.10
Nodes (9): OutboxEventAppender, EventOutbox, IdGenerator, TransactionRunner, AccountBlocked, AccountClosed, AccountEvent, AccountTypeChanged (+1 more)

### Community 161 - "StepOutcome"
Cohesion: 0.10
Nodes (14): Etapa 6 — limite diário: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Revisão final da branch (Beatriz), Rulings deste plano, Task 2: domínio, saga e migração, Task 4: endpoint, e2e, smoke e Bruno (+6 more)

### Community 163 - "uuid"
Cohesion: 0.03
Nodes (22): Caller, Customer, currentBalance(), CallerHasNoCustomerException, CustomerNotFoundException, EventOutbox, TransactionRunner, RegisterCustomerResult (+14 more)

### Community 169 - "TransferAdminController.kt"
Cohesion: 0.18
Nodes (4): CompensateManuallyDTO, ManualCompensationDTO, StuckTransferDTO, TransferAdminController

### Community 173 - "TransferStep"
Cohesion: 0.05
Nodes (39): Transferências, Etapa 2 — `transfers` + saga Implementation Plan, Global Constraints, Review Focus, Task 8: Domínio da transferência e passos da saga (Marina, `dev-kotlin`), 10. Documentação que acompanha, 3. Modelo de domínio, 5. Esquema do banco (`transfers`) (+31 more)

### Community 175 - "OutboxMessage"
Cohesion: 0.07
Nodes (17): 10. Documentação que acompanha, 3. Esquema do banco, 5. Outbox e relay (`libs/outbox`), 6. API, 7. Testes, 8. Ambiente local e CI, 9. Entrega em dois blocos, Critério de pronto (+9 more)

### Community 178 - "ApiClient"
Cohesion: 0.16
Nodes (4): CustomerEventsIntegrationTest, OutboxRow, ApiClient, ApiResponse

### Community 179 - "CustomerLookup"
Cohesion: 0.12
Nodes (10): CustomerProblemDTO, CustomerStatusResponseDTO, HttpCustomerGateway, CustomerGateway, CustomerLookup, ACTIVE, BLOCKED, NOT_FOUND (+2 more)

### Community 181 - "CustomerRepository"
Cohesion: 0.14
Nodes (5): CustomerRepository, ChangeCustomerStatusUseCase, GetCustomerUseCase, GetOwnCustomerUseCase, UseCaseConfiguration

### Community 182 - "StuckTransferAdminUseCasesTest.kt"
Cohesion: 0.05
Nodes (17): Task 9: Portas e execução da saga (Marina, `dev-kotlin`), JdbcStuckTransferFinder, OutboxEventAppender, CorrelationContext, EventOutbox, StuckTransfer, StuckTransferFinder, FakeEventOutbox (+9 more)

### Community 184 - "02-transfers-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 02-transfers-topic.sh script

### Community 186 - "accounts/adapters/web/OpenApiConfiguration.kt"
Cohesion: 0.10
Nodes (7): ApiDocs, OpenApiConfiguration, OpenApiConfiguration, OpenApiConfiguration, OpenApiConfiguration, ApiDocs, OpenApiConfiguration

### Community 187 - "transfer"
Cohesion: 0.22
Nodes (7): transfer, transfer_due_idx, transfer_step, transfer_step_transfer_idx, daily_limit_epoch, transfer_source_limit_day_idx, transfer_manual_resolution

### Community 188 - "TestHttp.kt"
Cohesion: 0.16
Nodes (5): ApiDocsIntegrationTest, Disabled, Enabled, GroupsClaimIntegrationTest, TestHttp

### Community 189 - "GetStatementUseCaseTest"
Cohesion: 0.11
Nodes (12): Etapa 4 — `statement`: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Task 1: Esqueleto Gradle e cliente DynamoDB no apoio de testes (Rafael, `infra`), Task 2: LocalStack, compose, nginx e CI (Rafael, `infra`), Task 4: Repositório DynamoDB e bootstrap (Marina, `dev-kotlin`), Task 7: e2e, smoke e Bruno (Marina, `dev-kotlin`) (+4 more)

### Community 191 - "03-notification-queues.sh"
Cohesion: 0.39
Nodes (7): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, create_consumer_queue(), queue_arn(), 03-notification-queues.sh script

### Community 192 - "V3__outbox.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 198 - "statement/bootstrap/DlqMetricsConfiguration.kt"
Cohesion: 0.20
Nodes (3): QueueProperties, DlqDepthRefresher, DlqMetricsConfiguration

### Community 200 - "NotificationController.kt"
Cohesion: 0.13
Nodes (8): MarkedNotificationsDTO, MarkNotificationsReadDTO, NotificationPageDTO, NotificationController, NotificationCursorCodec, InvalidField, InvalidRequestException, NotificationPage

### Community 201 - "loggerfactory"
Cohesion: 0.09
Nodes (3): CorrelationId, NotificationEventListenerTest, StatementEventListenerTest

### Community 202 - "0014 — Tipo de conta e cheque especial"
Cohesion: 0.40
Nodes (5): 0014 — Tipo de conta e cheque especial, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 203 - "NotificationText"
Cohesion: 0.12
Nodes (3): NotificationContent, NotificationText, NotificationTextTest

### Community 204 - ".request"
Cohesion: 0.24
Nodes (11): IdempotencyKeyHeader, AccountsUnavailableException, CustomerBlockedException, CustomerNotFoundException, CustomersUnavailableException, InvalidField, InvalidRequestException, OwnAccountNotFoundException (+3 more)

### Community 205 - "0007 — Conta e ledger no mesmo serviço (`accounts`)"
Cohesion: 0.50
Nodes (4): 0007 — Conta e ledger no mesmo serviço (`accounts`), Consequências, Contexto, Decisão

### Community 207 - "ProblemResponsesOpenApiCustomizerTest.kt"
Cohesion: 0.10
Nodes (3): ObservabilityAutoConfigurationTest, SqsCorrelationIdAutoConfigurationTest, DigitalBankingSecurityAutoConfigurationTest

### Community 214 - ".decode"
Cohesion: 0.19
Nodes (5): Task 6: API do extrato (Marina, `dev-kotlin`; paralela à Task 5), InvalidCursorException, StatementCursorCodec, StatementCursorCodecTest, StatementCursor

### Community 217 - "AccountsIntegrationTest"
Cohesion: 0.15
Nodes (3): AccountsIntegrationTest, UpdateGrantsIntegrationTest, UseCaseConfigurationIntegrationTest

### Community 220 - "0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS"
Cohesion: 0.40
Nodes (5): 0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 223 - "OpenAccountUseCaseTest.kt"
Cohesion: 0.08
Nodes (20): Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`), CallerNotCustomerException, CustomerAlreadyHasAccountException, CustomerBlockedException, CustomerNotFoundException, CustomersUnavailableException, IdempotencyKeyReusedException, PendingTransfersException (+12 more)

### Community 225 - "Banco digital em microsserviços"
Cohesion: 0.11
Nodes (14): Agentes do projeto, Ao terminar uma tarefa, Autonomia, Banco digital em microsserviços, Contexto, Fluxo de trabalho (skills), Git, Grafo do código (graphify) (+6 more)

### Community 227 - "04-statement.sh"
Cohesion: 0.43
Nodes (6): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, queue_arn(), 04-statement.sh script

### Community 229 - "AccountStatusAdminController.kt"
Cohesion: 0.16
Nodes (7): AccountStatusAdminController, IdempotencyKeyHeader, AccountTypeNotFoundException, EntryNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException

### Community 230 - "Etapa 0 — Fundação: design"
Cohesion: 0.22
Nodes (9): 2. Build e convenções, 4. Ambiente local, 5. CI (GitHub Actions), Critério de pronto, Etapa 0 — Fundação: design, Fora de escopo (e onde foi registrado), Objetivo, Restrições (de `CLAUDE.md` e ADRs) (+1 more)

### Community 231 - "Etapa 6 — limite diário: design"
Cohesion: 0.13
Nodes (15): 1. Fonte da verdade no PostgreSQL, 2. Contador no Redis (script Lua), 3. Passo `ReserveLimit` da saga, 4. Trava única e fallback no PostgreSQL, 5. Endpoint de consulta, 6. Observabilidade, 7. Testes, 8. Ambiente local, CI e documentação (+7 more)

### Community 232 - "content"
Cohesion: 0.19
Nodes (3): PendingTransfersDTO, PendingTransfersController, CountPendingTransfersUseCase

### Community 233 - "SecurityProblems.kt"
Cohesion: 0.19
Nodes (3): ProblemDetailsAccessDeniedHandler, ProblemDetailsAuthenticationEntryPoint, SecurityProblems

### Community 234 - "ConsumeEventUseCase"
Cohesion: 0.11
Nodes (16): Etapa 3 — Mensageria e `notification` Implementation Plan, Frente A — `notification`, Frentes e ordem, Review Focus, Task A10: Documentação (Clara, `documentador`) — paralela desde o início, Task A1: Esqueleto do `notification` e prova do `@SqsListener` (Marina, `dev-kotlin`), Task A2: Domínio das notificações (Marina, `dev-kotlin`), Task A3: Casos de uso e portas (Marina, `dev-kotlin`) (+8 more)

### Community 235 - "Frente B — débitos técnicos"
Cohesion: 0.29
Nodes (6): Frente B — débitos técnicos, Task B1: Configuração do relay em `libs/outbox-relay` (Marina, `dev-kotlin`) — cartão #92, Task B2: Backoff do relay e testes (Marina, `dev-kotlin`) — cartões #24 e #26, depois da B1, Task B3: Domínio e persistência do `transfers` (Marina, `dev-kotlin`) — cartões #95 e #93, Task B4: Aplicação e adapters do `transfers` (Marina, `dev-kotlin`) — cartão #94, depois da B3, Task B5: Endurecer os testes ponta a ponta (Marina, `dev-kotlin`) — cartão #96, antes da A9

### Community 236 - "notification/bootstrap/DlqMetricsConfiguration.kt"
Cohesion: 0.18
Nodes (4): QueueProperties, DlqDepthMonitor, DlqDepthProbe, DlqMetricsConfiguration

### Community 237 - "AccountsApplicationIntegrationTest.kt"
Cohesion: 0.17
Nodes (4): AccountsApplicationIntegrationTest, ParameterizedTypeReference, RequestThreadProbe, RequestThreadProbeConfiguration

### Community 238 - "AccountsClientProperties"
Cohesion: 0.14
Nodes (11): Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2), Task 10: Pedir, consultar e retomar transferências (Marina, `dev-kotlin`), Task 11: Persistência do `transfers` (Marina, `dev-kotlin`), Task 12: Cliente HTTP do `accounts` (Marina, `dev-kotlin`), Task 13: API HTTP do `transfers` (Marina, `dev-kotlin`), Task 14: Agendador, relay e eventos no `transfers` (Marina, `dev-kotlin`), Task 15: Infra local, Bruno, smoke e CI do `transfers` (Rafael, `infra`), Task 7: Esqueleto do `transfers` (Marina, `dev-kotlin`) (+3 more)

### Community 241 - "DailyLimitProperties"
Cohesion: 0.16
Nodes (4): CircuitBreaker, DailyLimitProperties, DailyLimitScripts, DailyLimitPropertiesBindingTest

### Community 244 - ".get"
Cohesion: 0.16
Nodes (5): StatementDTO, StatementEntryDTO, StatementController, InvalidField, InvalidRequestException

### Community 250 - "loggerfactory"
Cohesion: 0.22
Nodes (5): NotificationEventListener, ConsumeOutcome, DUPLICATE, IGNORED, PROCESSED

### Community 252 - "Status do projeto"
Cohesion: 0.33
Nodes (6): Ambiente do Claude, Como rodar localmente, Como usar o board, Concluído, Convenções que já custaram caro, Status do projeto

### Community 253 - "DailyLimitController.kt"
Cohesion: 0.14
Nodes (8): DailyLimitController, DailyLimitDTO, DailyLimitUsage, AccountNotFound, AccountsUnavailable, DailyLimitOutcome, Found, GetDailyLimitUseCase

### Community 256 - "0016 — Saga de transferência orquestrada por HTTP síncrono"
Cohesion: 0.40
Nodes (5): 0016 — Saga de transferência orquestrada por HTTP síncrono, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 257 - "V1__notification_schema.sql"
Cohesion: 0.50
Nodes (4): known_accounts, notifications, notifications_customer_page_idx, processed_events

### Community 259 - "send"
Cohesion: 0.22
Nodes (4): AccountsApiClient, CustomerApiClient, HttpResult, send()

### Community 261 - "assertProblem"
Cohesion: 0.19
Nodes (4): ApiClient, ApiResponse, assertProblem(), StatementOwnershipIntegrationTest

### Community 263 - "0018 — Extrato ordenado pela sequência do ledger"
Cohesion: 0.40
Nodes (5): 0018 — Extrato ordenado pela sequência do ledger, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 264 - "Bloco 1 — `accounts` gravando no outbox"
Cohesion: 0.14
Nodes (11): Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 13: Documentação do bloco 1 e das decisões da etapa (Clara, `documentador`; em paralelo às Tasks 8–12), Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`), Task 2: Valores monetários e tipos (Marina, `dev-kotlin`), Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`) (+3 more)

### Community 271 - "Etapa 5 — cache do extrato: design"
Cohesion: 0.10
Nodes (17): 1. Porta e casos de uso, 2. Adapter Redis, 3. Consistência, 4. Observabilidade, 5. Configuração, 6. Testes, 7. Ambiente local e CI, 8. Documentação que acompanha (+9 more)

### Community 275 - "SecurityTestApplication.kt"
Cohesion: 0.16
Nodes (3): CatchAllExceptionHandler, SecurityTestApplication, SecurityTestController

### Community 276 - "NotificationAccessIntegrationTest"
Cohesion: 0.13
Nodes (4): ApiClient, ApiResponse, assertProblem(), NotificationAccessIntegrationTest

### Community 277 - "OutboxMetrics"
Cohesion: 0.16
Nodes (4): 1. Entrega em dois blocos, OutboxMetrics, OutboxBacklog, OutboxMetricsTest

### Community 280 - "SagaIntegrationSupport.kt"
Cohesion: 0.18
Nodes (4): awaitStatus(), deleteTransfers(), outboxCorrelationIds(), statusOf()

### Community 281 - "0002 — Clean Architecture por serviço"
Cohesion: 0.40
Nodes (5): 0002 — Clean Architecture por serviço, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 283 - "StatementCacheProperties"
Cohesion: 0.12
Nodes (5): ServiceTokenAutoConfiguration, ServiceTokenProperties, StatementCacheProperties, StatementCachePropertiesTest, CacheConfiguration

### Community 284 - "TransferAdminExceptionHandler.kt"
Cohesion: 0.26
Nodes (7): CompensationCreditAlreadyUsedException, CompensationCreditMismatchException, CompensationCreditNotFoundException, TransferAdminExceptionHandler, TransferBusyException, TransferNotCompensatingException, TransferNotPendingException

### Community 287 - "SecurityRules"
Cohesion: 0.16
Nodes (4): SecurityRules, CustomerSecurityConfiguration, NotificationSecurityConfiguration, StatementSecurityConfiguration

### Community 290 - "CustomerController.kt"
Cohesion: 0.17
Nodes (4): CustomerController, RegisterCustomerCommand, RegisterCustomerUseCase, RegisterCustomerUseCaseTest

### Community 300 - "accounts/adapters/customer/HttpCustomerGateway.kt"
Cohesion: 0.20
Nodes (3): CustomerStatusResponseDTO, PendingTransfersResponseDTO, HttpTransfersGateway

### Community 302 - "Bloqueio e encerramento de conta: design"
Cohesion: 0.15
Nodes (11): Bloqueio e encerramento de conta: plano de implementação, Global Constraints, Review Focus, Revisão final da branch (Beatriz), Abordagens consideradas, Bloqueio e encerramento de conta: design, Decisões (usuário, 2026-11-16), Desenho (+3 more)

### Community 303 - "TransferController.kt"
Cohesion: 0.23
Nodes (3): TransferDTO, TransferController, TransferFailedException

### Community 304 - "ServiceTokenClientIntegrationTest"
Cohesion: 0.13
Nodes (3): MutableClock, ServiceTokenClientIntegrationTest, TokenRequest

### Community 305 - "Service"
Cohesion: 0.19
Nodes (5): Service, O que a lib faz sozinha, CallerRoles, ApplicationWithSpring, AccountsSecurityConfiguration

### Community 306 - "status.md"
Cohesion: 0.15
Nodes (5): 0009 — Cache do extrato, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 313 - "Ordem e paralelismo"
Cohesion: 0.13
Nodes (14): Etapa 7 — borda e identidade: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Revisão final da branch (Beatriz), Rulings deste plano, Task 2: ambiente local de identidade, Task 3: Terraform do Cognito e do API Gateway (sem aplicar) (+6 more)

### Community 318 - "Etapa 7 — borda e identidade: design"
Cohesion: 0.13
Nodes (15): 10. Documentação, 1. Identidade e o id do cliente, 4. Consulta ao `customer` no pedido, 5. `libs/observabilidade`, 6. Segurança transversal, 7. Ambiente local, 8. Terraform (sem aplicar), 9. Testes (+7 more)

### Community 321 - "RedisCircuitBreaker"
Cohesion: 0.23
Nodes (5): RedisCircuitBreaker, State, CLOSED, HALF_OPEN, OPEN

### Community 324 - "accounts/adapters/persistence/UuidV7IdGenerator.kt"
Cohesion: 0.15
Nodes (3): UuidV7IdGenerator, UuidV7IdGenerator, UuidV7IdGenerator

### Community 329 - "CorrelationIdRestClientInterceptor"
Cohesion: 0.17
Nodes (3): libs/observabilidade, CorrelationIdRestClientInterceptor, CustomerClientConfiguration

### Community 338 - "ReceivedEvent"
Cohesion: 0.18
Nodes (10): AccountBlocked, AccountClosed, AccountOpened, AccountTypeChanged, AccountUnblocked, ReceivedEvent, TransferCompensated, TransferCompleted (+2 more)

### Community 340 - "OrderController.kt"
Cohesion: 0.25
Nodes (4): OrderController, PlaceOrderCommand, PlaceOrderResult, PlaceOrderUseCase

### Community 344 - "05-customer-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 05-customer-topic.sh script

### Community 348 - "0010 — Limite diário com Redis + Lua"
Cohesion: 0.40
Nodes (5): 0010 — Limite diário com Redis + Lua, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 349 - "0020 — Identidade e autorização"
Cohesion: 0.40
Nodes (5): 0020 — Identidade e autorização, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 350 - "TransfersSecurityConfiguration.kt"
Cohesion: 0.32
Nodes (4): Ordem e paralelismo, Task 2: `transfers`: pendentes por conta e saga com conta bloqueada ou encerrada, Task 3: `notification`: notificações de status da conta, TransfersSecurityConfiguration

### Community 352 - "token.sh"
Cohesion: 0.83
Nodes (3): request(), token.sh script, usage()

### Community 353 - "V2__outbox.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 375 - ".outbox"
Cohesion: 0.33
Nodes (4): Etapa 1 — `accounts` Implementation Plan, Global Constraints, Review Focus, EventOutbox

### Community 376 - "AccountsApplication.kt"
Cohesion: 0.40
Nodes (3): 3. Esqueleto do `accounts`, Testes (no `bootstrap`, que enxerga todos os módulos), AccountsApplication

### Community 379 - "0004 — API Gateway HTTP API na borda"
Cohesion: 0.40
Nodes (5): 0004 — API Gateway HTTP API na borda, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 380 - "0019 — Runtime na AWS com EKS"
Cohesion: 0.40
Nodes (5): 0019 — Runtime na AWS com EKS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 381 - "0023 — Status da conta e encerramento"
Cohesion: 0.40
Nodes (5): 0023 — Status da conta e encerramento, Alternativas consideradas, Consequências, Contexto, Decisão

## Knowledge Gaps
- **89 isolated node(s):** `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` (+84 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 1053 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **219 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Transfer` connect `TransferStep` to `RequestTransferUseCaseTest`, `ResumeDueTransfersUseCaseTest`, `.transfer`, `CompensateTransferManuallyUseCase`, `OverdraftLimit`, `TransferMetricsIntegrationTest.kt`, `JdbcTransferRepository`, `TransferAdminExceptionHandler.kt`, `StepOutcome`, `uuid`, `TransferAdminController.kt`, `TransferController.kt`, `StuckTransferAdminUseCasesTest.kt`, `Money`, `JdbcTransferRepositoryIntegrationTest`, `.request`, `StepCall`, `SpringTransactionRunnerIntegrationTest`, `.repeat`, `RetryBackoff`, `RunTransferSagaUseCaseTest`, `InMemoryTransferRepository`, `BlockingRepository`, `AccountsClientProperties`?**
  _High betweenness centrality (0.049) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Transfer` (e.g. with `Transferências` and `Global Constraints`) actually correct?**
  _`Transfer` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION` to the rest of the system?**
  _89 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AccountApiIntegrationTest` be split into smaller, more focused modules?**
  _Cohesion score 0.11153846153846154 - nodes in this community are weakly interconnected._
- **Why does `Caller` connect `uuid` to `RequestTransferUseCaseTest`, `Account`, `ProcessedEventRepository`, `LedgerEntry`, `Etapa 5 — cache do extrato: design`, `AccountNotFoundException`, `OverdraftLimit`, `Decisões tomadas`, `AccountController.kt`, `TransferAdminController.kt`, `ServiceTokenClient`, `Service`, `Ordem e paralelismo`, `GetStatementUseCaseTest`, `Etapa 7 — borda e identidade: design`, `CallerAuthentication.kt`, `RetryBackoff`, `0020 — Identidade e autorização`, `OpenAccountUseCaseTest.kt`, `TransfersIntegrationTest`, `CallerResolver`, `Status do projeto`, `DailyLimitController.kt`?**
  _High betweenness centrality (0.044) - this node is a cross-community bridge._
- **Should `ServiceArchitectureRulesTest` be split into smaller, more focused modules?**
  _Cohesion score 0.05126050420168067 - nodes in this community are weakly interconnected._
- **Why does `TransfersIntegrationTest` connect `TransfersIntegrationTest` to `RedisDailyLimitGatewayIntegrationTest.kt`, `TransferApiIntegrationTest`, `assertOutboxSchemaMatchesModel`, `RedisDailyLimitGatewayIntegrationTest`, `TransferMetricsIntegrationTest.kt`, `TransferAuthorizationIntegrationTest`, `TransferLeaseContentionIntegrationTest.kt`, `RedisTestSupportIntegrationTest`, `SagaIntegrationSupport.kt`, `TransferAdminApiIntegrationTest`, `HttpAccountsGatewayIntegrationTest.kt`, `uuid`, `JwtTestSupport.kt`, `TransfersApplicationIntegrationTest.kt`, `OutboxMessage`, `Money`, `JdbcTransferRepositoryIntegrationTest`, `TransferSchemaIntegrationTest`, `SpringTransactionRunnerIntegrationTest`, `OpenApiIntegrationTest`, `DailyLimitSagaIntegrationTest.kt`, `transfers/bootstrap/ClockWiringIntegrationTest.kt`, `TransferEventsIntegrationTest.kt`, `AccountsClientProperties`, `BlockedOrClosedAccountSagaIntegrationTest.kt`, `JdbcTransferStepLogIntegrationTest.kt`, `transfers/bootstrap/web/AwsProfileIntegrationTest.kt`, `TransferSagaResumeIntegrationTest.kt`, `RedisDailyLimitGatewayResilienceIntegrationTest.kt`?**
  _High betweenness centrality (0.032) - this node is a cross-community bridge._