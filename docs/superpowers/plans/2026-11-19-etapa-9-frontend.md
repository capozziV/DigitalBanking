# Etapa 9: frontend do cliente — plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** app do cliente (Angular + BFF NestJS) que mostra, em ~2 minutos, uma transferência concluída, uma recusa pelo limite diário, uma devolução com motivo, o extrato atualizado e a notificação, local e na AWS.

**Architecture:** `frontend/` com npm workspaces (`web`, `bff`, `e2e`). O Angular compilado sai numa imagem nginx; o BFF numa imagem Node. Mesma origem: `/` vai ao `web`, `/bff` ao BFF (nginx local, ALB e API Gateway). O BFF guarda a sessão no Redis, fala com o Cognito pela API (ou com o `mock-oauth2-server` localmente) e expõe endpoints por tela. No backend: bônus de boas-vindas no `accounts`, telefone no `customer`, rotas `GET /accounts/me` e `GET /accounts/recipient`.

**Tech Stack:** Angular (standalone, signals, Angular Material), NestJS, ioredis, pino, Playwright; Kotlin/Spring Boot no backend; Terraform, Helm, docker-compose, GitHub Actions. Versões exatas conferidas na fonte primária pela Task 5.

**Spec:** [`docs/superpowers/specs/2026-11-19-etapa-9-frontend-design.md`](../specs/2026-11-19-etapa-9-frontend-design.md)

## Global Constraints
- Código, testes e logs em inglês, com os nomes do `CONTEXT.md`; textos da interface em português.
- Valores em centavos no backend e no BFF; o front formata em reais só para exibir.
- O navegador nunca recebe token: só o cookie de sessão `HttpOnly`, `SameSite=Strict`, `Secure` (desligável por configuração só no local).
- Senha: exatamente 6 dígitos numéricos (`^\d{6}$`), validada na tela e no BFF.
- Bônus de boas-vindas: 10000 centavos, configurável, lançado uma única vez, na transação da abertura.
- Telefone: celular brasileiro em E.164 `^\+55[1-9][0-9]9[0-9]{8}$`, único entre clientes, sem verificação.
- Nada de segredo versionado; na AWS a senha do usuário de demonstração vem de `secrets`.
- `domain` dos serviços sem Spring; DTOs com sufixo `DTO` em `adapters/web/dto/`; testes com sufixo `Test`.
- Visual: fundo `#f5f5f7`, texto `#1d1d1f`, secundário `#86868b`, fonte do sistema, cartões brancos com raio 18 px; entrada = ↓ em círculo preto cheio e valor em negrito; saída = ↑ em círculo vazado e valor em cinza. Sem cor.
- Toda mudança que mexe em `infra/local/localstack/ready.d`, compose ou testes de outros serviços roda o `./gradlew build` completo.

## Review Focus
1. **Telefone digitado com máscara** (`(11) 98765-4321`, espaços, sem `+55`): o BFF normaliza para E.164 antes de buscar; formato inválido dá mensagem clara, não 500. Teste na Task 7.
2. **Transferir para o próprio telefone:** a busca devolve o próprio cliente; o BFF recusa com `422` "não é possível transferir para você mesmo" antes de criar a transferência. Teste na Task 7.
3. **Sessão expirada no meio do uso** (refresh token vencido ou Redis sem a sessão): o BFF responde `401` e o Angular volta ao Entrar, sem tela quebrada. Testes nas Tasks 6 e 8.
4. **Duplo clique em "Confirmar"**: a mesma `Idempotency-Key` gerada ao abrir o formulário faz a segunda chamada devolver a mesma transferência. Teste nas Tasks 7 e 9.
5. **Destinatário sem conta aberta ou com conta encerrada:** a busca por telefone acha o cliente, mas `GET /accounts/recipient` dá `404`; a tela diz "este telefone não tem conta para receber". Testes nas Tasks 2 e 7.

---

## Mapa de arquivos
| Área | Arquivos |
|---|---|
| accounts | `OpenAccountUseCase.kt`, `UseCaseConfiguration.kt`, `application.yml` (`accounts.welcome-bonus.amount`), `AccountController.kt` (`/me`, `/recipient`), `dto/RecipientAccountDTO.kt`, `AccountsSecurityConfiguration.kt` |
| customer | `Customer.kt` (`phone`, `changePhone`, `CustomerPhoneChanged`), `V4__customer_phone.sql`, `RegisterCustomerUseCase.kt`, `ChangePhoneUseCase.kt`, `FindCustomerByPhoneUseCase.kt`, `CustomerController.kt`, `dto/`, `OutboxEventAppender.kt` |
| frontend | `frontend/package.json`, `frontend/web/**`, `frontend/bff/**`, `frontend/e2e/**`, `frontend/web/Dockerfile`, `frontend/bff/Dockerfile` |
| infra | `infra/terraform/modules/identity`, `modules/edge`, `modules/edge-alb`, `envs/aws/check-alb-routes.py`, `deploy/helm/values/{web,bff}.yaml`, `infra/local/docker-compose.yml`, `infra/local/nginx/nginx.conf`, `infra/local/seed.sh`, `.github/workflows/ci.yml`, `.github/workflows/aws-up.yml` |
| docs | `docs/adr/0024-login-e-telefone-do-app.md`, `CONTEXT.md`, `README.md`, `docs/arquitetura.md` |

## Ondas de execução (paralelismo)
- **Onda 1 (já):** Tasks 1, 2, 3, 4, 5, 12. Arquivos disjuntos, exceto `AccountController.kt`/`AccountsSecurityConfiguration.kt` (só a Task 2) e os testes de saldo zero (só a Task 1).
- **Onda 2 (depois da 5):** Tasks 6 e 8 em paralelo.
- **Onda 3 (depois de 2, 3, 6 e 8):** Tasks 7 e 9 em paralelo; Task 10 (precisa da 5).
- **Onda 4:** Task 11 (seed; precisa de 1, 3 e 10), depois Task 13 (Playwright no CI).

---

### Task 1: bônus de boas-vindas no `accounts` (Marina, `dev-kotlin`)

**Files:**
- Modify: `services/accounts/application/.../usecase/OpenAccountUseCase.kt`, `services/accounts/bootstrap/.../UseCaseConfiguration.kt`, `services/accounts/bootstrap/src/main/resources/application.yml`
- Test: `OpenAccountUseCaseTest.kt`, `OpenAccountIntegrationTest` (ou o integração de abertura existente)
- Ajustar tudo o que assume conta nova com saldo zero: `tests/e2e`, `infra/local/smoke.sh`, `api/bruno/**`, contratos de mensagem, testes do `statement` e do `notification`.

**Interfaces:**
- Produces: `OpenAccountUseCase(..., welcomeBonus: Money)`; propriedade `accounts.welcome-bonus.amount` (padrão `10000`); descrição do lançamento `WELCOME_BONUS_DESCRIPTION = "Bônus de boas-vindas"`; idempotencyKey do lançamento `"welcome-bonus:{accountId}"`.

- [ ] **Step 1: testes que falham** em `OpenAccountUseCaseTest`:
  - `opening an account records a welcome bonus credit of 10000 cents` — um `LedgerEntry` `CREDIT`, `amount == Money(10000)`, `balanceAfter == Balance(10000)`, `sequence == 1`, `description == "Bônus de boas-vindas"`; o outbox recebe `AccountOpened` e depois `LedgerEntryRecorded`, nessa ordem.
  - `retry with the same opening key does not record a second bonus` — duas chamadas com a mesma chave: um único lançamento; a 2ª devolve saldo 10000 e `created = false` (mutante: lançar o bônus fora do ramo `INSERTED` deixa este teste vermelho).
  - `the view of a new account shows the bonus balance` — `AccountView.balance == 10000`.
- [ ] **Step 2:** `./gradlew :services:accounts:application:test` — FAIL.
- [ ] **Step 3:** no ramo `INSERTED`, gravar o crédito com `LedgerEntry` de sequência 1 pelo mesmo `LedgerEntryRepository` usado pelo `RecordCreditUseCase` e anexar `LedgerEntryRecorded`, na mesma `tx.inTransaction`. Valor injetado pela configuração.
- [ ] **Step 4:** teste de integração: abrir conta grava 1 lançamento e 2 eventos no outbox na mesma transação; com o outbox falhando, nada é gravado.
- [ ] **Step 5:** ajustar os testes de saldo zero listados acima (saldo inicial 10000 ou débito do bônus antes do cenário, o que deixar o teste mais claro). O `statement` e o `notification` não mudam de código: só os testes que usam uma conta nova real.
- [ ] **Step 6:** `./gradlew build` completo, `infra/local/smoke.sh`, `bru run` das cinco coleções e `./gradlew :tests:e2e:test` verdes.
- [ ] **Step 7:** commit `feat(accounts): bônus de boas-vindas de R$ 100 ao abrir a conta`.

### Task 2: `GET /accounts/me` e `GET /accounts/recipient` (Marina, `dev-kotlin`)

**Files:**
- Modify: `AccountController.kt`, `AccountsSecurityConfiguration.kt`, `GetAccountUseCase.kt` (ou novo `FindRecipientAccountUseCase.kt`), `AccountRepository` + `JdbcAccountRepository` (`findOpenByCustomerId`)
- Create: `adapters/web/dto/RecipientAccountDTO.kt`; contrato HTTP em `src/contractTest`
- Test: unidade dos casos de uso e `AccountControllerIntegrationTest`

**Interfaces:**
- Produces:
  - `GET /accounts/me` (CUSTOMER): `AccountDTO` da conta não encerrada do chamador; `404` Problem Details se não houver.
  - `GET /accounts/recipient?customerId={uuid}` (CUSTOMER, ADMIN): `RecipientAccountDTO(accountId: UUID)` da conta não encerrada (`ACTIVE` ou `BLOCKED`) do cliente; `404` se não houver. Não devolve saldo nem tipo.
  - `AccountRepository.findNotClosedByCustomerId(customerId: UUID): Account?`

- [ ] **Step 1: testes que falham:** `me returns the caller account`, `me returns 404 when the caller has no account`, `me ignores a closed account`, `recipient returns only the account id`, `recipient returns 404 for a closed account` (Review Focus 5), `recipient requires authentication` (`401`), `service role cannot call me` (`403`).
- [ ] **Step 2:** rodar — FAIL. **Step 3:** implementar; regras de rota no `SecurityRules` (rota negada por padrão). **Step 4:** OpenAPI documenta as duas rotas; contrato HTTP novo para o BFF.
- [ ] **Step 5:** `./gradlew :services:accounts:build` verde; Bruno: requisições `me` e `recipient` na coleção `accounts`.
- [ ] **Step 6:** commit `feat(accounts): consulta da própria conta e da conta de destino por cliente`.

### Task 3: telefone do cliente no `customer` (Marina, `dev-kotlin`)

**Files:**
- Modify: `customer/domain/.../Customer.kt`, `RegisterCustomerUseCase.kt`, `CustomerController.kt`, `dto/CustomerDTOs.kt`, `OutboxEventAppender.kt`, `JdbcCustomerRepository.kt`
- Create: `adapters/src/main/resources/db/migration/V4__customer_phone.sql`, `ChangePhoneUseCase.kt`, `FindCustomerByPhoneUseCase.kt`, `PhoneNumber.kt` (domínio)
- Test: `CustomerTest`, `PhoneNumberTest`, testes dos casos de uso, integração do repositório e do controller, contratos de mensagem `customerRegistered` v2 e `customerPhoneChanged` v1

**Interfaces:**
- Produces:
  - `PhoneNumber(value: String)` com `PATTERN = "^\\+55[1-9][0-9]9[0-9]{8}$"`.
  - `Customer.phone: PhoneNumber`; `Customer.register(id, name, email, phone, now)`; `Customer.changePhone(newPhone, now): Pair<Customer, CustomerPhoneChanged?>` (mesmo número = sem evento).
  - Eventos: `CustomerRegistered(customerId, name, email, phone)` versão 2; `CustomerPhoneChanged(customerId, previousPhone, newPhone)` versão 1, no `customer-events.fifo`.
  - `POST /customers` aceita `phone` (obrigatório); `PUT /customers/me/phone` com `ChangePhoneDTO(phone)` → `CustomerDTO`; `GET /customers/lookup?phone=+55...` (CUSTOMER) → `CustomerLookupByPhoneDTO(customerId: UUID, fullName: String)`, `404` se não houver.
  - Telefone repetido → `409` Problem Details `phone-already-in-use`.
  - Migração: coluna `phone varchar(14)`, índice único; clientes já existentes recebem um telefone fictício único gerado na migração (ex.: `+5511900000000` + sequência) para a coluna ser `not null`.

- [ ] **Step 1: testes que falham:** `PhoneNumberTest` (aceita `+5511987654321`; recusa sem `+55`, fixo `+551134567890`, DDD `0`, 12 e 14 dígitos); `register stores the phone and the event carries it`; `two customers cannot have the same phone` (integração com o PostgreSQL real: o índice único segura duas inscrições concorrentes; mutante: sem o índice o teste fica vermelho); `change phone to a number in use is refused with 409`; `change phone to the same number is a no-op without event`; `lookup returns the full name`; `lookup of an unknown phone returns 404`; `lookup requires authentication`.
- [ ] **Step 2:** FAIL. **Step 3:** implementar. **Step 4:** consumidores que leem `CustomerRegistered` (conferir com `graphify query "CustomerRegistered consumer"`) aceitam v1 e v2.
- [ ] **Step 5:** `./gradlew build` completo, smoke e Bruno (`customer`) verdes; `seed.sh` atual passa a enviar telefone.
- [ ] **Step 6:** commit `feat(customer): telefone do cliente, troca e busca por telefone`.

### Task 4: Cognito, borda e rotas na AWS (Rafael, `infra`)

**Files:**
- Modify: `infra/terraform/modules/identity/*.tf`, `modules/edge/*.tf`, `modules/edge-alb/*.tf`, `envs/aws/main.tf`, `envs/aws/outputs.tf`, `envs/aws/check-alb-routes.py`, `envs/aws/validate.sh`

**Interfaces:**
- Produces:
  - Política de senha: `minimum_length = 6`, sem exigir classes; `auto_verified_attributes = ["email"]`; autocadastro ligado (`allow_admin_create_user_only = false`); e-mail padrão do Cognito.
  - App client novo `bff`: sem segredo, `explicit_auth_flows = ["ALLOW_USER_PASSWORD_AUTH", "ALLOW_REFRESH_TOKEN_AUTH"]`; o client `web` não muda. Output `cognito_bff_client_id`.
  - API Gateway (`edge`): prefixos `/` (web) e `/bff` (BFF); ALB: regras para os serviços `web` (porta 80) e `bff` (porta 3000); `check-alb-routes.py` conhece as duas.
- [ ] **Step 1:** `check-alb-routes.py` falha sem as rotas novas (rodar antes de mexer no Terraform).
- [ ] **Step 2:** implementar; `terraform fmt -check`, `terraform validate`, `envs/aws/validate.sh` e `check-alb-routes.py` verdes. Sem `apply`.
- [ ] **Step 3:** commit `feat(infra): Cognito com cadastro e app client do BFF; rotas / e /bff na borda`.

### Task 5: esqueleto do `frontend/` (Lucas, `dev-frontend`)

**Files:**
- Create: `frontend/package.json` (workspaces `web`, `bff`, `e2e`), `frontend/.nvmrc`, `frontend/web/` (Angular CLI), `frontend/bff/` (Nest CLI), `frontend/e2e/` (Playwright), ESLint e Prettier na raiz de `frontend/`, `frontend/README.md`
- [ ] **Step 1:** conferir na fonte primária (`mattpocock-skills:research`) as versões estáveis de Angular, NestJS, Playwright e Node LTS; anotar em `frontend/README.md`.
- [ ] **Step 2:** gerar os três projetos; o `web` com Angular Material, tema próprio com as cores das Global Constraints e fonte do sistema; o `bff` com prefixo global `/bff`, `GET /bff/health` → `{ "status": "UP" }`.
- [ ] **Step 3:** um teste de unidade em cada (`web`: o app renderiza; `bff`: `health` responde `UP`) e um Playwright que abre `/` — rodar e ver verde.
- [ ] **Step 4:** `npm ci && npm run lint && npm test && npm run build` em `frontend/` verdes.
- [ ] **Step 5:** commit `feat(frontend): esqueleto com Angular, NestJS e Playwright`.

### Task 6: BFF — identidade e sessão (Lucas, `dev-frontend`)

**Files:** `frontend/bff/src/identity/**`, `frontend/bff/src/session/**`, `frontend/bff/src/observability/**`, `frontend/bff/src/auth/**`

**Interfaces:**
- Produces:
  - Porta `IdentityProvider { signUp(email, password): Promise<{ confirmationRequired: boolean }>; confirmSignUp(email, code); signIn(email, password): Promise<Tokens>; refresh(refreshToken): Promise<Tokens> }`; adaptadores `CognitoIdentityProvider` (SDK `@aws-sdk/client-cognito-identity-provider`, `USER_PASSWORD_AUTH`) e `LocalIdentityProvider` (aceita o cadastro sem código, guarda e-mail + hash da senha no Redis e pede o token ao `mock-oauth2-server` com o `sub` estável do usuário). Escolha por `IDENTITY_PROVIDER=cognito|local`.
  - Sessão no Redis com prefixo `bff:session:`, cookie `bank_session` (`HttpOnly`, `SameSite=Strict`, `Secure` por `COOKIE_SECURE`, padrão `true`); CSRF por double submit: cookie `bank_csrf` legível + cabeçalho `X-CSRF-Token` em todo método que não é `GET`.
  - Refresh automático quando faltar menos de 60 s para o token expirar; refresh falho apaga a sessão e responde `401`.
  - `AsyncLocalStorage` com `X-Correlation-ID` (gera se faltar e repassa aos serviços); logs JSON com pino, sem token nem senha.
  - Rotas: `POST /bff/auth/sign-up`, `POST /bff/auth/confirm`, `POST /bff/auth/sign-in`, `POST /bff/auth/sign-out`, `GET /bff/auth/session` → `{ authenticated, email }`.
  - Cliente HTTP para os serviços `BackendClient.get/post/put(path, { token, idempotencyKey? })`, que converte Problem Details em erros tipados.
- [ ] **Step 1: testes que falham:** senha `12345` e `1234567` e `12a456` recusadas com `400` antes de chamar a porta (mutante do projeto); `123456` aceita; cookie com os três atributos; `POST` sem `X-CSRF-Token` → `403`; sessão expirada → `401` (Review Focus 3); refresh falho apaga a sessão; log não contém o token.
- [ ] **Step 2:** FAIL. **Step 3:** implementar. **Step 4:** teste de integração do `LocalIdentityProvider` contra o `mock-oauth2-server` e um Redis do compose.
- [ ] **Step 5:** `npm run lint && npm test -w bff && npm run build -w bff` verdes. **Step 6:** commit `feat(bff): identidade pelo Cognito e local, sessão no Redis com CSRF`.

### Task 7: BFF — endpoints por tela (Lucas, `dev-frontend`)

**Interfaces:**
- Consumes: rotas das Tasks 2 e 3; `BackendClient` da Task 6.
- Produces (valores em centavos; datas ISO):
  - `GET /bff/me` → `{ customer: {name, email, phone} | null, account: {...} | null }` (`customer` nulo quando `GET /customers/me` dá 404).
  - `POST /bff/customers` `{ name, phone }`; `PUT /bff/me/phone` `{ phone }`.
  - `POST /bff/accounts` (gera a chave de abertura); `POST /bff/accounts/close`.
  - `GET /bff/home` → `{ account, dailyLimit: {limit, used, available}, latestEntries: Entry[5] }`.
  - `GET /bff/statement?cursor=` → `{ entries: Entry[], nextCursor }`, `Entry = { id, kind, amount, description, balanceAfter, createdAt }`.
  - `GET /bff/transfers/recipient?phone=` → `{ recipientName, recipientToken }`, onde `recipientToken` é opaco (accountId assinado com HMAC da sessão), nunca o accountId cru.
  - `POST /bff/transfers` `{ recipientToken, amount }` + `Idempotency-Key` → `{ id, status }`; `GET /bff/transfers/:id` → `{ id, status, reason }`.
  - `GET /bff/notifications`, `POST /bff/notifications/read`, `GET /bff/notifications/unread-count`.
- [ ] **Step 1: testes que falham:** `(11) 98765-4321`, `11987654321` e `+55 11 98765-4321` viram `+5511987654321`; `1234` → `400` (Review Focus 1); telefone do próprio cliente → `422` (Review Focus 2); conta de destino `404` → `404` "sem conta para receber" (Review Focus 5); `recipientToken` adulterado → `400`; `POST /bff/transfers` repassa a mesma `Idempotency-Key` (Review Focus 4); `home` agrega as três chamadas em paralelo.
- [ ] **Step 2:** FAIL. **Step 3:** implementar com os caminhos conferidos no OpenAPI de cada serviço. **Step 4:** `npm run lint && npm test -w bff` verdes. **Step 5:** commit `feat(bff): endpoints das telas do cliente`.

### Task 8: Web — casca, Entrar, Criar conta, Completar cadastro (Lucas, `dev-frontend`)

**Interfaces:**
- Produces: `AppShellComponent` (menu lateral fixo ≥ 960 px; recolhível com ☰ abaixo disso; itens Início, Extrato, Transferir, Avisos com contador, Minha conta); `SessionService` (signal `session`); interceptor que manda `X-CSRF-Token` e leva `401` ao Entrar; guard `authGuard` e `profileGuard` (sem cliente → Completar cadastro); `MoneyPipe` (centavos → `R$ 1.234,56`); `EntryIconComponent` (↓ cheio / ↑ vazado).
- [ ] **Step 1: testes que falham:** formulário de senha recusa 5 e 7 dígitos e letras; confirmação diferente bloqueia o envio; `MoneyPipe(123456)` → `R$ 1.234,56` e `MoneyPipe(-500)` → `-R$ 5,00`; `401` leva a `/entrar`; menu recolhe abaixo de 960 px.
- [ ] **Step 2:** FAIL. **Step 3:** implementar as telas conforme a spec (6.2) e as referências em `.superpowers/brainstorm/2215-1795093861/content/` (fora do git; o prompt cola o essencial). **Step 4:** `npm run lint && npm test -w web && npm run build -w web` verdes. **Step 5:** commit `feat(web): casca, entrar, criar conta e completar cadastro`.

### Task 9: Web — Início, Extrato, Transferir, Avisos, Minha conta (Lucas, `dev-frontend`)

**Interfaces:**
- Consumes: endpoints da Task 7; componentes da Task 8.
- Produces: rodapé `AppFooterComponent` com "© 2026 capzz.labs" em todas as páginas (casca e telas de entrada), discreto, em `#86868b`, centralizado (pedido do usuário, 2026-11-19; cartão #220).
- Produces: polling do status da transferência a cada 1 s, por até 30 s (RxJS `timer` + `switchMap`, para em `COMPLETED`, `FAILED` ou `COMPENSATED`); textos de status: Concluída, Recusada (motivo), Devolvida (motivo), "Ainda processando" ao estourar o prazo.
- [ ] **Step 1: testes que falham:** Transferir em três passos (telefone → nome + valor → confirmar); a `Idempotency-Key` nasce ao abrir o formulário e se repete no duplo clique (Review Focus 4); polling para no status terminal e mostra o motivo; "Bônus de boas-vindas" aparece no extrato com ↓; extrato carrega a próxima página pelo cursor ("Carregar mais"); Início sem conta mostra "Abrir conta"; encerrar conta pede confirmação.
- [ ] **Step 2:** FAIL. **Step 3:** implementar. **Step 4:** `npm run lint && npm test -w web && npm run build -w web` verdes. **Step 5:** commit `feat(web): início, extrato, transferir, avisos e minha conta`.

### Task 10: imagens, compose, nginx e charts (Rafael, `infra`)

**Files:** `frontend/web/Dockerfile` (build Angular + nginx não-root), `frontend/bff/Dockerfile` (Node não-root), `infra/local/docker-compose.yml` (`web` e `bff`, `bff` com `IDENTITY_PROVIDER=local`, `COOKIE_SECURE=false`, Redis do compose com prefixo `bff:`), `infra/local/nginx/nginx.conf` (`location /bff/` → bff, `location /` → web, depois das rotas da API), `deploy/helm/values/web.yaml`, `deploy/helm/values/bff.yaml`, `deploy/helm/check-env-vars.sh`, `.github/workflows/aws-up.yml` (build/push das duas imagens nas convenções da etapa 8).
- [ ] **Step 1:** `deploy/helm/validate.sh` e `check-alb-routes.py` falham sem os valores novos. **Step 2:** implementar. **Step 3:** `docker compose up --wait` com `web` e `bff` saudáveis; `curl -s 127.0.0.1/bff/health` → `UP`; `curl -s 127.0.0.1/` devolve o `index.html`; rotas da API seguem iguais (`infra/local/smoke.sh` verde). **Step 4:** `deploy/helm/validate.sh` verde. **Step 5:** commit `feat(infra): imagens do web e do BFF no compose, nginx e charts`.

### Task 11: usuário de demonstração no seed (Rafael, `infra`)

**Files:** `infra/local/seed.sh`, `infra/local/seed-check.sh`, `.github/workflows/aws-up.yml`, `infra/local/.env.example` (`DEMO_EMAIL`, `DEMO_PASSWORD`, `DEMO_PHONE`)
- [ ] **Step 1:** `seed-check.sh` falha sem o demo. **Step 2:** o seed cria o usuário demo (local: pelo BFF com `IDENTITY_PROVIDER=local`; AWS: `AdminCreateUser` + senha do segredo `demo-user-password`), o cliente com telefone, a conta (com o bônus), duas transferências concluídas com outro cliente do seed, uma recusada pelo limite e as notificações. Idempotente: rodar duas vezes não duplica. **Step 3:** `seed.sh` duas vezes e `seed-check.sh` verdes. **Step 4:** commit `feat(seed): usuário de demonstração com saldo, extrato e avisos`.

### Task 12: ADR, `CONTEXT.md` e README (Clara, `documentador`)

**Files:** `docs/adr/0024-login-e-telefone-do-app.md`, `CONTEXT.md`, `README.md`, `docs/arquitetura.md`
- [ ] ADR 0024: senha numérica de 6 dígitos (fraca de propósito, bloqueio do Cognito ligado), login pela API do Cognito com app client próprio do BFF, telefone sem verificação como chave de transferência e busca que revela o nome completo; alternativas e consequências.
- [ ] `CONTEXT.md`: **Bônus de boas-vindas** (`WelcomeBonus`) e **Telefone** (`PhoneNumber`), com `_Evite_`.
- [ ] README: como abrir o app local e na AWS; limite diário de e-mail do Cognito; usuário de demonstração. `docs/arquitetura.md`: `web` e `bff` na lista de serviços.
- [ ] Commit `docs: ADR 0024, termos do app e README do frontend`.

### Task 13: Playwright no CI (Lucas, `dev-frontend`; job do CI com Rafael)

**Files:** `frontend/e2e/tests/*.spec.ts`, `.github/workflows/ci.yml` (job `frontend` com `paths-filter` em `frontend/**`; entra no `ci-ok`)
- [ ] **Step 1:** specs: `sign-up-and-first-transfer.spec.ts` (criar conta → completar cadastro → abrir conta → bônus R$ 100 → transferir ao telefone do demo → Concluída → extrato e aviso); `daily-limit-refusal.spec.ts` (transferência acima do limite → Recusada com o motivo); `demo-user.spec.ts` (entra com o demo e vê extrato e avisos).
- [ ] **Step 2:** rodar contra o compose com `bootRun` dos serviços (ou as imagens), verde localmente. **Step 3:** job `frontend` no CI: `npm ci`, lint, unidade, build e Playwright contra o compose; relatório do Playwright como artefato em falha. **Step 4:** PR de teste com o job verde. **Step 5:** commit `test(e2e): fluxo do cliente no navegador com Playwright no CI`.
