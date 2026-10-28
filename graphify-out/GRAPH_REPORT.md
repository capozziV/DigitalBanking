# Graph Report - DigitalBanking  (2026-10-27)

## Corpus Check
- 227 files · ~63,493 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 19 file(s) not represented in the graph (top: .bru 9, (none) 3, .properties 2)

## Summary
- 1810 nodes · 4330 edges · 127 communities (72 shown, 55 thin omitted)
- Extraction: 90% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 431 edges (avg confidence: 0.9)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `f9fa39e6`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Bloco 1 — `accounts` gravando no outbox
- AccountsIntegrationTest
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
- JdbcOutboxWriter
- LedgerEntry
- Account
- OutboxRelayTest
- duration
- Decisões tomadas
- README.md
- AccountController.kt
- SnsEventPublisherIntegrationTest.kt
- TransferStep
- coordenador.md
- 0004 — API Gateway HTTP API na borda
- 0005 — Frontend Angular e BFF em NestJS
- 0006 — Observabilidade só com correlation ID
- CorrelationIdFilter.kt
- LedgerEntryRecorded
- Rodando o projeto localmente (macOS)
- AccountTypeAdminController.kt
- 0008 — Extrato (`statement`) com CQRS no DynamoDB
- 0009 — Cache do extrato
- 0010 — Limite diário com Redis + Lua
- 0013 — Persistência com Spring JDBC (`JdbcClient`)
- 0014 — Tipo de conta e cheque especial
- ApiClient
- AccountController.kt
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
- Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff)
- outbox-table.sql
- V4__outbox.sql
- LedgerEntryRecorded
- RecordDebitUseCase.kt
- UseCaseConfiguration.kt
- OutboxMetricsTest
- Etapa 2 — `transfers` + saga: design
- 0001 — Stack: Kotlin + Spring Boot 4 na AWS
- smoke.sh
- 0016 — Saga de transferência orquestrada por HTTP síncrono
- Etapa 0 — Fundação: design
- 0012 — Saldo no lançamento e trava pessimista por conta
- AccountsApplicationIntegrationTest.kt
- 01-accounts-topic.sh
- AvailableBalance
- JdbcOutboxStore
- OutboxStore
- OutboxRelay
- Foco de revisão
- JdbcOutboxStoreIntegrationTest
- TransferStatus
- OrderResource
- .run
- AccountsCall
- OpenApiConfiguration.kt
- .record
- 0002 — Clean Architecture por serviço
- OutboxRelaySettingsTest.kt
- AccountsApplication.kt
- .outbox
- FindEntryByIdempotencyKeyUseCaseTest
- TransferEvent
- AccountNotFoundException
- AccountRequestDTOs.kt
- Regras inegociáveis
- Status do projeto
- OutboxWriterConfiguration.kt
- StepOutcome
- LegacyResource.kt
- PlaceOrderHandler
- LegacyController.kt
- .inTransaction
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

## God Nodes (most connected - your core abstractions)
1. `Account` - 70 edges
2. `AccountApiIntegrationTest` - 61 edges
3. `LedgerEntry` - 58 edges
4. `OverdraftLimit` - 54 edges
5. `Balance` - 49 edges
6. `assertProblem()` - 47 edges
7. `AccountType` - 47 edges
8. `JdbcRepositoriesIntegrationTest` - 45 edges
9. `Money` - 44 edges
10. `Transfer` - 44 edges

## Surprising Connections (you probably didn't know these)
- `Mensageria` --references--> `OutboxRelay`  [INFERRED]
  docs/arquitetura.md → libs/outbox-relay/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/relay/OutboxRelay.kt
- `Concluído` --references--> `OutboxRelay`  [INFERRED]
  docs/status.md → libs/outbox-relay/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/relay/OutboxRelay.kt
- `Task 9: API HTTP, Problem Details e correlation ID (Marina, `dev-kotlin`)` --references--> `CorrelationIdFilter`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → services/accounts/adapters/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/adapters/web/CorrelationIdFilter.kt
- `Task 4: Consulta de lançamento pela chave no `accounts` (Marina, `dev-kotlin`)` --references--> `AccountNotFoundException`  [INFERRED]
  docs/superpowers/plans/2026-10-27-etapa-2-transfers.md → services/accounts/application/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/application/ApplicationExceptions.kt
- `Task 10: Pedir, consultar e retomar transferências (Marina, `dev-kotlin`)` --references--> `IdempotencyKeyReusedException`  [INFERRED]
  docs/superpowers/plans/2026-10-27-etapa-2-transfers.md → services/accounts/application/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/application/ApplicationExceptions.kt

## Import Cycles
- None detected.

## Communities (127 total, 55 thin omitted)

### Community 1 - "AccountsIntegrationTest"
Cohesion: 0.05
Nodes (10): OutboxSchemaAssertionsIntegrationTest, assertOutboxSchemaMatchesModel(), createModelSchema(), currentSchema(), OutboxSchemaSnapshot, AccountsIntegrationTest, OutboxSchemaIntegrationTest, UseCaseConfigurationIntegrationTest (+2 more)

### Community 2 - "AccountApiIntegrationTest"
Cohesion: 0.09
Nodes (3): AccountApiIntegrationTest, assertProblem(), EntryLookupApiIntegrationTest

### Community 4 - "Transfer"
Cohesion: 0.15
Nodes (4): Transfer, Compensate, Credit, Debit

### Community 6 - "ServiceArchitectureRulesTest"
Cohesion: 0.06
Nodes (12): Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 2: Valores monetários e tipos (Marina, `dev-kotlin`), Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`), Task 9: API HTTP, Problem Details e correlation ID (Marina, `dev-kotlin`), Task 3: Regras ArchUnit compartilhadas (Marina, `dev-kotlin`) — cartão #32 (+4 more)

### Community 7 - "OutboxRelay"
Cohesion: 0.05
Nodes (10): Money, CompensateStepTest, CreditStepTest, DebitStepTest, MoneyTest, ResolveCreditStepTest, ResolveDebitStepTest, TerminalStepsTest (+2 more)

### Community 8 - "Account"
Cohesion: 0.08
Nodes (14): Clientes e contas, Task 8: Esquema, usuários de banco e repositórios JDBC (Marina, `dev-kotlin`; o script de init do compose segue o padrão do Rafael), JdbcAccountRepository, AccountBusyException, AccountRepository, InsertAccountResult, DUPLICATE_CUSTOMER, DUPLICATE_OPENING_KEY (+6 more)

### Community 9 - "Money"
Cohesion: 0.09
Nodes (6): Balance, Money, AccountTest, BalanceTest, LedgerEntryTest, MoneyTest

### Community 10 - "Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)"
Cohesion: 0.17
Nodes (11): Bloco 1 — preparação para o 2º serviço (branch `etapa-2-transfers`, PR 1), Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2), Task 10: Pedir, consultar e retomar transferências (Marina, `dev-kotlin`), Task 11: Persistência do `transfers` (Marina, `dev-kotlin`), Task 13: API HTTP do `transfers` (Marina, `dev-kotlin`), Task 15: Infra local, Bruno, smoke e CI do `transfers` (Rafael, `infra`), Task 16: Testes ponta a ponta (Marina, `dev-kotlin`; Gradle do módulo pode vir do Rafael), Task 1: Gradle para vários serviços (Rafael, `infra`) — cartão #37 (+3 more)

### Community 11 - "ProblemDetailsExceptionHandler.kt"
Cohesion: 0.12
Nodes (8): IdempotencyKeyHeader, ProblemDetailsExceptionHandler, AccountTypeNotFoundException, EntryNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException, BalanceOverflowException

### Community 12 - "OutboxRelayIntegrationTest.kt"
Cohesion: 0.08
Nodes (4): OutboxRelayIntegrationTest, ReceivedEvent, RecordingEventPublisher, RecordingPublisherConfiguration

### Community 13 - "JdbcOutboxWriter"
Cohesion: 0.12
Nodes (15): 10. Documentação que acompanha, 3. Esquema do banco, 5. Outbox e relay (`libs/outbox`), 6. API, 7. Testes, 8. Ambiente local e CI, 9. Entrega em dois blocos, Critério de pronto (+7 more)

### Community 14 - "LedgerEntry"
Cohesion: 0.08
Nodes (12): Ledger, JdbcLedgerEntryRepository, LedgerEntryRepository, InMemoryLedgerEntryRepository, DebitResult, EntryRecorded, Recorded, Refused (+4 more)

### Community 16 - "OutboxRelayTest"
Cohesion: 0.10
Nodes (4): FakeEventPublisher, FakeOutboxStore, MutableClock, OutboxRelayTest

### Community 17 - "duration"
Cohesion: 0.10
Nodes (14): JdbcAccountTypeRepository, AccountTypeRepository, InMemoryAccountTypeRepository, ChangeOverdraftLimitUseCaseTest, ListAccountTypesUseCaseTest, AccountTypeRepository, AccountType, BLACK (+6 more)

### Community 18 - "Decisões tomadas"
Cohesion: 0.13
Nodes (15): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Em aberto, Fluxo de entrada (+7 more)

### Community 20 - "AccountController.kt"
Cohesion: 0.31
Nodes (3): AccountController, AccountDTO, LedgerEntryDTO

### Community 21 - "SnsEventPublisherIntegrationTest.kt"
Cohesion: 0.06
Nodes (6): Task 14: Verificação do SNS FIFO no LocalStack (Rafael, `infra`), LocalStackFifoIntegrationTest, ReceivedMessage, SnsEventPublisherIntegrationTest, LocalStackTestSupport, LocalStackTestSupportTest

### Community 22 - "TransferStep"
Cohesion: 0.16
Nodes (13): Etapa 2 — `transfers` + saga Implementation Plan, Global Constraints, Review Focus, Task 8: Domínio da transferência e passos da saga (Marina, `dev-kotlin`), 3. Modelo de domínio, Riscos, Compensated, Completed (+5 more)

### Community 23 - "coordenador.md"
Cohesion: 0.29
Nodes (6): Ao começar uma sessão, Board, Para cada etapa do roadmap, Para cada tarefa, Paralelismo, Quando faltar algo

### Community 24 - "0004 — API Gateway HTTP API na borda"
Cohesion: 0.33
Nodes (5): 0004 — API Gateway HTTP API na borda, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 25 - "0005 — Frontend Angular e BFF em NestJS"
Cohesion: 0.33
Nodes (5): 0005 — Frontend Angular e BFF em NestJS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 26 - "0006 — Observabilidade só com correlation ID"
Cohesion: 0.40
Nodes (5): 0006 — Observabilidade só com correlation ID, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 27 - "CorrelationIdFilter.kt"
Cohesion: 0.13
Nodes (6): OrderController, PlaceOrderCommand, PlaceOrderResult, PlaceOrderUseCase, Order, CorrelationIdFilter

### Community 28 - "LedgerEntryRecorded"
Cohesion: 0.12
Nodes (7): Task 6: Casos de uso de tipo e limites (Marina, `dev-kotlin`), ChangeAccountTypeUseCase, CallLog, AccountTypeRepository, DirectTransactionRunner, ChangeAccountTypeUseCaseTest, AccountTypeChanged

### Community 29 - "Rodando o projeto localmente (macOS)"
Cohesion: 0.18
Nodes (11): 1. Configurar o ambiente, 2. Subir a infraestrutura (PostgreSQL, Redis, nginx, LocalStack), 3. Build e testes, 4. Subir o serviço `accounts`, 5. Conferir a API pelo nginx, 6. Ver os eventos na fila de inspeção, 7. Coleção Bruno, 8. Derrubar tudo (+3 more)

### Community 31 - "0008 — Extrato (`statement`) com CQRS no DynamoDB"
Cohesion: 0.40
Nodes (4): 0008 — Extrato (`statement`) com CQRS no DynamoDB, Consequências, Contexto, Decisão

### Community 32 - "0009 — Cache do extrato"
Cohesion: 0.40
Nodes (4): 0009 — Cache do extrato, Consequências, Contexto, Decisão

### Community 33 - "0010 — Limite diário com Redis + Lua"
Cohesion: 0.40
Nodes (4): 0010 — Limite diário com Redis + Lua, Consequências, Contexto, Decisão

### Community 34 - "0013 — Persistência com Spring JDBC (`JdbcClient`)"
Cohesion: 0.40
Nodes (5): 0013 — Persistência com Spring JDBC (`JdbcClient`), Alternativas consideradas, Consequências, Contexto, Decisão

### Community 35 - "0014 — Tipo de conta e cheque especial"
Cohesion: 0.40
Nodes (5): 0014 — Tipo de conta e cheque especial, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 36 - "ApiClient"
Cohesion: 0.05
Nodes (8): ConcurrencyIntegrationTest, OpenApiIntegrationTest, OutboxRelayUnreachableBrokerIntegrationTest, ApiClient, ApiResponse, isAccountBusy(), retryWhileAccountBusy(), RetryWhileAccountBusyTest

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
Nodes (7): 0015 — Relay do outbox com líder único e SNS FIFO, Alternativas consideradas, Consequências, Contexto, Decisão, Task 14: Agendador, relay e eventos no `transfers` (Marina, `dev-kotlin`), OutboxRelayScheduler

### Community 62 - "OutboxRelayConfiguration.kt"
Cohesion: 0.17
Nodes (3): AwsProperties, OutboxRelayConfiguration, OutboxRelayProperties

### Community 63 - "account"
Cohesion: 0.33
Nodes (3): account_type, account, ledger_entry

### Community 64 - "Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff)"
Cohesion: 0.14
Nodes (5): Task 7: `libs/outbox` — envelope e writer (Marie, `dev-kotlin`; em paralelo às Tasks 2–6), SnsEventPublisher, OutboxEventEnvelope, PendingEvent, OutboxEventEnvelopeTest

### Community 65 - "outbox-table.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 69 - "LedgerEntryRecorded"
Cohesion: 0.07
Nodes (14): UuidV7IdGenerator, IdGenerator, RecordCreditUseCase, DebitOutcome, RecordDebitUseCase, Recorded, Refused, RecordEntryCommand (+6 more)

### Community 70 - "RecordDebitUseCase.kt"
Cohesion: 0.22
Nodes (4): EventOutbox, TransactionRunner, RecordingEventOutbox, AccountEvent

### Community 71 - "UseCaseConfiguration.kt"
Cohesion: 0.18
Nodes (4): 1. Módulos e camadas, ChangeOverdraftLimitUseCase, GetAccountUseCase, ListAccountTypesUseCase

### Community 73 - "Etapa 2 — `transfers` + saga: design"
Cohesion: 0.15
Nodes (13): 2. Módulos e camadas, 4. Fluxos, 5. Esquema do banco (`transfers`), 6. Eventos, 7. API, nginx e Bruno, 8. Testes, 9. Ambiente local e CI, Critério de pronto (+5 more)

### Community 74 - "0001 — Stack: Kotlin + Spring Boot 4 na AWS"
Cohesion: 0.40
Nodes (5): 0001 — Stack: Kotlin + Spring Boot 4 na AWS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 75 - "smoke.sh"
Cohesion: 0.83
Nodes (3): aws_local(), call(), smoke.sh script

### Community 76 - "0016 — Saga de transferência orquestrada por HTTP síncrono"
Cohesion: 0.40
Nodes (5): 0016 — Saga de transferência orquestrada por HTTP síncrono, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 77 - "Etapa 0 — Fundação: design"
Cohesion: 0.18
Nodes (12): Task 13: Documentação do bloco 1 e das decisões da etapa (Clara, `documentador`; em paralelo às Tasks 8–12), Task 6: Build seletivo no CI (Rafael, `infra`), 2. Build e convenções, 4. Ambiente local, 5. CI (GitHub Actions), Critério de pronto, Etapa 0 — Fundação: design, Fora de escopo (e onde foi registrado) (+4 more)

### Community 78 - "0012 — Saldo no lançamento e trava pessimista por conta"
Cohesion: 0.40
Nodes (5): 0012 — Saldo no lançamento e trava pessimista por conta, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 79 - "AccountsApplicationIntegrationTest.kt"
Cohesion: 0.16
Nodes (5): Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`), AccountsApplicationIntegrationTest, ParameterizedTypeReference, RequestThreadProbe, RequestThreadProbeConfiguration

### Community 80 - "01-accounts-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 01-accounts-topic.sh script

### Community 81 - "AvailableBalance"
Cohesion: 0.23
Nodes (3): 2. Modelo de domínio, GetAccountUseCaseTest, AvailableBalance

### Community 82 - "JdbcOutboxStore"
Cohesion: 0.24
Nodes (3): Bloco 2 — relay e SNS FIFO, Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), JdbcOutboxStore

### Community 83 - "OutboxStore"
Cohesion: 0.13
Nodes (3): OutboxBacklog, OutboxStore, BlockingStore

### Community 84 - "OutboxRelay"
Cohesion: 0.23
Nodes (9): Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), Task 2: Relay em módulo próprio e teste de esquema (Marina, `dev-kotlin`) — cartões #25 e #30, 1. Entrega em dois blocos, EventPublisher, OutboxMetrics, OutboxRelay, OutboxRelaySettings, NoOpPublisher (+1 more)

### Community 85 - "Foco de revisão"
Cohesion: 0.15
Nodes (11): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 4: Ambiente local (docker-compose), Task 5: CI no GitHub Actions (+3 more)

### Community 87 - "JdbcOutboxStoreIntegrationTest"
Cohesion: 0.08
Nodes (3): JdbcOutboxStoreIntegrationTest, JdbcOutboxWriterIntegrationTest, OutboxMessage

### Community 88 - "TransferStatus"
Cohesion: 0.18
Nodes (9): Banco digital, Consulta e avisos, Transferências, 10. Documentação que acompanha, TransferStatus, COMPENSATED, COMPLETED, FAILED (+1 more)

### Community 89 - "OrderResource"
Cohesion: 0.27
Nodes (5): PersistenceUsesWeb, OrderResource, WebUsesPersistence, ApplicationUsesAdapters, DomainUsesAdapters

### Community 91 - "AccountsCall"
Cohesion: 0.28
Nodes (5): Task 12: Cliente HTTP do `accounts` (Marina, `dev-kotlin`), AccountsCall, Credit, Debit, FindEntry

### Community 94 - "0002 — Clean Architecture por serviço"
Cohesion: 0.40
Nodes (5): 0002 — Clean Architecture por serviço, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 96 - "AccountsApplication.kt"
Cohesion: 0.33
Nodes (4): Task 3: Aplicação `accounts` com health do banco (Testcontainers), 3. Esqueleto do `accounts`, Testes (no `bootstrap`, que enxerga todos os módulos), AccountsApplication

### Community 97 - ".outbox"
Cohesion: 0.33
Nodes (4): Etapa 1 — `accounts` Implementation Plan, Global Constraints, Review Focus, EventOutbox

### Community 99 - "TransferEvent"
Cohesion: 0.38
Nodes (4): TransferCompensated, TransferCompleted, TransferEvent, TransferFailed

### Community 100 - "AccountNotFoundException"
Cohesion: 0.13
Nodes (12): Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`), Task 5: Casos de uso de crédito e débito (Marina, `dev-kotlin`), 4. Fluxos, AccountNotFoundException, CustomerAlreadyHasAccountException, IdempotencyKeyReusedException, IdempotentEntryLookup, OpenAccountCommand (+4 more)

### Community 101 - "AccountRequestDTOs.kt"
Cohesion: 0.33
Nodes (4): ChangeAccountTypeDTO, ChangeOverdraftLimitDTO, OpenAccountDTO, RecordEntryDTO

### Community 102 - "Regras inegociáveis"
Cohesion: 0.22
Nodes (8): Agentes do projeto, Ao terminar uma tarefa, Banco digital em microsserviços, Contexto (leia antes de qualquer tarefa), Fluxo de trabalho (skills), Git, Grafo do código (graphify), Regras inegociáveis

### Community 103 - "Status do projeto"
Cohesion: 0.33
Nodes (6): Ambiente do Claude, Como rodar localmente, Como usar o board, Concluído, Convenções que já custaram caro, Status do projeto

### Community 106 - "StepOutcome"
Cohesion: 0.33
Nodes (4): Refused, StepOutcome, Success, TransientFailure

## Knowledge Gaps
- **19 isolated node(s):** `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION`, `INSERTED`, `DUPLICATE_OPENING_KEY` (+14 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 353 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **55 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AccountsIntegrationTest` connect `AccountsIntegrationTest` to `Bloco 1 — `accounts` gravando no outbox`, `AccountApiIntegrationTest`, `ApiClient`, `Regras inegociáveis`, `ServiceArchitectureRulesTest`, `Account`, `OutboxRelayIntegrationTest.kt`, `LedgerEntry`, `AccountsApplicationIntegrationTest.kt`, `Account`, `OutboxRelay`?**
  _High betweenness centrality (0.069) - this node is a cross-community bridge._
- **Are the 7 inferred relationships involving `Account` (e.g. with `Regras inegociáveis` and `Clientes e contas`) actually correct?**
  _`Account` has 7 INFERRED edges - model-reasoned connections that need verification._
- **What connects `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION` to the rest of the system?**
  _19 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Bloco 1 — `accounts` gravando no outbox` be split into smaller, more focused modules?**
  _Cohesion score 0.14516129032258066 - nodes in this community are weakly interconnected._
- **Why does `Transfer` connect `Transfer` to `TransferEvent`, `OutboxRelay`, `Etapa 2 — `transfers` + saga: design`, `Bloco 2 — o `transfers` (branch `etapa-2-bloco-2` a partir da `main` com o bloco 1, PR 2)`, `StepOutcome`, `TransferStep`, `TransferStatus`, `.run`, `AccountsCall`?**
  _High betweenness centrality (0.057) - this node is a cross-community bridge._
- **Are the 9 inferred relationships involving `LedgerEntry` (e.g. with `Regras inegociáveis` and `Ledger`) actually correct?**
  _`LedgerEntry` has 9 INFERRED edges - model-reasoned connections that need verification._
- **Should `AccountsIntegrationTest` be split into smaller, more focused modules?**
  _Cohesion score 0.05472636815920398 - nodes in this community are weakly interconnected._