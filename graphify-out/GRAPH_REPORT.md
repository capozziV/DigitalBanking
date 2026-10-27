# Graph Report - DigitalBanking  (2026-10-27)

## Corpus Check
- 167 files · ~52,819 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 18 file(s) not represented in the graph (top: .bru 8, (none) 3, .properties 2)

## Summary
- 1446 nodes · 3542 edges · 96 communities (54 shown, 42 thin omitted)
- Extraction: 90% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 338 edges (avg confidence: 0.9)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `066025ca`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- JsonMapper
- AccountsIntegrationTest
- AccountApiIntegrationTest
- SpringTransactionRunner
- JdbcOutboxStoreIntegrationTest
- SnsEventPublisherTest.kt
- SnsEventPublisherIntegrationTest.kt
- OutboxRelay
- CallLog
- Balance
- .record
- ProblemDetailsExceptionHandler.kt
- OutboxRelayIntegrationTest.kt
- Etapa 1 — `accounts`: design
- LedgerEntry
- Account
- Account
- OverdraftLimit
- Decisões tomadas
- README.md
- AccountController
- AccountTypeAdminController.kt
- OutboxStore
- coordenador.md
- 0004 — API Gateway HTTP API na borda
- 0005 — Frontend Angular e BFF em NestJS
- 0006 — Observabilidade só com correlation ID
- UseCaseConfiguration.kt
- Etapa 2 — `transfers` + saga: design
- Rodando o projeto localmente (macOS)
- LedgerEntryRecorded
- 0008 — Extrato (`statement`) com CQRS no DynamoDB
- 0009 — Cache do extrato
- 0010 — Limite diário com Redis + Lua
- 0013 — Persistência com Spring JDBC (`JdbcClient`)
- 0014 — Tipo de conta e cheque especial
- ApiClient
- JdbcAccountRepository.kt
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
- outbox-table.sql
- V4__outbox.sql
- LedgerEntryRecorded
- RecordCreditUseCaseTest.kt
- PendingEvent
- OutboxRelayTest
- uuid
- smoke.sh
- Money
- OutboxWriterConfiguration.kt
- OutboxBacklog
- AccountsApplicationIntegrationTest.kt
- 01-accounts-topic.sh
- RecordDebitUseCase
- OutboxStore
- 0001 — Stack: Kotlin + Spring Boot 4 na AWS
- 0012 — Saldo no lançamento e trava pessimista por conta
- .record
- Clock
- assertthat
- 0015 — Relay do outbox com líder único e SNS FIFO
- InMemoryAccountRepositoryTest.kt
- OpenApiConfiguration.kt
- 0002 — Clean Architecture por serviço
- AccountOpened
- AccountRequestDTOs.kt
- Regras inegociáveis

## God Nodes (most connected - your core abstractions)
1. `Account` - 67 edges
2. `AccountApiIntegrationTest` - 61 edges
3. `Money` - 56 edges
4. `LedgerEntry` - 55 edges
5. `OverdraftLimit` - 55 edges
6. `Balance` - 47 edges
7. `AccountType` - 46 edges
8. `JdbcRepositoriesIntegrationTest` - 45 edges
9. `assertProblem()` - 40 edges
10. `OutboxRelayIntegrationTest` - 35 edges

## Surprising Connections (you probably didn't know these)
- `Restrições (de `CLAUDE.md` e ADRs)` --references--> `Money`  [INFERRED]
  docs/superpowers/specs/2026-10-27-etapa-2-transfers-design.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/Money.kt
- `2. Módulos e camadas` --references--> `IdGenerator`  [INFERRED]
  docs/superpowers/specs/2026-10-27-etapa-2-transfers-design.md → services/accounts/application/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/application/port/IdGenerator.kt
- `3. Modelo de domínio` --references--> `Money`  [INFERRED]
  docs/superpowers/specs/2026-10-27-etapa-2-transfers-design.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/Money.kt
- `Decisão` --references--> `LedgerEntry`  [INFERRED]
  docs/adr/0012-saldo-no-lancamento-e-trava-pessimista.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/LedgerEntry.kt
- `Mensageria` --references--> `OutboxRelay`  [INFERRED]
  docs/arquitetura.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/OutboxRelay.kt

## Import Cycles
- None detected.

## Communities (96 total, 42 thin omitted)

### Community 0 - "JsonMapper"
Cohesion: 0.29
Nodes (3): Task 7: `libs/outbox` — envelope e writer (Marie, `dev-kotlin`; em paralelo às Tasks 2–6), OutboxEventEnvelope, OutboxEventEnvelopeTest

### Community 1 - "AccountsIntegrationTest"
Cohesion: 0.08
Nodes (4): AccountsIntegrationTest, OutboxRelayUnreachableBrokerIntegrationTest, AccountTypeAdminApiIntegrationTest, UnexpectedErrorApiIntegrationTest

### Community 4 - "JdbcOutboxStoreIntegrationTest"
Cohesion: 0.08
Nodes (3): JdbcOutboxStoreIntegrationTest, JdbcOutboxWriterIntegrationTest, OutboxMessage

### Community 6 - "SnsEventPublisherIntegrationTest.kt"
Cohesion: 0.06
Nodes (6): Task 14: Verificação do SNS FIFO no LocalStack (Rafael, `infra`), LocalStackFifoIntegrationTest, ReceivedMessage, SnsEventPublisherIntegrationTest, LocalStackTestSupport, LocalStackTestSupportTest

### Community 8 - "CallLog"
Cohesion: 0.16
Nodes (5): ChangeAccountTypeUseCase, CallLog, DirectTransactionRunner, ChangeAccountTypeUseCaseTest, AccountTypeChanged

### Community 10 - ".record"
Cohesion: 0.33
Nodes (4): DebitResult, EntryRecorded, Recorded, Refused

### Community 11 - "ProblemDetailsExceptionHandler.kt"
Cohesion: 0.11
Nodes (7): IdempotencyKeyHeader, ProblemDetailsExceptionHandler, AccountTypeNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException, BalanceOverflowException

### Community 12 - "OutboxRelayIntegrationTest.kt"
Cohesion: 0.08
Nodes (3): OutboxRelayIntegrationTest, ReceivedEvent, RecordingPublisherConfiguration

### Community 13 - "Etapa 1 — `accounts`: design"
Cohesion: 0.11
Nodes (17): Bloco 2 — relay e SNS FIFO, Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), 3. Esquema do banco, 5. Outbox e relay (`libs/outbox`), 6. API, 7. Testes, 8. Ambiente local e CI (+9 more)

### Community 14 - "LedgerEntry"
Cohesion: 0.12
Nodes (4): JdbcLedgerEntryRepository, InMemoryLedgerEntryRepository, currentBalance(), LedgerEntry

### Community 16 - "Account"
Cohesion: 0.15
Nodes (7): AccountRepository, InsertAccountResult, DUPLICATE_CUSTOMER, DUPLICATE_OPENING_KEY, INSERTED, InMemoryAccountRepository, Account

### Community 17 - "OverdraftLimit"
Cohesion: 0.06
Nodes (25): Banco digital, Clientes e contas, Consulta e avisos, Transferências, 1. Módulos e camadas, JdbcAccountTypeRepository, AccountTypeRepository, ChangeOverdraftLimitUseCase (+17 more)

### Community 18 - "Decisões tomadas"
Cohesion: 0.14
Nodes (14): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Em aberto, Fluxo de entrada (+6 more)

### Community 20 - "AccountController"
Cohesion: 0.33
Nodes (3): AccountController, AccountDTO, LedgerEntryDTO

### Community 23 - "coordenador.md"
Cohesion: 0.33
Nodes (5): Ao começar uma sessão, Para cada etapa do roadmap, Para cada tarefa, Paralelismo, Quando faltar algo

### Community 24 - "0004 — API Gateway HTTP API na borda"
Cohesion: 0.40
Nodes (5): 0004 — API Gateway HTTP API na borda, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 25 - "0005 — Frontend Angular e BFF em NestJS"
Cohesion: 0.40
Nodes (5): 0005 — Frontend Angular e BFF em NestJS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 26 - "0006 — Observabilidade só com correlation ID"
Cohesion: 0.40
Nodes (5): 0006 — Observabilidade só com correlation ID, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 28 - "Etapa 2 — `transfers` + saga: design"
Cohesion: 0.12
Nodes (16): 10. Documentação que acompanha, 2. Módulos e camadas, 3. Modelo de domínio, 4. Fluxos, 5. Esquema do banco (`transfers`), 6. Eventos, 7. API, nginx e Bruno, 8. Testes (+8 more)

### Community 29 - "Rodando o projeto localmente (macOS)"
Cohesion: 0.17
Nodes (11): 1. Configurar o ambiente, 2. Subir a infraestrutura (PostgreSQL, Redis, nginx, LocalStack), 3. Build e testes, 4. Subir o serviço `accounts`, 5. Conferir a API pelo nginx, 6. Ver os eventos na fila de inspeção, 7. Coleção Bruno, 8. Derrubar tudo (+3 more)

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
Cohesion: 0.06
Nodes (8): ClockWiringIntegrationTest, ConcurrencyIntegrationTest, OpenApiIntegrationTest, ApiClient, ApiResponse, isAccountBusy(), retryWhileAccountBusy(), RetryWhileAccountBusyTest

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

### Community 65 - "outbox-table.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 69 - "LedgerEntryRecorded"
Cohesion: 0.16
Nodes (8): RecordCreditUseCase, DebitOutcome, Recorded, Refused, RecordEntryCommand, RecordEntryResult, SequentialIdGenerator, RecordCreditUseCaseTest

### Community 70 - "RecordCreditUseCaseTest.kt"
Cohesion: 0.12
Nodes (19): Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 13: Documentação do bloco 1 e das decisões da etapa (Clara, `documentador`; em paralelo às Tasks 8–12), Task 2: Valores monetários e tipos (Marina, `dev-kotlin`), Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`), Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`) (+11 more)

### Community 71 - "PendingEvent"
Cohesion: 0.16
Nodes (5): EventPublisher, PendingEvent, SnsEventPublisher, RecordingEventPublisher, NoOpPublisher

### Community 72 - "OutboxRelayTest"
Cohesion: 0.07
Nodes (6): Task 9: API HTTP, Problem Details e correlation ID (Marina, `dev-kotlin`), FakeEventPublisher, FakeOutboxStore, MutableClock, OutboxRelayTest, CorrelationIdFilter

### Community 74 - "uuid"
Cohesion: 0.14
Nodes (5): UuidV7IdGenerator, EventOutbox, IdGenerator, LedgerEntryRepository, TransactionRunner

### Community 75 - "smoke.sh"
Cohesion: 0.83
Nodes (3): aws_local(), call(), smoke.sh script

### Community 76 - "Money"
Cohesion: 0.20
Nodes (6): Ledger, Pendências e decisões em aberto, 2. Modelo de domínio, AvailableBalance, Money, AccountTest

### Community 79 - "AccountsApplicationIntegrationTest.kt"
Cohesion: 0.06
Nodes (27): Ambiente do Claude, Com o usuário (fora do código), Como rodar localmente, Concluído, Em andamento, Próximos passos, Status do projeto, Task 3: Aplicação `accounts` com health do banco (Testcontainers) (+19 more)

### Community 80 - "01-accounts-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 01-accounts-topic.sh script

### Community 83 - "0001 — Stack: Kotlin + Spring Boot 4 na AWS"
Cohesion: 0.40
Nodes (5): 0001 — Stack: Kotlin + Spring Boot 4 na AWS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 84 - "0012 — Saldo no lançamento e trava pessimista por conta"
Cohesion: 0.40
Nodes (5): 0012 — Saldo no lançamento e trava pessimista por conta, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 85 - ".record"
Cohesion: 0.05
Nodes (17): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 4: Ambiente local (docker-compose), Task 5: CI no GitHub Actions (+9 more)

### Community 88 - "assertthat"
Cohesion: 0.10
Nodes (4): OutboxRelaySettingsTest, UseCaseConfigurationIntegrationTest, LedgerEntryTest, MoneyTest

### Community 89 - "0015 — Relay do outbox com líder único e SNS FIFO"
Cohesion: 0.40
Nodes (5): 0015 — Relay do outbox com líder único e SNS FIFO, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 94 - "0002 — Clean Architecture por serviço"
Cohesion: 0.20
Nodes (6): 0002 — Clean Architecture por serviço, Alternativas consideradas, Consequências, Contexto, Decisão, Roadmap

### Community 100 - "AccountOpened"
Cohesion: 0.22
Nodes (6): 4. Fluxos, OpenAccountCommand, OpenAccountResult, OpenAccountUseCase, OpenAccountUseCaseTest, AccountOpened

### Community 101 - "AccountRequestDTOs.kt"
Cohesion: 0.33
Nodes (4): ChangeAccountTypeDTO, ChangeOverdraftLimitDTO, OpenAccountDTO, RecordEntryDTO

### Community 102 - "Regras inegociáveis"
Cohesion: 0.22
Nodes (8): Agentes do projeto, Ao terminar uma tarefa, Banco digital em microsserviços, Contexto (leia antes de qualquer tarefa), Fluxo de trabalho (skills), Git, Grafo do código (graphify), Regras inegociáveis

## Knowledge Gaps
- **13 isolated node(s):** `DUPLICATE_CUSTOMER`, `DUPLICATE_OPENING_KEY`, `INSERTED`, `BLACK`, `DIAMOND` (+8 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 310 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **42 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AccountsIntegrationTest` connect `AccountsIntegrationTest` to `AccountApiIntegrationTest`, `ApiClient`, `JdbcAccountRepository.kt`, `Regras inegociáveis`, `uuid`, `OutboxRelayIntegrationTest.kt`, `Etapa 1 — `accounts`: design`, `AccountsApplicationIntegrationTest.kt`, `Account`, `assertthat`?**
  _High betweenness centrality (0.094) - this node is a cross-community bridge._
- **Are the 8 inferred relationships involving `Account` (e.g. with `Regras inegociáveis` and `Clientes e contas`) actually correct?**
  _`Account` has 8 INFERRED edges - model-reasoned connections that need verification._
- **What connects `DUPLICATE_CUSTOMER`, `DUPLICATE_OPENING_KEY`, `INSERTED` to the rest of the system?**
  _13 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AccountsIntegrationTest` be split into smaller, more focused modules?**
  _Cohesion score 0.08084163898117387 - nodes in this community are weakly interconnected._
- **Why does `LedgerEntry` connect `LedgerEntry` to `AccountRequestDTOs.kt`, `RecordCreditUseCaseTest.kt`, `Regras inegociáveis`, `LedgerEntryRecorded`, `Balance`, `uuid`, `ProblemDetailsExceptionHandler.kt`, `Money`, `Etapa 1 — `accounts`: design`, `.record`, `AccountsApplicationIntegrationTest.kt`, `Account`, `OverdraftLimit`, `0012 — Saldo no lançamento e trava pessimista por conta`, `AccountController`, `.record`, `assertthat`, `LedgerEntryRecorded`?**
  _High betweenness centrality (0.078) - this node is a cross-community bridge._
- **Are the 27 inferred relationships involving `Money` (e.g. with `Ledger` and `Pendências e decisões em aberto`) actually correct?**
  _`Money` has 27 INFERRED edges - model-reasoned connections that need verification._
- **Should `AccountApiIntegrationTest` be split into smaller, more focused modules?**
  _Cohesion score 0.1111111111111111 - nodes in this community are weakly interconnected._