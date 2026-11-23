# Graph Report - DigitalBanking  (2026-11-23)

## Corpus Check
- 1042 files · ~578,408 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 96 file(s) not represented in the graph (top: .bru 59, (none) 20, .scss 3)

## Summary
- 8930 nodes · 22017 edges · 530 communities (242 shown, 288 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 1351 edges (avg confidence: 0.88)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `b4833892`
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
- accounts/adapters/web/ProblemDetailsExceptionHandler.kt
- OutboxRelayIntegrationTest.kt
- Caller
- transfers/adapters/web/ProblemDetailsExceptionHandler.kt
- JdbcRepositoriesIntegrationTest
- OutboxRelayTest
- OverdraftLimit
- Decisões tomadas
- SecurityAutoConfigurationIntegrationTest
- TransferAuthorizationIntegrationTest
- transfers.controller.ts
- StepOutcome
- coordenador.md
- Terraform
- 0005 — Frontend Angular e BFF em NestJS
- 0006 — Observabilidade só com correlation ID
- TransferSagaEndToEndIntegrationTest
- test-app.ts
- Rodando o projeto localmente (macOS)
- AccountController.kt
- 0008 — Extrato (`statement`) com CQRS no DynamoDB
- TransferAdminApiIntegrationTest
- HttpCustomerGatewayIntegrationTest.kt
- 0013 — Persistência com Spring JDBC (`JdbcClient`)
- 0012 — Saldo no lançamento e trava pessimista por conta
- CloseAccountUseCaseTest.kt
- CorrelationIdSqsMessageInterceptorTest
- dev-frontend.md
- dev-kotlin.md
- infra.md
- 0003 — Transactional Outbox e mensageria SNS/SQS
- Caller
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
- LedgerEntry
- AccountClosingConcurrencyIntegrationTest
- aTransfer
- SagaPropertiesTest.kt
- JdbcKnownAccountRepositoryIntegrationTest.kt
- smoke.sh
- statement/adapters/web/ProblemDetailsExceptionHandler.kt
- statement/bootstrap/AwsCredentialsConfiguration.kt
- TransfersServiceStub.kt
- OutboxStore
- 01-accounts-topic.sh
- RecordStatementEntryUseCase
- OutboxRelayScheduler
- OutboxMessageVerifier
- HttpAccountsGatewayIntegrationTest
- Foco de revisão
- JdbcOutboxStoreIntegrationTest
- RunTransferSagaUseCase
- OrderResource
- RunTransferSagaUseCaseTest
- LocalStackFifoIntegrationTest.kt
- LocalStackTestSupport
- AwsClientConfigurationTest.kt
- InMemoryStatementRepository
- AccountTypeAdminController.kt
- migrate
- AccountAuthorizationIntegrationTest
- TransferEventsIntegrationTest.kt
- Etapa 3 — Mensageria e `notification`: design
- MarkNotificationsReadUseCaseTest
- EventEnvelopeReaderTest
- AwsProfileConfigAssertions
- OutboxRelayConfigurationSupport.kt
- @angular/core
- PhoneNumber
- LegacyResource.kt
- PlaceOrderHandler
- LegacyController.kt
- EntryLookupApiIntegrationTest
- HttpAccountsGateway
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
- loggerfactory
- Ligações entre as partes
- DynamoDbStatementRepositoryIntegrationTest
- ResumeDueTransfersUseCaseTest
- Balance
- aNotification
- RedisStatementFirstPageCacheIntegrationTest
- web
- CustomerAdminController.kt
- UseCaseConfiguration
- Order
- web/package.json
- RedisDailyLimitGatewayIntegrationTest
- OutboxAdminControllerIntegrationTest.kt
- bff/package.json
- EndToEndEnvironment
- StatementIntegrationTest
- ApiClient
- SnsEventPublisherTest.kt
- jsonmapper
- JdbcTransferRepository
- StatementConsumptionIntegrationTest
- BlockingRepository
- RecipientTokenService
- app.module.ts
- TransferMetricsConfiguration.kt
- .container
- CloseAccountUseCaseTest.kt
- customer/bootstrap/OutboxRelayIntegrationTest.kt
- ServiceProcess
- CustomerLookup
- retryWhileAccountBusy
- @angular/router
- uuid
- EndToEndInputs
- TransferAdminController.kt
- TransferEvent
- StatementCacheIntegrationTest.kt
- ConsumeEventUseCaseTest
- TransferStep
- SagaTimeoutsValidatorTest.kt
- OutboxMessage
- AccountsClientConfiguration
- MdcCorrelationContextTest.kt
- ApiClient
- Customer
- LocalStackInitScripts
- CustomerRepository
- ManualResolution
- ListNotificationsUseCaseTest
- 02-transfers-topic.sh
- OpenApiIntegrationTest
- accounts/adapters/web/OpenApiConfiguration.kt
- transfer
- TransferStatus
- GetStatementUseCaseTest
- InMemoryTransferRepository
- 03-notification-queues.sh
- V3__outbox.sql
- accounts/bootstrap/web/UnexpectedErrorApiIntegrationTest.kt
- statement/bootstrap/DlqMetricsConfiguration.kt
- TransferSchemaIntegrationTest
- NotificationController.kt
- statement/adapters/messaging/EventEnvelopeReader.kt
- 0014 — Tipo de conta e cheque especial
- NotificationText
- TransferController.kt
- 0007 — Conta e ledger no mesmo serviço (`accounts`)
- customer/adapters/web/ProblemDetailsExceptionHandler.kt
- ProblemResponsesOpenApiCustomizerTest.kt
- OpenApiIntegrationTest
- status.md
- CacheConfigurationTest.kt
- CreditStepTest
- customer/bootstrap/contract/MessagingContractBase.kt
- .decode
- OutboxAdminController.kt
- ConcurrencyIntegrationTest
- .handle
- StatementEntryTest
- 0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS
- ServiceProcess.kt
- OpenAccountUseCaseTest
- TransfersIntegrationTest
- Banco digital em microsserviços
- .brl
- 04-statement.sh
- auth.controller.ts
- KeyValueStore
- AccountOpeningCustomerDownIntegrationTest.kt
- Etapa 6 — limite diário: design
- transfers/bootstrap/contract/HttpContractBase.kt
- SecurityProblems.kt
- EventContractsTest.kt
- EventEnvelopeReader
- notification/bootstrap/DlqMetricsConfiguration.kt
- AccountsApplicationIntegrationTest.kt
- tick
- runapplication
- InfrastructureConfiguration
- DailyLimitProperties
- BlockedOrClosedAccountSagaIntegrationTest.kt
- .get
- header
- HttpTransfersGatewayTest
- ReserveLimitStepTest
- ConsumeEventUseCase
- StatementEntry
- ApiDocsIntegrationTest.kt
- DailyLimitController.kt
- .dailyLimitGateway
- RedisDailyLimitGatewayResilienceIntegrationTest.kt
- error.filter.ts
- V1__notification_schema.sql
- StatementEndToEndIntegrationTest
- send
- migrate_from_localstack.py
- assertProblem
- .record
- 0018 — Extrato ordenado pela sequência do ledger
- CorrelationIdFilter
- AccountTypeTest.kt
- SchedulingConfiguration.kt
- EntryKindTest.kt
- notification/bootstrap/SchedulingConfiguration.kt
- apiresponse
- Etapa 5 — cache do extrato: design
- HttpAccountsGatewayContractIntegrationTest.kt
- InMemoryDailyLimitGateway
- DailyLimitEndToEndIntegrationTest.kt
- SecurityTestApplication.kt
- NotificationAccessIntegrationTest
- OutboxMetrics
- CustomerAdminController.kt
- DynamoDbStatementRepositoryTest.kt
- ApiClient
- 0002 — Clean Architecture por serviço
- OpenApiIntegrationTest
- StatementCacheProperties
- transfer.component.ts
- AccountOpeningCustomerIntegrationTest
- SecurityRules
- ProcessLogsOnFailureExtension
- StatementPage
- RunTransferSagaUseCase
- JwtTestSupport.kt
- V6BackfillIntegrationTest.kt
- AccountStatusApiIntegrationTest
- httpclient
- ReceivedEvent
- JdbcCustomerRepository
- MockitoAgentArgumentProvider
- accounts/adapters/customer/HttpCustomerGateway.kt
- OpenApiIntegrationTest
- AccountStatusRulesTest
- support.ts
- ServiceTokenClientIntegrationTest
- AccountsIntegrationTest
- status.md
- frontend/package.json
- problemMessage
- CallerResolverTest
- DigitalBankingSecurityAutoConfiguration.kt
- .render
- Ordem e paralelismo
- Etapa 7 — borda e identidade: design
- CorrelationIdRestClientInterceptorTest.kt
- WebExceptionHandling.kt
- RedisCircuitBreaker
- IdentityEndToEndIntegrationTest.kt
- CallerAuthentication.kt
- accounts/adapters/persistence/UuidV7IdGenerator.kt
- transfers/bootstrap/contract/MessagingContractBase.kt
- CorrelationIdFilterTest
- TransactionRunner
- CustomerIntegrationTest
- CorrelationIdRestClientInterceptor
- HttpCustomerGatewayTest.kt
- NotificationQueues
- GetCustomerUseCaseTest
- OpenApiIntegrationTest
- CustomerTest
- TransfersClientConfiguration.kt
- test-app.ts
- NotificationApiIntegrationTest
- AccountsResponseClassifierTest
- ObservabilityAutoConfiguration.kt
- OrderController.kt
- Etapa 0 — Fundação: design
- LocalStackInitScripts
- SpringTransactionRunnerIntegrationTest.kt
- 05-customer-topic.sh
- CallerTest.kt
- TransferSagaScheduler
- .repeat
- 0010 — Limite diário com Redis + Lua
- 0020 — Identidade e autorização
- NotificationEventListenerTest
- CustomerServiceStub
- token.sh
- V2__outbox.sql
- seed.sh
- seed-check.sh
- accounts/bootstrap/AwsProfileIntegrationTest.kt
- ApiDocs.kt
- customer
- NotificationIntegrationTest
- customer/bootstrap/AwsProfileIntegrationTest.kt
- notification/bootstrap/web/AwsProfileIntegrationTest.kt
- statement/bootstrap/web/AwsProfileIntegrationTest.kt
- assertOutboxSchemaMatchesModel
- transfers/bootstrap/web/AwsProfileIntegrationTest.kt
- .token
- compilerOptions
- RedisStatementFirstPageCache
- OutboxEventAppenderIntegrationTest.kt
- NotificationEventListener
- 0019 — Runtime na AWS com EKS
- 0023 — Status da conta e encerramento
- accounts/adapters/customer/CustomerClientConfiguration.kt
- OutboxMessagingContractConfiguration
- SessionService
- notification-bell.component.spec.ts
- devDependencies
- AccountEventsContractTest.kt
- ProblemResponsesOpenApiCustomizerTest
- restclient
- SnsEventPublisherIntegrationTest.kt
- customer/bootstrap/ArchitectureTest.kt
- .properties
- TransfersDatabase
- customer/bootstrap/contract/HttpContractBase.kt
- Etapa 9: frontend do cliente (design)
- StatementCacheInvalidationRaceIntegrationTest
- Ordem e paralelismo
- Etapa 8 — acabamento: design
- AccountOwnLookupApiIntegrationTest
- re
- StuckTransferAdminUseCasesTest.kt
- .run
- properties
- TransferComponent
- CustomerRegisteredBackfillIntegrationTest.kt
- serviceRef
- WelcomeBonusIntegrationTest
- properties
- AppShellComponent
- HttpCustomerGateway
- check_stack
- ServiceArchitectureRules
- ApiClient
- deploy-services.sh
- JdbcProcessedEventRepositoryIntegrationTest
- CustomerEventsIntegrationTest
- statement/bootstrap/web/UnexpectedErrorApiIntegrationTest.kt
- Status do projeto
- ManualCompensationTest
- ServiceArchitectureRulesTest.kt
- matchExpressions
- StuckTransfer
- ServiceTokenAutoConfiguration.kt
- HomeComponent
- OutboxWriterClockIntegrationTest.kt
- .resumeDueOver
- networking
- status
- Banco digital em microsserviços
- NotificationConsumptionIntegrationTest
- DlqDepthMonitorTest
- Chart Helm dos serviços
- accounts/bootstrap/ArchitectureTest.kt
- SchedulerLeaseOwners
- ChangeCustomerStatusUseCaseTest
- Ondas de execução (paralelismo)
- statement/bootstrap/ArchitectureTest.kt
- JdbcPendingTransferStats.kt
- transfers/bootstrap/ArchitectureTest.kt
- LedgerEntryRepository
- helm/validate.sh
- test-local.sh
- Bloco 1 — preparação para o 2º serviço (branch `etapa-2-transfers`, PR 1)
- Sininho de notificações (design curto)
- JdbcOutboxWriter
- create-service-databases.sh
- nodeSelector
- spec
- 0016 — Saga de transferência orquestrada por HTTP síncrono
- DebitStepTest
- cognito-user.sh
- helm-teardown.test.sh
- reaper-decision.sh
- tsconfig.build.json
- check-alb-routes.py
- Frontend
- DataCentsMigrationIntegrationTest
- targetGroupARN
- leftovers.sh
- reaper-check.test.sh
- reaper-decision.test.sh
- seed-notifications-test.sh
- TransactionRunner
- fetch-secrets.sh
- leftovers.test.sh
- AccountTypeAdminApiIntegrationTest
- helm-teardown.sh
- reaper-check.sh
- bootstrap/validate.sh
- kubernetes-version.sh
- aws/validate.sh
- 0025 — Nome da contraparte nas notificações
- project_gate_revisao_task.md
- nest-cli.json
- .entry
- RequestTransferOutcome
- 0001 — Stack: Kotlin + Spring Boot 4 na AWS
- 0024 — Login e telefone do app
- NotificationsComponent
- 0004 — API Gateway HTTP API na borda
- seed-sub-test.sh
- V4__customer_names.sql
- BlockingRepository
- StatementApplication.kt
- 0021 — Deploy e ambiente efêmero na AWS
- Frente A — `notification`
- JdbcCustomerNameRepositoryIntegrationTest.kt
- assertProblem
- JdbcOutboxWriterIntegrationTest
- LocalStackTestSupportIntegrationTest
- 0026 — Bônus de boas-vindas por cliente
- uri
- ResolveCreditStepTest
- ResolveDebitStepTest
- DynamoDB Local com volume (design curto)
- persistence-check.sh
- SpringTransactionRunner
- OutboxEventAppender
- transfers/bootstrap/web/UnexpectedErrorApiIntegrationTest.kt
- DynamoDbConfigurationTest
- OutboxEventAppender
- 0027 — DynamoDB Local com volume
- run.sh
- CustomerApplication.kt
- GetTransferUseCaseTest
- RecordedEntry
- .exact
- ApiDocsAutoConfiguration.kt
- RequestTransferDTO
- CompensateStepTest
- transfers/domain/MoneyTest.kt
- Bloqueio e encerramento de conta: design
- 0022 — Testes de contrato com Stubborn Contract
- OutboxContractDatabase.kt
- NotificationTest.kt
- accounts/bootstrap/OutboxRelayConfiguration.kt
- DailyLimitScripts
- transfers/bootstrap/web/AwsProfileConfigIntegrationTest.kt
- transfers/bootstrap/OutboxRelayConfiguration.kt
- CustomerClientProperties
- IdempotencyKeyReusedException
- .withCorrelationId

## God Nodes (most connected - your core abstractions)
1. `Transfer` - 137 edges
2. `Account` - 105 edges
3. `Money` - 97 edges
4. `assertProblem()` - 90 edges
5. `Caller` - 84 edges
6. `LedgerEntry` - 76 edges
7. `OverdraftLimit` - 75 edges
8. `Balance` - 73 edges
9. `TransfersIntegrationTest` - 71 edges
10. `Money` - 69 edges

## Surprising Connections (you probably didn't know these)
- `Restrições (de `CLAUDE.md` e ADRs)` --references--> `DailyLimit`  [INFERRED]
  docs/superpowers/specs/2026-11-12-etapa-6-limite-diario-design.md → frontend/web/src/app/shared/api.ts
- `Task 9: Web — Início, Extrato, Transferir, Avisos, Minha conta (Lucas, `dev-frontend`)` --references--> `AppFooterComponent`  [INFERRED]
  docs/superpowers/plans/2026-11-19-etapa-9-frontend.md → frontend/web/src/app/shared/app-footer.component.ts
- `Mensageria` --references--> `OutboxRelay`  [INFERRED]
  docs/arquitetura.md → libs/outbox-relay/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/relay/OutboxRelay.kt
- `Task 14: Agendador, relay e eventos no `transfers` (Marina, `dev-kotlin`)` --references--> `OutboxRelayScheduler`  [INFERRED]
  docs/superpowers/plans/2026-10-27-etapa-2-transfers.md → libs/outbox-relay/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/relay/spring/OutboxRelayScheduler.kt
- `Task 7: `libs/outbox` — envelope e writer (Marie, `dev-kotlin`; em paralelo às Tasks 2–6)` --references--> `OutboxEventEnvelope`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/OutboxEventEnvelope.kt

## Import Cycles
- None detected.

## Communities (530 total, 288 thin omitted)

### Community 1 - "SqsListenerFifoIntegrationTest.kt"
Cohesion: 0.14
Nodes (3): ListenersConfiguration, SqsListenerFifoIntegrationTest, TestListeners

### Community 5 - "RedisDailyLimitGateway"
Cohesion: 0.16
Nodes (6): Outcome, EXCEEDED, FALLBACK_RESERVED, RESERVED, RedisDailyLimitGateway, RedisUnavailableException

### Community 7 - ".transfer"
Cohesion: 0.15
Nodes (4): DailyLimitDayTest, TerminalStepsTest, TransferFixtures, TransferTest

### Community 8 - "Account"
Cohesion: 0.04
Nodes (30): capzzPay, Clientes e contas, Consulta e avisos, Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 13: Documentação do bloco 1 e das decisões da etapa (Clara, `documentador`; em paralelo às Tasks 8–12) (+22 more)

### Community 9 - "check-messaging.py"
Cohesion: 0.15
Nodes (10): attributes(), expand(), Local, logical_lines(), one(), options(), parse_local(), ScriptError (+2 more)

### Community 10 - "CompensateTransferManuallyUseCase"
Cohesion: 0.12
Nodes (19): Accepted, AccountsUnavailable, Busy, Compensated, CompensateManuallyOutcome, CompensateTransferManuallyUseCase, CreditAlreadyUsed, CreditCheck (+11 more)

### Community 12 - "OutboxRelayIntegrationTest.kt"
Cohesion: 0.09
Nodes (3): OutboxRelayIntegrationTest, ReceivedEvent, RecordingPublisherConfiguration

### Community 13 - "Caller"
Cohesion: 0.02
Nodes (35): Task 5: Casos de uso de crédito e débito (Marina, `dev-kotlin`), Etapa 7 — borda e identidade: plano de implementação, Global Constraints, Review Focus, Revisão final da branch (Beatriz), Rulings deste plano, Task 5: `accounts` com JWT, dono e consulta ao `customer`, 4. Fluxos (+27 more)

### Community 16 - "OutboxRelayTest"
Cohesion: 0.16
Nodes (3): FakeEventPublisher, MutableClock, OutboxRelayTest

### Community 17 - "OverdraftLimit"
Cohesion: 0.06
Nodes (18): 1. Módulos e camadas, JdbcAccountTypeRepository, AccountDTO, AccountTypeRepository, ChangeOverdraftLimitUseCase, ListAccountTypesUseCase, AccountTypeRepository, InMemoryAccountTypeRepository (+10 more)

### Community 18 - "Decisões tomadas"
Cohesion: 0.11
Nodes (18): Arquitetura, Borda e segurança, Cache, Componentes do app do cliente (etapa 9), Contratos, Dados e consistência, Decisões tomadas, Deploy na AWS (+10 more)

### Community 21 - "transfers.controller.ts"
Cohesion: 0.05
Nodes (39): Task 7: BFF — endpoints por tela (Lucas, `dev-frontend`), BackendClient, BackendProblemError, CallOptions, FETCH, ProblemDetails, BackendService, CORRELATION_HEADER (+31 more)

### Community 22 - "StepOutcome"
Cohesion: 0.11
Nodes (16): Etapa 6 — limite diário: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Revisão final da branch (Beatriz), Rulings deste plano, Task 1: `libs/testing-redis` e ambiente, Task 2: domínio, saga e migração (+8 more)

### Community 23 - "coordenador.md"
Cohesion: 0.29
Nodes (6): Ao começar uma sessão, Board, Para cada etapa do roadmap, Para cada tarefa, Paralelismo, Quando faltar algo

### Community 24 - "Terraform"
Cohesion: 0.12
Nodes (16): ALB do Terraform, nunca do controller, Bancos e usuários por serviço, Borda: limites e log de acesso, Cognito: domínio e PKCE, Decisões do ambiente efêmero, Estrutura, Etiquetas e `created_at`, Mensageria igual ao LocalStack (+8 more)

### Community 25 - "0005 — Frontend Angular e BFF em NestJS"
Cohesion: 0.33
Nodes (6): 0005 — Frontend Angular e BFF em NestJS, Alternativas consideradas, Consequências, Contexto, Decisão, Emenda de 2026-11-22

### Community 26 - "0006 — Observabilidade só com correlation ID"
Cohesion: 0.40
Nodes (5): 0006 — Observabilidade só com correlation ID, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 28 - "test-app.ts"
Cohesion: 0.10
Nodes (17): Task 6: BFF — identidade e sessão (Lucas, `dev-frontend`), CognitoIdentityProvider, ERROR_CODES, REFRESH_REFUSED, providerFailingWith(), translate(), httpFetch(), IdentityError (+9 more)

### Community 29 - "Rodando o projeto localmente (macOS)"
Cohesion: 0.09
Nodes (22): 1. Configurar o ambiente, 2. Subir a infraestrutura (PostgreSQL, Redis, nginx, LocalStack, DynamoDB Local, mock-oauth2-server, web, BFF), 3. Build e testes, 5. Conferir a API pelo nginx, 6. Ver os eventos na fila de inspeção, 7. Coleção Bruno, 8. Derrubar tudo, A mesma sequência à mão (+14 more)

### Community 30 - "AccountController.kt"
Cohesion: 0.15
Nodes (4): currentCaller(), AccountController, LedgerEntryDTO, RecipientAccountDTO

### Community 31 - "0008 — Extrato (`statement`) com CQRS no DynamoDB"
Cohesion: 0.50
Nodes (4): 0008 — Extrato (`statement`) com CQRS no DynamoDB, Consequências, Contexto, Decisão

### Community 33 - "HttpCustomerGatewayIntegrationTest.kt"
Cohesion: 0.09
Nodes (4): CustomerClientProperties, HttpCustomerGatewayIntegrationTest, RequireServiceToken, ServiceTokenServers

### Community 34 - "0013 — Persistência com Spring JDBC (`JdbcClient`)"
Cohesion: 0.40
Nodes (5): 0013 — Persistência com Spring JDBC (`JdbcClient`), Alternativas consideradas, Consequências, Contexto, Decisão

### Community 35 - "0012 — Saldo no lançamento e trava pessimista por conta"
Cohesion: 0.40
Nodes (5): 0012 — Saldo no lançamento e trava pessimista por conta, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 36 - "CloseAccountUseCaseTest.kt"
Cohesion: 0.09
Nodes (14): Ordem e paralelismo, Task 1: `accounts`: status, bloqueio, encerramento e conta nova, Task 2: `transfers`: pendentes por conta e saga com conta bloqueada ou encerrada, Task 3: `notification`: notificações de status da conta, TransfersUnavailableException, StubTransfersGateway, CloseAccountUseCaseTest, AccountBlockedException (+6 more)

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

### Community 42 - "Caller"
Cohesion: 0.03
Nodes (38): Global Constraints, Task A3: Casos de uso e portas (Marina, `dev-kotlin`), Task A6: API das notificações (Marina, `dev-kotlin`) — paralela à A5, 2. Módulos e camadas, JdbcCustomerNameRepository, JdbcKnownAccountRepository, JdbcNotificationRepository, JdbcProcessedEventRepository (+30 more)

### Community 43 - "0011 — Contratos de API"
Cohesion: 0.50
Nodes (4): 0011 — Contratos de API, Consequências, Contexto, Decisão

### Community 44 - "Versões novas da etapa 1"
Cohesion: 0.50
Nodes (3): Observações, springdoc e Spring Boot 4.1.x, Versões novas da etapa 1

### Community 46 - "ServiceTokenClient"
Cohesion: 0.12
Nodes (9): Task 1: `libs/security` e `libs/observabilidade`, Configuração, libs/security, Módulos, Uso, CachedToken, RecentFailure, ServiceTokenClient (+1 more)

### Community 63 - "account"
Cohesion: 0.20
Nodes (5): account_customer_id_idx, account_type, account, ledger_entry, account_customer_id_not_closed_key

### Community 65 - "outbox-table.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 69 - "LedgerEntry"
Cohesion: 0.07
Nodes (13): JdbcLedgerEntryRepository, LedgerEntryRepository, RecordEntryResult, DirectTransactionRunner, InMemoryLedgerEntryRepository, GetOwnAccountUseCaseTest, RecordCreditUseCaseTest, RecordDebitUseCaseTest (+5 more)

### Community 71 - "aTransfer"
Cohesion: 0.11
Nodes (4): JdbcTransferRepositoryIntegrationTest, JdbcTransferStepLogIntegrationTest, aTransfer(), microsNow()

### Community 75 - "smoke.sh"
Cohesion: 0.38
Nodes (8): aws_local(), call(), check_events(), check_notification(), new_customer(), new_user(), smoke.sh script, uuid()

### Community 77 - "statement/bootstrap/AwsCredentialsConfiguration.kt"
Cohesion: 0.22
Nodes (3): AwsCredentialsConfiguration, AwsCredentialsConfiguration, credentialsFor()

### Community 78 - "TransfersServiceStub.kt"
Cohesion: 0.14
Nodes (5): Behavior, Pending, ServerError, Slow, TransfersServiceStub

### Community 79 - "OutboxStore"
Cohesion: 0.07
Nodes (12): JdbcOutboxStore, DiscardOutcome, ALREADY_DISCARDED, ALREADY_PUBLISHED, DISCARDED, NOT_FAILED, NOT_FOUND, RELAY_BUSY (+4 more)

### Community 80 - "01-accounts-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 01-accounts-topic.sh script

### Community 81 - "RecordStatementEntryUseCase"
Cohesion: 0.05
Nodes (35): Etapa 4 — `statement`: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Task 1: Esqueleto Gradle e cliente DynamoDB no apoio de testes (Rafael, `infra`), Task 2: LocalStack, compose, nginx e CI (Rafael, `infra`), Task 4: Repositório DynamoDB e bootstrap (Marina, `dev-kotlin`), Task 5: Consumo SQS e métrica da DLQ (Marina, `dev-kotlin`; paralela à Task 6) (+27 more)

### Community 82 - "OutboxRelayScheduler"
Cohesion: 0.11
Nodes (8): 0015 — Relay do outbox com líder único e SNS FIFO, Alternativas consideradas, Consequências, Contexto, Decisão, OutboxRelay, OutboxRelayScheduler, OutboxRelaySchedulerTest

### Community 85 - "Foco de revisão"
Cohesion: 0.14
Nodes (13): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 4: Ambiente local (docker-compose), Task 5: CI no GitHub Actions (+5 more)

### Community 88 - "RunTransferSagaUseCase"
Cohesion: 0.09
Nodes (6): 2. Módulos e camadas, GetTransferUseCase, RequestTransferUseCase, SagaSettings, SagaProperties, UseCaseConfiguration

### Community 89 - "OrderResource"
Cohesion: 0.27
Nodes (5): PersistenceUsesWeb, OrderResource, WebUsesPersistence, ApplicationUsesAdapters, DomainUsesAdapters

### Community 90 - "RunTransferSagaUseCaseTest"
Cohesion: 0.10
Nodes (3): RunTransferSagaUseCaseTest, Handler, Handler

### Community 92 - "LocalStackTestSupport"
Cohesion: 0.22
Nodes (3): Regras inegociáveis, Task 14: Verificação do SNS FIFO no LocalStack (Rafael, `infra`), LocalStackTestSupport

### Community 94 - "InMemoryStatementRepository"
Cohesion: 0.12
Nodes (5): Task 3: Domínio e casos de uso (Marina, `dev-kotlin`), 8. Testes, InMemoryStatementRepository, RecordStatementEntryUseCaseTest, RegisterAccountUseCaseTest

### Community 95 - "AccountTypeAdminController.kt"
Cohesion: 0.10
Nodes (7): AccountStatusAdminController, AccountTypeAdminController, ChangeAccountStatusDTO, ChangeAccountTypeDTO, ChangeOverdraftLimitDTO, RecordEntryDTO, AccountTypeLimitDTO

### Community 96 - "migrate"
Cohesion: 0.15
Nodes (6): migrate(), account(), entry(), FakeDynamoDb, MigrateTest, write_to_source()

### Community 98 - "TransferEventsIntegrationTest.kt"
Cohesion: 0.12
Nodes (3): ReceivedEvent, SentTransfer, TransferEventsIntegrationTest

### Community 99 - "Etapa 3 — Mensageria e `notification`: design"
Cohesion: 0.12
Nodes (17): 10. Ambiente local e CI, 11. Documentação que acompanha, 1. Frentes de trabalho, 3. Modelo de domínio, 4. Fluxo de consumo, 5. Esquema do banco (`notification`), 6. Eventos consumidos e mudança no `accounts`, 7. API, nginx e Bruno (+9 more)

### Community 102 - "AwsProfileConfigAssertions"
Cohesion: 0.16
Nodes (5): AwsProfileConfigAssertions, AwsProfileConfigIntegrationTest, AwsProfileConfigIntegrationTest, AwsProfileConfigIntegrationTest, AwsProfileConfigIntegrationTest

### Community 103 - "OutboxRelayConfigurationSupport.kt"
Cohesion: 0.10
Nodes (9): Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), 1. Entrega em dois blocos, OutboxMetrics, OutboxRelaySettings, AwsProperties, OutboxRelayConfigurationSupport, OutboxRelayProperties, OutboxRelaySettingsTest (+1 more)

### Community 105 - "@angular/core"
Cohesion: 0.10
Nodes (21): Customer, Me, SessionDTO, account, HomeDTO, NO_ACCOUNT, account, flushHome() (+13 more)

### Community 106 - "PhoneNumber"
Cohesion: 0.10
Nodes (7): RegisterCustomerCommand, RegisterCustomerResult, RegisterCustomerUseCase, InMemoryCustomerRepository, ChangePhoneUseCaseTest, FindCustomerByPhoneUseCaseTest, RegisterCustomerUseCaseTest

### Community 111 - "HttpAccountsGateway"
Cohesion: 0.18
Nodes (7): AccountsAccountResponseDTO, AccountsEntryRequestDTO, AccountsEntryResponseDTO, AccountsLedgerEntryResponseDTO, AccountsProblemDTO, HttpAccountsGateway, RawResponse

### Community 127 - "TransferSagaResumeIntegrationTest.kt"
Cohesion: 0.07
Nodes (4): DailyLimitSagaIntegrationTest, AccountsStubs, TransferSagaResumeIntegrationTest, DailyLimitApiIntegrationTest

### Community 129 - "AccountsProxy.kt"
Cohesion: 0.15
Nodes (3): AccountsProxy, HeldRequest, Hold

### Community 130 - "loggerfactory"
Cohesion: 0.10
Nodes (4): CorrelationId, RedisStatementFirstPageCacheTest, StringRedisTemplate, StatementEventListenerTest

### Community 131 - "Ligações entre as partes"
Cohesion: 0.08
Nodes (23): Legenda do Reels e do TikTok, Montagem, Post do LinkedIn, Roteiro corrido, Se passar de 3 minutos, Vídeo 2: a arquitetura do capzzPay, 1. Serviços → Clean Architecture, 2. Clean Architecture → Ledger (+15 more)

### Community 134 - "Balance"
Cohesion: 0.06
Nodes (14): Ledger, 2. Modelo de domínio, ChangeAccountTypeUseCaseTest, GetAccountUseCaseTest, AccountTypeChanged, AvailableBalance, Balance, Money (+6 more)

### Community 136 - "RedisStatementFirstPageCacheIntegrationTest"
Cohesion: 0.16
Nodes (4): CachedStatementEntry, CachedStatementPage, RedisStatementFirstPageCacheIntegrationTest, StatementPage

### Community 137 - "web"
Cohesion: 0.05
Nodes (40): build, serve, test, builder, configurations, defaultConfiguration, options, cli (+32 more)

### Community 138 - "CustomerAdminController.kt"
Cohesion: 0.14
Nodes (7): CustomerController, ChangeCustomerStatusDTO, ChangePhoneDTO, CustomerDTO, CustomerLookupByPhoneDTO, RegisterCustomerDTO, FindCustomerByPhoneUseCase

### Community 139 - "UseCaseConfiguration"
Cohesion: 0.19
Nodes (3): UseCaseConfiguration, welcomeBonusOf(), WelcomeBonusSettingTest

### Community 141 - "web/package.json"
Cohesion: 0.05
Nodes (39): dependencies, @angular/cdk, @angular/common, @angular/compiler, @angular/core, @angular/forms, @angular/material, @angular/platform-browser (+31 more)

### Community 142 - "RedisDailyLimitGatewayIntegrationTest"
Cohesion: 0.09
Nodes (4): Task 3: adapter Redis + Lua, trava, epoch e fallback, RedisTestSupportIntegrationTest, RedisTestSupport, RedisDailyLimitGatewayIntegrationTest

### Community 143 - "OutboxAdminControllerIntegrationTest.kt"
Cohesion: 0.11
Nodes (3): DemoRelayConfiguration, OutboxAdminControllerIntegrationTest, TestApplication

### Community 144 - "bff/package.json"
Cohesion: 0.05
Nodes (38): author, dependencies, @aws-sdk/client-cognito-identity-provider, ioredis, @nestjs/common, @nestjs/core, @nestjs/platform-express, pino (+30 more)

### Community 146 - "StatementIntegrationTest"
Cohesion: 0.10
Nodes (3): DynamoDbLocalTestSupportIntegrationTest, DynamoDbLocalTestSupport, StatementIntegrationTest

### Community 152 - "BlockingRepository"
Cohesion: 0.11
Nodes (13): Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2), Task 10: Pedir, consultar e retomar transferências (Marina, `dev-kotlin`), Task 11: Persistência do `transfers` (Marina, `dev-kotlin`), Task 13: API HTTP do `transfers` (Marina, `dev-kotlin`), Task 14: Agendador, relay e eventos no `transfers` (Marina, `dev-kotlin`), Task 15: Infra local, Bruno, smoke e CI do `transfers` (Rafael, `infra`), Task 7: Esqueleto do `transfers` (Marina, `dev-kotlin`), Outcome (+5 more)

### Community 153 - "RecipientTokenService"
Cohesion: 0.12
Nodes (3): RecipientTokenService, reasonOf(), TransfersController

### Community 154 - "app.module.ts"
Cohesion: 0.09
Nodes (21): AppModule, TEST_ENV, config, loadConfig(), loadConfig(), configureApp(), GLOBAL_PREFIX, HealthController (+13 more)

### Community 155 - "TransferMetricsConfiguration.kt"
Cohesion: 0.33
Nodes (3): TransferMetrics, TransferMetricsConfiguration, TransferMetricsProperties

### Community 157 - "CloseAccountUseCaseTest.kt"
Cohesion: 0.08
Nodes (9): EventOutbox, ChangeAccountStatusUseCase, RecordingEventOutbox, ChangeAccountStatusUseCaseTest, MessagingContractBase, AccountBlocked, AccountClosed, AccountEvent (+1 more)

### Community 160 - "CustomerLookup"
Cohesion: 0.12
Nodes (10): CustomerProblemDTO, CustomerStatusResponseDTO, HttpCustomerGateway, CustomerGateway, CustomerLookup, ACTIVE, BLOCKED, NOT_FOUND (+2 more)

### Community 161 - "retryWhileAccountBusy"
Cohesion: 0.23
Nodes (3): isAccountBusy(), retryWhileAccountBusy(), RetryWhileAccountBusyTest

### Community 162 - "@angular/router"
Cohesion: 0.10
Nodes (22): App, appConfig, routes, BlankComponent, fail(), open(), retry(), settle() (+14 more)

### Community 163 - "uuid"
Cohesion: 0.03
Nodes (28): OutboxEventEnvelope, DirectTransactionRunner, RecordingEventOutbox, EntryKind, CREDIT, DEBIT, TransferRows, AccountLookup (+20 more)

### Community 169 - "TransferAdminController.kt"
Cohesion: 0.10
Nodes (12): CompensateManuallyDTO, ManualCompensationDTO, StuckTransferDTO, TransferAdminController, CompensationCreditAlreadyUsedException, CompensationCreditMismatchException, CompensationCreditNotFoundException, TransferAdminExceptionHandler (+4 more)

### Community 170 - "TransferEvent"
Cohesion: 0.21
Nodes (6): EventOutbox, EventOutbox, TransferCompensated, TransferCompleted, TransferEvent, TransferFailed

### Community 173 - "TransferStep"
Cohesion: 0.08
Nodes (15): Task 8: Domínio da transferência e passos da saga (Marina, `dev-kotlin`), 3. Modelo de domínio, Riscos, Transfer, Compensate, Compensated, Completed, Credit (+7 more)

### Community 174 - "SagaTimeoutsValidatorTest.kt"
Cohesion: 0.13
Nodes (6): Task 12: Cliente HTTP do `accounts` (Marina, `dev-kotlin`), 3. Autorização nos serviços, AccountsClientProperties, SagaTimeoutsValidator, SagaTimeoutsValidatorTest, ValidatorConfiguration

### Community 178 - "ApiClient"
Cohesion: 0.15
Nodes (3): RouteRulesIntegrationTest, ApiClient, ApiResponse

### Community 179 - "Customer"
Cohesion: 0.10
Nodes (19): 4. Subir os serviços `customer`, `accounts`, `transfers`, `notification` e `statement`, Alternativa: os cinco serviços pelas imagens Docker (perfil `apps`), Migrar o extrato de uma stack anterior ao DynamoDB Local, CallerHasNoCustomerException, CustomerNotFoundByPhoneException, CustomerNotFoundException, PhoneAlreadyInUseException, EventOutbox (+11 more)

### Community 181 - "CustomerRepository"
Cohesion: 0.15
Nodes (5): CustomerRepository, ChangePhoneUseCase, GetCustomerUseCase, GetOwnCustomerUseCase, UseCaseConfiguration

### Community 182 - "ManualResolution"
Cohesion: 0.25
Nodes (4): JdbcManualResolutionLog, ManualResolution, ManualResolutionLog, InMemoryManualResolutionLog

### Community 184 - "02-transfers-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 02-transfers-topic.sh script

### Community 186 - "accounts/adapters/web/OpenApiConfiguration.kt"
Cohesion: 0.10
Nodes (7): ApiDocs, OpenApiConfiguration, OpenApiConfiguration, OpenApiConfiguration, OpenApiConfiguration, ApiDocs, OpenApiConfiguration

### Community 187 - "transfer"
Cohesion: 0.22
Nodes (7): transfer, transfer_due_idx, transfer_step, transfer_step_transfer_idx, daily_limit_epoch, transfer_source_limit_day_idx, transfer_manual_resolution

### Community 188 - "TransferStatus"
Cohesion: 0.09
Nodes (22): Transferências, Etapa 2 — `transfers` + saga Implementation Plan, Global Constraints, Review Focus, 10. Documentação que acompanha, 5. Esquema do banco (`transfers`), 6. Eventos, 7. API, nginx e Bruno (+14 more)

### Community 189 - "GetStatementUseCaseTest"
Cohesion: 0.16
Nodes (4): statementEntry(), GetStatementUseCaseTest, StatementRepository, StatementCursor

### Community 190 - "InMemoryTransferRepository"
Cohesion: 0.11
Nodes (4): InMemoryTransferRepository, Lease, InMemoryTransferRepositoryTest, CountPendingTransfersUseCaseTest

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
Cohesion: 0.12
Nodes (7): MarkedNotificationsDTO, MarkNotificationsReadDTO, NotificationPageDTO, NotificationController, NotificationCursorCodec, InvalidField, InvalidRequestException

### Community 201 - "statement/adapters/messaging/EventEnvelopeReader.kt"
Cohesion: 0.15
Nodes (7): AccountOpened, EventEnvelopeReader, Ignored, LedgerEntryRecorded, MalformedEventException, ReceivedEvent, EventEnvelopeReaderTest

### Community 202 - "0014 — Tipo de conta e cheque especial"
Cohesion: 0.40
Nodes (5): 0014 — Tipo de conta e cheque especial, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 203 - "NotificationText"
Cohesion: 0.14
Nodes (3): NotificationContent, NotificationText, NotificationTextTest

### Community 204 - "TransferController.kt"
Cohesion: 0.13
Nodes (13): IdempotencyKeyHeader, TransferController, AccountsUnavailableException, CustomerBlockedException, CustomerNotFoundException, CustomersUnavailableException, InvalidField, InvalidRequestException (+5 more)

### Community 205 - "0007 — Conta e ledger no mesmo serviço (`accounts`)"
Cohesion: 0.50
Nodes (4): 0007 — Conta e ledger no mesmo serviço (`accounts`), Consequências, Contexto, Decisão

### Community 206 - "customer/adapters/web/ProblemDetailsExceptionHandler.kt"
Cohesion: 0.18
Nodes (3): InvalidField, InvalidPhoneException, ProblemDetailsExceptionHandler

### Community 207 - "ProblemResponsesOpenApiCustomizerTest.kt"
Cohesion: 0.10
Nodes (3): ObservabilityAutoConfigurationTest, SqsCorrelationIdAutoConfigurationTest, DigitalBankingSecurityAutoConfigurationTest

### Community 214 - ".decode"
Cohesion: 0.27
Nodes (4): Task 6: API do extrato (Marina, `dev-kotlin`; paralela à Task 5), InvalidCursorException, StatementCursorCodec, StatementCursorCodecTest

### Community 215 - "OutboxAdminController.kt"
Cohesion: 0.16
Nodes (3): DiscardOutboxEventDTO, StuckOutboxEventDTO, OutboxAdminController

### Community 217 - ".handle"
Cohesion: 0.15
Nodes (6): IdempotencyKeyHeader, AccountTypeNotFoundException, EntryNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException

### Community 220 - "0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS"
Cohesion: 0.40
Nodes (5): 0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 223 - "OpenAccountUseCaseTest"
Cohesion: 0.07
Nodes (19): CallerNotCustomerException, CustomerAlreadyHasAccountException, CustomerBlockedException, CustomerNotFoundException, CustomersUnavailableException, IdempotencyKeyReusedException, PendingTransfersException, CustomerGateway (+11 more)

### Community 224 - "TransfersIntegrationTest"
Cohesion: 0.09
Nodes (3): ClockWiringIntegrationTest, DeleteTransfersIntegrationTest, TransfersIntegrationTest

### Community 225 - "Banco digital em microsserviços"
Cohesion: 0.25
Nodes (8): Agentes do projeto, Ao terminar uma tarefa, Autonomia, Banco digital em microsserviços, Contexto, Fluxo de trabalho (skills), Git, Grafo do código (graphify)

### Community 227 - "04-statement.sh"
Cohesion: 0.43
Nodes (6): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, queue_arn(), 04-statement.sh script

### Community 228 - "auth.controller.ts"
Cohesion: 0.07
Nodes (27): ADR-0024, AuthController, Credentials, readCredentials(), readEmail(), isValidPassword(), APP_CONFIG, AppConfig (+19 more)

### Community 229 - "KeyValueStore"
Cohesion: 0.07
Nodes (8): KEY_VALUE_STORE, KeyValueStore, RedisKeyValueStore, RECIPIENT_TOKEN_TTL_SECONDS, Stored, Entry, InMemoryKeyValueStore, ioredis

### Community 231 - "Etapa 6 — limite diário: design"
Cohesion: 0.13
Nodes (15): 1. Fonte da verdade no PostgreSQL, 2. Contador no Redis (script Lua), 3. Passo `ReserveLimit` da saga, 4. Trava única e fallback no PostgreSQL, 5. Endpoint de consulta, 6. Observabilidade, 7. Testes, 8. Ambiente local, CI e documentação (+7 more)

### Community 232 - "transfers/bootstrap/contract/HttpContractBase.kt"
Cohesion: 0.11
Nodes (4): PendingTransfersDTO, PendingTransfersController, CountPendingTransfersUseCase, HttpContractBase

### Community 233 - "SecurityProblems.kt"
Cohesion: 0.21
Nodes (3): ProblemDetailsAccessDeniedHandler, ProblemDetailsAuthenticationEntryPoint, SecurityProblems

### Community 236 - "notification/bootstrap/DlqMetricsConfiguration.kt"
Cohesion: 0.17
Nodes (4): QueueProperties, DlqDepthMonitor, DlqDepthProbe, DlqMetricsConfiguration

### Community 237 - "AccountsApplicationIntegrationTest.kt"
Cohesion: 0.16
Nodes (5): Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`), AccountsApplicationIntegrationTest, ParameterizedTypeReference, RequestThreadProbe, RequestThreadProbeConfiguration

### Community 238 - "tick"
Cohesion: 0.11
Nodes (22): settle(), settle(), fill(), finishSignIn(), flush(), StatementComponent, backToPhone(), confirmAndLoseTheAnswer() (+14 more)

### Community 239 - "runapplication"
Cohesion: 0.24
Nodes (3): Task 3: Aplicação `accounts` com health do banco (Testcontainers), AccountsApplication, TransfersApplication

### Community 240 - "InfrastructureConfiguration"
Cohesion: 0.18
Nodes (3): DemoRelayConfiguration, InfrastructureConfiguration, OutboxRelayConfigurationSupportTest

### Community 241 - "DailyLimitProperties"
Cohesion: 0.13
Nodes (4): TransfersTimeoutDefaultTest, CircuitBreaker, DailyLimitProperties, DailyLimitPropertiesBindingTest

### Community 244 - ".get"
Cohesion: 0.15
Nodes (4): StatementDTO, StatementController, InvalidField, InvalidRequestException

### Community 250 - "ConsumeEventUseCase"
Cohesion: 0.26
Nodes (5): Etapa 3 — Mensageria e `notification` Implementation Plan, Frentes e ordem, Review Focus, ConsumeEventUseCase, Draft

### Community 252 - "ApiDocsIntegrationTest.kt"
Cohesion: 0.27
Nodes (4): ApiDocsIntegrationTest, Disabled, Enabled, TestHttp

### Community 253 - "DailyLimitController.kt"
Cohesion: 0.11
Nodes (8): DailyLimitController, DailyLimitDTO, AccountNotFound, AccountsUnavailable, DailyLimitOutcome, Found, GetDailyLimitUseCase, GetDailyLimitUseCaseTest

### Community 256 - "error.filter.ts"
Cohesion: 0.20
Nodes (7): BackendUnavailableError, ErrorFilter, problemBody(), statusOf(), GENERIC_ERROR, PROBLEM_MESSAGES, UNAVAILABLE

### Community 257 - "V1__notification_schema.sql"
Cohesion: 0.50
Nodes (4): known_accounts, notifications, notifications_customer_page_idx, processed_events

### Community 259 - "send"
Cohesion: 0.12
Nodes (7): AccountsApiClient, CustomerApiClient, HttpResult, NotificationApiClient, send(), StatementApiClient, TransfersApiClient

### Community 260 - "migrate_from_localstack.py"
Cohesion: 0.15
Nodes (6): DynamoDbHttpClient, main(), MigrationError, read_items(), run_docker(), schema()

### Community 263 - "0018 — Extrato ordenado pela sequência do ledger"
Cohesion: 0.40
Nodes (5): 0018 — Extrato ordenado pela sequência do ledger, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 264 - "CorrelationIdFilter"
Cohesion: 0.24
Nodes (4): Task 9: API HTTP, Problem Details e correlation ID (Marina, `dev-kotlin`), 5. `libs/observabilidade`, libs/observabilidade, CorrelationIdFilter

### Community 271 - "Etapa 5 — cache do extrato: design"
Cohesion: 0.14
Nodes (14): 2. Adapter Redis, 3. Consistência, 4. Observabilidade, 5. Configuração, 6. Testes, 7. Ambiente local e CI, 8. Documentação que acompanha, Critério de pronto (+6 more)

### Community 275 - "SecurityTestApplication.kt"
Cohesion: 0.16
Nodes (3): CatchAllExceptionHandler, SecurityTestApplication, SecurityTestController

### Community 276 - "NotificationAccessIntegrationTest"
Cohesion: 0.13
Nodes (4): ApiClient, ApiResponse, assertProblem(), NotificationAccessIntegrationTest

### Community 280 - "ApiClient"
Cohesion: 0.14
Nodes (3): TransferLeaseContentionIntegrationTest, ApiClient, PendingTransfersApiIntegrationTest

### Community 281 - "0002 — Clean Architecture por serviço"
Cohesion: 0.40
Nodes (5): 0002 — Clean Architecture por serviço, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 283 - "StatementCacheProperties"
Cohesion: 0.19
Nodes (3): StatementCacheProperties, StatementCachePropertiesTest, CacheConfiguration

### Community 284 - "transfer.component.ts"
Cohesion: 0.13
Nodes (14): CompleteProfileComponent, FINAL_STATUSES, RecipientDTO, STATUS_TITLES, Step, TransferStatusDTO, parseAmountToCents(), MoneyPipe (+6 more)

### Community 287 - "SecurityRules"
Cohesion: 0.13
Nodes (6): Task 2: `GET /accounts/me` e `GET /accounts/recipient` (Marina, `dev-kotlin`), SecurityRules, AccountsSecurityConfiguration, NotificationSecurityConfiguration, StatementSecurityConfiguration, TransfersSecurityConfiguration

### Community 289 - "StatementPage"
Cohesion: 0.07
Nodes (22): Etapa 5 — cache do extrato: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Revisão final da branch (Beatriz), Rulings deste plano, Task 1: porta do cache e casos de uso, Task 2: adapter Redis (+14 more)

### Community 290 - "RunTransferSagaUseCase"
Cohesion: 0.20
Nodes (3): CallResult, Run, RunTransferSagaUseCase

### Community 297 - "ReceivedEvent"
Cohesion: 0.17
Nodes (11): AccountBlocked, AccountClosed, AccountOpened, AccountTypeChanged, AccountUnblocked, CustomerRegistered, ReceivedEvent, TransferCompensated (+3 more)

### Community 300 - "accounts/adapters/customer/HttpCustomerGateway.kt"
Cohesion: 0.18
Nodes (3): CustomerStatusResponseDTO, PendingTransfersResponseDTO, HttpTransfersGateway

### Community 303 - "support.ts"
Cohesion: 0.16
Nodes (20): devDependencies, @playwright/test, name, private, scripts, e2e, version, completeProfile() (+12 more)

### Community 304 - "ServiceTokenClientIntegrationTest"
Cohesion: 0.13
Nodes (3): MutableClock, ServiceTokenClientIntegrationTest, TokenRequest

### Community 305 - "AccountsIntegrationTest"
Cohesion: 0.10
Nodes (5): AccountCustomerIndexIntegrationTest, AccountsIntegrationTest, UpdateGrantsIntegrationTest, UseCaseConfigurationIntegrationTest, WelcomeBonusDefaultIntegrationTest

### Community 306 - "status.md"
Cohesion: 0.07
Nodes (15): 0009 — Cache do extrato, Alternativas consideradas, Consequências, Contexto, Decisão, Frente B — débitos técnicos, Task B1: Configuração do relay em `libs/outbox-relay` (Marina, `dev-kotlin`) — cartão #92, Task B2: Backoff do relay e testes (Marina, `dev-kotlin`) — cartões #24 e #26, depois da B1 (+7 more)

### Community 307 - "frontend/package.json"
Cohesion: 0.08
Nodes (25): devDependencies, angular-eslint, eslint, eslint-config-prettier, @eslint/js, prettier, typescript-eslint, engines (+17 more)

### Community 308 - "problemMessage"
Cohesion: 0.15
Nodes (6): AccountComponent, SignInComponent, SignUpComponent, passwordsMatch(), sixDigitPassword(), problemMessage()

### Community 313 - "Ordem e paralelismo"
Cohesion: 0.29
Nodes (7): Ordem e paralelismo, Task 2: ambiente local de identidade, Task 3: Terraform do Cognito e do API Gateway (sem aplicar), Task 4: serviço `customer`, Task 6: `transfers` com JWT, dono e consulta ao `customer`, Task 7: `statement` e `notification` com JWT, Task 8: borda local, e2e, seed, smoke e Bruno

### Community 318 - "Etapa 7 — borda e identidade: design"
Cohesion: 0.13
Nodes (15): 10. Documentação, 1. Identidade e o id do cliente, 2. Serviço `customer`, 4. Consulta ao `customer` no pedido, 6. Segurança transversal, 7. Ambiente local, 8. Terraform (sem aplicar), 9. Testes (+7 more)

### Community 321 - "RedisCircuitBreaker"
Cohesion: 0.23
Nodes (5): RedisCircuitBreaker, State, CLOSED, HALF_OPEN, OPEN

### Community 324 - "accounts/adapters/persistence/UuidV7IdGenerator.kt"
Cohesion: 0.15
Nodes (3): UuidV7IdGenerator, UuidV7IdGenerator, UuidV7IdGenerator

### Community 331 - "NotificationQueues"
Cohesion: 0.12
Nodes (5): DeadLetter, NotificationQueues, PublishedMessage, ReceivedEvent, TransferEventsQueue

### Community 334 - "CustomerTest"
Cohesion: 0.18
Nodes (3): Task 3: telefone do cliente no `customer` (Marina, `dev-kotlin`), CustomerTest, PhoneNumberTest

### Community 336 - "test-app.ts"
Cohesion: 0.10
Nodes (18): Case, Failure, Route, ROUTES, accountDto, customerDto, IDS, serviceProblem() (+10 more)

### Community 340 - "OrderController.kt"
Cohesion: 0.25
Nodes (4): OrderController, PlaceOrderCommand, PlaceOrderResult, PlaceOrderUseCase

### Community 341 - "Etapa 0 — Fundação: design"
Cohesion: 0.18
Nodes (11): 2. Build e convenções, 3. Esqueleto do `accounts`, 4. Ambiente local, 5. CI (GitHub Actions), Critério de pronto, Etapa 0 — Fundação: design, Fora de escopo (e onde foi registrado), Objetivo (+3 more)

### Community 343 - "SpringTransactionRunnerIntegrationTest.kt"
Cohesion: 0.13
Nodes (3): SpringTransactionRunner, TransactionRunner, SpringTransactionRunnerIntegrationTest

### Community 344 - "05-customer-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 05-customer-topic.sh script

### Community 348 - "0010 — Limite diário com Redis + Lua"
Cohesion: 0.40
Nodes (5): 0010 — Limite diário com Redis + Lua, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 349 - "0020 — Identidade e autorização"
Cohesion: 0.40
Nodes (5): 0020 — Identidade e autorização, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 351 - "CustomerServiceStub"
Cohesion: 0.13
Nodes (9): Active, Behavior, Blocked, CustomerServiceStub, Lookup, NotFound, ServerError, Slow (+1 more)

### Community 352 - "token.sh"
Cohesion: 0.83
Nodes (3): request(), token.sh script, usage()

### Community 353 - "V2__outbox.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 354 - "seed.sh"
Cohesion: 0.47
Nodes (8): bff_post(), call(), fetch_notifications(), local_sub_for(), notification_found_kinds(), notification_names_ok(), seed.sh script, transfer_status()

### Community 355 - "seed-check.sh"
Cohesion: 0.83
Nodes (3): demo_lines(), owner_lines(), seed-check.sh script

### Community 373 - "assertOutboxSchemaMatchesModel"
Cohesion: 0.12
Nodes (7): OutboxSchemaAssertionsIntegrationTest, assertOutboxSchemaMatchesModel(), createModelSchema(), currentSchema(), OutboxSchemaSnapshot, OutboxSchemaIntegrationTest, OutboxSchemaIntegrationTest

### Community 376 - "compilerOptions"
Cohesion: 0.10
Nodes (20): compilerOptions, allowSyntheticDefaultImports, declaration, emitDecoratorMetadata, esModuleInterop, experimentalDecorators, incremental, isolatedModules (+12 more)

### Community 379 - "NotificationEventListener"
Cohesion: 0.14
Nodes (6): NotificationEventListener, ListenerSender, ConsumeOutcome, DUPLICATE, IGNORED, PROCESSED

### Community 380 - "0019 — Runtime na AWS com EKS"
Cohesion: 0.33
Nodes (6): 0019 — Runtime na AWS com EKS, Alternativas consideradas, Consequências, Contexto, Decisão, Papel do GitHub Actions

### Community 381 - "0023 — Status da conta e encerramento"
Cohesion: 0.40
Nodes (5): 0023 — Status da conta e encerramento, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 384 - "SessionService"
Cohesion: 0.17
Nodes (3): onLoadedSession(), isSessionGone(), SessionService

### Community 385 - "notification-bell.component.spec.ts"
Cohesion: 0.16
Nodes (4): NotificationBellComponent, BlankComponent, openWith(), settle()

### Community 387 - "devDependencies"
Cohesion: 0.12
Nodes (16): devDependencies, @nestjs/cli, @nestjs/schematics, @nestjs/testing, prettier, source-map-support, supertest, @swc/core (+8 more)

### Community 391 - "SnsEventPublisherIntegrationTest.kt"
Cohesion: 0.08
Nodes (10): Bloco 2 — relay e SNS FIFO, Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), Task 2: Relay em módulo próprio e teste de esquema (Marina, `dev-kotlin`) — cartões #25 e #30, SnsEventPublisherIntegrationTest, EventPublisher, SnsEventPublisher, NoOpPublisher, PendingEvent (+2 more)

### Community 395 - "customer/bootstrap/contract/HttpContractBase.kt"
Cohesion: 0.11
Nodes (4): CustomerSecurityConfiguration, CustomerLookupDTO, InternalCustomerController, HttpContractBase

### Community 396 - "Etapa 9: frontend do cliente (design)"
Cohesion: 0.13
Nodes (15): 1. Intenção, 2. Objetivos, 3. Fora do escopo, 4. Decisões do usuário, 5. Abordagens, 6.1 Estrutura, 6.2 Telas, 6.3 BFF (+7 more)

### Community 398 - "Ordem e paralelismo"
Cohesion: 0.12
Nodes (15): Etapa 8 — acabamento: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Revisão final da branch (Beatriz), Task 10: subida real (pendente do usuário), Task 1: Dockerfile em camadas e smoke contra as imagens, Task 2: chart Helm (+7 more)

### Community 399 - "Etapa 8 — acabamento: design"
Cohesion: 0.13
Nodes (15): 1. Imagens, 2. Chart Helm, 3. Terraform, 4. Workflows, 5. Testes de contrato, 6. README e diagrama, 7. Revisão dos ADRs, Abordagens consideradas (+7 more)

### Community 401 - "re"
Cohesion: 0.24
Nodes (5): default_of(), access_services(), block(), chart_values(), variable_default()

### Community 402 - "StuckTransferAdminUseCasesTest.kt"
Cohesion: 0.05
Nodes (17): Task 9: Portas e execução da saga (Marina, `dev-kotlin`), JdbcTransferStepLog, CorrelationContext, StepRecord, TransferStepLog, FakeAccountsGateway, FakeAccountsGatewayTest, FakeEventOutbox (+9 more)

### Community 404 - "properties"
Cohesion: 0.14
Nodes (13): additionalProperties, description, type, description, description, type, type, properties (+5 more)

### Community 405 - "TransferComponent"
Cohesion: 0.14
Nodes (5): sha256(), TransferAttempt, TransferAttemptStore, TransferIntent, TransferComponent

### Community 407 - "serviceRef"
Cohesion: 0.15
Nodes (13): description, type, anyOf, description, x-kubernetes-int-or-string, name, port, serviceRef (+5 more)

### Community 409 - "properties"
Cohesion: 0.17
Nodes (12): description, enum, type, ipAddressType, targetType, vpcID, properties, description (+4 more)

### Community 410 - "AppShellComponent"
Cohesion: 0.16
Nodes (3): Task 8: Web — casca, Entrar, Criar conta, Completar cadastro (Lucas, `dev-frontend`), EntryIconComponent, AppShellComponent

### Community 412 - "check_stack"
Cohesion: 0.32
Nodes (3): check_stack(), CheckStackTest, FakeDocker

### Community 414 - "ApiClient"
Cohesion: 0.09
Nodes (4): ClockWiringIntegrationTest, OutboxRelayUnreachableBrokerIntegrationTest, ApiClient, ApiResponse

### Community 415 - "deploy-services.sh"
Cohesion: 0.38
Nodes (9): env_value(), load_service_args(), out(), out_map(), required_env(), deploy-services.sh script, fail(), has() (+1 more)

### Community 417 - "CustomerEventsIntegrationTest"
Cohesion: 0.19
Nodes (3): CustomerEventsIntegrationTest, OutboxRow, DatabaseIntegrationTest

### Community 419 - "Status do projeto"
Cohesion: 0.17
Nodes (12): Ambiente do Claude, Ambiente que ficou de pé, Como rodar localmente, Como usar o board, Concluído, Convenções que já custaram caro, Decisões do usuário em 2026-11-22, Depois do merge (+4 more)

### Community 422 - "matchExpressions"
Cohesion: 0.20
Nodes (10): description, items, type, x-kubernetes-list-type, additionalProperties, description, type, properties (+2 more)

### Community 423 - "StuckTransfer"
Cohesion: 0.22
Nodes (4): JdbcStuckTransferFinder, StuckTransfer, StuckTransferFinder, StuckTransferFinder

### Community 427 - ".resumeDueOver"
Cohesion: 0.09
Nodes (5): AccountsGateway, CorrelationContext, DailyLimitGateway, TransactionRunner, TransferStepLog

### Community 428 - "networking"
Cohesion: 0.22
Nodes (9): description, items, type, additionalProperties, description, properties, type, ingress (+1 more)

### Community 429 - "status"
Cohesion: 0.22
Nodes (9): description, format, type, observedGeneration, status, additionalProperties, description, properties (+1 more)

### Community 430 - "Banco digital em microsserviços"
Cohesion: 0.20
Nodes (10): App do cliente, Arquitetura, capzzPay, Como rodar localmente, Como subir na AWS, Custo por subida, Decisões principais, Pontos fortes (+2 more)

### Community 433 - "Chart Helm dos serviços"
Cohesion: 0.25
Nodes (8): App do cliente (web e bff), Chart Helm dos serviços, Instalação na AWS, Job `db-init`, Propriedades sem placeholder, Região, ServiceAccount e Pod Identity, Valores obrigatórios

### Community 438 - "Ondas de execução (paralelismo)"
Cohesion: 0.15
Nodes (13): Etapa 9: frontend do cliente — plano de implementação, Global Constraints, Mapa de arquivos, Ondas de execução (paralelismo), Review Focus, Task 10: imagens, compose, nginx e charts (Rafael, `infra`), Task 11: usuário de demonstração no seed (Rafael, `infra`), Task 12: ADR, `CONTEXT.md` e README (Clara, `documentador`) (+5 more)

### Community 444 - "test-local.sh"
Cohesion: 0.53
Nodes (4): as(), run_job(), test-local.sh script, write_secret()

### Community 445 - "Bloco 1 — preparação para o 2º serviço (branch `etapa-2-transfers`, PR 1)"
Cohesion: 0.33
Nodes (6): Bloco 1 — preparação para o 2º serviço (branch `etapa-2-transfers`, PR 1), Task 1: Gradle para vários serviços (Rafael, `infra`) — cartão #37, Task 3: Regras ArchUnit compartilhadas (Marina, `dev-kotlin`) — cartão #32, Task 4: Consulta de lançamento pela chave no `accounts` (Marina, `dev-kotlin`), Task 5: PostgreSQL com um banco por serviço (Rafael, `infra`) — cartão #38, Task 6: Build seletivo no CI (Rafael, `infra`)

### Community 446 - "Sininho de notificações (design curto)"
Cohesion: 0.22
Nodes (8): Abordagens, Consequência aceita, Decisões do usuário, Desenho (escolhas da coordenadora, reversíveis), Fora do escopo, Intenção, Sininho de notificações (design curto), Testes

### Community 447 - "JdbcOutboxWriter"
Cohesion: 0.09
Nodes (17): 10. Documentação que acompanha, 3. Esquema do banco, 5. Outbox e relay (`libs/outbox`), 6. API, 7. Testes, 8. Ambiente local e CI, 9. Entrega em dois blocos, Critério de pronto (+9 more)

### Community 448 - "create-service-databases.sh"
Cohesion: 0.70
Nodes (4): create_service_db(), json_field(), PGDATABASE, create-service-databases.sh script

### Community 449 - "nodeSelector"
Cohesion: 0.40
Nodes (5): additionalProperties, description, type, x-kubernetes-map-type, nodeSelector

### Community 450 - "spec"
Cohesion: 0.40
Nodes (5): spec, additionalProperties, description, required, type

### Community 451 - "0016 — Saga de transferência orquestrada por HTTP síncrono"
Cohesion: 0.40
Nodes (5): 0016 — Saga de transferência orquestrada por HTTP síncrono, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 453 - "cognito-user.sh"
Cohesion: 0.70
Nodes (4): idp(), random_password(), cognito-user.sh script, usage()

### Community 454 - "helm-teardown.test.sh"
Cohesion: 0.70
Nodes (4): check_count(), expect(), helm-teardown.test.sh script, uninstalls()

### Community 456 - "tsconfig.build.json"
Cohesion: 0.29
Nodes (6): compilerOptions, rootDir, exclude, extends, include, ./tsconfig.json

### Community 457 - "check-alb-routes.py"
Cohesion: 0.29
Nodes (5): alb_matcher(), alb_routes(), main(), nginx_routes(), main()

### Community 458 - "Frontend"
Cohesion: 0.29
Nodes (6): Comandos (na pasta `frontend/`), Ferramentas, Frontend, Tema, Variáveis do BFF, Versões (conferidas em 2026-11-19 nas fontes primárias)

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

### Community 464 - "seed-notifications-test.sh"
Cohesion: 0.48
Nodes (5): expect(), expect_kinds(), fake_seed(), item(), seed-notifications-test.sh script

### Community 474 - "0025 — Nome da contraparte nas notificações"
Cohesion: 0.33
Nodes (6): 0025 — Nome da contraparte nas notificações, Alternativas consideradas, Consequências, Contexto, Decisão, Emenda de 2026-11-22

### Community 476 - "project_gate_revisao_task.md"
Cohesion: 0.14
Nodes (5): Service, O que a lib faz sozinha, CallerRoles, ApplicationWithSpring, EventOutbox

### Community 477 - "nest-cli.json"
Cohesion: 0.33
Nodes (5): collection, compilerOptions, deleteOutDir, $schema, sourceRoot

### Community 479 - "RequestTransferOutcome"
Cohesion: 0.24
Nodes (10): AccountNotFound, AccountsUnavailable, Created, CustomerBlocked, CustomerNotFound, CustomersUnavailable, Existing, RequestTransferOutcome (+2 more)

### Community 480 - "0001 — Stack: Kotlin + Spring Boot 4 na AWS"
Cohesion: 0.40
Nodes (5): 0001 — Stack: Kotlin + Spring Boot 4 na AWS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 481 - "0024 — Login e telefone do app"
Cohesion: 0.40
Nodes (5): 0024 — Login e telefone do app, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 484 - "0004 — API Gateway HTTP API na borda"
Cohesion: 0.40
Nodes (5): 0004 — API Gateway HTTP API na borda, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 490 - "0021 — Deploy e ambiente efêmero na AWS"
Cohesion: 0.40
Nodes (5): 0021 — Deploy e ambiente efêmero na AWS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 491 - "Frente A — `notification`"
Cohesion: 0.22
Nodes (9): Frente A — `notification`, Task A10: Documentação (Clara, `documentador`) — paralela desde o início, Task A1: Esqueleto do `notification` e prova do `@SqsListener` (Marina, `dev-kotlin`), Task A2: Domínio das notificações (Marina, `dev-kotlin`), Task A4: Persistência (Marina, `dev-kotlin`), Task A5: Consumo das filas (Marina, `dev-kotlin`) — paralela à A6, Task A7: `AccountTypeChanged` versão 2 no `accounts` (Marina, `dev-kotlin`) — paralela desde o início, Task A8: Infra local, Bruno, smoke e CI do `notification` (Rafael, `infra`) (+1 more)

### Community 494 - "assertProblem"
Cohesion: 0.40
Nodes (3): ApiClient, ApiResponse, assertProblem()

### Community 497 - "0026 — Bônus de boas-vindas por cliente"
Cohesion: 0.40
Nodes (5): 0026 — Bônus de boas-vindas por cliente, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 501 - "DynamoDB Local com volume (design curto)"
Cohesion: 0.33
Nodes (5): Abordagens, Desenho, DynamoDB Local com volume (design curto), Fora do escopo, Intenção

### Community 502 - "persistence-check.sh"
Cohesion: 0.60
Nodes (5): post(), read_statement(), persistence-check.sh script, token(), uuid()

### Community 508 - "0027 — DynamoDB Local com volume"
Cohesion: 0.40
Nodes (5): 0027 — DynamoDB Local com volume, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 509 - "run.sh"
Cohesion: 0.80
Nodes (4): call(), run.sh script, stop(), table_exists()

### Community 513 - "RecordedEntry"
Cohesion: 0.32
Nodes (4): EntryKind, CREDIT, DEBIT, RecordedEntry

### Community 519 - "Bloqueio e encerramento de conta: design"
Cohesion: 0.29
Nodes (7): Abordagens consideradas, Bloqueio e encerramento de conta: design, Decisões (usuário, 2026-11-16), Desenho, Fora de escopo, Objetivo, Onde mexe

### Community 520 - "0022 — Testes de contrato com Stubborn Contract"
Cohesion: 0.40
Nodes (5): 0022 — Testes de contrato com Stubborn Contract, Alternativas consideradas, Consequências, Contexto, Decisão

## Knowledge Gaps
- **122 isolated node(s):** `PGDATABASE`, `@nestjs/platform-express`, `reflect-metadata`, `rxjs`, `@nestjs/cli` (+117 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 1784 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **288 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Ondas de execução (paralelismo)` connect `Ondas de execução (paralelismo)` to `CustomerTest`, `transfers.controller.ts`, `AppShellComponent`, `test-app.ts`, `SecurityRules`?**
  _High betweenness centrality (0.079) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Transfer` (e.g. with `Transferências` and `Global Constraints`) actually correct?**
  _`Transfer` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PGDATABASE`, `@nestjs/platform-express`, `reflect-metadata` to the rest of the system?**
  _122 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SqsListenerFifoIntegrationTest.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.14039408866995073 - nodes in this community are weakly interconnected._
- **Why does `Task 7: BFF — endpoints por tela (Lucas, `dev-frontend`)` connect `transfers.controller.ts` to `Ondas de execução (paralelismo)`?**
  _High betweenness centrality (0.048) - this node is a cross-community bridge._
- **Should `AccountApiIntegrationTest` be split into smaller, more focused modules?**
  _Cohesion score 0.11153846153846154 - nodes in this community are weakly interconnected._
- **Why does `Task 1: bônus de boas-vindas no `accounts` (Marina, `dev-kotlin`)` connect `Ondas de execução (paralelismo)` to `LedgerEntry`, `Caller`, `OpenAccountUseCaseTest`?**
  _High betweenness centrality (0.043) - this node is a cross-community bridge._