# Etapa 11 — Frontend do capzzPay em React, com redesenho

**Data:** 2026-11-26 · **Cartão:** [#261](https://github.com/capozziV/DigitalBanking/issues/261) · **Status:** aprovada pelo usuário em 2026-11-26

## Intenção
O capzzPay é a parte do portfólio que as pessoas veem. Hoje o web funciona, mas o visual é básico. O usuário quer que quem abrir o app (recrutador, amigo) sinta nos primeiros segundos que está diante de algo **muito moderno e tecnológico, no nível de um site da Apple, com animação diferenciada**.

Para isso, o web sai do Angular para **React + Tailwind + shadcn/ui**, que é onde as ferramentas de design escolhidas rendem: a skill **Impeccable** para decidir e revisar o design, o **shadcn MCP** e o **21st MCP** para trazer componentes.

## Objetivos
- Reescrever o web em React + Tailwind + shadcn/ui, no lugar, em `frontend/web`.
- Redesenhar todas as telas na direção visual **Noite Apple** (abaixo).
- Manter tudo o que o web faz hoje, com as mesmas regras de negócio e a mesma conversa com o BFF.
- Responsivo, pensado primeiro para o celular.

## Fora do escopo
- Página de apresentação pública antes do login e login como vitrine: ficam no cartão [#262](https://github.com/capozziV/DigitalBanking/issues/262).
- Qualquer mudança no BFF, nos serviços Kotlin, no Terraform ou no chart Helm, além do que o build novo do web exigir.
- Tema claro. Só tema escuro nesta etapa.
- Telas novas. A única mudança de fluxo é a transferência em passos (abaixo).
- Totais "entrou/saiu no mês" no extrato: exigem uma rota nova no BFF e ficam no cartão [#263](https://github.com/capozziV/DigitalBanking/issues/263).

## Anti-objetivos (falha mesmo se funcionar)
- Cara de "site genérico de IA": Inter em tudo, gradiente roxo-azul sem propósito, card dentro de card, ícone em quadradinho sobre cada título. O detector da Impeccable tem de passar.
- Animação que atrapalha: movimento que atrasa a ação, que não respeita "reduzir movimento" ou que derruba o desempenho no celular.
- Regressão de comportamento: transferência em dobro numa retentativa, valor arredondado, erro do BFF sem mensagem.

## Direção visual: Noite Apple (escolha do usuário)
Escolhida no navegador entre seis mockups (`.superpowers/brainstorm/`, fora do git).
- Fundo preto puro, com luzes de cor difusas (azul e lilás) que se movem devagar.
- Título grande, de tipografia apertada, com gradiente animado (azul → lilás → rosa) no trecho de destaque.
- Painéis de vidro fosco (fundo translúcido, borda clara fina, desfoque) para saldo, lançamentos e notificações.
- **Peça de assinatura:** o cartão 3D flutuando, inclinado, com um brilho que passa por ele de tempos em tempos. O usuário destacou esse cartão.

Propostas da coordenadora, aceitas pelo usuário no fluxo da conversa:
- O cartão aparece no início, na conta e no login. O acabamento muda pelo **tipo de conta** (Free, Standard, Gold, Black, Diamond).
- Animações de entrada suave dos itens e de transição entre telas, desligadas com `prefers-reduced-motion` (o cartão fica parado, as luzes param).
- Celular: barra de abas embaixo. Computador: barra lateral.
- O login usa o fundo com as luzes e o cartão, sem virar vitrine (#262).

```
┌───────────────────────────────────────────┐
│ capzzPay                        🔔  ◉     │
│ Seu dinheiro.                             │
│ Sem atrito. (gradiente)      ╭─────────╮  │
│                              │ BLACK ✦ │  │  ← flutua e brilha
│ Saldo disponível             │ •••• 48 │  │
│ R$ 12.480,90                 ╰─────────╯  │
│ ┌ vidro ───────────────────────────────┐  │
│ │ ↓ Ana Souza          + R$ 250,00     │  │
│ │ ↑ Bruno Lima         − R$ 89,90      │  │
│ └──────────────────────────────────────┘  │
└───────────────────────────────────────────┘
```

## Mockups das telas (aprovados pelo usuário em 2026-11-26)
Em `.superpowers/brainstorm/*/content/telas-noite-apple.html`, fora do git. Resumo para quem constrói:
- **Entrar:** luzes no fundo, título com gradiente, cartão Black flutuando, e-mail, senha e "Criar conta".
- **Início:** saudação com o nome, **"Saldo"** em destaque (pode ser negativo) e, embaixo, "Conta {Tipo} · Saldo disponível {saldo + cheque especial}". *Mudança do usuário em 2026-11-26: o saldo disponível em destaque parecia dinheiro em conta.*, cartão do tipo da conta, atalhos (Transferir, Extrato, Limite) e últimos lançamentos com a contraparte.
- **Transferir em passos:** 1) telefone do destino, com o nome encontrado; 2) valor, com teclado numérico grande e a barra do limite diário disponível; 3) confirmar. Depois, uma tela de resultado animada (pendente → concluída, recusada ou devolvida). As chamadas ao BFF e a regra da chave não mudam: a chave é aberta só na confirmação, com dono, telefone e valor, como hoje.
- **Extrato:** lançamentos agrupados por dia, com a hora, o saldo depois de cada um e "Carregar mais" pelo cursor.
- **Minha conta:** cartão do tipo de conta, tipo, cheque especial, limite diário, celular (editável), Sair e Encerrar conta.
- **Mudanças do usuário depois de ver o app (2026-11-26):**
  - Login no celular: cartão em cima, formulário embaixo (no computador segue lado a lado, cartão à esquerda).
  - Seta de voltar (←) no canto superior esquerdo de toda tela interna, menos o Início. Na transferência volta ao passo anterior sem perder o digitado (no 1º passo, ao Início); nas outras telas volta à tela anterior do histórico ou, sem histórico do app, ao Início.
  - O mesmo "Saldo" em destaque e "Saldo disponível" embaixo vale para Minha conta e para o computador.
- **Computador:** barra lateral com o sininho e o contador, e o início em blocos de vidro (saldo com ações, cartão, últimos lançamentos, limite diário).

## O que o web faz hoje (e continua fazendo)
**Telas e rotas** (os caminhos ficam iguais, porque o nginx e os e2e dependem deles):

| Rota | Tela | Acesso |
|---|---|---|
| `/entrar` | Entrar | só visitante |
| `/criar-conta` | Criar conta (com confirmação do código) | só visitante |
| `/completar-cadastro` | Completar cadastro (nome, telefone) | autenticado sem cliente |
| `/inicio` | Início: saldo, cartão, últimos lançamentos, abrir conta | cliente |
| `/extrato` | Extrato com paginação por cursor | cliente |
| `/transferir` | Transferir por telefone | cliente |
| `/notificacoes` (e `/avisos` → `/notificacoes`) | Notificações | cliente |
| `/minha-conta` | Minha conta: telefone, tipo, limite, encerrar | cliente |
| `/indisponivel` | BFF fora | qualquer um |
| `**` | volta para `/` | — |

**Chamadas ao BFF** (não mudam): `/bff/auth/session`, `/bff/auth/sign-in`, `/bff/auth/sign-up`, `/bff/auth/confirm`, `/bff/auth/sign-out`, `/bff/me`, `/bff/me/phone`, `/bff/customers`, `/bff/home`, `/bff/accounts`, `/bff/accounts/close`, `/bff/statement`, `/bff/transfers/recipient`, `/bff/transfers`, `/bff/transfers/{id}`, `/bff/notifications`, `/bff/notifications/read`, `/bff/notifications/unread-count`.

**Regras que a reescrita tem de manter** (cada uma com teste):
- **Sessão:** o navegador nunca vê tokens, só o cookie do BFF. Os guards de visitante, de autenticado sem cliente e de cliente continuam com a mesma lógica.
- **CSRF:** chamadas não GET ao `/bff/` levam `X-CSRF-Token` com o valor do cookie `bank_csrf` (double submit).
- **401:** fora de `/bff/auth/`, limpa a sessão e leva a `/entrar`. As chamadas de auth mostram o próprio erro no formulário.
- **BFF fora:** leva a `/indisponivel`, como hoje.
- **Chave da transferência:** a mesma regra do `TransferAttemptStore` (dono + telefone + valor → digest SHA-256 → slot no `sessionStorage`). Mesma intenção, mesma chave, até mostrar um status final. Nenhum `await` entre ler e gravar a chave, falha fechada (sem chave, sem POST) e `settle` só apaga a chave da própria tentativa. O limite conhecido (#241, outra aba gera chave nova) continua igual.
- **Dinheiro:** sempre em centavos (inteiro) do BFF até a tela. A digitação do valor converte para centavos sem `float`, e a exibição é em `R$` com `pt-BR`.
- **Telefone:** a mesma máscara e a mesma validação de celular brasileiro.
- **Problem Details:** as mensagens de erro do BFF (`title`/`detail`) aparecem como hoje, com a mesma tradução dos casos conhecidos (limite diário, saldo, conta bloqueada).
- **Sininho:** a contagem de não lidas consultada a cada 30 s, descartando a resposta de uma consulta antiga, e o menu das notificações recentes com "marcar como lidas".
- **Nome da contraparte** nas notificações e no extrato, como hoje.
- **Rodapé** "© 2026 capzz.labs".

## Caminhos considerados
1. **Reescrever no lugar, em `frontend/web`** — **escolhido pelo usuário.** Na branch da etapa, a base troca o Angular pelo React, e as telas vêm depois, em paralelo. A `main` segue com o Angular até o merge. Dockerfile, CI e nginx só trocam o build. É um app só, sem duplicar nada. Em troca, não dá para abrir os dois fronts lado a lado na branch (a `main` serve para comparar).
2. **App novo ao lado (`frontend/web-react`) e troca no fim.** Os dois rodam juntos durante a etapa, mas CI, Docker e testes ficam duplicados e entra uma task de troca no fim. Descartado.
3. **Next.js.** Precisaria de um servidor Node para o front, além do BFF, e mudaria como o nginx serve o web. Descartado.

## Decisões técnicas (escolhas da coordenadora, comunicadas ao usuário)
- **Vite + React + TypeScript**, SPA estático servido pelo nginx sem root, como hoje.
- **Tailwind CSS** e **shadcn/ui** (componentes copiados para `frontend/web/src/components/ui`), com o tema escuro da Noite Apple em variáveis CSS (tokens).
- **React Router** para as rotas e os guards, **TanStack Query** para as chamadas ao BFF, num cliente `fetch` único que faz o CSRF, o 401 e o BFF fora.
- **Motion** para transições de tela e entrada de itens. O cartão e as luzes ficam em CSS puro.
- **Vitest + Testing Library** nos testes de unidade, com o comportamento coberto pelos 224 testes atuais como checklist.
- **ESLint + Prettier** no padrão do workspace, trocando `angular-eslint` pelas regras de React.
- **Ferramentas de design no projeto:** Impeccable instalada no escopo do projeto, com o hook de design ligado e `PRODUCT.md`/`DESIGN.md` gerados pelo `init` e pelo `document`; shadcn MCP no `.mcp.json` do projeto. O 21st MCP já está no escopo local do usuário (a chave fica fora do git). Modo `live` da Impeccable só com `IMPECCABLE_LIVE_COPY_AGENT=chat`.
- **Um ADR novo** (0029, depois do 0028 da etapa 10) que substitui a escolha do Angular feita na etapa 9, e a seção do frontend de `docs/arquitetura.md` atualizada.

## Testes e prontidão
- Unidade (Vitest) para cada regra da lista acima e para cada tela.
- **Playwright:** os cinco cenários atuais (`skeleton`, `demo-user`, `sign-up-and-first-transfer`, `daily-limit-refusal`, `notification-bell`) continuam e passam, com seletores ajustados às telas novas, de preferência por papel e rótulo.
- **Qualidade de design:** `npx impeccable detect` sem achados nas telas, e `audit` da Impeccable sem P0/P1 (acessibilidade, contraste, responsividade).
- Build, lint, testes do web e do BFF e o job de Playwright do CI verdes. Smoke local contra as imagens.

## Respostas da leitura de construtor (2026-11-26)
Do projeto:
- **`/bff` sem mudança:** o nginx de borda (`infra/local/nginx/nginx.conf:115`) manda `/bff/` para `bff:3000`, e o cookie `bank_csrf` fica no mesmo domínio, lido de `document.cookie` como hoje. O `frontend/web/nginx.conf` só serve estáticos com fallback de SPA. Em desenvolvimento, o Vite usa `server.proxy` de `/bff` para `http://127.0.0.1:3000`.

Escolhas da coordenadora:
- **Saída do build:** o único consumidor de `dist/web/browser` é o `COPY` do `frontend/web/Dockerfile`, que passa a copiar o `dist/` do Vite.
- **Lógica fora dos componentes:** sessão, cliente do BFF (CSRF, 401, BFF fora), chave da transferência, centavos, telefone, senha e Problem Details ficam em módulos TypeScript puros em `frontend/web/src/lib/`, sem React, testados sozinhos. Os componentes e os hooks só os usam. A base da etapa porta esses módulos primeiro, e as telas vêm depois.
- **Textos e rótulos podem mudar** com o redesenho. Os e2e e o `e2e/tests/support.ts` são atualizados na mesma task da tela, e todo campo e botão mantém nome acessível (rótulo e papel). O saldo animado (contagem crescente) termina no valor exato, e o e2e espera o valor final.
- **`/avisos` → `/notificacoes`** é redirecionado no React Router, sem mexer no nginx.
- **Impeccable no CI:** o job do web ganha o passo `npx impeccable detect --github` com versão fixada, que falha se houver achados. O `audit` e o `critique` rodam localmente, na entrega de cada tela, e o resultado vai no relato.
- **Orçamento de desempenho:** o JavaScript inicial fica em até 250 KB gzip, as animações só usam `transform` e `opacity`, e o Lighthouse mobile, rodado localmente contra a imagem, dá pelo menos 90 em desempenho e acessibilidade.
- **Número do ADR:** o próximo livre no merge (0029 se a etapa 10 entrar antes com o 0028).

## Deixado para quem constrói
- A composição exata de cada tela dentro da direção visual, guiada pelos comandos da Impeccable (`shape`, `critique`, `polish`).
- Quais componentes vêm do shadcn, quais do 21st e quais são próprios.
- A fonte: sem Inter; de preferência uma fonte de sistema (SF Pro no Apple) com uma alternativa livre definida no `DESIGN.md`.
- Microinterações (hover, toque, estados de carregamento e de vazio).
