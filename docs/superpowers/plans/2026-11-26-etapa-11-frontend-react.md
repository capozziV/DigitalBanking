# Etapa 11 — Frontend do capzzPay em React: plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** trocar o web Angular de `frontend/web` por um SPA em React + Tailwind + shadcn/ui com o visual Noite Apple, mantendo as rotas, as chamadas ao BFF e as regras de negócio.

**Architecture:** um SPA Vite estático servido pelo mesmo nginx sem root. Há três camadas: `src/lib/` com a lógica pura em TypeScript, sem React (cliente do BFF, sessão, dinheiro, telefone, senha, erros, chave da transferência); `src/design/` com o sistema visual (tokens, vidro, luzes, cartão 3D, movimento); e `src/pages/` e `src/shell/` com as telas, que só consomem as outras duas. O BFF, o nginx de borda e os serviços não mudam.

**Tech Stack:** Vite, React 19, TypeScript, Tailwind CSS v4, shadcn/ui, React Router, TanStack Query, Motion, Vitest + Testing Library + jsdom, ESLint + Prettier, Playwright (já existente), Impeccable, shadcn MCP.

**Spec:** [docs/superpowers/specs/2026-11-26-etapa-11-frontend-react-design.md](../specs/2026-11-26-etapa-11-frontend-react-design.md) (aprovada em 2026-11-26). Os executores leem a spec inteira.

## Global Constraints
- Código, nomes, testes, comentários e mensagens de log em inglês; textos da interface em português (pt-BR); commits em português, autor capozziV, sem trailers.
- Rotas da spec, iguais às de hoje: `/entrar`, `/criar-conta`, `/completar-cadastro`, `/inicio`, `/extrato`, `/transferir`, `/notificacoes` (`/avisos` → `/notificacoes`), `/minha-conta`, `/indisponivel`, `**` → `/`.
- Chamadas só às rotas `/bff/...` listadas na spec, com os mesmos métodos e corpos de hoje.
- Dinheiro em centavos (inteiro) do BFF até a tela; nunca `parseFloat`/`Number` em texto de valor; exibição `pt-BR` com `R$`.
- Tokens nunca no navegador: só o cookie do BFF. CSRF: não GET ao `/bff/` com `X-CSRF-Token` = cookie `bank_csrf`.
- Tema escuro único "Noite Apple"; sem Inter; animações só com `transform`/`opacity`; tudo desligado com `prefers-reduced-motion: reduce`.
- Orçamento: JavaScript inicial ≤ 250 KB gzip; Lighthouse mobile ≥ 90 em desempenho e acessibilidade (local, contra a imagem).
- `npx impeccable detect` sem achados em `frontend/web/src`; `audit` sem P0/P1 nas telas entregues.
- Rodapé "© 2026 capzz.labs"; nome do banco "capzzPay".
- Lint (`npm run lint`), testes (`npm test`) e build (`npm run build`) verdes em `frontend/`, rodados de verdade e relatados.

## Review Focus
1. **Duplo clique ou retentativa na confirmação da transferência:** uma intenção, uma chave, um POST efetivo. Teste em `transfer-attempt.test.ts` (Task 2) e em `TransferPage.test.tsx` (Task 7: dois cliques rápidos em "Confirmar" disparam um POST).
2. **Valor digitado pelo teclado numérico com zeros, vírgula ou acima do disponível:** `1`, `0`, `0`, `,`, `5` → 1005 centavos; acima do limite diário mostra o aviso e não habilita "Continuar". Testes em `money.test.ts` (Task 2) e `AmountStep.test.tsx` (Task 7).
3. **Sessão expirada no meio de uma tela:** um 401 fora de `/bff/auth/` limpa a sessão e leva a `/entrar` sem laço de redirecionamento. Testes em `bff-client.test.ts` (Task 2) e `guards.test.tsx` (Task 3).
4. **BFF fora (rede ou 5xx no carregamento da sessão):** vai a `/indisponivel`, e "Tentar de novo" volta. Teste em `session.test.ts` (Task 2) e `UnavailablePage.test.tsx` (Task 5).
5. **"Reduzir movimento" ligado:** o cartão não flutua, as luzes não se movem, as transições de tela viram corte seco. Teste em `motion.test.tsx` (Task 3).

---

## Estrutura de arquivos (depois da etapa)
```
frontend/web/
  index.html  vite.config.ts  tsconfig.json  components.json  package.json  Dockerfile  nginx.conf (igual)
  PRODUCT.md  DESIGN.md                      (Impeccable)
  src/
    main.tsx  app.tsx  routes.tsx  index.css
    lib/          bff-client.ts  session.ts  money.ts  phone.ts  password.ts  problem.ts
                  transfer-attempt.ts  api-types.ts  (+ *.test.ts)
    design/       tokens.css  AmbientLights.tsx  GlassPanel.tsx  AccountCard.tsx  motion.tsx
                  GradientText.tsx  (+ *.test.tsx)
    components/ui/   (shadcn)
    shell/        AppShell.tsx  SideNav.tsx  TabBar.tsx  NotificationBell.tsx  Footer.tsx  guards.tsx
    pages/        sign-in/  sign-up/  complete-profile/  unavailable/  home/  account/
                  statement/  transfer/  notifications/
    test/         setup.ts  render.tsx
frontend/e2e/tests/*.spec.ts, support.ts     (Task 8)
.mcp.json                                    (Task 1: shadcn)
```

## Ordem e paralelismo
- **Task 1** sozinha (base). Depois, **Tasks 2 e 3 em paralelo** (2 só toca `src/lib/`; 3 só toca `src/design/`, `src/shell/`, `src/routes.tsx` e `src/app.tsx`).
- Com 2 e 3 revisadas, **Tasks 4, 5, 6 e 7 em paralelo**, cada uma só nas suas pastas de `src/pages/`. Quem precisar de um componente shadcn novo roda `npx shadcn add <x>` (arquivo novo em `components/ui/`); se dois pedirem o mesmo, vale o primeiro a juntar, e o segundo descarta o seu no merge.
- **Task 8** (e2e, desempenho, qualidade) depois de 4–7. **Task 9** (docs) em paralelo com a 8.

---

### Task 1: base React, ferramentas e build (Lucas, `dev-frontend`)

**Files:**
- Delete: `frontend/web/angular.json`, `tsconfig.app.json`, `tsconfig.spec.json`, `src/app/**`, `src/main.ts`, `src/styles.scss`.
- Create: `frontend/web/vite.config.ts`, `index.html`, `tsconfig.json`, `components.json`, `src/main.tsx`, `src/app.tsx`, `src/index.css`, `src/test/setup.ts`, `src/test/render.tsx`, `src/app.test.tsx`, `/.mcp.json`, `frontend/web/PRODUCT.md`.
- Modify: `frontend/web/package.json`, `frontend/web/Dockerfile:17`, `frontend/eslint.config.*`, `frontend/package.json` (devDeps: tirar `angular-eslint`, pôr `eslint-plugin-react-hooks`, `eslint-plugin-jsx-a11y`), `.github/workflows/ci.yml` (job `frontend`, linhas ~474–497).

**Interfaces:**
- Produces: `npm run dev|build|test|lint` no workspace `web`; `vite.config.ts` com `server.proxy['/bff'] = 'http://127.0.0.1:3000'` e `build.outDir = 'dist'`; alias `@/` → `src/`; `src/test/render.tsx` exporta `renderWithProviders(ui: ReactElement, opts?: { route?: string }): RenderResult` (MemoryRouter + QueryClient novo por teste, sem retry).

- [ ] **Step 1:** Antes de qualquer outra coisa, guarde a lista de comportamentos dos 23 `*.spec.ts` do Angular em `frontend/web/MIGRATION-CHECKLIST.md`, com uma linha por `it(...)` (arquivo → nome do teste). Esse arquivo é o checklist das Tasks 2–7 e sai na Task 8.
- [ ] **Step 2:** Escreva `src/app.test.tsx`: `renders the capzzPay name` → `renderWithProviders(<App />)` mostra o texto `capzzPay`. Rode `npm test -w web` e confira que falha (o app ainda não existe).
- [ ] **Step 3:** Troque o Angular por Vite + React + TS (Vitest com jsdom e `src/test/setup.ts` com `@testing-library/jest-dom`), Tailwind v4 (`@tailwindcss/vite`) e `npx shadcn@latest init` (estilo new-york, base neutra, CSS variables, `components.json` com aliases `@/components`, `@/lib/utils`). O `App` mínimo renderiza `capzzPay`.
- [ ] **Step 4:** Rode `npm test -w web`, `npm run build -w web` e `npm run lint` em `frontend/` e confira que passam.
- [ ] **Step 5:** Ajuste o `Dockerfile`: `COPY --from=build /app/web/dist /usr/share/nginx/html`. O resto da imagem fica igual. Rode `docker build -f frontend/web/Dockerfile -t digitalbanking/web:etapa11 frontend` e `docker run --rm --entrypoint id digitalbanking/web:etapa11 -u` (esperado: `101`).
- [ ] **Step 6:** Ferramentas de design. Rode `npx impeccable install --providers=claude --scope=project` aceitando o hook e fixe a versão em `frontend/package.json` (devDependency `impeccable`). Rode `/impeccable init` para gerar `frontend/web/PRODUCT.md` com o conteúdo de "Intenção", "Anti-objetivos" e "Direção visual" da spec. Crie `/.mcp.json` com o shadcn MCP (`npx shadcn@latest mcp init --client claude`), sem nenhum segredo. Anote no relato quais arquivos o instalador criou.
- [ ] **Step 7:** CI: no job `frontend`, depois do lint, um passo `npx impeccable detect --github web/src` (falha se houver achados). Rode o mesmo comando localmente (esperado: exit 0).
- [ ] **Step 8:** Commit: `Etapa 11: base do web em React, Vite, Tailwind e shadcn`.

### Task 2: lógica pura em `src/lib/` (Lucas, `dev-frontend`)

**Files:** Create `frontend/web/src/lib/{api-types,bff-client,session,money,phone,password,problem,transfer-attempt}.ts`, cada um com `*.test.ts`. Fonte da regra: os arquivos Angular equivalentes na `main` (`git show origin/main:frontend/web/src/app/...`) e o `MIGRATION-CHECKLIST.md`.

**Interfaces (Produces, usadas pelas Tasks 3–7):**
- `api-types.ts`: `Entry`, `AccountSummary`, `DailyLimit`, `Customer`, `Me`, `NotificationDTO`, `NotificationPageDTO` com os mesmos campos de hoje; `type EntryKind = 'CREDIT' | 'DEBIT'`; `type AccountType = 'FREE'|'STANDARD'|'GOLD'|'BLACK'|'DIAMOND'`; `accountTypeLabel(type: string): string`.
- `bff-client.ts`: `class BffError extends Error { status: number; problem: ProblemDetails | null }`; `bff<T>(path: \`/bff/${string}\`, init?: { method?: 'GET'|'POST'|'PUT'|'PATCH'|'DELETE'; body?: unknown; headers?: Record<string,string> }): Promise<T>`; `onSessionLost(listener: () => void): () => void`; `onUnavailable(listener: () => void): () => void`. Envia `credentials: 'same-origin'`, JSON, e `X-CSRF-Token` nos não GET. Um 401 fora de `/bff/auth/` chama os ouvintes de sessão perdida, e erro de rede ou 502/503/504 chama os de indisponível. Sempre lança `BffError`.
- `session.ts`: store sem React com `getSnapshot(): SessionState` e `subscribe(fn): () => void`; `SessionState = { loaded: boolean; authenticated: boolean; email: string|null; customer: Customer|null; account: AccountSummary|null; unreadCount: number; loadFailed: boolean; signOutFailed: boolean }`. Ações: `load()`, `reload()`, `loadMe()`, `signIn(email, password)`, `signUp(email, password): Promise<{ confirmationRequired: boolean }>`, `confirm(email, code)`, `signOut()`, `clear()`, `refreshUnread({ fresh?: boolean })`, `startUnreadPolling()`, `stopUnreadPolling()`. Polling de 30 000 ms; uma resposta de não lidas mais antiga que um `clear`/`refresh` posterior é descartada (contador de geração). Exporta `useSession(): SessionState` (com `useSyncExternalStore`) como único ponto React.
- `money.ts`: `parseAmountToCents(text: string): number | null` (mesma regra de hoje); `formatCents(cents: number): string` (`Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`); `appendDigit(cents: number, digit: number): number` e `dropDigit(cents: number): number` para o teclado numérico (o valor cresce pela direita: 0 → 1 → 10 → 100 centavos); limite superior: `Number.MAX_SAFE_INTEGER` não estoura (`appendDigit` devolve o mesmo valor se passar de 99 999 999 999 centavos).
- `phone.ts`: `phoneDigits`, `formatPhone`, `displayPhone`, `isMobilePhone(value: string): boolean`.
- `password.ts`: `isSixDigitPassword(value: string): boolean`, `passwordsMatch(a: string, b: string): boolean`.
- `problem.ts`: `type ProblemDetails = { type?: string; title?: string; detail?: string; status?: number }`; `problemMessage(error: unknown, fallback: string): string` com a mesma tradução de hoje.
- `transfer-attempt.ts`: `openAttempt(intent: { owner: string|null; phone: string; amount: number }): Promise<TransferAttempt>` e `settleAttempt(attempt: TransferAttempt): void`, com `TransferAttempt = { slot: string; idempotencyKey: string; resumed: boolean }` e a mesma regra do `TransferAttemptStore` (prefixo `transfer-attempt:`, SHA-256, nenhum `await` entre ler e gravar, falha fechada).

- [ ] **Step 1:** Para cada módulo, porte para Vitest cada caso do checklist que pertence a ele e rode `npm test -w web` para ver os testes falharem. Casos obrigatórios além do checklist: `bff-client.test.ts` → `adds X-CSRF-Token from bank_csrf on POST`, `does not add it on GET`, `401 outside /bff/auth notifies session lost once`, `401 on /bff/auth/sign-in does not`, `network error notifies unavailable`; `money.test.ts` → `appendDigit 1,0,0,5 gives 1005`, `dropDigit 1005 gives 100`, `formatCents 1005 is "R$ 10,05"` (comparar com ` `); `transfer-attempt.test.ts` → `two concurrent opens of the same intent get the same key`, `settle of an older attempt keeps the newer key`, `storage failure throws`; `session.test.ts` → `stale unread answer is discarded after clear`, `BFF down on load sets loadFailed`.
- [ ] **Step 2:** Implemente os módulos até os testes passarem.
- [ ] **Step 3:** Marque no `MIGRATION-CHECKLIST.md` cada caso portado (`[x]` + nome do teste novo).
- [ ] **Step 4:** Rode lint, testes e build em `frontend/`.
- [ ] **Step 5:** Commit: `Etapa 11: lógica do web em módulos TypeScript puros`.

### Task 3: sistema visual, shell e rotas (Lucas, `dev-frontend`)

**Files:** Create `frontend/web/src/design/*`, `src/shell/*`, `src/routes.tsx`, `frontend/web/DESIGN.md`; Modify `src/app.tsx`, `src/index.css`. Telas ainda como marcadores (`<h1>` com o título), cada uma num arquivo `src/pages/<pasta>/<Nome>Page.tsx` que as Tasks 4–7 substituem.

**Interfaces:**
- Consumes: `useSession`, `session.load`, `onSessionLost`, `onUnavailable` (Task 2), apenas por import. Enquanto a Task 2 não estiver na branch, use o contrato acima com um stub local em `src/shell/session-stub.ts`, apagado no merge.
- Produces:
  - `tokens.css`: variáveis `--bg`, `--fg`, `--muted`, `--line`, `--glass`, `--positive`, `--negative`, `--accent-1..3`, ligadas ao tema do Tailwind e do shadcn.
  - `<AmbientLights intensity?: 'low'|'high' />` (luzes difusas atrás do conteúdo); `<GlassPanel as?: ElementType className? />`; `<GradientText>`.
  - `<AccountCard type: AccountType; last4?: string; size?: 'sm'|'md'|'lg'; still?: boolean />` com acabamento por tipo (Free, Standard, Gold, Black, Diamond), flutuação e brilho em CSS, e `aria-label="Cartão {Tipo}"`.
  - `motion.tsx`: `usePrefersReducedMotion(): boolean`, `<PageTransition>`, `<Reveal delay?: number>` (entrada suave) e `<CountUp cents: number />`, que termina sempre no valor exato e sem animar quando o movimento está reduzido.
  - `<AppShell>` com `<SideNav>` (≥ 1024 px) e `<TabBar>` (< 1024 px), os links Início, Extrato, Transferir, Notificações e Minha conta, `<NotificationBell>` com o contador (`unreadCount`) e `<Footer>` ("© 2026 capzz.labs").
  - `guards.tsx`: `<GuestOnly>`, `<UnregisteredOnly>`, `<CustomerOnly>`, com a mesma lógica dos guards Angular (`authGuard`/`profileGuard`/`unregisteredGuard`/`guestGuard`), e `/indisponivel` quando `loadFailed`.
  - `routes.tsx`: todas as rotas da spec, com lazy load das telas de cliente e títulos de documento ("Início · capzzPay" etc.).

- [ ] **Step 1:** Testes que falham: `guards.test.tsx` (visitante em `/inicio` → `/entrar`; autenticado sem cliente → `/completar-cadastro`; cliente em `/entrar` → `/inicio`; 401 durante a tela → `/entrar` uma vez só); `routes.test.tsx` (`/avisos` → `/notificacoes`, `/qualquer` → `/`); `motion.test.tsx` (com `matchMedia('(prefers-reduced-motion: reduce)')` verdadeiro, o `AccountCard` tem `data-still="true"` e o `CountUp` mostra o valor final de imediato); `AccountCard.test.tsx` (um nome acessível por tipo); `AppShell.test.tsx` (o sininho mostra 3 com `unreadCount: 3`; os cinco links existem com o nome acessível).
- [ ] **Step 2:** Implemente com shadcn (Button, Sheet ou Popover para o sininho, Badge) e CSS próprio para as luzes e o cartão, seguindo os mockups da spec. Use a Impeccable: `/impeccable shape` antes de codificar o shell e `/impeccable critique` no `AccountCard` e no `AppShell` depois.
- [ ] **Step 3:** Rode `/impeccable document` para gerar o `DESIGN.md` com tokens, tipografia (sem Inter; pilha de sistema com SF Pro e uma alternativa livre), raios, sombras e regras de movimento.
- [ ] **Step 4:** Rode os testes, o lint, o build e `npx impeccable detect web/src`. Em `npm run dev` com o BFF local, confira a navegação e o comportamento no celular (DevTools, 390 px).
- [ ] **Step 5:** Commit: `Etapa 11: sistema visual Noite Apple, shell e rotas`.

### Task 4: Notificações e sininho completo (Lucas, `dev-frontend`)
**Files:** `src/pages/notifications/*`, `src/shell/NotificationBell.tsx` (só o conteúdo do menu).
**Interfaces:** Consumes `bff`, `NotificationPageDTO`, `session.refreshUnread`, `GlassPanel`, `Reveal`.
- [ ] **Step 1:** Porte para testes os casos do checklist de `notifications.component.spec.ts`, `notification-bell.component.spec.ts` e `notifications.ts`, mais `marking as read refreshes the unread count with fresh: true` e `empty list shows the empty state`. Confira que falham.
- [ ] **Step 2:** Implemente a lista (vidro, agrupada por dia, nome da contraparte, estado vazio desenhado com `/impeccable onboard`), "Marcar como lidas" e o menu do sininho com as recentes.
- [ ] **Step 3:** Testes, lint, build, `impeccable detect`; `/impeccable audit` na tela, sem P0/P1, com o resultado no relato.
- [ ] **Step 4:** Commit: `Etapa 11: tela de notificações e sininho`.

### Task 5: Entrar, Criar conta, Completar cadastro e Indisponível (Lucas, `dev-frontend`)
**Files:** `src/pages/{sign-in,sign-up,complete-profile,unavailable}/*`.
**Interfaces:** Consumes `session.signIn|signUp|confirm|loadMe|reload`, `bff('/bff/customers', POST)`, `isSixDigitPassword`, `passwordsMatch`, `formatPhone`, `isMobilePhone`, `problemMessage`, `AmbientLights`, `AccountCard`.
- [ ] **Step 1:** Porte os casos do checklist das quatro telas, mais `wrong password shows the BFF message on the form and stays on /entrar`, `phone input masks while typing`, `Unavailable "Tentar de novo" calls session.reload and leaves when it succeeds`. Confira que falham.
- [ ] **Step 2:** Implemente na direção do mockup "Entrar" (luzes, título com gradiente, cartão Black flutuando). Campos com `<Label>` do shadcn e rótulos visíveis.
- [ ] **Step 3:** Testes, lint, build, `impeccable detect`, `/impeccable audit` sem P0/P1.
- [ ] **Step 4:** Commit: `Etapa 11: telas de entrada e cadastro`.

### Task 6: Início, Extrato e Minha conta (Lucas, `dev-frontend`)
**Files:** `src/pages/{home,statement,account}/*`.
**Interfaces:** Consumes `bff('/bff/home')`, `bff('/bff/accounts', POST)`, `bff('/bff/statement?cursor=')`, `bff('/bff/me/phone', PUT/PATCH como hoje)`, `bff('/bff/accounts/close', POST)`, `formatCents`, `CountUp`, `AccountCard`, `GlassPanel`, `Reveal`.
- [ ] **Step 1:** Porte os casos do checklist de `home`, `statement`, `account`, `entry-list` e `entry-icon`, mais `statement groups entries by São Paulo day ("Hoje", "Ontem", data)`, `"Carregar mais" sends the cursor and appends`, `home shows "Abrir conta" when account is null`, `close account asks for confirmation in a dialog`. Confira que falham.
- [ ] **Step 2:** Implemente como nos mockups "Início", "Extrato" e "Minha conta" (celular e computador). Agrupe por dia no fuso `America/Sao_Paulo`.
- [ ] **Step 3:** Testes, lint, build, `impeccable detect`, `/impeccable audit` sem P0/P1.
- [ ] **Step 4:** Commit: `Etapa 11: início, extrato e minha conta`.

### Task 7: Transferir em passos e resultado (Lucas, `dev-frontend`)
**Files:** `src/pages/transfer/{TransferPage,RecipientStep,AmountStep,ConfirmStep,TransferResult}.tsx` + testes.
**Interfaces:** Consumes `bff('/bff/transfers/recipient')`, `bff('/bff/transfers', POST, { headers: { 'Idempotency-Key' } })`, `bff('/bff/transfers/{id}')`, `openAttempt`, `settleAttempt`, `appendDigit`, `dropDigit`, `formatCents`, `problemMessage`, `bff('/bff/home')` (limite diário, como hoje).
- [ ] **Step 1:** Porte os casos do checklist de `transfer.component.spec.ts` e `transfer-attempt.store.spec.ts` que dependem da tela, mais `AmountStep: keys 1,0,0,5 show "R$ 10,05"`, `AmountStep: above the daily available disables "Continuar" and shows the limit`, `ConfirmStep: two fast clicks send one POST with one Idempotency-Key`, `reload on a pending attempt resumes with the same key and shows the status`, `final status calls settleAttempt; pending does not`. Confira que falham.
- [ ] **Step 2:** Implemente os três passos e o resultado como nos mockups. A chave só é aberta na confirmação, com dono, telefone e valor, e o acompanhamento do status segue o de hoje. A barra de passos é navegável para trás sem perder o que foi digitado.
- [ ] **Step 3:** Testes, lint, build, `impeccable detect`, `/impeccable audit` sem P0/P1.
- [ ] **Step 4:** Commit: `Etapa 11: transferência em passos`.

### Task 8: e2e, desempenho e acabamento (Lucas, `dev-frontend`)
**Files:** `frontend/e2e/tests/*.spec.ts`, `frontend/e2e/tests/support.ts`; delete `frontend/web/MIGRATION-CHECKLIST.md`.
- [ ] **Step 1:** Confira que todo item do `MIGRATION-CHECKLIST.md` está `[x]` ou tem a justificativa de por que não se aplica mais. Depois, apague o arquivo.
- [ ] **Step 2:** Atualize os cinco cenários Playwright (`skeleton`, `demo-user`, `sign-up-and-first-transfer` com os passos novos, `daily-limit-refusal`, `notification-bell`) para as telas novas, com seletores por papel e rótulo e espera pelo valor final do `CountUp`.
- [ ] **Step 3:** Suba a stack isolada (projeto compose próprio, nunca o `capzzpay`) com as imagens da branch e rode `npm run e2e` (esperado: 5 passam) e `infra/local/smoke.sh`.
- [ ] **Step 4:** Rode o Lighthouse mobile contra a imagem (≥ 90 em desempenho e acessibilidade) e meça o JS inicial gzip (≤ 250 KB). Os números entram no relato.
- [ ] **Step 5:** Rode `/impeccable polish` nas telas, depois `impeccable detect` (0 achados) e `audit` (sem P0/P1).
- [ ] **Step 6:** Lint, testes e build de `frontend/`. Commit: `Etapa 11: e2e, desempenho e acabamento`.

### Task 9: documentação (Clara, `documentador`)
**Files:** `docs/adr/00NN-frontend-em-react-tailwind-shadcn.md` (próximo número livre; 0029 se a etapa 10 entrar antes), `docs/arquitetura.md` (seção do frontend, linha ~136), `frontend/README.md`, `docs/rodando-localmente.md` (comandos do web).
- [ ] **Step 1:** ADR curto: contexto (visual básico, ferramentas de design rendem em React), decisão (React + Tailwind + shadcn, SPA Vite), alternativas (manter Angular, Next.js, app ao lado) e consequências. Marca o ADR 0024 como "substituído em parte" no trecho do Angular.
- [ ] **Step 2:** Atualizar a arquitetura, o README do frontend e o guia local (`npm run dev -w web` com proxy para o BFF em 3000).
- [ ] **Step 3:** Commit: `Etapa 11: ADR e documentação do frontend em React`.

## Fechamento da etapa (coordenadora)
Revisão da branch inteira pela Beatriz, PR `etapa-11` → `main` com o CI verde (inclui `frontend`, `frontend-e2e` e `ci-ok`), merge, `graphify update .`, board e `docs/status.md`. Depois do merge, o prompt do Lucas (`.claude/agents/dev-frontend.md`) passa de Angular para React com `mattpocock-skills:writing-for-agents`, com o aval do usuário.
