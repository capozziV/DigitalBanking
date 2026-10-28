# Graph Report - DigitalBanking  (2026-10-28)

## Corpus Check
- 334 files · ~92,242 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 23 file(s) not represented in the graph (top: .bru 13, (none) 3, .properties 2)

## Summary
- 2946 nodes · 7111 edges · 189 communities (84 shown, 105 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 592 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `281de235`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AccountController
- JdbcOutboxWriterIntegrationTest.kt
- AccountApiIntegrationTest
- SpringTransactionRunner
- Transfer
- SnsEventPublisherTest.kt
- ServiceArchitectureRulesTest
- OutboxRelay
- Account
- Money
- Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)
- ProblemDetailsExceptionHandler.kt
- OutboxRelayIntegrationTest.kt
- SnsEventPublisherIntegrationTest.kt
- LedgerEntry
- Account
- OutboxRelayTest
- duration
- Decisões tomadas
- README.md
- LocalStackTestSupport
- LocalStackFifoIntegrationTest.kt
- TransferStep
- coordenador.md
- 0004 — API Gateway HTTP API na borda
- 0005 — Frontend Angular e BFF em NestJS
- 0006 — Observabilidade só com correlation ID
- TransferSagaEndToEndIntegrationTest
- LedgerEntryRecorded
- Rodando o projeto localmente (macOS)
- AccountController.kt
- 0008 — Extrato (`statement`) com CQRS no DynamoDB
- 0009 — Cache do extrato
- 0010 — Limite diário com Redis + Lua
- 0013 — Persistência com Spring JDBC (`JdbcClient`)
- 0014 — Tipo de conta e cheque especial
- OpenApiIntegrationTest
- TransfersIntegrationTest
- dev-frontend.md
- dev-kotlin.md
- infra.md
- 0003 — Transactional Outbox e mensageria SNS/SQS
- 0007 — Conta e ledger no mesmo serviço (`accounts`)
- 0011 — Contratos de API
- Versões novas da etapa 1
- documentador.md
- revisor.md
- 01-create-service-databases.sh
- LocalStackInitScripts
- outbox_stuck_idx
- OutboxRelayScheduler
- OutboxRelayConfiguration.kt
- account
- PendingEvent
- outbox-table.sql
- V4__outbox.sql
- LedgerEntryRecorded
- accounts/bootstrap/UseCaseConfiguration.kt
- JdbcTransferRepositoryIntegrationTest
- SagaTimeoutsValidatorTest.kt
- TransferStatus
- Etapa 3 — Mensageria e `notification`: design
- smoke.sh
- 0016 — Saga de transferência orquestrada por HTTP síncrono
- Etapa 0 — Fundação: design
- 0012 — Saldo no lançamento e trava pessimista por conta
- AccountsApplicationIntegrationTest.kt
- 01-accounts-topic.sh
- TransferApiIntegrationTest
- .handle
- duration
- HttpAccountsGatewayIntegrationTest
- Foco de revisão
- JdbcOutboxStoreIntegrationTest
- RunTransferSagaUseCase
- OrderResource
- RunTransferSagaUseCaseTest
- AccountsCall
- status.md
- .record
- 0002 — Clean Architecture por serviço
- transfers/adapters/web/ProblemDetailsExceptionHandler.kt
- EntryLookupApiIntegrationTest
- .outbox
- TransferEventsIntegrationTest.kt
- TransferEvent
- AccountNotFoundException
- OutboxRelayUnreachableBrokerIntegrationTest.kt
- Regras inegociáveis
- transfers/bootstrap/OutboxRelayConfiguration.kt
- ApiClient
- .request
- LegacyResource.kt
- PlaceOrderHandler
- LegacyController.kt
- .inTransaction
- RunTransferSagaUseCase
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
- assertthat
- AccountsProxy.kt
- Money
- InMemoryTransferRepository
- ResumeDueTransfersUseCaseTest
- ProcessLogsOnFailureExtension
- OutboxRelayScheduler
- .container
- ServiceApiClients.kt
- ConcurrencyIntegrationTest
- UseCaseConfiguration
- AccountRequestDTOs.kt
- RequestTransferUseCaseTest
- JdbcTransferRepositoryIntegrationTest.kt
- AccountsResponseClassifierTest
- accounts/adapters/web/OpenApiConfiguration.kt
- EndToEndEnvironment
- TransferLeaseContentionIntegrationTest.kt
- TransferMetricsIntegrationTest.kt
- MdcCorrelationContextTest.kt
- ResumeDueTransfersUseCase
- JdbcTransferRepository
- CallResult
- TransferSagaScheduler
- TransferMetricsConfiguration.kt
- transfers/adapters/persistence/OutboxEventAppender.kt
- HttpAccountsGateway.kt
- AccountTypeAdminApiIntegrationTest
- ApiClient
- transfers/adapters/web/OpenApiConfiguration.kt
- ServiceProcess
- RequestTransferDTO
- accounts/adapters/persistence/UuidV7IdGenerator.kt
- SchedulerLeaseOwners
- AccountsApplication.kt
- BlockingRepository
- EndToEndInputs
- AccountsIntegrationTest
- accounts/bootstrap/web/UnexpectedErrorApiIntegrationTest.kt
- InMemoryTransferRepositoryTest
- .entry
- Test
- TransferEventsQueue
- SagaProperties
- JdbcOutboxWriter
- LocalStackInitScripts
- .resumeDueOver
- Bloco 1 — preparação para o 2º serviço (branch `etapa-2-transfers`, PR 1)
- 02-transfers-topic.sh
- .record
- transfer
- 0015 — Relay do outbox com líder único e SNS FIFO
- TransfersApplication.kt
- V3__outbox.sql
- Money

## God Nodes (most connected - your core abstractions)
1. `Transfer` - 103 edges
2. `Account` - 70 edges
3. `AccountApiIntegrationTest` - 61 edges
4. `LedgerEntry` - 58 edges
5. `OverdraftLimit` - 54 edges
6. `TransferStep` - 52 edges
7. `Balance` - 49 edges
8. `Money` - 47 edges
9. `AccountType` - 47 edges
10. `assertProblem()` - 47 edges

## Surprising Connections (you probably didn't know these)
- `1. Frentes de trabalho` --references--> `AccountTypeChanged`  [INFERRED]
  docs/superpowers/specs/2026-10-28-etapa-3-mensageria-design.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/AccountEvent.kt
- `Riscos` --references--> `AccountOpened`  [INFERRED]
  docs/superpowers/specs/2026-10-28-etapa-3-mensageria-design.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/AccountEvent.kt
- `Task 4: Consulta de lançamento pela chave no `accounts` (Marina, `dev-kotlin`)` --references--> `AccountNotFoundException`  [INFERRED]
  docs/superpowers/plans/2026-10-27-etapa-2-transfers.md → services/accounts/application/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/application/ApplicationExceptions.kt
- `10. Documentação que acompanha` --references--> `EntryKind`  [INFERRED]
  docs/superpowers/specs/2026-10-22-etapa-1-accounts-design.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/EntryKind.kt
- `Decisão` --references--> `LedgerEntry`  [INFERRED]
  docs/adr/0012-saldo-no-lancamento-e-trava-pessimista.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/LedgerEntry.kt

## Import Cycles
- None detected.

## Communities (189 total, 105 thin omitted)

### Community 0 - "AccountController"
Cohesion: 0.24
Nodes (3): AccountController, AccountDTO, LedgerEntryDTO

### Community 4 - "Transfer"
Cohesion: 0.13
Nodes (4): TransferRepository, Transfer, Credit, Debit

### Community 6 - "ServiceArchitectureRulesTest"
Cohesion: 0.07
Nodes (4): ServiceArchitectureRules, ServiceArchitectureRulesTest, ArchitectureTest, ArchitectureTest

### Community 7 - "OutboxRelay"
Cohesion: 0.05
Nodes (9): CompensateStepTest, CreditStepTest, DebitStepTest, ResolveCreditStepTest, ResolveDebitStepTest, TerminalStepsTest, TransferFixtures, TransferSagaPathsTest (+1 more)

### Community 8 - "Account"
Cohesion: 0.06
Nodes (18): Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`), Task 8: Esquema, usuários de banco e repositórios JDBC (Marina, `dev-kotlin`; o script de init do compose segue o padrão do Rafael), JdbcAccountRepository, AccountBusyException, AccountNotFoundException, CustomerAlreadyHasAccountException, AccountRepository, InsertAccountResult (+10 more)

### Community 9 - "Money"
Cohesion: 0.13
Nodes (6): 2. Modelo de domínio, AvailableBalance, Balance, Money, AccountTest, BalanceTest

### Community 10 - "Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)"
Cohesion: 0.29
Nodes (7): Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2), Task 10: Pedir, consultar e retomar transferências (Marina, `dev-kotlin`), Task 11: Persistência do `transfers` (Marina, `dev-kotlin`), Task 13: API HTTP do `transfers` (Marina, `dev-kotlin`), Task 14: Agendador, relay e eventos no `transfers` (Marina, `dev-kotlin`), Task 15: Infra local, Bruno, smoke e CI do `transfers` (Rafael, `infra`), Task 7: Esqueleto do `transfers` (Marina, `dev-kotlin`)

### Community 11 - "ProblemDetailsExceptionHandler.kt"
Cohesion: 0.09
Nodes (8): IdempotencyKeyHeader, ProblemDetailsExceptionHandler, AccountTypeNotFoundException, EntryNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException, BalanceOverflowException

### Community 12 - "OutboxRelayIntegrationTest.kt"
Cohesion: 0.10
Nodes (3): OutboxRelayIntegrationTest, ReceivedEvent, RecordingPublisherConfiguration

### Community 14 - "LedgerEntry"
Cohesion: 0.07
Nodes (18): Banco digital, Clientes e contas, Consulta e avisos, Ledger, JdbcLedgerEntryRepository, IdempotencyKeyReusedException, LedgerEntryRepository, IdempotentEntryLookup (+10 more)

### Community 16 - "OutboxRelayTest"
Cohesion: 0.09
Nodes (6): FakeEventPublisher, FakeOutboxStore, MutableClock, OutboxRelayTest, Lease, TransfersDatabase

### Community 17 - "duration"
Cohesion: 0.09
Nodes (15): JdbcAccountTypeRepository, AccountTypeRepository, InMemoryAccountTypeRepository, ChangeOverdraftLimitUseCaseTest, GetAccountUseCaseTest, ListAccountTypesUseCaseTest, AccountTypeRepository, AccountType (+7 more)

### Community 18 - "Decisões tomadas"
Cohesion: 0.13
Nodes (15): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Em aberto, Fluxo de entrada (+7 more)

### Community 22 - "TransferStep"
Cohesion: 0.11
Nodes (11): Task 8: Domínio da transferência e passos da saga (Marina, `dev-kotlin`), 3. Modelo de domínio, Riscos, Compensate, Compensated, Completed, Failed, ResolveCredit (+3 more)

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

### Community 27 - "TransferSagaEndToEndIntegrationTest"
Cohesion: 0.07
Nodes (8): OrderController, PlaceOrderCommand, PlaceOrderResult, PlaceOrderUseCase, Order, CorrelationIdFilter, CorrelationIdFilter, TransferSagaEndToEndIntegrationTest

### Community 28 - "LedgerEntryRecorded"
Cohesion: 0.18
Nodes (4): ChangeAccountTypeUseCase, CallLog, AccountTypeRepository, ChangeAccountTypeUseCaseTest

### Community 29 - "Rodando o projeto localmente (macOS)"
Cohesion: 0.15
Nodes (13): 1. Configurar o ambiente, 2. Subir a infraestrutura (PostgreSQL, Redis, nginx, LocalStack), 3. Build e testes, 4. Subir os serviços `accounts` e `transfers`, 5. Conferir a API pelo nginx, 6. Ver os eventos na fila de inspeção, 7. Coleção Bruno, 8. Derrubar tudo (+5 more)

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

### Community 35 - "0014 — Tipo de conta e cheque especial"
Cohesion: 0.40
Nodes (5): 0014 — Tipo de conta e cheque especial, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 37 - "TransfersIntegrationTest"
Cohesion: 0.05
Nodes (12): OutboxSchemaAssertionsIntegrationTest, assertOutboxSchemaMatchesModel(), createModelSchema(), currentSchema(), OutboxSchemaSnapshot, OutboxSchemaIntegrationTest, OutboxSchemaIntegrationTest, awaitStatus() (+4 more)

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

### Community 42 - "0007 — Conta e ledger no mesmo serviço (`accounts`)"
Cohesion: 0.50
Nodes (4): 0007 — Conta e ledger no mesmo serviço (`accounts`), Consequências, Contexto, Decisão

### Community 43 - "0011 — Contratos de API"
Cohesion: 0.50
Nodes (4): 0011 — Contratos de API, Consequências, Contexto, Decisão

### Community 44 - "Versões novas da etapa 1"
Cohesion: 0.50
Nodes (3): Observações, springdoc e Spring Boot 4.1.x, Versões novas da etapa 1

### Community 61 - "OutboxRelayScheduler"
Cohesion: 0.12
Nodes (3): OutboxRelayScheduler, BlockingStore, OutboxRelaySchedulerTest

### Community 62 - "OutboxRelayConfiguration.kt"
Cohesion: 0.11
Nodes (7): Task 2: Relay em módulo próprio e teste de esquema (Marina, `dev-kotlin`) — cartões #25 e #30, OutboxMetrics, OutboxRelaySettings, OutboxRelaySettingsTest, AwsProperties, OutboxRelayConfiguration, OutboxRelayProperties

### Community 63 - "account"
Cohesion: 0.33
Nodes (3): account_type, account, ledger_entry

### Community 64 - "PendingEvent"
Cohesion: 0.09
Nodes (12): Bloco 2 — relay e SNS FIFO, Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), 1. Entrega em dois blocos, EventPublisher, JdbcOutboxStore, OutboxEventEnvelope, PendingEvent (+4 more)

### Community 65 - "outbox-table.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 69 - "LedgerEntryRecorded"
Cohesion: 0.11
Nodes (8): RecordCreditUseCase, RecordEntryCommand, RecordEntryResult, DirectTransactionRunner, SequentialIdGenerator, RecordCreditUseCaseTest, RecordDebitUseCaseTest, LedgerEntryRecorded

### Community 70 - "accounts/bootstrap/UseCaseConfiguration.kt"
Cohesion: 0.05
Nodes (22): Regras inegociáveis, 10. Documentação que acompanha, 1. Módulos e camadas, 3. Esquema do banco, 4. Fluxos, 6. API, 7. Testes, 8. Ambiente local e CI (+14 more)

### Community 71 - "JdbcTransferRepositoryIntegrationTest"
Cohesion: 0.05
Nodes (12): Task 9: Portas e execução da saga (Marina, `dev-kotlin`), JdbcTransferStepLog, SpringTransactionRunner, TransactionRunner, StepRecord, TransferStepLog, InMemoryTransferStepLog, RetryBackoffTest (+4 more)

### Community 72 - "SagaTimeoutsValidatorTest.kt"
Cohesion: 0.07
Nodes (7): AccountsClientConfiguration, AccountsClientProperties, SagaTimeoutsValidator, PropertiesConfiguration, SagaPropertiesTest, SagaTimeoutsValidatorTest, ValidatorConfiguration

### Community 73 - "TransferStatus"
Cohesion: 0.09
Nodes (21): Transferências, Etapa 2 — `transfers` + saga Implementation Plan, Global Constraints, Review Focus, 10. Documentação que acompanha, 5. Esquema do banco (`transfers`), 6. Eventos, 7. API, nginx e Bruno (+13 more)

### Community 74 - "Etapa 3 — Mensageria e `notification`: design"
Cohesion: 0.12
Nodes (16): 10. Ambiente local e CI, 11. Documentação que acompanha, 1. Frentes de trabalho, 2. Módulos e camadas, 4. Fluxo de consumo, 5. Esquema do banco (`notification`), 6. Eventos consumidos e mudança no `accounts`, 7. API, nginx e Bruno (+8 more)

### Community 75 - "smoke.sh"
Cohesion: 0.67
Nodes (5): aws_local(), call(), check_events(), smoke.sh script, uuid()

### Community 76 - "0016 — Saga de transferência orquestrada por HTTP síncrono"
Cohesion: 0.40
Nodes (5): 0016 — Saga de transferência orquestrada por HTTP síncrono, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 77 - "Etapa 0 — Fundação: design"
Cohesion: 0.22
Nodes (9): 2. Build e convenções, 4. Ambiente local, 5. CI (GitHub Actions), Critério de pronto, Etapa 0 — Fundação: design, Fora de escopo (e onde foi registrado), Objetivo, Restrições (de `CLAUDE.md` e ADRs) (+1 more)

### Community 78 - "0012 — Saldo no lançamento e trava pessimista por conta"
Cohesion: 0.40
Nodes (5): 0012 — Saldo no lançamento e trava pessimista por conta, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 79 - "AccountsApplicationIntegrationTest.kt"
Cohesion: 0.11
Nodes (7): Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`), AccountsApplicationIntegrationTest, ParameterizedTypeReference, RequestThreadProbe, RequestThreadProbeConfiguration, TransfersApplicationIntegrationTest, ParameterizedTypeReference

### Community 80 - "01-accounts-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 01-accounts-topic.sh script

### Community 82 - ".handle"
Cohesion: 0.22
Nodes (8): IdempotencyKeyHeader, AccountsUnavailableException, InvalidField, InvalidRequestException, SameAccountTransferException, TransferAccountNotFoundException, TransferFailedException, TransferNotFoundException

### Community 85 - "Foco de revisão"
Cohesion: 0.17
Nodes (11): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 3: Aplicação `accounts` com health do banco (Testcontainers), Task 4: Ambiente local (docker-compose) (+3 more)

### Community 87 - "JdbcOutboxStoreIntegrationTest"
Cohesion: 0.07
Nodes (11): JdbcOutboxStoreIntegrationTest, isAccountBusy(), retryWhileAccountBusy(), RetryWhileAccountBusyTest, RequestTransferCommand, AccountNotFound, AccountsUnavailable, Created (+3 more)

### Community 88 - "RunTransferSagaUseCase"
Cohesion: 0.11
Nodes (5): 2. Módulos e camadas, GetTransferUseCase, RequestTransferUseCase, SagaSettings, UseCaseConfiguration

### Community 89 - "OrderResource"
Cohesion: 0.27
Nodes (5): PersistenceUsesWeb, OrderResource, WebUsesPersistence, ApplicationUsesAdapters, DomainUsesAdapters

### Community 91 - "AccountsCall"
Cohesion: 0.08
Nodes (19): Task 12: Cliente HTTP do `accounts` (Marina, `dev-kotlin`), AccountsEntryRequestDTO, IdempotencyKeyReusedException, AccountLookup, EXISTS, NOT_FOUND, UNAVAILABLE, AccountsGateway (+11 more)

### Community 92 - "status.md"
Cohesion: 0.12
Nodes (11): 0001 — Stack: Kotlin + Spring Boot 4 na AWS, Alternativas consideradas, Consequências, Contexto, Decisão, Ambiente do Claude, Como rodar localmente, Como usar o board (+3 more)

### Community 94 - "0002 — Clean Architecture por serviço"
Cohesion: 0.40
Nodes (5): 0002 — Clean Architecture por serviço, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 97 - ".outbox"
Cohesion: 0.33
Nodes (4): Etapa 1 — `accounts` Implementation Plan, Global Constraints, Review Focus, EventOutbox

### Community 98 - "TransferEventsIntegrationTest.kt"
Cohesion: 0.12
Nodes (3): ReceivedEvent, SentTransfer, TransferEventsIntegrationTest

### Community 99 - "TransferEvent"
Cohesion: 0.21
Nodes (7): 3. Modelo de domínio, Decisões desta etapa (usuário, 2026-10-28), EventOutbox, TransferCompensated, TransferCompleted, TransferEvent, TransferFailed

### Community 100 - "AccountNotFoundException"
Cohesion: 0.08
Nodes (20): Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 13: Documentação do bloco 1 e das decisões da etapa (Clara, `documentador`; em paralelo às Tasks 8–12), Task 2: Valores monetários e tipos (Marina, `dev-kotlin`), Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`), Task 5: Casos de uso de crédito e débito (Marina, `dev-kotlin`) (+12 more)

### Community 102 - "Regras inegociáveis"
Cohesion: 0.20
Nodes (8): Agentes do projeto, Ao terminar uma tarefa, Banco digital em microsserviços, Contexto, Fluxo de trabalho (skills), Git, Grafo do código (graphify), Roadmap

### Community 103 - "transfers/bootstrap/OutboxRelayConfiguration.kt"
Cohesion: 0.12
Nodes (4): OutboxStore, AwsProperties, OutboxRelayConfiguration, OutboxRelayProperties

### Community 128 - "assertthat"
Cohesion: 0.08
Nodes (3): RetryBackoff, FakeEventOutbox, MutableClock

### Community 129 - "AccountsProxy.kt"
Cohesion: 0.12
Nodes (3): AccountsProxy, HeldRequest, Hold

### Community 132 - "InMemoryTransferRepository"
Cohesion: 0.14
Nodes (3): InMemoryTransferRepository, Lease, GetTransferUseCaseTest

### Community 135 - "OutboxRelayScheduler"
Cohesion: 0.12
Nodes (4): OutboxRelay, OutboxRelayScheduler, BlockingStore, OutboxRelaySchedulerTest

### Community 137 - "ServiceApiClients.kt"
Cohesion: 0.21
Nodes (4): AccountsApiClient, HttpResult, send(), TransfersApiClient

### Community 140 - "AccountRequestDTOs.kt"
Cohesion: 0.33
Nodes (4): ChangeAccountTypeDTO, ChangeOverdraftLimitDTO, OpenAccountDTO, RecordEntryDTO

### Community 149 - "ResumeDueTransfersUseCase"
Cohesion: 0.16
Nodes (7): Task 3: Regras ArchUnit compartilhadas (Marina, `dev-kotlin`) — cartão #32, CorrelationContext, Outcome, INTERRUPTED, SKIPPED, TAKEN, ResumeDueTransfersUseCase

### Community 151 - "CallResult"
Cohesion: 0.21
Nodes (4): 4. Fluxos, AccountsResponseClassifier, HttpAccountsGateway, RawResponse

### Community 153 - "TransferMetricsConfiguration.kt"
Cohesion: 0.26
Nodes (4): JdbcPendingTransferStats, PendingTransfers, TransferMetrics, TransferMetricsConfiguration

### Community 155 - "HttpAccountsGateway.kt"
Cohesion: 0.24
Nodes (3): AccountsAccountResponseDTO, AccountsEntryResponseDTO, AccountsProblemDTO

### Community 164 - "AccountsApplication.kt"
Cohesion: 0.40
Nodes (3): 3. Esqueleto do `accounts`, Testes (no `bootstrap`, que enxerga todos os módulos), AccountsApplication

### Community 178 - "JdbcOutboxWriter"
Cohesion: 0.11
Nodes (8): 5. Outbox e relay (`libs/outbox`), 9. Entrega em dois blocos, Restrições (de `CLAUDE.md` e ADRs), SnsEventPublisher, JdbcOutboxWriter, OutboxWriterConfiguration, OutboxWriterConfiguration, ClockWiringIntegrationTest

### Community 181 - ".resumeDueOver"
Cohesion: 0.11
Nodes (5): AccountsGateway, CorrelationContext, EventOutbox, TransactionRunner, TransferStepLog

### Community 183 - "Bloco 1 — preparação para o 2º serviço (branch `etapa-2-transfers`, PR 1)"
Cohesion: 0.33
Nodes (5): Bloco 1 — preparação para o 2º serviço (branch `etapa-2-transfers`, PR 1), Task 1: Gradle para vários serviços (Rafael, `infra`) — cartão #37, Task 4: Consulta de lançamento pela chave no `accounts` (Marina, `dev-kotlin`), Task 5: PostgreSQL com um banco por serviço (Rafael, `infra`) — cartão #38, Task 6: Build seletivo no CI (Rafael, `infra`)

### Community 184 - "02-transfers-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 02-transfers-topic.sh script

### Community 187 - "transfer"
Cohesion: 0.47
Nodes (4): transfer, transfer_due_idx, transfer_step, transfer_step_transfer_idx

### Community 188 - "0015 — Relay do outbox com líder único e SNS FIFO"
Cohesion: 0.40
Nodes (5): 0015 — Relay do outbox com líder único e SNS FIFO, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 192 - "V3__outbox.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 193 - "Money"
Cohesion: 0.21
Nodes (3): FakeAccountsGatewayTest, Money, MoneyTest

## Knowledge Gaps
- **29 isolated node(s):** `Success`, `TransientFailure`, `SameAccount`, `CREDIT`, `DEBIT` (+24 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 518 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **105 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Transfer` connect `Transfer` to `assertthat`, `InMemoryTransferRepository`, `ResumeDueTransfersUseCaseTest`, `OutboxRelay`, `Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)`, `RequestTransferUseCaseTest`, `JdbcTransferRepositoryIntegrationTest.kt`, `TransferMetricsIntegrationTest.kt`, `JdbcTransferRepository`, `TransferStep`, `AccountController.kt`, `BlockingRepository`, `JdbcTransferRepositoryIntegrationTest`, `TransferStatus`, `.handle`, `JdbcOutboxStoreIntegrationTest`, `RunTransferSagaUseCase`, `RunTransferSagaUseCaseTest`, `AccountsCall`, `.request`, `RunTransferSagaUseCase`?**
  _High betweenness centrality (0.068) - this node is a cross-community bridge._
- **Are the 5 inferred relationships involving `Transfer` (e.g. with `Transferências` and `Global Constraints`) actually correct?**
  _`Transfer` has 5 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Success`, `TransientFailure`, `SameAccount` to the rest of the system?**
  _29 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AccountApiIntegrationTest` be split into smaller, more focused modules?**
  _Cohesion score 0.13157894736842105 - nodes in this community are weakly interconnected._
- **Why does `AccountsIntegrationTest` connect `AccountsIntegrationTest` to `PendingEvent`, `EntryLookupApiIntegrationTest`, `AccountApiIntegrationTest`, `OpenApiIntegrationTest`, `OutboxRelayUnreachableBrokerIntegrationTest.kt`, `accounts/bootstrap/UseCaseConfiguration.kt`, `TransfersIntegrationTest`, `Account`, `accounts/bootstrap/web/UnexpectedErrorApiIntegrationTest.kt`, `Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)`, `ConcurrencyIntegrationTest`, `OutboxRelayIntegrationTest.kt`, `JdbcTransferRepositoryIntegrationTest.kt`, `AccountsApplicationIntegrationTest.kt`, `Account`, `AccountTypeAdminApiIntegrationTest`?**
  _High betweenness centrality (0.041) - this node is a cross-community bridge._
- **Are the 7 inferred relationships involving `Account` (e.g. with `Regras inegociáveis` and `Clientes e contas`) actually correct?**
  _`Account` has 7 INFERRED edges - model-reasoned connections that need verification._
- **Should `Transfer` be split into smaller, more focused modules?**
  _Cohesion score 0.12681159420289856 - nodes in this community are weakly interconnected._