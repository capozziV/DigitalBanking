# 0005 — Frontend Angular e BFF em NestJS

- **Status:** aceito; o login OAuth2 com `openid-client` foi trocado pelo login pela API do Cognito no [ADR 0024](0024-login-e-telefone-do-app.md); o Angular e o Angular Material foram substituídos por React, Tailwind e shadcn/ui no [ADR 0029](0029-frontend-em-react-tailwind-shadcn.md) (o BFF e o resto seguem valendo)
- **Data:** 2026-10-21

## Contexto
O projeto terá frontend na **etapa 9**. Até lá, testes manuais usam uma coleção Bruno/Insomnia versionada no repositório. É preciso definir como o navegador autentica sem expor tokens.

## Decisão
- **Angular** (standalone components, signals) + **BFF em Node/NestJS**.
- Tokens Cognito ficam **só no BFF** (login OAuth2 com `openid-client`, sessão no Redis, refresh de token).
- Navegador usa **cookie `HttpOnly` + token CSRF**.
- **Angular Material** como biblioteca de componentes (decidido em 2026-11-12).
- BFF expõe **endpoints por tela** (agregação).
- Front e BFF na **mesma origem**: ALB público roteando `/` (Angular) e `/bff` (BFF), sem CORS.
- BFF gera o correlation ID (via `AsyncLocalStorage`) e envia `X-Correlation-Id` e o Bearer token ao API Gateway.
- `Idempotency-Key` gerada no Angular ao abrir o formulário de transferência; o BFF só repassa. *(Corrigido em 2026-11-22: ver a emenda.)*
- Organização: `frontend/web`, `frontend/bff`, `frontend/shared` (opcional), workspaces npm/pnpm, job próprio no CI.

## Consequências
- Positivas: tokens fora do navegador; sem CORS; API sob medida por tela.
- Negativas: mais um serviço (BFF) com sessão e Redis; endpoints acoplados às telas.

## Alternativas consideradas
- **Proxy genérico no BFF:** descartado em favor de endpoints por tela.
- **PrimeNG:** descartado em favor do Angular Material.

## Emenda de 2026-11-22
Três afirmações da decisão não valem como escritas (o texto original fica acima, como história):
- **Borda:** não há "ALB público". A rota real é API Gateway -> ALB interno -> web (`/`) e BFF (`/bff`); a mesma origem se mantém.
- **Chamadas do BFF:** o BFF não envia o Bearer token ao API Gateway. Chama os Services do cluster direto, pelo DNS de Service, com o Bearer token e o `X-Correlation-Id`.
- **Chave da transferência:** gerar ao abrir o formulário causava débito em dobro na retentativa. A chave é gerada no Angular por tentativa (usuário + telefone + valor), guardada no `sessionStorage` da aba até o estado final aparecer; o BFF só repassa. Limite: outra aba gera chave nova (#241).
