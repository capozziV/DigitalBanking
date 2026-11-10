# Etapa 4 — `statement` (extrato com CQRS no DynamoDB): design

- **Data:** 2026-11-10
- **Status:** rascunho, aguardando revisão do usuário
- **Roadmap:** etapa 4 de [`docs/roadmap.md`](../../roadmap.md)
- **Board:** cartão [#11](https://github.com/capozziV/DigitalBanking/issues/11) (Stage 4 · Statement)

## Objetivo
Entregar o serviço `statement`, o modelo de leitura do extrato (CQRS). Ele consome os eventos do `accounts` numa fila SQS FIFO com DLQ, grava cada lançamento no DynamoDB com `PutItem` condicional e expõe o extrato paginado por API. O valor para o portfólio é provar por teste que a projeção é idempotente sem tabela de controle (o próprio item é a prova), que a ordem do extrato é a ordem do ledger mesmo com entrega fora de ordem e que a leitura do extrato não toca o `accounts`.

## Decisões desta etapa (usuário, 2026-11-10)
| # | Decisão |
|---|---|
| 1 | Eventos consumidos: só `LedgerEntryRecorded` e `AccountOpened`, do tópico do `accounts`. Os eventos do `transfers` ficam fora; a transferência aparece no extrato pela `description` do lançamento. |
| 2 | Chave de ordenação: a `sequence` do lançamento (`N` no DynamoDB), no lugar de `timestamp#entryId` do ADR 0008. Registrado no ADR 0018. |
| 3 | O `statement` guarda a conta a partir do `AccountOpened`: conta desconhecida → `404`; conta sem lançamentos → lista vazia. |
| 4 | Paginação por cursor opaco, do lançamento mais novo para o mais antigo, 20 itens por padrão, no máximo 100. |
| 5 | Sem filtros nesta etapa. Filtro por intervalo de datas fica para quando o frontend pedir (cartão [#116](https://github.com/capozziV/DigitalBanking/issues/116)). |
| 6 | Cada item traz o `balanceAfter` do lançamento; a resposta não traz saldo atual no topo (o saldo oficial é o do `accounts`). |
| 7 | Rota `GET /accounts/{accountId}/statement`, com o nginx mandando esse caminho para o `statement`. |
| 8 | Cliente `DynamoDbClient` de baixo nível do AWS SDK v2, todo o código do DynamoDB num adapter só. |
| 9 | Tabela, fila e DLQ criadas pelo script de init do LocalStack (padrão da etapa 3); nos testes, pelo apoio de testes. Na AWS, Terraform na etapa 8. |
| 10 | Lançamentos anteriores ao `statement` não são importados. Reconstrução fica no cartão pós-MVP [#117](https://github.com/capozziV/DigitalBanking/issues/117). |
| 11 | Antes do `statement`, ajustar no `notification` os padrões que ele vai copiar (migrations em `adapters`, endpoint AWS vazio = AWS real, WARN sem stack trace, gauge da DLQ sem chamada bloqueante). Feito em paralelo à revisão desta spec (cartão [#113](https://github.com/capozziV/DigitalBanking/issues/113)). |
| 12 | Porta `8084`, sem PostgreSQL; fila `statement-accounts.fifo` com DLQ `statement-accounts-dlq.fifo` (5 tentativas, visibilidade 30 s, configuráveis). |

## Critério de pronto
- Um crédito, um débito e uma transferência aparecem no extrato das contas envolvidas, em ordem de `sequence`, poucos segundos depois de gravados (e2e com os quatro serviços).
- A mesma mensagem entregue várias vezes gera um item só (teste de integração com mutante: sem a condição do `PutItem`, o teste fica vermelho).
- Lançamentos entregues fora de ordem saem ordenados no extrato.
- Mensagem malformada vai para a DLQ sem travar as outras contas.
- Build, testes, ktlint e ArchUnit verdes; smoke e Bruno das quatro coleções verdes; CI com o job `statement` dentro do `ci-ok`.

## Restrições (de `CLAUDE.md` e ADRs)
- Clean Architecture: `domain` → `application` → `adapters` → `bootstrap`; `domain` sem Spring nem AWS SDK (ArchUnit, com as regras de `libs/testing-architecture`).
- Nenhuma classe de domínio compartilhada com o `accounts`: o `statement` tem o seu próprio modelo.
- Valores em centavos (`Long`).
- Consumidor idempotente (ADR 0003); eventos chegam pelo outbox do `accounts` (ADR 0015).
- DTOs em `adapters/web/dto/` com sufixo `DTO`; erros em Problem Details (ADR 0011); correlation ID propagado do evento para o log.
- Código em inglês com os nomes do `CONTEXT.md`.

## 1. Frentes de trabalho
| Frente | Quem | Depende de |
|---|---|---|
| A. Infra: módulos no `settings.gradle.kts`, LocalStack (tabela, fila, DLQ, assinatura), compose, nginx, job `statement` no CI | Rafael (`infra`) | — |
| B. `domain` e `application` | Marina (`dev-kotlin`) | — |
| C. `adapters` (DynamoDB, SQS, web) e `bootstrap` | Marina (`dev-kotlin`) | B (portas), A (LocalStack nos testes) |
| D. e2e, smoke e coleção Bruno | Marina (`dev-kotlin`) | C |
| E. `CONTEXT.md`, `docs/arquitetura.md`, `docs/rodando-localmente.md` | Clara (`documentador`) | — |

A e B correm em paralelo; o Rafael é o único a editar `settings.gradle.kts` e `build-logic/`.

## 2. Módulos e camadas
`services/statement/{domain,application,adapters,bootstrap}`, pacote `io.github.capozziv.digitalbanking.statement`.
- **domain:** `StatementEntry`, `EntryKind` (próprio do serviço), `StatementPage`, `StatementCursor`.
- **application:** portas `StatementRepository`; casos de uso `RecordStatementEntryUseCase`, `RegisterAccountUseCase`, `GetStatementUseCase`; exceções `AccountNotFoundException`, `ConflictingStatementEntryException`.
- **adapters:** `persistence/DynamoDbStatementRepository`; `messaging/StatementEventListener` (`@SqsListener`) e o mapeamento do payload; `web/StatementController` com `dto/StatementDTO` e `dto/StatementEntryDTO`.
- **bootstrap:** aplicação, `DynamoDbClient` e configuração do listener; endpoint AWS vazio = AWS real (mesma regra dos relays depois do ajuste do #113).

## 3. Modelo de domínio
- **`StatementEntry`:** `entryId` (UUID), `accountId` (UUID), `sequence` (`Long` ≥ 1), `kind` (`CREDIT`/`DEBIT`), `amount` (centavos, > 0), `balanceAfter` (centavos, pode ser negativo), `description?` (1–140), `createdAt` (`Instant`).
- **`StatementPage`:** lista de `StatementEntry` em ordem decrescente de `sequence` e `nextCursor?`.
- **`StatementCursor`:** a última `sequence` devolvida. Na API vira texto opaco (base64url); cursor inválido → `400`.

## 4. Tabela no DynamoDB
Tabela única `statement`, chave `accountId` (`S`, partição) + `sequence` (`N`, ordenação).
- **Item de lançamento:** `sequence` ≥ 1 e os campos de `StatementEntry`.
- **Item da conta:** `sequence = 0`, com `customerId` e `openedAt`. Fica fora da listagem porque a consulta usa `sequence > 0`.
- Listagem: `Query` com `accountId = :id AND sequence > 0` (ou `< :cursor`), `ScanIndexForward = false`, `Limit = limit`. Se a página vier vazia sem cursor, `GetItem` do item da conta decide entre `404` e lista vazia.

## 5. Fluxo de consumo
1. O `StatementEventListener` recebe a mensagem de `statement-accounts.fifo` (grupo FIFO por conta, então `AccountOpened` chega antes dos lançamentos da conta).
2. `LedgerEntryRecorded` v1 → `RecordStatementEntryUseCase` → `PutItem` com `ConditionExpression attribute_not_exists(accountId)`.
   - Gravou → confirma.
   - `ConditionalCheckFailedException` → `GetItem` da mesma chave: mesmo `entryId` = repetição, confirma sem efeito; `entryId` diferente = inconsistência, lança `ConflictingStatementEntryException` (log ERROR, a mensagem vai para a DLQ depois de 5 tentativas).
3. `AccountOpened` v1 → `RegisterAccountUseCase` → `PutItem` condicional do item da conta (repetição confirma sem efeito).
4. Outro tipo de evento (ex.: `AccountTypeChanged`) ou versão desconhecida → log INFO e confirma.
5. Payload malformado → falha, volta para a fila e vai para a DLQ na 5ª tentativa.

Sem tabela `processed_events`: a chave do item é a idempotência (ADR 0018).

## 6. API, nginx e Bruno
`GET /accounts/{accountId}/statement?cursor=…&limit=…`
- `200` `{accountId, entries: [{entryId, sequence, kind, amount, balanceAfter, description, createdAt}], nextCursor}`; `nextCursor` nulo na última página.
- `400 validation-error`: `accountId` não é UUID, `limit` fora de 1..100, cursor inválido.
- `404 account-not-found`: conta que o `statement` não conhece.
- nginx: `location ~ ^/accounts/[^/]+/statement$` para `host.docker.internal:8084`, antes do `location /accounts/`; actuator em `/statement/actuator/`.
- OpenAPI com Problem Details e a coleção `api/bruno/statement`.

## 7. Operação
- Métricas: gauge da DLQ no padrão do `notification` (valor guardado, atualizado por agendamento).
- Logs com o correlation ID do evento no MDC.
- Comando documentado para devolver mensagens da DLQ à fila (mesmo do `notification`).

## 8. Testes
- **Unidade:** domínio (validações, cursor), casos de uso com `InMemoryStatementRepository`.
- **Integração (LocalStack via `libs/testing-localstack`):**
  - repositório: grava, repete (um item só), conflito de `entryId`, paginação nas duas direções do cursor, item da conta fora da listagem;
  - consumo: 20 entregas da mesma mensagem geram um item (com mutante), entrega fora de ordem sai ordenada, payload malformado vai para a DLQ, `AccountTypeChanged` é confirmado sem efeito;
  - API: `200`, `400`, `404`, lista vazia.
- **ArchUnit:** regras de `libs/testing-architecture`.
- **e2e (`tests/e2e`):** abrir conta, crédito, débito e transferência; o extrato das duas contas mostra os lançamentos em ordem.

## 9. Ambiente local e CI
- LocalStack: init em `infra/local/localstack/init/ready.d` cria a tabela, a fila, a DLQ e a assinatura do tópico do `accounts`.
- `bootRun` do `statement` na porta 8084; `docs/rodando-localmente.md` atualizado.
- `infra/local/smoke.sh` confere o extrato depois de um crédito.
- CI: job `statement` no build seletivo, incluído no `ci-ok`; o e2e passa a subir os quatro serviços.

## 10. Documentação que acompanha
- ADR 0018 (chave pela `sequence`, idempotência pelo item, cliente de baixo nível) e ajuste do ADR 0008.
- `CONTEXT.md`: termo novo **Lançamento do extrato** (`StatementEntry`).
- `docs/arquitetura.md`: linha do `statement` (só DynamoDB nesta etapa; Redis na etapa 5).

## Fora de escopo (e onde fica registrado)
- Filtros por data ou tipo: cartão #116 (etapa 9).
- Cache da primeira página: etapa 5.
- Autenticação e dono da conta: etapa 7.
- Reconstrução do extrato a partir do `accounts`: cartão pós-MVP [#117](https://github.com/capozziV/DigitalBanking/issues/117).
- Terraform da tabela e da fila: etapa 8.

## Riscos
- LocalStack com DynamoDB, SNS e SQS no mesmo contêiner pode demorar mais a ficar pronto; o apoio de testes já usa 3 min de prazo.
- Consistência eventual: o extrato pode ficar atrás do ledger por alguns instantes; o e2e espera por condição, sem `sleep`.
- Buraco na `sequence` (evento perdido) não é detectado nesta etapa; o outbox garante a entrega e a DLQ mostra a falha.
