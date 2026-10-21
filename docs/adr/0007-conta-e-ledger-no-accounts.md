# 0007 — Conta e ledger no mesmo serviço (`accounts`)

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto
Saldo e lançamentos precisam estar sempre consistentes entre si. Débitos só podem ser autorizados com base no saldo real.

## Decisão
- Conta e ledger ficam no mesmo serviço, `accounts`, gravados na **mesma transação ACID** (PostgreSQL).
- O ledger é **imutável**.
- `accounts` é a fonte da verdade do saldo.
- O saldo usado para autorizar débito **nunca vem de cache**.
- Valores monetários sempre em centavos (`Long`), nunca `Double`/float.

## Consequências
- Positivas: saldo e lançamento nunca divergem; histórico auditável; sem risco de autorizar débito com saldo desatualizado.
- Negativas: toda autorização de débito vai ao banco; o `accounts` concentra a carga de escrita e precisa tratar concorrência no saldo (tema da etapa 1).

## Pontos em aberto
- Mecanismo de controle de concorrência no saldo: não definido no planejamento (previsto para a etapa 1).

## Alternativas consideradas
Não registradas no planejamento.
