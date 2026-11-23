# Sininho de notificações (design curto)

**Data:** 2026-11-22
**Status:** execução liberada pelo usuário sem validação prévia do texto (2026-11-22: "já pode sair executando que não precisa validar nada")
**Cartão:** [#102](https://github.com/capozziV/DigitalBanking/issues/102), no PR da etapa 9

## Intenção
Ver que chegou notificação, e lê-la, sem sair da tela atual ("a notificação chega", spec da etapa 9).

## Decisões do usuário
- Entra no PR da etapa 9.
- As 5 mais recentes e o resto ao rolar.
- Todas viram lidas ao abrir o sininho.
- Polling de 30 s com a aba visível.

## Abordagens
1. **Sino com painel, mantendo a página e o item do menu (escolha da coordenadora).** É aditiva e não desfaz nada já aprovado. Custo: dois caminhos para a mesma lista, que dividem o mesmo estado.
2. Sino no lugar do item do menu e da página: um caminho só, mas remove uma tela aprovada.
3. Sino que só navega para a página: não atende ao #102, que pede a lista no clique.

## Desenho (escolhas da coordenadora, reversíveis)
- Sino no canto superior direito: na barra do topo no celular, no topo do conteúdo no computador. Visual preto e branco da etapa, com o contador de não lidas.
- O clique abre um painel com altura para cerca de 5 itens. A rolagem mostra o resto da página de 20 e busca a seguinte pelo `nextCursor`.
- Ao abrir e carregar, marca como lidas até a mais recente exibida (`upTo`). O que chegar depois segue não lido. As que eram não lidas ficam destacadas até o painel fechar.
- O contador atualiza a cada 30 s só com a aba visível e na hora em que ela volta a ficar visível. Sem chamadas sobrepostas, para ao sair da sessão, e uma falha transitória mantém o último número.
- Fecha com Esc, clique fora e navegação, devolvendo o foco ao sino no Esc. Funciona por teclado e no celular.
- A página Notificações e o botão "Marcar como lidas" continuam.

## Fora do escopo
Push, mudança no backend e preferências de notificação.

## Consequência aceita
Corrigido em 2026-11-22: a versão anterior dizia que o polling renova a sessão de 7 dias, e isso é falso. O polling renova o token e o TTL no Redis, mas o cookie tem `maxAge` fixo desde o login: a sessão cai 7 dias após o login mesmo com uso. Débito #236.

## Testes
Unidade para intervalo, visibilidade, `upTo`, paginação, falha e teclado. Playwright: o contador aparece, zera ao abrir e nenhum texto de notificação tem id de conta.
