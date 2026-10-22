# 0012 — Saldo no lançamento e trava pessimista por conta

- **Status:** aceito
- **Data:** 2026-10-22

## Contexto
O [ADR 0007](0007-conta-e-ledger-no-accounts.md) fixou conta e ledger no `accounts`, na mesma transação, e deixou em aberto como controlar a concorrência no saldo. Dois débitos simultâneos na mesma conta não podem ambos passar pela checagem de saldo disponível, e o saldo nunca pode divergir dos lançamentos.

## Decisão
- **Saldo no lançamento:** cada lançamento (`LedgerEntry`) guarda `sequence` (por conta, a partir de 1) e `balance_after`. O saldo da conta é o `balance_after` do último lançamento. Não existe coluna de saldo na conta.
- `UNIQUE (account_id, sequence)` impede lançamento duplicado na sequência.
- **Mesmo caminho de gravação** para débito e crédito: ambos precisam do lançamento anterior para a próxima `sequence` e o `balance_after`. Só a validação muda: o débito confere o saldo disponível; o crédito não.
- **Trava pessimista:** `SELECT … FROM account WHERE id = ? FOR UPDATE` trava a conta; depois lê o último lançamento e grava. A `UNIQUE` fica como rede de segurança.
- **Tempo de espera:** `SET LOCAL lock_timeout` na transação, configurável (padrão 2s). Ao estourar, a resposta é `503` com `Retry-After`, seguro porque as operações são idempotentes. `NOWAIT` descartado.
- **Regra contra deadlock:** nenhuma transação do `accounts` trava duas contas. A transferência é uma saga, com débito e crédito em etapas separadas. Quem precisar travar duas contas na mesma transação terá de rever este ADR.
- **Conta nova:** nasce sem lançamentos, com saldo 0 implícito; o primeiro lançamento tem `sequence` 1. Sem lançamento de abertura.
- **Débito recusado** por saldo disponível insuficiente: `422` em Problem Details, tipo `insufficient-available-balance`, devolvendo o saldo disponível atual. A recusa não entra no ledger; fica em log e métrica.

## Consequências
- Positivas: saldo e lançamento nunca divergem (um único registro guarda os dois); sem segunda fonte de saldo para reconciliar; o `FOR UPDATE` e a `UNIQUE` tornam a concorrência explícita no SQL e no esquema; sem nova tentativa em disputa.
- Negativas: todas as operações de uma mesma conta ficam em fila, então uma conta muito movimentada (ex.: conta de recebimentos) vira gargalo. Bancos reais contornam com saldo particionado ou créditos em lote; fora do escopo. Sob disputa forte, requisições podem terminar em `503`.

## Alternativas consideradas
- **Coluna `balance` na `account`, atualizada na mesma transação:** descartada em favor do saldo no lançamento, que elimina a segunda fonte de saldo.
- **Saldo sempre calculado por `SUM` dos lançamentos:** descartada; o custo cresce com o tamanho do ledger.
- **Controle otimista usando a própria `UNIQUE`, com novas tentativas:** descartado; as tentativas se multiplicam sob disputa e exigem backoff e limite de tentativas.
- **`pg_advisory_xact_lock`:** descartado; preso a uma função específica do PostgreSQL e menos visível no esquema.
