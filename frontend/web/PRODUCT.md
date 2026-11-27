# Product

<!-- impeccable:product-schema 1 -->

<!-- Fatos inferidos, não respondidos numa entrevista: a sessão do agente não tinha ferramenta de pergunta ao usuário.
Tudo vem da spec da etapa 11 (docs/superpowers/specs/2026-11-26-etapa-11-frontend-react-design.md, aprovada pelo
usuário em 2026-11-26) e do CONTEXT.md. -->

## Platform

web

## Stack

React, Vite, Tailwind CSS v4 e shadcn/ui (decisão do usuário na spec da etapa 11), no lugar do Angular. Fala só com o BFF
NestJS em `/bff`; servido por nginx. Só tema escuro nesta etapa.

## Users

Quem abre o app do portfólio: recrutadores, pessoas conhecidas e quem avalia o projeto. Usam o celular ou o computador,
sem contexto prévio, e decidem nos primeiros segundos se o produto parece moderno e bem feito. O cliente do banco
(simulado) tem uma conta capzzPay: vê saldo, extrato, notificações e faz transferências por telefone.

## Product Purpose

O capzzPay é a parte do portfólio que as pessoas veem: um banco digital simulado (sem dinheiro nem dados reais) em
microsserviços. Esta etapa reescreve o web para que quem abrir o app sinta, nos primeiros segundos, algo muito moderno e
tecnológico, no nível de um site da Apple, com animação diferenciada. Sucesso: o app mantém todo o comportamento de hoje
(mesmas regras de negócio e conversa com o BFF) e ganha a cara nova.

## Positioning

Um banco simulado cujo valor está em resolver bem consistência, idempotência, mensageria confiável, cache e
observabilidade, agora com uma interface de acabamento visível: "Noite Apple".

## Operating Context

Entrar e cadastrar no Cognito (via BFF, cookie de sessão HttpOnly e CSRF), ver saldo e cheque especial, extrato, limite
diário, notificações e transferir por telefone em passos. Dinheiro trafega em centavos (inteiro) e só é formatado na
exibição. O status da transferência vem por polling até o estado final.

## Capabilities and Constraints

- Interface em português (pt-BR); código em inglês. Termos do domínio em `CONTEXT.md` (Cliente, Conta, Lançamento,
  Extrato, Notificação, Transferência, Limite diário).
- Tokens do Cognito ficam só no BFF; o navegador nunca os vê.
- A chave de idempotência da transferência é aberta só na confirmação, com dono, telefone e valor.
- Fora do escopo: página pública de apresentação antes do login (#262), tema claro, telas novas, mudanças no BFF.

## Brand Commitments

O nome do banco é **capzzPay**; rodapé "© 2026 capzz.labs"; "Notificações" no lugar de "Avisos"; o nome da pessoa, e
não o id da conta, nas notificações. Direção visual escolhida pelo usuário entre seis mockups: **Noite Apple** (fundo
preto puro com luzes difusas azul e lilás em movimento lento; título grande de tipografia apertada com gradiente
animado azul, lilás e rosa; painéis de vidro fosco; o cartão 3D flutuante, com brilho periódico, é a peça de assinatura
e muda o acabamento pelo tipo de conta). Celular: barra de abas embaixo; computador: barra lateral.

## Evidence on Hand

Spec e mockups aprovados em `docs/superpowers/specs/2026-11-26-etapa-11-frontend-react-design.md`. Não há depoimentos,
clientes reais nem métricas de uso: nada disso deve ser inventado.

## Product Principles

1. Primeiro impacto: o app tem de parecer excepcional nos primeiros segundos, no celular antes do computador.
2. Movimento a serviço da ação: animação nunca atrasa a tarefa e respeita "reduzir movimento".
3. Nada de cara de site genérico de IA: sem Inter em tudo, gradiente roxo-azul sem propósito, card dentro de card ou
   ícone em quadradinho sobre cada título.
4. Zero regressão de comportamento: sem transferência em dobro numa retentativa, sem valor arredondado, sem erro do BFF
   sem mensagem.

## Accessibility & Inclusion

Animações desligadas com `prefers-reduced-motion` (o cartão fica parado, as luzes param). Contraste adequado no tema
escuro e navegação por teclado nos formulários.
