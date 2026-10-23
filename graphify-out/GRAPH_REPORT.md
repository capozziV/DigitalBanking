# Graph Report - digitalbanking  (2026-10-23)

## Corpus Check
- 157 files · ~43,752 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 18 file(s) not represented in the graph (top: .bru 8, (none) 3, .properties 2)

## Summary
- 1266 nodes · 3093 edges · 87 communities (49 shown, 38 thin omitted)
- Extraction: 90% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 298 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `6d1bd389`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- LedgerEntryRecorded
- IdGenerator
- AccountApiIT
- SpringTransactionRunner
- AccountsIntegrationTest
- 0012 — Saldo no lançamento e trava pessimista por conta
- LocalStackFifoIT.kt
- JdbcRepositoriesIT
- AccountEvent
- Balance
- Bloco 1 — `accounts` gravando no outbox
- ProblemDetailsExceptionHandler.kt
- CallLog
- AvailableBalance
- LedgerEntry
- uuid
- Money
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
- JdbcOutboxWriter
- AccountsApplicationIT.kt
- Rodando o projeto localmente (macOS)
- InMemoryAccountRepositoryTest.kt
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
- .entry
- account
- ConcurrencyIT
- outbox-table.sql
- V4__outbox.sql
- Test
- PendingEvent
- OutboxRelayTest
- EntryKind
- smoke.sh
- AccountOpened
- ClockWiringIT.kt
- AccountNotFoundException
- duration
- OutboxStore
- JdbcOutboxStore
- OutboxMetrics
- Banco digital em microsserviços
- .record
- Etapa 1 — `accounts`: design
- .record
- OutboxWriterConfiguration.kt
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
- `Task 15: Relay e publicador SNS (Marina, `dev-kotlin`)` --references--> `EventPublisher`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/EventPublisher.kt
- `Task 15: Relay e publicador SNS (Marina, `dev-kotlin`)` --references--> `JdbcOutboxStore`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/JdbcOutboxStore.kt

## Import Cycles
- None detected.

## Communities (87 total, 38 thin omitted)

### Community 0 - "LedgerEntryRecorded"
Cohesion: 0.14
Nodes (9): RecordCreditUseCase, DebitOutcome, Recorded, Refused, RecordEntryCommand, RecordEntryResult, RecordCreditUseCaseTest, RecordDebitUseCaseTest (+1 more)

### Community 1 - "IdGenerator"
Cohesion: 0.08
Nodes (8): 4. Fluxos, UuidV7IdGenerator, IdGenerator, ChangeAccountTypeUseCase, GetAccountUseCase, OpenAccountUseCase, RecordDebitUseCase, UseCaseConfiguration

### Community 4 - "AccountsIntegrationTest"
Cohesion: 0.09
Nodes (4): AccountsIntegrationTest, UseCaseConfigurationIT, AccountTypeAdminApiIT, UnexpectedErrorApiIT

### Community 5 - "0012 — Saldo no lançamento e trava pessimista por conta"
Cohesion: 0.40
Nodes (5): 0012 — Saldo no lançamento e trava pessimista por conta, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 6 - "LocalStackFifoIT.kt"
Cohesion: 0.06
Nodes (5): LocalStackFifoIT, ReceivedMessage, SnsEventPublisherIT, LocalStackTestSupport, LocalStackTestSupportTest

### Community 8 - "AccountEvent"
Cohesion: 0.33
Nodes (3): EventOutbox, RecordingEventOutbox, AccountEvent

### Community 10 - "Bloco 1 — `accounts` gravando no outbox"
Cohesion: 0.07
Nodes (12): Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 2: Valores monetários e tipos (Marina, `dev-kotlin`), Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`), Task 6: Casos de uso de tipo e limites (Marina, `dev-kotlin`), Task 7: `libs/outbox` — envelope e writer (Marie, `dev-kotlin`; em paralelo às Tasks 2–6) (+4 more)

### Community 11 - "ProblemDetailsExceptionHandler.kt"
Cohesion: 0.13
Nodes (6): ProblemDetailsExceptionHandler, AccountTypeNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException, BalanceOverflowException

### Community 12 - "CallLog"
Cohesion: 0.24
Nodes (4): CallLog, DirectTransactionRunner, ChangeAccountTypeUseCaseTest, AccountTypeChanged

### Community 13 - "AvailableBalance"
Cohesion: 0.29
Nodes (3): AccountView, GetAccountUseCaseTest, AvailableBalance

### Community 14 - "LedgerEntry"
Cohesion: 0.16
Nodes (4): JdbcLedgerEntryRepository, LedgerEntryRepository, InMemoryLedgerEntryRepository, LedgerEntry

### Community 17 - "OverdraftLimit"
Cohesion: 0.06
Nodes (21): 1. Módulos e camadas, 2. Modelo de domínio, JdbcAccountTypeRepository, AccountTypeAdminController, AccountTypeLimitResponse, AccountTypeRepository, ChangeOverdraftLimitUseCase, ListAccountTypesUseCase (+13 more)

### Community 18 - "Decisões tomadas"
Cohesion: 0.14
Nodes (14): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Em aberto, Fluxo de entrada (+6 more)

### Community 20 - "JdbcOutboxStoreIT"
Cohesion: 0.08
Nodes (3): JdbcOutboxStoreIT, JdbcOutboxWriterIT, OutboxMessage

### Community 21 - "AccountController.kt"
Cohesion: 0.08
Nodes (10): AccountController, ChangeAccountTypeRequest, ChangeOverdraftLimitRequest, OpenAccountRequest, RecordEntryRequest, AccountResponse, LedgerEntryResponse, IdempotencyKeyHeader (+2 more)

### Community 22 - "status.md"
Cohesion: 0.20
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

### Community 27 - "JdbcOutboxWriter"
Cohesion: 0.18
Nodes (8): Bloco 2 — relay e SNS FIFO, Task 14: Verificação do SNS FIFO no LocalStack (Rafael, `infra`), Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), 3. Esquema do banco, 5. Outbox e relay (`libs/outbox`), JdbcOutboxWriter, OutboxEventAppender

### Community 28 - "AccountsApplicationIT.kt"
Cohesion: 0.06
Nodes (29): Ambiente do Claude, Com o usuário (fora do código), Como rodar localmente, Concluído, Em andamento, Pendências e decisões em aberto, Próximos passos, Status do projeto (+21 more)

### Community 29 - "Rodando o projeto localmente (macOS)"
Cohesion: 0.18
Nodes (10): 1. Configurar o ambiente, 2. Subir a infraestrutura (PostgreSQL, Redis, nginx), 3. Build e testes, 4. Subir o serviço `accounts`, 5. Conferir a API pelo nginx, 6. Coleção Bruno, 7. Derrubar tudo, Opcional: grafo do código (graphify) (+2 more)

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
Cohesion: 0.10
Nodes (10): Task 8: Esquema, usuários de banco e repositórios JDBC (Marina, `dev-kotlin`; o script de init do compose segue o padrão do Rafael), JdbcAccountRepository, AccountBusyException, AccountRepository, InsertAccountResult, DUPLICATE_CUSTOMER, DUPLICATE_OPENING_KEY, INSERTED (+2 more)

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
Nodes (7): ConcurrencyIT, OpenApiIT, ApiClient, ApiResponse, isAccountBusy(), retryWhileAccountBusy(), RetryWhileAccountBusyTest

### Community 71 - "PendingEvent"
Cohesion: 0.16
Nodes (5): EventPublisher, OutboxRelay, OutboxRelaySettings, PendingEvent, OutboxEnvelopeTest

### Community 73 - "EntryKind"
Cohesion: 0.14
Nodes (9): Banco digital, Clientes e contas, Consulta e avisos, Ledger, Transferências, IdempotentEntryLookup, EntryKind, CREDIT (+1 more)

### Community 80 - "AccountOpened"
Cohesion: 0.28
Nodes (4): OpenAccountCommand, SequentialIdGenerator, OpenAccountUseCaseTest, AccountOpened

### Community 83 - "AccountNotFoundException"
Cohesion: 0.18
Nodes (7): Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`), Task 5: Casos de uso de crédito e débito (Marina, `dev-kotlin`), AccountNotFoundException, CustomerAlreadyHasAccountException, IdempotencyKeyReusedException, TransactionRunner, OpenAccountResult

### Community 85 - "duration"
Cohesion: 0.16
Nodes (3): OutboxBacklog, OutboxStore, OutboxMetricsTest

### Community 93 - "Banco digital em microsserviços"
Cohesion: 0.22
Nodes (8): Agentes do projeto, Ao terminar uma tarefa, Banco digital em microsserviços, Contexto (leia antes de qualquer tarefa), Fluxo de trabalho (skills), Git, Grafo do código (graphify), Regras inegociáveis

### Community 95 - ".record"
Cohesion: 0.08
Nodes (16): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 4: Ambiente local (docker-compose), Task 5: CI no GitHub Actions (+8 more)

### Community 97 - "Etapa 1 — `accounts`: design"
Cohesion: 0.14
Nodes (12): 10. Documentação que acompanha, 6. API, 7. Testes, 8. Ambiente local e CI, 9. Entrega em dois blocos, Critério de pronto, Etapa 1 — `accounts`: design, Fora de escopo (e onde fica registrado) (+4 more)

### Community 99 - ".record"
Cohesion: 0.33
Nodes (4): DebitResult, EntryRecorded, Recorded, Refused

### Community 106 - "0001 — Stack: Kotlin + Spring Boot 4 na AWS"
Cohesion: 0.40
Nodes (5): 0001 — Stack: Kotlin + Spring Boot 4 na AWS, Alternativas consideradas, Consequências, Contexto, Decisão

## Knowledge Gaps
- **10 isolated node(s):** `INSERTED`, `DUPLICATE_OPENING_KEY`, `DUPLICATE_CUSTOMER`, `FREE`, `STANDARD` (+5 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 276 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **38 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AccountsIntegrationTest` connect `AccountsIntegrationTest` to `ConcurrencyIT`, `AccountApiIT`, `Account`, `JdbcRepositoriesIT`, `uuid`, `ClockWiringIT.kt`, `JdbcOutboxWriter`, `AccountsApplicationIT.kt`?**
  _High betweenness centrality (0.106) - this node is a cross-community bridge._
- **Are the 8 inferred relationships involving `Account` (e.g. with `Regras inegociáveis` and `Clientes e contas`) actually correct?**
  _`Account` has 8 INFERRED edges - model-reasoned connections that need verification._
- **What connects `INSERTED`, `DUPLICATE_OPENING_KEY`, `DUPLICATE_CUSTOMER` to the rest of the system?**
  _10 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `LedgerEntryRecorded` be split into smaller, more focused modules?**
  _Cohesion score 0.1379800853485064 - nodes in this community are weakly interconnected._
- **Why does `AccountApiIT` connect `AccountApiIT` to `ConcurrencyIT`, `OutboxMetrics`, `AccountsIntegrationTest`?**
  _High betweenness centrality (0.076) - this node is a cross-community bridge._
- **Are the 10 inferred relationships involving `LedgerEntry` (e.g. with `Regras inegociáveis` and `Ledger`) actually correct?**
  _`LedgerEntry` has 10 INFERRED edges - model-reasoned connections that need verification._
- **Should `IdGenerator` be split into smaller, more focused modules?**
  _Cohesion score 0.08383838383838384 - nodes in this community are weakly interconnected._