# Etapa 2 — `transfers` + saga: design

- **Data:** 2026-10-27
- **Status:** aprovada (2026-10-27)
- **Roadmap:** etapa 2 de [`docs/roadmap.md`](../../roadmap.md)
- **Board:** cartão [#9](https://github.com/capozziV/DigitalBanking/issues/9) (Stage 2 · Transfers + saga)

## Objetivo
Entregar o 2º serviço, `transfers`, que recebe uma transferência (`Transfer`) entre duas contas e a conduz por uma saga orquestrada (`TransferSaga`): débito na origem, crédito no destino e, quando o crédito não sai, compensação (`Compensation`) que devolve o valor à origem. O valor para o portfólio é provar por teste que, com falha no meio, retentativa ou queda de processo, o dinheiro nunca some nem duplica. A etapa também prepara o monorepo para mais de um serviço (relay separado, um banco por serviço, build seletivo no CI).

## Decisões desta etapa (usuário, 2026-10-27)
| # | Decisão |
|---|---|
| 1 | O `transfers` fala com o `accounts` por **HTTP síncrono**, usando os endpoints de crédito e débito que já existem. |
| 2 | `POST /transfers` roda a saga na requisição e responde com o resultado; falha passageira devolve `202 PENDING` e o agendador termina depois. |
| 3 | Falha definitiva no crédito compensa na hora; falha passageira é tentada de novo até um **prazo de 2 minutos** desde a criação; vencido o prazo, a saga consulta o lançamento pela chave (decisão 10) e compensa só se o crédito não aconteceu. |
| 4 | A saga ganha ADR próprio (ADR 0016). ECS Service Connect fica para a etapa 8 (novo cartão `Decision`). |
| 5 | Valor mínimo de 1 centavo, sem máximo até o limite diário (etapa 6). A origem pode usar o cheque especial, com a regra do débito comum. |
| 6 | Origem igual ao destino, ou conta inexistente: `422`, nada gravado. |
| 7 | Descrição opcional do cliente (até 140 caracteres); o lançamento leva texto fixo com o ID da transferência. |
| 8 | `Idempotency-Key` obrigatória: mesma chave e mesmo pedido devolvem a transferência existente; outro pedido com a mesma chave é `422 idempotency-key-reused`. |
| 9 | Status visíveis: `PENDING`, `COMPLETED`, `FAILED`, `COMPENSATED`. Só `POST /transfers` e `GET /transfers/{id}`. |
| 10 | Dúvida sobre um débito ou crédito depois do prazo se resolve **consultando o lançamento pela chave** num endpoint novo do `accounts`, sem gravar nada. |
| 11 | Eventos `TransferCompleted`, `TransferFailed`, `TransferCompensated` pelo outbox; coleção Bruno e smoke com transferência. |
| 12 | Passos da saga com o padrão **State**; sem Strategy na política de nova tentativa (uma implementação só, configurável). |

## Critério de pronto
1. `./gradlew build` verde (compilação, ktlint, unitários, `integrationTest`, ArchUnit, `e2e`).
2. Jobs do CI verdes, com o build seletivo e o job agregador `ci-ok`.
3. **Concorrência:** conta de origem `FREE` com crédito de 5000; 50 transferências paralelas de 200 para contas diferentes → exatamente 25 `COMPLETED` e 25 `FAILED`; saldo final da origem 0; a soma dos saldos de todas as contas é a mesma antes e depois.
4. **Repetição:** 20 pedidos paralelos com a mesma `Idempotency-Key` → uma transferência, um débito e um crédito no ledger.
5. **Queda do `accounts`:** com o `accounts` derrubado depois do débito, a transferência fica `PENDING`; se ele volta antes de 2 min, termina `COMPLETED`; se não volta, termina `COMPENSATED` quando ele voltar; em nenhum caso o destino recebe e a origem é reembolsada ao mesmo tempo.
6. **Queda do `transfers`:** processo derrubado no meio da saga; ao subir de novo, o agendador termina a transferência.
7. Eventos dos três tipos chegam à fila de inspeção `transfers-events-inspection.fifo`.
8. Coleção Bruno com os endpoints do `transfers` e o novo do `accounts`; o smoke abre duas contas, credita a origem, transfere e confere os saldos pelo nginx.

## Restrições (de `CLAUDE.md` e ADRs)
- Clean Architecture por serviço, conferida por ArchUnit; `domain` sem Spring nem infraestrutura ([ADR 0002](../../adr/0002-clean-architecture-por-servico.md)).
- Nada de domínio compartilhado entre serviços: o `transfers` tem o próprio `Money`; bibliotecas compartilhadas só de infraestrutura.
- Valores em centavos (`Long`). Código em inglês com os nomes do `CONTEXT.md`; docs e commits em português.
- Saldo para autorizar débito nunca vem de cache: a autorização continua no `accounts`, sob a trava da conta.
- Eventos só via Transactional Outbox; consumidores idempotentes ([ADR 0003](../../adr/0003-transactional-outbox-e-mensageria.md), [ADR 0015](../../adr/0015-outbox-relay-lider-e-sns-fifo.md)).
- OpenAPI, Problem Details, `Idempotency-Key` em transferências ([ADR 0011](../../adr/0011-contratos-de-api.md)).
- Correlation ID propagado nas chamadas HTTP, no outbox e nos atributos SNS ([ADR 0006](../../adr/0006-observabilidade-com-correlation-id.md)).
- Spring JDBC com `JdbcClient` ([ADR 0013](../../adr/0013-persistencia-com-spring-jdbc.md)).
- Sem autenticação (etapa 7), sem limite diário (etapa 6), sem consumidores SQS (etapa 3).

## 1. Entrega em dois blocos
**Bloco 1 — preparação para o 2º serviço** (um PR):
- **Relay separado ([#25](https://github.com/capozziV/DigitalBanking/issues/25)):** `libs/outbox` fica com envelope, `JdbcOutboxWriter` e o DDL modelo, sem AWS SDK nem Micrometer; `libs/outbox-relay` (novo) fica com `OutboxRelay`, `JdbcOutboxStore`, `SnsEventPublisher`, `OutboxMetrics` e `OutboxRelaySettings`, com AWS SDK e Micrometer como `api`.
- **Teste de esquema ([#30](https://github.com/capozziV/DigitalBanking/issues/30)):** teste que compara o esquema do outbox gerado pelas migrations de cada serviço com o DDL modelo de `libs/outbox`.
- **Regras ArchUnit compartilhadas:** saem do `bootstrap` do `accounts` para `libs/testing-architecture` (só teste), parametrizadas pelo pacote raiz do serviço; as lacunas de [#32](https://github.com/capozziV/DigitalBanking/issues/32) são fechadas junto (sufixo `UseCase` fora do pacote `usecase`, isenções por sufixo, `domain`/`application` contra infraestrutura, `@Controller` + `@ResponseBody`).
- **Gradle ([#37](https://github.com/capozziV/DigitalBanking/issues/37)):** `repositoriesMode = FAIL_ON_PROJECT_REPOS`; `pluginManagement` só com `includeBuild` se o `build-logic` resolver os plugins sozinho; `val libs` extraído se repetir.
- **PostgreSQL ([#38](https://github.com/capozziV/DigitalBanking/issues/38)):** script de init idempotente, senha via `psql -v`, fluxo documentado para adicionar um banco; banco `transfers` com `transfers_owner` (Flyway) e `transfers_app` (aplicação); volume nomeado.
- **Endpoint novo no `accounts`:** `GET /accounts/{id}/entries?idempotencyKey=…` → `200` com o lançamento (mesmo corpo do `POST` de crédito/débito) ou `404 entry-not-found`; `404 account-not-found` se a conta não existe; `400 validation-error` sem o parâmetro. Sem trava, leitura do banco.
- **Build seletivo no CI:** filtro por caminho (`dorny/paths-filter` ou equivalente):
  - job `accounts` quando muda `services/accounts/**`; job `transfers` quando muda `services/transfers/**`;
  - os dois, mais o `e2e`, quando muda `libs/**`, `build-logic/**`, `gradle/**`, `settings.gradle.kts`, `build.gradle.kts`, `infra/local/**` ou `.github/workflows/**`;
  - `e2e` e `smoke` quando muda qualquer serviço;
  - job `ci-ok` que depende de todos e passa quando cada um passou ou foi pulado — é o check exigido pela proteção da `main` ([#51](https://github.com/capozziV/DigitalBanking/issues/51)).

**Bloco 2 — o `transfers`** (um PR): seções 2 a 8.

**Fora desta etapa** (continuam como cartões Ready independentes): [#28](https://github.com/capozziV/DigitalBanking/issues/28), [#29](https://github.com/capozziV/DigitalBanking/issues/29), [#31](https://github.com/capozziV/DigitalBanking/issues/31), [#34](https://github.com/capozziV/DigitalBanking/issues/34).

## 2. Módulos e camadas
```
libs/
├── outbox/                 # envelope, writer, DDL modelo (sem AWS nem Micrometer)
├── outbox-relay/           # novo: relay, store, publicador SNS, métricas
├── testing-architecture/   # novo: regras ArchUnit compartilhadas (só teste)
└── testing-localstack/
services/transfers/
├── domain/                 # Transfer, TransferStatus, TransferStep (State), Money, eventos
├── application/            # casos de uso e portas
├── adapters/               # web, persistence (JdbcClient, Flyway), accounts (cliente HTTP)
└── bootstrap/              # aplicação Spring, agendador, configuração, ArchUnit
tests/e2e/                  # novo: testes ponta a ponta com os dois serviços
api/bruno/transfers/        # coleção Bruno
```
- **Pacote:** `io.github.capozziv.digitalbanking.transfers`. **Porta:** `8082`. **Caminhos Gradle:** `:services:transfers:<camada>`, `:libs:outbox-relay`, `:libs:testing-architecture`, `:tests:e2e`.
- **Casos de uso (`application`):** `RequestTransferUseCase` (valida, grava e roda a saga), `RunTransferSagaUseCase` (executa passos até terminar ou precisar esperar), `ResumeDueTransfersUseCase` (chamado pelo agendador), `GetTransferUseCase`.
- **Portas:** `TransferRepository`, `TransferStepLog`, `AccountsGateway`, `EventOutbox`, `TransactionRunner`, `IdGenerator`.
- **Correlation ID:** filtro mínimo no adapter web do `transfers`, igual ao do `accounts` (os dois vão para `libs/observabilidade` na etapa 7, cartão [#43](https://github.com/capozziV/DigitalBanking/issues/43)); o cliente HTTP envia o ID do MDC em `X-Correlation-Id`; o agendador restaura o ID guardado na transferência antes de cada passo.

## 3. Modelo de domínio
- **`Transfer`:** `id` (UUID v7), `sourceAccountId`, `destinationAccountId`, `amount: Money`, `description?`, `idempotencyKey`, `status: TransferStatus`, `step: TransferStep`, `failureReason?`, `deadline` (criação + prazo configurável, padrão 2 min), `attempts` (no passo atual), `correlationId?`, `createdAt`, `updatedAt`.
- **`Money`:** centavos, `Long`, > 0. Próprio do `transfers`.
- **`TransferStatus`:** `PENDING`, `COMPLETED`, `FAILED`, `COMPENSATED`. Derivado do passo: passo terminal define o status; qualquer outro é `PENDING`.
- **`TransferStep` (padrão State):** interface selada; cada passo é um objeto que sabe qual chamada ao `accounts` fazer e qual é o próximo passo para cada resultado:
  ```kotlin
  sealed interface TransferStep {
      fun onSuccess(transfer: Transfer): TransferStep
      fun onRefused(transfer: Transfer, reason: String): TransferStep
      fun onTransientFailure(transfer: Transfer, now: Instant): TransferStep
  }
  ```
  O resultado da consulta (`RESOLVE_*`) chega como `onSuccess` (lançamento encontrado) ou `onRefused` com `ENTRY_NOT_FOUND`.

| Passo | Chamada ao `accounts` | Sucesso | Recusa definitiva | Falha passageira no prazo | Falha passageira após o prazo |
|---|---|---|---|---|---|
| `Debit` | `POST /accounts/{source}/debits`, chave `transfer-<id>-debit` | `Credit` | `Failed` (motivo da recusa) | `Debit` | `ResolveDebit` |
| `ResolveDebit` | `GET /accounts/{source}/entries?idempotencyKey=transfer-<id>-debit` | `Compensate` | `ENTRY_NOT_FOUND` → `Failed` (motivo `debit-not-confirmed`) | `ResolveDebit` | `ResolveDebit` |
| `Credit` | `POST /accounts/{destination}/credits`, chave `transfer-<id>-credit` | `Completed` | `Compensate` | `Credit` | `ResolveCredit` |
| `ResolveCredit` | `GET /accounts/{destination}/entries?idempotencyKey=transfer-<id>-credit` | `Completed` | `ENTRY_NOT_FOUND` → `Compensate` | `ResolveCredit` | `ResolveCredit` |
| `Compensate` | `POST /accounts/{source}/credits`, chave `transfer-<id>-compensation` | `Compensated` | `Compensate` (recusa indica bug: log `ERROR`, nova tentativa com a espera máxima) | `Compensate` | `Compensate` |
| `Completed`, `Failed`, `Compensated` | — (terminais) | — | — | — | — |

- **Compensação recusada:** o crédito na origem não tem regra de recusa; uma recusa aqui (por exemplo, `400`) indica bug. O passo continua `Compensate`, a nova tentativa segue a espera máxima e cada recusa gera log `ERROR` com o ID da transferência. A transferência não termina sozinha nesse caso (registrado em Riscos).
- **Prazo:** só afeta `Debit` e `Credit`. `ResolveDebit`, `ResolveCredit` e `Compensate` tentam até conseguir.
- **Descrições no ledger:** débito `Transfer to <destinationAccountId>`, crédito `Transfer from <sourceAccountId>`, compensação `Transfer <id> compensation`. A descrição do cliente fica na transferência e nos eventos, não no ledger (o limite de 140 caracteres do `accounts` não comporta as duas).
- **Eventos de domínio:** `TransferCompleted`, `TransferFailed` (com `reason`), `TransferCompensated`.

## 4. Fluxos
**`POST /transfers`** (`Idempotency-Key`) `{sourceAccountId, destinationAccountId, amount, description?}`:
1. Validação do corpo (`amount` ≥ 1; `description` com as regras do `accounts`; origem ≠ destino) → `400 validation-error` ou `422 same-account-transfer`.
2. Busca pela `Idempotency-Key`: existe com o mesmo pedido (origem, destino, valor) → responde pelo estado atual (tabela abaixo); com outro pedido → `422 idempotency-key-reused`.
3. `GET /accounts/{id}` das duas contas: `404` → `422 account-not-found` (indica qual), nada gravado. Falha passageira aqui → `503 accounts-unavailable` com `Retry-After`, nada gravado.
4. Grava a transferência (`step = Debit`, `deadline = agora + prazo`) numa transação. Conflito na `idempotency_key` (pedido simultâneo) → relê e segue o passo 2.
5. Toma o arrendamento e roda `RunTransferSagaUseCase` na requisição, com até 3 tentativas imediatas para `503 account-busy`.
6. Responde pelo estado:

| Estado | 1ª resposta | Repetição com a mesma chave |
|---|---|---|
| `COMPLETED` | `201` | `200` |
| `FAILED` por saldo | `422 insufficient-available-balance` (com `transferId`) | `422` igual |
| `FAILED` por outra recusa | `422 transfer-failed` (com `reason`) | `422` igual |
| `PENDING` | `202` | `200` com o status atual |
| `COMPENSATED` | `200` | `200` |

Corpo da transferência: `{id, sourceAccountId, destinationAccountId, amount, description, status, failureReason, createdAt, updatedAt}`.

**`GET /transfers/{id}`** → `200` ou `404 transfer-not-found`.

**`RunTransferSagaUseCase`** (laço até um passo terminal ou uma espera):
1. Chama o `accounts` conforme o passo atual (sem transação aberta).
2. Numa transação: grava a linha em `transfer_step`; calcula o próximo passo pelo objeto do passo; atualiza a transferência; se chegou a um terminal, grava o evento no outbox; em falha passageira, `attempts + 1` e `next_attempt_at = agora + RetryBackoff`.
3. Mudou de passo: `attempts = 0` e segue o laço. Falha passageira: solta o arrendamento e para.

**Arrendamento (uma saga roda em um lugar só):**
- Tomada: `UPDATE transfer SET locked_until = now() + :lease, lock_owner = :owner WHERE id = :id AND status = 'PENDING' AND (locked_until IS NULL OR locked_until < now())`; 1 linha = posse.
- Cada gravação de passo confere `lock_owner`; se perdeu a posse (arrendamento venceu), a gravação é descartada e o laço para. A chamada repetida pelo novo dono é segura porque as chaves são idempotentes.
- Arrendamento padrão 30 s (configurável), maior que o pior caso de um passo (3 tentativas × 4 s de chamada).
- Nenhuma transação do banco fica aberta durante chamada HTTP.

**Agendador (`ResumeDueTransfersUseCase`):** a cada 1 s (configurável), busca até 50 transferências `PENDING` com `next_attempt_at <= now()` e arrendamento livre (`FOR UPDATE SKIP LOCKED` só na seleção), toma o arrendamento de cada uma e roda a saga. Várias instâncias do `transfers` podem rodar juntas. Desligamento gracioso como o do relay.

**`RetryBackoff`:** classe concreta, sem interface: espera `initial × 2^(tentativa−1)` com teto `max` (padrão 1 s e 15 s, configuráveis).

**Cliente HTTP (`HttpAccountsGateway`, `RestClient`):** conexão 1 s, leitura 3 s (configuráveis), URL base por configuração (`TRANSFERS_ACCOUNTS_URL`, padrão `http://localhost:8081`). Classificação (`AccountsResponseClassifier`, classe comum no adapter):
- `2xx` → sucesso;
- `422 insufficient-available-balance` → recusa com motivo `INSUFFICIENT_AVAILABLE_BALANCE`;
- `404 entry-not-found` na consulta → `ENTRY_NOT_FOUND`;
- outros `4xx` (`404 account-not-found`, `400`, `422 idempotency-key-reused`) → recusa definitiva com o `type` do Problem Details como motivo;
- `5xx` (inclui `503 account-busy`), timeout, conexão recusada → falha passageira.

## 5. Esquema do banco (`transfers`)
**`transfer`**
- `id` (`uuid`, PK), `source_account_id`, `destination_account_id` (`uuid`), `amount` (`bigint`, `CHECK > 0`), `description` (`varchar(140)`), `idempotency_key` (`text`, `UNIQUE`), `status` (`CHECK` nos 4 valores), `step` (`text`), `failure_reason` (`text`), `deadline_at`, `next_attempt_at`, `attempts` (`int`, padrão 0), `locked_until`, `lock_owner`, `correlation_id`, `created_at`, `updated_at`.
- `CHECK (source_account_id <> destination_account_id)`.
- Índice parcial `(next_attempt_at) WHERE status = 'PENDING'` para o agendador.

**`transfer_step`** (trilha de auditoria; só `INSERT`)
- `id` (`bigserial`), `transfer_id` (FK), `step`, `outcome` (`SUCCESS`, `REFUSED`, `TRANSIENT_FAILURE`), `http_status` (nulo em timeout), `detail`, `created_at`.

**`outbox`**: DDL modelo de `libs/outbox` (mesmas colunas e índices do `accounts`).

**Usuários:** `transfers_owner` (Flyway); `transfers_app` com `SELECT`/`INSERT`/`UPDATE` em `transfer`, `SELECT`/`INSERT` em `transfer_step` (sem `UPDATE`/`DELETE`), `SELECT`/`INSERT`/`UPDATE` em `outbox`; `grant` por tabela.

## 6. Eventos
- Envelope de `libs/outbox`, `eventVersion` 1, `aggregateId` = `transferId`.
- Payloads: `TransferCompleted` e `TransferCompensated`: `transferId`, `sourceAccountId`, `destinationAccountId`, `amount`, `description`; `TransferFailed`: os mesmos + `reason`.
- Tópico `transfers-events.fifo` (`MessageGroupId` = `transferId`, `MessageDeduplicationId` = `eventId`) e fila `transfers-events-inspection.fifo` (`RawMessageDelivery=true`), criados no init do LocalStack.
- Relay do `transfers` com chave de trava consultiva própria (banco próprio, mas a chave é distinta da do `accounts` por convenção).

## 7. API, nginx e Bruno
- Problem Details com `type` relativo `/problems/<slug>`; slugs novos: `same-account-transfer`, `account-not-found`, `accounts-unavailable`, `transfer-failed`, `transfer-not-found`, `entry-not-found` (no `accounts`).
- `Idempotency-Key` com as mesmas regras do `accounts` (1 a 255 caracteres, ausente → `400 missing-idempotency-key`).
- OpenAPI com springdoc.
- nginx: `/transfers` e `/transfers/**` → `host.docker.internal:8082`; `/transfers/actuator/` → `/actuator/`.
- Bruno: `api/bruno/transfers/` (criar transferência, repetir com a mesma chave, consultar) e a consulta por chave em `api/bruno/accounts/`.

## 8. Testes
- **Domínio:** um teste por passo (cada coluna da tabela da seção 3) e um teste que percorre todos os caminhos até um terminal.
- **Aplicação:** casos de uso com `AccountsGateway` falso (sucesso, recusa, falha passageira, timeout), relógio controlado e repositórios em memória; prazo vencido; perda do arrendamento.
- **Integração do `transfers`** (Testcontainers PostgreSQL + WireMock no lugar do `accounts`): classificação de cada resposta; tentativas rápidas para `503`; `RESOLVE_*`; arrendamento disputado entre requisição e agendador; retomada depois de reiniciar o contexto; idempotência do `POST`; outbox e relay com LocalStack; ArchUnit pelas regras de `libs/testing-architecture`.
- **`accounts`:** integração do endpoint de consulta por chave.
- **Ponta a ponta (`tests/e2e`):** os dois serviços como processos (`java -jar` dos `bootJar`), PostgreSQL e LocalStack por Testcontainers; cobre os critérios 3 a 7. A queda é simulada matando e reiniciando o processo; o prazo é reduzido por configuração no teste (por exemplo, 5 s).
- **Smoke do CI:** os dois jars, compose, transferência pelo nginx e eventos nas duas filas de inspeção.

## 9. Ambiente local e CI
- `.env.example` com as senhas do banco `transfers` e `TRANSFERS_ACCOUNTS_URL`.
- Guia [`rodando-localmente.md`](../../rodando-localmente.md) com o 2º `bootRun` e a sequência manual de transferência.
- CI conforme a seção 1, com `e2e` num job próprio.

## 10. Documentação que acompanha
- **ADR 0016** — saga orquestrada por HTTP síncrono com compensação e prazo (Clara).
- **`CONTEXT.md`** — Status da transferência (`TransferStatus`) e Passo da saga (`TransferStep`) (Clara).
- **`docs/arquitetura.md`** — `transfers` nas decisões tomadas; item da saga sai de "Em aberto"; ECS Service Connect fica em "Em aberto" para a etapa 8 (Clara).
- **`docs/roadmap.md`** — etapa 2 com o link para esta spec.
- **Board** — #17 fechado ao aprovar o ADR; cartão `Decision` do ECS Service Connect na etapa 8; tasks do plano como sub-issues de #9 (Helena).

## Fora de escopo (e onde fica registrado)
| Item | Quando | Registrado em |
|---|---|---|
| Reserva de limite diário na saga | etapa 6 | roadmap |
| Autenticação e autorização (quem pode transferir de qual conta) | etapa 7 | roadmap |
| Consumidores dos eventos de transferência | etapa 3 | roadmap |
| Listagem de transferências | etapa 4 (extrato) | roadmap |
| ECS Service Connect | etapa 8 | cartão `Decision` novo |
| Desbloqueio manual de compensação recusada | quando incomodar | cartão novo |
| Mover os filtros de correlation ID para `libs/observabilidade` | etapa 7 | [#43](https://github.com/capozziV/DigitalBanking/issues/43) |

## Riscos
- **Chamada em voo depois do prazo:** uma chamada que ainda estivesse no `accounts` quando a consulta pela chave roda poderia gravar depois. Com leitura de 3 s, `lock_timeout` de 2 s e prazo de 2 min, isso não acontece na prática; o `ResolveCredit` encontrar o lançamento tardio também está coberto (vai para `Completed`). Risco residual aceito e registrado no ADR 0016.
- **Compensação recusada fica em laço:** indica bug; aparece em log `ERROR` e na métrica de transferências pendentes antigas. Desbloqueio manual fica como cartão.
- **Tempo do CI:** o `e2e` sobe dois processos; o build seletivo evita rodá-lo em PRs só de docs.
- **Testcontainers com dois processos Java:** se os processos ficarem lentos no runner, o plano B é rodar o `e2e` contra o docker-compose do smoke.
