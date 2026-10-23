# Graph Report - digitalbanking  (2026-10-23)

## Corpus Check
- 160 files · ~45,006 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 18 file(s) not represented in the graph (top: .bru 8, (none) 3, .properties 2)

## Summary
- 1303 nodes · 3175 edges · 77 communities (47 shown, 30 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 301 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `17c38a6e`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- LedgerEntryRecorded
- IdGenerator
- AccountApiIT
- SpringTransactionRunner
- AccountsIntegrationTest
- SnsEventPublisherTest.kt
- LocalStackFifoIT.kt
- JdbcRepositoriesIT
- CallLog
- Money
- Bloco 1 — `accounts` gravando no outbox
- ProblemDetailsExceptionHandler.kt
- OutboxRelay
- AvailableBalance
- LedgerEntry
- uuid
- SnsEventPublisher
- OverdraftLimit
- Decisões tomadas
- README.md
- JdbcOutboxStoreIT
- AccountController.kt
- status.md
- coordenador.md
- 0004 — API Gateway HTTP API na borda
- 0005 — Frontend Angular e BFF em NestJS
- 0006 — Observabilidade só com correlation ID
- Etapa 1 — `accounts`: design
- AccountsApplicationIT.kt
- Rodando o projeto localmente (macOS)
- 0012 — Saldo no lançamento e trava pessimista por conta
- 0008 — Extrato (`statement`) com CQRS no DynamoDB
- 0009 — Cache do extrato
- 0010 — Limite diário com Redis + Lua
- 0013 — Persistência com Spring JDBC (`JdbcClient`)
- 0014 — Tipo de conta e cheque especial
- 0015 — Relay do outbox com líder único e SNS FIFO
- Account
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
- outbox_stuck_idx
- account
- ConcurrencyIT
- outbox-table.sql
- V4__outbox.sql
- PendingEvent
- OutboxRelayTest
- smoke.sh
- SnsEventPublisherIT.kt
- AccountNotFoundException
- JdbcOutboxStore
- Banco digital em microsserviços
- .record
- 0001 — Stack: Kotlin + Spring Boot 4 na AWS

## God Nodes (most connected - your core abstractions)
1. `Account` - 67 edges
2. `AccountApiIT` - 61 edges
3. `LedgerEntry` - 55 edges
4. `OverdraftLimit` - 55 edges
5. `Money` - 54 edges
6. `Balance` - 47 edges
7. `AccountType` - 46 edges
8. `JdbcRepositoriesIT` - 45 edges
9. `assertProblem()` - 40 edges
10. `LedgerEntryRecorded` - 33 edges

## Surprising Connections (you probably didn't know these)
- `Decisão` --references--> `OutboxRelay`  [INFERRED]
  docs/adr/0015-outbox-relay-lider-e-sns-fifo.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/OutboxRelay.kt
- `Mensageria` --references--> `OutboxRelay`  [INFERRED]
  docs/arquitetura.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/OutboxRelay.kt
- `Decisão` --references--> `LedgerEntry`  [INFERRED]
  docs/adr/0012-saldo-no-lancamento-e-trava-pessimista.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/LedgerEntry.kt
- `Task 15: Relay e publicador SNS (Marina, `dev-kotlin`)` --references--> `JdbcOutboxStore`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/JdbcOutboxStore.kt
- `Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff)` --references--> `JdbcOutboxStore`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/JdbcOutboxStore.kt

## Import Cycles
- None detected.

## Communities (77 total, 30 thin omitted)

### Community 0 - "LedgerEntryRecorded"
Cohesion: 0.15
Nodes (9): DebitOutcome, Recorded, Refused, RecordEntryCommand, RecordEntryResult, SequentialIdGenerator, RecordCreditUseCaseTest, RecordDebitUseCaseTest (+1 more)

### Community 1 - "IdGenerator"
Cohesion: 0.07
Nodes (12): 1. Módulos e camadas, 4. Fluxos, UuidV7IdGenerator, IdGenerator, ChangeAccountTypeUseCase, ChangeOverdraftLimitUseCase, GetAccountUseCase, ListAccountTypesUseCase (+4 more)

### Community 4 - "AccountsIntegrationTest"
Cohesion: 0.09
Nodes (4): AccountsIntegrationTest, UseCaseConfigurationIT, AccountTypeAdminApiIT, UnexpectedErrorApiIT

### Community 6 - "LocalStackFifoIT.kt"
Cohesion: 0.08
Nodes (4): LocalStackFifoIT, ReceivedMessage, LocalStackTestSupport, LocalStackTestSupportTest

### Community 8 - "CallLog"
Cohesion: 0.12
Nodes (8): EventOutbox, CallLog, AccountTypeRepository, DirectTransactionRunner, RecordingEventOutbox, ChangeAccountTypeUseCaseTest, AccountEvent, AccountTypeChanged

### Community 9 - "Money"
Cohesion: 0.07
Nodes (10): Balance, DebitResult, EntryRecorded, Recorded, Refused, Money, AccountTest, BalanceTest (+2 more)

### Community 10 - "Bloco 1 — `accounts` gravando no outbox"
Cohesion: 0.07
Nodes (10): Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 2: Valores monetários e tipos (Marina, `dev-kotlin`), Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`), Task 6: Casos de uso de tipo e limites (Marina, `dev-kotlin`), Task 9: API HTTP, Problem Details e correlation ID (Marina, `dev-kotlin`) (+2 more)

### Community 11 - "ProblemDetailsExceptionHandler.kt"
Cohesion: 0.13
Nodes (6): ProblemDetailsExceptionHandler, AccountTypeNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException, BalanceOverflowException

### Community 12 - "OutboxRelay"
Cohesion: 0.26
Nodes (6): Bloco 2 — relay e SNS FIFO, Task 14: Verificação do SNS FIFO no LocalStack (Rafael, `infra`), Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), EventPublisher, OutboxRelay

### Community 13 - "AvailableBalance"
Cohesion: 0.15
Nodes (9): Banco digital, Clientes e contas, Consulta e avisos, Ledger, Transferências, Pendências e decisões em aberto, 2. Modelo de domínio, GetAccountUseCaseTest (+1 more)

### Community 14 - "LedgerEntry"
Cohesion: 0.14
Nodes (5): JdbcLedgerEntryRepository, LedgerEntryRepository, InMemoryLedgerEntryRepository, currentBalance(), LedgerEntry

### Community 15 - "uuid"
Cohesion: 0.11
Nodes (5): TransactionRunner, ClockWiringIT, EntryKind, CREDIT, DEBIT

### Community 17 - "OverdraftLimit"
Cohesion: 0.09
Nodes (14): JdbcAccountTypeRepository, AccountTypeRepository, InMemoryAccountTypeRepository, ChangeOverdraftLimitUseCaseTest, ListAccountTypesUseCaseTest, AccountTypeRepository, AccountType, BLACK (+6 more)

### Community 18 - "Decisões tomadas"
Cohesion: 0.14
Nodes (14): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Em aberto, Fluxo de entrada (+6 more)

### Community 21 - "AccountController.kt"
Cohesion: 0.06
Nodes (12): AccountController, AccountTypeAdminController, ChangeAccountTypeRequest, ChangeOverdraftLimitRequest, OpenAccountRequest, RecordEntryRequest, AccountResponse, AccountTypeLimitResponse (+4 more)

### Community 22 - "status.md"
Cohesion: 0.17
Nodes (6): 0002 — Clean Architecture por serviço, Alternativas consideradas, Consequências, Contexto, Decisão, Roadmap

### Community 23 - "coordenador.md"
Cohesion: 0.33
Nodes (5): Ao começar uma sessão, Para cada etapa do roadmap, Para cada tarefa, Paralelismo, Quando faltar algo

### Community 24 - "0004 — API Gateway HTTP API na borda"
Cohesion: 0.33
Nodes (5): 0004 — API Gateway HTTP API na borda, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 25 - "0005 — Frontend Angular e BFF em NestJS"
Cohesion: 0.33
Nodes (5): 0005 — Frontend Angular e BFF em NestJS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 26 - "0006 — Observabilidade só com correlation ID"
Cohesion: 0.33
Nodes (5): 0006 — Observabilidade só com correlation ID, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 27 - "Etapa 1 — `accounts`: design"
Cohesion: 0.07
Nodes (18): Task 7: `libs/outbox` — envelope e writer (Marie, `dev-kotlin`; em paralelo às Tasks 2–6), 10. Documentação que acompanha, 3. Esquema do banco, 5. Outbox e relay (`libs/outbox`), 6. API, 7. Testes, 8. Ambiente local e CI, 9. Entrega em dois blocos (+10 more)

### Community 28 - "AccountsApplicationIT.kt"
Cohesion: 0.06
Nodes (27): Ambiente do Claude, Com o usuário (fora do código), Como rodar localmente, Concluído, Em andamento, Próximos passos, Status do projeto, Task 3: Aplicação `accounts` com health do banco (Testcontainers) (+19 more)

### Community 29 - "Rodando o projeto localmente (macOS)"
Cohesion: 0.20
Nodes (10): 1. Configurar o ambiente, 2. Subir a infraestrutura (PostgreSQL, Redis, nginx), 3. Build e testes, 4. Subir o serviço `accounts`, 5. Conferir a API pelo nginx, 6. Coleção Bruno, 7. Derrubar tudo, Opcional: grafo do código (graphify) (+2 more)

### Community 30 - "0012 — Saldo no lançamento e trava pessimista por conta"
Cohesion: 0.40
Nodes (5): 0012 — Saldo no lançamento e trava pessimista por conta, Alternativas consideradas, Consequências, Contexto, Decisão

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

### Community 36 - "0015 — Relay do outbox com líder único e SNS FIFO"
Cohesion: 0.40
Nodes (5): 0015 — Relay do outbox com líder único e SNS FIFO, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 37 - "Account"
Cohesion: 0.08
Nodes (13): Task 8: Esquema, usuários de banco e repositórios JDBC (Marina, `dev-kotlin`; o script de init do compose segue o padrão do Rafael), Restrições (de `CLAUDE.md` e ADRs), JdbcAccountRepository, AccountBusyException, AccountRepository, InsertAccountResult, DUPLICATE_CUSTOMER, DUPLICATE_OPENING_KEY (+5 more)

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

### Community 63 - "account"
Cohesion: 0.33
Nodes (3): account_type, account, ledger_entry

### Community 64 - "ConcurrencyIT"
Cohesion: 0.06
Nodes (9): OutboxRelaySettings, OutboxRelaySettingsTest, ConcurrencyIT, OpenApiIT, ApiClient, ApiResponse, isAccountBusy(), retryWhileAccountBusy() (+1 more)

### Community 65 - "outbox-table.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 71 - "PendingEvent"
Cohesion: 0.27
Nodes (3): OutboxEnvelope, PendingEvent, OutboxEnvelopeTest

### Community 72 - "OutboxRelayTest"
Cohesion: 0.05
Nodes (8): OutboxMetrics, OutboxBacklog, OutboxStore, OutboxMetricsTest, FakeEventPublisher, FakeOutboxStore, MutableClock, OutboxRelayTest

### Community 83 - "AccountNotFoundException"
Cohesion: 0.15
Nodes (10): Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`), Task 5: Casos de uso de crédito e débito (Marina, `dev-kotlin`), AccountNotFoundException, CustomerAlreadyHasAccountException, IdempotencyKeyReusedException, IdempotentEntryLookup, OpenAccountCommand, OpenAccountResult (+2 more)

### Community 93 - "Banco digital em microsserviços"
Cohesion: 0.22
Nodes (8): Agentes do projeto, Ao terminar uma tarefa, Banco digital em microsserviços, Contexto (leia antes de qualquer tarefa), Fluxo de trabalho (skills), Git, Grafo do código (graphify), Regras inegociáveis

### Community 95 - ".record"
Cohesion: 0.08
Nodes (17): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 4: Ambiente local (docker-compose), Task 5: CI no GitHub Actions (+9 more)

### Community 106 - "0001 — Stack: Kotlin + Spring Boot 4 na AWS"
Cohesion: 0.40
Nodes (5): 0001 — Stack: Kotlin + Spring Boot 4 na AWS, Alternativas consideradas, Consequências, Contexto, Decisão

## Knowledge Gaps
- **10 isolated node(s):** `INSERTED`, `DUPLICATE_OPENING_KEY`, `DUPLICATE_CUSTOMER`, `FREE`, `STANDARD` (+5 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 285 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **30 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AccountsIntegrationTest` connect `AccountsIntegrationTest` to `ConcurrencyIT`, `AccountApiIT`, `Account`, `JdbcRepositoriesIT`, `OutboxRelay`, `uuid`, `AccountsApplicationIT.kt`?**
  _High betweenness centrality (0.095) - this node is a cross-community bridge._
- **Are the 8 inferred relationships involving `Account` (e.g. with `Regras inegociáveis` and `Clientes e contas`) actually correct?**
  _`Account` has 8 INFERRED edges - model-reasoned connections that need verification._
- **What connects `INSERTED`, `DUPLICATE_OPENING_KEY`, `DUPLICATE_CUSTOMER` to the rest of the system?**
  _10 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `IdGenerator` be split into smaller, more focused modules?**
  _Cohesion score 0.06818181818181818 - nodes in this community are weakly interconnected._
- **Why does `LedgerEntry` connect `LedgerEntry` to `LedgerEntryRecorded`, `Account`, `JdbcRepositoriesIT`, `Money`, `Bloco 1 — `accounts` gravando no outbox`, `AvailableBalance`, `uuid`, `OverdraftLimit`, `AccountNotFoundException`, `AccountController.kt`, `AccountsApplicationIT.kt`, `Banco digital em microsserviços`, `0012 — Saldo no lançamento e trava pessimista por conta`, `.record`?**
  _High betweenness centrality (0.078) - this node is a cross-community bridge._
- **Are the 10 inferred relationships involving `LedgerEntry` (e.g. with `Regras inegociáveis` and `Ledger`) actually correct?**
  _`LedgerEntry` has 10 INFERRED edges - model-reasoned connections that need verification._
- **Should `AccountApiIT` be split into smaller, more focused modules?**
  _Cohesion score 0.11367127496159754 - nodes in this community are weakly interconnected._