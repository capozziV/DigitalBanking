# 0026 — Bônus de boas-vindas por cliente

- **Status:** aceito
- **Data:** 2026-11-22

## Contexto
O Bônus de boas-vindas (`WelcomeBonus`, R$ 100) era creditado a cada conta aberta. Como o cliente pode encerrar a conta ([ADR 0023](0023-status-da-conta-e-encerramento.md)) e abrir outra, o bônus se repetia sem fim, e o limite diário vinha novo a cada conta. O banco é simulado, mas dinheiro criado sem limite derruba o sentido do ledger.

## Decisão
- O bônus é **um por cliente**, só na primeira conta. Encerrar e reabrir não repete: a conta nova nasce com saldo zero.
- Ao abrir, na mesma transação e depois do insert da conta, o `accounts` credita o bônus só se o cliente não tem outra conta, **inclusive encerrada**.
- O índice único parcial de conta não encerrada por cliente serializa as aberturas do mesmo cliente, então duas aberturas concorrentes não concedem o bônus duas vezes. Não há tabela nem migration nova.

## Consequências
- Positivas: o bônus não se repete; nenhuma estrutura nova a manter; a concessão é atômica com a abertura da conta.
- Negativas: a regra depende de nunca apagar contas encerradas (apagar uma reabriria o direito ao bônus); quem encerrou uma conta anterior ao bônus não o recebe ao reabrir.

## Alternativas consideradas
- **Aceitar a repetição:** simples, mas permite criar R$ 100 a cada encerramento e abertura.
- **Tabela própria de bônus concedidos:** não depende da retenção das contas encerradas, mas exige tabela, migration e outro ponto de consistência com a abertura.
