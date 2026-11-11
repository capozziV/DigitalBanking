# Graph Report - DigitalBanking  (2026-11-10)

## Corpus Check
- 424 files · ~120,848 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 29 file(s) not represented in the graph (top: .bru 19, (none) 3, .properties 2)

## Summary
- 3731 nodes · 8968 edges · 247 communities (109 shown, 138 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 688 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `b3766f87`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- accounts/bootstrap/UseCaseConfiguration.kt
- SqsListenerFifoIntegrationTest.kt
- AccountApiIntegrationTest
- SpringTransactionRunner
- TransferApiIntegrationTest
- JdbcNotificationRepositoryIntegrationTest
- ServiceArchitectureRulesTest
- .transfer
- Account
- AccountNotFoundException
- ListNotificationsUseCaseTest
- ProblemDetailsExceptionHandler.kt
- OutboxRelayIntegrationTest.kt
- TransferSchemaIntegrationTest
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
- OpenApiIntegrationTest
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
- OutboxRelay
- JdbcTransferRepositoryIntegrationTest
- SagaPropertiesTest.kt
- NotificationConsumptionIntegrationTest
- ProcessedEventRepository
- smoke.sh
- assertOutboxSchemaMatchesModel
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
- RunTransferSagaUseCaseTest
- ConsumeEventUseCaseTest
- LocalStackTestSupport
- MdcCorrelationContextTest.kt
- SagaIntegrationSupport.kt
- AccountTypeAdminController.kt
- InMemoryTransferRepository
- retryWhileAccountBusy
- TransferEventsIntegrationTest.kt
- Etapa 3 — Mensageria e `notification`: design
- MarkNotificationsReadUseCaseTest
- NotificationApiIntegrationTest
- RunTransferSagaUseCase
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
- OutboxRelaySettings
- TransferMetricsConfiguration.kt
- JdbcProcessedEventRepositoryIntegrationTest.kt
- ApiClient
- ProcessLogsOnFailureExtension
- ServiceProcess
- InMemoryTransferRepositoryTest
- IdGenerator
- SchedulerLeaseOwners
- RunTransferSagaUseCaseTest.kt
- EndToEndInputs
- .run
- .entry
- Test
- TransactionRunner
- Etapa 2 — `transfers` + saga: design
- uuid
- TransferSagaSchedulerTest.kt
- .brl
- JdbcOutboxWriter
- TransferStep
- TransfersIntegrationTest
- LocalStackInitScripts
- .resumeDueOver
- RetryBackoffTest
- AccountRequestDTOs.kt
- 02-transfers-topic.sh
- OpenApiIntegrationTest
- accounts/adapters/web/OpenApiConfiguration.kt
- transfer
- HttpAccountsGateway.kt
- transfers/adapters/web/OpenApiConfiguration.kt
- JdbcOutboxWriterIntegrationTest.kt
- 03-notification-queues.sh
- V3__outbox.sql
- accounts/bootstrap/web/UnexpectedErrorApiIntegrationTest.kt
- notification/adapters/web/OpenApiConfiguration.kt
- OrderController.kt
- NotificationCursor
- .accounts
- 0014 — Tipo de conta e cheque especial
- ConsumeEventUseCase.kt
- Order
- 0007 — Conta e ledger no mesmo serviço (`accounts`)
- transfers/bootstrap/web/UnexpectedErrorApiIntegrationTest.kt
- transfers/adapters/web/CorrelationIdFilter.kt
- BlockingRepository
- Regras inegociáveis
- TransactionRunner
- accounts/adapters/web/CorrelationIdFilter.kt
- CreditStepTest
- OutboxSchemaAssertions.kt
- Frente A — `notification`
- V1__notification_schema.sql
- JdbcTransferStepLogIntegrationTest.kt
- AccountsIntegrationTest
- DebitStepTest
- 0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS
- AccountsApplication.kt
- 2026-10-21-etapa-0-fundacao-design.md
- Frente B — débitos técnicos
- 04-statement.sh
- IdGenerator
- CorrelationContext
- Transfer.kt
- NotificationCursorCodec.kt
- roadmap.md
- Handler
- Status do projeto
- TransfersApplication.kt
- .updateLimit
- transfers/bootstrap/ClockWiringIntegrationTest.kt
- .inTransaction
- bootstrap/OutboxSchemaIntegrationTest.kt
- persistence/OutboxSchemaIntegrationTest.kt
- .record

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
- `Task 4: Consulta de lançamento pela chave no `accounts` (Marina, `dev-kotlin`)` --references--> `AccountNotFoundException`  [INFERRED]
  docs/superpowers/plans/2026-10-27-etapa-2-transfers.md → services/accounts/application/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/application/ApplicationExceptions.kt
- `Task 3: Domínio e casos de uso (Marina, `dev-kotlin`)` --references--> `AccountNotFoundException`  [INFERRED]
  docs/superpowers/plans/2026-11-10-etapa-4-statement.md → services/accounts/application/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/application/ApplicationExceptions.kt
- `Task 6: API do extrato (Marina, `dev-kotlin`; paralela à Task 5)` --references--> `AccountNotFoundException`  [INFERRED]
  docs/superpowers/plans/2026-11-10-etapa-4-statement.md → services/accounts/application/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/application/ApplicationExceptions.kt
- `Task A7: `AccountTypeChanged` versão 2 no `accounts` (Marina, `dev-kotlin`) — paralela desde o início` --references--> `LedgerEntryRecorded`  [INFERRED]
  docs/superpowers/plans/2026-10-28-etapa-3-mensageria.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/AccountEvent.kt

## Import Cycles
- None detected.

## Communities (247 total, 138 thin omitted)

### Community 0 - "accounts/bootstrap/UseCaseConfiguration.kt"
Cohesion: 0.07
Nodes (10): 1. Módulos e camadas, 4. Fluxos, Restrições (de `CLAUDE.md` e ADRs), ChangeAccountTypeUseCase, ChangeOverdraftLimitUseCase, GetAccountUseCase, ListAccountTypesUseCase, OpenAccountUseCase (+2 more)

### Community 1 - "SqsListenerFifoIntegrationTest.kt"
Cohesion: 0.06
Nodes (8): LocalStackFifoIntegrationTest, ReceivedMessage, QueueProperties, ListenersConfiguration, SqsListenerFifoIntegrationTest, TestListeners, DlqDepthProbe, DlqMetricsConfiguration

### Community 5 - "JdbcNotificationRepositoryIntegrationTest"
Cohesion: 0.24
Nodes (3): JdbcNotificationRepositoryIntegrationTest, aNotification(), microsNow()

### Community 6 - "ServiceArchitectureRulesTest"
Cohesion: 0.06
Nodes (5): ServiceArchitectureRules, ServiceArchitectureRulesTest, ArchitectureTest, ArchitectureTest, ArchitectureTest

### Community 7 - ".transfer"
Cohesion: 0.14
Nodes (4): FakeAccountsGatewayTest, Money, MoneyTest, TransferTest

### Community 8 - "Account"
Cohesion: 0.09
Nodes (9): JdbcAccountRepository, AccountRepository, InsertAccountResult, DUPLICATE_CUSTOMER, DUPLICATE_OPENING_KEY, INSERTED, InMemoryAccountRepository, InMemoryAccountRepositoryTest (+1 more)

### Community 9 - "AccountNotFoundException"
Cohesion: 0.06
Nodes (32): Ledger, Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 13: Documentação do bloco 1 e das decisões da etapa (Clara, `documentador`; em paralelo às Tasks 8–12), Task 2: Valores monetários e tipos (Marina, `dev-kotlin`), Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`) (+24 more)

### Community 10 - "ListNotificationsUseCaseTest"
Cohesion: 0.24
Nodes (5): Etapa 3 — Mensageria e `notification` Implementation Plan, Frentes e ordem, Global Constraints, Review Focus, ListNotificationsUseCaseTest

### Community 11 - "ProblemDetailsExceptionHandler.kt"
Cohesion: 0.08
Nodes (8): IdempotencyKeyHeader, ProblemDetailsExceptionHandler, AccountTypeNotFoundException, EntryNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException, BalanceOverflowException

### Community 12 - "OutboxRelayIntegrationTest.kt"
Cohesion: 0.09
Nodes (3): OutboxRelayIntegrationTest, ReceivedEvent, RecordingPublisherConfiguration

### Community 14 - "LedgerEntry"
Cohesion: 0.08
Nodes (10): JdbcLedgerEntryRepository, LedgerEntryRepository, FindEntryByIdempotencyKeyUseCase, InMemoryLedgerEntryRepository, FindEntryByIdempotencyKeyUseCaseTest, DebitResult, EntryRecorded, Recorded (+2 more)

### Community 16 - "OutboxRelayTest"
Cohesion: 0.09
Nodes (6): FakeEventPublisher, FakeOutboxStore, MutableClock, OutboxRelayTest, Lease, TransfersDatabase

### Community 17 - "duration"
Cohesion: 0.07
Nodes (19): 2. Modelo de domínio, JdbcAccountTypeRepository, AccountTypeRepository, CallLog, DirectTransactionRunner, InMemoryAccountTypeRepository, ChangeAccountTypeUseCaseTest, ChangeOverdraftLimitUseCaseTest (+11 more)

### Community 18 - "Decisões tomadas"
Cohesion: 0.13
Nodes (15): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Em aberto, Fluxo de entrada (+7 more)

### Community 21 - "SnsEventPublisherIntegrationTest.kt"
Cohesion: 0.09
Nodes (12): Task 7: `libs/outbox` — envelope e writer (Marie, `dev-kotlin`; em paralelo às Tasks 2–6), Bloco 1 — preparação para o 2º serviço (branch `etapa-2-transfers`, PR 1), Task 1: Gradle para vários serviços (Rafael, `infra`) — cartão #37, Task 2: Relay em módulo próprio e teste de esquema (Marina, `dev-kotlin`) — cartões #25 e #30, Task 4: Consulta de lançamento pela chave no `accounts` (Marina, `dev-kotlin`), Task 5: PostgreSQL com um banco por serviço (Rafael, `infra`) — cartão #38, Task 6: Build seletivo no CI (Rafael, `infra`), 6. Eventos consumidos e mudança no `accounts` (+4 more)

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
Cohesion: 0.23
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
Cohesion: 0.09
Nodes (20): Etapa 4 — `statement`: plano de implementação, Global Constraints, Ordem e paralelismo, Review Focus, Task 1: Esqueleto Gradle e cliente DynamoDB no apoio de testes (Rafael, `infra`), Task 2: LocalStack, compose, nginx e CI (Rafael, `infra`), Task 3: Domínio e casos de uso (Marina, `dev-kotlin`), Task 4: Repositório DynamoDB e bootstrap (Marina, `dev-kotlin`) (+12 more)

### Community 63 - "account"
Cohesion: 0.33
Nodes (3): account_type, account, ledger_entry

### Community 64 - "Balance"
Cohesion: 0.10
Nodes (8): AccountView, GetAccountUseCaseTest, AvailableBalance, Balance, currentBalance(), Money, AccountTest, BalanceTest

### Community 65 - "outbox-table.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 69 - "LedgerEntryRecorded"
Cohesion: 0.09
Nodes (15): 0018 — Extrato ordenado pela sequência do ledger, Alternativas consideradas, Consequências, Contexto, Decisão, RecordCreditUseCase, DebitOutcome, Recorded (+7 more)

### Community 70 - "OutboxRelay"
Cohesion: 0.09
Nodes (11): Bloco 2 — relay e SNS FIFO, Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), 1. Entrega em dois blocos, EventPublisher, JdbcOutboxStore, OutboxMetrics, OutboxRelay (+3 more)

### Community 71 - "JdbcTransferRepositoryIntegrationTest"
Cohesion: 0.17
Nodes (3): JdbcTransferRepositoryIntegrationTest, aTransfer(), microsNow()

### Community 72 - "SagaPropertiesTest.kt"
Cohesion: 0.07
Nodes (7): AccountsClientConfiguration, AccountsClientProperties, SagaTimeoutsValidator, PropertiesConfiguration, SagaPropertiesTest, SagaTimeoutsValidatorTest, ValidatorConfiguration

### Community 73 - "NotificationConsumptionIntegrationTest"
Cohesion: 0.06
Nodes (7): NotificationConsumptionIntegrationTest, NotificationApplication, DeadLetter, NotificationQueues, PublishedMessage, ReceivedEvent, TransferEventsQueue

### Community 74 - "ProcessedEventRepository"
Cohesion: 0.15
Nodes (5): 2. Módulos e camadas, JdbcProcessedEventRepository, ProcessedEventRepository, MarkNotificationsReadUseCase, UseCaseConfiguration

### Community 75 - "smoke.sh"
Cohesion: 0.62
Nodes (6): aws_local(), call(), check_events(), check_notification(), smoke.sh script, uuid()

### Community 77 - "Etapa 0 — Fundação: design"
Cohesion: 0.20
Nodes (10): 1. Estrutura do monorepo, 2. Build e convenções, 4. Ambiente local, 5. CI (GitHub Actions), Critério de pronto, Etapa 0 — Fundação: design, Fora de escopo (e onde foi registrado), Objetivo (+2 more)

### Community 78 - "HttpAccountsGateway.kt"
Cohesion: 0.14
Nodes (8): 4. Fluxos, AccountsResponseClassifier, HttpAccountsGateway, RawResponse, Refused, StepOutcome, Success, TransientFailure

### Community 79 - "AccountsApplicationIntegrationTest.kt"
Cohesion: 0.11
Nodes (7): Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`), AccountsApplicationIntegrationTest, ParameterizedTypeReference, RequestThreadProbe, RequestThreadProbeConfiguration, TransfersApplicationIntegrationTest, ParameterizedTypeReference

### Community 80 - "01-accounts-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 01-accounts-topic.sh script

### Community 81 - "TransferEvent"
Cohesion: 0.29
Nodes (5): EventOutbox, TransferCompensated, TransferCompleted, TransferEvent, TransferFailed

### Community 82 - "OutboxRelayScheduler"
Cohesion: 0.13
Nodes (7): 0015 — Relay do outbox com líder único e SNS FIFO, Alternativas consideradas, Consequências, Contexto, Decisão, OutboxRelayScheduler, OutboxRelaySchedulerTest

### Community 83 - "OutboxBacklog"
Cohesion: 0.10
Nodes (4): OutboxBacklog, OutboxStore, OutboxMetricsTest, BlockingStore

### Community 85 - "Foco de revisão"
Cohesion: 0.22
Nodes (9): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 4: Ambiente local (docker-compose), Task 5: CI no GitHub Actions (+1 more)

### Community 88 - "RunTransferSagaUseCase"
Cohesion: 0.13
Nodes (3): SagaSettings, SagaProperties, UseCaseConfiguration

### Community 89 - "OrderResource"
Cohesion: 0.27
Nodes (5): PersistenceUsesWeb, OrderResource, WebUsesPersistence, ApplicationUsesAdapters, DomainUsesAdapters

### Community 92 - "LocalStackTestSupport"
Cohesion: 0.12
Nodes (3): Task 14: Verificação do SNS FIFO no LocalStack (Rafael, `infra`), LocalStackTestSupport, LocalStackTestSupportTest

### Community 96 - "InMemoryTransferRepository"
Cohesion: 0.15
Nodes (3): InMemoryTransferRepository, Lease, GetTransferUseCaseTest

### Community 97 - "retryWhileAccountBusy"
Cohesion: 0.21
Nodes (3): isAccountBusy(), retryWhileAccountBusy(), RetryWhileAccountBusyTest

### Community 98 - "TransferEventsIntegrationTest.kt"
Cohesion: 0.13
Nodes (3): ReceivedEvent, SentTransfer, TransferEventsIntegrationTest

### Community 99 - "Etapa 3 — Mensageria e `notification`: design"
Cohesion: 0.07
Nodes (26): Banco digital, Clientes e contas, Consulta e avisos, 10. Ambiente local e CI, 11. Documentação que acompanha, 1. Frentes de trabalho, 3. Modelo de domínio, 4. Fluxo de consumo (+18 more)

### Community 101 - "NotificationApiIntegrationTest"
Cohesion: 0.08
Nodes (7): Task A5: Consumo das filas (Marina, `dev-kotlin`) — paralela à A6, EventEnvelopeReader, MalformedEventException, ApiClient, ApiResponse, assertProblem(), NotificationApiIntegrationTest

### Community 102 - "RunTransferSagaUseCase"
Cohesion: 0.24
Nodes (3): CallResult, Run, RunTransferSagaUseCase

### Community 103 - "OutboxRelayConfigurationSupport.kt"
Cohesion: 0.12
Nodes (5): AwsProperties, OutboxRelayConfigurationSupport, OutboxRelayProperties, OutboxRelayConfiguration, OutboxRelayConfiguration

### Community 105 - "Etapa 4 — `statement` (extrato com CQRS no DynamoDB): design"
Cohesion: 0.12
Nodes (16): 10. Documentação que acompanha, 1. Frentes de trabalho, 3. Modelo de domínio, 4. Tabela no DynamoDB, 5. Fluxo de consumo, 6. API, nginx e Bruno, 7. Operação, 8. Testes (+8 more)

### Community 106 - "transfers/adapters/web/ProblemDetailsExceptionHandler.kt"
Cohesion: 0.05
Nodes (21): Task A6: API das notificações (Marina, `dev-kotlin`) — paralela à A5, RequestTransferDTO, TransferDTO, IdempotencyKeyHeader, ProblemDetailsExceptionHandler, TransferController, AccountsUnavailableException, InvalidField (+13 more)

### Community 111 - "ApiClient"
Cohesion: 0.16
Nodes (3): ClockWiringIntegrationTest, ApiClient, ApiResponse

### Community 128 - "RequestTransferUseCaseTest"
Cohesion: 0.20
Nodes (4): RequestTransferUseCase, RequestTransferUseCaseTest, TransferRepository, TransferRepository

### Community 129 - "AccountsProxy.kt"
Cohesion: 0.17
Nodes (3): AccountsProxy, HeldRequest, Hold

### Community 132 - "AccountsCall"
Cohesion: 0.12
Nodes (12): Task 12: Cliente HTTP do `accounts` (Marina, `dev-kotlin`), AccountsEntryRequestDTO, AccountLookup, EXISTS, NOT_FOUND, UNAVAILABLE, AccountsGateway, AccountsGateway (+4 more)

### Community 134 - "ConsumeEventUseCase"
Cohesion: 0.25
Nodes (4): Task A3: Casos de uso e portas (Marina, `dev-kotlin`), UnknownAccountException, ConsumeEventUseCase, Draft

### Community 135 - "NotificationIntegrationTest"
Cohesion: 0.07
Nodes (7): JdbcKnownAccountRepository, KnownAccountRepository, InMemoryKnownAccountRepository, NotificationIntegrationTest, JdbcKnownAccountRepositoryIntegrationTest, NotificationWebIntegrationTest, OpenApiIntegrationTest

### Community 136 - "OutboxRelayConfigurationSupportTest.kt"
Cohesion: 0.14
Nodes (3): DemoRelayConfiguration, InfrastructureConfiguration, OutboxRelayConfigurationSupportTest

### Community 137 - "ServiceApiClients.kt"
Cohesion: 0.19
Nodes (5): AccountsApiClient, HttpResult, NotificationApiClient, send(), TransfersApiClient

### Community 140 - "Notification"
Cohesion: 0.10
Nodes (8): JdbcNotificationRepository, NotificationDTO, NotificationPageDTO, NotificationCursor, NotificationRepository, NotificationPage, InMemoryNotificationRepository, Notification

### Community 141 - "ResumeDueTransfersUseCase"
Cohesion: 0.10
Nodes (14): Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2), Task 10: Pedir, consultar e retomar transferências (Marina, `dev-kotlin`), Task 11: Persistência do `transfers` (Marina, `dev-kotlin`), Task 13: API HTTP do `transfers` (Marina, `dev-kotlin`), Task 14: Agendador, relay e eventos no `transfers` (Marina, `dev-kotlin`), Task 15: Infra local, Bruno, smoke e CI do `transfers` (Rafael, `infra`), Task 3: Regras ArchUnit compartilhadas (Marina, `dev-kotlin`) — cartão #32, Task 7: Esqueleto do `transfers` (Marina, `dev-kotlin`) (+6 more)

### Community 142 - "TransferRepository"
Cohesion: 0.14
Nodes (3): 2. Módulos e camadas, TransferRepository, GetTransferUseCase

### Community 153 - "TransferStatus"
Cohesion: 0.18
Nodes (10): Transferências, Etapa 2 — `transfers` + saga Implementation Plan, Global Constraints, Review Focus, 10. Documentação que acompanha, TransferStatus, COMPENSATED, COMPLETED (+2 more)

### Community 155 - "TransferMetricsConfiguration.kt"
Cohesion: 0.26
Nodes (4): JdbcPendingTransferStats, PendingTransfers, TransferMetrics, TransferMetricsConfiguration

### Community 161 - "IdGenerator"
Cohesion: 0.15
Nodes (3): UuidV7IdGenerator, UuidV7IdGenerator, UuidV7IdGenerator

### Community 163 - "RunTransferSagaUseCaseTest.kt"
Cohesion: 0.10
Nodes (7): Task 9: Portas e execução da saga (Marina, `dev-kotlin`), IdGenerator, RetryBackoff, FakeAccountsGateway, FakeEventOutbox, InMemoryTransferStepLog, MutableClock

### Community 173 - "Etapa 2 — `transfers` + saga: design"
Cohesion: 0.18
Nodes (11): 5. Esquema do banco (`transfers`), 6. Eventos, 7. API, nginx e Bruno, 8. Testes, 9. Ambiente local e CI, Critério de pronto, Decisões desta etapa (usuário, 2026-10-27), Etapa 2 — `transfers` + saga: design (+3 more)

### Community 174 - "uuid"
Cohesion: 0.08
Nodes (4): InMemoryProcessedEventRepository, MutableClock, NotificationTest, TransferFixtures

### Community 175 - "TransferSagaSchedulerTest.kt"
Cohesion: 0.17
Nodes (4): JdbcTransferStepLog, TransactionRunner, StepRecord, TransferStepLog

### Community 177 - "JdbcOutboxWriter"
Cohesion: 0.11
Nodes (14): 3. Esquema do banco, 5. Outbox e relay (`libs/outbox`), 6. API, 7. Testes, 8. Ambiente local e CI, 9. Entrega em dois blocos, Critério de pronto, Etapa 1 — `accounts`: design (+6 more)

### Community 178 - "TransferStep"
Cohesion: 0.09
Nodes (14): Task 8: Domínio da transferência e passos da saga (Marina, `dev-kotlin`), 3. Modelo de domínio, Riscos, Transfer, Compensate, Compensated, Completed, Credit (+6 more)

### Community 181 - ".resumeDueOver"
Cohesion: 0.14
Nodes (4): CorrelationContext, EventOutbox, TransactionRunner, TransferStepLog

### Community 183 - "AccountRequestDTOs.kt"
Cohesion: 0.33
Nodes (4): ChangeAccountTypeDTO, ChangeOverdraftLimitDTO, OpenAccountDTO, RecordEntryDTO

### Community 184 - "02-transfers-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 02-transfers-topic.sh script

### Community 187 - "transfer"
Cohesion: 0.47
Nodes (4): transfer, transfer_due_idx, transfer_step, transfer_step_transfer_idx

### Community 188 - "HttpAccountsGateway.kt"
Cohesion: 0.27
Nodes (3): AccountsAccountResponseDTO, AccountsEntryResponseDTO, AccountsProblemDTO

### Community 190 - "JdbcOutboxWriterIntegrationTest.kt"
Cohesion: 0.07
Nodes (6): JdbcOutboxWriterIntegrationTest, OutboxMessage, OutboxEventAppender, OutboxEventAppender, FixedClockConfiguration, OutboxWriterClockIntegrationTest

### Community 191 - "03-notification-queues.sh"
Cohesion: 0.39
Nodes (7): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, create_consumer_queue(), queue_arn(), 03-notification-queues.sh script

### Community 192 - "V3__outbox.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 199 - "OrderController.kt"
Cohesion: 0.25
Nodes (4): OrderController, PlaceOrderCommand, PlaceOrderResult, PlaceOrderUseCase

### Community 200 - "NotificationCursor"
Cohesion: 0.16
Nodes (6): MarkedNotificationsDTO, MarkNotificationsReadDTO, NotificationController, InvalidField, InvalidRequestException, ListNotificationsUseCase

### Community 201 - ".accounts"
Cohesion: 0.25
Nodes (4): Etapa 1 — `accounts` Implementation Plan, Global Constraints, Review Focus, AccountRepository

### Community 202 - "0014 — Tipo de conta e cheque especial"
Cohesion: 0.40
Nodes (5): 0014 — Tipo de conta e cheque especial, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 203 - "ConsumeEventUseCase.kt"
Cohesion: 0.19
Nodes (3): NotificationContent, NotificationText, NotificationTextTest

### Community 205 - "0007 — Conta e ledger no mesmo serviço (`accounts`)"
Cohesion: 0.50
Nodes (4): 0007 — Conta e ledger no mesmo serviço (`accounts`), Consequências, Contexto, Decisão

### Community 209 - "Regras inegociáveis"
Cohesion: 0.25
Nodes (8): Agentes do projeto, Ao terminar uma tarefa, Banco digital em microsserviços, Contexto, Fluxo de trabalho (skills), Git, Grafo do código (graphify), Regras inegociáveis

### Community 213 - "OutboxSchemaAssertions.kt"
Cohesion: 0.46
Nodes (3): createModelSchema(), currentSchema(), OutboxSchemaSnapshot

### Community 214 - "Frente A — `notification`"
Cohesion: 0.25
Nodes (8): Frente A — `notification`, Task A10: Documentação (Clara, `documentador`) — paralela desde o início, Task A1: Esqueleto do `notification` e prova do `@SqsListener` (Marina, `dev-kotlin`), Task A2: Domínio das notificações (Marina, `dev-kotlin`), Task A4: Persistência (Marina, `dev-kotlin`), Task A7: `AccountTypeChanged` versão 2 no `accounts` (Marina, `dev-kotlin`) — paralela desde o início, Task A8: Infra local, Bruno, smoke e CI do `notification` (Rafael, `infra`), Task A9: Testes ponta a ponta (Marina, `dev-kotlin`) — depois de A5, A6, A7, A8 e B5

### Community 215 - "V1__notification_schema.sql"
Cohesion: 0.50
Nodes (4): known_accounts, notifications, notifications_customer_page_idx, processed_events

### Community 220 - "0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS"
Cohesion: 0.40
Nodes (5): 0017 — `notification` com banco próprio e consumo SQS com Spring Cloud AWS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 224 - "AccountsApplication.kt"
Cohesion: 0.33
Nodes (4): Task 3: Aplicação `accounts` com health do banco (Testcontainers), 3. Esqueleto do `accounts`, Testes (no `bootstrap`, que enxerga todos os módulos), AccountsApplication

### Community 225 - "2026-10-21-etapa-0-fundacao-design.md"
Cohesion: 0.10
Nodes (15): 0001 — Stack: Kotlin + Spring Boot 4 na AWS, Alternativas consideradas, Consequências, Contexto, Decisão, 0002 — Clean Architecture por serviço, Alternativas consideradas, Consequências (+7 more)

### Community 226 - "Frente B — débitos técnicos"
Cohesion: 0.29
Nodes (6): Frente B — débitos técnicos, Task B1: Configuração do relay em `libs/outbox-relay` (Marina, `dev-kotlin`) — cartão #92, Task B2: Backoff do relay e testes (Marina, `dev-kotlin`) — cartões #24 e #26, depois da B1, Task B3: Domínio e persistência do `transfers` (Marina, `dev-kotlin`) — cartões #95 e #93, Task B4: Aplicação e adapters do `transfers` (Marina, `dev-kotlin`) — cartão #94, depois da B3, Task B5: Endurecer os testes ponta a ponta (Marina, `dev-kotlin`) — cartão #96, antes da A9

### Community 227 - "04-statement.sh"
Cohesion: 0.43
Nodes (6): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, queue_arn(), 04-statement.sh script

### Community 231 - "Transfer.kt"
Cohesion: 0.11
Nodes (4): CompensateStepTest, ResolveCreditStepTest, ResolveDebitStepTest, TerminalStepsTest

### Community 235 - "Status do projeto"
Cohesion: 0.33
Nodes (6): Ambiente do Claude, Como rodar localmente, Como usar o board, Concluído, Convenções que já custaram caro, Status do projeto

## Knowledge Gaps
- **48 isolated node(s):** `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` (+43 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 652 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **138 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Transfer` connect `TransferStep` to `RequestTransferUseCaseTest`, `ResumeDueTransfersUseCaseTest`, `.transfer`, `ListNotificationsUseCaseTest`, `ResumeDueTransfersUseCase`, `TransferRepository`, `TransferMetricsIntegrationTest.kt`, `SpringTransactionRunnerIntegrationTest.kt`, `JdbcTransferRepository`, `TransferStatus`, `InMemoryTransferRepositoryTest`, `RunTransferSagaUseCaseTest.kt`, `.run`, `Etapa 2 — `transfers` + saga: design`, `uuid`, `TransferSagaSchedulerTest.kt`, `JdbcTransferRepositoryIntegrationTest`, `BlockingRepository`, `RunTransferSagaUseCaseTest`, `offsetdatetime`, `InMemoryTransferRepository`, `RunTransferSagaUseCase`, `Transfer.kt`, `transfers/adapters/web/ProblemDetailsExceptionHandler.kt`?**
  _High betweenness centrality (0.058) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Transfer` (e.g. with `Transferências` and `Global Constraints`) actually correct?**
  _`Transfer` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION` to the rest of the system?**
  _48 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `accounts/bootstrap/UseCaseConfiguration.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.06906906906906907 - nodes in this community are weakly interconnected._
- **Why does `Etapa 2 — `transfers` + saga: design` connect `Etapa 2 — `transfers` + saga: design` to `OutboxRelay`, `TransferRepository`, `HttpAccountsGateway.kt`, `TransferStep`, `README.md`, `TransferStatus`?**
  _High betweenness centrality (0.028) - this node is a cross-community bridge._
- **Are the 8 inferred relationships involving `Account` (e.g. with `Regras inegociáveis` and `Clientes e contas`) actually correct?**
  _`Account` has 8 INFERRED edges - model-reasoned connections that need verification._
- **Should `SqsListenerFifoIntegrationTest.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.060655737704918035 - nodes in this community are weakly interconnected._