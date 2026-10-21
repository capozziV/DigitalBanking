# Arquitetura

## Stack
- **Backend:** Kotlin + Spring Boot 4.1, JDK 21, Spring MVC com virtual threads, Gradle Kotlin DSL multi-módulo, monorepo.
- **Arquitetura por serviço:** Clean Architecture (`domain` → `application` → `adapters` → `bootstrap`), regras validadas com ArchUnit.
- **AWS:** RDS PostgreSQL, DynamoDB, SNS, SQS + DLQ, ElastiCache (Redis), Cognito, API Gateway (HTTP API), ECS Fargate.
- **Lint:** ktlint.
- **Infraestrutura como código:** Terraform.
- **Local:** docker-compose em `infra/local/` com PostgreSQL (um banco por serviço), Redis e nginx (no papel do API Gateway); emulador AWS a definir antes da etapa 3. Testes de integração com Testcontainers.
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
- `statement` no DynamoDB, chave `accountId` + `timestamp#entryId`, gravação idempotente com `PutItem` condicional.
- (ver [ADR 0007](adr/0007-conta-e-ledger-no-accounts.md) e [ADR 0008](adr/0008-statement-cqrs-dynamodb.md))

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
- (ver [ADR 0003](adr/0003-transactional-outbox-e-mensageria.md))

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

**Antes da etapa 1**
- Mecanismo de controle de concorrência no saldo ([ADR 0007](adr/0007-conta-e-ledger-no-accounts.md)).
- Biblioteca de persistência: Spring JDBC, JPA ou jOOQ (datasource, driver e Flyway já ficam prontos na etapa 0).

**Antes da etapa 3**
- Emulador AWS local (SNS, SQS, DynamoDB): LocalStack com conta gratuita e token (local e segredo no CI) ou emulador aberto como Moto. Desde 2026-03-23 o LocalStack exige autenticação ([ADR 0001](adr/0001-stack-kotlin-spring-boot-aws.md)).

**Antes das etapas 2 a 7**
- Avaliar ADRs próprios para a saga orquestrada com compensação (`transfers`) e para ECS Service Connect.
- Relay do outbox dentro de cada serviço ou como processo separado ([ADR 0003](adr/0003-transactional-outbox-e-mensageria.md)).
- Cache local (Caffeine) na frente do Redis ou só Redis ([ADR 0009](adr/0009-cache-do-extrato.md)).
- Valor padrão do limite diário e se varia por tipo de conta ([ADR 0010](adr/0010-limite-diario-redis-lua.md)).
- Estratégia de autenticação no ambiente local, sem Cognito ([ADR 0004](adr/0004-api-gateway-http-api-na-borda.md)).

**Etapa 9**
- Angular Material ou PrimeNG ([ADR 0005](adr/0005-frontend-angular-e-bff-nestjs.md)).
