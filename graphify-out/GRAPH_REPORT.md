# Graph Report - agent-acc050ceb7d824d3e  (2026-10-27)

## Corpus Check
- 170 files · ~49,761 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 19 file(s) not represented in the graph (top: .bru 8, (none) 4, .properties 2)

## Summary
- 1458 nodes · 3577 edges · 105 communities (55 shown, 50 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 320 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `c0b82b5d`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- uuid
- AccountsIntegrationTest
- AccountApiIntegrationTest
- SpringTransactionRunner
- RecordCreditUseCaseTest
- SnsEventPublisherTest.kt
- LocalStackTestSupport
- JdbcOutboxStoreIntegrationTest
- Account
- Balance
- .record
- ProblemDetailsExceptionHandler.kt
- OutboxRelayIntegrationTest.kt
- JdbcOutboxWriterIntegrationTest.kt
- LedgerEntry
- Account
- OutboxRelayTest
- OverdraftLimit
- Decisões tomadas
- README.md
- AccountController.kt
- LocalStackFifoIntegrationTest.kt
- AvailableBalance
- coordenador.md
- 0004 — API Gateway HTTP API na borda
- 0005 — Frontend Angular e BFF em NestJS
- 0006 — Observabilidade só com correlation ID
- SnsEventPublisherIntegrationTest.kt
- AccountEvent
- Rodando o projeto localmente (macOS)
- Etapa 0 — Fundação: design
- 0008 — Extrato (`statement`) com CQRS no DynamoDB
- 0009 — Cache do extrato
- 0010 — Limite diário com Redis + Lua
- 0013 — Persistência com Spring JDBC (`JdbcClient`)
- 0014 — Tipo de conta e cheque especial
- ApiClient
- LedgerEntryRecorded
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
- SnsEventPublisher
- outbox-table.sql
- V4__outbox.sql
- IdGenerator
- Foco de revisão
- OutboxRelaySchedulerTest.kt
- OutboxBacklog
- Etapa 1 — `accounts`: design
- 0001 — Stack: Kotlin + Spring Boot 4 na AWS
- smoke.sh
- Money
- Test
- 0012 — Saldo no lançamento e trava pessimista por conta
- AccountsApplicationIntegrationTest.kt
- 01-accounts-topic.sh
- JdbcOutboxWriter
- PendingEvent
- OutboxRelayUnreachableBrokerIntegrationTest.kt
- OutboxRelay
- CallLog
- .entry
- Test
- OutboxStore
- .record
- ArchitectureTest
- JdbcRepositoriesIntegrationTest.kt
- 0002 — Clean Architecture por serviço
- duration
- .outbox
- Status do projeto
- Bloco 1 — `accounts` gravando no outbox
- AccountOpened
- OutboxWriterConfiguration.kt
- Regras inegociáveis
- LedgerEntryRepository

## God Nodes (most connected - your core abstractions)
1. `Account` - 66 edges
2. `AccountApiIntegrationTest` - 61 edges
3. `LedgerEntry` - 54 edges
4. `OverdraftLimit` - 54 edges
5. `Money` - 53 edges
6. `Balance` - 47 edges
7. `AccountType` - 46 edges
8. `JdbcRepositoriesIntegrationTest` - 45 edges
9. `assertProblem()` - 40 edges
10. `OutboxRelayIntegrationTest` - 34 edges

## Surprising Connections (you probably didn't know these)
- `Mensageria` --references--> `OutboxRelay`  [INFERRED]
  docs/arquitetura.md → libs/outbox-relay/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/relay/OutboxRelay.kt
- `Concluído` --references--> `OutboxRelay`  [INFERRED]
  docs/status.md → libs/outbox-relay/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/relay/OutboxRelay.kt
- `Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`)` --references--> `ArchitectureTest`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → services/accounts/bootstrap/src/test/kotlin/io/github/capozziv/digitalbanking/accounts/bootstrap/ArchitectureTest.kt
- `Decisão` --references--> `LedgerEntry`  [INFERRED]
  docs/adr/0012-saldo-no-lancamento-e-trava-pessimista.md → services/accounts/domain/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/domain/LedgerEntry.kt
- `Task 15: Relay e publicador SNS (Marina, `dev-kotlin`)` --references--> `EventPublisher`  [INFERRED]
  docs/superpowers/plans/2026-10-22-etapa-1-accounts.md → libs/outbox-relay/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/relay/EventPublisher.kt

## Import Cycles
- None detected.

## Communities (105 total, 50 thin omitted)

### Community 0 - "uuid"
Cohesion: 0.22
Nodes (7): Task 4: Portas e casos de uso de abrir e consultar conta (Marina, `dev-kotlin`), Task 5: Casos de uso de crédito e débito (Marina, `dev-kotlin`), AccountBusyException, AccountNotFoundException, CustomerAlreadyHasAccountException, IdempotencyKeyReusedException, InMemoryAccountRepositoryTest

### Community 1 - "AccountsIntegrationTest"
Cohesion: 0.05
Nodes (10): OutboxSchemaAssertionsIntegrationTest, assertOutboxSchemaMatchesModel(), createModelSchema(), currentSchema(), OutboxSchemaSnapshot, AccountsIntegrationTest, OutboxSchemaIntegrationTest, UseCaseConfigurationIntegrationTest (+2 more)

### Community 4 - "RecordCreditUseCaseTest"
Cohesion: 0.19
Nodes (7): DebitOutcome, Recorded, Refused, RecordEntryCommand, RecordEntryResult, SequentialIdGenerator, RecordCreditUseCaseTest

### Community 8 - "Account"
Cohesion: 0.10
Nodes (10): Regras inegociáveis, Task 8: Esquema, usuários de banco e repositórios JDBC (Marina, `dev-kotlin`; o script de init do compose segue o padrão do Rafael), JdbcAccountRepository, AccountRepository, InsertAccountResult, DUPLICATE_CUSTOMER, DUPLICATE_OPENING_KEY, INSERTED (+2 more)

### Community 10 - ".record"
Cohesion: 0.33
Nodes (4): DebitResult, EntryRecorded, Recorded, Refused

### Community 12 - "OutboxRelayIntegrationTest.kt"
Cohesion: 0.08
Nodes (3): OutboxRelayIntegrationTest, ReceivedEvent, RecordingPublisherConfiguration

### Community 14 - "LedgerEntry"
Cohesion: 0.09
Nodes (11): Ledger, 10. Documentação que acompanha, JdbcLedgerEntryRepository, LedgerEntryRepository, IdempotentEntryLookup, InMemoryLedgerEntryRepository, EntryKind, CREDIT (+3 more)

### Community 16 - "OutboxRelayTest"
Cohesion: 0.07
Nodes (6): Task 9: API HTTP, Problem Details e correlation ID (Marina, `dev-kotlin`), FakeEventPublisher, FakeOutboxStore, MutableClock, OutboxRelayTest, CorrelationIdFilter

### Community 17 - "OverdraftLimit"
Cohesion: 0.05
Nodes (24): Banco digital, Clientes e contas, Consulta e avisos, Transferências, 1. Módulos e camadas, JdbcAccountTypeRepository, AccountTypeAdminController, AccountTypeLimitDTO (+16 more)

### Community 18 - "Decisões tomadas"
Cohesion: 0.14
Nodes (14): Arquitetura, Borda e segurança, Cache, Contratos, Dados e consistência, Decisões tomadas, Em aberto, Fluxo de entrada (+6 more)

### Community 20 - "AccountController.kt"
Cohesion: 0.06
Nodes (14): AccountController, ChangeAccountTypeDTO, ChangeOverdraftLimitDTO, OpenAccountDTO, RecordEntryDTO, AccountDTO, LedgerEntryDTO, IdempotencyKeyHeader (+6 more)

### Community 22 - "AvailableBalance"
Cohesion: 0.22
Nodes (4): 2. Modelo de domínio, AccountView, GetAccountUseCaseTest, AvailableBalance

### Community 23 - "coordenador.md"
Cohesion: 0.29
Nodes (6): Ao começar uma sessão, Board, Para cada etapa do roadmap, Para cada tarefa, Paralelismo, Quando faltar algo

### Community 24 - "0004 — API Gateway HTTP API na borda"
Cohesion: 0.33
Nodes (5): 0004 — API Gateway HTTP API na borda, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 25 - "0005 — Frontend Angular e BFF em NestJS"
Cohesion: 0.33
Nodes (5): 0005 — Frontend Angular e BFF em NestJS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 26 - "0006 — Observabilidade só com correlation ID"
Cohesion: 0.33
Nodes (5): 0006 — Observabilidade só com correlation ID, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 28 - "AccountEvent"
Cohesion: 0.39
Nodes (3): EventOutbox, RecordingEventOutbox, AccountEvent

### Community 29 - "Rodando o projeto localmente (macOS)"
Cohesion: 0.17
Nodes (11): 1. Configurar o ambiente, 2. Subir a infraestrutura (PostgreSQL, Redis, nginx, LocalStack), 3. Build e testes, 4. Subir o serviço `accounts`, 5. Conferir a API pelo nginx, 6. Ver os eventos na fila de inspeção, 7. Coleção Bruno, 8. Derrubar tudo (+3 more)

### Community 30 - "Etapa 0 — Fundação: design"
Cohesion: 0.14
Nodes (14): Task 13: Documentação do bloco 1 e das decisões da etapa (Clara, `documentador`; em paralelo às Tasks 8–12), 2. Build e convenções, 3. Esqueleto do `accounts`, 4. Ambiente local, 5. CI (GitHub Actions), Critério de pronto, Etapa 0 — Fundação: design, Fora de escopo (e onde foi registrado) (+6 more)

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

### Community 61 - "OutboxRelayScheduler"
Cohesion: 0.18
Nodes (6): 0015 — Relay do outbox com líder único e SNS FIFO, Alternativas consideradas, Consequências, Contexto, Decisão, OutboxRelayScheduler

### Community 62 - "OutboxRelayConfiguration.kt"
Cohesion: 0.17
Nodes (3): AwsProperties, OutboxRelayConfiguration, OutboxRelayProperties

### Community 63 - "account"
Cohesion: 0.33
Nodes (3): account_type, account, ledger_entry

### Community 64 - "SnsEventPublisher"
Cohesion: 0.21
Nodes (3): SnsEventPublisher, OutboxEventEnvelope, OutboxEventEnvelopeTest

### Community 65 - "outbox-table.sql"
Cohesion: 0.83
Nodes (3): outbox, outbox_stuck_idx, outbox_unpublished_idx

### Community 69 - "IdGenerator"
Cohesion: 0.08
Nodes (9): 4. Fluxos, Restrições (de `CLAUDE.md` e ADRs), UuidV7IdGenerator, IdGenerator, GetAccountUseCase, OpenAccountUseCase, RecordCreditUseCase, RecordDebitUseCase (+1 more)

### Community 70 - "Foco de revisão"
Cohesion: 0.17
Nodes (11): Ambiente de execução, Etapa 0 — Fundação: plano de implementação, Foco de revisão, Restrições globais, Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint), Task 2: Regras de arquitetura (ArchUnit), Task 3: Aplicação `accounts` com health do banco (Testcontainers), Task 4: Ambiente local (docker-compose) (+3 more)

### Community 73 - "Etapa 1 — `accounts`: design"
Cohesion: 0.17
Nodes (10): Task 7: `libs/outbox` — envelope e writer (Marie, `dev-kotlin`; em paralelo às Tasks 2–6), 3. Esquema do banco, 6. API, 7. Testes, 8. Ambiente local e CI, Critério de pronto, Etapa 1 — `accounts`: design, Fora de escopo (e onde fica registrado) (+2 more)

### Community 74 - "0001 — Stack: Kotlin + Spring Boot 4 na AWS"
Cohesion: 0.40
Nodes (5): 0001 — Stack: Kotlin + Spring Boot 4 na AWS, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 75 - "smoke.sh"
Cohesion: 0.83
Nodes (3): aws_local(), call(), smoke.sh script

### Community 78 - "0012 — Saldo no lançamento e trava pessimista por conta"
Cohesion: 0.40
Nodes (5): 0012 — Saldo no lançamento e trava pessimista por conta, Alternativas consideradas, Consequências, Contexto, Decisão

### Community 79 - "AccountsApplicationIntegrationTest.kt"
Cohesion: 0.15
Nodes (5): Task 1: Source set `integrationTest` e versões novas (Rafael, `infra`), AccountsApplicationIntegrationTest, ParameterizedTypeReference, RequestThreadProbe, RequestThreadProbeConfiguration

### Community 80 - "01-accounts-topic.sh"
Cohesion: 0.40
Nodes (5): AWS_ACCESS_KEY_ID, AWS_DEFAULT_REGION, aws_local(), AWS_SECRET_ACCESS_KEY, 01-accounts-topic.sh script

### Community 81 - "JdbcOutboxWriter"
Cohesion: 0.26
Nodes (4): 5. Outbox e relay (`libs/outbox`), 9. Entrega em dois blocos, JdbcOutboxWriter, OutboxEventAppender

### Community 82 - "PendingEvent"
Cohesion: 0.13
Nodes (5): EventPublisher, JdbcOutboxStore, PendingEvent, RecordingEventPublisher, NoOpPublisher

### Community 85 - "CallLog"
Cohesion: 0.18
Nodes (5): Task 6: Casos de uso de tipo e limites (Marina, `dev-kotlin`), ChangeAccountTypeUseCase, CallLog, ChangeAccountTypeUseCaseTest, AccountTypeChanged

### Community 90 - "OutboxStore"
Cohesion: 0.24
Nodes (5): Bloco 2 — relay e SNS FIFO, Task 14: Verificação do SNS FIFO no LocalStack (Rafael, `infra`), Task 15: Relay e publicador SNS (Marina, `dev-kotlin`), Task 16: Relay no `accounts`, LocalStack no compose e CI (Rafael, `infra`, com a configuração Spring feita pela Marina na mesma task se o diff de Kotlin for a maior parte — decidir pelo diff), OutboxStore

### Community 94 - "0002 — Clean Architecture por serviço"
Cohesion: 0.20
Nodes (6): 0002 — Clean Architecture por serviço, Alternativas consideradas, Consequências, Contexto, Decisão, Roadmap

### Community 97 - ".outbox"
Cohesion: 0.33
Nodes (4): Etapa 1 — `accounts` Implementation Plan, Global Constraints, Review Focus, EventOutbox

### Community 98 - "Status do projeto"
Cohesion: 0.33
Nodes (6): Ambiente do Claude, Como rodar localmente, Como usar o board, Concluído, Convenções que já custaram caro, Status do projeto

### Community 99 - "Bloco 1 — `accounts` gravando no outbox"
Cohesion: 0.33
Nodes (6): Bloco 1 — `accounts` gravando no outbox, Task 10: OpenAPI, concorrência e idempotência ponta a ponta (Marina, `dev-kotlin`), Task 11: Regras ArchUnit e melhorias da etapa 0 (Marina, `dev-kotlin`), Task 12: nginx, coleção Bruno e smoke (Rafael, `infra`), Task 2: Valores monetários e tipos (Marina, `dev-kotlin`), Task 3: Conta, lançamento e eventos de domínio (Marina, `dev-kotlin`)

### Community 100 - "AccountOpened"
Cohesion: 0.26
Nodes (5): OpenAccountCommand, OpenAccountResult, DirectTransactionRunner, OpenAccountUseCaseTest, AccountOpened

### Community 102 - "Regras inegociáveis"
Cohesion: 0.25
Nodes (7): Agentes do projeto, Ao terminar uma tarefa, Banco digital em microsserviços, Contexto (leia antes de qualquer tarefa), Fluxo de trabalho (skills), Git, Grafo do código (graphify)

## Knowledge Gaps
- **13 isolated node(s):** `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION`, `INSERTED`, `DUPLICATE_OPENING_KEY` (+8 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 302 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **50 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AccountsIntegrationTest` connect `AccountsIntegrationTest` to `AccountApiIntegrationTest`, `ApiClient`, `Account`, `OutboxRelayIntegrationTest.kt`, `AccountsApplicationIntegrationTest.kt`, `Account`, `OutboxRelayUnreachableBrokerIntegrationTest.kt`, `OutboxStore`, `JdbcRepositoriesIntegrationTest.kt`?**
  _High betweenness centrality (0.102) - this node is a cross-community bridge._
- **Are the 7 inferred relationships involving `Account` (e.g. with `Regras inegociáveis` and `Clientes e contas`) actually correct?**
  _`Account` has 7 INFERRED edges - model-reasoned connections that need verification._
- **What connects `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION` to the rest of the system?**
  _13 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AccountsIntegrationTest` be split into smaller, more focused modules?**
  _Cohesion score 0.053555750658472345 - nodes in this community are weakly interconnected._
- **Why does `Account` connect `Account` to `uuid`, `AccountOpened`, `IdGenerator`, `RecordCreditUseCaseTest`, `LedgerEntryRecorded`, `.record`, `Money`, `LedgerEntry`, `Account`, `OverdraftLimit`, `CallLog`, `AvailableBalance`, `.record`, `JdbcRepositoriesIntegrationTest.kt`?**
  _High betweenness centrality (0.077) - this node is a cross-community bridge._
- **Are the 9 inferred relationships involving `LedgerEntry` (e.g. with `Regras inegociáveis` and `Ledger`) actually correct?**
  _`LedgerEntry` has 9 INFERRED edges - model-reasoned connections that need verification._
- **Should `AccountApiIntegrationTest` be split into smaller, more focused modules?**
  _Cohesion score 0.1111111111111111 - nodes in this community are weakly interconnected._