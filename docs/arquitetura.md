# Arquitetura

## Stack
- **Backend:** Kotlin + Spring Boot 4.1, JDK 21, Spring MVC com virtual threads, Spring JDBC (`JdbcClient`) na persistência, Gradle Kotlin DSL multi-módulo, monorepo.
- **Arquitetura por serviço:** Clean Architecture (`domain` → `application` → `adapters` → `bootstrap`), regras validadas com ArchUnit.
- **AWS:** RDS PostgreSQL, DynamoDB, SNS, SQS + DLQ, ElastiCache (Redis), Cognito, API Gateway (HTTP API), ECS Fargate.
- **Lint:** ktlint.
- **Infraestrutura como código:** Terraform.
- **Local:** docker-compose em `infra/local/` com PostgreSQL (um banco por serviço), Redis e nginx (no papel do API Gateway) e LocalStack (SNS/SQS, conta gratuita com token; a partir da etapa 1). Testes de integração com Testcontainers.
- **Frontend (etapa 9):** Angular + BFF em Node/NestJS.
- **Diagramas:** Excalidraw.

## Serviços
| Serviço | Responsabilidade | Dados |
|---|---|---|
| customer | Cadastro e status do cliente | PostgreSQL |
| accounts | Contas, ledger imutável e saldo (fonte da verdade) | PostgreSQL |
| transfers | Saga orquestrada de transferência, idempotência, limite diário | PostgreSQL + Redis |
| statement | Extrato para leitura (CQRS) | DynamoDB + Redis |
| notification | Consome eventos e notifica | Sem banco |

## Fluxo de entrada
```
Navegador (Angular)
   │ cookie de sessão HttpOnly + token CSRF
   ▼
ALB público ── /     → Angular (nginx)
            └─ /bff  → BFF (NestJS, sessão no Redis)
                          │ Bearer token Cognito + X-Correlation-Id
                          ▼
                 API Gateway HTTP API (autorizador JWT Cognito)
                          │ VPC Link
                          ▼
                 ALB interno → customer / accounts / transfers / statement
                               (chamadas entre serviços via ECS Service Connect)
```
Até a etapa 9 não existe BFF: testes manuais usam uma coleção Bruno/Insomnia versionada no repositório.

## Decisões tomadas
### Dados e consistência
- Conta e ledger no mesmo serviço (`accounts`), na mesma transação ACID.
- O saldo fica no lançamento (`sequence` por conta e `balance_after`); não há coluna de saldo na conta. Concorrência com trava pessimista `FOR UPDATE` na conta e `UNIQUE (account_id, sequence)` como rede de segurança; `lock_timeout` configurável (padrão 2s), estouro vira `503` com `Retry-After`. Nenhuma transação do `accounts` trava duas contas.
- O saldo pode ficar negativo até o limite de cheque especial, definido pelo tipo de conta (`FREE`, `STANDARD`, `GOLD`, `BLACK`, `DIAMOND`). Os limites ficam numa tabela do `accounts`, alterável pelo endpoint `/admin/account-types/{type}` (protegido na etapa 7). Débito acima do saldo disponível: `422` com tipo `insufficient-available-balance`.
- Persistência com Spring JDBC (`JdbcClient`), SQL explícito e mapeamento manual.
- Idempotência por chave no lançamento: `UNIQUE (account_id, idempotency_key)`, conferida sob a trava da conta. Nova tentativa igual devolve o lançamento original; mesma chave com outro tipo ou valor é `422 idempotency-key-reused`.
- `statement` no DynamoDB, chave `accountId` + `timestamp#entryId`, gravação idempotente com `PutItem` condicional.
- (ver [ADR 0007](adr/0007-conta-e-ledger-no-accounts.md), [ADR 0008](adr/0008-statement-cqrs-dynamodb.md), [ADR 0012](adr/0012-saldo-no-lancamento-e-trava-pessimista.md), [ADR 0013](adr/0013-persistencia-com-spring-jdbc.md) e [ADR 0014](adr/0014-tipo-de-conta-e-cheque-especial.md))

### Cache
- Extrato: cache-aside só da primeira página, TTL curto com jitter, invalidação ao consumir evento, fallback para DynamoDB se o Redis cair.
- Saldo para autorizar débito nunca vem de cache.
- (ver [ADR 0009](adr/0009-cache-do-extrato.md) e [ADR 0007](adr/0007-conta-e-ledger-no-accounts.md))

### Limite diário
- Contador no Redis com script Lua (reservar e liberar), reserva idempotente por `transferId`.
- PostgreSQL do `transfers` é a fonte da verdade; o contador é reconstruído a partir dele.
- (ver [ADR 0010](adr/0010-limite-diario-redis-lua.md))

### Mensageria
- Eventos publicados via Transactional Outbox.
- Um tópico SNS por produtor; uma fila SQS + DLQ por consumidor; consumidores idempotentes.
- O tópico é SNS FIFO (`accounts-events.fifo`), com `MessageGroupId` = conta e `MessageDeduplicationId` = `eventId`: ordem garantida por conta.
- Relay (`OutboxRelay`) dentro de cada serviço, com um relay ativo por vez (trava consultiva do PostgreSQL). Falha de publicação bloqueia só a conta afetada. Outbox guardado para sempre; envelope com `eventVersion`.
- (ver [ADR 0003](adr/0003-transactional-outbox-e-mensageria.md) e [ADR 0015](adr/0015-outbox-relay-lider-e-sns-fifo.md))

### Borda e segurança
- **API Gateway HTTP API** substitui um gateway em Spring: roteamento por caminho, autorizador JWT nativo do Cognito, VPC Link para ALB interno.
- Os serviços **também** validam o JWT (Spring Security resource server); não confiam só na borda.
- Localmente, nginx faz o roteamento por caminho. Cognito no LocalStack é recurso pago: prever modo local com usuários/tokens de teste.
- (ver [ADR 0004](adr/0004-api-gateway-http-api-na-borda.md))

### Contratos
- Cada serviço expõe contrato OpenAPI.
- Erros no formato Problem Details (RFC 9457).
- Transferências exigem header `Idempotency-Key`.
- (ver [ADR 0011](adr/0011-contratos-de-api.md))

### Observabilidade
- Apenas correlation ID, sem tracing distribuído (sem OpenTelemetry, Jaeger ou X-Ray).
- O ID vem do header `X-Correlation-Id` ou é gerado na entrada, vai para o MDC (logs JSON) e é propagado nas chamadas HTTP, numa coluna do outbox, no envelope do evento e nos atributos SNS/SQS (raw message delivery).
- Até existir o BFF, o serviço que recebe a chamada gera o ID; depois, o BFF gera e os serviços reaproveitam.
- (ver [ADR 0006](adr/0006-observabilidade-com-correlation-id.md))

### Frontend (etapa 9, detalhar depois)
- Angular (standalone components, signals); biblioteca de componentes em aberto.
- BFF em NestJS: login OAuth2 com Cognito (`openid-client`), sessão no Redis, cookie `HttpOnly`, CSRF, refresh de token, correlation ID via `AsyncLocalStorage`, logs JSON com pino.
- BFF expõe endpoints por tela (agregação), não proxy genérico.
- `Idempotency-Key` gerada no Angular ao abrir o formulário de transferência; o BFF só repassa.
- Status da saga por polling (RxJS).
- Front e BFF na mesma origem (ALB roteando `/` e `/bff`), sem CORS.
- Organização: `frontend/web` (Angular), `frontend/bff` (NestJS), `frontend/shared` (DTOs de apresentação, opcional), com workspaces npm/pnpm e job próprio no CI.
- (ver [ADR 0005](adr/0005-frontend-angular-e-bff-nestjs.md))

## Em aberto
Fonte única das decisões pendentes. Ao decidir, registre no ADR indicado (ou crie um) e remova o item daqui.

**Antes das etapas 2 a 7**
- Avaliar ADRs próprios para a saga orquestrada com compensação (`transfers`) e para ECS Service Connect.
- Cache local (Caffeine) na frente do Redis ou só Redis ([ADR 0009](adr/0009-cache-do-extrato.md)).
- Valor padrão do limite diário, único para todas as contas no MVP, a definir antes da etapa 6; variação por tipo de conta fica para depois do MVP ([ADR 0010](adr/0010-limite-diario-redis-lua.md)).
- Estratégia de autenticação no ambiente local, sem Cognito ([ADR 0004](adr/0004-api-gateway-http-api-na-borda.md)).

**Etapa 9**
- Angular Material ou PrimeNG ([ADR 0005](adr/0005-frontend-angular-e-bff-nestjs.md)).
