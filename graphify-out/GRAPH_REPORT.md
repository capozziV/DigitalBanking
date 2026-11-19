# Graph Report - etapa-8  (2026-11-19)

## Corpus Check
- 847 files · ~269,122 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 75 file(s) not represented in the graph (top: .bru 50, (none) 14, .tpl 2)

## Summary
- 7494 nodes · 18616 edges · 476 communities (204 shown, 272 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 1228 edges (avg confidence: 0.88)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `d448cc42`
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
- check-messaging.py
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
- HttpCustomerGatewayIntegrationTest.kt
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
- StuckTransferAdminUseCasesTest
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
- transfers/bootstrap/UseCaseConfiguration.kt
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
- AwsProfileConfigAssertions
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
- RedisDailyLimitGatewayIntegrationTest.kt
- StepRecord
- DynamoDbStatementRepositoryIntegrationTest
- ResumeDueTransfersUseCaseTest
- Balance
- aNotification
- assertOutboxSchemaMatchesModel
- TokenIssuer
- CustomerAdminController.kt
- UseCaseConfiguration
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
- accounts/bootstrap/contract/HttpContractBase.kt
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
- TransferEvent
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
- ObservabilityAutoConfigurationTest.kt
- OpenApiIntegrationTest
- status.md
- SpringTransactionRunner
- CreditStepTest
- .entryCreated
- .decode
- OutboxAdminController.kt
- TransactionRunner
- AccountsIntegrationTest
- StatementEntryTest
- 0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS
- OpenAccountUseCaseTest.kt
- TransfersIntegrationTest
- Banco digital em microsserviços
- .brl
- 04-statement.sh
- BlockingRepository
- AccountStatusAdminController.kt
- Etapa 0 — Fundação: design
- Etapa 6 — limite diário: design
- transfers/bootstrap/contract/HttpContractBase.kt
- SecurityProblems.kt
- EventContractsTest.kt
- Frente B — débitos técnicos
- notification/bootstrap/DlqMetricsConfiguration.kt
- AccountsApplicationIntegrationTest.kt
- Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)
- DynamoDbConfiguration.kt
- Transfer.kt
- DailyLimitProperties
- BlockedOrClosedAccountSagaIntegrationTest.kt
- .get
- header
- HttpTransfersGatewayTest
- ReserveLimitStepTest
- loggerfactory
- StatementEntry
- StuckTransfer
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
- CorrelationIdFilter
- AccountTypeTest.kt
- SchedulingConfiguration.kt
- EntryKindTest.kt
- notification/bootstrap/SchedulingConfiguration.kt
- ProblemResponsesOpenApiCustomizer.kt
- Etapa 5 — cache do extrato: design
- Money
- GetDailyLimitUseCaseTest
- DailyLimitEndToEndIntegrationTest.kt
- SecurityTestApplication.kt
- NotificationAccessIntegrationTest
- OutboxMetrics
- RedisTestSupportIntegrationTest
- DynamoDbStatementRepositoryTest.kt
- HttpAccountsGatewayIntegrationTest.kt
- 0002 — Clean Architecture por serviço
- OpenApiIntegrationTest
- StatementCacheProperties
- TransferAdminExceptionHandler.kt
- AccountOpeningCustomerIntegrationTest
- SecurityRules
- ProcessLogsOnFailureExtension
- RedisStatementFirstPageCacheIntegrationTest.kt
- RegisterCustomerUseCaseTest
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
- CustomerLookup
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
- HttpCustomerGatewayTest.kt
- JdbcCustomerRepository
- GetCustomerUseCaseTest
- OpenApiIntegrationTest
- CustomerTest
- TransfersClientConfiguration.kt
- ChangeAccountTypeUseCaseTest
- NotificationApiIntegrationTest
- AccountsResponseClassifierTest
- ObservabilityAutoConfiguration.kt
- OrderController.kt
- TransfersDatabase
- LocalStackInitScripts
- RequestTransferDTO
- 05-customer-topic.sh
- CallerTest.kt
- JdbcOutboxWriter
- HttpAccountsGateway
- 0010 — Limite diário com Redis + Lua
- 0020 — Identidade e autorização
- TransfersSecurityConfiguration.kt
- CustomerServiceStub
- token.sh
- V2__outbox.sql
- seed.sh
- seed-check.sh
- accounts/bootstrap/AwsProfileIntegrationTest.kt
- ApiDocs.kt
- V1__customer.sql
- NotificationIntegrationTest
- customer/bootstrap/AwsProfileIntegrationTest.kt
- notification/bootstrap/web/AwsProfileIntegrationTest.kt
- statement/bootstrap/web/AwsProfileIntegrationTest.kt
- OutboxRelayConfigurationSupportTest.kt
- transfers/bootstrap/web/AwsProfileIntegrationTest.kt
- .outbox
- AccountsApplication.kt
- RedisStatementFirstPageCache
- LedgerEntryRepository
- 0004 — API Gateway HTTP API na borda
- 0019 — Runtime na AWS com EKS
- 0023 — Status da conta e encerramento
- TransactionRunner
- JdbcOutboxStore
- StatementApplication.kt
- RetryBackoff
- AccountEventsContractTest.kt
- TransferStatus
- ResumeDueTransfersUseCase
- SnsEventPublisherIntegrationTest.kt
- customer/bootstrap/ArchitectureTest.kt
- RunTransferSagaUseCase
- OutboxMessagingContractConfiguration.kt
- customer/bootstrap/contract/HttpContractBase.kt
- Etapa 9: frontend do cliente (design)
- SecurityRules
- Ordem e paralelismo
- Etapa 8 — acabamento: design
- EventEnvelopeReaderTest
- ManualResolution
- OutboxEventAppenderIntegrationTest.kt
- .run
- properties
- ChangeAccountStatusUseCaseTest
- LocalStackTestSupportIntegrationTest.kt
- serviceRef
- TransferRepository
- properties
- CorrelationIdSqsMessageInterceptor
- AccountRequestDTOs.kt
- IdGenerator
- ServiceArchitectureRules
- OutboxRelayUnreachableBrokerIntegrationTest.kt
- deploy-services.sh
- InternalCustomerController.kt
- CustomerEventsIntegrationTest
- EntryKind
- OutboxWriterClockIntegrationTest.kt
- ManualCompensationTest
- ServiceArchitectureRulesTest.kt
- matchExpressions
- Review Focus
- ServiceTokenAutoConfiguration.kt
- Handler
- TransfersServiceStub
- InMemoryDailyLimitGateway
- networking
- status
- Banco digital em microsserviços
- JdbcKnownAccountRepositoryIntegrationTest.kt
- DlqDepthMonitorTest
- Chart Helm dos serviços
- accounts/bootstrap/ArchitectureTest.kt
- DigitalBankingSecurityAutoConfigurationTest
- ApiDocsAutoConfiguration.kt
- OutboxEventAppender
- InMemoryCustomerRepository
- statement/bootstrap/ArchitectureTest.kt
- JdbcPendingTransferStats.kt
- transfers/bootstrap/ArchitectureTest.kt
- ResolveDebitStepTest
- helm/validate.sh
- test-local.sh
- Bloco 1 — preparação para o 2º serviço (branch `etapa-2-transfers`, PR 1)
- InMemoryTransferRepositoryTest
- RetryBackoffTest
- create-service-databases.sh
- nodeSelector
- spec
- 0001 — Stack: Kotlin + Spring Boot 4 na AWS
- 0021 — Deploy e ambiente efêmero na AWS
- cognito-user.sh
- helm-teardown.test.sh
- reaper-decision.sh
- Test
- OutboxContractDatabase.kt
- .localStack
- DataCentsMigrationIntegrationTest
- targetGroupARN
- leftovers.sh
- reaper-check.test.sh
- reaper-decision.test.sh
- NotificationApplication
- FakeAccountsGatewayTest
- fetch-secrets.sh
- leftovers.test.sh
- Customer
- helm-teardown.sh
- reaper-check.sh
- bootstrap/validate.sh
- kubernetes-version.sh
- aws/validate.sh

## God Nodes (most connected - your core abstractions)
1. `Transfer` - 137 edges
2. `Money` - 97 edges
3. `Account` - 93 edges
4. `assertProblem()` - 83 edges
5. `Caller` - 75 edges
6. `OverdraftLimit` - 73 edges
7. `LedgerEntry` - 72 edges
8. `TransfersIntegrationTest` - 68 edges
9. `Balance` - 67 edges
10. `TransferStep` - 66 edges

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

## Communities (476 total, 272 thin omitted)

### Community 1 - "SqsListenerFifoIntegrationTest.kt"
Cohesion: 0.14
Nodes (3): ListenersConfiguration, SqsListenerFifoIntegrationTest, TestListeners

### Community 7 - ".transfer"
Cohesion: 0.17
Nodes (3): TerminalStepsTest, TransferFixtures, TransferTest

### Community 8 - "Account"
Cohesion: 0.04
Nodes (39): Clientes e contas, Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`), Task 8: Esquema, usuários de banco e repositórios JDBC (Marina, `dev-kotlin`; o script de init do compose segue o padrão do Rafael), Task 1: `accounts`: status, bloqueio, encerramento e conta nova, JdbcAccountRepository, AccountBusyException, AccountNotFoundException, CallerNotCustomerException (+31 more)

### Community 9 - "check-messaging.py"
Cohesion: 0.09
Nodes (19): alb_matcher(), alb_routes(), main(), nginx_routes(), attributes(), expand(), Local, logical_lines() (+11 more)

### Community 10 - "CompensateTransferManuallyUseCase"
Cohesion: 0.12
Nodes (18): Accepted, AccountsUnavailable, Busy, Compensated, CompensateManuallyOutcome, CompensateTransferManuallyUseCase, CreditAlreadyUsed, CreditCheck (+10 more)

### Community 12 - "OutboxRelayIntegrationTest.kt"
Cohesion: 0.09
Nodes (3): OutboxRelayIntegrationTest, ReceivedEvent, RecordingPublisherConfiguration

### Community 13 - "CloseAccountUseCaseTest"
Cohesion: 0.10
Nodes (8): CustomerGateway, CustomerStatus, ACTIVE, BLOCKED, TransfersGateway, CloseAccountUseCase, StubTransfersGateway, CloseAccountUseCaseTest

### Community 14 - "LedgerEntry"
Cohesion: 0.06
Nodes (20): Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 13: Documentação do bloco 1 e das decisões da etapa (Clara, `documentador`; em paralelo às Tasks 8–12), Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`), Task 2: Valores monetários e tipos (Marina, `dev-kotlin`), Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`) (+12 more)

### Community 16 - "OutboxRelayTest"
Cohesion: 0.18
Nodes (3): FakeEventPublisher, MutableClock, OutboxRelayTest

### Community 17 - "OverdraftLimit"
Cohesion: 0.06
Nodes (21): Global Constraints, 1. Módulos e camadas, 2. Modelo de domínio, JdbcAccountTypeRepository, AccountTypeRepository, ChangeOverdraftLimitUseCase, ListAccountTypesUseCase, AccountTypeRepository (+13 more)

### Community 18 - "Decisões tomadas"
Cohesion: 0.12
Nodes (16): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Deploy na AWS, Em aberto (+8 more)

### Community 19 - "SecurityAutoConfigurationIntegrationTest"
Cohesion: 0.14
Nodes (3): SecurityAutoConfigurationIntegrationTest, assertProblem(), JwtTestSupportTest

### Community 21 - "PendingEvent"
Cohesion: 0.10
Nodes (14): 0022 — Testes de contrato com Stubborn Contract, Alternativas consideradas, Consequências, Contexto, Decisão, Bloco 2 — relay e SNS FIFO, Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), Task 2: Relay em módulo próprio e teste de esquema (Marina, `dev-kotlin`) — cartões #25 e #30 (+6 more)

### Community 22 - "TransferLeaseContentionIntegrationTest.kt"
Cohesion: 0.14
Nodes (3): TransferLeaseContentionIntegrationTest, ApiClient, PendingTransfersApiIntegrationTest

### Community 23 - "coordenador.md"
Cohesion: 0.29
Nodes (6): Ao começar uma sessão, Board, Para cada etapa do roadmap, Para cada tarefa, Paralelismo, Quando faltar algo

### Community 24 - "Terraform"
Cohesion: 0.12
Nodes (16): ALB do Terraform, nunca do controller, Bancos e usuários por serviço, Borda: limites e log de acesso, Cognito: domínio e PKCE, Decisões do ambiente efêmero, Estrutura, Etiquetas e `created_at`, Mensageria igual ao LocalStack (+8 more)

### Community 25 - "0005 — Frontend Angular e BFF em NestJS"
Cohesion: 0.33
Nodes (5): 0005 — Frontend Angular e BFF em NestJS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 26 - "0006 — Observabilidade só com correlation ID"
Cohesion: 0.40
Nodes (5): 0006 — Observabilidade só com correlation ID, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 29 - "Rodando o projeto localmente (macOS)"
Cohesion: 0.09
Nodes (22): 1. Configurar o ambiente, 2. Subir a infraestrutura (PostgreSQL, Redis, nginx, LocalStack, mock-oauth2-server), 3. Build e testes, 5. Conferir a API pelo nginx, 6. Ver os eventos na fila de inspeção, 7. Coleção Bruno, 8. Derrubar tudo, A mesma sequência à mão (+14 more)

### Community 30 - "AccountController.kt"
Cohesion: 0.15
Nodes (5): Regras inegociáveis, currentCaller(), AccountController, AccountDTO, LedgerEntryDTO

### Community 31 - "0008 — Extrato (`statement`) com CQRS no DynamoDB"
Cohesion: 0.50
Nodes (4): 0008 — Extrato (`statement`) com CQRS no DynamoDB, Consequências, Contexto, Decisão

### Community 33 - "HttpCustomerGatewayIntegrationTest.kt"
Cohesion: 0.08
Nodes (4): CustomerClientProperties, HttpCustomerGatewayIntegrationTest, RequireServiceToken, ServiceTokenServers

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
Cohesion: 0.10
Nodes (7): JdbcNotificationRepository, NotificationCursorCodec, NotificationCursor, NotificationPage, InMemoryNotificationRepository, Notification, NotificationTest

### Community 43 - "0011 — Contratos de API"
Cohesion: 0.50
Nodes (4): 0011 — Contratos de API, Consequências, Contexto, Decisão

### Community 44 - "Versões novas da etapa 1"
Cohesion: 0.50
Nodes (3): Observações, springdoc e Spring Boot 4.1.x, Versões novas da etapa 1

### Community 46 - "ServiceTokenClient"
Cohesion: 0.12
Nodes (9): Task 1: `libs/security` e `libs/observabilidade`, Configuração, libs/security, Módulos, Uso, CachedToken, RecentFailure, ServiceTokenClient (+1 more)

### Community 61 - "StuckTransferAdminUseCasesTest"
Cohesion: 0.13
Nodes (7): StuckTransferAdminUseCasesTest, CorrelationContext, EventOutbox, EntryKind, CREDIT, DEBIT, RecordedEntry

### Community 63 - "account"
Cohesion: 0.25
Nodes (4): account_type, account, ledger_entry, account_customer_id_not_closed_key

### Community 65 - "outbox-table.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 69 - "RecordDebitUseCaseTest"
Cohesion: 0.13
Nodes (6): RecordCreditUseCase, RecordEntryResult, SequentialIdGenerator, RecordCreditUseCaseTest, RecordDebitUseCaseTest, LedgerEntryRecorded

### Community 71 - "JdbcTransferRepositoryIntegrationTest"
Cohesion: 0.15
Nodes (3): JdbcTransferRepositoryIntegrationTest, aTransfer(), microsNow()

### Community 74 - "ProcessedEventRepository"
Cohesion: 0.17
Nodes (3): JdbcKnownAccountRepository, KnownAccountRepository, InMemoryKnownAccountRepository

### Community 75 - "smoke.sh"
Cohesion: 0.44
Nodes (8): aws_local(), call(), check_events(), check_notification(), new_customer(), new_user(), smoke.sh script, uuid()

### Community 78 - "StepCall"
Cohesion: 0.11
Nodes (14): AccountLookup, AccountsGateway, EntryLookup, Found, NotFound, Unavailable, FakeAccountsGateway, AccountsGateway (+6 more)

### Community 80 - "01-accounts-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 01-accounts-topic.sh script

### Community 81 - "StatementPage"
Cohesion: 0.07
Nodes (20): Etapa 5 — cache do extrato: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Revisão final da branch (Beatriz), Rulings deste plano, Task 1: porta do cache e casos de uso, Task 2: adapter Redis (+12 more)

### Community 82 - "OutboxRelayScheduler"
Cohesion: 0.11
Nodes (8): 0015 — Relay do outbox com líder único e SNS FIFO, Alternativas consideradas, Consequências, Contexto, Decisão, OutboxRelay, OutboxRelayScheduler, OutboxRelaySchedulerTest

### Community 83 - "OutboxStore"
Cohesion: 0.08
Nodes (11): DiscardOutcome, ALREADY_DISCARDED, ALREADY_PUBLISHED, DISCARDED, NOT_FAILED, NOT_FOUND, RELAY_BUSY, OutboxStore (+3 more)

### Community 85 - "Foco de revisão"
Cohesion: 0.17
Nodes (11): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 3: Aplicação `accounts` com health do banco (Testcontainers), Task 4: Ambiente local (docker-compose) (+3 more)

### Community 87 - ".repeat"
Cohesion: 0.05
Nodes (16): JdbcOutboxStoreIntegrationTest, ConcurrencyIntegrationTest, isAccountBusy(), retryWhileAccountBusy(), RetryWhileAccountBusyTest, RedisCircuitBreakerTest, AccountNotFound, AccountsUnavailable (+8 more)

### Community 88 - "transfers/bootstrap/UseCaseConfiguration.kt"
Cohesion: 0.12
Nodes (4): RequestTransferUseCase, SagaSettings, RetryTransferUseCase, UseCaseConfiguration

### Community 89 - "OrderResource"
Cohesion: 0.27
Nodes (5): PersistenceUsesWeb, OrderResource, WebUsesPersistence, ApplicationUsesAdapters, DomainUsesAdapters

### Community 94 - "StatementEntry"
Cohesion: 0.08
Nodes (22): 10. Documentação que acompanha, 1. Frentes de trabalho, 2. Módulos e camadas, 3. Modelo de domínio, 4. Tabela no DynamoDB, 5. Fluxo de consumo, 6. API, nginx e Bruno, 7. Operação (+14 more)

### Community 96 - "InMemoryTransferRepository"
Cohesion: 0.10
Nodes (4): InMemoryTransferRepository, Lease, CountPendingTransfersUseCaseTest, GetTransferUseCaseTest

### Community 98 - "TransferEventsIntegrationTest.kt"
Cohesion: 0.12
Nodes (3): ReceivedEvent, SentTransfer, TransferEventsIntegrationTest

### Community 99 - "NotificationKind"
Cohesion: 0.06
Nodes (30): Banco digital, Consulta e avisos, 10. Ambiente local e CI, 11. Documentação que acompanha, 1. Frentes de trabalho, 3. Modelo de domínio, 4. Fluxo de consumo, 5. Esquema do banco (`notification`) (+22 more)

### Community 101 - "EventEnvelopeReaderTest"
Cohesion: 0.08
Nodes (13): EventEnvelopeReader, MalformedEventException, EventEnvelopeReaderTest, AccountBlocked, AccountClosed, AccountOpened, AccountTypeChanged, AccountUnblocked (+5 more)

### Community 102 - "AwsProfileConfigAssertions"
Cohesion: 0.13
Nodes (6): AwsProfileConfigAssertions, AwsProfileConfigIntegrationTest, AwsProfileConfigIntegrationTest, AwsProfileConfigIntegrationTest, AwsProfileConfigIntegrationTest, AwsProfileConfigIntegrationTest

### Community 103 - "OutboxRelayConfigurationSupport.kt"
Cohesion: 0.09
Nodes (9): Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), OutboxRelaySettings, AwsProperties, OutboxRelayConfigurationSupport, OutboxRelayProperties, OutboxRelaySettingsTest, OutboxRelayConfiguration, OutboxRelayConfiguration (+1 more)

### Community 105 - "InMemoryStatementRepository"
Cohesion: 0.12
Nodes (5): Task 3: Domínio e casos de uso (Marina, `dev-kotlin`), 8. Testes, InMemoryStatementRepository, RecordStatementEntryUseCaseTest, RegisterAccountUseCaseTest

### Community 111 - "ApiClient"
Cohesion: 0.13
Nodes (3): ClockWiringIntegrationTest, ApiClient, ApiResponse

### Community 129 - "AccountsProxy.kt"
Cohesion: 0.14
Nodes (3): AccountsProxy, HeldRequest, Hold

### Community 130 - "RedisDailyLimitGatewayIntegrationTest.kt"
Cohesion: 0.08
Nodes (9): Outcome, EXCEEDED, FALLBACK_RESERVED, RESERVED, ReservationResult, EXCEEDED, RESERVED, UNAVAILABLE (+1 more)

### Community 131 - "StepRecord"
Cohesion: 0.11
Nodes (7): Task 9: Portas e execução da saga (Marina, `dev-kotlin`), JdbcTransferStepLog, StepRecord, TransferStepLog, InMemoryTransferStepLog, JdbcTransferStepLogIntegrationTest, TransferStepLog

### Community 134 - "Balance"
Cohesion: 0.07
Nodes (9): Ledger, AvailableBalance, Balance, Money, AccountStatusRulesTest, AccountTest, AvailableBalanceTest, BalanceTest (+1 more)

### Community 135 - "aNotification"
Cohesion: 0.13
Nodes (4): JdbcNotificationRepositoryIntegrationTest, JdbcProcessedEventRepositoryIntegrationTest, aNotification(), microsNow()

### Community 136 - "assertOutboxSchemaMatchesModel"
Cohesion: 0.12
Nodes (7): OutboxSchemaAssertionsIntegrationTest, assertOutboxSchemaMatchesModel(), createModelSchema(), currentSchema(), OutboxSchemaSnapshot, OutboxSchemaIntegrationTest, OutboxSchemaIntegrationTest

### Community 138 - "CustomerAdminController.kt"
Cohesion: 0.13
Nodes (5): CustomerAdminController, CustomerController, ChangeCustomerStatusDTO, CustomerDTO, RegisterCustomerDTO

### Community 141 - "ProcessedEventRepository"
Cohesion: 0.10
Nodes (10): Task A3: Casos de uso e portas (Marina, `dev-kotlin`), Task A6: API das notificações (Marina, `dev-kotlin`) — paralela à A5, 2. Módulos e camadas, NotificationRepository, ProcessedEventRepository, UnknownAccountException, ListNotificationsUseCase, MarkNotificationsReadUseCase (+2 more)

### Community 143 - "OutboxAdminControllerIntegrationTest.kt"
Cohesion: 0.11
Nodes (3): DemoRelayConfiguration, OutboxAdminControllerIntegrationTest, TestApplication

### Community 144 - "AccountNotFoundException"
Cohesion: 0.11
Nodes (5): 4. Fluxos, AccountView, GetAccountUseCase, GetAccountUseCaseTest, currentBalance()

### Community 145 - "EndToEndEnvironment"
Cohesion: 0.26
Nodes (3): Task 4: ambiente local, e2e e guia, Task 1: `libs/testing-redis` e ambiente, EndToEndEnvironment

### Community 153 - "Customer"
Cohesion: 0.10
Nodes (17): 4. Subir os serviços `customer`, `accounts`, `transfers`, `notification` e `statement`, Alternativa: os cinco serviços pelas imagens Docker (perfil `apps`), Task 4: serviço `customer`, 2. Serviço `customer`, EventOutbox, TransactionRunner, MutableClock, RecordingEventOutbox (+9 more)

### Community 154 - ".get"
Cohesion: 0.25
Nodes (6): AccountOpened, EventEnvelopeReader, Ignored, LedgerEntryRecorded, MalformedEventException, ReceivedEvent

### Community 155 - "TransferMetricsConfiguration.kt"
Cohesion: 0.33
Nodes (3): TransferMetrics, TransferMetricsConfiguration, TransferMetricsProperties

### Community 157 - "JdbcRepositoriesIntegrationTest.kt"
Cohesion: 0.08
Nodes (10): EventOutbox, IdGenerator, TransactionRunner, MessagingContractBase, AccountBlocked, AccountClosed, AccountEvent, AccountOpened (+2 more)

### Community 160 - "accounts/bootstrap/contract/HttpContractBase.kt"
Cohesion: 0.09
Nodes (7): DebitOutcome, RecordDebitUseCase, Recorded, Refused, RecordEntryCommand, HttpContractBase, Meters

### Community 161 - "StepOutcome"
Cohesion: 0.15
Nodes (12): Etapa 6 — limite diário: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Revisão final da branch (Beatriz), Task 2: domínio, saga e migração, Task 4: endpoint, e2e, smoke e Bruno, AccountsResponseClassifier (+4 more)

### Community 163 - "uuid"
Cohesion: 0.03
Nodes (13): OutboxEventEnvelope, Caller, DirectTransactionRunner, JdbcProcessedEventRepository, ForeignCustomerException, NotificationNotFoundException, CorrelationContext, DailyLimitGateway (+5 more)

### Community 169 - "TransferAdminController.kt"
Cohesion: 0.18
Nodes (4): CompensateManuallyDTO, ManualCompensationDTO, StuckTransferDTO, TransferAdminController

### Community 173 - "TransferStep"
Cohesion: 0.07
Nodes (20): Etapa 2 — `transfers` + saga Implementation Plan, Global Constraints, Review Focus, Task 8: Domínio da transferência e passos da saga (Marina, `dev-kotlin`), Rulings deste plano, 3. Modelo de domínio, Riscos, TransferRows (+12 more)

### Community 175 - "OutboxMessage"
Cohesion: 0.10
Nodes (3): JdbcOutboxWriterIntegrationTest, OutboxMessage, OutboxMessageVerifierIntegrationTest

### Community 178 - "ApiClient"
Cohesion: 0.15
Nodes (3): RouteRulesIntegrationTest, ApiClient, ApiResponse

### Community 179 - "CustomerLookup"
Cohesion: 0.24
Nodes (3): CustomerProblemDTO, CustomerStatusResponseDTO, HttpCustomerGateway

### Community 181 - "CustomerRepository"
Cohesion: 0.11
Nodes (7): CallerHasNoCustomerException, CustomerNotFoundException, CustomerRepository, ChangeCustomerStatusUseCase, GetCustomerUseCase, GetOwnCustomerUseCase, UseCaseConfiguration

### Community 182 - "TransferEvent"
Cohesion: 0.07
Nodes (8): OutboxEventAppender, EventOutbox, MessagingContractBase, EventOutbox, TransferCompensated, TransferCompleted, TransferEvent, TransferFailed

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
Cohesion: 0.18
Nodes (5): ApiDocsIntegrationTest, Disabled, Enabled, GroupsClaimIntegrationTest, TestHttp

### Community 189 - "GetStatementUseCaseTest"
Cohesion: 0.20
Nodes (3): statementEntry(), GetStatementUseCaseTest, StatementCursor

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
Cohesion: 0.16
Nodes (6): MarkedNotificationsDTO, MarkNotificationsReadDTO, NotificationPageDTO, NotificationController, InvalidField, InvalidRequestException

### Community 201 - "loggerfactory"
Cohesion: 0.11
Nodes (3): CorrelationId, NotificationEventListenerTest, StatementEventListenerTest

### Community 202 - "0014 — Tipo de conta e cheque especial"
Cohesion: 0.40
Nodes (5): 0014 — Tipo de conta e cheque especial, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 203 - "NotificationText"
Cohesion: 0.09
Nodes (8): Etapa 3 — Mensageria e `notification` Implementation Plan, Frentes e ordem, Review Focus, ConsumeEventUseCase, Draft, NotificationContent, NotificationText, NotificationTextTest

### Community 204 - ".request"
Cohesion: 0.14
Nodes (12): IdempotencyKeyHeader, AccountsUnavailableException, CustomerBlockedException, CustomerNotFoundException, CustomersUnavailableException, InvalidField, InvalidRequestException, OwnAccountNotFoundException (+4 more)

### Community 205 - "0007 — Conta e ledger no mesmo serviço (`accounts`)"
Cohesion: 0.50
Nodes (4): 0007 — Conta e ledger no mesmo serviço (`accounts`), Consequências, Contexto, Decisão

### Community 213 - ".entryCreated"
Cohesion: 0.10
Nodes (3): DailyLimitSagaIntegrationTest, AccountsStubs, DailyLimitApiIntegrationTest

### Community 214 - ".decode"
Cohesion: 0.24
Nodes (4): Task 6: API do extrato (Marina, `dev-kotlin`; paralela à Task 5), InvalidCursorException, StatementCursorCodec, StatementCursorCodecTest

### Community 215 - "OutboxAdminController.kt"
Cohesion: 0.16
Nodes (3): DiscardOutboxEventDTO, StuckOutboxEventDTO, OutboxAdminController

### Community 217 - "AccountsIntegrationTest"
Cohesion: 0.15
Nodes (3): AccountsIntegrationTest, UpdateGrantsIntegrationTest, UseCaseConfigurationIntegrationTest

### Community 220 - "0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS"
Cohesion: 0.40
Nodes (5): 0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 223 - "OpenAccountUseCaseTest.kt"
Cohesion: 0.13
Nodes (6): OpenAccountCommand, OpenAccountResult, OpenAccountUseCase, CallLog, StubCustomerGateway, OpenAccountUseCaseTest

### Community 224 - "TransfersIntegrationTest"
Cohesion: 0.11
Nodes (3): ClockWiringIntegrationTest, TransfersIntegrationTest, UnexpectedErrorApiIntegrationTest

### Community 225 - "Banco digital em microsserviços"
Cohesion: 0.25
Nodes (8): Agentes do projeto, Ao terminar uma tarefa, Autonomia, Banco digital em microsserviços, Contexto, Fluxo de trabalho (skills), Git, Grafo do código (graphify)

### Community 226 - ".brl"
Cohesion: 0.15
Nodes (11): Frente A — `notification`, Task A10: Documentação (Clara, `documentador`) — paralela desde o início, Task A1: Esqueleto do `notification` e prova do `@SqsListener` (Marina, `dev-kotlin`), Task A2: Domínio das notificações (Marina, `dev-kotlin`), Task A4: Persistência (Marina, `dev-kotlin`), Task A5: Consumo das filas (Marina, `dev-kotlin`) — paralela à A6, Task A7: `AccountTypeChanged` versão 2 no `accounts` (Marina, `dev-kotlin`) — paralela desde o início, Task A8: Infra local, Bruno, smoke e CI do `notification` (Rafael, `infra`) (+3 more)

### Community 227 - "04-statement.sh"
Cohesion: 0.43
Nodes (6): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, queue_arn(), 04-statement.sh script

### Community 229 - "AccountStatusAdminController.kt"
Cohesion: 0.17
Nodes (7): AccountStatusAdminController, IdempotencyKeyHeader, AccountTypeNotFoundException, EntryNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException

### Community 230 - "Etapa 0 — Fundação: design"
Cohesion: 0.22
Nodes (9): 2. Build e convenções, 4. Ambiente local, 5. CI (GitHub Actions), Critério de pronto, Etapa 0 — Fundação: design, Fora de escopo (e onde foi registrado), Objetivo, Restrições (de `CLAUDE.md` e ADRs) (+1 more)

### Community 231 - "Etapa 6 — limite diário: design"
Cohesion: 0.13
Nodes (15): 1. Fonte da verdade no PostgreSQL, 2. Contador no Redis (script Lua), 3. Passo `ReserveLimit` da saga, 4. Trava única e fallback no PostgreSQL, 5. Endpoint de consulta, 6. Observabilidade, 7. Testes, 8. Ambiente local, CI e documentação (+7 more)

### Community 232 - "transfers/bootstrap/contract/HttpContractBase.kt"
Cohesion: 0.10
Nodes (4): PendingTransfersDTO, PendingTransfersController, CountPendingTransfersUseCase, HttpContractBase

### Community 233 - "SecurityProblems.kt"
Cohesion: 0.19
Nodes (3): ProblemDetailsAccessDeniedHandler, ProblemDetailsAuthenticationEntryPoint, SecurityProblems

### Community 235 - "Frente B — débitos técnicos"
Cohesion: 0.29
Nodes (6): Frente B — débitos técnicos, Task B1: Configuração do relay em `libs/outbox-relay` (Marina, `dev-kotlin`) — cartão #92, Task B2: Backoff do relay e testes (Marina, `dev-kotlin`) — cartões #24 e #26, depois da B1, Task B3: Domínio e persistência do `transfers` (Marina, `dev-kotlin`) — cartões #95 e #93, Task B4: Aplicação e adapters do `transfers` (Marina, `dev-kotlin`) — cartão #94, depois da B3, Task B5: Endurecer os testes ponta a ponta (Marina, `dev-kotlin`) — cartão #96, antes da A9

### Community 236 - "notification/bootstrap/DlqMetricsConfiguration.kt"
Cohesion: 0.17
Nodes (4): QueueProperties, DlqDepthMonitor, DlqDepthProbe, DlqMetricsConfiguration

### Community 237 - "AccountsApplicationIntegrationTest.kt"
Cohesion: 0.17
Nodes (4): AccountsApplicationIntegrationTest, ParameterizedTypeReference, RequestThreadProbe, RequestThreadProbeConfiguration

### Community 238 - "Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)"
Cohesion: 0.29
Nodes (7): Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2), Task 10: Pedir, consultar e retomar transferências (Marina, `dev-kotlin`), Task 11: Persistência do `transfers` (Marina, `dev-kotlin`), Task 13: API HTTP do `transfers` (Marina, `dev-kotlin`), Task 14: Agendador, relay e eventos no `transfers` (Marina, `dev-kotlin`), Task 15: Infra local, Bruno, smoke e CI do `transfers` (Rafael, `infra`), Task 7: Esqueleto do `transfers` (Marina, `dev-kotlin`)

### Community 240 - "Transfer.kt"
Cohesion: 0.12
Nodes (3): CompensateStepTest, DebitStepTest, ResolveCreditStepTest

### Community 241 - "DailyLimitProperties"
Cohesion: 0.20
Nodes (3): CircuitBreaker, DailyLimitProperties, DailyLimitPropertiesBindingTest

### Community 244 - ".get"
Cohesion: 0.16
Nodes (5): StatementDTO, StatementEntryDTO, StatementController, InvalidField, InvalidRequestException

### Community 250 - "loggerfactory"
Cohesion: 0.22
Nodes (5): NotificationEventListener, ConsumeOutcome, DUPLICATE, IGNORED, PROCESSED

### Community 252 - "StuckTransfer"
Cohesion: 0.11
Nodes (12): Ambiente do Claude, Como rodar localmente, Como usar o board, Concluído, Convenções que já custaram caro, Em andamento (handoff de 2026-11-16), Status do projeto, JdbcStuckTransferFinder (+4 more)

### Community 253 - "DailyLimitController.kt"
Cohesion: 0.22
Nodes (3): DailyLimitController, DailyLimitDTO, DailyLimitUsage

### Community 256 - "0016 — Saga de transferência orquestrada por HTTP síncrono"
Cohesion: 0.40
Nodes (5): 0016 — Saga de transferência orquestrada por HTTP síncrono, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 257 - "V1__notification_schema.sql"
Cohesion: 0.50
Nodes (4): known_accounts, notifications, notifications_customer_page_idx, processed_events

### Community 259 - "send"
Cohesion: 0.13
Nodes (6): AccountsApiClient, CustomerApiClient, HttpResult, send(), StatementApiClient, TransfersApiClient

### Community 261 - "assertProblem"
Cohesion: 0.11
Nodes (5): ApiClient, ApiResponse, assertProblem(), StatementOwnershipIntegrationTest, UnexpectedErrorApiIntegrationTest

### Community 263 - "0018 — Extrato ordenado pela sequência do ledger"
Cohesion: 0.40
Nodes (5): 0018 — Extrato ordenado pela sequência do ledger, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 264 - "CorrelationIdFilter"
Cohesion: 0.24
Nodes (4): Task 9: API HTTP, Problem Details e correlation ID (Marina, `dev-kotlin`), 5. `libs/observabilidade`, libs/observabilidade, CorrelationIdFilter

### Community 271 - "Etapa 5 — cache do extrato: design"
Cohesion: 0.14
Nodes (14): 2. Adapter Redis, 3. Consistência, 4. Observabilidade, 5. Configuração, 6. Testes, 7. Ambiente local e CI, 8. Documentação que acompanha, Critério de pronto (+6 more)

### Community 272 - "Money"
Cohesion: 0.12
Nodes (3): HttpAccountsGatewayContractIntegrationTest, Money, MoneyTest

### Community 273 - "GetDailyLimitUseCaseTest"
Cohesion: 0.13
Nodes (7): AccountNotFound, AccountsUnavailable, DailyLimitOutcome, Found, GetDailyLimitUseCase, GetDailyLimitUseCaseTest, DailyLimitDayTest

### Community 275 - "SecurityTestApplication.kt"
Cohesion: 0.15
Nodes (3): CatchAllExceptionHandler, SecurityTestApplication, SecurityTestController

### Community 276 - "NotificationAccessIntegrationTest"
Cohesion: 0.13
Nodes (4): ApiClient, ApiResponse, assertProblem(), NotificationAccessIntegrationTest

### Community 277 - "OutboxMetrics"
Cohesion: 0.16
Nodes (4): 1. Entrega em dois blocos, OutboxMetrics, OutboxBacklog, OutboxMetricsTest

### Community 280 - "HttpAccountsGatewayIntegrationTest.kt"
Cohesion: 0.08
Nodes (4): awaitStatus(), deleteTransfers(), outboxCorrelationIds(), statusOf()

### Community 281 - "0002 — Clean Architecture por serviço"
Cohesion: 0.40
Nodes (5): 0002 — Clean Architecture por serviço, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 283 - "StatementCacheProperties"
Cohesion: 0.19
Nodes (3): StatementCacheProperties, StatementCachePropertiesTest, CacheConfiguration

### Community 284 - "TransferAdminExceptionHandler.kt"
Cohesion: 0.26
Nodes (7): CompensationCreditAlreadyUsedException, CompensationCreditMismatchException, CompensationCreditNotFoundException, TransferAdminExceptionHandler, TransferBusyException, TransferNotCompensatingException, TransferNotPendingException

### Community 290 - "RegisterCustomerUseCaseTest"
Cohesion: 0.22
Nodes (4): RegisterCustomerCommand, RegisterCustomerResult, RegisterCustomerUseCase, RegisterCustomerUseCaseTest

### Community 302 - "Bloqueio e encerramento de conta: design"
Cohesion: 0.22
Nodes (7): Abordagens consideradas, Bloqueio e encerramento de conta: design, Decisões (usuário, 2026-11-16), Desenho, Fora de escopo, Objetivo, Onde mexe

### Community 303 - "TransferController.kt"
Cohesion: 0.24
Nodes (3): TransferDTO, TransferController, RequestTransferCommand

### Community 304 - "ServiceTokenClientIntegrationTest"
Cohesion: 0.13
Nodes (3): MutableClock, ServiceTokenClientIntegrationTest, TokenRequest

### Community 305 - "Service"
Cohesion: 0.27
Nodes (3): Service, ApplicationWithSpring, AccountsSecurityConfiguration

### Community 306 - "status.md"
Cohesion: 0.12
Nodes (6): 0009 — Cache do extrato, Alternativas consideradas, Consequências, Contexto, Decisão, Roadmap

### Community 308 - "CustomerLookup"
Cohesion: 0.16
Nodes (8): IdempotencyKeyReusedException, CustomerGateway, CustomerLookup, ACTIVE, BLOCKED, NOT_FOUND, UNAVAILABLE, FakeCustomerGateway

### Community 311 - "DigitalBankingSecurityAutoConfiguration.kt"
Cohesion: 0.21
Nodes (3): DigitalBankingSecurityAutoConfiguration, DigitalBankingSecurityProperties, SecurityExceptionHandling

### Community 313 - "Ordem e paralelismo"
Cohesion: 0.14
Nodes (13): Etapa 7 — borda e identidade: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Revisão final da branch (Beatriz), Rulings deste plano, Task 2: ambiente local de identidade, Task 3: Terraform do Cognito e do API Gateway (sem aplicar) (+5 more)

### Community 318 - "Etapa 7 — borda e identidade: design"
Cohesion: 0.11
Nodes (16): 10. Documentação, 1. Identidade e o id do cliente, 3. Autorização nos serviços, 4. Consulta ao `customer` no pedido, 6. Segurança transversal, 7. Ambiente local, 8. Terraform (sem aplicar), 9. Testes (+8 more)

### Community 321 - "RedisCircuitBreaker"
Cohesion: 0.23
Nodes (5): RedisCircuitBreaker, State, CLOSED, HALF_OPEN, OPEN

### Community 324 - "accounts/adapters/persistence/UuidV7IdGenerator.kt"
Cohesion: 0.16
Nodes (3): UuidV7IdGenerator, UuidV7IdGenerator, IdGenerator

### Community 338 - "AccountsResponseClassifierTest"
Cohesion: 0.15
Nodes (3): Task 12: Cliente HTTP do `accounts` (Marina, `dev-kotlin`), AccountsClientProperties, AccountsResponseClassifierTest

### Community 340 - "OrderController.kt"
Cohesion: 0.25
Nodes (4): OrderController, PlaceOrderCommand, PlaceOrderResult, PlaceOrderUseCase

### Community 344 - "05-customer-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 05-customer-topic.sh script

### Community 346 - "JdbcOutboxWriter"
Cohesion: 0.07
Nodes (18): 10. Documentação que acompanha, 3. Esquema do banco, 5. Outbox e relay (`libs/outbox`), 6. API, 7. Testes, 8. Ambiente local e CI, 9. Entrega em dois blocos, Critério de pronto (+10 more)

### Community 347 - "HttpAccountsGateway"
Cohesion: 0.18
Nodes (7): AccountsAccountResponseDTO, AccountsEntryRequestDTO, AccountsEntryResponseDTO, AccountsLedgerEntryResponseDTO, AccountsProblemDTO, HttpAccountsGateway, RawResponse

### Community 348 - "0010 — Limite diário com Redis + Lua"
Cohesion: 0.40
Nodes (5): 0010 — Limite diário com Redis + Lua, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 349 - "0020 — Identidade e autorização"
Cohesion: 0.40
Nodes (5): 0020 — Identidade e autorização, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 350 - "TransfersSecurityConfiguration.kt"
Cohesion: 0.20
Nodes (8): Bloqueio e encerramento de conta: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Revisão final da branch (Beatriz), Task 2: `transfers`: pendentes por conta e saga com conta bloqueada ou encerrada, Task 3: `notification`: notificações de status da conta, TransfersSecurityConfiguration

### Community 351 - "CustomerServiceStub"
Cohesion: 0.13
Nodes (9): Active, Behavior, Blocked, CustomerServiceStub, Lookup, NotFound, ServerError, Slow (+1 more)

### Community 352 - "token.sh"
Cohesion: 0.83
Nodes (3): request(), token.sh script, usage()

### Community 353 - "V2__outbox.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 373 - "OutboxRelayConfigurationSupportTest.kt"
Cohesion: 0.15
Nodes (3): DemoRelayConfiguration, InfrastructureConfiguration, OutboxRelayConfigurationSupportTest

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
Cohesion: 0.33
Nodes (6): 0019 — Runtime na AWS com EKS, Alternativas consideradas, Consequências, Contexto, Decisão, Papel do GitHub Actions

### Community 381 - "0023 — Status da conta e encerramento"
Cohesion: 0.40
Nodes (5): 0023 — Status da conta e encerramento, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 383 - "JdbcOutboxStore"
Cohesion: 0.15
Nodes (3): JdbcOutboxStore, EnvelopeMessage, OutboxMessageVerifier

### Community 387 - "RetryBackoff"
Cohesion: 0.14
Nodes (5): 4. Fluxos, RetryBackoff, SagaProperties, CorrelationContext, TransactionRunner

### Community 389 - "TransferStatus"
Cohesion: 0.11
Nodes (18): Transferências, 10. Documentação que acompanha, 5. Esquema do banco (`transfers`), 6. Eventos, 7. API, nginx e Bruno, 8. Testes, 9. Ambiente local e CI, Critério de pronto (+10 more)

### Community 390 - "ResumeDueTransfersUseCase"
Cohesion: 0.16
Nodes (7): 2. Módulos e camadas, Outcome, INTERRUPTED, SKIPPED, TAKEN, ResumeDueTransfersUseCase, TransferSagaSchedulerConfiguration

### Community 393 - "RunTransferSagaUseCase"
Cohesion: 0.20
Nodes (3): CallResult, Run, RunTransferSagaUseCase

### Community 396 - "Etapa 9: frontend do cliente (design)"
Cohesion: 0.12
Nodes (15): 1. Intenção, 2. Objetivos, 3. Fora do escopo, 4. Decisões do usuário, 5. Abordagens, 6.1 Estrutura, 6.2 Telas, 6.3 BFF (+7 more)

### Community 397 - "SecurityRules"
Cohesion: 0.18
Nodes (4): O que a lib faz sozinha, CallerRoles, SecurityRules, NotificationSecurityConfiguration

### Community 398 - "Ordem e paralelismo"
Cohesion: 0.13
Nodes (15): Etapa 8 — acabamento: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Revisão final da branch (Beatriz), Task 10: subida real (pendente do usuário), Task 1: Dockerfile em camadas e smoke contra as imagens, Task 2: chart Helm (+7 more)

### Community 399 - "Etapa 8 — acabamento: design"
Cohesion: 0.13
Nodes (15): 1. Imagens, 2. Chart Helm, 3. Terraform, 4. Workflows, 5. Testes de contrato, 6. README e diagrama, 7. Revisão dos ADRs, Abordagens consideradas (+7 more)

### Community 401 - "ManualResolution"
Cohesion: 0.25
Nodes (4): JdbcManualResolutionLog, ManualResolution, ManualResolutionLog, InMemoryManualResolutionLog

### Community 404 - "properties"
Cohesion: 0.14
Nodes (13): additionalProperties, description, type, description, description, type, type, properties (+5 more)

### Community 407 - "serviceRef"
Cohesion: 0.15
Nodes (13): description, type, anyOf, description, x-kubernetes-int-or-string, name, port, serviceRef (+5 more)

### Community 409 - "properties"
Cohesion: 0.17
Nodes (12): description, enum, type, ipAddressType, targetType, vpcID, properties, description (+4 more)

### Community 411 - "AccountRequestDTOs.kt"
Cohesion: 0.30
Nodes (4): ChangeAccountStatusDTO, ChangeAccountTypeDTO, ChangeOverdraftLimitDTO, RecordEntryDTO

### Community 412 - "IdGenerator"
Cohesion: 0.18
Nodes (3): UuidV7IdGenerator, IdGenerator, SequentialIdGenerator

### Community 415 - "deploy-services.sh"
Cohesion: 0.36
Nodes (9): env_value(), load_service_args(), out(), out_map(), required_env(), deploy-services.sh script, fail(), has() (+1 more)

### Community 418 - "EntryKind"
Cohesion: 0.24
Nodes (5): CachedStatementEntry, CachedStatementPage, EntryKind, CREDIT, DEBIT

### Community 422 - "matchExpressions"
Cohesion: 0.20
Nodes (10): description, items, type, x-kubernetes-list-type, additionalProperties, description, type, properties (+2 more)

### Community 423 - "Review Focus"
Cohesion: 0.20
Nodes (10): Etapa 4 — `statement`: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Task 1: Esqueleto Gradle e cliente DynamoDB no apoio de testes (Rafael, `infra`), Task 2: LocalStack, compose, nginx e CI (Rafael, `infra`), Task 4: Repositório DynamoDB e bootstrap (Marina, `dev-kotlin`), Task 5: Consumo SQS e métrica da DLQ (Marina, `dev-kotlin`; paralela à Task 6) (+2 more)

### Community 426 - "TransfersServiceStub"
Cohesion: 0.27
Nodes (5): Behavior, Pending, ServerError, Slow, TransfersServiceStub

### Community 428 - "networking"
Cohesion: 0.22
Nodes (9): description, items, type, additionalProperties, description, properties, type, ingress (+1 more)

### Community 429 - "status"
Cohesion: 0.22
Nodes (9): description, format, type, observedGeneration, status, additionalProperties, description, properties (+1 more)

### Community 430 - "Banco digital em microsserviços"
Cohesion: 0.22
Nodes (9): Arquitetura, Banco digital em microsserviços, Como rodar localmente, Como subir na AWS, Custo por subida, Decisões principais, Pontos fortes, Uma vez, pelo usuário (+1 more)

### Community 433 - "Chart Helm dos serviços"
Cohesion: 0.25
Nodes (7): Chart Helm dos serviços, Instalação na AWS, Job `db-init`, Propriedades sem placeholder, Região, ServiceAccount e Pod Identity, Valores obrigatórios

### Community 444 - "test-local.sh"
Cohesion: 0.53
Nodes (4): as(), run_job(), test-local.sh script, write_secret()

### Community 445 - "Bloco 1 — preparação para o 2º serviço (branch `etapa-2-transfers`, PR 1)"
Cohesion: 0.33
Nodes (6): Bloco 1 — preparação para o 2º serviço (branch `etapa-2-transfers`, PR 1), Task 1: Gradle para vários serviços (Rafael, `infra`) — cartão #37, Task 3: Regras ArchUnit compartilhadas (Marina, `dev-kotlin`) — cartão #32, Task 4: Consulta de lançamento pela chave no `accounts` (Marina, `dev-kotlin`), Task 5: PostgreSQL com um banco por serviço (Rafael, `infra`) — cartão #38, Task 6: Build seletivo no CI (Rafael, `infra`)

### Community 448 - "create-service-databases.sh"
Cohesion: 0.70
Nodes (4): create_service_db(), json_field(), PGDATABASE, create-service-databases.sh script

### Community 449 - "nodeSelector"
Cohesion: 0.40
Nodes (5): additionalProperties, description, type, x-kubernetes-map-type, nodeSelector

### Community 450 - "spec"
Cohesion: 0.40
Nodes (5): spec, additionalProperties, description, required, type

### Community 451 - "0001 — Stack: Kotlin + Spring Boot 4 na AWS"
Cohesion: 0.40
Nodes (5): 0001 — Stack: Kotlin + Spring Boot 4 na AWS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 452 - "0021 — Deploy e ambiente efêmero na AWS"
Cohesion: 0.40
Nodes (5): 0021 — Deploy e ambiente efêmero na AWS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 453 - "cognito-user.sh"
Cohesion: 0.70
Nodes (4): idp(), random_password(), cognito-user.sh script, usage()

### Community 454 - "helm-teardown.test.sh"
Cohesion: 0.70
Nodes (4): check_count(), expect(), helm-teardown.test.sh script, uninstalls()

### Community 460 - "targetGroupARN"
Cohesion: 0.50
Nodes (4): targetGroupARN, description, minLength, type

### Community 461 - "leftovers.sh"
Cohesion: 0.83
Nodes (3): leftovers.sh script, tagged(), vpc_leftovers()

### Community 462 - "reaper-check.test.sh"
Cohesion: 0.83
Nodes (3): expect(), resource(), reaper-check.test.sh script

### Community 463 - "reaper-decision.test.sh"
Cohesion: 0.83
Nodes (3): expect(), expect_error(), reaper-decision.test.sh script

## Knowledge Gaps
- **90 isolated node(s):** `PGDATABASE`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION`, `AWS_ACCESS_KEY_ID` (+85 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 1273 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **272 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Caller` connect `uuid` to `RequestTransferUseCaseTest`, `Account`, `SecurityRules`, `LedgerEntry`, `ProcessedEventRepository`, `AccountNotFoundException`, `GetDailyLimitUseCaseTest`, `Decisões tomadas`, `TransferRepository`, `Customer`, `AccountController.kt`, `accounts/bootstrap/contract/HttpContractBase.kt`, `RegisterCustomerUseCaseTest`, `TransferAdminController.kt`, `Notification`, `ServiceTokenClient`, `TransferController.kt`, `Service`, `CustomerRepository`, `Ordem e paralelismo`, `GetStatementUseCaseTest`, `Etapa 7 — borda e identidade: design`, `CallerAuthentication.kt`, `StatementPage`, `Customer`, `0020 — Identidade e autorização`, `OpenAccountUseCaseTest.kt`, `TransfersIntegrationTest`, `CallerResolver`, `StuckTransfer`?**
  _High betweenness centrality (0.037) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Transfer` (e.g. with `Transferências` and `Global Constraints`) actually correct?**
  _`Transfer` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PGDATABASE`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` to the rest of the system?**
  _90 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SqsListenerFifoIntegrationTest.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.14039408866995073 - nodes in this community are weakly interconnected._
- **Why does `Money` connect `Money` to `RequestTransferUseCaseTest`, `RedisDailyLimitGatewayIntegrationTest.kt`, `RedisDailyLimitGateway`, `ResumeDueTransfersUseCaseTest`, `.transfer`, `RedisDailyLimitGatewayIntegrationTest`, `GetDailyLimitUseCaseTest`, `OutboxEventAppenderIntegrationTest.kt`, `TransferMetricsIntegrationTest.kt`, `HttpAccountsGatewayIntegrationTest.kt`, `uuid`, `ManualCompensationTest`, `TransferStep`, `TransferController.kt`, `TransferEvent`, `StuckTransferAdminUseCasesTest`, `InMemoryTransferRepositoryTest`, `JdbcTransferRepositoryIntegrationTest`, `FakeAccountsGatewayTest`, `HttpAccountsGatewayIntegrationTest`, `CreditStepTest`, `RunTransferSagaUseCaseTest`, `HttpAccountsGateway`, `InMemoryTransferRepository`, `Transfer.kt`, `ReserveLimitStepTest`, `RedisDailyLimitGatewayResilienceIntegrationTest.kt`?**
  _High betweenness centrality (0.033) - this node is a cross-community bridge._
- **Should `AccountApiIntegrationTest` be split into smaller, more focused modules?**
  _Cohesion score 0.11153846153846154 - nodes in this community are weakly interconnected._
- **Why does `Account` connect `Account` to `accounts/bootstrap/contract/HttpContractBase.kt`, `uuid`, `RecordDebitUseCaseTest`, `.record`, `Balance`, `CloseAccountUseCaseTest`, `LedgerEntry`, `JdbcRepositoriesIntegrationTest`, `AccountNotFoundException`, `OverdraftLimit`, `ChangeAccountTypeUseCaseTest`, `ChangeAccountStatusUseCaseTest`, `JdbcOutboxWriter`, `JdbcRepositoriesIntegrationTest.kt`, `AccountController.kt`, `OpenAccountUseCaseTest.kt`?**
  _High betweenness centrality (0.032) - this node is a cross-community bridge._