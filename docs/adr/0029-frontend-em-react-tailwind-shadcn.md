# 0029 — Frontend em React, Tailwind e shadcn/ui

- **Status:** aceito; substitui a escolha do Angular e do Angular Material do [ADR 0005](0005-frontend-angular-e-bff-nestjs.md)
- **Data:** 2026-11-26

## Contexto
O web da etapa 9 funciona, mas o visual é básico. O usuário quer que quem abrir o app sinta, nos primeiros segundos, algo muito moderno e tecnológico, no nível do site da Apple. As ferramentas de design escolhidas (skill Impeccable, shadcn MCP e 21st MCP) rendem em React com Tailwind, não em Angular. A numeração segue o 0028, da etapa 10, ainda não mergeada.

## Decisão
- **SPA Vite + React + TypeScript + Tailwind CSS v4 + shadcn/ui**, servida como estático pelo mesmo nginx sem root, com fallback de SPA.
- **React Router** nas rotas e guards, **TanStack Query** nas chamadas ao BFF, **Motion** nas transições. Cartão e luzes de fundo em CSS puro.
- **Vitest + Testing Library** nos testes de unidade. Playwright continua, com seletores ajustados às telas novas.
- **Lógica de negócio em módulos TypeScript puros** (`frontend/web/src/lib/`): sessão, cliente do BFF (CSRF, 401, BFF fora), chave da transferência, centavos, telefone, senha e Problem Details. Os componentes só os usam.
- **Tema escuro único**, direção visual "Noite Apple" (`frontend/web/DESIGN.md`).
- **Sem mudança** no BFF, nas rotas do web, nas chamadas `/bff` nem nas regras (CSRF, chave da transferência, dinheiro em centavos).

## Consequências
- Positivas: as ferramentas de design rendem; o redesenho cabe no mesmo app, sem duplicar CI, Docker e testes; lógica de negócio testável sem React.
- Negativas: os testes de unidade do web são reescritos (os 224 atuais viram checklist); os e2e mantêm os cenários, mas com seletores novos; só tema escuro; o CI ganha o detector da Impeccable (`impeccable detect`, versão fixada), que falha com achados; o JavaScript inicial tem orçamento de 250 KB gzip.

## Alternativas consideradas
- **Manter o Angular:** o visual continua básico e as ferramentas de design não rendem.
- **App novo ao lado (`frontend/web-react`) e troca no fim:** os dois rodam juntos, mas CI, Docker e testes ficam duplicados e entra uma task de troca.
- **Next.js:** exigiria um servidor Node para o front, além do BFF, e mudaria como o nginx serve o web.
