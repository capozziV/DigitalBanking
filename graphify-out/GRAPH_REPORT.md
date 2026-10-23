# Graph Report - digitalbanking  (2026-10-23)

## Corpus Check
- 140 files · ~39,651 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 18 file(s) not represented in the graph (top: .bru 8, (none) 3, .properties 2)

## Summary
- 1084 nodes · 2666 edges · 78 communities (44 shown, 34 thin omitted)
- Extraction: 90% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 268 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `88e461ed`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- LedgerEntry
- IdGenerator
- AccountApiIT
- .record
- AccountsIntegrationTest
- OverdraftLimit
- .findLimit
- JdbcRepositoriesIT
- InMemoryAccountTypeRepository
- Etapa 0 — Fundação: design
- CorrelationIdFilter.kt
- ProblemDetailsExceptionHandler.kt
- JdbcOutboxWriter
- Bloco 1 — `accounts` gravando no outbox
- Money
- uuid
- Etapa 1 — `accounts`: design
- AvailableBalance
- Decisões tomadas
- README.md
- JdbcOutboxWriterIT.kt
- AccountController.kt
- status.md
- coordenador.md
- 0004 — API Gateway HTTP API na borda
- 0005 — Frontend Angular e BFF em NestJS
- 0006 — Observabilidade só com correlation ID
- SpringTransactionRunner
- AccountsApplicationIT.kt
- Rodando o projeto localmente (macOS)
- 0001 — Stack: Kotlin + Spring Boot 4 na AWS
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
- Banco digital em microsserviços
- .eventType
- OutboxWriterConfiguration.kt
- account
- ConcurrencyIT
- outbox-table.sql
- V4__outbox.sql
- JdbcOutboxWriterIT.kt
- CallLog
- smoke.sh
- AccountOpened

## God Nodes (most connected - your core abstractions)
1. `Account` - 67 edges
2. `AccountApiIT` - 61 edges
3. `OverdraftLimit` - 55 edges
4. `LedgerEntry` - 54 edges
5. `Money` - 54 edges
6. `Balance` - 47 edges
7. `AccountType` - 46 edges
8. `JdbcRepositoriesIT` - 45 edges
9. `assertProblem()` - 40 edges
10. `LedgerEntryRecorded` - 33 edges

## Surprising Connections (you probably didn't know these)
- `Task 9: API HTTP, Problem Details e correlation ID (Marina, `dev-kotlin`)` --references--> `CorrelationIdFilter`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → services/accounts/adapters/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/adapters/web/CorrelationIdFilter.kt
- `Restrições (de `CLAUDE.md` e ADRs)` --references--> `JdbcOutboxWriter`  [INFERRED]
  docs/superpowers/specs/2026-10-22-etapa-1-accounts-design.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/JdbcOutboxWriter.kt
- `Restrições (de `CLAUDE.md` e ADRs)` --references--> `AccountController`  [INFERRED]
  docs/superpowers/specs/2026-10-22-etapa-1-accounts-design.md → services/accounts/adapters/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/adapters/web/AccountController.kt
- `Restrições (de `CLAUDE.md` e ADRs)` --references--> `Balance`  [INFERRED]
  docs/superpowers/specs/2026-10-22-etapa-1-accounts-design.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/Balance.kt
- `Restrições (de `CLAUDE.md` e ADRs)` --references--> `LedgerEntry`  [INFERRED]
  docs/superpowers/specs/2026-10-22-etapa-1-accounts-design.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/LedgerEntry.kt

## Import Cycles
- None detected.

## Communities (78 total, 34 thin omitted)

### Community 0 - "LedgerEntry"
Cohesion: 0.05
Nodes (26): 0012 — Saldo no lançamento e trava pessimista por conta, Alternativas consideradas, Consequências, Contexto, Decisão, Task 5: Casos de uso de crédito e débito (Marina, `dev-kotlin`), JdbcLedgerEntryRepository, IdempotencyKeyReusedException (+18 more)

### Community 1 - "IdGenerator"
Cohesion: 0.07
Nodes (9): 1. Módulos e camadas, UuidV7IdGenerator, IdGenerator, ChangeAccountTypeUseCase, GetAccountUseCase, ListAccountTypesUseCase, RecordDebitUseCase, ChangeAccountTypeUseCaseTest (+1 more)

### Community 3 - ".record"
Cohesion: 0.05
Nodes (18): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 4: Ambiente local (docker-compose), Task 5: CI no GitHub Actions (+10 more)

### Community 4 - "AccountsIntegrationTest"
Cohesion: 0.09
Nodes (4): AccountsIntegrationTest, UseCaseConfigurationIT, AccountTypeAdminApiIT, UnexpectedErrorApiIT

### Community 5 - "OverdraftLimit"
Cohesion: 0.14
Nodes (10): JdbcAccountTypeRepository, AccountTypeRepository, AccountType, BLACK, DIAMOND, FREE, GOLD, STANDARD (+2 more)

### Community 8 - "InMemoryAccountTypeRepository"
Cohesion: 0.23
Nodes (3): InMemoryAccountTypeRepository, ListAccountTypesUseCaseTest, AccountTypeRepository

### Community 9 - "Etapa 0 — Fundação: design"
Cohesion: 0.10
Nodes (23): Ambiente do Claude, Com o usuário (fora do código), Como rodar localmente, Concluído, Em andamento, Próximos passos, Status do projeto, Task 3: Aplicação `accounts` com health do banco (Testcontainers) (+15 more)

### Community 11 - "ProblemDetailsExceptionHandler.kt"
Cohesion: 0.12
Nodes (7): IdempotencyKeyHeader, ProblemDetailsExceptionHandler, AccountTypeNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException, BalanceOverflowException

### Community 12 - "JdbcOutboxWriter"
Cohesion: 0.33
Nodes (3): 9. Entrega em dois blocos, JdbcOutboxWriter, OutboxEventAppender

### Community 13 - "Bloco 1 — `accounts` gravando no outbox"
Cohesion: 0.22
Nodes (8): Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 2: Valores monetários e tipos (Marina, `dev-kotlin`), Task 7: `libs/outbox` — envelope e writer (Marie, `dev-kotlin`; em paralelo às Tasks 2–6), Task 8: Esquema, usuários de banco e repositórios JDBC (Marina, `dev-kotlin`; o script de init do compose segue o padrão do Rafael), Task 9: API HTTP, Problem Details e correlation ID (Marina, `dev-kotlin`), OutboxEnvelope

### Community 14 - "Money"
Cohesion: 0.08
Nodes (6): Balance, Money, AccountTest, BalanceTest, LedgerEntryTest, MoneyTest

### Community 15 - "uuid"
Cohesion: 0.16
Nodes (6): AccountNotFoundException, EventOutbox, TransactionRunner, EntryKind, CREDIT, DEBIT

### Community 16 - "Etapa 1 — `accounts`: design"
Cohesion: 0.22
Nodes (9): 10. Documentação que acompanha, 6. API, 7. Testes, 8. Ambiente local e CI, Critério de pronto, Etapa 1 — `accounts`: design, Fora de escopo (e onde fica registrado), Objetivo (+1 more)

### Community 17 - "AvailableBalance"
Cohesion: 0.17
Nodes (6): Ledger, Pendências e decisões em aberto, AccountView, GetAccountUseCaseTest, AvailableBalance, currentBalance()

### Community 18 - "Decisões tomadas"
Cohesion: 0.14
Nodes (14): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Em aberto, Fluxo de entrada (+6 more)

### Community 21 - "AccountController.kt"
Cohesion: 0.07
Nodes (11): AccountController, AccountTypeAdminController, ChangeAccountTypeRequest, ChangeOverdraftLimitRequest, OpenAccountRequest, RecordEntryRequest, AccountResponse, AccountTypeLimitResponse (+3 more)

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

### Community 28 - "AccountsApplicationIT.kt"
Cohesion: 0.15
Nodes (5): Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`), AccountsApplicationIT, ParameterizedTypeReference, RequestThreadProbe, RequestThreadProbeConfiguration

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

### Community 37 - "Account"
Cohesion: 0.06
Nodes (22): Regras inegociáveis, Banco digital, Clientes e contas, Consulta e avisos, Transferências, Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`), Restrições (de `CLAUDE.md` e ADRs), JdbcAccountRepository (+14 more)

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

### Community 60 - "Banco digital em microsserviços"
Cohesion: 0.25
Nodes (7): Agentes do projeto, Ao terminar uma tarefa, Banco digital em microsserviços, Contexto (leia antes de qualquer tarefa), Fluxo de trabalho (skills), Git, Grafo do código (graphify)

### Community 61 - ".eventType"
Cohesion: 0.32
Nodes (5): Bloco 2 — relay e SNS FIFO, Task 14: Verificação do SNS FIFO no LocalStack (Rafael, `infra`), Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), 3. Esquema do banco

### Community 63 - "account"
Cohesion: 0.33
Nodes (3): account_type, account, ledger_entry

### Community 64 - "ConcurrencyIT"
Cohesion: 0.06
Nodes (7): ConcurrencyIT, OpenApiIT, ApiClient, ApiResponse, isAccountBusy(), retryWhileAccountBusy(), RetryWhileAccountBusyTest

### Community 72 - "CallLog"
Cohesion: 0.22
Nodes (4): ChangeOverdraftLimitUseCase, CallLog, DirectTransactionRunner, ChangeOverdraftLimitUseCaseTest

### Community 78 - "AccountOpened"
Cohesion: 0.20
Nodes (9): Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`), Task 6: Casos de uso de tipo e limites (Marina, `dev-kotlin`), 2. Modelo de domínio, 4. Fluxos, 5. Outbox e relay (`libs/outbox`), RecordingEventOutbox, AccountEvent, AccountOpened (+1 more)

## Knowledge Gaps
- **10 isolated node(s):** `CREDIT`, `DEBIT`, `DUPLICATE_CUSTOMER`, `DUPLICATE_OPENING_KEY`, `INSERTED` (+5 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 259 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **34 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AccountsIntegrationTest` connect `AccountsIntegrationTest` to `ConcurrencyIT`, `AccountApiIT`, `JdbcOutboxWriterIT.kt`, `JdbcRepositoriesIT`, `Bloco 1 — `accounts` gravando no outbox`, `uuid`, `AccountsApplicationIT.kt`, `.eventType`?**
  _High betweenness centrality (0.116) - this node is a cross-community bridge._
- **Are the 8 inferred relationships involving `Account` (e.g. with `Regras inegociáveis` and `Clientes e contas`) actually correct?**
  _`Account` has 8 INFERRED edges - model-reasoned connections that need verification._
- **What connects `CREDIT`, `DEBIT`, `DUPLICATE_CUSTOMER` to the rest of the system?**
  _10 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `LedgerEntry` be split into smaller, more focused modules?**
  _Cohesion score 0.05450372920252438 - nodes in this community are weakly interconnected._
- **Why does `Etapa 1 — `accounts`: design` connect `Etapa 1 — `accounts`: design` to `IdGenerator`, `Account`, `JdbcOutboxWriter`, `AccountOpened`, `README.md`, `.eventType`?**
  _High betweenness centrality (0.094) - this node is a cross-community bridge._
- **Are the 15 inferred relationships involving `OverdraftLimit` (e.g. with `Ledger` and `Pendências e decisões em aberto`) actually correct?**
  _`OverdraftLimit` has 15 INFERRED edges - model-reasoned connections that need verification._
- **Should `IdGenerator` be split into smaller, more focused modules?**
  _Cohesion score 0.07450980392156863 - nodes in this community are weakly interconnected._