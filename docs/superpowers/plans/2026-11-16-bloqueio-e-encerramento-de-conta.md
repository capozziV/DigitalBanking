# Bloqueio e encerramento de conta: plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** status da conta (`ACTIVE`, `BLOCKED`, `CLOSED`), bloqueio pelo admin, encerramento pelo cliente com saldo zero, e conta nova depois de encerrar.

**Architecture:** o status vive na conta do `accounts` e é conferido dentro da transação que já trava a conta no débito e no crédito. O encerramento consulta o `transfers` por transferências pendentes. Os eventos saem pelo outbox e viram notificações.

**Tech Stack:** Kotlin, Spring Boot 4.1.1, Spring JDBC, Flyway, Testcontainers, WireMock.

**Spec:** [`docs/superpowers/specs/2026-11-16-bloqueio-e-encerramento-de-conta-design.md`](../specs/2026-11-16-bloqueio-e-encerramento-de-conta-design.md)

## Global Constraints
- Status: `AccountStatus { ACTIVE, BLOCKED, CLOSED }` no `domain` do `accounts`, sem Spring.
- Problems (`422`): `account-blocked` (débito em conta bloqueada), `account-closed` (débito ou crédito em conta encerrada; mudar status de conta encerrada), `balance-not-zero`, `pending-transfers`. `503 transfers-unavailable` quando o `transfers` não responde no encerramento.
- Eventos (tipo no outbox): `AccountBlocked`, `AccountUnblocked`, `AccountClosed`, com `accountId`, `customerId`, `occurredAt`.
- Notificações: `ACCOUNT_BLOCKED` ("Conta bloqueada"), `ACCOUNT_UNBLOCKED` ("Conta desbloqueada"), `ACCOUNT_CLOSED` ("Conta encerrada").
- Rotas: `PUT /admin/accounts/{id}/status` (só `ADMIN`); `POST /accounts/{id}/close` (dono ou `ADMIN`; outro cliente → `404`, serviço → `403`); `GET /internal/accounts/{id}/pending-transfers` no `transfers` (só `SERVICE`), resposta `{"count": N}`.
- Unicidade: índice único parcial em `account(customer_id) WHERE status <> 'CLOSED'`.
- Código, testes e logs em inglês; testes em `Test`/`IntegrationTest`; DTOs em `adapters/web/dto/` com `DTO`; grants para `accounts_app`.

## Review Focus
1. Débito e encerramento concorrentes na mesma conta: o encerramento trava a conta e relê o saldo; débito depois do encerramento → `422 account-closed` (Task 1, teste de concorrência).
2. Crédito de transferência em conta encerrada no meio da saga → transferência `COMPENSATED`, valor de volta à origem (Task 2).
3. Cliente com conta bloqueada tenta abrir outra → `409 customer-already-has-account` (Task 1).
4. Encerrar duas vezes → `422 account-closed`, sem segundo evento (Task 1).
5. `transfers` lento no encerramento → `503` dentro do timeout, conta continua `ACTIVE` (Task 1).

## Ordem e paralelismo
- Tasks 1 e 3 em paralelo (Marina, worktrees separados, a partir de `feature/conta-status`).
- Task 2 depois das frentes de débito técnico do `transfers` mergeadas na `main`.
- Clara: `AccountStatus` no `CONTEXT.md`, em paralelo.

---

### Task 1: `accounts`: status, bloqueio, encerramento e conta nova
**Files:**
- `domain`: `AccountStatus.kt`; `Account` ganha `status` e as regras (`debit` recusa `BLOCKED`/`CLOSED`, `credit` recusa `CLOSED`, `close()` exige saldo zero, `changeStatus()`); `AccountEvent` ganha `AccountBlocked`, `AccountUnblocked`, `AccountClosed`.
- `application`: `ChangeAccountStatusUseCase`, `CloseAccountUseCase`, porta `TransfersGateway.pendingOutgoing(accountId): Int`.
- `adapters`: `V9__account_status.sql` (coluna `status text not null default 'ACTIVE'` com `check`; troca a `unique` por índice parcial; grants); `JdbcAccountRepository`; `OutboxEventAppender` com os três tipos; `HttpTransfersGateway` (`adapters/transfers/`, token de máquina e correlation ID, timeout `${ACCOUNTS_TRANSFERS_TIMEOUT:500ms}`, base `${ACCOUNTS_TRANSFERS_BASE_URL:http://localhost:8082}`); `AccountStatusAdminController` e `AccountClosingController` (controllers novos); `AccountsSecurityConfiguration` com as duas rotas; `AccountDTO` com `status`; problems novos no handler.

**Interfaces (produz):** eventos `AccountBlocked`/`AccountUnblocked`/`AccountClosed` (envelope igual ao do `AccountTypeChanged`); consome `GET /internal/accounts/{id}/pending-transfers` → `{"count": Int}`.

- [ ] **Testes:**
  - domínio: débito em `BLOCKED` → `AccountBlockedException`; crédito em `BLOCKED` aceito; débito e crédito em `CLOSED` → `AccountClosedException`; `close()` com saldo ≠ 0 → `BalanceNotZeroException`; `close()` em `CLOSED` → `AccountClosedException`;
  - casos de uso: bloqueio grava evento `AccountBlocked`; encerramento com `pendingOutgoing > 0` → `PendingTransfersException`; gateway falhando → `TransfersUnavailableException`, conta `ACTIVE`;
  - integração: `PUT /admin/accounts/{id}/status` `200` com admin, `403` com cliente e serviço; `POST /accounts/{id}/close` `200` pelo dono com saldo 0 e stub do `transfers` `{"count":0}`, `404` de outro cliente, `422 balance-not-zero`, `422 pending-transfers`, `503` com stub lento (< 1 s); conta nova depois de encerrar `201`; com conta bloqueada `409`; débito de serviço em conta bloqueada `422 account-blocked`, crédito `201`; Review Focus 1 e 4.
- [ ] Build do `accounts` verde. Commit `feat(accounts): bloqueio e encerramento de conta`.

### Task 2: `transfers`: pendentes por conta e saga com conta bloqueada ou encerrada
**Files:** `GET /internal/accounts/{id}/pending-transfers` (controller novo; `SERVICE` no `TransfersSecurityConfiguration`); consulta `count(*)` de transferências `PENDING` com origem na conta.

- [ ] **Testes:** rota `200 {"count":N}` com token de serviço, `403` com cliente; saga com débito `422 account-blocked` → `FAILED`; crédito `422 account-closed` → `COMPENSATED` (Review Focus 2).
- [ ] Build do `transfers` verde. Commit `feat(transfers): transferências pendentes por conta e saga com conta bloqueada ou encerrada`.

### Task 3: `notification`: notificações de status da conta
**Files:** `NotificationKind` com `ACCOUNT_BLOCKED`, `ACCOUNT_UNBLOCKED`, `ACCOUNT_CLOSED`; textos; consumo dos três eventos da fila do `accounts` (idempotente por `processed_events`); migração se o `check` da tabela listar os tipos.

- [ ] **Testes:** cada evento vira uma notificação do cliente com o texto certo; o mesmo evento duas vezes vira uma só.
- [ ] Build do `notification` verde. Commit `feat(notification): notificações de bloqueio, desbloqueio e encerramento de conta`.

## Revisão final da branch (Beatriz)
Mutantes: débito aceitando `BLOCKED` → teste vermelho; encerramento sem checar saldo → vermelho; índice único sem o `WHERE` → teste da conta nova vermelho.
