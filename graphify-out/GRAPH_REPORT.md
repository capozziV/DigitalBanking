# Graph Report - digitalbanking  (2026-10-22)

## Corpus Check
- 132 files · ~34,703 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 10 file(s) not represented in the graph (top: (none) 3, .properties 2, .toml 1)

## Summary
- 980 nodes · 2401 edges · 90 communities (49 shown, 41 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 254 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `7f9609a3`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- OverdraftLimit
- IdGenerator
- AccountApiIT
- OutboxEventAppender.kt
- AccountsIntegrationTest
- OverdraftLimit
- AccountController.kt
- JdbcRepositoriesIT
- Foco de revisão
- LedgerEntry
- Status do projeto
- ProblemDetailsExceptionHandler.kt
- LedgerEntryRecorded
- Account
- Bloco 1 — `accounts` gravando no outbox
- RecordDebitUseCaseTest.kt
- CallLog
- uuid
- Decisões tomadas
- README.md
- Money
- assertthat
- 2026-10-21-etapa-0-fundacao-design.md
- coordenador.md
- 0004 — API Gateway HTTP API na borda
- 0005 — Frontend Angular e BFF em NestJS
- 0006 — Observabilidade só com correlation ID
- SpringTransactionRunner
- AvailableBalance
- RecordDebitUseCase.kt
- 0001 — Stack: Kotlin + Spring Boot 4 na AWS
- 0008 — Extrato (`statement`) com CQRS no DynamoDB
- 0009 — Cache do extrato
- 0010 — Limite diário com Redis + Lua
- 0013 — Persistência com Spring JDBC (`JdbcClient`)
- 0014 — Tipo de conta e cheque especial
- 0015 — Relay do outbox com líder único e SNS FIFO
- AccountOpened
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
- JdbcOutboxWriterIT.kt
- .record
- 0012 — Saldo no lançamento e trava pessimista por conta
- account
- ClockWiringIT.kt
- outbox-table.sql
- V4__outbox.sql
- EntryKind
- .record
- AccountsApplicationIT
- Etapa 1 — `accounts`: design
- 1. Módulos e camadas
- AccountsApplication.kt
- Task 8: Esquema, usuários de banco e repositórios JDBC (Marina, `dev-kotlin`; o script de init do compose segue o padrão do Rafael)
- Etapa 0 — Fundação: design
- assertthatthrownby
- JdbcOutboxWriter
- OutboxWriterConfiguration.kt
- .inTransaction

## God Nodes (most connected - your core abstractions)
1. `Account` - 66 edges
2. `AccountApiIT` - 57 edges
3. `OverdraftLimit` - 54 edges
4. `LedgerEntry` - 54 edges
5. `Money` - 53 edges
6. `Balance` - 47 edges
7. `AccountType` - 46 edges
8. `JdbcRepositoriesIT` - 45 edges
9. `assertProblem()` - 34 edges
10. `LedgerEntryRecorded` - 33 edges

## Surprising Connections (you probably didn't know these)
- `Pendências e decisões em aberto` --references--> `AccountsApplicationIT`  [INFERRED]
  docs/status.md → services/accounts/bootstrap/src/integrationTest/kotlin/io/github/capozziv/digitalbanking/accounts/bootstrap/AccountsApplicationIT.kt
- `Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`)` --references--> `LedgerEntryRecorded`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/AccountEvent.kt
- `Decisão` --references--> `LedgerEntry`  [INFERRED]
  docs/adr/0012-saldo-no-lancamento-e-trava-pessimista.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/LedgerEntry.kt
- `Task 9: API HTTP, Problem Details e correlation ID (Marina, `dev-kotlin`)` --references--> `CorrelationIdFilter`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → services/accounts/adapters/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/adapters/web/CorrelationIdFilter.kt
- `Ledger` --references--> `AvailableBalance`  [INFERRED]
  CONTEXT.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/AvailableBalance.kt

## Import Cycles
- None detected.

## Communities (90 total, 41 thin omitted)

### Community 1 - "IdGenerator"
Cohesion: 0.14
Nodes (4): UuidV7IdGenerator, IdGenerator, GetAccountUseCase, UseCaseConfiguration

### Community 2 - "AccountApiIT"
Cohesion: 0.08
Nodes (4): AccountApiIT, ApiClient, ApiResponse, assertProblem()

### Community 3 - "OutboxEventAppender.kt"
Cohesion: 0.20
Nodes (7): Bloco 2 — relay e SNS FIFO, Task 14: Verificação do SNS FIFO no LocalStack (Rafael, `infra`), Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), 3. Esquema do banco, 5. Outbox e relay (`libs/outbox`), OutboxEventAppender

### Community 4 - "AccountsIntegrationTest"
Cohesion: 0.11
Nodes (3): AccountsIntegrationTest, AccountTypeAdminApiIT, UnexpectedErrorApiIT

### Community 5 - "OverdraftLimit"
Cohesion: 0.06
Nodes (20): 4. Fluxos, JdbcAccountTypeRepository, AccountTypeAdminController, AccountTypeLimitResponse, AccountTypeRepository, ChangeOverdraftLimitUseCase, ListAccountTypesUseCase, AccountTypeRepository (+12 more)

### Community 6 - "AccountController.kt"
Cohesion: 0.08
Nodes (17): Agentes do projeto, Ao terminar uma tarefa, Banco digital em microsserviços, Contexto (leia antes de qualquer tarefa), Fluxo de trabalho (skills), Git, Grafo do código (graphify), Regras inegociáveis (+9 more)

### Community 8 - "Foco de revisão"
Cohesion: 0.18
Nodes (10): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 4: Ambiente local (docker-compose), Task 5: CI no GitHub Actions (+2 more)

### Community 9 - "LedgerEntry"
Cohesion: 0.13
Nodes (5): JdbcLedgerEntryRepository, LedgerEntryRepository, InMemoryLedgerEntryRepository, currentBalance(), LedgerEntry

### Community 10 - "Status do projeto"
Cohesion: 0.21
Nodes (12): Ambiente do Claude, Com o usuário (fora do código), Como rodar localmente, Concluído, Em andamento, Pendências e decisões em aberto, Próximos passos, Status do projeto (+4 more)

### Community 11 - "ProblemDetailsExceptionHandler.kt"
Cohesion: 0.11
Nodes (7): IdempotencyKeyHeader, ProblemDetailsExceptionHandler, AccountTypeNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException, BalanceOverflowException

### Community 12 - "LedgerEntryRecorded"
Cohesion: 0.10
Nodes (11): RecordCreditUseCase, DebitOutcome, RecordDebitUseCase, Recorded, Refused, RecordEntryCommand, RecordEntryResult, SequentialIdGenerator (+3 more)

### Community 13 - "Account"
Cohesion: 0.16
Nodes (3): JdbcAccountRepository, InMemoryAccountRepository, Account

### Community 14 - "Bloco 1 — `accounts` gravando no outbox"
Cohesion: 0.08
Nodes (12): Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 2: Valores monetários e tipos (Marina, `dev-kotlin`), Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`), Task 6: Casos de uso de tipo e limites (Marina, `dev-kotlin`), Task 7: `libs/outbox` — envelope e writer (Marie, `dev-kotlin`; em paralelo às Tasks 2–6) (+4 more)

### Community 15 - "RecordDebitUseCaseTest.kt"
Cohesion: 0.28
Nodes (7): Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`), Task 5: Casos de uso de crédito e débito (Marina, `dev-kotlin`), AccountBusyException, AccountNotFoundException, CustomerAlreadyHasAccountException, IdempotencyKeyReusedException, IdempotentEntryLookup

### Community 16 - "CallLog"
Cohesion: 0.16
Nodes (4): ChangeAccountTypeUseCase, CallLog, ChangeAccountTypeUseCaseTest, AccountTypeChanged

### Community 17 - "uuid"
Cohesion: 0.12
Nodes (5): AccountRepository, InsertAccountResult, DUPLICATE_CUSTOMER, DUPLICATE_OPENING_KEY, INSERTED

### Community 18 - "Decisões tomadas"
Cohesion: 0.14
Nodes (14): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Em aberto, Fluxo de entrada (+6 more)

### Community 21 - "assertthat"
Cohesion: 0.14
Nodes (3): UseCaseConfigurationIT, LedgerEntryTest, MoneyTest

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
Cohesion: 0.29
Nodes (3): AccountView, GetAccountUseCaseTest, AvailableBalance

### Community 29 - "RecordDebitUseCase.kt"
Cohesion: 0.21
Nodes (4): EventOutbox, TransactionRunner, RecordingEventOutbox, AccountEvent

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

### Community 37 - "AccountOpened"
Cohesion: 0.24
Nodes (5): OpenAccountCommand, OpenAccountResult, DirectTransactionRunner, OpenAccountUseCaseTest, AccountOpened

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

### Community 69 - "EntryKind"
Cohesion: 0.18
Nodes (9): Banco digital, Clientes e contas, Consulta e avisos, Ledger, Transferências, 10. Documentação que acompanha, EntryKind, CREDIT (+1 more)

### Community 71 - "AccountsApplicationIT"
Cohesion: 0.33
Nodes (3): Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`), AccountsApplicationIT, ParameterizedTypeReference

### Community 72 - "Etapa 1 — `accounts`: design"
Cohesion: 0.20
Nodes (10): 2. Modelo de domínio, 6. API, 7. Testes, 8. Ambiente local e CI, 9. Entrega em dois blocos, Critério de pronto, Etapa 1 — `accounts`: design, Fora de escopo (e onde fica registrado) (+2 more)

### Community 73 - "1. Módulos e camadas"
Cohesion: 0.25
Nodes (5): Etapa 1 — `accounts` Implementation Plan, Global Constraints, Review Focus, 1. Módulos e camadas, EventOutbox

### Community 74 - "AccountsApplication.kt"
Cohesion: 0.33
Nodes (4): Task 3: Aplicação `accounts` com health do banco (Testcontainers), 3. Esqueleto do `accounts`, Testes (no `bootstrap`, que enxerga todos os módulos), AccountsApplication

### Community 76 - "Etapa 0 — Fundação: design"
Cohesion: 0.29
Nodes (7): 2. Build e convenções, 4. Ambiente local, Critério de pronto, Etapa 0 — Fundação: design, Objetivo, Restrições (de `CLAUDE.md` e ADRs), Riscos

## Knowledge Gaps
- **10 isolated node(s):** `DUPLICATE_CUSTOMER`, `DUPLICATE_OPENING_KEY`, `INSERTED`, `CREDIT`, `DEBIT` (+5 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 242 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **41 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AccountsIntegrationTest` connect `AccountsIntegrationTest` to `ClockWiringIT.kt`, `AccountApiIT`, `OutboxEventAppender.kt`, `AccountsApplicationIT`, `JdbcRepositoriesIT`, `Task 8: Esquema, usuários de banco e repositórios JDBC (Marina, `dev-kotlin`; o script de init do compose segue o padrão do Rafael)`, `uuid`, `assertthat`?**
  _High betweenness centrality (0.117) - this node is a cross-community bridge._
- **Are the 7 inferred relationships involving `Account` (e.g. with `Regras inegociáveis` and `Clientes e contas`) actually correct?**
  _`Account` has 7 INFERRED edges - model-reasoned connections that need verification._
- **What connects `DUPLICATE_CUSTOMER`, `DUPLICATE_OPENING_KEY`, `INSERTED` to the rest of the system?**
  _10 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `IdGenerator` be split into smaller, more focused modules?**
  _Cohesion score 0.13793103448275862 - nodes in this community are weakly interconnected._
- **Why does `Etapa 1 — `accounts`: design` connect `Etapa 1 — `accounts`: design` to `OutboxEventAppender.kt`, `OverdraftLimit`, `EntryKind`, `AccountController.kt`, `1. Módulos e camadas`, `README.md`?**
  _High betweenness centrality (0.112) - this node is a cross-community bridge._
- **Are the 14 inferred relationships involving `OverdraftLimit` (e.g. with `Ledger` and `2. Modelo de domínio`) actually correct?**
  _`OverdraftLimit` has 14 INFERRED edges - model-reasoned connections that need verification._
- **Should `AccountApiIT` be split into smaller, more focused modules?**
  _Cohesion score 0.08140350877192983 - nodes in this community are weakly interconnected._