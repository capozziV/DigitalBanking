# Etapa 7 — borda e identidade: plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** JWT em todos os serviços, regras de dono por cliente, grupo `admin`, token de máquina entre serviços, serviço `customer` consultado na hora, correlation ID numa lib e Terraform do Cognito e do API Gateway validado sem aplicar.

**Architecture:** a `libs/security` traz a configuração comum de resource server, o `Caller`, as respostas `401`/`403` e o cliente de token de máquina. A `libs/observabilidade` traz o filtro e a propagação do correlation ID. Cada serviço declara as próprias regras de rota, e cada caso de uso recebe o `Caller`. O `customer` é um serviço novo no molde do `notification`.

**Tech Stack:** Kotlin, Spring Boot 4.1.1, Spring Security (oauth2 resource server e client), `mock-oauth2-server` (navikt), Testcontainers, Terraform (AWS provider), Flyway.

**Spec:** [`docs/superpowers/specs/2026-11-13-etapa-7-borda-e-identidade-design.md`](../specs/2026-11-13-etapa-7-borda-e-identidade-design.md)

## Global Constraints
- `Caller` (em `libs/security-caller`, sem Spring; o resto em `libs/security`). Rotas sem regra são negadas: cada serviço lista todas as suas rotas, e as de cliente incluem `ADMIN`.
- `Caller` (forma):
  - `sealed interface Caller { data class Customer(val customerId: UUID); data class Admin(val userId: UUID); data object Service }`;
  - um admin é `Admin`, mas pode agir como cliente nas próprias contas: `Caller.actsAsCustomer(customerId)` retorna verdadeiro para `Customer` com o mesmo id e para `Admin` com `userId == customerId`.
- Claims: `sub` UUID; grupos em `digitalbanking.security.groups-claim` (padrão `groups`; na AWS `cognito:groups`); escopo de máquina `digitalbanking/internal` em `scope`.
- Respostas:
  - sem token ou token inválido → `401` `/problems/unauthorized`;
  - rota de admin ou interna sem permissão → `403` `/problems/forbidden`;
  - recurso de outro cliente → `404` com o problem de "não encontrado" que o serviço já usa.
- Consulta ao `customer`: `GET /internal/customers/{id}`, timeout `${*_CUSTOMER_TIMEOUT:500ms}`, sem cache. `404` → `422 customer-not-found`; `BLOCKED` → `422 customer-blocked`; erro, timeout ou `5xx` → `503 customers-unavailable`.
- `customer`: porta `8085`, banco `customer` (`customer_owner`/`customer_app`, grants próprios), tópico `customer-events.fifo`.
- Ambiente local:
  - `mock-oauth2-server` na porta `8090`, emissor `http://localhost:8090/default`;
  - variáveis `*_JWT_ISSUER_URI`, `*_OAUTH_TOKEN_URI` (local `http://localhost:8090/default/token`; na AWS, a saída `token_endpoint_url` do Terraform), `*_OAUTH_CLIENT_ID` e `*_OAUTH_CLIENT_SECRET` por serviço.
- Usuário de teste do dono: `sub` `00000000-0000-7000-8000-000000000001`, nome `Owner Test`, e-mail `owner@example.test`, grupo `admin`.
- Código, testes e logs em inglês; docs em português. Testes terminam em `Test` ou `IntegrationTest`.
- Agentes em worktree não regeneram nem commitam `graphify-out/`.
- Pronto = `./gradlew build` verde, com a saída real no relato.

## Rulings deste plano
- `libs/security` e `libs/observabilidade` são infraestrutura pura. O `Caller` não é classe de domínio de nenhum serviço, então pode ser compartilhado. O `application` de cada serviço depende só do `Caller`, sem Spring. Custo se errado: mover o tipo para cada serviço.
- Os testes de integração usam `libs/testing-security`: uma chave RSA gerada no teste, um `JwtDecoder` apontando para ela e uma função `token(sub, groups, scopes)`. Só o e2e sobe o `mock-oauth2-server`.
- O `customer` é chamado por um cliente HTTP próprio em cada serviço que o consulta (`accounts` e `transfers`). Nenhuma classe de domínio é compartilhada.
- O Bruno e o smoke só passam a exigir token na Task 8, que também faz o seed. Até lá, os testes de cada serviço cobrem a segurança.

## Review Focus
1. Admin que também é cliente: opera as próprias contas e, como admin, as dos outros (Tasks 1 e 5–7).
2. Token de cliente em rota interna (débito, crédito, `/internal/**`) → `403`, nunca `200` (Tasks 4–6).
3. Recurso de outro cliente → `404`, inclusive no extrato, no limite diário e nas notificações (Tasks 5–7).
4. `customer` lento além do timeout → `503` sem criar conta nem transferência (Tasks 5 e 6).
5. Token expirado ou de outro emissor → `401` (Task 1).

## Ordem e paralelismo
- **Onda 1 (paralelas):** Task 1 (Marina), Task 2 (Rafael), Task 3 (Rafael, em outro worktree). Clara escreve o ADR de identidade em paralelo.
- **Onda 2 (paralelas, um serviço por task):** Tasks 4, 5, 6 e 7 (Marina, em worktrees separados). Só a Task 4 mexe em `settings.gradle.kts` e no `ci.yml`.
- **Onda 3:** Task 8 (Marina): nginx, e2e, seed, smoke e Bruno.

---

### Task 1: `libs/security` e `libs/observabilidade`
**Files:**
- Create `libs/security`, com:
  - `Caller.kt`;
  - `CallerResolver.kt` (de `Jwt` para `Caller`);
  - `SecurityAutoConfiguration` (resource server, conversor de grupos e escopo, `AuthenticationEntryPoint` e `AccessDeniedHandler` com Problem Details);
  - `ServiceTokenClient` (client credentials com cache até 30 s antes de expirar, e um interceptor de `RestClient` que põe `Authorization: Bearer`);
  - `ClientDisconnectHandling` (`@ControllerAdvice` de prioridade alta para `AsyncRequestNotUsableException`/`ClientAbortException`, que só loga DEBUG).
- Create `libs/observabilidade`, com:
  - `CorrelationIdFilter`, igual aos quatro filtros atuais, mesmo cabeçalho e mesmas regras;
  - `CorrelationIdRestClientInterceptor`;
  - a autoconfiguração.
- Create `libs/testing-security` (`JwtTestSupport`: chave RSA, `jwtDecoder()`, `token(sub, groups = emptyList(), scopes = emptyList(), expiresIn = 5m)`).
- Modify `settings.gradle.kts` (as três libs).

**Interfaces (produz):** `Caller`, `Caller.actsAsCustomer(UUID)`, a anotação ou função `currentCaller()` para controllers, `ServiceTokenClient.interceptor()`, `JwtTestSupport.token(...)`.

- [ ] **Testes:**
  - `CallerResolverTest` cobre cliente, admin, serviço, `sub` não UUID e grupo vindo do claim configurado;
  - teste de integração de uma app de teste mínima: `401` sem token, `401` com token expirado, `401` com outro emissor, `403` em `/admin/**` sem grupo, `200` com grupo, `403` em `/internal/**` com token de cliente, Problem Details nos corpos;
  - `ServiceTokenClientIntegrationTest` com um servidor de token fake: o cache evita uma 2ª chamada;
  - `CorrelationIdFilterTest` cobre as regras atuais.
- [ ] Build das três libs verde. Commit `feat(libs): segurança comum, observabilidade e apoio de testes de JWT`.

### Task 2: ambiente local de identidade
**Files:**
- Modify `infra/local/docker-compose.yml`: o `mock-oauth2-server` (`ghcr.io/navikt/mock-oauth2-server`), porta `8090`, healthcheck em `127.0.0.1`, e um `JSON_CONFIG` que emite `sub` UUID e `groups` a partir de parâmetros do login e aceita client credentials com o escopo `digitalbanking/internal`.
- Create `infra/local/auth/token.sh`, com os comandos `user <sub> [admin]` e `service`, que imprime um access token.
- Modify `infra/local/.env.example` e `docs/rodando-localmente.md`: emissor, clientes por serviço e como pegar token.

- [ ] Compose de pé; `token.sh user ...` e `token.sh service` devolvem JWTs com as claims certas (`jq` no payload). Registre a saída real. Commit `build: mock-oauth2-server no ambiente local`.

### Task 3: Terraform do Cognito e do API Gateway (sem aplicar)
**Files:**
- Create `infra/terraform/modules/identity`: User Pool, grupo `admin`, resource server `digitalbanking` com o escopo `internal`, app client web com PKCE e app client de máquina.
- Create `infra/terraform/modules/edge`: HTTP API, autorizador JWT, rotas por prefixo (`/accounts`, `/transfers`, `/notifications`, `/customers`, `/admin`, statement e daily-limit) e VPC Link para `var.alb_listener_arn` e `var.subnet_ids`.
- Create `infra/terraform/envs/aws` (composição, `versions.tf`, sem backend remoto) e `infra/terraform/README.md`.
- Modify `.github/workflows/ci.yml`: job `terraform` (`setup-terraform`, `fmt -check -recursive`, `init -backend=false` e `validate` em `envs/aws`), entrando no `ci-ok` e no filtro de mudanças.

- [ ] `terraform fmt -check`, `init -backend=false` e `validate` verdes localmente, com o Terraform instalado em `~/.local/bin` se faltar. Commit `infra: Terraform do Cognito e do API Gateway (sem apply)`.

### Task 4: serviço `customer`
**Files:** `services/customer/{domain,application,adapters,bootstrap}`, no molde do `notification`, com:
- banco e migrações (`customer`, grants);
- outbox e relay para `customer-events.fifo`;
- script `infra/local/localstack/ready.d` do tópico, montado só nos testes que precisam;
- `infra/local/postgres` com o banco e os usuários;
- `settings.gradle.kts`;
- job `customer` no `ci.yml`;
- OpenAPI.

**API:** a seção 2 da spec, com regras pelo `Caller` (Task 1).

- [ ] **Testes:**
  - domínio (`Customer`, status);
  - casos de uso: cadastro idempotente por `sub`, status pelo admin, eventos `CustomerRegistered`, `CustomerBlocked` e `CustomerUnblocked` no outbox;
  - integração: `201` e depois `200` no cadastro repetido, `GET /customers/me`, `404` em `/me` sem cadastro, `/admin` com e sem grupo, `/internal/customers/{id}` com token de serviço `200`/`404` e com token de cliente `403`;
  - ArchUnit.
- [ ] `./gradlew build` verde. Commit `feat(customer): serviço de clientes com status e eventos`.

### Task 5: `accounts` com JWT, dono e consulta ao `customer`
**Files:**
- `accounts` usa `libs/security` e `libs/observabilidade`, e apaga o filtro próprio;
- regras de rota da seção 3 da spec;
- `POST /accounts` sem `customerId` no corpo, com o cliente vindo do `Caller`;
- `HttpCustomerGateway` (`adapters/customer/`) com o token de máquina;
- `GET /accounts/{id}` e `/entries` com a regra de dono;
- débitos e créditos só para `Service` e `Admin` (a busca de lançamento em `/entries` vale também para o cliente dono, como na tabela da spec);
- actuator restrito;
- OpenAPI `bearerAuth`.

- [ ] **Testes:**
  - abrir conta: `201` para cliente ativo, `422 customer-not-found`, `422 customer-blocked`, `503` com o `customer` fora ou lento (servidor HTTP de teste);
  - `404` na conta de outro cliente;
  - admin vê qualquer conta;
  - `403` de cliente em débito e em crédito;
  - token de serviço no débito `200`;
  - os testes atuais migrados para mandar token.
- [ ] Build do `accounts` verde. Commit `feat(accounts): JWT, dono da conta e consulta ao customer`.

### Task 6: `transfers` com JWT, dono e consulta ao `customer`
**Files:**
- `transfers` usa as libs e apaga o filtro próprio;
- `POST /transfers` exige origem do cliente: a consulta da conta ao `accounts` já devolve o `customerId`;
- `source_customer_id` gravado (migração V7);
- consulta ao `customer` antes de criar a transferência;
- `GET /transfers/{id}` com a regra de dono;
- `GET /accounts/{id}/daily-limit` confere o dono pelo `accounts`;
- `HttpAccountsGateway` e `HttpCustomerGateway` com o token de máquina;
- actuator e OpenAPI.

- [ ] **Testes:**
  - `422 customer-blocked` e `503` sem criar transferência;
  - origem de outro cliente: `404` (conta não encontrada para esse cliente);
  - `GET` de transferência alheia: `404`;
  - limite de conta alheia: `404`;
  - admin passa;
  - as chamadas ao `accounts` levam o `Bearer` de serviço (servidor de teste confere o cabeçalho);
  - testes atuais migrados.
- [ ] Build do `transfers` verde. Commit `feat(transfers): JWT, dono e consulta ao customer`.

### Task 7: `statement` e `notification` com JWT
**Files:**
- os dois serviços usam as libs e apagam os filtros próprios;
- `statement`: extrato só do dono (o `customerId` da conta já está na tabela) ou do admin;
- `notification`: `GET /notifications` e `POST /notifications/read` pelo `Caller`, sem o parâmetro `customerId`;
- actuator e OpenAPI.

- [ ] **Testes:** `401`, `404` do extrato alheio, admin passa, notificações só do próprio cliente; testes atuais migrados. Build dos dois verde. Commit `feat(statement,notification): JWT e dono`.

### Task 8: borda local, e2e, seed, smoke e Bruno
**Files:**
- nginx (`/customers` e `/admin/customers` para o `customer`);
- e2e:
  - `mock-oauth2-server` em Testcontainers e `customer` como processo;
  - os clientes de API pegam token;
  - teste novo `a blocked customer cannot transfer and another customer cannot read the statement`;
- `infra/local/seed.sh`: usuário do dono, duas contas e crédito inicial pelo admin;
- `smoke.sh` e as cinco coleções Bruno com tokens (coleção nova `customer`);
- `docs/rodando-localmente.md`.

- [ ] `./gradlew build` completo verde; smoke, seed e `bru run` das cinco coleções verdes com o compose de pé. Commit `test(e2e): identidade de ponta a ponta, seed do dono, smoke e Bruno`.

## Revisão final da branch (Beatriz)
Mutantes, cada um tem de deixar teste vermelho:
- sem a regra de dono no extrato → `404` alheio;
- sem a consulta ao `customer` no `transfers` → teste do bloqueado;
- débito aceitando token de cliente → `403`;
- `Caller` ignorando o claim de grupos → testes de admin;
- `ServiceTokenClient` sem cache → teste do cache.
