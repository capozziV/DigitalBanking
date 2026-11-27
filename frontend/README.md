# Frontend

Aplicativo do cliente da etapa 9: `web` (React, [ADR 0029](../docs/adr/0029-frontend-em-react-tailwind-shadcn.md)), `bff` (NestJS) e `e2e` (Playwright), em npm workspaces.

## Versões (conferidas em 2026-11-19 nas fontes primárias)

| Peça               | Versão | Fonte                                                                                         |
| ------------------ | ------ | --------------------------------------------------------------------------------------------- |
| Node               | 24 LTS | `nodejs.org/dist/index.json` (24 "Krypton" e 26 "Lithium" são LTS; `engines` pede `>=24.15`)  |
| npm                | 11     | vem com o Node 24                                                                             |
| React, Vite, Tailwind | 19, 8, 4 | `frontend/web/package.json` (React 19, Vite 8, Tailwind 4, shadcn/ui)                    |
| NestJS             | 12.5.1 | registro npm (`@nestjs/core`, tag `latest`; CLI 12.0.13)                                      |
| Playwright         | 1.65.1 | registro npm (`@playwright/test`, tag `latest`)                                               |

Escolhemos o Node 24 (já instalado no ambiente) e não o 26: ambos são LTS e atendem ao Vite 8 e ao Nest 12. `.nvmrc` fixa o 24.

## Ferramentas

- Teste de unidade do `web`: Vitest com Testing Library e jsdom (`npm test -w web`). A lógica de negócio fica em `web/src/lib/`, sem React, testada sozinha.
- Design: skill Impeccable (`PRODUCT.md` e `DESIGN.md` em `web/`); o CI roda `impeccable detect` e falha com achados.
- Teste de unidade do `bff`: Vitest com SWC (`unplugin-swc`) para emitir os metadados dos decorators do Nest; HTTP com `supertest`.
- Lint: ESLint (flat config na raiz, `typescript-eslint`, `react-hooks` e `jsx-a11y`) e Prettier (`npm run format` formata).
- E2E: Playwright (Chromium). Sem `BASE_URL`, sobe o servidor do Vite (`npm run dev -w web`); com `BASE_URL`, testa a pilha indicada. Os specs em `e2e/tests/` rodam contra a pilha real (nginx, BFF, cinco serviços; sem `page.route`), cada um com e-mail e telefone próprios; o usuário de demonstração vem do `infra/local/seed.sh` (rode o seed antes). No CI, o job `frontend-e2e` sobe o perfil `apps` do compose com as imagens do job `images`, roda o seed e o Playwright com `BASE_URL=http://127.0.0.1`; o relatório e os traces saem como artefato `playwright-report` em falha.

## Tema

Tema escuro único, "Noite Apple": fundo preto, luzes difusas, painéis de vidro e o cartão 3D como peça de assinatura. Tokens em variáveis CSS do Tailwind; decisões visuais em `web/DESIGN.md` e `web/PRODUCT.md`.

## Comandos (na pasta `frontend/`)

```
npm ci
npm run lint     # ESLint + Prettier
npm test         # unidade do web e do bff
npm run build    # web (tsc + vite build, saída em web/dist) e bff (nest build)
npm run e2e      # Playwright (primeira vez: npx playwright install chromium)
npm run dev -w web   # Vite em desenvolvimento; /bff vai por proxy para http://127.0.0.1:3000
```

Para o `dev`, deixe o BFF no ar na porta 3000 (`npm run start:dev -w bff`, com as variáveis abaixo e o Redis do compose).

O BFF escuta na porta 3000 com prefixo global `/bff`; `GET /bff/health` devolve `{ "status": "UP" }`.

## Variáveis do BFF

| Variável            | Obrigatória   | Padrão                          | Uso                                                                                                                                                                                    |
| ------------------- | ------------- | ------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `IDENTITY_PROVIDER` | sim           | -                               | `cognito` (AWS) ou `local` (adaptador sobre o `mock-oauth2-server`)                                                                                                                    |
| `COGNITO_REGION`    | com `cognito` | -                               | região do user pool                                                                                                                                                                    |
| `COGNITO_CLIENT_ID` | com `cognito` | -                               | app client do BFF (sem segredo, `USER_PASSWORD_AUTH`)                                                                                                                                  |
| `REDIS_URL`         | não           | `redis://localhost:6379`        | sessões (`bff:session:`) e, no modo local, usuários (`bff:local-user:`)                                                                                                                |
| `COOKIE_SECURE`     | não           | `true`                          | `false` só no local sem HTTPS                                                                                                                                                          |
| `MOCK_OAUTH2_URL`   | modo `local`  | `http://localhost:8090/default` | emissor do mock; em contêiner, `http://mock-oauth2-server:8080/default` (o `iss` sai do host pedido)                                                                                   |
| `MOCK_OAUTH2_HOST`  | não           | -                               | modo `local`: cabeçalho `Host` do `POST /token`; o mock monta o `iss` com ele. No compose, `localhost:8090` (serviços em `bootRun`); com o perfil `apps`/CI, `mock-oauth2-server:8080` |
| `ACCOUNTS_URL`      | não           | `http://localhost:8081`         | serviço `accounts`                                                                                                                                                                     |
| `TRANSFERS_URL`     | não           | `http://localhost:8082`         | serviço `transfers`                                                                                                                                                                    |
| `NOTIFICATION_URL`  | não           | `http://localhost:8083`         | serviço `notification`                                                                                                                                                                 |
| `STATEMENT_URL`     | não           | `http://localhost:8084`         | serviço `statement`                                                                                                                                                                    |
| `CUSTOMER_URL`      | não           | `http://localhost:8085`         | serviço `customer`                                                                                                                                                                     |
| `PORT`, `LOG_LEVEL` | não           | `3000`, `info`                  | porta e nível do pino                                                                                                                                                                  |

No nginx local e no ALB, rota inexistente cai no `web` e devolve o `index.html` (200), inclusive uma rota de API que não existe.

Sessão e CSRF: cookie `bank_session` (`HttpOnly`, `SameSite=Strict`, `Secure` por `COOKIE_SECURE`) e cookie legível `bank_csrf`; todo método diferente de `GET` numa rota autenticada exige o cabeçalho `X-CSRF-Token` igual ao cookie. O token do Cognito nunca sai do BFF.

Teste de integração do adaptador local (precisa do Redis e do mock do compose):

```
docker compose -f infra/local/docker-compose.yml up -d --wait redis mock-oauth2-server
npm run test:integration -w bff
```
