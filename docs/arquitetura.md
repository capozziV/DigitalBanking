# Arquitetura

## Stack
- **Backend:** Kotlin + Spring Boot 4.1, JDK 21, Spring MVC com virtual threads, Spring JDBC (`JdbcClient`) na persistência, Gradle Kotlin DSL multi-módulo, monorepo.
- **Arquitetura por serviço:** Clean Architecture (`domain` → `application` → `adapters` → `bootstrap`), um módulo Gradle por camada, regras validadas com ArchUnit. Pacote raiz `io.github.capozziv.digitalbanking`.
- **AWS:** RDS PostgreSQL, DynamoDB, SNS, SQS + DLQ, ElastiCache (Redis), Cognito, API Gateway (HTTP API), EKS ([ADR 0019](adr/0019-runtime-na-aws-com-eks.md)), em `sa-east-1`. O ambiente na AWS é efêmero: sobe sob demanda, fica no máximo 8 h e é destruído com Terraform ([ADR 0021](adr/0021-deploy-e-ambiente-efemero.md)).
- **Lint:** ktlint.
- **Infraestrutura como código:** Terraform.
- **Local:** docker-compose em `infra/local/` com PostgreSQL (um banco por serviço), Redis, nginx (no papel do API Gateway), `mock-oauth2-server` (no papel do Cognito), LocalStack (SNS e SQS, conta gratuita com token) e DynamoDB Local com volume nomeado para a tabela do extrato ([ADR 0027](adr/0027-dynamodb-local-com-volume.md)). Perfil `apps` sobe os cinco serviços pelas imagens Docker. Testes de integração com Testcontainers.
- **Frontend (etapas 9 e 11):** SPA React + Tailwind + shadcn/ui ([ADR 0029](adr/0029-frontend-em-react-tailwind-shadcn.md)) + BFF em Node/NestJS.
- **Diagramas:** Mermaid (no `README.md`).

## Serviços
| Serviço | Responsabilidade | Dados |
|---|---|---|
| customer | Cadastro e status do cliente (porta 8085, eventos `customer-events.fifo`) | PostgreSQL próprio |
| accounts | Contas, ledger imutável e saldo (fonte da verdade) | PostgreSQL |
| transfers | Saga orquestrada de transferência, idempotência, limite diário | PostgreSQL + Redis |
| statement | Extrato para leitura (CQRS) | DynamoDB + Redis (cache da 1ª página) |
| notification | Consome eventos e grava notificações dentro do aplicativo (sininho) | PostgreSQL |

### Componentes do app do cliente (etapa 9)
| Componente | Responsabilidade | Dados |
|---|---|---|
| web | SPA React compilada (Vite), servida por nginx na rota `/` | sem estado |
| bff | NestJS: endpoints por tela, login pela API do Cognito, sessão e CSRF; rota `/bff` | sessão no Redis |

`web` e `bff` ficam na mesma origem (`/` e `/bff`). Login, senha de 6 dígitos e telefone: [ADR 0024](adr/0024-login-e-telefone-do-app.md).

## Fluxo de entrada
```
Navegador (React)
   │ cookie de sessão HttpOnly + token CSRF
   ▼
API Gateway HTTP API (autorizador JWT Cognito nas rotas da API)
   │ VPC Link
   ▼
ALB interno ── rotas da API → customer / accounts / transfers / statement / notification
            ├─ /     → web React (nginx)
            └─ /bff  → BFF (NestJS, sessão no Redis)
                          │ Bearer token Cognito + X-Correlation-Id
                          ▼
                 customer / accounts / transfers / statement / notification
                 (o BFF e os serviços se chamam pelo DNS de Service do Kubernetes, sem passar pelo API Gateway)
```
O BFF não passa pelo API Gateway: chama os Services do cluster direto. Testes manuais da API usam a coleção Bruno em `api/bruno/`.

## Decisões tomadas
### Dados e consistência
- Conta e ledger no mesmo serviço (`accounts`), na mesma transação ACID.
- Uma conta não encerrada por cliente (antes, uma conta por cliente): depois de encerrar, o cliente pode abrir outra ([spec do bloqueio e encerramento](superpowers/specs/2026-11-16-bloqueio-e-encerramento-de-conta-design.md)).
- O saldo fica no lançamento (`sequence` por conta e `balance_after`); não há coluna de saldo na conta. Concorrência com trava pessimista `FOR UPDATE` na conta e `UNIQUE (account_id, sequence)` como rede de segurança; `lock_timeout` configurável (padrão 2s), estouro vira `503` com `Retry-After`. Nenhuma transação do `accounts` trava duas contas.
- O saldo pode ficar negativo até o limite de cheque especial, definido pelo tipo de conta (`FREE`, `STANDARD`, `GOLD`, `BLACK`, `DIAMOND`). Os limites ficam numa tabela do `accounts`, alterável pelo endpoint `/admin/account-types/{type}` (protegido na etapa 7). Débito acima do saldo disponível: `422` com tipo `insufficient-available-balance`.
- Persistência com Spring JDBC (`JdbcClient`), SQL explícito e mapeamento manual.
- Idempotência por chave no lançamento: `UNIQUE (account_id, idempotency_key)`, conferida sob a trava da conta. Nova tentativa igual devolve o lançamento original; mesma chave com outro tipo ou valor é `422 idempotency-key-reused`. A abertura de conta tem chave de idempotência própria: a mesma chave devolve a conta existente.
- `statement` no DynamoDB, chave `accountId` + `sequence` do lançamento (numérica), gravação idempotente com `PutItem` condicional.
- (ver [ADR 0007](adr/0007-conta-e-ledger-no-accounts.md), [ADR 0008](adr/0008-statement-cqrs-dynamodb.md), [ADR 0018](adr/0018-chave-do-extrato-pela-sequencia.md), [ADR 0012](adr/0012-saldo-no-lancamento-e-trava-pessimista.md), [ADR 0013](adr/0013-persistencia-com-spring-jdbc.md) e [ADR 0014](adr/0014-tipo-de-conta-e-cheque-especial.md))

### Saga de transferência
- O `transfers` orquestra a saga: débito na origem, crédito no destino e, se o crédito não sai, compensação (crédito de volta na origem, tentado até conseguir). Cada passo é um objeto do padrão State.
- Chamadas HTTP síncronas ao `accounts`, com chaves de idempotência derivadas do `transferId` (uma por passo). `POST /transfers` roda a saga na requisição e devolve `202` com a transferência `PENDING` em falha passageira; um agendador termina depois.
- Prazo de 2 minutos para falha passageira em débito e crédito. Depois dele, o `transfers` consulta o lançamento pela chave (`GET /accounts/{id}/entries?idempotencyKey=…`) em vez de repetir a chamada.
- Arrendamento (`locked_until`) no lugar de trava de banco durante a chamada HTTP.
- (ver [ADR 0016](adr/0016-saga-orquestrada-por-http.md) e a [spec da etapa 2](superpowers/specs/2026-10-27-etapa-2-transfers-design.md))

### Falha longa no extrato
- Decidido em 2026-11-22, a implementar (#239): o `statement` tenta por cerca de 1 hora (120 recepções de 30 s) antes da DLQ, em vez de 2,5 min. Custo aceito: mensagem defeituosa segura os lançamentos seguintes daquela conta por até 1 hora. Sem alarme, retenção maior nem backup por ora: passada 1 hora, o lançamento fica na DLQ sem aviso até o reenvio manual ou a derrubada do ambiente.

### Cache
- Extrato: cache-aside só no Redis (sem Caffeine), só a 1ª página com `limit` 20, TTL 60 s ± 10 s, invalidação ao consumir o lançamento por versão por conta (um script Lua incrementa a versão e apaga a página; a leitura só grava se a versão não mudou, senão `statement.cache.writes{result=stale}`), fallback para o DynamoDB com timeout de 100 ms se o Redis cair (sem circuit breaker). Resíduo aceito: se a versão se perder ou a invalidação falhar no Redis, página velha por no máximo um TTL.
- Saldo para autorizar débito nunca vem de cache.
- (ver [ADR 0009](adr/0009-cache-do-extrato.md) e [ADR 0007](adr/0007-conta-e-ledger-no-accounts.md))

### Limite diário
- Passo `ReserveLimit` da saga antes do débito; limite estourado termina `FAILED` com `DAILY_LIMIT_EXCEEDED`. Dia em `America/Sao_Paulo`.
- A reserva conta em `PENDING`, é consumida em `COMPLETED` e liberada em `FAILED` e `COMPENSATED`.
- Contador no Redis com script Lua (reservar, liberar, semear), reserva idempotente por `transferId`, TTL de 10 min e reconstrução sob demanda.
- PostgreSQL do `transfers` é a fonte da verdade (`limit_day` e `limit_reserved` em `transfer`).
- Trava única `pg_advisory_xact_lock` por conta e dia nas duas vias; o `daily_limit_epoch` força a reconstrução do contador depois de reservas feitas pelo PostgreSQL. Com o Redis fora, a conferência é feita no PostgreSQL.
- Valor padrão: R$ 5.000,00 (500000 centavos), único para todas as contas no MVP; variação por tipo de conta fica para depois do MVP.
- (ver [ADR 0010](adr/0010-limite-diario-redis-lua.md))

### Mensageria
- Eventos publicados via Transactional Outbox.
- Um tópico SNS por produtor; uma fila SQS + DLQ por consumidor e por tópico de origem; consumidores idempotentes.
- Os tópicos são SNS FIFO: `accounts-events.fifo`, com `MessageGroupId` = conta (ordem garantida por conta), e `transfers-events.fifo`, com `MessageGroupId` = transferência (um evento final por transferência). Nos dois, `MessageDeduplicationId` = `eventId`.
- Relay (`OutboxRelay`) dentro de cada serviço, com um relay ativo por vez (trava consultiva do PostgreSQL). Falha de publicação bloqueia só a conta afetada. Outbox guardado para sempre; envelope com `eventVersion`.
- A leitura do lote pula os eventos seguintes de uma conta presa, que não ocupa o lote das outras. Limites de tempo: 2 s por publicação e 10 s por ciclo (configuráveis).
- Evento preso se destrava por descarte manual (só `ADMIN`): `GET /admin/outbox/stuck-events` e `POST /admin/outbox/events/{eventId}/discard`, com motivo obrigatório, em `accounts`, `transfers` e `customer`, chamados direto na porta do serviço (o nginx não roteia). Descartar é perda deliberada, com consequências nos consumidores (ver [ADR 0015](adr/0015-outbox-relay-lider-e-sns-fifo.md)).
- O `notification` consome com `@SqsListener` (Spring Cloud AWS) as filas FIFO `notification-accounts.fifo`, `notification-transfers.fifo` e `notification-customer.fifo`, cada uma com DLQ FIFO (`notification-accounts-dlq.fifo`, `notification-transfers-dlq.fifo`, `notification-customer-dlq.fifo`); a mensagem vai para a DLQ na 5ª tentativa (visibilidade de 30 s). A terceira fila vem do [ADR 0025](adr/0025-nome-da-contraparte-nas-notificacoes.md).
- Para mostrar o nome da outra parte, o `notification` consome também `AccountOpened` (`accounts-events.fifo`) e `CustomerRegistered` (`customer-events.fifo`, fila `notification-customer.fifo`) numa projeção própria; o texto é montado na leitura (ver [ADR 0025](adr/0025-nome-da-contraparte-nas-notificacoes.md)).
- Idempotência do consumidor pela tabela `processed_events`, gravada na mesma transação das notificações; a mensagem é confirmada só depois do commit.
- O `statement` (porta 8084, sem PostgreSQL) consome `AccountOpened` e `LedgerEntryRecorded` do `accounts-events.fifo` na fila FIFO `statement-accounts.fifo`, com DLQ `statement-accounts-dlq.fifo` (120 recebimentos, visibilidade 30 s: cerca de 1 hora de retentativa antes da DLQ, para o DynamoDB fora não tirar o lançamento do extrato; uma mensagem envenenada segura os lançamentos seguintes da mesma conta por esse tempo, e passada 1 hora o lançamento vai para a DLQ sem alarme). Não usa `processed_events`: o `PutItem` condicional é a idempotência.
- (ver [ADR 0003](adr/0003-transactional-outbox-e-mensageria.md), [ADR 0015](adr/0015-outbox-relay-lider-e-sns-fifo.md), [ADR 0017](adr/0017-notification-com-banco-e-consumo-sqs.md) e [ADR 0018](adr/0018-chave-do-extrato-pela-sequencia.md))

### Borda e segurança
- **API Gateway HTTP API** substitui um gateway em Spring: roteamento por caminho, autorizador JWT nativo do Cognito, VPC Link para ALB interno.
- Os serviços **também** validam o JWT (Spring Security resource server); não confiam só na borda.
- Localmente, nginx faz o roteamento por caminho. Cognito no LocalStack é recurso pago: localmente, o `mock-oauth2-server` no docker-compose emite JWT com JWKS e os serviços o validam como validam o do Cognito.
- Na AWS os serviços rodam no EKS e se chamam pelo DNS de Service do Kubernetes ([ADR 0019](adr/0019-runtime-na-aws-com-eks.md)). Deploy só com a derrubada automática e o alarme de orçamento ativos ([ADR 0021](adr/0021-deploy-e-ambiente-efemero.md)).
- **Identidade:** o id do cliente é o `sub` do usuário. Três chamadores: cliente, admin (grupo `admin`, claim configurável) e serviço (token de máquina por client credentials, escopo `digitalbanking/internal`). Recurso de outro cliente devolve `404`; sem token, `401`; rota de admin sem o grupo, `403`.
- `accounts` (abrir conta) e `transfers` (criar transferência) consultam o `customer` na hora, sem cache; inexistente ou bloqueado dá `422`, `customer` fora dá `503`.
- **Status da conta** (`ACTIVE`, `BLOCKED`, `CLOSED`): conferido sob a trava da conta, no débito e no crédito. Admin bloqueia e desbloqueia por `PUT /admin/accounts/{id}/status`; o dono (ou admin) encerra por `POST /accounts/{id}/close`, com saldo zero. No encerramento o `accounts` consulta o `transfers` de forma síncrona (`GET /internal/accounts/{id}/pending-transfers`, só token de máquina; transferência pendente dá `422 pending-transfers`, `transfers` fora dá `503 transfers-unavailable`), o que fecha um ciclo síncrono com a saga. Lançamento depois da consulta recusa o encerramento (ver [ADR 0023](adr/0023-status-da-conta-e-encerramento.md)).
- **Bônus de boas-vindas** (R$ 100): um por cliente, só na primeira conta; encerrar e reabrir não repete (a conta nova nasce com saldo zero). Ao abrir, na mesma transação e depois do insert, o `accounts` credita só se o cliente não tem outra conta, inclusive encerrada; o índice único parcial de conta não encerrada por cliente serializa as aberturas (ver [ADR 0026](adr/0026-bonus-de-boas-vindas-por-cliente.md)).
- `401` e `403` em Problem Details; desconexão do cliente não vira `500`. `/actuator/health` aberto sem detalhes; o resto do actuator só para admin.
- **Terraform da borda** (`infra/terraform/modules/identity` e `edge`): parte do ambiente efêmero `envs/aws`, aplicado pelo `aws-up` ([ADR 0021](adr/0021-deploy-e-ambiente-efemero.md)).
- **Bibliotecas compartilhadas** (só infraestrutura): `libs/security` (resource server, `Caller`, token de máquina), `libs/observabilidade` (filtro de correlation ID, propagação nas chamadas HTTP de saída e interceptor SQS que põe o ID da mensagem no MDC) e `libs/testing-security` (emissor de JWT de teste).
- **Perfil Spring `aws`** (`application-aws.yml` em `services/<svc>/bootstrap/src/main/resources/`, nos cinco serviços): desliga o OpenAPI e o swagger-ui e, no `accounts`, no `transfers` e no `customer`, troca o padrão do LocalStack de `<svc>.aws.endpoint` por um valor em branco, para os SDKs usarem os endpoints reais da AWS. No `notification` e no `statement`, os endpoints (`spring.cloud.aws.sqs.endpoint` e `statement.dynamodb.endpoint`) já são em branco por padrão no `application.yml`, e o ambiente local os preenche por variável. JWT issuer, banco e Redis mantêm o padrão `localhost`: sem a variável de ambiente o serviço falha fechado. As variáveis e o `SPRING_PROFILES_ACTIVE=aws` vêm do chart Helm. O ambiente local, os testes, o smoke e o e2e usam o perfil padrão. O `AwsProfileConfigIntegrationTest` de cada serviço garante os endpoints em branco.
- (ver [ADR 0004](adr/0004-api-gateway-http-api-na-borda.md) e [ADR 0020](adr/0020-identidade-e-autorizacao.md))

### Contratos
- Cada serviço expõe contrato OpenAPI.
- Erros no formato Problem Details (RFC 9457).
- Transferências exigem header `Idempotency-Key`.
- Testes de contrato entre os serviços (HTTP e eventos do outbox) com o Stubborn Contract: o produtor gera os testes e o jar de stubs; o consumidor roda contra os stubs.
- (ver [ADR 0011](adr/0011-contratos-de-api.md) e [ADR 0022](adr/0022-testes-de-contrato.md))

### Observabilidade
- Apenas correlation ID, sem tracing distribuído (sem OpenTelemetry, Jaeger ou X-Ray).
- O ID vem do header `X-Correlation-Id` ou é gerado na entrada, vai para o MDC (logs JSON) e é propagado nas chamadas HTTP, numa coluna do outbox, no envelope do evento e nos atributos SNS/SQS (raw message delivery).
- Até existir o BFF, o serviço que recebe a chamada gera o ID; depois, o BFF gera e os serviços reaproveitam.
- O filtro, a propagação e o interceptor SQS (o ID do atributo da mensagem fica no MDC até o fim do processamento, inclusive na linha de erro do contêiner) ficam em `libs/observabilidade`, usada por todos os serviços.
- (ver [ADR 0006](adr/0006-observabilidade-com-correlation-id.md))

### Deploy na AWS
- Dois stacks Terraform: `infra/terraform/bootstrap` (permanente, aplicado pelo usuário: estado, papel OIDC do GitHub, ECR, Budget) e `infra/terraform/envs/aws` (efêmero).
- EKS em `sa-east-1` com node group EC2 gerenciado, 2× `t3.medium`; RDS `db.t4g.micro`, ElastiCache `cache.t4g.micro`, um NAT Gateway.
- ALB interno do Terraform; o AWS Load Balancer Controller só liga os pods por `TargetGroupBinding`. Pod Identity por serviço, sem IRSA. Job `db-init` cria bancos e usuários no RDS.
- Um chart Helm comum (`deploy/helm/service`) e imagem por `Dockerfile` em camadas, etiquetada com o SHA do commit.
- Workflows `aws-up`, `aws-down` e `aws-reaper`, só da `main`. Tempo máximo ligado de 8 h pela etiqueta `created-at`; etiqueta ausente ou malformada derruba. Budget de US$ 20/mês (alertas em 50%, 80% e 100%).
- Papel do GitHub com `AdministratorAccess` e policy de deny ([ADR 0019](adr/0019-runtime-na-aws-com-eks.md)).
- Actuator fora da borda; endpoint público do EKS aberto para `0.0.0.0/0`; Redis sem TLS nem AUTH.
- (ver [ADR 0021](adr/0021-deploy-e-ambiente-efemero.md), [`infra/terraform/README.md`](../infra/terraform/README.md) e [`deploy/helm/README.md`](../deploy/helm/README.md))

### Frontend (etapas 9 e 11)
- Web: SPA Vite + React + TypeScript + Tailwind v4 + shadcn/ui, tema escuro único ("Noite Apple"); React Router, TanStack Query e Motion; Vitest + Testing Library. Lógica de negócio (sessão, cliente do BFF, chave da transferência, centavos, telefone, Problem Details) em módulos TypeScript puros em `frontend/web/src/lib/`. Em desenvolvimento, o Vite faz proxy de `/bff` para `http://127.0.0.1:3000` ([ADR 0029](adr/0029-frontend-em-react-tailwind-shadcn.md), que substitui o Angular do ADR 0005).
- BFF em NestJS: login e cadastro pela API do Cognito (`SignUp`, `ConfirmSignUp`, `InitiateAuth` com `USER_PASSWORD_AUTH`), com app client próprio e sem hosted UI ([ADR 0024](adr/0024-login-e-telefone-do-app.md)), sessão no Redis, cookie `HttpOnly`, CSRF, refresh de token, correlation ID via `AsyncLocalStorage`, logs JSON com pino.
- BFF expõe endpoints por tela (agregação), não proxy genérico.
- `Idempotency-Key` gerada no web por tentativa (usuário + telefone + valor), guardada no `sessionStorage` da aba até o estado final aparecer; o BFF só repassa. Limite: outra aba gera chave nova (#241).
- Status da saga por polling (TanStack Query).
- Front e BFF na mesma origem (API Gateway -> ALB interno roteando `/` e `/bff`), sem CORS; o BFF chama os Services do cluster direto.
- Organização: `frontend/web` (React), `frontend/bff` (NestJS), `frontend/e2e` (Playwright), com workspaces npm e job próprio no CI.
- (ver [ADR 0005](adr/0005-frontend-angular-e-bff-nestjs.md))

## Em aberto
Fonte única das decisões pendentes; cada item tem um cartão `Decision` no [board](https://github.com/users/capozziV/projects/2). Ao decidir, registre no ADR indicado (ou crie um), remova o item daqui e feche o cartão.

Nenhum item em aberto.
