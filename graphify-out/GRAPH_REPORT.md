# Graph Report - digitalbanking  (2026-10-22)

## Corpus Check
- 132 files · ~34,703 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 10 file(s) not represented in the graph (top: (none) 3, .properties 2, .toml 1)

## Summary
- 969 nodes · 2401 edges · 69 communities (42 shown, 27 thin omitted)
- Extraction: 90% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 252 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `3e2aade9`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- OverdraftLimit
- IdGenerator
- AccountApiIT
- Etapa 1 — `accounts`: design
- AccountsIntegrationTest
- AccountType
- AccountController.kt
- JdbcRepositoriesIT
- CallLog
- LedgerEntry
- Etapa 0 — Fundação: design
- ProblemDetailsExceptionHandler.kt
- LedgerEntryRecorded
- Account
- Bloco 1 — `accounts` gravando no outbox
- RecordDebitUseCaseTest.kt
- CallLog
- Decisões tomadas
- README.md
- Money
- .entry
- 2026-10-21-etapa-0-fundacao-design.md
- coordenador.md
- 0004 — API Gateway HTTP API na borda
- 0005 — Frontend Angular e BFF em NestJS
- 0006 — Observabilidade só com correlation ID
- SpringTransactionRunner
- AvailableBalance
- InMemoryAccountTypeRepository
- 0001 — Stack: Kotlin + Spring Boot 4 na AWS
- 0008 — Extrato (`statement`) com CQRS no DynamoDB
- 0009 — Cache do extrato
- 0010 — Limite diário com Redis + Lua
- 0013 — Persistência com Spring JDBC (`JdbcClient`)
- 0014 — Tipo de conta e cheque especial
- 0015 — Relay do outbox com líder único e SNS FIFO
- DirectTransactionRunner
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
- Test
- .record
- 0012 — Saldo no lançamento e trava pessimista por conta
- account
- ClockWiringIT.kt
- outbox-table.sql
- V4__outbox.sql

## God Nodes (most connected - your core abstractions)
1. `Account` - 66 edges
2. `AccountApiIT` - 57 edges
3. `LedgerEntry` - 54 edges
4. `OverdraftLimit` - 54 edges
5. `Money` - 53 edges
6. `Balance` - 47 edges
7. `AccountType` - 46 edges
8. `JdbcRepositoriesIT` - 45 edges
9. `assertProblem()` - 34 edges
10. `LedgerEntryRecorded` - 33 edges

## Surprising Connections (you probably didn't know these)
- `Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`)` --references--> `AccountsApplicationIT`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → services/accounts/bootstrap/src/integrationTest/kotlin/io/github/capozziv/digitalbanking/accounts/bootstrap/AccountsApplicationIT.kt
- `Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`)` --references--> `LedgerEntryRecorded`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/AccountEvent.kt
- `Decisão` --references--> `LedgerEntry`  [INFERRED]
  docs/adr/0012-saldo-no-lancamento-e-trava-pessimista.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/LedgerEntry.kt
- `4. Fluxos` --references--> `JdbcOutboxWriter`  [INFERRED]
  docs/superpowers/specs/2026-10-22-etapa-1-accounts-design.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/JdbcOutboxWriter.kt
- `Task 8: Esquema, usuários de banco e repositórios JDBC (Marina, `dev-kotlin`; o script de init do compose segue o padrão do Rafael)` --references--> `OutboxMessage`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/OutboxMessage.kt

## Import Cycles
- None detected.

## Communities (69 total, 27 thin omitted)

### Community 0 - "OverdraftLimit"
Cohesion: 0.21
Nodes (3): Balance, OverdraftLimit, BalanceTest

### Community 1 - "IdGenerator"
Cohesion: 0.08
Nodes (11): 1. Módulos e camadas, 4. Fluxos, UuidV7IdGenerator, IdGenerator, ChangeAccountTypeUseCase, ChangeOverdraftLimitUseCase, GetAccountUseCase, ListAccountTypesUseCase (+3 more)

### Community 2 - "AccountApiIT"
Cohesion: 0.08
Nodes (4): AccountApiIT, ApiClient, ApiResponse, assertProblem()

### Community 3 - "Etapa 1 — `accounts`: design"
Cohesion: 0.06
Nodes (22): Bloco 2 — relay e SNS FIFO, Task 14: Verificação do SNS FIFO no LocalStack (Rafael, `infra`), Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), 10. Documentação que acompanha, 3. Esquema do banco, 5. Outbox e relay (`libs/outbox`), 6. API (+14 more)

### Community 4 - "AccountsIntegrationTest"
Cohesion: 0.09
Nodes (4): AccountsIntegrationTest, UseCaseConfigurationIT, AccountTypeAdminApiIT, UnexpectedErrorApiIT

### Community 5 - "AccountType"
Cohesion: 0.14
Nodes (9): JdbcAccountTypeRepository, AccountTypeRepository, AccountType, BLACK, DIAMOND, FREE, GOLD, STANDARD (+1 more)

### Community 6 - "AccountController.kt"
Cohesion: 0.09
Nodes (9): AccountController, AccountTypeAdminController, ChangeAccountTypeRequest, ChangeOverdraftLimitRequest, OpenAccountRequest, RecordEntryRequest, AccountResponse, AccountTypeLimitResponse (+1 more)

### Community 8 - "CallLog"
Cohesion: 0.07
Nodes (17): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 4: Ambiente local (docker-compose), Task 5: CI no GitHub Actions (+9 more)

### Community 9 - "LedgerEntry"
Cohesion: 0.14
Nodes (4): JdbcLedgerEntryRepository, LedgerEntryRepository, InMemoryLedgerEntryRepository, LedgerEntry

### Community 10 - "Etapa 0 — Fundação: design"
Cohesion: 0.08
Nodes (26): Ambiente do Claude, Com o usuário (fora do código), Como rodar localmente, Concluído, Em andamento, Pendências e decisões em aberto, Próximos passos, Status do projeto (+18 more)

### Community 11 - "ProblemDetailsExceptionHandler.kt"
Cohesion: 0.12
Nodes (7): IdempotencyKeyHeader, ProblemDetailsExceptionHandler, AccountTypeNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException, BalanceOverflowException

### Community 12 - "LedgerEntryRecorded"
Cohesion: 0.13
Nodes (10): RecordCreditUseCase, DebitOutcome, Recorded, Refused, RecordEntryCommand, RecordEntryResult, SequentialIdGenerator, RecordCreditUseCaseTest (+2 more)

### Community 13 - "Account"
Cohesion: 0.05
Nodes (26): Agentes do projeto, Ao terminar uma tarefa, Banco digital em microsserviços, Contexto (leia antes de qualquer tarefa), Fluxo de trabalho (skills), Git, Grafo do código (graphify), Regras inegociáveis (+18 more)

### Community 14 - "Bloco 1 — `accounts` gravando no outbox"
Cohesion: 0.09
Nodes (12): Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`), Task 2: Valores monetários e tipos (Marina, `dev-kotlin`), Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`), Task 7: `libs/outbox` — envelope e writer (Marie, `dev-kotlin`; em paralelo às Tasks 2–6) (+4 more)

### Community 15 - "RecordDebitUseCaseTest.kt"
Cohesion: 0.17
Nodes (10): Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`), Task 5: Casos de uso de crédito e débito (Marina, `dev-kotlin`), AccountBusyException, AccountNotFoundException, CustomerAlreadyHasAccountException, IdempotencyKeyReusedException, IdempotentEntryLookup, EntryKind (+2 more)

### Community 16 - "CallLog"
Cohesion: 0.16
Nodes (7): Task 6: Casos de uso de tipo e limites (Marina, `dev-kotlin`), EventOutbox, CallLog, RecordingEventOutbox, ChangeAccountTypeUseCaseTest, AccountEvent, AccountTypeChanged

### Community 18 - "Decisões tomadas"
Cohesion: 0.14
Nodes (14): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Em aberto, Fluxo de entrada (+6 more)

### Community 22 - "2026-10-21-etapa-0-fundacao-design.md"
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

### Community 28 - "AvailableBalance"
Cohesion: 0.18
Nodes (6): Ledger, 2. Modelo de domínio, AccountView, GetAccountUseCaseTest, AvailableBalance, currentBalance()

### Community 29 - "InMemoryAccountTypeRepository"
Cohesion: 0.21
Nodes (3): InMemoryAccountTypeRepository, ListAccountTypesUseCaseTest, AccountTypeRepository

### Community 30 - "0001 — Stack: Kotlin + Spring Boot 4 na AWS"
Cohesion: 0.40
Nodes (5): 0001 — Stack: Kotlin + Spring Boot 4 na AWS, Alternativas consideradas, Consequências, Contexto, Decisão

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

### Community 37 - "DirectTransactionRunner"
Cohesion: 0.21
Nodes (3): TransactionRunner, DirectTransactionRunner, ChangeOverdraftLimitUseCaseTest

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

### Community 61 - ".record"
Cohesion: 0.33
Nodes (4): DebitResult, EntryRecorded, Recorded, Refused

### Community 62 - "0012 — Saldo no lançamento e trava pessimista por conta"
Cohesion: 0.40
Nodes (5): 0012 — Saldo no lançamento e trava pessimista por conta, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 63 - "account"
Cohesion: 0.33
Nodes (3): account_type, account, ledger_entry

## Knowledge Gaps
- **10 isolated node(s):** `INSERTED`, `DUPLICATE_OPENING_KEY`, `DUPLICATE_CUSTOMER`, `FREE`, `STANDARD` (+5 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 232 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **27 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AccountsIntegrationTest` connect `AccountsIntegrationTest` to `ClockWiringIT.kt`, `AccountApiIT`, `Etapa 1 — `accounts`: design`, `JdbcRepositoriesIT`, `Etapa 0 — Fundação: design`, `Account`, `uuid`?**
  _High betweenness centrality (0.119) - this node is a cross-community bridge._
- **Are the 7 inferred relationships involving `Account` (e.g. with `Regras inegociáveis` and `Clientes e contas`) actually correct?**
  _`Account` has 7 INFERRED edges - model-reasoned connections that need verification._
- **What connects `INSERTED`, `DUPLICATE_OPENING_KEY`, `DUPLICATE_CUSTOMER` to the rest of the system?**
  _10 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `IdGenerator` be split into smaller, more focused modules?**
  _Cohesion score 0.07607843137254902 - nodes in this community are weakly interconnected._
- **Why does `Etapa 1 — `accounts`: design` connect `Etapa 1 — `accounts`: design` to `IdGenerator`, `README.md`, `AvailableBalance`?**
  _High betweenness centrality (0.114) - this node is a cross-community bridge._
- **Are the 9 inferred relationships involving `LedgerEntry` (e.g. with `Regras inegociáveis` and `Ledger`) actually correct?**
  _`LedgerEntry` has 9 INFERRED edges - model-reasoned connections that need verification._
- **Should `AccountApiIT` be split into smaller, more focused modules?**
  _Cohesion score 0.08140350877192983 - nodes in this community are weakly interconnected._