# Etapa 1 — `accounts`: design

- **Data:** 2026-10-22
- **Status:** aprovada (2026-10-22)
- **Roadmap:** etapa 1 de [`docs/roadmap.md`](../../roadmap.md) (com o outbox, o relay e o SNS antecipados da etapa 3)

## Objetivo
Entregar o `accounts` com regra de negócio real: abrir conta, creditar, debitar com cheque especial, trocar tipo de conta e ajustar os limites por tipo, com concorrência e idempotência provadas por teste. Cada fato do ledger vira um evento publicado num tópico SNS FIFO pelo Transactional Outbox, mesmo sem consumidor ainda.

## Critério de pronto
1. `./gradlew build` verde (compilação, ktlint, unitários, `integrationTest`, ArchUnit).
2. Jobs `build` e `smoke` do CI verdes no GitHub Actions.
3. Teste de concorrência: conta `FREE` com um crédito de 3700; 100 débitos paralelos de 100 → exatamente 37 aprovados e 63 recusados, saldo final 0, `sequence` de 1 a 38 sem buraco (o crédito e os 37 débitos).
4. Teste de idempotência: 20 pedidos paralelos com a mesma `Idempotency-Key` → um único lançamento.
5. Teste com LocalStack: eventos de duas contas chegam à fila FIFO de inspeção em ordem por conta, com duas instâncias do relay rodando juntas, sem duplicata nem evento fora de ordem.
6. Coleção Bruno com todos os endpoints; o smoke do CI abre conta, credita, debita e consulta pelo nginx.

## Restrições (de `CLAUDE.md` e ADRs)
- Conta e ledger na mesma transação; ledger imutável; saldo para débito nunca de cache ([ADR 0007](../../adr/0007-conta-e-ledger-no-accounts.md)).
- Saldo no lançamento, trava `FOR UPDATE` na conta, `lock_timeout`, uma conta por transação ([ADR 0012](../../adr/0012-saldo-no-lancamento-e-trava-pessimista.md)).
- Spring JDBC com `JdbcClient` ([ADR 0013](../../adr/0013-persistencia-com-spring-jdbc.md)).
- Cheque especial por tipo de conta, limites em tabela, `/admin` aberto até a etapa 7 ([ADR 0014](../../adr/0014-tipo-de-conta-e-cheque-especial.md)).
- Eventos só via Transactional Outbox; um tópico SNS por produtor ([ADR 0003](../../adr/0003-transactional-outbox-e-mensageria.md)).
- OpenAPI, Problem Details, `Idempotency-Key` ([ADR 0011](../../adr/0011-contratos-de-api.md)).
- Código em inglês com os nomes do `CONTEXT.md`; valores em centavos (`Long`).
- **Convenção de nomes (nova):** classe com papel técnico leva o papel como sufixo (`OpenAccountUseCase`, `AccountRepository`, `JdbcAccountRepository`, `AccountController`, `SnsEventPublisher`, `OutboxRelay`, `JdbcOutboxWriter`); classe de modelo de domínio não leva sufixo (`Account`, `LedgerEntry`, `Balance`).
- Sem autenticação (etapa 7) e sem o serviço `customer`: `customerId` é uma referência opaca, sem validação.

## 1. Módulos e camadas
```
build-logic/                       # + plugin de convenção do source set integrationTest
libs/
└── outbox/                        # novo: envelope, writer JDBC, relay, publicador SNS
services/accounts/
├── domain/                        # Account, AccountType, LedgerEntry, value objects, eventos de domínio
├── application/                   # *UseCase e portas
├── adapters/                      # web (controllers, Problem Details, OpenAPI, correlation ID) e persistence (JdbcClient, Flyway)
└── bootstrap/                     # aplicação Spring, configuração, ArchUnit
infra/local/                       # + LocalStack, init do SNS/SQS, dois usuários de banco, nginx sem tirar o prefixo da API
api/bruno/accounts/                # coleção Bruno
```
- **`domain`:** Kotlin puro. As operações devolvem o lançamento e o evento de domínio.
- **`application`:** um caso de uso por operação (`OpenAccountUseCase`, `GetAccountUseCase`, `RecordCreditUseCase`, `RecordDebitUseCase`, `ChangeAccountTypeUseCase`, `ListAccountTypesUseCase`, `ChangeOverdraftLimitUseCase`). Portas: `AccountRepository`, `LedgerEntryRepository`, `AccountTypeRepository`, `EventOutbox`, `TransactionRunner` (mantém `@Transactional` fora desta camada). Sem Spring.
- **`libs/outbox`:** só infraestrutura ([ADR 0002](../../adr/0002-clean-architecture-por-servico.md)). Não depende de nenhum serviço. O serviço fornece só o tipo e o payload de cada evento.
- **Caminho Gradle:** `:libs:outbox`. Pacote `io.github.capozziv.digitalbanking.outbox`.
- **Source set `integrationTest`:** plugin de convenção no `build-logic`. `./gradlew test` roda só unitários, sem Docker; `./gradlew integrationTest` roda os de integração; `./gradlew build` roda os dois.

## 2. Modelo de domínio
- **`Account`:** `id`, `customerId`, `type: AccountType`. Não guarda saldo. Recebe o último lançamento e o limite do tipo para decidir:
  - `credit(amount, description, idempotencyKey)` → novo `LedgerEntry` (`sequence` n+1, `balanceAfter` = anterior + valor) e `LedgerEntryRecorded`;
  - `debit(...)` → o mesmo, depois de conferir `AvailableBalance ≥ amount`; se não passar, devolve a recusa com o saldo disponível atual;
  - `changeType(newType)` → `AccountTypeChanged`, ou nenhum evento se o tipo for o mesmo.
- **`AccountType`:** enum `FREE`, `STANDARD`, `GOLD`, `BLACK`, `DIAMOND`. A lista de tipos vive só no código; o limite de cada tipo vem da tabela, pela porta `AccountTypeRepository`.
- **`LedgerEntry`:** imutável: `id`, `accountId`, `sequence`, `kind` (`CREDIT`/`DEBIT`), `amount`, `balanceAfter`, `description`, `idempotencyKey`, `createdAt`.
- **Value objects:** `Money` (centavos, `Long`, > 0 para valores de lançamento), `Balance` (pode ser negativo), `OverdraftLimit` (≥ 0), `AvailableBalance` (saldo + limite).
- **Recusa como resultado, não exceção:** `debit` devolve um tipo selado (`Recorded` | `Refused`). Exceções ficam para erros de verdade (conta inexistente, validação, timeout da trava).
- **Eventos de domínio:** `AccountOpened`, `LedgerEntryRecorded`, `AccountTypeChanged`, no `domain`. O caso de uso os converte no envelope do `libs/outbox`.
- **Conta nova:** nasce `FREE`, sem lançamentos (saldo 0 implícito); o primeiro lançamento tem `sequence` 1.

## 3. Esquema do banco
**`account_type`** (limites por tipo)
- `type` (PK, `text`, validado pelo enum do código, sem `CHECK` repetindo a lista), `overdraft_limit` (`bigint`, `CHECK ≥ 0`), `updated_at`.
- Migration semeia, em centavos: `FREE` 0, `STANDARD` 50000, `GOLD` 200000, `BLACK` 1000000, `DIAMOND` 5000000. Tipo novo = valor novo no enum + migration semeando a linha.

**`account`**
- `id` (`uuid`), `customer_id` (`uuid`, `UNIQUE`: uma conta por cliente), `type` (FK → `account_type`), `opening_idempotency_key` (`UNIQUE`), `created_at`, `updated_at`.

**`ledger_entry`**
- `id` (`uuid`), `account_id` (FK), `sequence` (`bigint`), `kind` (`CHECK` em `CREDIT`/`DEBIT`), `amount` (`bigint`, `CHECK > 0`), `balance_after` (`bigint`), `description` (`varchar(140)`, opcional), `idempotency_key`, `created_at`.
- `UNIQUE (account_id, sequence)` e `UNIQUE (account_id, idempotency_key)`. O primeiro serve também para buscar o último lançamento (`ORDER BY sequence DESC LIMIT 1`).

**`outbox`** (migration modelo fornecida pelo `libs/outbox`, copiada para as migrations do serviço)
- `position` (`bigserial`, PK, ordem de gravação), `event_id` (`uuid`, `UNIQUE`), `aggregate_id`, `event_type`, `event_version`, `payload` (`jsonb`), `correlation_id`, `occurred_at`, `published_at` (nulo até publicar), `attempts` (padrão 0), `last_error`.
- `NOT NULL` nas colunas que o writer sempre preenche: `event_id`, `aggregate_id`, `event_type`, `event_version`, `payload`, `occurred_at`. Aceitam nulo: `correlation_id`, `published_at` e `last_error`.
- Índice parcial `(position) WHERE published_at IS NULL`. As linhas publicadas ficam para sempre.

**Usuários de banco** (criados pelo script de init do PostgreSQL)
- `accounts_owner`: dono das tabelas, usado pelo Flyway (`spring.flyway.user`/`password`).
- `accounts_app`: usado pela aplicação. `SELECT`/`INSERT` em `ledger_entry` e `outbox` (+ `UPDATE` em `outbox` para o relay), `SELECT`/`INSERT`/`UPDATE` em `account` e `account_type`. **Sem `UPDATE` nem `DELETE` em `ledger_entry`:** a imutabilidade do ledger é garantida pelo PostgreSQL.

**IDs:** UUID v7 gerados na aplicação (ordenáveis por tempo; o domínio conhece o `id` antes de gravar).

## 4. Fluxos
**Débito** (`RecordDebitUseCase`; o crédito é igual, sem a checagem de saldo), numa transação:
1. `SET LOCAL lock_timeout` com o valor de configuração (padrão `2s`).
2. `SELECT … FROM account WHERE id = ? FOR UPDATE`. Conta inexistente → `404`. Trava estourada → `503`.
3. Com a trava na mão, busca lançamento com `(account_id, idempotency_key)`:
   - mesmo `kind` e `amount` → devolve o lançamento original com `200`, sem gravar;
   - `kind` ou `amount` diferente → `422 idempotency-key-reused`.
4. Lê o último lançamento (saldo e `sequence` n) e o limite do tipo da conta.
5. `account.debit(...)`:
   - `Refused` → `422 insufficient-available-balance`, nada gravado, log `INFO` e métrica;
   - `Recorded` → `INSERT` do lançamento (`sequence` n+1) e `INSERT` do `LedgerEntryRecorded` no outbox.
6. Commit e `201`. Violação de `UNIQUE` neste ponto indica bug (a trava deveria impedir): erro interno com log `ERROR`.

**Abrir conta** (`OpenAccountUseCase`): `INSERT` em `account` com `FREE` e a `opening_idempotency_key`, mais `AccountOpened` no outbox.
- Conflito na `opening_idempotency_key` → relê e devolve a conta com `200` (cobre tentativas simultâneas com a mesma chave).
- Conflito no `customer_id` com outra chave → `409 customer-already-has-account`.

**Trocar tipo** (`ChangeAccountTypeUseCase`): trava a conta; mesmo tipo → `200` sem evento; senão `UPDATE` e `AccountTypeChanged` no outbox. Downgrade aceito mesmo com o saldo abaixo do novo limite.

**Alterar limite** (`ChangeOverdraftLimitUseCase`): `UPDATE account_type` com `updated_at`, sem evento e sem trava de conta. Débitos em andamento usam o valor que leram. Tipo desconhecido → `404`.

**Consultar** (`GetAccountUseCase`): conta, último lançamento e limite do tipo, sem trava, sempre do banco.

**Correlation ID:** filtro mínimo no adapter web do `accounts` lê `X-Correlation-Id` ou gera um UUID, põe no MDC e devolve no header da resposta. O `JdbcOutboxWriter` lê do MDC. O filtro muda para a `libs/observabilidade` na etapa 7.

## 5. Outbox e relay (`libs/outbox`)
**Envelope** (JSON publicado no SNS):
```json
{
  "eventId": "…", "eventType": "LedgerEntryRecorded", "eventVersion": 1,
  "occurredAt": "2026-10-22T14:31:00Z", "aggregateId": "…",
  "correlationId": "…", "payload": { … }
}
```
Payloads `eventVersion` 1:
- `AccountOpened`: `accountId`, `customerId`, `type`.
- `LedgerEntryRecorded`: `entryId`, `accountId`, `sequence`, `kind`, `amount`, `balanceAfter`, `description`, `idempotencyKey`, `createdAt`.
- `AccountTypeChanged`: `accountId`, `previousType`, `newType`.

**Gravação:** a porta `EventOutbox` fica na camada `application` do `accounts` e é implementada pelo `OutboxEventAppender` do adapter de persistência, que chama o `JdbcOutboxWriter` da lib (a lib não depende do serviço). A gravação usa a mesma conexão e transação do caso de uso. Preenche `eventId` (UUID v7), `occurredAt` e `correlationId`.

**Ciclo do `OutboxRelay`** (`@Scheduled`, intervalo configurável, padrão 500 ms):
1. Abre transação e tenta `pg_try_advisory_xact_lock(<chave do relay do serviço>)`. Sem a trava, encerra o ciclo. A trava se solta no fim da transação.
2. Lê até 100 linhas (configurável) com `published_at IS NULL`, `ORDER BY position`.
3. Para cada linha, em ordem:
   - conta (`aggregate_id`) que já falhou neste ciclo → pula;
   - publica no SNS FIFO com `MessageGroupId = aggregateId`, `MessageDeduplicationId = eventId` e atributos `eventType` e `correlationId`;
   - sucesso → `published_at = now()`;
   - falha → `attempts + 1`, `last_error`, conta marcada como bloqueada no ciclo; com `attempts` ≥ 10 (configurável), log `ERROR`.
4. Commit.

- Publicado e não confirmado (queda antes do commit) → sai de novo no próximo ciclo; a deduplicação FIFO (5 min) e os consumidores idempotentes absorvem.
- **`SnsEventPublisher`:** AWS SDK for Java v2 (`SnsClient`) direto; endpoint e região por configuração (LocalStack no ambiente local e nos testes).
- **Recursos:** tópico `accounts-events.fifo` e fila `accounts-events-inspection.fifo` assinada nele (sem consumidor; para inspeção manual e testes), criados pelo script de init do LocalStack.
- **Métricas Micrometer:** eventos pendentes, idade do pendente mais antigo, falhas de publicação. `/actuator/metrics` passa a ser exposto.
- O relay **não** afeta o `/actuator/health`.
- Desligamento: o ciclo em andamento termina antes de a aplicação parar.

## 6. API
**Caminhos.** O nginx deixa de remover o prefixo da API: `/accounts/**` e `/admin/account-types/**` vão ao serviço como estão. Só o actuator continua sem o prefixo (`/accounts/actuator/…` → `/actuator/…`), e o smoke da etapa 0 segue igual.

| Endpoint | Sucesso | Erros |
|---|---|---|
| `POST /accounts` (`Idempotency-Key`) `{customerId}` | `201` conta nova; `200` nova tentativa | `400`, `409 customer-already-has-account` |
| `GET /accounts/{id}` | `200` `{id, customerId, type, balance, overdraftLimit, availableBalance}` | `404 account-not-found` |
| `POST /accounts/{id}/credits` (`Idempotency-Key`) `{amount, description?}` | `201` lançamento; `200` nova tentativa | `400`, `404`, `422 idempotency-key-reused`, `503 account-busy` |
| `POST /accounts/{id}/debits` (`Idempotency-Key`) `{amount, description?}` | `201`; `200` | os mesmos + `422 insufficient-available-balance` (com `availableBalance`) |
| `PUT /accounts/{id}/type` `{type}` | `200` conta | `400`, `404`, `503 account-busy` |
| `GET /admin/account-types` | `200` `[{type, overdraftLimit, updatedAt}]` | — |
| `PUT /admin/account-types/{type}` `{overdraftLimit}` | `200` | `400`, `404 account-type-not-found` |

- Valores em centavos, inteiros. JSON em camelCase. `amount` > 0; `description` até 140 caracteres; só real, sem campo de moeda.
- `Idempotency-Key`: 1 a 255 caracteres (UUID recomendado). Ausente → `400 missing-idempotency-key`. Escopo da chave: a conta (crédito/débito) ou a abertura de conta.
- Problem Details (RFC 9457) com `type` relativo `/problems/<slug>`. `400 validation-error` lista os campos inválidos. `503 account-busy` com `Retry-After: 1`.
- Resposta do lançamento: `{id, accountId, sequence, kind, amount, balanceAfter, description, createdAt}`.
- **OpenAPI** gerado do código com springdoc, se houver versão compatível com o Spring Boot 4.1 (verificado na primeira task do bloco 1). Se não houver, contrato em YAML escrito à mão e validado num teste.
- **Coleção Bruno** em `api/bruno/accounts/`: uma requisição por endpoint, `Idempotency-Key` gerada a cada envio.

## 7. Testes
**Unitários** (`test`):
- Domínio: crédito e débito com `sequence` e `balanceAfter`; débito dentro e fora do cheque especial; recusa com o saldo disponível; conta abaixo do limite após downgrade (débito recusado, crédito aceito); troca para o mesmo tipo sem evento.
- Casos de uso com portas falsas (fakes escritos à mão): nova tentativa → `200` sem gravar; chave reaproveitada com outro valor → erro; cada operação grava o evento certo no outbox.
- Relay com publicador falso: ordem por conta; falha numa conta não bloqueia as outras; `attempts` e `last_error`; log `ERROR` a partir do limite.

**Integração** (`integrationTest`, Testcontainers):
- PostgreSQL: SQL dos repositórios, migrations, `accounts_app` recebe erro em `UPDATE`/`DELETE` de `ledger_entry`.
- Concorrência e idempotência: os testes 3 e 4 do critério de pronto.
- Trava: outra transação segura a conta → o débito recebe `503` em cerca de 2 s.
- Web: status e corpo Problem Details de cada endpoint.
- LocalStack: o teste 5 do critério de pronto.

**ArchUnit** (no `bootstrap`):
- Camadas como na etapa 0, **sem `allowEmptyShould`**, cada regra vista falhando uma vez.
- `adapters.web` → `adapters.persistence` e `adapters.persistence` → `adapters.web` em testes separados.
- `libs/outbox` não depende de nenhum pacote de serviço.
- Classes no pacote de casos de uso terminam em `UseCase`; classes anotadas como controller terminam em `Controller`.
- Melhorias da etapa 0: asserção AssertJ de mapa no teste de health (sem cast); o teste de virtual threads passa a verificar que uma requisição roda numa virtual thread, não só a propriedade.

## 8. Ambiente local e CI
- **Compose:** + `localstack` (imagem `localstack/localstack`, versão fixa, `LOCALSTACK_AUTH_TOKEN` do `.env`), com script de init criando o tópico e a fila FIFO. `.env.example` ganha a variável vazia e o aviso de que ela é necessária.
- **PostgreSQL:** script de init cria `accounts_owner` e `accounts_app`. Exige `down -v` para recriar.
- **CI `build`:** recebe o segredo `LOCALSTACK_AUTH_TOKEN` para os testes com LocalStack.
- **CI `smoke`:** sobe o compose com LocalStack; pelo nginx abre conta, credita, debita e consulta, conferindo os valores.

## 9. Entrega em dois blocos
1. **Bloco 1 — `accounts` gravando no outbox** (PR próprio)
   - Primeira task: confirmar a versão do springdoc compatível com o Spring Boot 4.1 (fonte primária).
   - Source set `integrationTest`; ArchUnit (regras novas e sem `allowEmptyShould`).
   - Domínio, casos de uso, persistência, web, correlation ID, as sete operações.
   - `libs/outbox` só com envelope e `JdbcOutboxWriter` (sem relay); porta `EventOutbox` no `application` e `OutboxEventAppender` no adapter de persistência do `accounts`.
   - Dois usuários de banco; nginx; coleção Bruno; smoke com as operações.
2. **Bloco 2 — relay e SNS FIFO** (PR próprio)
   - Primeira task: teste de SNS FIFO → SQS FIFO no LocalStack (ordem por `MessageGroupId`, deduplicação). Se o LocalStack não suportar, parar e levar a decisão ao usuário.
   - `OutboxRelay`, `SnsEventPublisher`, métricas, LocalStack no compose e no CI.

**Pré-requisito do bloco 2 (com o usuário):** conta Hobby no LocalStack, token no `infra/local/.env` e segredo `LOCALSTACK_AUTH_TOKEN` no GitHub.

## 10. Documentação que acompanha
- **ADR 0015 (novo):** outbox e relay — relay no próprio serviço, um relay ativo por trava consultiva, SNS FIFO com `MessageGroupId` por conta, falha bloqueando só a conta, outbox guardado para sempre, envelope com `eventVersion`, LocalStack como emulador. Alternativas: `SKIP LOCKED` com vários relays (quebra a ordem por conta), relay separado, CDC, tópico Standard, CloudEvents.
- **ADR 0003:** referência ao 0015; relay decidido. **ADR 0014:** lista de tipos no enum, limites na tabela. **ADR 0001:** LocalStack escolhido.
- **`docs/arquitetura.md`:** remover de "Em aberto" o emulador AWS e o relay; registrar idempotência por chave no lançamento.
- **`CONTEXT.md`:** novos termos Valor (`Money`) e Tipo de lançamento (`EntryKind`).
- **`CLAUDE.md`:** convenção de sufixos nas regras de código.
- **`docs/roadmap.md`:** outbox, relay, SNS e LocalStack na etapa 1; a etapa 3 fica com SQS, DLQ, `notification` e consumidores idempotentes.

## Fora de escopo (e onde fica registrado)
| Item | Quando | Registrado em |
|---|---|---|
| Cache no Redis com TTL das respostas de idempotência (mesma resposta para recusa repetida) | se algum consumidor precisar | `status.md` → débito técnico |
| Desbloqueio manual de conta presa no relay (evento com falha permanente) | quando incomodar | `status.md` → débito técnico |
| Vários relays divididos por conta | se a vazão exigir | ADR 0015 |
| Mover o filtro de correlation ID para `libs/observabilidade` | etapa 7 | `roadmap.md` |
| Proteger `/admin` com grupo de administradores | etapa 7 | `roadmap.md` |
| Listar lançamentos (extrato) | etapa 4 (`statement`) | `roadmap.md` |
| Encerrar ou bloquear conta | sem regra definida | `status.md` → débito técnico |
| Juros, histórico de troca de tipo, auditoria de limite, limite diário por tipo | pós-MVP | `status.md` (já registrado) |

## Riscos
- **LocalStack e SNS FIFO:** a documentação não confirma o suporte a FIFO. Mitigação: primeira task do bloco 2.
- **springdoc e Spring Boot 4.1:** compatibilidade não confirmada. Mitigação: primeira task do bloco 1, com plano B em YAML.
- **Plano gratuito do LocalStack:** só uso não comercial, dentro da política de uso justo no CI; se mudar, a alternativa aberta (MiniStack, Moto) volta à mesa.
- **Escopo:** a etapa 1 absorveu parte da etapa 3. Mitigação: dois blocos com PRs independentes.
