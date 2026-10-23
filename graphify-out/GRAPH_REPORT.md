# Graph Report - digitalbanking  (2026-10-22)

## Corpus Check
- 135 files · ~37,100 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 10 file(s) not represented in the graph (top: (none) 3, .properties 2, .toml 1)

## Summary
- 1025 nodes · 2551 edges · 72 communities (42 shown, 30 thin omitted)
- Extraction: 90% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 255 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `c59e9bae`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- OverdraftLimit
- IdGenerator
- AccountApiIT
- Etapa 1 — `accounts`: design
- AccountsIntegrationTest
- OverdraftLimit
- AccountController.kt
- JdbcRepositoriesIT
- Foco de revisão
- RecordDebitUseCaseTest.kt
- Status do projeto
- ProblemDetailsExceptionHandler.kt
- LedgerEntryRecorded
- Account
- Bloco 1 — `accounts` gravando no outbox
- CallLog
- uuid
- AccountBusyException
- Decisões tomadas
- README.md
- AccountOpened
- .entry
- 0002 — Clean Architecture por serviço
- coordenador.md
- 0004 — API Gateway HTTP API na borda
- 0005 — Frontend Angular e BFF em NestJS
- 0006 — Observabilidade só com correlation ID
- SpringTransactionRunner
- CorrelationIdFilter.kt
- ArchitectureTest
- 0001 — Stack: Kotlin + Spring Boot 4 na AWS
- 0008 — Extrato (`statement`) com CQRS no DynamoDB
- 0009 — Cache do extrato
- 0010 — Limite diário com Redis + Lua
- 0013 — Persistência com Spring JDBC (`JdbcClient`)
- 0014 — Tipo de conta e cheque especial
- 0015 — Relay do outbox com líder único e SNS FIFO
- Banco digital em microsserviços
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
- ClockWiringIT.kt
- 0012 — Saldo no lançamento e trava pessimista por conta
- account
- ConcurrencyIT
- outbox-table.sql
- V4__outbox.sql
- .record
- AccountsApplicationIT
- AccountsApplication.kt
- .inTransaction

## God Nodes (most connected - your core abstractions)
1. `Account` - 66 edges
2. `AccountApiIT` - 57 edges
3. `LedgerEntry` - 54 edges
4. `OverdraftLimit` - 54 edges
5. `Money` - 53 edges
6. `Balance` - 47 edges
7. `AccountType` - 46 edges
8. `JdbcRepositoriesIT` - 45 edges
9. `assertProblem()` - 37 edges
10. `LedgerEntryRecorded` - 33 edges

## Surprising Connections (you probably didn't know these)
- `Task 9: API HTTP, Problem Details e correlation ID (Marina, `dev-kotlin`)` --references--> `CorrelationIdFilter`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → services/accounts/adapters/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/adapters/web/CorrelationIdFilter.kt
- `Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`)` --references--> `AccountsApplicationIT`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → services/accounts/bootstrap/src/integrationTest/kotlin/io/github/capozziv/digitalbanking/accounts/bootstrap/AccountsApplicationIT.kt
- `Task 13: Documentação do bloco 1 e das decisões da etapa (Clara, `documentador`; em paralelo às Tasks 8–12)` --references--> `main()`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → services/accounts/bootstrap/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/bootstrap/AccountsApplication.kt
- `Decisão` --references--> `LedgerEntry`  [INFERRED]
  docs/adr/0012-saldo-no-lancamento-e-trava-pessimista.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/LedgerEntry.kt
- `4. Fluxos` --references--> `JdbcOutboxWriter`  [INFERRED]
  docs/superpowers/specs/2026-10-22-etapa-1-accounts-design.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/JdbcOutboxWriter.kt

## Import Cycles
- None detected.

## Communities (72 total, 30 thin omitted)

### Community 0 - "OverdraftLimit"
Cohesion: 0.05
Nodes (21): Ledger, Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`), 2. Modelo de domínio, JdbcLedgerEntryRepository, LedgerEntryRepository, AccountView, InMemoryLedgerEntryRepository, GetAccountUseCaseTest (+13 more)

### Community 1 - "IdGenerator"
Cohesion: 0.06
Nodes (12): 1. Módulos e camadas, 4. Fluxos, UuidV7IdGenerator, IdGenerator, ChangeAccountTypeUseCase, ChangeOverdraftLimitUseCase, GetAccountUseCase, OpenAccountUseCase (+4 more)

### Community 3 - "Etapa 1 — `accounts`: design"
Cohesion: 0.06
Nodes (21): Bloco 2 — relay e SNS FIFO, Task 14: Verificação do SNS FIFO no LocalStack (Rafael, `infra`), Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), 10. Documentação que acompanha, 3. Esquema do banco, 5. Outbox e relay (`libs/outbox`), 6. API (+13 more)

### Community 4 - "AccountsIntegrationTest"
Cohesion: 0.09
Nodes (4): AccountsIntegrationTest, UseCaseConfigurationIT, AccountTypeAdminApiIT, UnexpectedErrorApiIT

### Community 5 - "OverdraftLimit"
Cohesion: 0.06
Nodes (19): Banco digital, Clientes e contas, Consulta e avisos, Transferências, JdbcAccountTypeRepository, AccountTypeRepository, ListAccountTypesUseCase, AccountTypeRepository (+11 more)

### Community 6 - "AccountController.kt"
Cohesion: 0.07
Nodes (11): AccountController, AccountTypeAdminController, ChangeAccountTypeRequest, ChangeOverdraftLimitRequest, OpenAccountRequest, RecordEntryRequest, AccountResponse, AccountTypeLimitResponse (+3 more)

### Community 8 - "Foco de revisão"
Cohesion: 0.17
Nodes (11): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 3: Aplicação `accounts` com health do banco (Testcontainers), Task 4: Ambiente local (docker-compose) (+3 more)

### Community 9 - "RecordDebitUseCaseTest.kt"
Cohesion: 0.17
Nodes (6): AccountNotFoundException, TransactionRunner, DirectTransactionRunner, EntryKind, CREDIT, DEBIT

### Community 10 - "Status do projeto"
Cohesion: 0.15
Nodes (17): Ambiente do Claude, Com o usuário (fora do código), Como rodar localmente, Concluído, Em andamento, Próximos passos, Status do projeto, 2. Build e convenções (+9 more)

### Community 11 - "ProblemDetailsExceptionHandler.kt"
Cohesion: 0.12
Nodes (7): IdempotencyKeyHeader, ProblemDetailsExceptionHandler, AccountTypeNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException, BalanceOverflowException

### Community 12 - "LedgerEntryRecorded"
Cohesion: 0.15
Nodes (9): DebitOutcome, Recorded, Refused, RecordEntryCommand, RecordEntryResult, SequentialIdGenerator, RecordCreditUseCaseTest, RecordDebitUseCaseTest (+1 more)

### Community 13 - "Account"
Cohesion: 0.11
Nodes (10): Regras inegociáveis, Restrições (de `CLAUDE.md` e ADRs), JdbcAccountRepository, AccountRepository, InsertAccountResult, DUPLICATE_CUSTOMER, DUPLICATE_OPENING_KEY, INSERTED (+2 more)

### Community 14 - "Bloco 1 — `accounts` gravando no outbox"
Cohesion: 0.14
Nodes (12): Bloco 1 — `accounts` gravando no outbox, Etapa 1 — `accounts` Implementation Plan, Global Constraints, Review Focus, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 13: Documentação do bloco 1 e das decisões da etapa (Clara, `documentador`; em paralelo às Tasks 8–12), Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`) (+4 more)

### Community 15 - "CallLog"
Cohesion: 0.16
Nodes (7): Task 6: Casos de uso de tipo e limites (Marina, `dev-kotlin`), EventOutbox, CallLog, RecordingEventOutbox, ChangeAccountTypeUseCaseTest, AccountEvent, AccountTypeChanged

### Community 17 - "AccountBusyException"
Cohesion: 0.21
Nodes (7): Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`), Task 5: Casos de uso de crédito e débito (Marina, `dev-kotlin`), Task 8: Esquema, usuários de banco e repositórios JDBC (Marina, `dev-kotlin`; o script de init do compose segue o padrão do Rafael), AccountBusyException, CustomerAlreadyHasAccountException, IdempotencyKeyReusedException, IdempotentEntryLookup

### Community 18 - "Decisões tomadas"
Cohesion: 0.14
Nodes (14): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Em aberto, Fluxo de entrada (+6 more)

### Community 20 - "AccountOpened"
Cohesion: 0.33
Nodes (4): OpenAccountCommand, OpenAccountResult, OpenAccountUseCaseTest, AccountOpened

### Community 22 - "0002 — Clean Architecture por serviço"
Cohesion: 0.40
Nodes (5): 0002 — Clean Architecture por serviço, Alternativas consideradas, Consequências, Contexto, Decisão

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

### Community 37 - "Banco digital em microsserviços"
Cohesion: 0.25
Nodes (7): Agentes do projeto, Ao terminar uma tarefa, Banco digital em microsserviços, Contexto (leia antes de qualquer tarefa), Fluxo de trabalho (skills), Git, Grafo do código (graphify)

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

### Community 62 - "0012 — Saldo no lançamento e trava pessimista por conta"
Cohesion: 0.40
Nodes (5): 0012 — Saldo no lançamento e trava pessimista por conta, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 63 - "account"
Cohesion: 0.33
Nodes (3): account_type, account, ledger_entry

### Community 64 - "ConcurrencyIT"
Cohesion: 0.08
Nodes (4): ConcurrencyIT, OpenApiIT, ApiClient, ApiResponse

### Community 71 - "AccountsApplicationIT"
Cohesion: 0.29
Nodes (3): Pendências e decisões em aberto, AccountsApplicationIT, ParameterizedTypeReference

### Community 74 - "AccountsApplication.kt"
Cohesion: 0.40
Nodes (3): 3. Esqueleto do `accounts`, Testes (no `bootstrap`, que enxerga todos os módulos), AccountsApplication

## Knowledge Gaps
- **10 isolated node(s):** `INSERTED`, `DUPLICATE_OPENING_KEY`, `DUPLICATE_CUSTOMER`, `FREE`, `STANDARD` (+5 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 239 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **30 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AccountsIntegrationTest` connect `AccountsIntegrationTest` to `ConcurrencyIT`, `AccountApiIT`, `Etapa 1 — `accounts`: design`, `AccountsApplicationIT`, `JdbcRepositoriesIT`, `uuid`, `AccountBusyException`, `ClockWiringIT.kt`?**
  _High betweenness centrality (0.114) - this node is a cross-community bridge._
- **Are the 7 inferred relationships involving `Account` (e.g. with `Regras inegociáveis` and `Clientes e contas`) actually correct?**
  _`Account` has 7 INFERRED edges - model-reasoned connections that need verification._
- **What connects `INSERTED`, `DUPLICATE_OPENING_KEY`, `DUPLICATE_CUSTOMER` to the rest of the system?**
  _10 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `OverdraftLimit` be split into smaller, more focused modules?**
  _Cohesion score 0.05306930693069307 - nodes in this community are weakly interconnected._
- **Why does `Etapa 1 — `accounts`: design` connect `Etapa 1 — `accounts`: design` to `OverdraftLimit`, `IdGenerator`, `README.md`, `Account`?**
  _High betweenness centrality (0.107) - this node is a cross-community bridge._
- **Are the 9 inferred relationships involving `LedgerEntry` (e.g. with `Regras inegociáveis` and `Ledger`) actually correct?**
  _`LedgerEntry` has 9 INFERRED edges - model-reasoned connections that need verification._
- **Should `IdGenerator` be split into smaller, more focused modules?**
  _Cohesion score 0.06253652834599649 - nodes in this community are weakly interconnected._