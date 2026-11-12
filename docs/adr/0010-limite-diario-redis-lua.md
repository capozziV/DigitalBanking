# 0010 — Limite diário com Redis + Lua

- **Status:** aceito (atualizado em 2026-11-12 com as decisões da etapa 6; [spec](../superpowers/specs/2026-11-12-etapa-6-limite-diario-design.md))
- **Data:** 2026-10-21

## Contexto
Transferências têm limite diário. A verificação precisa ser atômica sob concorrência, resistente a retentativas da saga e continuar valendo se o Redis cair ou perder os dados.

## Decisão
- **Passo `ReserveLimit` antes do débito.** Limite estourado termina a transferência em `FAILED`, motivo `DAILY_LIMIT_EXCEEDED`.
- **Dia do calendário em `America/Sao_Paulo`**, o do pedido (`created_at`), fixo para a transferência.
- **Ciclo da reserva:** conta enquanto a transferência está `PENDING`, é consumida em `COMPLETED` e liberada em `FAILED` e `COMPENSATED`.
- **Fonte da verdade no PostgreSQL do `transfers`:** colunas `limit_day` e `limit_reserved` da tabela `transfer`. O total do dia é a soma das reservas em `PENDING` ou `COMPLETED`; a liberação é a mudança de status.
- **Contador no Redis**, manipulado por **script Lua** (reservar, liberar, semear), com reserva **idempotente por `transferId`**. TTL de 10 minutos, renovado a cada reserva; contador ausente é **reconstruído sob demanda** a partir do PostgreSQL.
- **Trava única:** toda reserva, pela via do Redis ou do PostgreSQL, roda numa transação que começa com `pg_advisory_xact_lock` por conta e dia.
- **Epoch:** a tabela `daily_limit_epoch` (conta e dia) é incrementada a cada reserva feita pela via do PostgreSQL. A via do Redis compara o epoch com o do contador e, se diferir, reconstrói antes de conferir.
- **Fallback:** com o Redis fora, a conferência é feita no PostgreSQL, na mesma trava. Se o PostgreSQL também falhar, há retentativa até o prazo da saga e depois `FAILED` com `LIMIT_UNAVAILABLE`.
- **Valor padrão** (decidido em 2026-11-12): R$ 5.000,00 (500000 centavos), único para todas as contas no MVP. A variação por tipo de conta fica para depois do MVP.

## Consequências
- Positivas: verificação e reserva atômicas; sem brecha entre as duas vias; o contador do Redis só erra para cima (lado seguro) e o TTL o corrige; retentativas não consomem limite duas vezes; perda ou queda do Redis não quebra o limite.
- Negativas: dois lugares com o mesmo dado (Redis e PostgreSQL); uma transação curta por reserva; contenção só entre transferências da mesma conta e dia; liberação no Redis é melhor esforço; exige rotina de reconstrução e o epoch.

## Alternativas consideradas
- **Só Redis, sem trava no PostgreSQL:** deixa brecha entre o Redis e o fallback, que poderia passar do limite. Descartada.
- **Só PostgreSQL:** simples e correto, mas perde o contador rápido que a etapa pretende exercitar. Descartada.
