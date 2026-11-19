# Frontend

Aplicativo do cliente da etapa 9: `web` (Angular), `bff` (NestJS) e `e2e` (Playwright), em npm workspaces.

## Versões (conferidas em 2026-11-19 nas fontes primárias)

| Peça               | Versão | Fonte                                                                                         |
| ------------------ | ------ | --------------------------------------------------------------------------------------------- |
| Node               | 24 LTS | `nodejs.org/dist/index.json` (24 "Krypton" e 26 "Lithium" são LTS; Angular 22 exige `^24.15`) |
| npm                | 11     | vem com o Node 24                                                                             |
| Angular e Material | 22.3.0 | registro npm (`@angular/cli`, `@angular/material`, tag `latest`)                              |
| NestJS             | 12.5.1 | registro npm (`@nestjs/core`, tag `latest`; CLI 12.0.13)                                      |
| Playwright         | 1.65.1 | registro npm (`@playwright/test`, tag `latest`)                                               |

Escolhemos o Node 24 (já instalado no ambiente) e não o 26: ambos são LTS e atendem ao Angular 22 e ao Nest 12. `.nvmrc` fixa o 24.

## Ferramentas

- Teste de unidade do `web`: Vitest pelo `ng test` (padrão do Angular 22, com jsdom).
- Teste de unidade do `bff`: Vitest com SWC (`unplugin-swc`) para emitir os metadados dos decorators do Nest; HTTP com `supertest`.
- Lint: ESLint (flat config na raiz, `typescript-eslint` e `angular-eslint`) e Prettier (`npm run format` formata).
- E2E: Playwright (Chromium). Sem `BASE_URL`, sobe o `ng serve` em `127.0.0.1:4200`; com `BASE_URL`, testa a pilha indicada.

## Tema

Tema próprio sobre o Angular Material, sem cor: fundo `#f5f5f7`, texto `#1d1d1f`, secundário `#86868b`, fonte do sistema (`-apple-system`, SF), cartões brancos com raio de 18 px e sombra leve. Tokens em `web/src/styles.scss`.

## Comandos (na pasta `frontend/`)

```
npm ci
npm run lint     # ESLint + Prettier
npm test         # unidade do web e do bff
npm run build    # web (ng build) e bff (nest build)
npm run e2e      # Playwright (primeira vez: npx playwright install chromium)
```

O BFF escuta na porta 3000 com prefixo global `/bff`; `GET /bff/health` devolve `{ "status": "UP" }`.

## Variáveis do BFF

| Variável            | Obrigatória   | Padrão                          | Uso                                                                                                  |
| ------------------- | ------------- | ------------------------------- | ---------------------------------------------------------------------------------------------------- |
| `IDENTITY_PROVIDER` | sim           | -                               | `cognito` (AWS) ou `local` (adaptador sobre o `mock-oauth2-server`)                                  |
| `COGNITO_REGION`    | com `cognito` | -                               | região do user pool                                                                                  |
| `COGNITO_CLIENT_ID` | com `cognito` | -                               | app client do BFF (sem segredo, `USER_PASSWORD_AUTH`)                                                |
| `REDIS_URL`         | não           | `redis://localhost:6379`        | sessões (`bff:session:`) e, no modo local, usuários (`bff:local-user:`)                              |
| `COOKIE_SECURE`     | não           | `true`                          | `false` só no local sem HTTPS                                                                        |
| `MOCK_OAUTH2_URL`   | modo `local`  | `http://localhost:8090/default` | emissor do mock; em contêiner, `http://mock-oauth2-server:8080/default` (o `iss` sai do host pedido) |
| `ACCOUNTS_URL`      | não           | `http://localhost:8081`         | serviço `accounts`                                                                                   |
| `TRANSFERS_URL`     | não           | `http://localhost:8082`         | serviço `transfers`                                                                                  |
| `NOTIFICATION_URL`  | não           | `http://localhost:8083`         | serviço `notification`                                                                               |
| `STATEMENT_URL`     | não           | `http://localhost:8084`         | serviço `statement`                                                                                  |
| `CUSTOMER_URL`      | não           | `http://localhost:8085`         | serviço `customer`                                                                                   |
| `PORT`, `LOG_LEVEL` | não           | `3000`, `info`                  | porta e nível do pino                                                                                |

Sessão e CSRF: cookie `bank_session` (`HttpOnly`, `SameSite=Strict`, `Secure` por `COOKIE_SECURE`) e cookie legível `bank_csrf`; todo método diferente de `GET` numa rota autenticada exige o cabeçalho `X-CSRF-Token` igual ao cookie. O token do Cognito nunca sai do BFF.

Teste de integração do adaptador local (precisa do Redis e do mock do compose):

```
docker compose -f infra/local/docker-compose.yml up -d --wait redis mock-oauth2-server
npm run test:integration -w bff
```
