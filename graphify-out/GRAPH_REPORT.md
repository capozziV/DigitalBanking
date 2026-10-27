# Graph Report - DigitalBanking  (2026-10-27)

## Corpus Check
- 167 files · ~51,964 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 18 file(s) not represented in the graph (top: .bru 8, (none) 3, .properties 2)

## Summary
- 1445 nodes · 3530 edges · 98 communities (56 shown, 42 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 326 edges (avg confidence: 0.9)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `e85591c5`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AccountNotFoundException
- AccountsIntegrationTest
- AccountApiIntegrationTest
- SpringTransactionRunner
- OverdraftLimit
- SnsEventPublisherTest.kt
- SnsEventPublisherIntegrationTest.kt
- OutboxRelay
- Account
- Balance
- .record
- ProblemDetailsExceptionHandler.kt
- OutboxRelayIntegrationTest.kt
- JdbcOutboxStoreIntegrationTest
- LedgerEntry
- Account
- CorrelationIdFilter.kt
- duration
- Decisões tomadas
- README.md
- AccountController
- AccountTypeAdminController.kt
- AvailableBalance
- coordenador.md
- 0004 — API Gateway HTTP API na borda
- 0005 — Frontend Angular e BFF em NestJS
- 0006 — Observabilidade só com correlation ID
- .findLimit
- LedgerEntryRecorded
- Rodando o projeto localmente (macOS)
- CallLog
- 0008 — Extrato (`statement`) com CQRS no DynamoDB
- 0009 — Cache do extrato
- 0010 — Limite diário com Redis + Lua
- 0013 — Persistência com Spring JDBC (`JdbcClient`)
- 0014 — Tipo de conta e cheque especial
- ApiClient
- RecordCreditUseCaseTest
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
- Banco digital
- PendingEvent
- OutboxRelayTest
- 0001 — Stack: Kotlin + Spring Boot 4 na AWS
- smoke.sh
- Money
- uuid
- 0012 — Saldo no lançamento e trava pessimista por conta
- AccountsApplicationIntegrationTest.kt
- 01-accounts-topic.sh
- SnsEventPublisher
- JdbcOutboxStore
- OutboxStore
- .record
- OutboxWriterConfiguration.kt
- .entry
- Test
- OutboxRelaySettingsTest.kt
- .require
- OpenApiConfiguration.kt
- .execute
- 0002 — Clean Architecture por serviço
- AccountOpened
- AccountRequestDTOs.kt
- Regras inegociáveis

## God Nodes (most connected - your core abstractions)
1. `Account` - 66 edges
2. `AccountApiIntegrationTest` - 61 edges
3. `Money` - 55 edges
4. `LedgerEntry` - 54 edges
5. `OverdraftLimit` - 54 edges
6. `Balance` - 47 edges
7. `AccountType` - 46 edges
8. `JdbcRepositoriesIntegrationTest` - 45 edges
9. `assertProblem()` - 40 edges
10. `OutboxRelayIntegrationTest` - 34 edges

## Surprising Connections (you probably didn't know these)
- `Mensageria` --references--> `OutboxRelay`  [INFERRED]
  docs/arquitetura.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/OutboxRelay.kt
- `Concluído` --references--> `OutboxRelay`  [INFERRED]
  docs/status.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/OutboxRelay.kt
- `Task 9: API HTTP, Problem Details e correlation ID (Marina, `dev-kotlin`)` --references--> `CorrelationIdFilter`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → services/accounts/adapters/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/adapters/web/CorrelationIdFilter.kt
- `Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`)` --references--> `ArchitectureTest`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → services/accounts/bootstrap/src/test/kotlin/io/github/capozziv/digitalbanking/accounts/bootstrap/ArchitectureTest.kt
- `Decisão` --references--> `LedgerEntry`  [INFERRED]
  docs/adr/0012-saldo-no-lancamento-e-trava-pessimista.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/LedgerEntry.kt

## Import Cycles
- None detected.

## Communities (98 total, 42 thin omitted)

### Community 0 - "AccountNotFoundException"
Cohesion: 0.14
Nodes (15): Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 2: Valores monetários e tipos (Marina, `dev-kotlin`), Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`), Task 5: Casos de uso de crédito e débito (Marina, `dev-kotlin`), Task 6: Casos de uso de tipo e limites (Marina, `dev-kotlin`) (+7 more)

### Community 1 - "AccountsIntegrationTest"
Cohesion: 0.07
Nodes (5): AccountsIntegrationTest, OutboxRelayUnreachableBrokerIntegrationTest, UseCaseConfigurationIntegrationTest, AccountTypeAdminApiIntegrationTest, UnexpectedErrorApiIntegrationTest

### Community 4 - "OverdraftLimit"
Cohesion: 0.09
Nodes (15): JdbcAccountTypeRepository, AccountTypeRepository, ListAccountTypesUseCase, InMemoryAccountTypeRepository, ChangeOverdraftLimitUseCaseTest, ListAccountTypesUseCaseTest, AccountTypeRepository, AccountType (+7 more)

### Community 6 - "SnsEventPublisherIntegrationTest.kt"
Cohesion: 0.06
Nodes (6): Task 14: Verificação do SNS FIFO no LocalStack (Rafael, `infra`), LocalStackFifoIntegrationTest, ReceivedMessage, SnsEventPublisherIntegrationTest, LocalStackTestSupport, LocalStackTestSupportTest

### Community 7 - "OutboxRelay"
Cohesion: 0.17
Nodes (7): 0015 — Relay do outbox com líder único e SNS FIFO, Alternativas consideradas, Consequências, Contexto, Decisão, OutboxMetrics, OutboxRelay

### Community 8 - "Account"
Cohesion: 0.08
Nodes (10): JdbcAccountRepository, AccountRepository, InsertAccountResult, DUPLICATE_CUSTOMER, DUPLICATE_OPENING_KEY, INSERTED, InMemoryAccountRepository, InMemoryAccountRepositoryTest (+2 more)

### Community 10 - ".record"
Cohesion: 0.33
Nodes (4): DebitResult, EntryRecorded, Recorded, Refused

### Community 11 - "ProblemDetailsExceptionHandler.kt"
Cohesion: 0.13
Nodes (6): ProblemDetailsExceptionHandler, AccountTypeNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException, BalanceOverflowException

### Community 12 - "OutboxRelayIntegrationTest.kt"
Cohesion: 0.08
Nodes (3): OutboxRelayIntegrationTest, ReceivedEvent, RecordingPublisherConfiguration

### Community 13 - "JdbcOutboxStoreIntegrationTest"
Cohesion: 0.08
Nodes (3): JdbcOutboxStoreIntegrationTest, JdbcOutboxWriterIntegrationTest, OutboxMessage

### Community 14 - "LedgerEntry"
Cohesion: 0.13
Nodes (5): Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`), JdbcLedgerEntryRepository, LedgerEntryRepository, InMemoryLedgerEntryRepository, LedgerEntry

### Community 18 - "Decisões tomadas"
Cohesion: 0.14
Nodes (14): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Em aberto, Fluxo de entrada (+6 more)

### Community 20 - "AccountController"
Cohesion: 0.33
Nodes (3): AccountController, AccountDTO, LedgerEntryDTO

### Community 22 - "AvailableBalance"
Cohesion: 0.23
Nodes (5): Ledger, 2. Modelo de domínio, AccountView, GetAccountUseCaseTest, AvailableBalance

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

### Community 28 - "LedgerEntryRecorded"
Cohesion: 0.38
Nodes (3): RecordEntryCommand, RecordDebitUseCaseTest, LedgerEntryRecorded

### Community 29 - "Rodando o projeto localmente (macOS)"
Cohesion: 0.17
Nodes (11): 1. Configurar o ambiente, 2. Subir a infraestrutura (PostgreSQL, Redis, nginx, LocalStack), 3. Build e testes, 4. Subir o serviço `accounts`, 5. Conferir a API pelo nginx, 6. Ver os eventos na fila de inspeção, 7. Coleção Bruno, 8. Derrubar tudo (+3 more)

### Community 30 - "CallLog"
Cohesion: 0.13
Nodes (7): 4. Fluxos, ChangeAccountTypeUseCase, ChangeOverdraftLimitUseCase, CallLog, DirectTransactionRunner, ChangeAccountTypeUseCaseTest, AccountTypeChanged

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

### Community 36 - "ApiClient"
Cohesion: 0.07
Nodes (7): ConcurrencyIntegrationTest, OpenApiIntegrationTest, ApiClient, ApiResponse, isAccountBusy(), retryWhileAccountBusy(), RetryWhileAccountBusyTest

### Community 37 - "RecordCreditUseCaseTest"
Cohesion: 0.31
Nodes (3): RecordEntryResult, SequentialIdGenerator, RecordCreditUseCaseTest

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

### Community 62 - "OutboxRelayConfiguration.kt"
Cohesion: 0.17
Nodes (4): OutboxRelaySettings, AwsProperties, OutboxRelayConfiguration, OutboxRelayProperties

### Community 63 - "account"
Cohesion: 0.33
Nodes (3): account_type, account, ledger_entry

### Community 64 - "Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff)"
Cohesion: 0.20
Nodes (6): Bloco 2 — relay e SNS FIFO, Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), Task 7: `libs/outbox` — envelope e writer (Marie, `dev-kotlin`; em paralelo às Tasks 2–6), 3. Esquema do banco, OutboxEventAppender

### Community 65 - "outbox-table.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 69 - "LedgerEntryRecorded"
Cohesion: 0.05
Nodes (24): 1. Módulos e camadas, 10. Documentação que acompanha, 2. Módulos e camadas, 3. Modelo de domínio, 4. Fluxos, 5. Esquema do banco (`transfers`), 6. Eventos, 7. API, nginx e Bruno (+16 more)

### Community 70 - "Banco digital"
Cohesion: 0.40
Nodes (4): Banco digital, Clientes e contas, Consulta e avisos, Transferências

### Community 71 - "PendingEvent"
Cohesion: 0.15
Nodes (6): EventPublisher, OutboxEventEnvelope, PendingEvent, OutboxEventEnvelopeTest, RecordingEventPublisher, NoOpPublisher

### Community 72 - "OutboxRelayTest"
Cohesion: 0.10
Nodes (4): FakeEventPublisher, FakeOutboxStore, MutableClock, OutboxRelayTest

### Community 74 - "0001 — Stack: Kotlin + Spring Boot 4 na AWS"
Cohesion: 0.40
Nodes (5): 0001 — Stack: Kotlin + Spring Boot 4 na AWS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 75 - "smoke.sh"
Cohesion: 0.83
Nodes (3): aws_local(), call(), smoke.sh script

### Community 77 - "uuid"
Cohesion: 0.12
Nodes (4): ClockWiringIntegrationTest, EntryKind, CREDIT, DEBIT

### Community 78 - "0012 — Saldo no lançamento e trava pessimista por conta"
Cohesion: 0.40
Nodes (5): 0012 — Saldo no lançamento e trava pessimista por conta, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 79 - "AccountsApplicationIntegrationTest.kt"
Cohesion: 0.06
Nodes (27): Ambiente do Claude, Como rodar localmente, Como usar o board, Concluído, Convenções que já custaram caro, Status do projeto, Task 3: Aplicação `accounts` com health do banco (Testcontainers), Task 13: Documentação do bloco 1 e das decisões da etapa (Clara, `documentador`; em paralelo às Tasks 8–12) (+19 more)

### Community 80 - "01-accounts-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 01-accounts-topic.sh script

### Community 81 - "SnsEventPublisher"
Cohesion: 0.13
Nodes (15): 10. Documentação que acompanha, 5. Outbox e relay (`libs/outbox`), 6. API, 7. Testes, 8. Ambiente local e CI, 9. Entrega em dois blocos, Critério de pronto, Etapa 1 — `accounts`: design (+7 more)

### Community 85 - ".record"
Cohesion: 0.06
Nodes (16): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 4: Ambiente local (docker-compose), Task 5: CI no GitHub Actions (+8 more)

### Community 93 - ".execute"
Cohesion: 0.83
Nodes (3): DebitOutcome, Recorded, Refused

### Community 94 - "0002 — Clean Architecture por serviço"
Cohesion: 0.20
Nodes (6): 0002 — Clean Architecture por serviço, Alternativas consideradas, Consequências, Contexto, Decisão, Roadmap

### Community 100 - "AccountOpened"
Cohesion: 0.12
Nodes (9): EventOutbox, TransactionRunner, OpenAccountCommand, OpenAccountResult, OpenAccountUseCase, RecordingEventOutbox, OpenAccountUseCaseTest, AccountEvent (+1 more)

### Community 101 - "AccountRequestDTOs.kt"
Cohesion: 0.33
Nodes (4): ChangeAccountTypeDTO, ChangeOverdraftLimitDTO, OpenAccountDTO, RecordEntryDTO

### Community 102 - "Regras inegociáveis"
Cohesion: 0.22
Nodes (8): Agentes do projeto, Ao terminar uma tarefa, Banco digital em microsserviços, Contexto (leia antes de qualquer tarefa), Fluxo de trabalho (skills), Git, Grafo do código (graphify), Regras inegociáveis

## Knowledge Gaps
- **13 isolated node(s):** `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION`, `INSERTED`, `DUPLICATE_OPENING_KEY` (+8 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 311 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **42 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AccountsIntegrationTest` connect `AccountsIntegrationTest` to `Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff)`, `AccountNotFoundException`, `AccountApiIntegrationTest`, `ApiClient`, `Regras inegociáveis`, `OutboxRelayIntegrationTest.kt`, `uuid`, `AccountsApplicationIntegrationTest.kt`, `Account`?**
  _High betweenness centrality (0.106) - this node is a cross-community bridge._
- **Are the 7 inferred relationships involving `Account` (e.g. with `Regras inegociáveis` and `Clientes e contas`) actually correct?**
  _`Account` has 7 INFERRED edges - model-reasoned connections that need verification._
- **What connects `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION` to the rest of the system?**
  _13 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AccountNotFoundException` be split into smaller, more focused modules?**
  _Cohesion score 0.14285714285714285 - nodes in this community are weakly interconnected._
- **Why does `OutboxRelay` connect `OutboxRelay` to `Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff)`, `PendingEvent`, `OutboxRelayIntegrationTest.kt`, `uuid`, `AccountsApplicationIntegrationTest.kt`, `SnsEventPublisher`, `Decisões tomadas`, `OutboxStore`, `OutboxRelayScheduler`, `OutboxRelayConfiguration.kt`?**
  _High betweenness centrality (0.072) - this node is a cross-community bridge._
- **Are the 26 inferred relationships involving `Money` (e.g. with `Ledger` and `10. Documentação que acompanha`) actually correct?**
  _`Money` has 26 INFERRED edges - model-reasoned connections that need verification._
- **Should `AccountsIntegrationTest` be split into smaller, more focused modules?**
  _Cohesion score 0.07092198581560284 - nodes in this community are weakly interconnected._