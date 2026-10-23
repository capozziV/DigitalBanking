# Graph Report - digitalbanking  (2026-10-22)

## Corpus Check
- 140 files · ~38,788 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 18 file(s) not represented in the graph (top: .bru 8, (none) 3, .properties 2)

## Summary
- 1073 nodes · 2630 edges · 90 communities (49 shown, 41 thin omitted)
- Extraction: 90% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 262 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `00d1ae55`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- LedgerEntry
- IdGenerator
- AccountApiIT
- .outbox
- AccountsIntegrationTest
- OverdraftLimit
- AccountTypeAdminController.kt
- JdbcRepositoriesIT
- ArchitectureTest
- AvailableBalance
- Status do projeto
- ProblemDetailsExceptionHandler.kt
- LedgerEntryRecorded
- Account
- Balance
- AccountNotFoundException
- Money
- CallLog
- Decisões tomadas
- README.md
- Etapa 1 — `accounts`: design
- AccountController.kt
- 0002 — Clean Architecture por serviço
- coordenador.md
- 0004 — API Gateway HTTP API na borda
- 0005 — Frontend Angular e BFF em NestJS
- 0006 — Observabilidade só com correlation ID
- DirectTransactionRunner
- AccountsApplicationIT.kt
- Rodando o projeto localmente (macOS)
- 0001 — Stack: Kotlin + Spring Boot 4 na AWS
- 0008 — Extrato (`statement`) com CQRS no DynamoDB
- 0009 — Cache do extrato
- 0010 — Limite diário com Redis + Lua
- 0013 — Persistência com Spring JDBC (`JdbcClient`)
- 0014 — Tipo de conta e cheque especial
- 0015 — Relay do outbox com líder único e SNS FIFO
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
- AccountOpened
- .entry
- InMemoryAccountRepositoryTest
- account
- ConcurrencyIT
- outbox-table.sql
- V4__outbox.sql
- AccountTypeChanged
- Test
- AccountEvent
- Foco de revisão
- .record
- .record
- smoke.sh
- .accounts
- Etapa 0 — Fundação: design
- AccountsApplication.kt
- Banco digital
- 0012 — Saldo no lançamento e trava pessimista por conta
- .inTransaction
- AccountTypeRepository
- .findAll

## God Nodes (most connected - your core abstractions)
1. `Account` - 66 edges
2. `AccountApiIT` - 57 edges
3. `LedgerEntry` - 54 edges
4. `OverdraftLimit` - 54 edges
5. `Money` - 53 edges
6. `Balance` - 47 edges
7. `AccountType` - 46 edges
8. `JdbcRepositoriesIT` - 45 edges
9. `assertProblem()` - 36 edges
10. `LedgerEntryRecorded` - 33 edges

## Surprising Connections (you probably didn't know these)
- `Decisão` --references--> `LedgerEntry`  [INFERRED]
  docs/adr/0012-saldo-no-lancamento-e-trava-pessimista.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/LedgerEntry.kt
- `Task 9: API HTTP, Problem Details e correlation ID (Marina, `dev-kotlin`)` --references--> `CorrelationIdFilter`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → services/accounts/adapters/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/adapters/web/CorrelationIdFilter.kt
- `Pendências e decisões em aberto` --references--> `AccountsApplicationIT`  [INFERRED]
  docs/status.md → services/accounts/bootstrap/src/integrationTest/kotlin/io/github/capozziv/digitalbanking/accounts/bootstrap/AccountsApplicationIT.kt
- `Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`)` --references--> `ConcurrencyIT`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → services/accounts/bootstrap/src/integrationTest/kotlin/io/github/capozziv/digitalbanking/accounts/bootstrap/ConcurrencyIT.kt
- `Regras inegociáveis` --references--> `LedgerEntry`  [INFERRED]
  CLAUDE.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/LedgerEntry.kt

## Import Cycles
- None detected.

## Communities (90 total, 41 thin omitted)

### Community 0 - "LedgerEntry"
Cohesion: 0.10
Nodes (6): JdbcLedgerEntryRepository, AccountNotFoundException, LedgerEntryRepository, InMemoryLedgerEntryRepository, currentBalance(), LedgerEntry

### Community 1 - "IdGenerator"
Cohesion: 0.11
Nodes (6): 4. Fluxos, UuidV7IdGenerator, IdGenerator, GetAccountUseCase, RecordDebitUseCase, UseCaseConfiguration

### Community 4 - "AccountsIntegrationTest"
Cohesion: 0.09
Nodes (4): AccountsIntegrationTest, ClockWiringIT, UseCaseConfigurationIT, AccountTypeAdminApiIT

### Community 5 - "OverdraftLimit"
Cohesion: 0.09
Nodes (13): JdbcAccountTypeRepository, AccountTypeRepository, InMemoryAccountTypeRepository, ChangeOverdraftLimitUseCaseTest, ListAccountTypesUseCaseTest, AccountType, BLACK, DIAMOND (+5 more)

### Community 6 - "AccountTypeAdminController.kt"
Cohesion: 0.08
Nodes (7): AccountTypeAdminController, AccountTypeLimitResponse, ApiDocs, OpenApiConfiguration, ChangeOverdraftLimitUseCase, ListAccountTypesUseCase, UnexpectedErrorApiIT

### Community 9 - "AvailableBalance"
Cohesion: 0.16
Nodes (9): Ledger, 10. Documentação que acompanha, 2. Modelo de domínio, AccountView, GetAccountUseCaseTest, AvailableBalance, EntryKind, CREDIT (+1 more)

### Community 10 - "Status do projeto"
Cohesion: 0.21
Nodes (12): Ambiente do Claude, Com o usuário (fora do código), Como rodar localmente, Concluído, Em andamento, Pendências e decisões em aberto, Próximos passos, Status do projeto (+4 more)

### Community 11 - "ProblemDetailsExceptionHandler.kt"
Cohesion: 0.11
Nodes (7): IdempotencyKeyHeader, ProblemDetailsExceptionHandler, AccountTypeNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException, BalanceOverflowException

### Community 12 - "LedgerEntryRecorded"
Cohesion: 0.13
Nodes (10): RecordCreditUseCase, DebitOutcome, Recorded, Refused, RecordEntryCommand, RecordEntryResult, SequentialIdGenerator, RecordCreditUseCaseTest (+2 more)

### Community 13 - "Account"
Cohesion: 0.15
Nodes (7): AccountRepository, InsertAccountResult, DUPLICATE_CUSTOMER, DUPLICATE_OPENING_KEY, INSERTED, InMemoryAccountRepository, Account

### Community 15 - "AccountNotFoundException"
Cohesion: 0.27
Nodes (6): Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`), Task 5: Casos de uso de crédito e débito (Marina, `dev-kotlin`), AccountBusyException, CustomerAlreadyHasAccountException, IdempotencyKeyReusedException, IdempotentEntryLookup

### Community 17 - "CallLog"
Cohesion: 0.23
Nodes (4): 1. Módulos e camadas, ChangeAccountTypeUseCase, CallLog, ChangeAccountTypeUseCaseTest

### Community 18 - "Decisões tomadas"
Cohesion: 0.14
Nodes (14): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Em aberto, Fluxo de entrada (+6 more)

### Community 20 - "Etapa 1 — `accounts`: design"
Cohesion: 0.06
Nodes (21): Bloco 2 — relay e SNS FIFO, Task 14: Verificação do SNS FIFO no LocalStack (Rafael, `infra`), Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), Task 8: Esquema, usuários de banco e repositórios JDBC (Marina, `dev-kotlin`; o script de init do compose segue o padrão do Rafael), 3. Esquema do banco, 5. Outbox e relay (`libs/outbox`), 6. API (+13 more)

### Community 21 - "AccountController.kt"
Cohesion: 0.08
Nodes (17): Agentes do projeto, Ao terminar uma tarefa, Banco digital em microsserviços, Contexto (leia antes de qualquer tarefa), Fluxo de trabalho (skills), Git, Grafo do código (graphify), Regras inegociáveis (+9 more)

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

### Community 27 - "DirectTransactionRunner"
Cohesion: 0.16
Nodes (3): SpringTransactionRunner, TransactionRunner, DirectTransactionRunner

### Community 28 - "AccountsApplicationIT.kt"
Cohesion: 0.11
Nodes (8): Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`), Task 7: `libs/outbox` — envelope e writer (Marie, `dev-kotlin`; em paralelo às Tasks 2–6), OutboxEnvelope, CorrelationIdFilter, AccountsApplicationIT, ParameterizedTypeReference, RequestThreadProbe, RequestThreadProbeConfiguration

### Community 29 - "Rodando o projeto localmente (macOS)"
Cohesion: 0.18
Nodes (10): 1. Configurar o ambiente, 2. Subir a infraestrutura (PostgreSQL, Redis, nginx), 3. Build e testes, 4. Subir o serviço `accounts`, 5. Conferir a API pelo nginx, 6. Coleção Bruno, 7. Derrubar tudo, Opcional: grafo do código (graphify) (+2 more)

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

### Community 60 - "AccountOpened"
Cohesion: 0.38
Nodes (4): OpenAccountCommand, OpenAccountResult, OpenAccountUseCaseTest, AccountOpened

### Community 63 - "account"
Cohesion: 0.33
Nodes (3): account_type, account, ledger_entry

### Community 64 - "ConcurrencyIT"
Cohesion: 0.06
Nodes (7): ConcurrencyIT, OpenApiIT, ApiClient, ApiResponse, isAccountBusy(), retryWhileAccountBusy(), RetryWhileAccountBusyTest

### Community 69 - "AccountTypeChanged"
Cohesion: 0.20
Nodes (8): Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 2: Valores monetários e tipos (Marina, `dev-kotlin`), Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`), Task 6: Casos de uso de tipo e limites (Marina, `dev-kotlin`), Task 9: API HTTP, Problem Details e correlation ID (Marina, `dev-kotlin`), AccountTypeChanged

### Community 71 - "AccountEvent"
Cohesion: 0.33
Nodes (3): EventOutbox, RecordingEventOutbox, AccountEvent

### Community 72 - "Foco de revisão"
Cohesion: 0.22
Nodes (9): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 4: Ambiente local (docker-compose), Task 5: CI no GitHub Actions (+1 more)

### Community 74 - ".record"
Cohesion: 0.33
Nodes (4): DebitResult, EntryRecorded, Recorded, Refused

### Community 77 - ".accounts"
Cohesion: 0.29
Nodes (4): Etapa 1 — `accounts` Implementation Plan, Global Constraints, Review Focus, AccountRepository

### Community 78 - "Etapa 0 — Fundação: design"
Cohesion: 0.25
Nodes (8): 1. Estrutura do monorepo, 2. Build e convenções, 4. Ambiente local, Critério de pronto, Etapa 0 — Fundação: design, Objetivo, Restrições (de `CLAUDE.md` e ADRs), Riscos

### Community 80 - "AccountsApplication.kt"
Cohesion: 0.33
Nodes (4): Task 3: Aplicação `accounts` com health do banco (Testcontainers), 3. Esqueleto do `accounts`, Testes (no `bootstrap`, que enxerga todos os módulos), AccountsApplication

### Community 81 - "Banco digital"
Cohesion: 0.40
Nodes (4): Banco digital, Clientes e contas, Consulta e avisos, Transferências

### Community 82 - "0012 — Saldo no lançamento e trava pessimista por conta"
Cohesion: 0.40
Nodes (5): 0012 — Saldo no lançamento e trava pessimista por conta, Alternativas consideradas, Consequências, Contexto, Decisão

## Knowledge Gaps
- **10 isolated node(s):** `DUPLICATE_CUSTOMER`, `DUPLICATE_OPENING_KEY`, `INSERTED`, `BLACK`, `DIAMOND` (+5 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 261 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **41 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Etapa 1 — `accounts`: design` connect `Etapa 1 — `accounts`: design` to `IdGenerator`, `AvailableBalance`, `CallLog`, `README.md`, `AccountController.kt`?**
  _High betweenness centrality (0.108) - this node is a cross-community bridge._
- **Are the 7 inferred relationships involving `Account` (e.g. with `Regras inegociáveis` and `Clientes e contas`) actually correct?**
  _`Account` has 7 INFERRED edges - model-reasoned connections that need verification._
- **What connects `DUPLICATE_CUSTOMER`, `DUPLICATE_OPENING_KEY`, `INSERTED` to the rest of the system?**
  _10 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `LedgerEntry` be split into smaller, more focused modules?**
  _Cohesion score 0.10256410256410256 - nodes in this community are weakly interconnected._
- **Why does `AccountsIntegrationTest` connect `AccountsIntegrationTest` to `ConcurrencyIT`, `AccountApiIT`, `AccountTypeAdminController.kt`, `JdbcRepositoriesIT`, `JdbcRepositoriesIT.kt`, `Etapa 1 — `accounts`: design`, `AccountsApplicationIT.kt`?**
  _High betweenness centrality (0.107) - this node is a cross-community bridge._
- **Are the 9 inferred relationships involving `LedgerEntry` (e.g. with `Regras inegociáveis` and `Ledger`) actually correct?**
  _`LedgerEntry` has 9 INFERRED edges - model-reasoned connections that need verification._
- **Should `IdGenerator` be split into smaller, more focused modules?**
  _Cohesion score 0.10952380952380952 - nodes in this community are weakly interconnected._