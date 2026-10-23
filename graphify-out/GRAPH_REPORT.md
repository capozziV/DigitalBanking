# Graph Report - digitalbanking  (2026-10-23)

## Corpus Check
- 166 files · ~48,096 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 18 file(s) not represented in the graph (top: .bru 8, (none) 3, .properties 2)

## Summary
- 1414 nodes · 3460 edges · 99 communities (59 shown, 40 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 305 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `0f0f1b22`
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
- ChangeAccountTypeUseCase
- OverdraftLimit
- Bloco 1 — `accounts` gravando no outbox
- ProblemDetailsExceptionHandler.kt
- OutboxRelayIT.kt
- Status do projeto
- LedgerEntry
- uuid
- duration
- AccountType
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
- OutboxRelayScheduler
- OutboxRelayConfiguration.kt
- account
- ConcurrencyIT
- outbox-table.sql
- V4__outbox.sql
- RecordCreditUseCaseTest
- .record
- OutboxRelay
- OutboxRelayTest
- .record
- OutboxMetrics
- smoke.sh
- Money
- Clock
- AccountsApplication.kt
- Etapa 0 — Fundação: design
- 01-accounts-topic.sh
- OutboxRelaySettings
- OutboxStore
- OpenAccountUseCase
- .inTransaction
- InMemoryAccountTypeRepository
- JdbcAccountRepository.kt
- .entry
- Test
- .accounts
- AccountNotFoundException
- CallLog
- Banco digital em microsserviços
- 0002 — Clean Architecture por serviço
- Foco de revisão
- Banco digital
- .updateLimit
- InMemoryAccountRepositoryTest

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
10. `OutboxRelayIT` - 33 edges

## Surprising Connections (you probably didn't know these)
- `Decisão` --references--> `OutboxRelay`  [INFERRED]
  docs/adr/0015-outbox-relay-lider-e-sns-fifo.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/OutboxRelay.kt
- `Mensageria` --references--> `OutboxRelay`  [INFERRED]
  docs/arquitetura.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/OutboxRelay.kt
- `Decisão` --references--> `LedgerEntry`  [INFERRED]
  docs/adr/0012-saldo-no-lancamento-e-trava-pessimista.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/LedgerEntry.kt
- `Task 15: Relay e publicador SNS (Marina, `dev-kotlin`)` --references--> `EventPublisher`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/EventPublisher.kt
- `Restrições (de `CLAUDE.md` e ADRs)` --references--> `JdbcOutboxWriter`  [INFERRED]
  docs/superpowers/specs/2026-10-22-etapa-1-accounts-design.md → libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/JdbcOutboxWriter.kt

## Import Cycles
- None detected.

## Communities (99 total, 40 thin omitted)

### Community 0 - "LedgerEntryRecorded"
Cohesion: 0.38
Nodes (3): RecordEntryCommand, RecordDebitUseCaseTest, LedgerEntryRecorded

### Community 1 - "IdGenerator"
Cohesion: 0.11
Nodes (7): UuidV7IdGenerator, IdGenerator, DebitOutcome, RecordDebitUseCase, Recorded, Refused, UseCaseConfiguration

### Community 4 - "AccountsIntegrationTest"
Cohesion: 0.09
Nodes (4): AccountsIntegrationTest, UseCaseConfigurationIT, AccountTypeAdminApiIT, UnexpectedErrorApiIT

### Community 6 - "LocalStackFifoIT.kt"
Cohesion: 0.05
Nodes (7): LocalStackFifoIT, ReceivedMessage, SnsEventPublisherIT, SnsEventPublisher, OutboxEnvelopeTest, LocalStackTestSupport, LocalStackTestSupportTest

### Community 9 - "OverdraftLimit"
Cohesion: 0.12
Nodes (10): Pendências e decisões em aberto, 2. Modelo de domínio, AccountView, GetAccountUseCase, GetAccountUseCaseTest, AvailableBalance, Balance, currentBalance() (+2 more)

### Community 10 - "Bloco 1 — `accounts` gravando no outbox"
Cohesion: 0.07
Nodes (12): Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 2: Valores monetários e tipos (Marina, `dev-kotlin`), Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`), Task 6: Casos de uso de tipo e limites (Marina, `dev-kotlin`), Task 7: `libs/outbox` — envelope e writer (Marie, `dev-kotlin`; em paralelo às Tasks 2–6) (+4 more)

### Community 11 - "ProblemDetailsExceptionHandler.kt"
Cohesion: 0.12
Nodes (7): IdempotencyKeyHeader, ProblemDetailsExceptionHandler, AccountTypeNotFoundException, InsufficientAvailableBalanceException, InvalidField, InvalidRequestException, BalanceOverflowException

### Community 12 - "OutboxRelayIT.kt"
Cohesion: 0.06
Nodes (5): OpenApiIT, OutboxRelayIT, ReceivedEvent, RecordingEventPublisher, RecordingPublisherConfiguration

### Community 13 - "Status do projeto"
Cohesion: 0.24
Nodes (11): Ambiente do Claude, Com o usuário (fora do código), Como rodar localmente, Concluído, Em andamento, Próximos passos, Status do projeto, Task 13: Documentação do bloco 1 e das decisões da etapa (Clara, `documentador`; em paralelo às Tasks 8–12) (+3 more)

### Community 14 - "LedgerEntry"
Cohesion: 0.10
Nodes (11): Ledger, Task 5: Casos de uso de crédito e débito (Marina, `dev-kotlin`), JdbcLedgerEntryRepository, IdempotencyKeyReusedException, LedgerEntryRepository, IdempotentEntryLookup, InMemoryLedgerEntryRepository, EntryKind (+3 more)

### Community 17 - "AccountType"
Cohesion: 0.13
Nodes (9): JdbcAccountTypeRepository, AccountTypeRepository, AccountType, BLACK, DIAMOND, FREE, GOLD, STANDARD (+1 more)

### Community 18 - "Decisões tomadas"
Cohesion: 0.14
Nodes (14): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Em aberto, Fluxo de entrada (+6 more)

### Community 21 - "AccountController.kt"
Cohesion: 0.07
Nodes (11): AccountController, AccountTypeAdminController, ChangeAccountTypeRequest, ChangeOverdraftLimitRequest, OpenAccountRequest, RecordEntryRequest, AccountResponse, AccountTypeLimitResponse (+3 more)

### Community 22 - "status.md"
Cohesion: 0.17
Nodes (6): 0001 — Stack: Kotlin + Spring Boot 4 na AWS, Alternativas consideradas, Consequências, Contexto, Decisão, Roadmap

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

### Community 27 - "Etapa 1 — `accounts`: design"
Cohesion: 0.05
Nodes (24): 10. Documentação que acompanha, 1. Módulos e camadas, 3. Esquema do banco, 4. Fluxos, 5. Outbox e relay (`libs/outbox`), 6. API, 7. Testes, 8. Ambiente local e CI (+16 more)

### Community 28 - "AccountsApplicationIT.kt"
Cohesion: 0.15
Nodes (5): Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`), AccountsApplicationIT, ParameterizedTypeReference, RequestThreadProbe, RequestThreadProbeConfiguration

### Community 29 - "Rodando o projeto localmente (macOS)"
Cohesion: 0.18
Nodes (11): 1. Configurar o ambiente, 2. Subir a infraestrutura (PostgreSQL, Redis, nginx, LocalStack), 3. Build e testes, 4. Subir o serviço `accounts`, 5. Conferir a API pelo nginx, 6. Ver os eventos na fila de inspeção, 7. Coleção Bruno, 8. Derrubar tudo (+3 more)

### Community 30 - "0012 — Saldo no lançamento e trava pessimista por conta"
Cohesion: 0.40
Nodes (5): 0012 — Saldo no lançamento e trava pessimista por conta, Alternativas consideradas, Consequências, Contexto, Decisão

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

### Community 36 - "0015 — Relay do outbox com líder único e SNS FIFO"
Cohesion: 0.40
Nodes (5): 0015 — Relay do outbox com líder único e SNS FIFO, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 37 - "Account"
Cohesion: 0.15
Nodes (7): AccountRepository, InsertAccountResult, DUPLICATE_CUSTOMER, DUPLICATE_OPENING_KEY, INSERTED, InMemoryAccountRepository, Account

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
Cohesion: 0.13
Nodes (3): OutboxRelayScheduler, BlockingStore, OutboxRelaySchedulerTest

### Community 62 - "OutboxRelayConfiguration.kt"
Cohesion: 0.19
Nodes (4): OutboxRelaySettings, AwsProperties, OutboxRelayConfiguration, OutboxRelayProperties

### Community 63 - "account"
Cohesion: 0.33
Nodes (3): account_type, account, ledger_entry

### Community 64 - "ConcurrencyIT"
Cohesion: 0.08
Nodes (7): ConcurrencyIT, OutboxRelayUnreachableBrokerIT, ApiClient, ApiResponse, isAccountBusy(), retryWhileAccountBusy(), RetryWhileAccountBusyTest

### Community 65 - "outbox-table.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 69 - "RecordCreditUseCaseTest"
Cohesion: 0.21
Nodes (4): RecordCreditUseCase, RecordEntryResult, SequentialIdGenerator, RecordCreditUseCaseTest

### Community 71 - "OutboxRelay"
Cohesion: 0.19
Nodes (4): EventPublisher, OutboxRelay, PendingEvent, NoOpPublisher

### Community 72 - "OutboxRelayTest"
Cohesion: 0.13
Nodes (4): FakeEventPublisher, FakeOutboxStore, MutableClock, OutboxRelayTest

### Community 73 - ".record"
Cohesion: 0.33
Nodes (4): DebitResult, EntryRecorded, Recorded, Refused

### Community 75 - "smoke.sh"
Cohesion: 0.83
Nodes (3): aws_local(), call(), smoke.sh script

### Community 78 - "AccountsApplication.kt"
Cohesion: 0.33
Nodes (4): Task 3: Aplicação `accounts` com health do banco (Testcontainers), 3. Esqueleto do `accounts`, Testes (no `bootstrap`, que enxerga todos os módulos), AccountsApplication

### Community 79 - "Etapa 0 — Fundação: design"
Cohesion: 0.25
Nodes (8): 1. Estrutura do monorepo, 2. Build e convenções, 4. Ambiente local, Critério de pronto, Etapa 0 — Fundação: design, Objetivo, Restrições (de `CLAUDE.md` e ADRs), Riscos

### Community 80 - "01-accounts-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 01-accounts-topic.sh script

### Community 82 - "OutboxStore"
Cohesion: 0.18
Nodes (6): Bloco 2 — relay e SNS FIFO, Task 14: Verificação do SNS FIFO no LocalStack (Rafael, `infra`), Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), JdbcOutboxStore, OutboxStore

### Community 83 - "OpenAccountUseCase"
Cohesion: 0.22
Nodes (5): Restrições (de `CLAUDE.md` e ADRs), OpenAccountCommand, OpenAccountResult, OpenAccountUseCase, OpenAccountUseCaseTest

### Community 85 - "InMemoryAccountTypeRepository"
Cohesion: 0.18
Nodes (4): ListAccountTypesUseCase, InMemoryAccountTypeRepository, ListAccountTypesUseCaseTest, AccountTypeRepository

### Community 90 - ".accounts"
Cohesion: 0.25
Nodes (4): Etapa 1 — `accounts` Implementation Plan, Global Constraints, Review Focus, AccountRepository

### Community 91 - "AccountNotFoundException"
Cohesion: 0.36
Nodes (5): Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`), Task 8: Esquema, usuários de banco e repositórios JDBC (Marina, `dev-kotlin`; o script de init do compose segue o padrão do Rafael), AccountBusyException, AccountNotFoundException, CustomerAlreadyHasAccountException

### Community 92 - "CallLog"
Cohesion: 0.33
Nodes (3): ChangeOverdraftLimitUseCase, CallLog, ChangeOverdraftLimitUseCaseTest

### Community 93 - "Banco digital em microsserviços"
Cohesion: 0.22
Nodes (8): Agentes do projeto, Ao terminar uma tarefa, Banco digital em microsserviços, Contexto (leia antes de qualquer tarefa), Fluxo de trabalho (skills), Git, Grafo do código (graphify), Regras inegociáveis

### Community 94 - "0002 — Clean Architecture por serviço"
Cohesion: 0.33
Nodes (5): 0002 — Clean Architecture por serviço, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 95 - "Foco de revisão"
Cohesion: 0.22
Nodes (9): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 4: Ambiente local (docker-compose), Task 5: CI no GitHub Actions (+1 more)

### Community 96 - "Banco digital"
Cohesion: 0.40
Nodes (4): Banco digital, Clientes e contas, Consulta e avisos, Transferências

## Knowledge Gaps
- **13 isolated node(s):** `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION`, `INSERTED`, `DUPLICATE_OPENING_KEY` (+8 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 293 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **40 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AccountsIntegrationTest` connect `AccountsIntegrationTest` to `ConcurrencyIT`, `AccountApiIT`, `JdbcRepositoriesIT`, `OutboxRelayIT.kt`, `Clock`, `OutboxStore`, `AccountNotFoundException`, `AccountsApplicationIT.kt`?**
  _High betweenness centrality (0.091) - this node is a cross-community bridge._
- **Are the 8 inferred relationships involving `Account` (e.g. with `Regras inegociáveis` and `Clientes e contas`) actually correct?**
  _`Account` has 8 INFERRED edges - model-reasoned connections that need verification._
- **What connects `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION` to the rest of the system?**
  _13 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `IdGenerator` be split into smaller, more focused modules?**
  _Cohesion score 0.10810810810810811 - nodes in this community are weakly interconnected._
- **Why does `OutboxRelay` connect `OutboxRelay` to `0015 — Relay do outbox com líder único e SNS FIFO`, `OutboxRelayTest`, `OutboxMetrics`, `OutboxRelayIT.kt`, `Clock`, `duration`, `Decisões tomadas`, `OpenAccountUseCase`, `OutboxStore`, `Etapa 1 — `accounts`: design`, `OutboxRelayScheduler`, `OutboxRelayConfiguration.kt`?**
  _High betweenness centrality (0.079) - this node is a cross-community bridge._
- **Are the 10 inferred relationships involving `LedgerEntry` (e.g. with `Regras inegociáveis` and `Ledger`) actually correct?**
  _`LedgerEntry` has 10 INFERRED edges - model-reasoned connections that need verification._
- **Should `AccountApiIT` be split into smaller, more focused modules?**
  _Cohesion score 0.1111111111111111 - nodes in this community are weakly interconnected._