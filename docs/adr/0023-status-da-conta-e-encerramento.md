# 0023 — Status da conta e encerramento

- **Status:** aceito
- **Data:** 2026-11-18

## Contexto
O admin precisa bloquear uma conta e o cliente precisa encerrar a própria. O ledger nunca é apagado. O `accounts` não conhece as transferências, e a saga do `transfers` ([ADR 0016](0016-saga-orquestrada-por-http.md)) já chama o `accounts` por HTTP. Encerrar uma conta com transferência `PENDING` saindo dela deixaria uma compensação a creditar uma conta encerrada.

## Decisão
- **Status da conta** (`ACTIVE`, `BLOCKED`, `CLOSED`) fica na própria conta. É conferido na mesma transação do débito e do crédito, **sob a trava da conta** ([ADR 0012](0012-saldo-no-lancamento-e-trava-pessimista.md)). Conta bloqueada recusa débito (`422 account-blocked`) e aceita crédito; conta encerrada recusa os dois (`422 account-closed`). A conferência do status vem depois da busca por chave de idempotência: a repetição de um lançamento já gravado devolve o resultado original, mesmo que a conta tenha sido bloqueada ou encerrada depois.
- **No encerramento, o `accounts` consulta o `transfers` de forma síncrona:** `GET /internal/accounts/{id}/pending-transfers`, só com token de máquina ([ADR 0020](0020-identidade-e-autorizacao.md)), fora de qualquer transação. Há transferência `PENDING` saindo da conta: `422 pending-transfers`. `transfers` fora, com erro ou timeout: `503 transfers-unavailable`, e nada é gravado. Isso fecha um ciclo síncrono: o `transfers` chama o `accounts` na saga e o `accounts` chama o `transfers` ao encerrar.
- **Lançamento depois da consulta recusa o encerramento.** A pré-checagem guarda a `sequence` do último lançamento. Sob a trava, o encerramento exige a mesma `sequence`; se mudou, responde `422 pending-transfers`, mesmo que o saldo tenha voltado a zero, porque o débito pode ser de uma transferência que a consulta não viu.
- **Fecha a corrida:** depois do commit do encerramento, um débito vê `CLOSED` e a transferência termina `FAILED` (o limite volta); um crédito recusado vira compensação. Uma compensação nunca credita conta encerrada, porque transferência com débito feito continua `PENDING` até terminar.

## Consequências
- Positivas: o `transfers` não muda (recusa de débito já vira `FAILED`, recusa de crédito já vira compensação); nenhum estado novo a sincronizar entre serviços; bloqueio e encerramento valem na hora, pela fonte da verdade; sem conexão de banco aberta durante a chamada ao `transfers`.
- Negativas: ciclo síncrono `accounts` ↔ `transfers`, em que o `transfers` fora do ar impede encerrar (o resto do `accounts` não depende dele); recusa conservadora, pois qualquer lançamento entre a consulta e a trava (um crédito, por exemplo) barra o encerramento com `count` 1 e o cliente tenta de novo; a transferência `COMPENSATED` por `account-closed` não guarda o motivo.

## Alternativas consideradas
- **Projetar os eventos do `transfers` no `accounts`** (pendentes por conta): evitaria o ciclo, mas traria consistência eventual justamente na checagem que impede dinheiro preso, além de uma tabela e um consumidor novos.
- **Confiar só na `sequence` e na compensação:** sem a consulta, uma transferência `PENDING` com o débito ainda por fazer terminaria `FAILED`, mas uma já debitada teria a compensação recusada na conta encerrada, sem destino para o valor.
- **Status só no `customer`:** o bloqueio do cliente é por pessoa e não cobre encerramento nem conta.
