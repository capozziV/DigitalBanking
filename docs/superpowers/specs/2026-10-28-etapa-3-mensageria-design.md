# Etapa 3 — Mensageria e `notification`: design

- **Data:** 2026-10-28
- **Status:** aprovada (2026-10-28)
- **Roadmap:** etapa 3 de [`docs/roadmap.md`](../../roadmap.md)
- **Board:** cartão [#10](https://github.com/capozziV/DigitalBanking/issues/10) (Stage 3 · Messaging)

## Objetivo
Entregar o primeiro consumidor de eventos, o serviço `notification`. Ele lê os eventos do `accounts` e do `transfers` em filas SQS FIFO, cada uma com sua DLQ, e grava notificações dentro do aplicativo, que o cliente consulta pela API (o sininho do frontend chega na etapa 9). O valor para o portfólio é provar por teste que o consumidor é idempotente: a mesma mensagem entregue várias vezes gera uma notificação só, e uma mensagem com defeito vai para a DLQ sem travar as outras. A etapa também fecha débitos técnicos do relay do outbox e da etapa 2.

## Decisões desta etapa (usuário, 2026-10-28)
| # | Decisão |
|---|---|
| 1 | Notificação dentro do aplicativo: o `notification` guarda as notificações e as expõe por API, para um sininho no frontend (cartão [#102](https://github.com/capozziV/DigitalBanking/issues/102), etapa 9). Sem e-mail ou SMS. |
| 2 | Eventos notificados: `AccountOpened` (boas-vindas com instruções), `AccountTypeChanged` (novo tipo e novo limite de cheque especial), `TransferCompleted` (para quem enviou e para quem recebeu), `TransferFailed` e `TransferCompensated` (para quem enviou). Entrada no cheque especial fica para depois (cartão [#101](https://github.com/capozziV/DigitalBanking/issues/101)). |
| 3 | Banco PostgreSQL próprio (`notification`), no mesmo contêiner dos outros. Muda o "sem banco" de `docs/arquitetura.md` (ADR 0017). |
| 4 | Consumo com `@SqsListener` do Spring Cloud AWS. |
| 5 | Uma fila FIFO por tópico, cada uma com sua DLQ FIFO: `notification-accounts.fifo` e `notification-transfers.fifo`. A mensagem vai para a DLQ na 5ª tentativa; visibilidade de 30 s. Os dois valores são configuráveis. |
| 6 | Conta → cliente vem só de uma cópia local alimentada pelo `AccountOpened`. Evento de conta ainda desconhecida falha e volta para a fila. Sem consulta HTTP ao `accounts`. |
| 7 | `AccountTypeChanged` passa a levar `overdraftLimit` (`eventVersion` 2). |
| 8 | API: lista paginada de 20, mais recentes primeiro, com a contagem de não lidas, e um endpoint para marcar como lidas. Apresentação (5 visíveis, rolagem) e regra de leitura serão validadas no frontend (cartão #102). |
| 9 | Texto em português montado no consumo e gravado com a notificação. O texto de boas-vindas é provisório e será revisto com o frontend. |
| 10 | Evento de tipo desconhecido: log e confirmação. Só vai para a DLQ a mensagem malformada ou cujo processamento falha. |
| 11 | DLQ: métrica com a quantidade de mensagens paradas e comando documentado para devolvê-las à fila. Sem endpoint de administração. |
| 12 | Entram na etapa os débitos #92, #24, #26 e #93–#96. Os itens #23 e #56 vão para a etapa 7 (precisam do `/admin` protegido). |

## Critério de pronto
1. Abrir uma conta gera uma notificação de boas-vindas para o cliente dela.
2. Trocar o tipo da conta gera uma notificação com o novo tipo e o novo limite.
3. Uma transferência concluída gera 2 notificações (enviada e recebida); a mesma mensagem entregue 20 vezes continua gerando 2.
4. Transferência recusada e transferência compensada geram uma notificação cada para quem enviou.
5. Uma mensagem que sempre falha vai para a DLQ depois de 5 tentativas, e as mensagens dos outros grupos continuam sendo processadas.
6. Com o `notification` parado, os eventos se acumulam na fila; ao voltar, ele consome tudo sem perda nem duplicata.
7. Smoke, coleção Bruno, CI (job `notification` no CI seletivo) e `tests/e2e` cobrem o serviço.
8. `./gradlew build --rerun-tasks` verde com LocalStack, 0 falhas e 0 pulados; ktlint e ArchUnit verdes.

## Restrições (de `CLAUDE.md` e ADRs)
- Clean Architecture com um módulo Gradle por camada; o `domain` não depende de Spring nem de infraestrutura (ArchUnit, regras de `libs/testing-architecture`).
- Código em inglês com os nomes de `CONTEXT.md`; valores em centavos (`Long`).
- Consumidores sempre idempotentes ([ADR 0003](../../adr/0003-transactional-outbox-e-mensageria.md)).
- Nenhuma classe de domínio compartilhada entre serviços: o `notification` lê o envelope JSON, não importa tipos do `accounts` nem do `transfers`.
- Persistência com `JdbcClient` e SQL explícito ([ADR 0013](../../adr/0013-persistencia-com-spring-jdbc.md)); toda tabela com `grant` próprio para o usuário da aplicação.
- Correlation ID propagado do atributo da mensagem para o MDC ([ADR 0006](../../adr/0006-observabilidade-com-correlation-id.md)).
- Problem Details e OpenAPI na API ([ADR 0011](../../adr/0011-contratos-de-api.md)).

## 1. Frentes de trabalho
A etapa tem duas frentes independentes, que podem andar em paralelo:
- **Frente A — `notification`:** prova do `@SqsListener`, serviço, filas, mudança no `AccountTypeChanged`, API, infraestrutura, testes ponta a ponta.
- **Frente B — débitos:** relay do outbox (#92, #24, #26) e lacunas de teste do `transfers` (#93–#96).

Arquivos compartilhados entre as frentes (`settings.gradle.kts`, `gradle/libs.versions.toml`, workflow do CI) têm a divisão combinada no plano.

## 2. Módulos e camadas
`services/notification/{domain,application,adapters,bootstrap}`, pacote raiz `io.github.capozziv.digitalbanking.notification`, porta 8083.
- **domain:** `Notification`, `NotificationKind`, montagem dos textos (`NotificationText` ou equivalente), regras de leitura.
- **application:** casos de uso por evento (`RecordWelcomeNotificationUseCase` etc., ou um caso de uso por família de evento), `ListNotificationsUseCase`, `MarkNotificationsReadUseCase`; portas `NotificationRepository`, `KnownAccountRepository`, `ProcessedEventRepository`.
- **adapters:** `messaging/` (listeners `@SqsListener` e leitura do envelope), `persistence/` (`Jdbc…Repository`), `web/` (controller e DTOs em `web/dto/` com sufixo `DTO`).
- **bootstrap:** aplicação Spring Boot, configuração das filas, métricas, Flyway.

## 3. Modelo de domínio
- **`Notification`:** `id`, `customerId`, `kind`, `title`, `body`, `data` (valores e contas que aparecem no texto), `sourceEventId`, `createdAt`, `readAt` (nulo enquanto não lida).
- **`NotificationKind`:** `WELCOME`, `ACCOUNT_TYPE_CHANGED`, `TRANSFER_SENT`, `TRANSFER_RECEIVED`, `TRANSFER_FAILED`, `TRANSFER_COMPENSATED`.
- **Mapeamento evento → notificações:**

| Evento | Notificações |
|---|---|
| `AccountOpened` | grava conta → cliente; `WELCOME` para o cliente |
| `AccountTypeChanged` (v2) | `ACCOUNT_TYPE_CHANGED` para o dono da conta, com tipo e limite |
| `TransferCompleted` | `TRANSFER_SENT` para o dono da origem; `TRANSFER_RECEIVED` para o dono do destino |
| `TransferFailed` | `TRANSFER_FAILED` para o dono da origem, com o motivo |
| `TransferCompensated` | `TRANSFER_COMPENSATED` para o dono da origem |
| outro tipo | nenhuma (log e confirmação) |

- Texto de boas-vindas (provisório): "Bem-vindo ao banco! Sua conta {tipo} está aberta. Você pode receber transferências pelo número {conta} e acompanhar tudo por aqui no sininho."
- Valores aparecem formatados em reais a partir dos centavos, sem `Double`.

## 4. Fluxo de consumo
1. O `@SqsListener` recebe a mensagem (raw delivery: corpo = envelope; `eventType` e `correlationId` nos atributos).
2. O correlation ID vai para o MDC durante o processamento.
3. Numa transação: insere `event_id` em `processed_events`; se já existe, encerra sem efeito. Senão, aplica o evento (grava conta → cliente e/ou as notificações).
4. Commit e confirmação da mensagem (`acknowledgement` só depois do sucesso).
5. Qualquer exceção desfaz a transação e não confirma: a mensagem volta depois da visibilidade e, na 5ª tentativa, a política de redirecionamento a manda para a DLQ.
- Conta desconhecida num evento de transferência ou de troca de tipo: exceção (decisão 6).
- Envelope malformado ou payload sem campo obrigatório: exceção.
- FIFO: a ordem vale dentro do `MessageGroupId` (conta ou transferência); mensagem presa segura só o seu grupo.

## 5. Esquema do banco (`notification`)
- `notifications (id uuid pk, customer_id uuid, kind text, title text, body text, data jsonb, source_event_id uuid, created_at timestamptz, read_at timestamptz null)`, com `UNIQUE (source_event_id, customer_id, kind)` como rede de segurança e índice `(customer_id, created_at desc, id desc)` para a lista.
- `known_accounts (account_id uuid pk, customer_id uuid)`.
- `processed_events (event_id uuid pk, event_type text, processed_at timestamptz)`.
- Usuário `notification_app` com `grant` por tabela; o init do PostgreSQL cria o banco de forma idempotente.

## 6. Eventos consumidos e mudança no `accounts`
- O `notification` lê o envelope de [`OutboxEventEnvelope`](../../../libs/outbox/src/main/kotlin/io/github/capozziv/digitalbanking/outbox/OutboxEventEnvelope.kt): `eventId`, `eventType`, `eventVersion`, `occurredAt`, `aggregateId`, `correlationId`, `payload`.
- `AccountTypeChanged` ganha `overdraftLimit` (centavos) no payload e passa a `eventVersion` 2, gravado na mesma transação da troca de tipo. Hoje só a fila de inspeção consome o tópico.

## 7. API, nginx e Bruno
- `GET /notifications?customerId=…&cursor=…` → `{ items, unreadCount, nextCursor }`, 20 por página, mais recentes primeiro (cursor por `created_at` + `id`).
- `POST /notifications/read` com `{ customerId, upTo }` → marca como lidas as notificações do cliente até a `upTo` (inclusive), sem afetar as que chegaram depois. Idempotente.
- Os caminhos não começam por `/customers` porque esse prefixo será do serviço `customer` na etapa 7. Na etapa 7 o `customerId` passa a vir do JWT.
- Problem Details, OpenAPI e `X-Correlation-Id`. nginx roteia `/notifications`. Coleção Bruno em `api/bruno/notification/`.

## 8. Operação
- Métrica `notification.dlq.messages` por DLQ (gauge com `ApproximateNumberOfMessages`).
- Guia local ([`docs/rodando-localmente.md`](../../rodando-localmente.md)): como ver a DLQ e devolver as mensagens com `aws sqs start-message-move-task`.

## 9. Testes
- **Prova inicial (primeira task):** o Spring Cloud AWS 4.2.1 é compilado contra o Spring Boot 4.0.7 e o AWS SDK 2.57.0; o projeto usa Boot 4.1.1 e SDK 2.57.1. Um teste de integração com LocalStack prova `@SqsListener` numa fila FIFO com confirmação no sucesso (`ON_SUCCESS`) antes do resto da frente A. Se falhar, a frente para e a decisão volta ao usuário.
- **Unidade:** textos, mapeamento evento → notificações, regra de leitura até `upTo`.
- **Integração (Testcontainers + LocalStack):** entrega repetida da mesma mensagem, ida para a DLQ na 5ª tentativa, evento desconhecido confirmado, conta desconhecida voltando para a fila, API.
- **Ponta a ponta (`tests/e2e`):** critérios de pronto 1 a 6 com os três serviços como processos reais.

## 10. Ambiente local e CI
- Script novo `infra/local/localstack/init/ready.d/03-notification-queues.sh`: filas, DLQs, política de redirecionamento (com os valores da decisão 5) e assinaturas com raw delivery nos dois tópicos. As filas de inspeção continuam.
- docker-compose, smoke (`infra/local/smoke.sh` confere a notificação de uma transferência), CI seletivo com o job `notification` dentro do agregador `ci-ok`.

## 11. Documentação que acompanha
- ADR 0017: banco próprio do `notification`, consumo com Spring Cloud AWS e idempotência por `processed_events`.
- `docs/arquitetura.md`: tabela de serviços e seção de mensageria.
- `CONTEXT.md`: termo "Tipo de notificação" (`NotificationKind`).
- `docs/rodando-localmente.md`: o novo serviço e a operação da DLQ.

## Fora de escopo (e onde fica registrado)
- Notificação de entrada no cheque especial: cartão #101.
- Sininho, apresentação da lista e regra de leitura no frontend: cartão #102 (etapa 9).
- Desbloqueio manual no relay e da compensação recusada: #23 e #56 (etapa 7).
- E-mail, SMS e push: fora do MVP.
- Autenticação da API: etapa 7.

## Riscos
- **Compatibilidade do Spring Cloud AWS com o Boot 4.1.1:** mitigado pela prova inicial (seção 9).
- **Evento de transferência antes do `AccountOpened` correspondente:** filas diferentes não têm ordem entre si. Na prática a conta é aberta bem antes; se acontecer, a mensagem volta até a conta ser conhecida, e só vai para a DLQ se o `AccountOpened` nunca chegar.
- **Contas abertas antes da etapa 3:** não estão em `known_accounts`. No ambiente local o banco é recriado; o e2e e o smoke criam contas novas.
