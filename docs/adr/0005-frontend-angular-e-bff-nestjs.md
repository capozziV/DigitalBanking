# 0005 — Frontend Angular e BFF em NestJS

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto
O projeto terá frontend na **etapa 9**. Até lá, testes manuais usam uma coleção Bruno/Insomnia versionada no repositório. É preciso definir como o navegador autentica sem expor tokens.

## Decisão
- **Angular** (standalone components, signals) + **BFF em Node/NestJS**.
- Tokens Cognito ficam **só no BFF** (login OAuth2 com `openid-client`, sessão no Redis, refresh de token).
- Navegador usa **cookie `HttpOnly` + token CSRF**.
- BFF expõe **endpoints por tela** (agregação).
- Front e BFF na **mesma origem**: ALB público roteando `/` (Angular) e `/bff` (BFF), sem CORS.
- BFF gera o correlation ID (via `AsyncLocalStorage`) e envia `X-Correlation-Id` e o Bearer token ao API Gateway.
- `Idempotency-Key` gerada no Angular ao abrir o formulário de transferência; o BFF só repassa.
- Organização: `frontend/web`, `frontend/bff`, `frontend/shared` (opcional), workspaces npm/pnpm, job próprio no CI.

## Consequências
- Positivas: tokens fora do navegador; sem CORS; API sob medida por tela.
- Negativas: mais um serviço (BFF) com sessão e Redis; endpoints acoplados às telas.

## Pontos em aberto
- Biblioteca de componentes: Angular Material ou PrimeNG. Demais detalhes ficam para a etapa 9.

## Alternativas consideradas
- **Proxy genérico no BFF:** descartado em favor de endpoints por tela.
